package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.api.memory.MemoryRelation;
import com.marcelogm.istarimcp.api.relationship.CreateRelationshipRequest;
import com.marcelogm.istarimcp.api.relationship.CreateRelationshipResponse;
import com.marcelogm.istarimcp.domain.converter.ToMemorySummaryResponse;
import com.marcelogm.istarimcp.domain.model.RelationshipType;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Singleton
public class RelationshipService {

    private final MemoryRepository repository;
    private final ToMemorySummaryResponse toMemorySummaryResponse;

    @Inject
    public RelationshipService(
            MemoryRepository repository,
            ToMemorySummaryResponse toMemorySummaryResponse
    ) {
        this.repository = repository;
        this.toMemorySummaryResponse = toMemorySummaryResponse;
    }

    public List<String> getAll() {
        return Stream.of(RelationshipType.values())
                .map(RelationshipType::getType)
                .toList();
    }

    public List<MemoryRelation> buildMemoryRelationsByName(String memoryName) {
        return repository.findRelationsByMemoryName(memoryName).stream()
                .map(pair -> new MemoryRelation(
                        RelationshipType.fromString(pair.getLeft()),
                        toMemorySummaryResponse.apply(pair.getRight(), null)
                ))
                .toList();
    }

    public List<MemoryRelation> buildMemoryRelationsById(UUID memoryId) {
        return repository.findRelationsByMemoryId(memoryId).stream()
                .map(pair -> new MemoryRelation(
                        RelationshipType.fromString(pair.getLeft()),
                        toMemorySummaryResponse.apply(pair.getRight(), null)
                ))
                .toList();
    }

    public CreateRelationshipResponse createRelationship(CreateRelationshipRequest request) {
        RelationshipType.fromString(request.relationshipType());

        boolean created = repository.createRelationship(
                request.sourceMemoryName(),
                request.targetMemoryName(),
                request.relationshipType()
        );

        if (!created) {
            throw new IllegalArgumentException("Failed to create relationship. Check if both memories exist.");
        }

        return new CreateRelationshipResponse(
                request.sourceMemoryName(),
                request.targetMemoryName(),
                request.relationshipType()
        );
    }

}
