package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.domain.converter.ToMemoryNode;
import com.marcelogm.istarimcp.domain.converter.ToMemoryResponse;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import jakarta.inject.Singleton;

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


    public CreateMemoryResponse create(CreateMemoryRequest create) {
        final var node = toMemoryNode.apply(create);
        final var created = repository.createMemory(node);

        // TODO: find correlations to that memory using embbeding
        // TODO: rerank memories correlations
        // TODO: return to LLM as suggestions

        return toMemoryResponse.apply(created, Collections.emptyList());
    }

}
