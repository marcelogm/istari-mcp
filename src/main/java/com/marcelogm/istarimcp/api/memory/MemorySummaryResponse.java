package com.marcelogm.istarimcp.api.memory;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.Optional;
import java.util.UUID;

@Serdeable
@Introspected
public record MemorySummaryResponse(
        UUID id,
        String name,
        String description,
        Optional<Float> score
) {
}
