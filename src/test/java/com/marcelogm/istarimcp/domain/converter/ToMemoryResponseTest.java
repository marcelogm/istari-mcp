package com.marcelogm.istarimcp.domain.converter;

import com.marcelogm.istarimcp.api.memory.MemoryRelation;
import com.marcelogm.istarimcp.api.memory.MemorySummaryResponse;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.model.RelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

class ToMemoryResponseTest {

    private ToMemoryResponse toMemoryResponse;

    @BeforeEach
    void setUp() {
        toMemoryResponse = new ToMemoryResponse();
    }

    @Test
    @DisplayName("should convert memory node with observations and relations to response")
    void shouldConvertMemoryNodeWithObservationsAndRelations() {
        // given
        final var memoryId = randomUUID();
        final var observation1 = new ObservationNode(
                randomUUID(),
                "first observation",
                List.of(1.0f, 2.0f, 3.0f));
        final var observation2 = new ObservationNode(
                randomUUID(),
                "second observation",
                List.of(4.0f, 5.0f, 6.0f));

        final var memoryNode = new MemoryNode(
                memoryId,
                "Test Memory",
                "Test Description",
                List.of(observation1, observation2),
                List.of(7.0f, 8.0f, 9.0f));

        final var relations = List.of(
                new MemoryRelation(
                        RelationshipType.RELATED_TO,
                        new MemorySummaryResponse(randomUUID(), "Related Memory", "Description", of(0.9f))));

        // when
        final var result = toMemoryResponse.apply(memoryNode, relations, 0.95f);

        // then
        assertNotNull(result);
        assertEquals(memoryId, result.id());
        assertEquals("Test Memory", result.name());
        assertEquals("Test Description", result.description());
        assertEquals(2, result.observations().size());
        assertEquals("first observation", result.observations().get(0));
        assertEquals("second observation", result.observations().get(1));
        assertEquals(1, result.relations().size());
        assertEquals(RelationshipType.RELATED_TO, result.relations().get(0).type());
        assertTrue(result.score().isPresent());
        assertEquals(0.95f, result.score().get());
    }

    @Test
    @DisplayName("should convert memory node without observations")
    void shouldConvertMemoryNodeWithoutObservations() {
        // given
        final var memoryId = randomUUID();
        final var memoryNode = new MemoryNode(
                memoryId,
                "Empty Memory",
                "Memory without observations",
                Collections.emptyList(),
                List.of(1.0f, 2.0f, 3.0f));

        // when
        final var result = toMemoryResponse.apply(memoryNode, Collections.emptyList(), null);

        // then
        assertNotNull(result);
        assertEquals(memoryId, result.id());
        assertEquals("Empty Memory", result.name());
        assertEquals("Memory without observations", result.description());
        assertTrue(result.observations().isEmpty());
        assertTrue(result.relations().isEmpty());
        assertTrue(result.score().isEmpty());
    }

    @Test
    @DisplayName("should convert memory node without relations")
    void shouldConvertMemoryNodeWithoutRelations() {
        // given
        final var memoryNode = new MemoryNode(
                randomUUID(),
                "Isolated Memory",
                "Memory without relations",
                List.of(new ObservationNode(randomUUID(), "observation", List.of(1.0f))),
                List.of(2.0f, 3.0f));

        // when
        final var result = toMemoryResponse.apply(memoryNode, Collections.emptyList(), 0.8f);

        // then
        assertNotNull(result);
        assertEquals("Isolated Memory", result.name());
        assertEquals(1, result.observations().size());
        assertTrue(result.relations().isEmpty());
        assertEquals(0.8f, result.score().get());
    }

    @Test
    @DisplayName("should convert memory node without score")
    void shouldConvertMemoryNodeWithoutScore() {
        // given
        final var memoryNode = new MemoryNode(
                randomUUID(),
                "Test Memory",
                "Test Description",
                List.of(new ObservationNode(randomUUID(), "observation", List.of(1.0f))),
                List.of(2.0f));

        // when
        final var result = toMemoryResponse.apply(memoryNode, Collections.emptyList(), null);

        // then
        assertNotNull(result);
        assertEquals("Test Memory", result.name());
        assertTrue(result.score().isEmpty());
    }

    @Test
    @DisplayName("should extract observation texts from observation nodes")
    void shouldExtractObservationTexts() {
        // given
        final var memoryNode = new MemoryNode(
                randomUUID(),
                "Test Memory",
                "Test Description",
                List.of(
                        new ObservationNode(randomUUID(), "obs1", List.of(1.0f)),
                        new ObservationNode(randomUUID(), "obs2", List.of(2.0f)),
                        new ObservationNode(randomUUID(), "obs3", List.of(3.0f))),
                List.of(4.0f));

        // when
        final var result = toMemoryResponse.apply(memoryNode, Collections.emptyList(), null);

        // then
        assertNotNull(result);
        assertEquals(3, result.observations().size());
        assertEquals("obs1", result.observations().get(0));
        assertEquals("obs2", result.observations().get(1));
        assertEquals("obs3", result.observations().get(2));
    }

    @Test
    @DisplayName("should maintain relation order")
    void shouldMaintainRelationOrder() {
        // given
        final var memoryNode = new MemoryNode(
                randomUUID(),
                "Test Memory",
                "Test Description",
                Collections.emptyList(),
                List.of(1.0f));

        final var relations = List.of(
                new MemoryRelation(
                        RelationshipType.RELATED_TO,
                        new MemorySummaryResponse(randomUUID(), "First", "", of(0.9f))),
                new MemoryRelation(
                        RelationshipType.SIMILAR_TO,
                        new MemorySummaryResponse(randomUUID(), "Second", "", of(0.8f))),
                new MemoryRelation(
                        RelationshipType.PART_OF,
                        new MemorySummaryResponse(randomUUID(), "Third", "", of(0.7f))));

        // when
        final var result = toMemoryResponse.apply(memoryNode, relations, null);

        // then
        assertNotNull(result);
        assertEquals(3, result.relations().size());
        assertEquals(RelationshipType.RELATED_TO, result.relations().get(0).type());
        assertEquals(RelationshipType.SIMILAR_TO, result.relations().get(1).type());
        assertEquals(RelationshipType.PART_OF, result.relations().get(2).type());
    }
}
