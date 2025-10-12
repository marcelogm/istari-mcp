package com.marcelogm.istarimcp.domain.repository;

import com.marcelogm.istarimcp.IntegrationTest;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.marcelogm.istarimcp.helper.MemoryDatabaseAssertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MemoryRepositoryIT extends IntegrationTest {
    @Inject
    private MemoryRepository memoryRepository;

    @Test
    @DisplayName("should persist memory with observations")
    void shouldCreateMemoryWithObservations() {
        // given
        final var observation1 = new ObservationNode(
                UUID.randomUUID(),
                "First observation",
                List.of(1.0f, 2.0f, 3.0f)
        );

        final var observation2 = new ObservationNode(
                UUID.randomUUID(),
                "Second observation",
                List.of(4.0f, 5.0f, 6.0f)
        );

        final var memoryNode = new MemoryNode(
                UUID.randomUUID(),
                "Test Memory",
                "Test Description",
                List.of(observation1, observation2),
                List.of(7.0f, 8.0f, 9.0f)
        );

        // when
        final var result = memoryRepository.createMemory(memoryNode);

        // then
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
        // given
        final var memoryNode = new MemoryNode(
                UUID.randomUUID(),
                "Empty Memory",
                "Memory without observations",
                List.of(),
                List.of(1.0f, 2.0f, 3.0f)
        );

        // when
        final var result = memoryRepository.createMemory(memoryNode);

        // then
        assertNotNull(result);
        assertMemoryInDatabase(driver, memoryNode);
        assertNoObservations(driver, memoryNode.id());
    }
}
