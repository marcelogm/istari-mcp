package com.marcelogm.istarimcp.api.observation;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
@Introspected
public record CreateObservationResponse(
        UUID id,
        String memoryName,
        String observation
) {
}
