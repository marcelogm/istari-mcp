package com.marcelogm.istarimcp.domain.service.maintenance;

import com.marcelogm.istarimcp.api.maintenance.RefreshStatusResponse.RefreshResult;
import com.marcelogm.istarimcp.api.maintenance.RefreshStatusResponse.RefreshStatus;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import com.marcelogm.istarimcp.domain.service.EmbeddingService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);
    private static final int MAX_CONCURRENT_MEMORIES = 16;
    private static final int MEMORY_RETRY_ATTEMPTS = 3;
    private static final Duration MEMORY_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration MEMORY_RETRY_DELAY = Duration.ofSeconds(2);
    private static final Duration MEMORY_MAX_BACKOFF = Duration.ofSeconds(10);

    private final MemoryRepository repository;
    private final EmbeddingService embeddingService;
    private final ObservationRefreshService observationRefreshService;
    private final AtomicReference<RefreshStatus> currentStatusType = new AtomicReference<>(RefreshStatus.IDLE);
    private final AtomicReference<RefreshResult> lastResult = new AtomicReference<>(null);
    private final AtomicReference<String> lastError = new AtomicReference<>(null);

    @Inject
    public MaintenanceService(
            MemoryRepository repository,
            EmbeddingService embeddingService,
            ObservationRefreshService observationRefreshService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.observationRefreshService = observationRefreshService;
    }

    public RefreshStatus getStatus() {
        return currentStatusType.get();
    }

    public RefreshResult getLastResult() {
        return lastResult.get();
    }

    public String getLastError() {
        return lastError.get();
    }

    public Mono<Boolean> startRefreshing() {
        if (!currentStatusType.compareAndSet(RefreshStatus.IDLE, RefreshStatus.RUNNING)) {
            log.warn("Cannot start refreshing. Current status: {}", currentStatusType.get());
            return Mono.just(false);
        }

        log.info("Starting refreshing");
        refreshAllAsync();
        return Mono.just(true);
    }

    private void refreshAllAsync() {
        final var startTime = LocalDateTime.now();
        final var processed = new AtomicInteger(0);
        final var failed = new AtomicInteger(0);

        repository.streamAll()
                .flatMap(memory -> refreshMemory(memory, processed, failed), MAX_CONCURRENT_MEMORIES)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnComplete(() -> handleCompletion(processed, failed, startTime))
                .doOnError(error -> handleError(processed, failed, startTime, error))
                .subscribe();
    }

    private Mono<Boolean> refreshMemory(MemoryNode memory, AtomicInteger processed, AtomicInteger failed) {
        var observationTexts = extractObservationTexts(memory);
        var mainText = buildMainText(memory);

        return observationRefreshService.refreshObservations(memory.observations())
                .then(generateEmbedding(memory, mainText, observationTexts))
                .flatMap(embedding -> updateMemoryWithEmbedding(memory, embedding, processed, failed))
                .onErrorResume(error -> handleMemoryError(memory, error, failed));
    }

    private List<String> extractObservationTexts(MemoryNode memory) {
        return memory.observations().stream()
                .map(obs -> obs.observation())
                .toList();
    }

    private String buildMainText(MemoryNode memory) {
        return memory.name() + ": " + memory.description();
    }

    private Mono<List<Float>> generateEmbedding(MemoryNode memory, String mainText, List<String> observationTexts) {
        return embeddingService.createFromParts(mainText, observationTexts)
                .retryWhen(buildRetryStrategy(memory))
                .timeout(MEMORY_TIMEOUT);
    }

    private Retry buildRetryStrategy(MemoryNode memory) {
        return Retry.backoff(MEMORY_RETRY_ATTEMPTS, MEMORY_RETRY_DELAY)
                .maxBackoff(MEMORY_MAX_BACKOFF)
                .doBeforeRetry(signal -> log.warn("Retrying embedding for memory: {} (attempt {})",
                        memory.name(), signal.totalRetries() + 1));
    }

    private Mono<Boolean> updateMemoryWithEmbedding(
            MemoryNode memory,
            List<Float> embedding,
            AtomicInteger processed,
            AtomicInteger failed) {
        var updated = repository.updateMemory(memory.name(), memory.description(), embedding);

        if (updated.isPresent()) {
            processed.incrementAndGet();
            log.info("Refreshed memory: {}", memory.name());
            return Mono.just(true);
        } else {
            failed.incrementAndGet();
            log.error("Failed to update memory: {}", memory.name());
            return Mono.just(false);
        }
    }

    private Mono<Boolean> handleMemoryError(MemoryNode memory, Throwable error, AtomicInteger failed) {
        failed.incrementAndGet();
        log.error("Error refreshing memory: {}", memory.name(), error);
        return Mono.just(false);
    }

    private void handleCompletion(AtomicInteger processed, AtomicInteger failed, LocalDateTime startTime) {
        var result = new RefreshResult(
                processed.get(),
                failed.get(),
                startTime,
                LocalDateTime.now());
        lastResult.set(result);
        currentStatusType.set(RefreshStatus.COMPLETED);
        log.info("Refreshing completed. Processed: {}, Failed: {}",
                processed.get(), failed.get());
    }

    private void handleError(AtomicInteger processed, AtomicInteger failed, LocalDateTime startTime, Throwable error) {
        var result = new RefreshResult(
                processed.get(),
                failed.get(),
                startTime,
                LocalDateTime.now());
        lastResult.set(result);
        lastError.set(error.getMessage());
        currentStatusType.set(RefreshStatus.FAILED);
        log.error("Refreshing failed", error);
    }
}
