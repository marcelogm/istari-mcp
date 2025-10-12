package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.IntegrationTest;
import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.marcelogm.istarimcp.helper.MemoryDatabaseAssertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MemoryControllerIT extends IntegrationTest {

    @Inject
    @Client("/")
    private HttpClient httpClient;

    @Test
    @DisplayName("should create memory with observations")
    void shouldCreateMemoryWithObservations() {
        // given
        final var request = new CreateMemoryRequest(
                "Test Memory",
                "Test Description",
                List.of("observation1", "observation2", "observation3")
        );

        // when
        final var httpRequest = HttpRequest.POST("/memories", request);
        final var response = httpClient.toBlocking().retrieve(
                httpRequest,
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        // then
        assertNotNull(response);
        final var memory = (CreateMemoryResponse) response.data();
        assertNotNull(memory);
        assertNotNull(memory.id());
        assertEquals("Test Memory", memory.name());
        assertEquals("Test Description", memory.description());

        assertMemoryInDatabase(driver, memory);
        assertObservationsByText(driver, request.observations());
    }

    @Test
    @DisplayName("should create memory without observations")
    void shouldCreateMemoryWithoutObservations() {
        // given
        final var request = new CreateMemoryRequest(
                "Empty Memory",
                "Memory without observations",
                List.of()
        );

        // when
        final var httpRequest = HttpRequest.POST("/memories", request);
        final var response = httpClient.toBlocking().retrieve(
                httpRequest,
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        // then
        assertNotNull(response);
        final var memory = (CreateMemoryResponse) response.data();
        assertNotNull(memory);
        assertNotNull(memory.id());
        assertEquals("Empty Memory", memory.name());
        assertEquals("Memory without observations", memory.description());

        assertMemoryInDatabase(driver, memory);
        assertNoObservations(driver, memory.id());
    }
}
