package com.marcelogm.istarimcp.domain.repository;

import com.marcelogm.istarimcp.IntegrationTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.marcelogm.istarimcp.helper.MemoryDatabaseAssertions.*;
import static com.marcelogm.istarimcp.helper.MemoryFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class MemoryRepositoryIT extends IntegrationTest {
    @Inject
    private MemoryRepository memoryRepository;

    @Test
    @DisplayName("should persist memory with observations")
    void shouldCreateMemoryWithObservations() {
        final var observation1 = observation("First observation", embedding(1.0f, 2.0f, 3.0f));
        final var observation2 = observation("Second observation", embedding(4.0f, 5.0f, 6.0f));
        final var memoryNode = memoryWithObservations(
                "Test Memory",
                "Test Description",
                embedding(7.0f, 8.0f, 9.0f),
                List.of(observation1, observation2)
        );

        final var result = memoryRepository.createMemory(memoryNode);

        assertNotNull(result);
        assertEquals(memoryNode.id(), result.id());
        assertEquals(memoryNode.name(), result.name());
        assertEquals(memoryNode.description(), result.description());

        assertMemoryInDatabase(driver, memoryNode);
        assertObservationsInDatabase(driver, memoryNode);
        assertRelationships(driver, memoryNode);
    }

    @Test
    @DisplayName("should persist memory without observations")
    void shouldCreateMemoryWithoutObservations() {
        final var memoryNode = memory("Empty Memory", "Memory without observations", embedding(1.0f, 2.0f, 3.0f));
        final var result = memoryRepository.createMemory(memoryNode);
        assertNotNull(result);
        assertMemoryInDatabase(driver, memoryNode);
        assertNoObservations(driver, memoryNode.id());
    }

    @Test
    @DisplayName("should find memory by name")
    void shouldFindMemoryByName() {
        // given
        final var observation = observation("test observation", embedding(1.0f, 2.0f, 3.0f));
        final var memoryNode = memoryWithObservations(
                "Unique Memory Name",
                "Test Description",
                embedding(4.0f, 5.0f, 6.0f),
                List.of(observation)
        );

        memoryRepository.createMemory(memoryNode);

        // when
        final var result = memoryRepository.findByName("Unique Memory Name");

        // then
        assertTrue(result.isPresent());
        assertEquals(memoryNode.id(), result.get().id());
        assertEquals("Unique Memory Name", result.get().name());
        assertEquals("Test Description", result.get().description());
        assertEquals(1, result.get().observations().size());
        assertEquals("test observation", result.get().observations().get(0).observation());
    }

    @Test
    @DisplayName("should return empty when memory name not found")
    void shouldReturnEmptyWhenMemoryNameNotFound() {
        // when
        final var result = memoryRepository.findByName("Non Existent Memory");

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should find relations by memory id")
    void shouldFindRelationsByMemoryId() {
        // given
        final var memory1 = memory("Parent Memory", "Parent Description", embedding(1.0f, 2.0f));
        final var memory2 = memory("Related Memory", "Related Description", embedding(3.0f, 4.0f));
        memoryRepository.createMemory(memory1);
        memoryRepository.createMemory(memory2);

        driver.executableQuery(
                "MATCH (m1:Memory {id: $id1}), (m2:Memory {id: $id2}) " +
                        "CREATE (m1)-[:RELATED_TO]->(m2)"
        ).withParameters(
                java.util.Map.of(
                        "id1", memory1.id().toString(),
                        "id2", memory2.id().toString()
                )
        ).execute();

        // when
        final var relations = memoryRepository.findRelationsByMemoryId(memory1.id());

        // then
        assertNotNull(relations);
        assertFalse(relations.isEmpty());
        assertEquals(1, relations.size());
        assertEquals("RELATED_TO", relations.get(0).getLeft());
        assertEquals(memory2.id(), relations.get(0).getRight().id());
        assertEquals("Related Memory", relations.get(0).getRight().name());
    }

    @Test
    @DisplayName("should return empty list when no relations found by id")
    void shouldReturnEmptyListWhenNoRelationsFoundById() {
        // given
        final var memory = memory("Isolated Memory", "No relations", embedding(1.0f));
        memoryRepository.createMemory(memory);

        // when
        final var relations = memoryRepository.findRelationsByMemoryId(memory.id());

        // then
        assertNotNull(relations);
        assertTrue(relations.isEmpty());
    }

    @Test
    @DisplayName("should find relations by memory name")
    void shouldFindRelationsByMemoryName() {
        // given
        final var memory1 = memory("Source Memory", "Source Description", embedding(1.0f, 2.0f));
        final var memory2 = memory("Target Memory", "Target Description", embedding(3.0f, 4.0f));
        memoryRepository.createMemory(memory1);
        memoryRepository.createMemory(memory2);

        driver.executableQuery(
                "MATCH (m1:Memory {name: $name1}), (m2:Memory {name: $name2}) " +
                        "CREATE (m1)-[:SIMILAR_TO]->(m2)"
        ).withParameters(
                java.util.Map.of(
                        "name1", "Source Memory",
                        "name2", "Target Memory"
                )
        ).execute();

        // when
        final var relations = memoryRepository.findRelationsByMemoryName("Source Memory");

        // then
        assertNotNull(relations);
        assertFalse(relations.isEmpty());
        assertEquals(1, relations.size());
        assertEquals("SIMILAR_TO", relations.get(0).getLeft());
        assertEquals(memory2.id(), relations.get(0).getRight().id());
        assertEquals("Target Memory", relations.get(0).getRight().name());
    }

    @Test
    @DisplayName("should return empty list when no relations found by name")
    void shouldReturnEmptyListWhenNoRelationsFoundByName() {
        // given
        final var memory = memory("Lonely Memory", "No connections", embedding(1.0f));
        memoryRepository.createMemory(memory);

        // when
        final var relations = memoryRepository.findRelationsByMemoryName("Lonely Memory");

        // then
        assertNotNull(relations);
        assertTrue(relations.isEmpty());
    }

    @Test
    @DisplayName("should delete memory by name")
    void shouldDeleteMemoryByName() {
        // given
        final var observation = observation("observation to delete", embedding(1.0f, 2.0f, 3.0f));
        final var memoryNode = memoryWithObservations(
                "Memory To Delete",
                "This will be deleted",
                embedding(4.0f, 5.0f, 6.0f),
                List.of(observation)
        );

        memoryRepository.createMemory(memoryNode);
        assertMemoryInDatabase(driver, memoryNode);

        // when
        final var result = memoryRepository.deleteByName("Memory To Delete");

        // then
        assertTrue(result);
        assertMemoryNotInDatabase(driver, memoryNode.id());
    }

    @Test
    @DisplayName("should return false when deleting non-existent memory")
    void shouldReturnFalseWhenDeletingNonExistentMemory() {
        // when
        final var result = memoryRepository.deleteByName("Non Existent Memory");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("should delete memory with observations")
    void shouldDeleteMemoryWithObservations() {
        // given
        final var observation1 = observation("First observation", embedding(1.0f, 2.0f, 3.0f));
        final var observation2 = observation("Second observation", embedding(4.0f, 5.0f, 6.0f));
        final var memoryNode = memoryWithObservations(
                "Memory With Observations",
                "Memory to delete with observations",
                embedding(7.0f, 8.0f, 9.0f),
                List.of(observation1, observation2)
        );

        memoryRepository.createMemory(memoryNode);
        assertMemoryInDatabase(driver, memoryNode);
        assertObservationsInDatabase(driver, memoryNode);

        // when
        final var result = memoryRepository.deleteByName("Memory With Observations");

        // then
        assertTrue(result);
        assertMemoryNotInDatabase(driver, memoryNode.id());
        assertNoObservations(driver, memoryNode.id());
    }

    @Test
    @DisplayName("should create relationship between memories")
    void shouldCreateRelationshipBetweenMemories() {
        // given
        final var memory1 = memory("Source Memory Rel", "Source Description", embedding(1.0f, 2.0f));
        final var memory2 = memory("Target Memory Rel", "Target Description", embedding(3.0f, 4.0f));
        memoryRepository.createMemory(memory1);
        memoryRepository.createMemory(memory2);

        // when
        final var result = memoryRepository.createRelationship(
                "Source Memory Rel",
                "Target Memory Rel",
                "RELATED_TO"
        );

        // then
        assertTrue(result);

        final var records = driver.executableQuery(
                        "MATCH (m1:Memory {name: $name1})-[r:RELATED_TO]->(m2:Memory {name: $name2}) RETURN r"
                )
                .withParameters(
                        java.util.Map.of(
                                "name1", "Source Memory Rel",
                                "name2", "Target Memory Rel"
                        )
                )
                .execute()
                .records();

        assertEquals(1, records.size());
    }

    @Test
    @DisplayName("should return false when creating relationship with non-existent source memory")
    void shouldReturnFalseWhenCreatingRelationshipWithNonExistentSourceMemory() {
        // given
        final var memory = memory("Target Memory Only", "Target Description", embedding(1.0f, 2.0f));
        memoryRepository.createMemory(memory);

        // when
        final var result = memoryRepository.createRelationship(
                "Non Existent Source",
                "Target Memory Only",
                "RELATED_TO"
        );

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("should return false when creating relationship with non-existent target memory")
    void shouldReturnFalseWhenCreatingRelationshipWithNonExistentTargetMemory() {
        // given
        final var memory = memory("Source Memory Only", "Source Description", embedding(1.0f, 2.0f));
        memoryRepository.createMemory(memory);

        // when
        final var result = memoryRepository.createRelationship(
                "Source Memory Only",
                "Non Existent Target",
                "RELATED_TO"
        );

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("should create different relationship types between memories")
    void shouldCreateDifferentRelationshipTypesBetweenMemories() {
        // given
        final var memory1 = memory("Memory A", "Description A", embedding(1.0f, 2.0f));
        final var memory2 = memory("Memory B", "Description B", embedding(3.0f, 4.0f));
        memoryRepository.createMemory(memory1);
        memoryRepository.createMemory(memory2);

        // when
        final var result1 = memoryRepository.createRelationship(
                "Memory A",
                "Memory B",
                "HAS"
        );

        final var result2 = memoryRepository.createRelationship(
                "Memory A",
                "Memory B",
                "USES"
        );

        // then
        assertTrue(result1);
        assertTrue(result2);

        final var records = driver.executableQuery(
                        "MATCH (m1:Memory {name: $name1})-[r]->(m2:Memory {name: $name2}) RETURN type(r) as relType"
                )
                .withParameters(
                        java.util.Map.of(
                                "name1", "Memory A",
                                "name2", "Memory B"
                        )
                )
                .execute()
                .records();

        assertEquals(2, records.size());
        final var relationshipTypes = records.stream()
                .map(record -> record.get("relType").asString())
                .toList();
        assertTrue(relationshipTypes.contains("HAS"));
        assertTrue(relationshipTypes.contains("USES"));
    }

    @Test
    @DisplayName("should update memory description and embedding")
    void shouldUpdateMemoryDescriptionAndEmbedding() {
        // given
        final var memory = memory("Update Memory", "Original Description", embedding(1.0f, 0.0f, 0.0f));
        memoryRepository.createMemory(memory);

        // when
        final var newEmbedding = embedding(0.5f, 0.5f, 0.0f);
        final var result = memoryRepository.updateMemory("Update Memory", "Updated Description", newEmbedding);

        // then
        assertTrue(result.isPresent());
        assertEquals(memory.id(), result.get().id());
        assertEquals("Update Memory", result.get().name());
        assertEquals("Updated Description", result.get().description());
        assertEquals(newEmbedding, result.get().embedding());

        final var records = driver.executableQuery(
                        "MATCH (m:Memory {name: $name}) RETURN m.description as description"
                )
                .withParameters(java.util.Map.of("name", "Update Memory"))
                .execute()
                .records();

        assertEquals("Updated Description", records.get(0).get("description").asString());
    }

    @Test
    @DisplayName("should return empty when updating non-existent memory")
    void shouldReturnEmptyWhenUpdatingNonExistentMemory() {
        // when
        final var result = memoryRepository.updateMemory(
                "Non Existent Memory",
                "New Description",
                embedding(1.0f, 0.0f, 0.0f)
        );

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should update memory while preserving observations")
    void shouldUpdateMemoryWhilePreservingObservations() {
        // given
        final var observation1 = observation("First obs", embedding(1.0f, 0.0f, 0.0f));
        final var observation2 = observation("Second obs", embedding(0.0f, 1.0f, 0.0f));
        final var memory = memoryWithObservations(
                "Memory With Obs",
                "Original Description",
                embedding(1.0f, 0.0f, 0.0f),
                List.of(observation1, observation2)
        );
        memoryRepository.createMemory(memory);

        // when
        final var result = memoryRepository.updateMemory(
                "Memory With Obs",
                "Updated Description",
                embedding(0.5f, 0.5f, 0.0f)
        );

        // then
        assertTrue(result.isPresent());
        assertEquals("Updated Description", result.get().description());
        assertEquals(2, result.get().observations().size());
    }
}
