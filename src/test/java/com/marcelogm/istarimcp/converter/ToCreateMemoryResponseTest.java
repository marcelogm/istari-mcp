package com.marcelogm.istarimcp.converter;

import com.marcelogm.istarimcp.api.memory.MemorySummaryResponse;
import com.marcelogm.istarimcp.domain.converter.ToCreateMemoryResponse;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

class ToCreateMemoryResponseTest {

    private ToCreateMemoryResponse toCreateMemoryResponse;

    @BeforeEach
    void setUp() {
        toCreateMemoryResponse = new ToCreateMemoryResponse();
    }

    @Test
    @DisplayName("should convert memory node with suggestions to response")
    void shouldConvertMemoryNodeWithSuggestions() {
        // given
        final var memoryId = randomUUID();
        final var observation = new ObservationNode(
                randomUUID(),
                "test observation",
                List.of(1.0f, 2.0f, 3.0f)
        );

        final var memoryNode = new MemoryNode(
                memoryId,
                "Test Memory",
                "Test Description",
                List.of(observation),
                List.of(4.0f, 5.0f, 6.0f)
        );

        final var suggestions = List.of(
                new MemorySummaryResponse(randomUUID(), "Similar Memory 1", "Description 1", of(0.95f)),
                new MemorySummaryResponse(randomUUID(), "Similar Memory 2", "Description 2", of(0.85f))
        );

        // when
        final var result = toCreateMemoryResponse.apply(memoryNode, suggestions);

        // then
        assertNotNull(result);
        assertEquals(memoryId, result.id());
        assertEquals("Test Memory", result.name());
        assertEquals("Test Description", result.description());
        assertEquals(2, result.suggestions().size());
        assertEquals("Similar Memory 1", result.suggestions().get(0).name());
        assertEquals("Description 1", result.suggestions().get(0).description());
        assertEquals(0.95f, result.suggestions().get(0).score().get());
        assertEquals("Similar Memory 2", result.suggestions().get(1).name());
        assertEquals("Description 2", result.suggestions().get(1).description());
        assertEquals(0.85f, result.suggestions().get(1).score().get());
    }

    @Test
    @DisplayName("should convert memory node without suggestions to response")
    void shouldConvertMemoryNodeWithoutSuggestions() {
        // given
        final var memoryId = randomUUID();
        final var memoryNode = new MemoryNode(
                memoryId,
                "Empty Memory",
                "Memory without suggestions",
                Collections.emptyList(),
                List.of(1.0f, 2.0f, 3.0f)
        );

        // when
        final var result = toCreateMemoryResponse.apply(memoryNode, Collections.emptyList());

        // then
        assertNotNull(result);
        assertEquals(memoryId, result.id());
        assertEquals("Empty Memory", result.name());
        assertEquals("Memory without suggestions", result.description());
        assertTrue(result.suggestions().isEmpty());
    }

    @Test
    @DisplayName("should maintain suggestion order")
    void shouldMaintainSuggestionOrder() {
        // given
        final var memoryNode = new MemoryNode(
                randomUUID(),
                "Test Memory",
                "Test Description",
                Collections.emptyList(),
                List.of(1.0f)
        );

        final var suggestions = List.of(
                new MemorySummaryResponse(randomUUID(), "First", "", of(0.9f)),
                new MemorySummaryResponse(randomUUID(), "Second", "", of(0.8f)),
                new MemorySummaryResponse(randomUUID(), "Third", "", of(0.7f))
        );

        // when
        final var result = toCreateMemoryResponse.apply(memoryNode, suggestions);

        // then
        assertNotNull(result);
        assertEquals(3, result.suggestions().size());
        assertEquals("First", result.suggestions().get(0).name());
        assertEquals("Second", result.suggestions().get(1).name());
        assertEquals("Third", result.suggestions().get(2).name());
    }
}
