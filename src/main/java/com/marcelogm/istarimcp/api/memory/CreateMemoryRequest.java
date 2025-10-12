package com.marcelogm.istarimcp.api.memory;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public record CreateMemoryRequest(
        String name,
        String description,
        List<String> observations
) {
}
