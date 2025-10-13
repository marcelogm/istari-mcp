package com.marcelogm.istarimcp.api.memory;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record UpdateMemoryRequest(
        String name,
        String description
) {
}
