package com.marcelogm.istarimcp.api.observation;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record CreateObservationRequest(
        String memoryName,
        String observation
) {
}
