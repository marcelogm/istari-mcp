package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import com.marcelogm.istarimcp.domain.repository.ObservationRepository;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
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
public class MemoryReprocessService {

    private static final Logger log = LoggerFactory.getLogger(MemoryReprocessService.class);
    private static final int MAX_CONCURRENT_MEMORIES = 16;
    private static final int MEMORY_RETRY_ATTEMPTS = 3;
    private static final int OBSERVATION_RETRY_ATTEMPTS = 2;
    private static final Duration MEMORY_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration OBSERVATION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration MEMORY_RETRY_DELAY = Duration.ofSeconds(2);
    private static final Duration OBSERVATION_RETRY_DELAY = Duration.ofSeconds(1);
    private static final Duration MEMORY_MAX_BACKOFF = Duration.ofSeconds(10);
    private static final Duration OBSERVATION_MAX_BACKOFF = Duration.ofSeconds(5);

    private final MemoryRepository repository;
    private final EmbeddingService embeddingService;
    private final ObservationRepository observationRepository;
    private final AtomicReference<String> currentStatusType = new AtomicReference<>("IDLE");
    private final AtomicReference<ReprocessResult> lastResult = new AtomicReference<>(null);
    private final AtomicReference<String> lastError = new AtomicReference<>(null);

    @Inject
    public MemoryReprocessService(
            MemoryRepository repository,
            EmbeddingService embeddingService,
            ObservationRepository observationRepository
    ) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.observationRepository = observationRepository;
    }

    public ReprocessStatus getStatus() {
        var statusType = currentStatusType.get();
        return switch (statusType) {
            case "IDLE" -> ReprocessStatus.idle();
            case "RUNNING" -> ReprocessStatus.running();
            case "COMPLETED" -> ReprocessStatus.completed(lastResult.get());
            case "FAILED" -> ReprocessStatus.failed(lastResult.get(), lastError.get());
            default -> ReprocessStatus.idle();
        };
    }

    public Mono<Boolean> startReprocessing() {
        if (!currentStatusType.compareAndSet("IDLE", "RUNNING")) {
            log.warn("Cannot start reprocessing. Current status: {}", currentStatusType.get());
            return Mono.just(false);
        }

        log.info("Starting reprocessing");
        reprocessAllAsync();
        return Mono.just(true);
    }

    private void reprocessAllAsync() {
        final var startTime = LocalDateTime.now();
        final var processed = new AtomicInteger(0);
        final var failed = new AtomicInteger(0);

        repository.streamAll()
                .flatMap(memory -> reprocessMemory(memory, processed, failed), MAX_CONCURRENT_MEMORIES)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnComplete(() -> handleCompletion(processed, failed, startTime))
                .doOnError(error -> handleError(processed, failed, startTime, error))
                .subscribe();
    }

    private Mono<Boolean> reprocessMemory(MemoryNode memory, AtomicInteger processed, AtomicInteger failed) {
        var observationTexts = extractObservationTexts(memory);
        var mainText = buildMainText(memory);

        return reprocessObservations(memory)
                .then(generateEmbedding(memory, mainText, observationTexts))
                .flatMap(embedding -> updateMemoryWithEmbedding(memory, embedding, processed, failed))
                .onErrorResume(error -> handleMemoryError(memory, error, failed));
    }

    private Mono<Void> reprocessObservations(MemoryNode memory) {
        return Flux.fromIterable(memory.observations())
                .flatMap(this::reprocessSingleObservation)
                .then();
    }

    private Mono<Boolean> reprocessSingleObservation(ObservationNode observation) {
        return embeddingService.create(observation.observation())
                .flatMap(embedding -> updateObservationEmbedding(observation, embedding))
                .retryWhen(buildRetryStrategyForObservation())
                .timeout(OBSERVATION_TIMEOUT)
                .onErrorResume(error -> handleObservationError(observation, error));
    }

    private Mono<Boolean> updateObservationEmbedding(ObservationNode observation, List<Float> embedding) {
        var updated = observationRepository.updateObservationEmbedding(observation.id(), embedding);

        if (updated) {
            log.debug("Updated observation embedding for: {}", observation.observation());
        } else {
            log.warn("Failed to update observation embedding for: {}", observation.observation());
        }
        return Mono.just(updated);
    }

    private Mono<Boolean> handleObservationError(ObservationNode observation, Throwable error) {
        log.error("Error updating observation: {}", observation.observation(), error);
        return Mono.just(false);
    }

    private Retry buildRetryStrategyForObservation() {
        return Retry.backoff(OBSERVATION_RETRY_ATTEMPTS, OBSERVATION_RETRY_DELAY)
                .maxBackoff(OBSERVATION_MAX_BACKOFF)
                .doBeforeRetry(signal ->
                        log.warn("Retrying embedding for observation (attempt {})",
                                signal.totalRetries() + 1));
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
                .doBeforeRetry(signal ->
                        log.warn("Retrying embedding for memory: {} (attempt {})",
                                memory.name(), signal.totalRetries() + 1));
    }

    private Mono<Boolean> updateMemoryWithEmbedding(
            MemoryNode memory,
            List<Float> embedding,
            AtomicInteger processed,
            AtomicInteger failed
    ) {
        var updated = repository.updateMemory(memory.name(), memory.description(), embedding);

        if (updated.isPresent()) {
            processed.incrementAndGet();
            log.info("Reprocessed memory: {}", memory.name());
            return Mono.just(true);
        } else {
            failed.incrementAndGet();
            log.error("Failed to update memory: {}", memory.name());
            return Mono.just(false);
        }
    }

    private Mono<Boolean> handleMemoryError(MemoryNode memory, Throwable error, AtomicInteger failed) {
        failed.incrementAndGet();
        log.error("Error reprocessing memory: {}", memory.name(), error);
        return Mono.just(false);
    }

    private void handleCompletion(AtomicInteger processed, AtomicInteger failed, LocalDateTime startTime) {
        var result = new ReprocessResult(
                processed.get(),
                failed.get(),
                startTime,
                LocalDateTime.now()
        );
        lastResult.set(result);
        currentStatusType.set("COMPLETED");
        log.info("Reprocessing completed. Processed: {}, Failed: {}",
                processed.get(), failed.get());
    }

    private void handleError(AtomicInteger processed, AtomicInteger failed, LocalDateTime startTime, Throwable error) {
        var result = new ReprocessResult(
                processed.get(),
                failed.get(),
                startTime,
                LocalDateTime.now()
        );
        lastResult.set(result);
        lastError.set(error.getMessage());
        currentStatusType.set("FAILED");
        log.error("Reprocessing failed", error);
    }

    @Serdeable
    @Introspected
    public record ReprocessResult(
            int memoriesProcessed,
            int memoriesFailed,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
    }

    @Serdeable
    @Introspected
    public record ReprocessStatus(
            String status,
            ReprocessResult result,
            String errorMessage
    ) {
        public static ReprocessStatus idle() {
            return new ReprocessStatus("IDLE", null, null);
        }

        public static ReprocessStatus running() {
            return new ReprocessStatus("RUNNING", null, null);
        }

        public static ReprocessStatus completed(ReprocessResult result) {
            return new ReprocessStatus("COMPLETED", result, null);
        }

        public static ReprocessStatus failed(ReprocessResult result, String errorMessage) {
            return new ReprocessStatus("FAILED", result, errorMessage);
        }
    }
}
