package com.marcelogm.istarimcp.api.memory;

import com.marcelogm.istarimcp.domain.model.RelationshipType;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record MemoryRelation(
        RelationshipType type,
        MemorySummaryResponse memory
) {
}
