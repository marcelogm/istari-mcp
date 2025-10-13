package com.marcelogm.istarimcp.api.relationship;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record CreateRelationshipResponse(
        String sourceMemoryName,
        String targetMemoryName,
        String relationshipType
) {
}
