package com.marcelogm.istarimcp;

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

@MicronautTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTest implements TestPropertyProvider {

    @Container
    static final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5")
            .withoutAuthentication();

    @Inject
    protected Driver driver;

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
