package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.client.EmbeddingClient;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("should create embedding from text")
    void shouldCreateEmbeddingFromText() {
        final var text = "Test text for embedding";
        final var embedding = List.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        final var response = new EmbeddingClient.EmbeddingResponse(embedding);

        when(embeddingClient.apply(any(EmbeddingClient.EmbeddingRequest.class)))
                .thenReturn(Mono.just(response));

        StepVerifier.create(embeddingService.create(text))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(5, result.size());
                    assertEquals(1.0f, result.get(0));
                    assertEquals(2.0f, result.get(1));
                    assertEquals(3.0f, result.get(2));
                    assertEquals(4.0f, result.get(3));
                    assertEquals(5.0f, result.get(4));
                })
                .verifyComplete();

        verify(embeddingClient).apply(any(EmbeddingClient.EmbeddingRequest.class));
    }

    @Test
    @DisplayName("should create embedding from parts with main text and additional texts")
    void shouldCreateEmbeddingFromPartsWithAdditionalTexts() {
        final var mainText = "Main text";
        final var additionalTexts = List.of("Additional 1", "Additional 2", "Additional 3");
        final var embedding = List.of(1.0f, 2.0f, 3.0f);
        final var response = new EmbeddingClient.EmbeddingResponse(embedding);

        when(embeddingClient.apply(any(EmbeddingClient.EmbeddingRequest.class)))
                .thenReturn(Mono.just(response));

        StepVerifier.create(embeddingService.createFromParts(mainText, additionalTexts))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(3, result.size());
                    assertEquals(1.0f, result.get(0));
                    assertEquals(2.0f, result.get(1));
                    assertEquals(3.0f, result.get(2));
                })
                .verifyComplete();

        verify(embeddingClient).apply(any(EmbeddingClient.EmbeddingRequest.class));
    }

    @Test
    @DisplayName("should create embedding from parts with only main text")
    void shouldCreateEmbeddingFromPartsWithOnlyMainText() {
        final var mainText = "Only main text";
        final var additionalTexts = Collections.<String>emptyList();
        final var embedding = List.of(5.0f, 6.0f, 7.0f);
        final var response = new EmbeddingClient.EmbeddingResponse(embedding);

        when(embeddingClient.apply(any(EmbeddingClient.EmbeddingRequest.class)))
                .thenReturn(Mono.just(response));

        StepVerifier.create(embeddingService.createFromParts(mainText, additionalTexts))
                .assertNext(result -> {
                    assertNotNull(result);
                    assertEquals(3, result.size());
                    assertEquals(5.0f, result.get(0));
                    assertEquals(6.0f, result.get(1));
                    assertEquals(7.0f, result.get(2));
                })
                .verifyComplete();

        verify(embeddingClient).apply(any(EmbeddingClient.EmbeddingRequest.class));
    }
}
