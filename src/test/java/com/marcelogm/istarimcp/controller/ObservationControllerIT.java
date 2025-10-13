package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.IntegrationTest;
import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.api.observation.CreateObservationRequest;
import com.marcelogm.istarimcp.api.observation.CreateObservationResponse;
import com.marcelogm.istarimcp.client.EmbeddingClient;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ObservationControllerIT extends IntegrationTest {

    @Inject
    @Client("/")
    private HttpClient httpClient;

    @Inject
    private EmbeddingClient embeddingClient;

    @BeforeEach
    public void setup() {
        when(embeddingClient.apply(any())).thenReturn(Mono.just(
                new EmbeddingClient.EmbeddingResponse(List.of(0.0f, 1.0f, 0.0f))
        ));
    }

    @Test
    @DisplayName("should add observation to existing memory")
    void shouldAddObservationToExistingMemory() {
        final var memoryRequest = new CreateMemoryRequest(
                "Test Memory",
                "Test Description",
                Collections.emptyList()
        );

        httpClient.toBlocking().retrieve(
                HttpRequest.POST("/memories", memoryRequest),
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        final var observationRequest = new CreateObservationRequest(
                "Test Memory",
                "New observation for test memory"
        );

        final var httpRequest = HttpRequest.POST("/observations", observationRequest);
        final var response = httpClient.toBlocking().retrieve(
                httpRequest,
                Argument.of(ContextResponse.class, CreateObservationResponse.class)
        );

        assertNotNull(response);
        assertEquals("Observation added to memory.", response.context());

        final var observation = (CreateObservationResponse) response.data();
        assertNotNull(observation);
        assertNotNull(observation.id());
        assertEquals("Test Memory", observation.memoryName());
        assertEquals("New observation for test memory", observation.observation());

        final var records = driver.executableQuery(
                        "MATCH (m:Memory {name: $name})-[:HAS_OBSERVATION]->(o:Observation) RETURN count(o) as count"
                )
                .withParameters(java.util.Map.of("name", "Test Memory"))
                .execute()
                .records();

        assertEquals(1, records.get(0).get("count").asInt());
    }

    @Test
    @DisplayName("should add multiple observations to same memory")
    void shouldAddMultipleObservationsToSameMemory() {
        final var memoryRequest = new CreateMemoryRequest(
                "Multi Obs Memory",
                "Test Description",
                Collections.emptyList()
        );

        httpClient.toBlocking().retrieve(
                HttpRequest.POST("/memories", memoryRequest),
                Argument.of(ContextResponse.class, CreateMemoryResponse.class)
        );

        final var observation1Request = new CreateObservationRequest(
                "Multi Obs Memory",
                "First observation"
        );

        final var observation2Request = new CreateObservationRequest(
                "Multi Obs Memory",
                "Second observation"
        );

        httpClient.toBlocking().retrieve(
                HttpRequest.POST("/observations", observation1Request),
                Argument.of(ContextResponse.class, CreateObservationResponse.class)
        );

        httpClient.toBlocking().retrieve(
                HttpRequest.POST("/observations", observation2Request),
                Argument.of(ContextResponse.class, CreateObservationResponse.class)
        );

        final var records = driver.executableQuery(
                        "MATCH (m:Memory {name: $name})-[:HAS_OBSERVATION]->(o:Observation) RETURN count(o) as count"
                )
                .withParameters(java.util.Map.of("name", "Multi Obs Memory"))
                .execute()
                .records();

        assertEquals(2, records.get(0).get("count").asInt());
    }
}
