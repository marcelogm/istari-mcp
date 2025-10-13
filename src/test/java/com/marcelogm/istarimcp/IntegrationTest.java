package com.marcelogm.istarimcp;

import com.marcelogm.istarimcp.client.EmbeddingClient;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.Driver;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.mockito.Mockito.mock;

@MicronautTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTest implements TestPropertyProvider {

    @Container
    static final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5")
            .withoutAuthentication()
            .withEnv("NEO4J_PLUGINS", "[\"apoc\"]");

    @Inject
    protected Driver driver;

    @MockBean(EmbeddingClient.class)
    EmbeddingClient embeddingClient() {
        return mock(EmbeddingClient.class);
    }

    @Override
    public Map<String, String> getProperties() {
        if (!neo4jContainer.isRunning()) {
            neo4jContainer.start();
        }
        return Map.of(
                "neo4j.uri", neo4jContainer.getBoltUrl(),
                "neo4j.username", "neo4j",
                "neo4j.password", ""
        );
    }

    @AfterEach
    void cleanDatabase() {
        driver.executableQuery("MATCH (n) DETACH DELETE n")
                .execute();
    }
}
