package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.IntegrationTest;
import com.marcelogm.istarimcp.api.ContextResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RelationshipControllerIT extends IntegrationTest {

    @Inject
    @Client("/")
    private HttpClient httpClient;

    @Test
    @DisplayName("should return relationships with context message")
    void shouldReturnRelationshipsWithContext() {
        final var httpRequest = HttpRequest.GET("/relationships");
        final var response = httpClient.toBlocking().retrieve(
                httpRequest,
                Argument.of(ContextResponse.class, Argument.listOf(String.class)));

        assertNotNull(response);
        assertNotNull(response.context());
        assertEquals("Use the following relationships to create correlations between memories.", response.context());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("should return expected number of relationship types")
    void shouldReturnExpectedNumberOfRelationships() {
        final var httpRequest = HttpRequest.GET("/relationships");
        final var response = httpClient.toBlocking().retrieve(
                httpRequest,
                Argument.of(ContextResponse.class, Argument.listOf(String.class)));

        assertNotNull(response);
        final var relationships = (List<String>) response.data();
        assertEquals(48, relationships.size());
    }
}
