package com.marcelogm.istarimcp.domain.converter;

import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.service.EmbeddingService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.Optional.ofNullable;

@Singleton
public class ToMemoryNode implements Function<CreateMemoryRequest, MemoryNode> {

    @Inject
    private final EmbeddingService embeddingService;

    public ToMemoryNode(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public MemoryNode apply(CreateMemoryRequest create) {
        final var observationTexts = ofNullable(create.observations())
                .orElse(Collections.emptyList());
        final var memoryEmbeddingFuture = embeddingService.create(create.name() + ": " + create.description());
        final var observationFutures = observationTexts
                .stream()
                .map(embeddingService::create)
                .toList();

        CompletableFuture.allOf(
                observationFutures.toArray(CompletableFuture[]::new)
        ).join();

        final var observations = IntStream.range(0, observationTexts.size())
                .mapToObj(i -> new ObservationNode(
                        UUID.randomUUID(),
                        observationTexts.get(i),
                        observationFutures.get(i).join()))
                .toList();

        return new MemoryNode(
                UUID.randomUUID(),
                create.name(),
                create.description(),
                observations,
                memoryEmbeddingFuture.join());
    }
}
