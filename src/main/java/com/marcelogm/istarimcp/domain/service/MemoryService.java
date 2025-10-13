package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.api.memory.UpdateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.UpdateMemoryResponse;
import com.marcelogm.istarimcp.domain.converter.ToCreateMemoryResponse;
import com.marcelogm.istarimcp.domain.converter.ToMemoryNode;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Singleton
public class MemoryService {

    private final MemoryRepository repository;
    private final ToMemoryNode toMemoryNode;
    private final ToCreateMemoryResponse toCreateMemoryResponse;
    private final MemoryQueryService queryService;
    private final EmbeddingService embeddingService;

    @Inject
    public MemoryService(
            MemoryRepository repository,
            ToMemoryNode toMemoryNode,
            ToCreateMemoryResponse toCreateMemoryResponse,
            MemoryQueryService queryService,
            EmbeddingService embeddingService
    ) {
        this.repository = repository;
        this.toMemoryNode = toMemoryNode;
        this.toCreateMemoryResponse = toCreateMemoryResponse;
        this.queryService = queryService;
        this.embeddingService = embeddingService;
    }

    public Mono<CreateMemoryResponse> create(CreateMemoryRequest create) {
        return toMemoryNode.apply(create)
                .flatMap(memoryNode -> {
                    var created = repository.createMemory(memoryNode);
                    var query = created.name() + ": " + created.description();

                    return queryService.summarySimilaritySearch(query)
                            .collectList()
                            .map(suggestions -> toCreateMemoryResponse.apply(created, suggestions));
                });
    }

    public Mono<Boolean> deleteByName(String name) {
        return Mono.fromCallable(() -> repository.deleteByName(name));
    }

    public Mono<UpdateMemoryResponse> updateMemory(UpdateMemoryRequest request) {
        return embeddingService.create(request.name() + ": " + request.description())
                .flatMap(embedding -> {
                    var updated = repository.updateMemory(request.name(), request.description(), embedding);

                    if (updated.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("Memory not found: " + request.name()));
                    }

                    var memory = updated.get();
                    return Mono.just(new UpdateMemoryResponse(
                            memory.id(),
                            memory.name(),
                            memory.description()
                    ));
                });
    }

}
