package com.marcelogm.istarimcp.domain.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;
import java.util.UUID;

@Introspected
public record MemoryNode (
        UUID id,
        String name,
        String description,
        List<ObservationNode> observations,
        List<Float> embedding
) {

}
