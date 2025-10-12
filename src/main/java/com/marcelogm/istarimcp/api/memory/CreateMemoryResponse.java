package com.marcelogm.istarimcp.api.memory;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.UUID;

@Serdeable
public record CreateMemoryResponse(
        UUID id,
        String name,
        String description,
        List<MemorySuggestionResponse> suggestions
) {
}
