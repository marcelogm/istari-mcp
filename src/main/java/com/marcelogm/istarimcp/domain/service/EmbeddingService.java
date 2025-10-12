package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.client.EmbeddingClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import reactor.core.publisher.Mono;

import java.util.List;

@Singleton
public class EmbeddingService {

    @Inject
    public final EmbeddingClient embeddingClient;

    public EmbeddingService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public Mono<List<Float>> create(String text) {
        final var request = new EmbeddingClient.EmbeddingRequest("qwen3-embedding:latest", text);
        return embeddingClient.apply(request)
                .map(EmbeddingClient.EmbeddingResponse::embedding);
    }

}
