package com.marcelogm.istarimcp.domain.repository;

import com.marcelogm.istarimcp.domain.model.ObservationNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class ObservationRepository {

    private final Driver driver;

    @Inject
    public ObservationRepository(Driver driver) {
        this.driver = driver;
    }

    public boolean updateObservationEmbedding(UUID observationId, List<Float> embedding) {
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                var query = """
                        MATCH (o:Observation {id: $id})
                        SET o.embedding = $embedding
                        RETURN count(o) > 0 AS updated
                        """;
                var parameters = Values.parameters(
                        "id", observationId.toString(),
                        "embedding", embedding
                );
                var result = tx.run(query, parameters).single();
                return result.get("updated").asBoolean();
            });
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<ObservationNode> addObservationToMemory(String memoryName, ObservationNode observation) {
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                var query = """
                        MATCH (m:Memory {name: $memoryName})
                        CREATE (o:Observation {
                            id: $id,
                            observation: $observation,
                            embedding: $embedding
                        })
                        CREATE (m)-[:HAS_OBSERVATION]->(o)
                        RETURN o
                        """;

                var parameters = Values.parameters(
                        "memoryName", memoryName,
                        "id", observation.id().toString(),
                        "observation", observation.observation(),
                        "embedding", observation.embedding()
                );
                var result = tx.run(query, parameters).list();
                if (result.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(observation);
            });
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
