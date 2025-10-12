package com.marcelogm.istarimcp.api.memory;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public record MemorySuggestionResponse(
        UUID id,
        String name,
        Float correlation
) {
}
