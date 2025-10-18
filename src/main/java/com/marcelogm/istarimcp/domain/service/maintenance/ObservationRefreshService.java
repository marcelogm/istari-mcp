package com.marcelogm.istarimcp.domain.service.maintenance;

import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.ObservationRepository;
import com.marcelogm.istarimcp.domain.service.EmbeddingService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Singleton
public class ObservationRefreshService {

    private static final Logger log = LoggerFactory.getLogger(ObservationRefreshService.class);
    private static final int OBSERVATION_RETRY_ATTEMPTS = 2;
    private static final Duration OBSERVATION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration OBSERVATION_RETRY_DELAY = Duration.ofSeconds(1);
    private static final Duration OBSERVATION_MAX_BACKOFF = Duration.ofSeconds(5);

    private final EmbeddingService embeddingService;
    private final ObservationRepository observationRepository;

    @Inject
    public ObservationRefreshService(
            EmbeddingService embeddingService,
            ObservationRepository observationRepository) {
        this.embeddingService = embeddingService;
        this.observationRepository = observationRepository;
    }

    public Mono<Void> refreshObservations(List<ObservationNode> observations) {
        return Flux.fromIterable(observations)
                .flatMap(this::refreshSingleObservation)
                .then();
    }

    public Mono<Boolean> refreshSingleObservation(ObservationNode observation) {
        return embeddingService.create(observation.observation())
                .flatMap(embedding -> updateObservationEmbedding(observation, embedding))
                .retryWhen(buildRetryStrategy())
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

    private Retry buildRetryStrategy() {
        return Retry.backoff(OBSERVATION_RETRY_ATTEMPTS, OBSERVATION_RETRY_DELAY)
                .maxBackoff(OBSERVATION_MAX_BACKOFF)
                .doBeforeRetry(signal -> log.warn("Retrying embedding for observation (attempt {})",
                        signal.totalRetries() + 1));
    }
}
