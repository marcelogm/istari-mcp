package com.marcelogm.istarimcp.domain.converter;

import com.marcelogm.istarimcp.api.memory.MemoryRelation;
import com.marcelogm.istarimcp.api.memory.MemoryResponse;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class ToMemoryResponse {

    public MemoryResponse apply(MemoryNode memoryNode, List<MemoryRelation> relations, Float score) {
        return new MemoryResponse(
                memoryNode.id(),
                memoryNode.name(),
                memoryNode.description(),
                memoryNode.observations().stream().map(ObservationNode::observation).toList(),
                relations,
                Optional.ofNullable(score)
        );
    }
}
