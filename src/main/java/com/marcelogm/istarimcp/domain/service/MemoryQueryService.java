package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.api.memory.MemoryResponse;
import com.marcelogm.istarimcp.api.memory.MemorySummaryResponse;
import com.marcelogm.istarimcp.domain.converter.ToMemoryResponse;
import com.marcelogm.istarimcp.domain.converter.ToMemorySummaryResponse;
import com.marcelogm.istarimcp.domain.repository.MemoryEmbeddingRepository;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
public class MemoryQueryService {

    private final MemoryRepository memoryRepository;
    private final MemoryEmbeddingRepository memoryEmbeddingRepository;
    private final EmbeddingService embeddingService;
    private final RelationshipService relationshipService;
    private final ToMemoryResponse toMemoryResponse;
    private final ToMemorySummaryResponse toMemorySummaryResponse;

    @Inject
    public MemoryQueryService(MemoryRepository memoryRepository,
                              MemoryEmbeddingRepository memoryEmbeddingRepository,
                              EmbeddingService embeddingService,
                              RelationshipService relationshipService,
                              ToMemoryResponse toMemoryResponse,
                              ToMemorySummaryResponse toMemorySummaryResponse) {
        this.memoryRepository = memoryRepository;
        this.memoryEmbeddingRepository = memoryEmbeddingRepository;
        this.embeddingService = embeddingService;
        this.relationshipService = relationshipService;
        this.toMemoryResponse = toMemoryResponse;
        this.toMemorySummaryResponse = toMemorySummaryResponse;
    }

    public Mono<MemoryResponse> findByName(String name) {
        return Mono.justOrEmpty(
                memoryRepository.findByName(name).map(memoryNode -> {
                    var relations = relationshipService.buildMemoryRelationsByName(name);
                    return toMemoryResponse.apply(memoryNode, relations, null);
                })
        );
    }

    public Flux<MemoryResponse> similaritySearch(String query) {
        return embeddingService.create(query)
                .flatMapMany(embedding -> Flux.fromIterable(memoryEmbeddingRepository.search(embedding)))
                .map(result -> {
                    var memory = result.getLeft();
                    var score = result.getRight();
                    var relations = relationshipService.buildMemoryRelationsById(memory.id());

                    return toMemoryResponse.apply(memory, relations, score);
                });
    }

    public Flux<MemorySummaryResponse> summarySimilaritySearch(String query) {
        return embeddingService.create(query)
                .flatMapMany(embedding -> Flux.fromIterable(memoryEmbeddingRepository.searchWithoutObservations(embedding)))
                .map(result -> toMemorySummaryResponse.apply(result.getLeft(), result.getRight()));
    }

}
