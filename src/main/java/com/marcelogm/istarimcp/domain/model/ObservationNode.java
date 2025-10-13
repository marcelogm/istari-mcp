package com.marcelogm.istarimcp.domain.model;

import io.micronaut.core.annotation.Introspected;

import java.util.List;
import java.util.UUID;

@Introspected
public record ObservationNode(
        UUID id,
        String observation,
        List<Float> embedding
) {
}
