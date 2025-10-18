package com.marcelogm.istarimcp.domain.converter;

import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

class ToMemorySummaryResponseTest {

    private ToMemorySummaryResponse toMemorySummaryResponse;

    @BeforeEach
    void setUp() {
        toMemorySummaryResponse = new ToMemorySummaryResponse();
    }

    @Test
    @DisplayName("should convert memory node with score to summary response")
    void shouldConvertMemoryNodeWithScore() {
        // given
        final var memoryId = randomUUID();
        final var memoryNode = new MemoryNode(
                memoryId,
                "Test Memory",
                "Test Description",
                List.of(new ObservationNode(randomUUID(), "observation", List.of(1.0f))),
                List.of(2.0f, 3.0f));

        // when
        final var result = toMemorySummaryResponse.apply(memoryNode, 0.95f);

        // then
        assertNotNull(result);
        assertEquals(memoryId, result.id());
        assertEquals("Test Memory", result.name());
        assertEquals("Test Description", result.description());
        assertTrue(result.score().isPresent());
        assertEquals(0.95f, result.score().get());
    }

    @Test
    @DisplayName("should convert memory node without score to summary response")
    void shouldConvertMemoryNodeWithoutScore() {
        // given
        final var memoryId = randomUUID();
        final var memoryNode = new MemoryNode(
                memoryId,
                "Test Memory",
                "Test Description",
                Collections.emptyList(),
                List.of(1.0f, 2.0f));

        // when
        final var result = toMemorySummaryResponse.apply(memoryNode, null);

        // then
        assertNotNull(result);
        assertEquals(memoryId, result.id());
        assertEquals("Test Memory", result.name());
        assertEquals("Test Description", result.description());
        assertTrue(result.score().isEmpty());
    }

    @Test
    @DisplayName("should handle high similarity score")
    void shouldHandleHighSimilarityScore() {
        // given
        final var memoryNode = new MemoryNode(
                randomUUID(),
                "Very Similar Memory",
                "Almost identical",
                Collections.emptyList(),
                List.of(1.0f));

        // when
        final var result = toMemorySummaryResponse.apply(memoryNode, 0.99f);

        // then
        assertNotNull(result);
        assertEquals("Very Similar Memory", result.name());
        assertEquals("Almost identical", result.description());
        assertEquals(0.99f, result.score().get());
    }
}
