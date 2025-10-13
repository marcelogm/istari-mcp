package com.marcelogm.istarimcp.domain.repository;

import com.marcelogm.istarimcp.IntegrationTest;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.marcelogm.istarimcp.helper.MemoryFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class MemoryEmbeddingRepositoryIT extends IntegrationTest {

    @Inject
    private MemoryEmbeddingRepository memoryEmbeddingRepository;

    @Inject
    private MemoryRepository memoryRepository;

    @Test
    @DisplayName("should search memories with similar embeddings")
    void shouldSearchMemoriesWithSimilarEmbeddings() {
        // given
        final var observation = observation("test observation", embedding(1.0f, 0.0f, 0.0f));
        final var memory1 = memoryWithObservations("Memory 1", "First memory", embedding(1.0f, 0.0f, 0.0f), List.of(observation));
        final var memory2 = memory("Memory 2", "Second memory", embedding(0.9f, 0.1f, 0.0f));
        memoryRepository.createMemory(memory1);
        memoryRepository.createMemory(memory2);

        // when
        final var results = memoryEmbeddingRepository.search(embedding(1.0f, 0.0f, 0.0f));

        // then
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 10);

        final var firstResult = results.get(0);
        assertNotNull(firstResult.getLeft());
        assertNotNull(firstResult.getRight());
        assertTrue(firstResult.getRight() > 0.5f);
        assertEquals("Memory 1", firstResult.getLeft().name());
        assertEquals(1, firstResult.getLeft().observations().size());
    }

    @Test
    @DisplayName("should search memories without observations")
    void shouldSearchMemoriesWithoutObservations() {
        // given
        final var memory1 = memory("Summary Memory 1", "First summary", embedding(1.0f, 0.0f, 0.0f));
        final var memory2 = memory("Summary Memory 2", "Second summary", embedding(0.95f, 0.05f, 0.0f));
        memoryRepository.createMemory(memory1);
        memoryRepository.createMemory(memory2);

        // when
        final var results = memoryEmbeddingRepository.searchWithoutObservations(embedding(1.0f, 0.0f, 0.0f));

        // then
        assertNotNull(results);
        assertFalse(results.isEmpty());

        final var firstResult = results.get(0);
        assertNotNull(firstResult.getLeft());
        assertNotNull(firstResult.getRight());
        assertTrue(firstResult.getRight() > 0.5f);
        assertTrue(firstResult.getLeft().observations().isEmpty());
    }

    @Test
    @DisplayName("should order results by similarity score descending")
    void shouldOrderResultsBySimilarityScoreDescending() {
        // given
        final var memory1 = memory("Low Score Memory", "Low similarity", embedding(0.6f, 0.4f, 0.0f));
        final var memory2 = memory("High Score Memory", "High similarity", embedding(1.0f, 0.0f, 0.0f));
        final var memory3 = memory("Medium Score Memory", "Medium similarity", embedding(0.8f, 0.2f, 0.0f));
        memoryRepository.createMemory(memory1);
        memoryRepository.createMemory(memory2);
        memoryRepository.createMemory(memory3);

        // when
        final var results = memoryEmbeddingRepository.search(embedding(1.0f, 0.0f, 0.0f));

        // then
        assertNotNull(results);
        assertEquals(3, results.size());

        assertEquals("High Score Memory", results.get(0).getLeft().name());
        assertEquals("Medium Score Memory", results.get(1).getLeft().name());
        assertEquals("Low Score Memory", results.get(2).getLeft().name());

        assertTrue(results.get(0).getRight() >= results.get(1).getRight());
        assertTrue(results.get(1).getRight() >= results.get(2).getRight());
    }

    @Test
    @DisplayName("should filter memories with score below threshold")
    void shouldFilterMemoriesWithScoreBelowThreshold() {
        // given
        final var memory1 = memory("High Score Memory", "High similarity", embedding(1.0f, 0.0f, 0.0f));
        final var memory2 = memory("Low Score Memory", "Low similarity", embedding(0.0f, 1.0f, 0.0f));
        memoryRepository.createMemory(memory1);
        memoryRepository.createMemory(memory2);

        // when
        final var results = memoryEmbeddingRepository.search(embedding(1.0f, 0.0f, 0.0f));

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("High Score Memory", results.get(0).getLeft().name());
        assertTrue(results.get(0).getRight() > 0.5f);
    }

    @Test
    @DisplayName("should limit results to maximum of 10 memories")
    void shouldLimitResultsToMaximumOf10Memories() {
        // given
        for (int i = 0; i < 15; i++) {
            memoryRepository.createMemory(memory("Memory " + i, "Description " + i, embedding(1.0f, 0.0f, 0.0f)));
        }
        // when
        final var results = memoryEmbeddingRepository.search(embedding(1.0f, 0.0f, 0.0f));

        // then
        assertNotNull(results);
        assertEquals(10, results.size());
    }

    @Test
    @DisplayName("should return empty list when no memories match threshold")
    void shouldReturnEmptyListWhenNoMemoriesMatchThreshold() {
        // given
        memoryRepository.createMemory(memory("Dissimilar Memory", "Very different", embedding(0.0f, 0.0f, 1.0f)));
        // when
        final var results = memoryEmbeddingRepository.search(embedding(1.0f, 0.0f, 0.0f));

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("should return empty list when no memories exist")
    void shouldReturnEmptyListWhenNoMemoriesExist() {
        // when
        final var results = memoryEmbeddingRepository.search(embedding(1.0f, 0.0f, 0.0f));

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("should include observations in full search")
    void shouldIncludeObservationsInFullSearch() {
        // given
        final var observation1 = observation("first observation", embedding(1.0f, 0.0f, 0.0f));
        final var observation2 = observation("second observation", embedding(1.0f, 0.0f, 0.0f));
        final var memory = memoryWithObservations(
                "Memory With Observations",
                "Has observations",
                embedding(1.0f, 0.0f, 0.0f),
                List.of(observation1, observation2)
        );

        memoryRepository.createMemory(memory);

        // when
        final var results = memoryEmbeddingRepository.search(embedding(1.0f, 0.0f, 0.0f));

        // then
        assertNotNull(results);
        assertEquals(1, results.size());

        final var foundMemory = results.get(0).getLeft();
        assertEquals(2, foundMemory.observations().size());
        final var observationTexts = foundMemory.observations().stream()
                .map(ObservationNode::observation)
                .toList();
        assertTrue(observationTexts.contains("first observation"));
        assertTrue(observationTexts.contains("second observation"));
    }

    @Test
    @DisplayName("should not include observations in summary search")
    void shouldNotIncludeObservationsInSummarySearch() {
        // given
        final var observation = observation("test observation", embedding(1.0f, 0.0f, 0.0f));
        final var memory = memoryWithObservations(
                "Memory With Observations",
                "Has observations",
                embedding(1.0f, 0.0f, 0.0f),
                List.of(observation)
        );

        memoryRepository.createMemory(memory);

        // when
        final var results = memoryEmbeddingRepository.searchWithoutObservations(embedding(1.0f, 0.0f, 0.0f));

        // then
        assertNotNull(results);
        assertEquals(1, results.size());

        final var foundMemory = results.get(0).getLeft();
        assertTrue(foundMemory.observations().isEmpty());
    }
}
