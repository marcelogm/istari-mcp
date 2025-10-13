package com.marcelogm.istarimcp.domain.repository;

import com.marcelogm.istarimcp.IntegrationTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.marcelogm.istarimcp.helper.MemoryFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationRepositoryIT extends IntegrationTest {

    @Inject
    private ObservationRepository observationRepository;

    @Inject
    private MemoryRepository memoryRepository;

    @Test
    @DisplayName("should add observation to existing memory")
    void shouldAddObservationToExistingMemory() {
        final var memory = memory("Test Memory", "Test Description", embedding(1.0f, 0.0f, 0.0f));
        memoryRepository.createMemory(memory);

        final var observation = observation("New observation", embedding(0.5f, 0.5f, 0.0f));
        final var result = observationRepository.addObservationToMemory("Test Memory", observation);

        assertTrue(result.isPresent());
        assertEquals(observation.id(), result.get().id());
        assertEquals("New observation", result.get().observation());

        final var records = driver.executableQuery(
                        "MATCH (m:Memory {name: $name})-[:HAS_OBSERVATION]->(o:Observation {id: $id}) RETURN o"
                )
                .withParameters(
                        java.util.Map.of(
                                "name", "Test Memory",
                                "id", observation.id().toString()
                        )
                )
                .execute()
                .records();

        assertEquals(1, records.size());
    }

    @Test
    @DisplayName("should return empty when memory does not exist")
    void shouldReturnEmptyWhenMemoryDoesNotExist() {
        final var observation = observation("New observation", embedding(0.5f, 0.5f, 0.0f));
        final var result = observationRepository.addObservationToMemory("Non Existent Memory", observation);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should add multiple observations to same memory")
    void shouldAddMultipleObservationsToSameMemory() {
        final var memory = memory("Multi Obs Memory", "Test Description", embedding(1.0f, 0.0f, 0.0f));
        memoryRepository.createMemory(memory);

        final var observation1 = observation("First observation", embedding(0.5f, 0.5f, 0.0f));
        final var observation2 = observation("Second observation", embedding(0.6f, 0.4f, 0.0f));

        final var result1 = observationRepository.addObservationToMemory("Multi Obs Memory", observation1);
        final var result2 = observationRepository.addObservationToMemory("Multi Obs Memory", observation2);

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());

        final var records = driver.executableQuery(
                        "MATCH (m:Memory {name: $name})-[:HAS_OBSERVATION]->(o:Observation) RETURN count(o) as count"
                )
                .withParameters(java.util.Map.of("name", "Multi Obs Memory"))
                .execute()
                .records();

        assertEquals(2, records.get(0).get("count").asInt());
    }
}
