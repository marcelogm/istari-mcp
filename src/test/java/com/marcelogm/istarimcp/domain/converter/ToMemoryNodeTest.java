package com.marcelogm.istarimcp.domain.converter;

import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.domain.service.EmbeddingService;
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
        final var observations = List.of("observation1", "observation2", "observation3");
        final var request = new CreateMemoryRequest(
                "Test Memory",
                "Test Description",
                observations);
        final var embedding1 = List.of(1.0f, 2.0f, 3.0f);
        final var embedding2 = List.of(4.0f, 5.0f, 6.0f);
        final var embedding3 = List.of(7.0f, 8.0f, 9.0f);
        final var memoryEmbedding = List.of(10.0f, 11.0f, 12.0f);

        when(embeddingService.create("observation1"))
                .thenReturn(Mono.just(embedding1));
        when(embeddingService.create("observation2"))
                .thenReturn(Mono.just(embedding2));
        when(embeddingService.create("observation3"))
                .thenReturn(Mono.just(embedding3));
        when(embeddingService.createFromParts("Test Memory: Test Description", observations))
                .thenReturn(Mono.just(memoryEmbedding));

        // when
        StepVerifier.create(toMemoryNode.apply(request))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertNotNull(result.id());
                    assertEquals("Test Memory", result.name());
                    assertEquals("Test Description", result.description());
                    assertEquals(memoryEmbedding, result.embedding());
                    assertEquals(3, result.observations().size());

                    assertEquals("observation1", result.observations().get(0).observation());
                    assertEquals(embedding1, result.observations().get(0).embedding());
                    assertEquals("observation2", result.observations().get(1).observation());
                    assertEquals(embedding2, result.observations().get(1).embedding());
                    assertEquals("observation3", result.observations().get(2).observation());
                    assertEquals(embedding3, result.observations().get(2).embedding());
                })
                .verifyComplete();

        verify(embeddingService, times(3)).create(anyString());
        verify(embeddingService, times(1)).createFromParts(anyString(), any());
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

        when(embeddingService.createFromParts("Test Memory: Test Description", Collections.emptyList()))
                .thenReturn(Mono.just(memoryEmbedding));

        // when
        StepVerifier.create(toMemoryNode.apply(request))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertNotNull(result.id());
                    assertEquals("Test Memory", result.name());
                    assertEquals("Test Description", result.description());
                    assertEquals(memoryEmbedding, result.embedding());
                    assertEquals(0, result.observations().size());
                })
                .verifyComplete();

        verify(embeddingService, times(1)).createFromParts(anyString(), any());
    }
}
