package com.marcelogm.istarimcp.helper;

import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import org.neo4j.driver.Driver;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MemoryDatabaseAssertions {

    private MemoryDatabaseAssertions() {
    }

    public static void assertMemoryInDatabase(Driver driver, UUID id, String name, String description) {
        final var records = driver.executableQuery(
                        "MATCH (m:Memory {id: $id}) RETURN m"
                )
                .withParameters(Map.of("id", id.toString()))
                .execute()
                .records();

        assertEquals(1, records.size());

        final var node = records.get(0).get("m").asNode();
        assertEquals(name, node.get("name").asString());
        assertEquals(description, node.get("description").asString());
    }

    public static void assertMemoryInDatabase(Driver driver, MemoryNode memoryNode) {
        assertMemoryInDatabase(driver, memoryNode.id(), memoryNode.name(), memoryNode.description());
    }

    public static void assertMemoryInDatabase(Driver driver, CreateMemoryResponse memory) {
        assertMemoryInDatabase(driver, memory.id(), memory.name(), memory.description());
    }

    public static void assertObservationsByText(Driver driver, List<String> observations) {
        for (var observation : observations) {
            final var records = driver.executableQuery(
                            "MATCH (o:Observation {observation: $observation}) RETURN o"
                    )
                    .withParameters(Map.of("observation", observation))
                    .execute()
                    .records();

            assertTrue(records.size() >= 1, "Observation '" + observation + "' not found in database");
        }
    }

    public static void assertObservationsInDatabase(Driver driver, MemoryNode memoryNode) {
        for (var observation : memoryNode.observations()) {
            final var records = driver.executableQuery(
                            "MATCH (o:Observation {id: $id}) RETURN o"
                    )
                    .withParameters(Map.of("id", observation.id().toString()))
                    .execute()
                    .records();
            assertEquals(1, records.size());

            final var node = records.get(0).get("o").asNode();
            assertEquals(observation.observation(), node.get("observation").asString());
        }
    }

    public static void assertNoObservations(Driver driver, UUID memoryId) {
        final var observationCount = driver.executableQuery(
                        "MATCH (m:Memory {id: $id})-[:HAS_OBSERVATION]->(o:Observation) RETURN count(o) as count"
                )
                .withParameters(Map.of("id", memoryId.toString()))
                .execute()
                .records()
                .get(0)
                .get("count")
                .asInt();

        assertEquals(0, observationCount);
    }

    public static void assertRelationships(Driver driver, MemoryNode memoryNode) {
        final var relationshipCount = driver.executableQuery(
                        "MATCH (m:Memory {id: $id})-[r:HAS_OBSERVATION]->(o:Observation) RETURN count(r) as count"
                )
                .withParameters(Map.of("id", memoryNode.id().toString()))
                .execute()
                .records()
                .get(0)
                .get("count")
                .asInt();

        assertEquals(memoryNode.observations().size(), relationshipCount);
    }
}
