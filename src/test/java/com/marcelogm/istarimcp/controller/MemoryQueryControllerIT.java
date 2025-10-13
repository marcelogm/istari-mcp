package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.IntegrationTest;
import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.api.memory.MemoryResponse;
import com.marcelogm.istarimcp.client.EmbeddingClient;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MemoryQueryControllerIT extends IntegrationTest {

    @Inject
    @Client("/")
    private HttpClient httpClient;

    @Inject
    private EmbeddingClient embeddingClient;

    @Test
    @DisplayName("should retrieve memory by name")
    void shouldRetrieveMemoryByName() {
        when(embeddingClient.apply(any())).thenReturn(Mono.just(
                new EmbeddingClient.EmbeddingResponse(List.of(0.0f, 1.0f, 0.0f))
        ));
        final var createRequest = new CreateMemoryRequest(
                "Findable Memory",
                "Memory to find",
                List.of("observation1")
        );

        httpClient.toBlocking().retrieve(
                HttpRequest.POST("/memories", createRequest),
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        final var getRequest = HttpRequest.GET("/memories").uri(uri ->
                uri.queryParam("name", "Findable Memory")
        );
        final var response = httpClient.toBlocking().retrieve(
                getRequest,
                Argument.of(ContextResponse.class, MemoryResponse.class)
        );

        assertNotNull(response);
        assertEquals("Memory retrieved by name.", response.context());

        final var memory = (MemoryResponse) response.data();
        assertNotNull(memory);
        assertEquals("Findable Memory", memory.name());
        assertEquals("Memory to find", memory.description());
        assertEquals(1, memory.observations().size());
    }

    @Test
    @DisplayName("should return empty response when memory not found by name")
    void shouldReturnEmptyResponseWhenMemoryNotFoundByName() {
        final var getRequest = HttpRequest.GET("/memories").uri(uri ->
                uri.queryParam("name", "Non Existent")
        );
        final var response = httpClient.toBlocking().retrieve(
                getRequest,
                Argument.of(ContextResponse.class, MemoryResponse.class)
        );

        assertNotNull(response);
        assertEquals("No memory retrieved by name.", response.context());
        assertNull(response.data());
    }

    @Test
    @DisplayName("should search memories by similarity")
    void shouldSearchMemoriesBySimilarity() {
        when(embeddingClient.apply(any())).thenReturn(Mono.just(
                new EmbeddingClient.EmbeddingResponse(List.of(0.0f, 1.0f, 0.0f))
        ));
        final var request1 = new CreateMemoryRequest(
                "Java Programming",
                "Java is an object-oriented language",
                List.of("observation1")
        );

        final var request2 = new CreateMemoryRequest(
                "Python Programming",
                "Python is a high-level language",
                List.of("observation2")
        );

        httpClient.toBlocking().retrieve(
                HttpRequest.POST("/memories", request1),
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        httpClient.toBlocking().retrieve(
                HttpRequest.POST("/memories", request2),
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        final var searchRequest = HttpRequest.GET("/memories/search").uri(uri ->
                uri.queryParam("context", "programming language")
        );
        final var response = httpClient.toBlocking().retrieve(
                searchRequest,
                Argument.of(ContextResponse.class, Argument.listOf(MemoryResponse.class))
        );

        assertNotNull(response);
        assertEquals("Memories retrieved by similarity.", response.context());

        final var memories = (List<MemoryResponse>) response.data();
        assertNotNull(memories);
        assertFalse(memories.isEmpty());
    }
}
