package com.marcelogm.istarimcp.api.relationship;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record CreateRelationshipRequest(
        String sourceMemoryName,
        String targetMemoryName,
        String relationshipType
) {
}
