package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.client.EmbeddingClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class EmbeddingService {

    public final EmbeddingClient embeddingClient;

    @Inject
    public EmbeddingService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public Mono<List<Float>> create(String text) {
        final var request = new EmbeddingClient.EmbeddingRequest("qwen3-embedding:latest", text);
        return embeddingClient.apply(request)
                .map(EmbeddingClient.EmbeddingResponse::embedding);
    }

    public Mono<List<Float>> createFromParts(String mainText, List<String> additionalTexts) {
        final var compositeText = buildCompositeText(mainText, additionalTexts);
        return create(compositeText);
    }

    private String buildCompositeText(String mainText, List<String> additionalTexts) {
        if (additionalTexts == null || additionalTexts.isEmpty()) {
            return mainText;
        }

        final var additionalContent = additionalTexts.stream()
                .collect(Collectors.joining(" "));

        return mainText + " " + additionalContent;
    }

}
