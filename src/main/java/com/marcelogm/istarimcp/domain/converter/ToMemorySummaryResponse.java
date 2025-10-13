package com.marcelogm.istarimcp.domain.converter;

import com.marcelogm.istarimcp.api.memory.MemorySummaryResponse;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.function.BiFunction;

@Singleton
public class ToMemorySummaryResponse implements BiFunction<MemoryNode, Float, MemorySummaryResponse> {

    @Override
    public MemorySummaryResponse apply(MemoryNode memoryNode, Float score) {
        return new MemorySummaryResponse(
                memoryNode.id(),
                memoryNode.name(),
                memoryNode.description(),
                Optional.ofNullable(score)
        );
    }
}
