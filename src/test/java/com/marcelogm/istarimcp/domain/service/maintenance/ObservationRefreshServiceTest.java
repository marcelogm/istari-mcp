package com.marcelogm.istarimcp.domain.service.maintenance;

import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.ObservationRepository;
import com.marcelogm.istarimcp.domain.service.EmbeddingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservationRefreshServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ObservationRepository observationRepository;

    @InjectMocks
    private ObservationRefreshService observationRefreshService;

    private ObservationNode testObservation;
    private List<Float> testEmbedding;

    @BeforeEach
    void setUp() {
        testObservation = new ObservationNode(
                UUID.randomUUID(),
                "Test observation",
                List.of(1.0f, 2.0f, 3.0f));

        testEmbedding = List.of(4.0f, 5.0f, 6.0f);
    }

    @Test
    @DisplayName("should refresh single observation successfully")
    void shouldRefreshSingleObservationSuccessfully() {
        // given
        when(embeddingService.create(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(observationRepository.updateObservationEmbedding(any(UUID.class), anyList()))
                .thenReturn(true);

        // when & then
        StepVerifier.create(observationRefreshService.refreshSingleObservation(testObservation))
                .expectNext(true)
                .verifyComplete();

        verify(embeddingService).create("Test observation");
        verify(observationRepository).updateObservationEmbedding(testObservation.id(), testEmbedding);
    }

    @Test
    @DisplayName("should handle embedding service error")
    void shouldHandleEmbeddingServiceError() {
        // given
        when(embeddingService.create(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Embedding service error")));

        // when & then
        StepVerifier.create(observationRefreshService.refreshSingleObservation(testObservation))
                .expectNext(false)
                .verifyComplete();

        verify(embeddingService, atLeast(1)).create("Test observation");
        verify(observationRepository, never()).updateObservationEmbedding(any(UUID.class), anyList());
    }

    @Test
    @DisplayName("should handle repository update failure")
    void shouldHandleRepositoryUpdateFailure() {
        // given
        when(embeddingService.create(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(observationRepository.updateObservationEmbedding(any(UUID.class), anyList()))
                .thenReturn(false);

        // when & then
        StepVerifier.create(observationRefreshService.refreshSingleObservation(testObservation))
                .expectNext(false)
                .verifyComplete();

        verify(embeddingService).create("Test observation");
        verify(observationRepository).updateObservationEmbedding(testObservation.id(), testEmbedding);
    }

    @Test
    @DisplayName("should handle timeout")
    void shouldHandleTimeout() {
        // given
        when(embeddingService.create(anyString()))
                .thenReturn(Mono.just(testEmbedding).delayElement(Duration.ofSeconds(35)));

        // when & then
        StepVerifier.create(observationRefreshService.refreshSingleObservation(testObservation))
                .expectNext(false)
                .verifyComplete();

        verify(embeddingService).create("Test observation");
        verify(observationRepository, never()).updateObservationEmbedding(any(UUID.class), anyList());
    }

    @Test
    @DisplayName("should refresh multiple observations successfully")
    void shouldRefreshMultipleObservationsSuccessfully() {
        // given
        final var observation1 = new ObservationNode(
                UUID.randomUUID(),
                "Observation 1",
                List.of(1.0f, 2.0f));

        final var observation2 = new ObservationNode(
                UUID.randomUUID(),
                "Observation 2",
                List.of(3.0f, 4.0f));

        final var observation3 = new ObservationNode(
                UUID.randomUUID(),
                "Observation 3",
                List.of(5.0f, 6.0f));

        final var observations = List.of(observation1, observation2, observation3);

        when(embeddingService.create(anyString()))
                .thenReturn(Mono.just(testEmbedding));
        when(observationRepository.updateObservationEmbedding(any(UUID.class), anyList()))
                .thenReturn(true);

        // when & then
        StepVerifier.create(observationRefreshService.refreshObservations(observations))
                .verifyComplete();

        verify(embeddingService, times(3)).create(anyString());
        verify(observationRepository, times(3)).updateObservationEmbedding(any(UUID.class), anyList());
    }

    @Test
    @DisplayName("should handle partial failures in multiple observations")
    void shouldHandlePartialFailuresInMultipleObservations() {
        // given
        final var observation1 = new ObservationNode(
                UUID.randomUUID(),
                "Observation 1",
                List.of(1.0f, 2.0f));

        final var observation2 = new ObservationNode(
                UUID.randomUUID(),
                "Observation 2",
                List.of(3.0f, 4.0f));

        final var observations = List.of(observation1, observation2);

        when(embeddingService.create("Observation 1"))
                .thenReturn(Mono.just(testEmbedding));
        when(embeddingService.create("Observation 2"))
                .thenReturn(Mono.error(new RuntimeException("Error on observation 2")));
        when(observationRepository.updateObservationEmbedding(observation1.id(), testEmbedding))
                .thenReturn(true);

        // when & then
        StepVerifier.create(observationRefreshService.refreshObservations(observations))
                .verifyComplete();

        verify(embeddingService).create("Observation 1");
        verify(embeddingService, atLeast(1)).create("Observation 2");
        verify(observationRepository).updateObservationEmbedding(observation1.id(), testEmbedding);
        verify(observationRepository, never()).updateObservationEmbedding(eq(observation2.id()), anyList());
    }

    @Test
    @DisplayName("should process observations concurrently in batch refresh")
    void shouldProcessObservationsConcurrentlyInBatchRefresh() {
        // given
        final var observations = List.of(
                new ObservationNode(UUID.randomUUID(), "Obs 1", List.of(1.0f)),
                new ObservationNode(UUID.randomUUID(), "Obs 2", List.of(2.0f)),
                new ObservationNode(UUID.randomUUID(), "Obs 3", List.of(3.0f)),
                new ObservationNode(UUID.randomUUID(), "Obs 4", List.of(4.0f)),
                new ObservationNode(UUID.randomUUID(), "Obs 5", List.of(5.0f)));

        when(embeddingService.create(anyString()))
                .thenReturn(Mono.just(testEmbedding).delayElement(Duration.ofMillis(100)));
        when(observationRepository.updateObservationEmbedding(any(UUID.class), anyList()))
                .thenReturn(true);

        // when & then
        StepVerifier.create(observationRefreshService.refreshObservations(observations))
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        verify(embeddingService, times(5)).create(anyString());
        verify(observationRepository, times(5)).updateObservationEmbedding(any(UUID.class), anyList());
    }
}
