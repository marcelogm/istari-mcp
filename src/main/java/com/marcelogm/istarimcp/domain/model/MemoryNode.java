package com.marcelogm.istarimcp.domain.model;

import java.util.List;
import java.util.UUID;

public record MemoryNode (
        UUID id,
        String name,
        String description,
        List<ObservationNode> observations,
        List<Float> embedding
) {

}
