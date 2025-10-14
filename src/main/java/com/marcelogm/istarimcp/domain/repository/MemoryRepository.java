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
import reactor.core.publisher.Flux;

import java.util.*;

@Singleton
public class MemoryRepository {

    private final Driver driver;

    @Inject
    public MemoryRepository(Driver driver) {
        this.driver = driver;
    }

    public MemoryNode createMemory(MemoryNode memoryNode) {
        final var observations = memoryNode.observations().stream()
                .map(obs -> Map.of(
                        "id", obs.id().toString(),
                        "observation", obs.observation(),
                        "embedding", obs.embedding()
                ))
                .toList();

        driver.executableQuery("""
                        CREATE (m:Memory {
                            id: $id,
                            name: $name,
                            description: $description,
                            embedding: $embedding
                        })
                        WITH m
                        UNWIND $observations AS obs
                        CREATE (o:Observation {
                            id: obs.id,
                            observation: obs.observation,
                            embedding: obs.embedding
                        })
                        CREATE (m)-[:HAS_OBSERVATION]->(o)
                        """)
                .withParameters(Map.of(
                        "id", memoryNode.id().toString(),
                        "name", memoryNode.name(),
                        "description", memoryNode.description(),
                        "embedding", memoryNode.embedding(),
                        "observations", observations
                )).execute();

        return memoryNode;
    }

    public List<Pair<String, MemoryNode>> findRelationsByMemoryId(UUID memoryId) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var query = """
                        MATCH (m:Memory {id: $memoryId})-[r]->(related:Memory)
                        RETURN type(r) AS relationType, related
                        """;

                var parameters = Values.parameters("memoryId", memoryId.toString());
                return tx.run(query, parameters).list(this::mapToRelationship);
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Flux<MemoryNode> streamAll() {
        return Flux.create(sink -> {
            try (Session session = driver.session()) {
                session.executeRead(tx -> {
                    var query = """
                                MATCH (m:Memory)
                            OPTIONAL MATCH (m)-[:HAS_OBSERVATION]->(o:Observation)
                            WITH m, collect(o) AS observations
                            RETURN m, observations
                            """;

                    var result = tx.run(query);
                    while (result.hasNext() && !sink.isCancelled()) {
                        var record = result.next();
                        var memory = mapToMemoryWithObservations(record);
                        sink.next(memory);
                    }
                    sink.complete();
                    return null;
                });
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    public Optional<MemoryNode> findByName(String name) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var query = """
                        MATCH (m:Memory {name: $name})
                        OPTIONAL MATCH (m)-[:HAS_OBSERVATION]->(o:Observation)
                        WITH m, collect(o) AS observations
                        RETURN m, observations
                        """;

                var parameters = Values.parameters("name", name);
                var result = tx.run(query, parameters).list(this::mapToMemoryWithObservations);
                return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
            });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Pair<String, MemoryNode>> findRelationsByMemoryName(String name) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var query = """
                        MATCH (m:Memory {name: $name})-[r]->(related:Memory)
                        RETURN type(r) AS relationType, related
                        """;

                var parameters = Values.parameters("name", name);
                return tx.run(query, parameters).list(this::mapToRelationship);
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public boolean deleteByName(String name) {
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                var query = """
                        MATCH (m:Memory {name: $name})
                        OPTIONAL MATCH (m)-[:HAS_OBSERVATION]->(o:Observation)
                        DETACH DELETE m, o
                        RETURN count(m) > 0 AS deleted
                        """;

                var parameters = Values.parameters("name", name);
                var result = tx.run(query, parameters).single();
                return result.get("deleted").asBoolean();
            });
        } catch (Exception e) {
            return false;
        }
    }

    public boolean createRelationship(String sourceMemoryName, String targetMemoryName, String relationshipType) {
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                var query = """
                        MATCH (source:Memory {name: $sourceName})
                        MATCH (target:Memory {name: $targetName})
                        CALL apoc.create.relationship(source, $relationshipType, {}, target) YIELD rel
                        RETURN count(rel) > 0 AS created
                        """;

                var parameters = Values.parameters(
                        "sourceName", sourceMemoryName,
                        "targetName", targetMemoryName,
                        "relationshipType", relationshipType
                );
                var result = tx.run(query, parameters).single();
                return result.get("created").asBoolean();
            });
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<MemoryNode> updateMemory(String name, String newDescription, List<Float> newEmbedding) {
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                var checkQuery = "MATCH (m:Memory {name: $name}) RETURN count(m) as count";
                var checkParams = Values.parameters("name", name);
                var checkResult = tx.run(checkQuery, checkParams).single();

                if (checkResult.get("count").asInt() == 0) {
                    return Optional.empty();
                }

                var updateQuery = """
                        MATCH (m:Memory {name: $name})
                        SET m.description = $description,
                            m.embedding = $embedding
                        RETURN m
                        """;

                var updateParams = Values.parameters(
                        "name", name,
                        "description", newDescription,
                        "embedding", newEmbedding
                );
                tx.run(updateQuery, updateParams).consume();

                var fetchQuery = """
                        MATCH (m:Memory {name: $name})
                        OPTIONAL MATCH (m)-[:HAS_OBSERVATION]->(o:Observation)
                        WITH m, collect(o) AS observations
                        RETURN m, observations
                        """;

                var fetchParams = Values.parameters("name", name);
                var result = tx.run(fetchQuery, fetchParams).list(this::mapToMemoryWithObservations);
                return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
            });
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private MemoryNode mapToMemoryWithObservations(org.neo4j.driver.Record record) {
        var memoryNode = record.get("m").asNode();
        var observations = mapToObservations(record.get("observations"));
        return mapToMemoryNode(memoryNode, observations);
    }

    private Pair<String, MemoryNode> mapToRelationship(org.neo4j.driver.Record record) {
        var relationType = record.get("relationType").asString();
        var relatedNode = record.get("related").asNode();
        var memory = mapToMemoryNode(relatedNode, Collections.emptyList());
        return Pair.create(relationType, memory);
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
