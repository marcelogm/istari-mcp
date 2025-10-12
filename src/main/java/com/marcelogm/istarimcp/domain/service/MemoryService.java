package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.domain.converter.ToMemoryNode;
import com.marcelogm.istarimcp.domain.converter.ToMemoryResponse;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Singleton
public class MemoryService {

    private final MemoryRepository repository;
    private final ToMemoryNode toMemoryNode;
    private final ToMemoryResponse toMemoryResponse;

    public MemoryService(MemoryRepository repository, ToMemoryNode toMemoryNode, ToMemoryResponse toMemoryResponse) {
        this.repository = repository;
        this.toMemoryNode = toMemoryNode;
        this.toMemoryResponse = toMemoryResponse;
    }


    public Mono<CreateMemoryResponse> create(CreateMemoryRequest create) {
        return toMemoryNode.apply(create)
                .map(repository::createMemory)
                .map(created -> toMemoryResponse.apply(created, Collections.emptyList()));
    }

}
