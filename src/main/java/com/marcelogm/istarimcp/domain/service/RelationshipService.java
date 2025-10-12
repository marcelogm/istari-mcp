package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.domain.model.RelationshipType;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.stream.Stream;

@Singleton
public class RelationshipService {

    public List<String> getAll() {
        return Stream.of(RelationshipType.values())
                .map(RelationshipType::getType)
                .toList();

    }

}
