package com.marcelogm.istarimcp.domain.repository;

import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graalvm.collections.Pair;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Singleton
public class MemoryEmbeddingRepository {

    private final Driver driver;

    @Inject
    public MemoryEmbeddingRepository(Driver driver) {
        this.driver = driver;
    }

    public List<Pair<MemoryNode, Float>> searchWithoutObservations(List<Float> queryEmbedding) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var query = """
                        MATCH (m:Memory)
                        WITH m, vector.similarity.cosine(m.embedding, $queryEmbedding) AS score
                        WHERE score > 0.5
                        RETURN m, score
                        ORDER BY score DESC
                        LIMIT $limit
                        """;

                var parameters = Values.parameters("queryEmbedding", queryEmbedding, "limit", 10);
                return tx.run(query, parameters).list(this::mapToMemoryWithScore);
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<Pair<MemoryNode, Float>> search(List<Float> queryEmbedding) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var query = """
                        MATCH (m:Memory)
                        WITH m, vector.similarity.cosine(m.embedding, $queryEmbedding) AS score
                        WHERE score > 0.5
                        OPTIONAL MATCH (m)-[:HAS_OBSERVATION]->(o:Observation)
                        WITH m, score, collect(o) AS observations
                        RETURN m, observations, score
                        ORDER BY score DESC
                        LIMIT $limit
                        """;

                var parameters = Values.parameters("queryEmbedding", queryEmbedding, "limit", 10);
                return tx.run(query, parameters).list(this::mapToMemoryWithObservationsAndScore);
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Pair<MemoryNode, Float> mapToMemoryWithScore(org.neo4j.driver.Record record) {
        var memoryNode = record.get("m").asNode();
        var memory = mapToMemoryNode(memoryNode, Collections.emptyList());
        var score = record.get("score").asFloat();
        return Pair.create(memory, score);
    }

    private Pair<MemoryNode, Float> mapToMemoryWithObservationsAndScore(org.neo4j.driver.Record record) {
        var memoryNode = record.get("m").asNode();
        var observations = mapToObservations(record.get("observations"));
        var memory = mapToMemoryNode(memoryNode, observations);
        var score = record.get("score").asFloat();
        return Pair.create(memory, score);
    }

    private MemoryNode mapToMemoryNode(org.neo4j.driver.types.Node node, List<ObservationNode> observations) {
        return new MemoryNode(
                UUID.fromString(node.get("id").asString()),
                node.get("name").asString(),
                node.get("description").asString(),
                observations,
                node.get("embedding").asList(Value::asFloat)
        );
    }

    private List<ObservationNode> mapToObservations(Value observationsValue) {
        return observationsValue.asList(obsNode -> {
            var node = obsNode.asNode();
            return new ObservationNode(
                    UUID.fromString(node.get("id").asString()),
                    node.get("observation").asString(),
                    node.get("embedding").asList(Value::asFloat)
            );
        });
    }

}
