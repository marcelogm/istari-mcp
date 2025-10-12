package com.marcelogm.istarimcp.domain.converter;

import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.api.memory.MemorySuggestionResponse;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.function.BiFunction;

@Singleton
public class ToMemoryResponse implements BiFunction<MemoryNode, List<MemorySuggestionResponse>, CreateMemoryResponse> {

    @Override
    public CreateMemoryResponse apply(MemoryNode memoryNode, List<MemorySuggestionResponse> memorySuggestionResponses) {
        return new CreateMemoryResponse(
                memoryNode.id(),
                memoryNode.name(),
                memoryNode.description(),
                memorySuggestionResponses
        );
    }
}
