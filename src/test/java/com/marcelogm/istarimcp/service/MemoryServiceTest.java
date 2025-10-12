package com.marcelogm.istarimcp.service;

import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.domain.converter.ToMemoryNode;
import com.marcelogm.istarimcp.domain.converter.ToMemoryResponse;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import com.marcelogm.istarimcp.domain.service.MemoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private MemoryRepository repository;

    @Mock
    private ToMemoryNode toMemoryNode;

    @Mock
    private ToMemoryResponse toMemoryResponse;

    @InjectMocks
    private MemoryService memoryService;

    @Test
    @DisplayName("should create memory with observations")
    void shouldCreateMemoryWithObservations() {
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

        final var expectedResponse = new CreateMemoryResponse(
                memoryNode.id(),
                memoryNode.name(),
                memoryNode.description(),
                Collections.emptyList());

        when(toMemoryNode.apply(request)).thenReturn(Mono.just(memoryNode));
        when(repository.createMemory(memoryNode)).thenReturn(memoryNode);
        when(toMemoryResponse.apply(eq(memoryNode), any())).thenReturn(expectedResponse);

        StepVerifier.create(memoryService.create(request))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(memoryNode.id(), result.id());
                    assertEquals(memoryNode.name(), result.name());
                    assertEquals(memoryNode.description(), result.description());
                    assertTrue(result.suggestions().isEmpty());
                })
                .verifyComplete();

        verify(toMemoryNode).apply(request);
        verify(repository).createMemory(memoryNode);
        verify(toMemoryResponse).apply(eq(memoryNode), eq(Collections.emptyList()));
    }

    @Test
    @DisplayName("should persist memory through repository")
    void shouldPersistMemoryThroughRepository() {
        final var request = new CreateMemoryRequest(
                "Test Memory",
                "Test Description",
                List.of("observation"));

        final var memoryNode = new MemoryNode(
                UUID.randomUUID(),
                "Test Memory",
                "Test Description",
                Collections.emptyList(),
                List.of(1.0f, 2.0f, 3.0f));

        final var expectedResponse = new CreateMemoryResponse(
                memoryNode.id(),
                memoryNode.name(),
                memoryNode.description(),
                Collections.emptyList());

        when(toMemoryNode.apply(request)).thenReturn(Mono.just(memoryNode));
        when(repository.createMemory(memoryNode)).thenReturn(memoryNode);
        when(toMemoryResponse.apply(eq(memoryNode), any())).thenReturn(expectedResponse);

        StepVerifier.create(memoryService.create(request))
                .expectNextCount(1)
                .verifyComplete();

        verify(repository).createMemory(memoryNode);
    }
}
