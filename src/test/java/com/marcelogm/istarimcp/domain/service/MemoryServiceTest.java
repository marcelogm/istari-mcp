package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.api.memory.MemorySummaryResponse;
import com.marcelogm.istarimcp.api.memory.UpdateMemoryRequest;
import com.marcelogm.istarimcp.domain.converter.ToCreateMemoryResponse;
import com.marcelogm.istarimcp.domain.converter.ToMemoryNode;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private MemoryRepository repository;

    @Mock
    private ToMemoryNode toMemoryNode;

    @Mock
    private ToCreateMemoryResponse toCreateMemoryResponse;

    @Mock
    private MemoryQueryService queryService;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private MemoryService memoryService;

    @Test
    @DisplayName("should create memory with observations and search for suggestions")
    void shouldCreateMemoryWithObservations() {
        // given
        final var request = new CreateMemoryRequest(
                "Test Memory",
                "Test Description",
                List.of("observation1", "observation2"));

        final var observation1 = new ObservationNode(
                UUID.randomUUID(),
                "observation1",
                List.of(1.0f, 2.0f, 3.0f));
        final var observation2 = new ObservationNode(
                UUID.randomUUID(),
                "observation2",
                List.of(4.0f, 5.0f, 6.0f));

        final var memoryNode = new MemoryNode(
                UUID.randomUUID(),
                "Test Memory",
                "Test Description",
                List.of(observation1, observation2),
                List.of(10.0f, 11.0f, 12.0f));

        final var suggestions = List.of(
                new MemorySummaryResponse(UUID.randomUUID(), "Similar Memory", "", Optional.of(0.9f)));

        final var expectedResponse = new CreateMemoryResponse(
                memoryNode.id(),
                memoryNode.name(),
                memoryNode.description(),
                suggestions);

        when(toMemoryNode.apply(request)).thenReturn(Mono.just(memoryNode));
        when(repository.createMemory(memoryNode)).thenReturn(memoryNode);
        when(queryService.summarySimilaritySearch(anyString())).thenReturn(Flux.fromIterable(suggestions));
        when(toCreateMemoryResponse.apply(memoryNode, suggestions)).thenReturn(expectedResponse);

        // expect
        StepVerifier.create(memoryService.create(request))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(memoryNode.id(), result.id());
                    assertEquals(memoryNode.name(), result.name());
                    assertEquals(memoryNode.description(), result.description());
                    assertEquals(1, result.suggestions().size());
                })
                .verifyComplete();

        // then
        verify(toMemoryNode).apply(request);
        verify(repository).createMemory(memoryNode);
        verify(queryService).summarySimilaritySearch("Test Memory: Test Description");
        verify(toCreateMemoryResponse).apply(memoryNode, suggestions);
    }

    @Test
    @DisplayName("should create memory without observations")
    void shouldCreateMemoryWithoutObservations() {
        // given
        final var request = new CreateMemoryRequest(
                "Empty Memory",
                "Memory without observations",
                Collections.emptyList());

        final var memoryNode = new MemoryNode(
                UUID.randomUUID(),
                "Empty Memory",
                "Memory without observations",
                Collections.emptyList(),
                List.of(10.0f, 11.0f, 12.0f));

        final var expectedResponse = new CreateMemoryResponse(
                memoryNode.id(),
                memoryNode.name(),
                memoryNode.description(),
                Collections.emptyList());

        when(toMemoryNode.apply(request)).thenReturn(Mono.just(memoryNode));
        when(repository.createMemory(memoryNode)).thenReturn(memoryNode);
        when(queryService.summarySimilaritySearch(anyString())).thenReturn(Flux.empty());
        when(toCreateMemoryResponse.apply(memoryNode, Collections.emptyList())).thenReturn(expectedResponse);

        // expect
        StepVerifier.create(memoryService.create(request))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(memoryNode.id(), result.id());
                    assertEquals("Empty Memory", result.name());
                    assertEquals("Memory without observations", result.description());
                    assertTrue(result.suggestions().isEmpty());
                })
                .verifyComplete();

        // then
        verify(toMemoryNode).apply(request);
        verify(repository).createMemory(memoryNode);
        verify(queryService).summarySimilaritySearch("Empty Memory: Memory without observations");
    }

    @Test
    @DisplayName("should delete memory by name")
    void shouldDeleteMemoryByName() {
        // given
        when(repository.deleteByName("Test Memory")).thenReturn(true);

        // expect
        StepVerifier.create(memoryService.deleteByName("Test Memory"))
                .assertNext(result -> assertTrue(result))
                .verifyComplete();

        // then
        verify(repository).deleteByName("Test Memory");
    }

    @Test
    @DisplayName("should return false when deleting non-existent memory")
    void shouldReturnFalseWhenDeletingNonExistentMemory() {
        // given
        when(repository.deleteByName("Non Existent")).thenReturn(false);

        // expect
        StepVerifier.create(memoryService.deleteByName("Non Existent"))
                .assertNext(result -> assertFalse(result))
                .verifyComplete();

        // then
        verify(repository).deleteByName("Non Existent");
    }

    @Test
    @DisplayName("should update memory")
    void shouldUpdateMemory() {
        // given
        final var request = new UpdateMemoryRequest("Test Memory", "Updated Description");
        final var embedding = List.of(1.0f, 0.0f, 0.0f);
        final var updatedMemory = new MemoryNode(
                UUID.randomUUID(),
                "Test Memory",
                "Updated Description",
                Collections.emptyList(),
                embedding);

        when(embeddingService.create("Test Memory: Updated Description")).thenReturn(Mono.just(embedding));
        when(repository.updateMemory("Test Memory", "Updated Description", embedding))
                .thenReturn(Optional.of(updatedMemory));

        // expect
        StepVerifier.create(memoryService.updateMemory(request))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(updatedMemory.id(), result.id());
                    assertEquals("Test Memory", result.name());
                    assertEquals("Updated Description", result.description());
                })
                .verifyComplete();

        // then
        verify(embeddingService).create("Test Memory: Updated Description");
        verify(repository).updateMemory("Test Memory", "Updated Description", embedding);
    }

    @Test
    @DisplayName("should throw exception when updating non-existent memory")
    void shouldThrowExceptionWhenUpdatingNonExistentMemory() {
        // given
        final var request = new UpdateMemoryRequest("Non Existent", "New Description");
        final var embedding = List.of(1.0f, 0.0f, 0.0f);

        when(embeddingService.create("Non Existent: New Description")).thenReturn(Mono.just(embedding));
        when(repository.updateMemory("Non Existent", "New Description", embedding))
                .thenReturn(Optional.empty());

        // expect
        StepVerifier.create(memoryService.updateMemory(request))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Memory not found: Non Existent"))
                .verify();

        // then
        verify(embeddingService).create("Non Existent: New Description");
        verify(repository).updateMemory("Non Existent", "New Description", embedding);
    }
}
