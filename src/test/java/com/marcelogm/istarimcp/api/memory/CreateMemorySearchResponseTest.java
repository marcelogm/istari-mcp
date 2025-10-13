package com.marcelogm.istarimcp.api.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CreateMemorySearchResponseTest {

    @Test
    @DisplayName("should create response with all fields")
    void shouldCreateResponseWithAllFields() {
        final var id = UUID.randomUUID();
        final var suggestions = List.of(
                new MemorySummaryResponse(UUID.randomUUID(), "Suggestion 1", "", Optional.of(0.9f)),
                new MemorySummaryResponse(UUID.randomUUID(), "Suggestion 2", "", Optional.of(0.8f))
        );

        final var response = new CreateMemoryResponse(
                id,
                "Test Memory",
                "Test Description",
                suggestions
        );

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals("Test Memory", response.name());
        assertEquals("Test Description", response.description());
        assertEquals(2, response.suggestions().size());
        assertEquals("Suggestion 1", response.suggestions().get(0).name());
        assertEquals(0.9f, response.suggestions().get(0).score().get());
    }
}
