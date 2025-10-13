package com.marcelogm.istarimcp.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record GenericResponse(
        String context
) {
}
