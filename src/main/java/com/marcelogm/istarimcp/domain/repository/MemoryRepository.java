package com.marcelogm.istarimcp.domain.repository;

import com.marcelogm.istarimcp.domain.model.MemoryNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.neo4j.driver.Driver;

import java.util.Map;

@Singleton
public class MemoryRepository {

    @Inject
    private Driver driver;

    public MemoryNode createMemory(MemoryNode memoryNode) {
        final var observations = memoryNode.observations().stream()
                .map(obs -> Map.of(
                        "id", obs.id().toString(),
                        "observation", obs.observation(),
                        "embedding", obs.embedding()
                ))
                .toList();

        driver.executableQuery("""
                        CREATE (m:Memory {
                            id: $id,
                            name: $name,
                            description: $description,
                            embedding: $embedding
                        })
                        WITH m
                        UNWIND $observations AS obs
                        CREATE (o:Observation {
                            id: obs.id,
                            observation: obs.observation,
                            embedding: obs.embedding
                        })
                        CREATE (m)-[:HAS_OBSERVATION]->(o)
                        """)
                .withParameters(Map.of(
                        "id", memoryNode.id().toString(),
                        "name", memoryNode.name(),
                        "description", memoryNode.description(),
                        "embedding", memoryNode.embedding(),
                        "observations", observations
                )).execute();

        return memoryNode;
    }

}
