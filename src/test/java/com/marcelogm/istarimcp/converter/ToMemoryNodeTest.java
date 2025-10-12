package com.marcelogm.istarimcp.converter;

import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.domain.converter.ToMemoryNode;
import com.marcelogm.istarimcp.domain.service.EmbeddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToMemoryNodeTest {

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private ToMemoryNode toMemoryNode;

    @Test
    @DisplayName("should convert memory request to memory node with observations")
    void shouldConvertCreateMemoryRequestToMemoryNode() {
        // given
        final var request = new CreateMemoryRequest(
                "Test Memory",
                "Test Description",
                List.of("observation1", "observation2", "observation3"));
        final var embedding1 = List.of(1.0f, 2.0f, 3.0f);
        final var embedding2 = List.of(4.0f, 5.0f, 6.0f);
        final var embedding3 = List.of(7.0f, 8.0f, 9.0f);
        final var memoryEmbedding = List.of(10.0f, 11.0f, 12.0f);

        when(embeddingService.create("observation1"))
                .thenReturn(CompletableFuture.completedFuture(embedding1));
        when(embeddingService.create("observation2"))
                .thenReturn(CompletableFuture.completedFuture(embedding2));
        when(embeddingService.create("observation3"))
                .thenReturn(CompletableFuture.completedFuture(embedding3));
        when(embeddingService.create("Test Memory: Test Description"))
                .thenReturn(CompletableFuture.completedFuture(memoryEmbedding));

        // when
        final var result = toMemoryNode.apply(request);

        // then
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Test Memory", result.name());
        assertEquals("Test Description", result.description());
        assertEquals(memoryEmbedding, result.embedding());
        assertEquals(3, result.observations().size());

        // and: verify embeddings
        assertEquals("observation1", result.observations().get(0).observation());
        assertEquals(embedding1, result.observations().get(0).embedding());
        assertEquals("observation2", result.observations().get(1).observation());
        assertEquals(embedding2, result.observations().get(1).embedding());
        assertEquals("observation3", result.observations().get(2).observation());
        assertEquals(embedding3, result.observations().get(2).embedding());

        verify(embeddingService, times(4)).create(anyString());
    }

    @Test
    @DisplayName("should convert memory request to memory node without observations")
    void shouldHandleEmptyObservations() {
        // given
        final var request = new CreateMemoryRequest(
                "Test Memory",
                "Test Description",
                List.of());
        final var memoryEmbedding = List.of(10.0f, 11.0f, 12.0f);

        when(embeddingService.create("Test Memory: Test Description"))
                .thenReturn(CompletableFuture.completedFuture(memoryEmbedding));

        // when
        final var result = toMemoryNode.apply(request);

        // then
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Test Memory", result.name());
        assertEquals("Test Description", result.description());
        assertEquals(memoryEmbedding, result.embedding());
        assertEquals(0, result.observations().size());

        verify(embeddingService, times(1)).create(anyString());
    }
}
