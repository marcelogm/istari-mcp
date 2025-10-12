package com.marcelogm.istarimcp.domain.converter;

import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.service.EmbeddingService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@Singleton
public class ToMemoryNode {
    @Inject
    private final EmbeddingService embeddingService;

    public ToMemoryNode(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public Mono<MemoryNode> apply(CreateMemoryRequest create) {
        final var observationTexts = ofNullable(create.observations())
                .orElse(Collections.emptyList());
        final var memoryEmbeddingMono = embeddingService.create(create.name() + ": " + create.description());
        final var observationsMono = Flux.fromIterable(observationTexts)
                .flatMap(text -> embeddingService.create(text)
                        .map(embedding -> new ObservationNode(
                                UUID.randomUUID(),
                                text,
                                embedding)))
                .collectList();

        return Mono.zip(observationsMono, memoryEmbeddingMono)
                .map(tuple -> new MemoryNode(
                        UUID.randomUUID(),
                        create.name(),
                        create.description(),
                        tuple.getT1(),
                        tuple.getT2()));
    }
}
