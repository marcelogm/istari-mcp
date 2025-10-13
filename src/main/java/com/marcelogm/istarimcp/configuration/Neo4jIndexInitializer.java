package com.marcelogm.istarimcp.infrastructure.configuration;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Inject;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes Neo4j database indexes and constraints on application startup.
 * This ensures that all necessary indexes are created before the application begins processing requests.
 */
@Context
public class Neo4jIndexInitializer implements ApplicationEventListener<ServerStartupEvent> {

    private static final Logger log = LoggerFactory.getLogger(Neo4jIndexInitializer.class);

    private final Driver driver;
    private final int vectorDimensions;

    @Inject
    public Neo4jIndexInitializer(
            Driver driver,
            @Property(name = "neo4j.vector.dimensions", defaultValue = "4096") int vectorDimensions
    ) {
        this.driver = driver;
        this.vectorDimensions = vectorDimensions;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        log.info("Initializing Neo4j indexes and constraints...");
        createConstraints();
        createVectorIndexes();
        log.info("Neo4j indexes and constraints initialized successfully");
    }

    private void createConstraints() {
        try (var session = driver.session()) {
            // Memory unique constraints
            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT memory_id_unique IF NOT EXISTS FOR (m:Memory) REQUIRE m.id IS UNIQUE");
                return null;
            });
            log.debug("Created constraint: memory_id_unique");

            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT memory_name_unique IF NOT EXISTS FOR (m:Memory) REQUIRE m.name IS UNIQUE");
                return null;
            });
            log.debug("Created constraint: memory_name_unique");

            // Observation unique constraint
            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT observation_id_unique IF NOT EXISTS FOR (o:Observation) REQUIRE o.id IS UNIQUE");
                return null;
            });
            log.debug("Created constraint: observation_id_unique");

        } catch (Exception e) {
            log.error("Error creating constraints", e);
            throw new RuntimeException("Failed to create database constraints", e);
        }
    }

    private void createVectorIndexes() {
        try (var session = driver.session()) {
            // Memory embedding vector index
            session.executeWrite(tx -> {
                var query = String.format("""
                        CREATE VECTOR INDEX memory_embedding_index IF NOT EXISTS
                        FOR (m:Memory) ON (m.embedding)
                        OPTIONS {
                          indexConfig: {
                            `vector.dimensions`: %d,
                            `vector.similarity_function`: 'cosine'
                          }
                        }
                        """, vectorDimensions);
                tx.run(query);
                return null;
            });
            log.debug("Created vector index: memory_embedding_index (dimensions: {})", vectorDimensions);

            // Observation embedding vector index
            session.executeWrite(tx -> {
                var query = String.format("""
                        CREATE VECTOR INDEX observation_embedding_index IF NOT EXISTS
                        FOR (o:Observation) ON (o.embedding)
                        OPTIONS {
                          indexConfig: {
                            `vector.dimensions`: %d,
                            `vector.similarity_function`: 'cosine'
                          }
                        }
                        """, vectorDimensions);
                tx.run(query);
                return null;
            });
            log.debug("Created vector index: observation_embedding_index (dimensions: {})", vectorDimensions);

        } catch (Exception e) {
            log.error("Error creating vector indexes", e);
            throw new RuntimeException("Failed to create vector indexes", e);
        }
    }
}
