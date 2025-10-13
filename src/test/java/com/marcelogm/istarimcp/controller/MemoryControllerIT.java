package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.IntegrationTest;
import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.api.memory.UpdateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.UpdateMemoryResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.marcelogm.istarimcp.helper.MemoryDatabaseAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals("Memory created.", response.context());

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
                Collections.emptyList()
        );

        // when
        final var httpRequest = HttpRequest.POST("/memories", request);
        final var response = httpClient.toBlocking().retrieve(
                httpRequest,
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        // then
        assertNotNull(response);
        assertEquals("Memory created.", response.context());

        final var memory = (CreateMemoryResponse) response.data();
        assertNotNull(memory);
        assertNotNull(memory.id());
        assertEquals("Empty Memory", memory.name());
        assertEquals("Memory without observations", memory.description());

        assertMemoryInDatabase(driver, memory);
        assertNoObservations(driver, memory.id());
    }

    @Test
    @DisplayName("should delete memory by name")
    void shouldDeleteMemoryByName() {
        // given
        final var createRequest = new CreateMemoryRequest(
                "Memory To Delete",
                "This memory will be deleted",
                List.of("observation1", "observation2")
        );

        final var createResponse = httpClient.toBlocking().retrieve(
                HttpRequest.POST("/memories", createRequest),
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        final var memory = (CreateMemoryResponse) createResponse.data();
        assertMemoryInDatabase(driver, memory);

        // when
        final var deleteRequest = HttpRequest.DELETE("/memories").uri(uri ->
                uri.queryParam("name", "Memory To Delete")
        );
        final var response = httpClient.toBlocking().retrieve(
                deleteRequest,
                Argument.of(ContextResponse.class, String.class)
        );

        // then
        assertNotNull(response);
        assertEquals("Memory deleted.", response.context());
        assertNull(response.data());

        assertMemoryNotInDatabase(driver, memory.id());
    }

    @Test
    @DisplayName("should return not found when deleting non-existent memory")
    void shouldReturnNotFoundWhenDeletingNonExistentMemory() {
        // when
        final var deleteRequest = HttpRequest.DELETE("/memories").uri(uri ->
                uri.queryParam("name", "Non Existent Memory")
        );
        final var response = httpClient.toBlocking().retrieve(
                deleteRequest,
                Argument.of(ContextResponse.class, String.class)
        );

        // then
        assertNotNull(response);
        assertEquals("Memory not found.", response.context());
        assertNull(response.data());
    }

    @Test
    @DisplayName("should update memory description")
    void shouldUpdateMemoryDescription() {
        // given
        final var createRequest = new CreateMemoryRequest(
                "Update Test Memory",
                "Original Description",
                Collections.emptyList()
        );


        httpClient.toBlocking().retrieve(
                HttpRequest.POST("/memories", createRequest),
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        final var updateRequest = new UpdateMemoryRequest(
                "Update Test Memory",
                "Updated Description"
        );

        // when
        final var httpRequest = HttpRequest.PUT("/memories", updateRequest);
        final var response = httpClient.toBlocking().retrieve(
                httpRequest,
                Argument.of(ContextResponse.class, UpdateMemoryResponse.class)
        );

        // then
        assertNotNull(response);
        assertEquals("Memory updated.", response.context());

        final var memory = (UpdateMemoryResponse) response.data();
        assertNotNull(memory);
        assertNotNull(memory.id());
        assertEquals("Update Test Memory", memory.name());
        assertEquals("Updated Description", memory.description());

        final var records = driver.executableQuery(
                        "MATCH (m:Memory {name: $name}) RETURN m.description as description"
                )
                .withParameters(java.util.Map.of("name", "Update Test Memory"))
                .execute()
                .records();

        assertEquals("Updated Description", records.get(0).get("description").asString());
    }
}
