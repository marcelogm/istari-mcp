package com.marcelogm.istarimcp.api.memory;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Serdeable
@Introspected
public record MemoryResponse(
        UUID id,
        String name,
        String description,
        List<String> observations,
        List<MemoryRelation> relations,
        Optional<Float> score
) {
}
