package com.marcelogm.istarimcp.service;

import com.marcelogm.istarimcp.api.observation.CreateObservationRequest;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.ObservationRepository;
import com.marcelogm.istarimcp.domain.service.EmbeddingService;
import com.marcelogm.istarimcp.domain.service.ObservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservationServiceTest {

    @Mock
    private ObservationRepository repository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private ObservationService observationService;

    @Test
    @DisplayName("should add observation to memory")
    void shouldAddObservationToMemory() {
        final var request = new CreateObservationRequest("Test Memory", "New observation");
        final var embedding = List.of(1.0f, 0.0f, 0.0f);
        final var observationNode = new ObservationNode(UUID.randomUUID(), "New observation", embedding);

        when(embeddingService.create("New observation")).thenReturn(Mono.just(embedding));
        when(repository.addObservationToMemory(eq("Test Memory"), any(ObservationNode.class)))
                .thenReturn(Optional.of(observationNode));

        StepVerifier.create(observationService.addObservation(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.id());
                    assertEquals("Test Memory", response.memoryName());
                    assertEquals("New observation", response.observation());
                })
                .verifyComplete();

        verify(embeddingService).create("New observation");
        verify(repository).addObservationToMemory(eq("Test Memory"), any(ObservationNode.class));
    }

    @Test
    @DisplayName("should throw exception when memory not found")
    void shouldThrowExceptionWhenMemoryNotFound() {
        final var request = new CreateObservationRequest("Non Existent Memory", "New observation");
        final var embedding = List.of(1.0f, 0.0f, 0.0f);

        when(embeddingService.create("New observation")).thenReturn(Mono.just(embedding));
        when(repository.addObservationToMemory(eq("Non Existent Memory"), any(ObservationNode.class)))
                .thenReturn(Optional.empty());

        StepVerifier.create(observationService.addObservation(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().contains("Memory not found: Non Existent Memory")
                )
                .verify();

        verify(embeddingService).create("New observation");
        verify(repository).addObservationToMemory(eq("Non Existent Memory"), any(ObservationNode.class));
    }
}
