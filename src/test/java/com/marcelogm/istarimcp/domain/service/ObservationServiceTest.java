package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.api.observation.CreateObservationRequest;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import com.marcelogm.istarimcp.domain.repository.ObservationRepository;
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

import static com.marcelogm.istarimcp.helper.MemoryFixtures.memory;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservationServiceTest {

    @Mock
    private ObservationRepository repository;

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private ObservationService observationService;

    @Test
    @DisplayName("should add observation to memory and update memory embedding")
    void shouldAddObservationToMemory() {
        // given
        final var MEMORY_NAME = "Test Memory";
        final var OBSERVATION = "New observation";

        final var request = new CreateObservationRequest(MEMORY_NAME, OBSERVATION);
        final var embedding = List.of(1.0f, 0.0f, 0.0f);
        final var newEmbedding = List.of(0.0f, 1.0f, 0.0f);
        final var observationNode = new ObservationNode(UUID.randomUUID(), OBSERVATION, embedding);
        final var memory = memory(MEMORY_NAME, OBSERVATION, embedding);

        when(embeddingService.create("New observation")).thenReturn(Mono.just(embedding));
        when(memoryRepository.findByName(MEMORY_NAME))
                .thenReturn(Optional.of(memory));
        when(embeddingService.createFromParts("Test Memory: New observation", singletonList(OBSERVATION)))
                .thenReturn(Mono.just(newEmbedding));
        when(repository.addObservationToMemory(eq(MEMORY_NAME), any(ObservationNode.class)))
                .thenReturn(Optional.of(observationNode));

        // when
        StepVerifier.create(observationService.addObservation(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.id());
                    assertEquals("Test Memory", response.memoryName());
                    assertEquals("New observation", response.observation());
                })
                .verifyComplete();

        // then
        verify(embeddingService).create("New observation");
        verify(repository).addObservationToMemory(eq("Test Memory"), any(ObservationNode.class));
        verify(memoryRepository).updateMemory("Test Memory", "New observation", newEmbedding);
    }

    @Test
    @DisplayName("should throw exception when memory not found")
    void shouldThrowExceptionWhenMemoryNotFound() {
        // given
        final var request = new CreateObservationRequest("Non Existent Memory", "New observation");

        when(memoryRepository.findByName("Non Existent Memory"))
                .thenReturn(Optional.empty());

        // when
        StepVerifier.create(observationService.addObservation(request))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Memory not found: Non Existent Memory"))
                .verify();

        // then
        verify(embeddingService, never()).create(any());
        verify(repository, never()).addObservationToMemory(anyString(), any(ObservationNode.class));
    }

    @Test
    @DisplayName("should throw exception when fail to create embedding")
    void shouldThrowExceptionWhenFailToCreateEmbedding() {
        // given
        final var MEMORY_NAME = "Test Memory";
        final var OBSERVATION = "New observation";
        final var request = new CreateObservationRequest(MEMORY_NAME, OBSERVATION);
        final var embedding = List.of(1.0f, 0.0f, 0.0f);
        final var memory = memory(MEMORY_NAME, OBSERVATION, embedding);

        when(embeddingService.create("New observation")).thenReturn(Mono.just(embedding));
        when(memoryRepository.findByName(MEMORY_NAME))
                .thenReturn(Optional.of(memory));
        when(repository.addObservationToMemory(eq(MEMORY_NAME), any(ObservationNode.class)))
                .thenReturn(Optional.empty());

        // when
        StepVerifier.create(observationService.addObservation(request))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Failed to add observation"))
                .verify();

        // then
        verify(embeddingService).create("New observation");
        verify(repository).addObservationToMemory(eq("Test Memory"), any(ObservationNode.class));
    }
}
