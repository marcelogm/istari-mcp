package com.marcelogm.istarimcp.domain.model;

import java.util.List;
import java.util.UUID;

public record ObservationNode(
        UUID id,
        String observation,
        List<Float> embedding
) {
}
