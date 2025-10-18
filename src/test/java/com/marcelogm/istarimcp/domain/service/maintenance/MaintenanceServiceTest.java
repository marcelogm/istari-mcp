package com.marcelogm.istarimcp.domain.service.maintenance;

import com.marcelogm.istarimcp.api.maintenance.RefreshStatusResponse.RefreshStatus;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import com.marcelogm.istarimcp.domain.service.EmbeddingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private MemoryRepository repository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ObservationRefreshService observationRefreshService;

    @InjectMocks
    private MaintenanceService maintenanceService;

    private MemoryNode testMemory;
    private List<Float> testEmbedding;

    @BeforeEach
    void setUp() {
        final var observation1 = new ObservationNode(
                UUID.randomUUID(),
                "Test observation 1",
                List.of(1.0f, 2.0f, 3.0f));

        final var observation2 = new ObservationNode(
                UUID.randomUUID(),
                "Test observation 2",
                List.of(4.0f, 5.0f, 6.0f));

        testMemory = new MemoryNode(
                UUID.randomUUID(),
                "Test Memory",
                "Test Description",
                List.of(observation1, observation2),
                List.of(10.0f, 11.0f, 12.0f));

        testEmbedding = List.of(7.0f, 8.0f, 9.0f);
    }

    @Test
    @DisplayName("should return IDLE status initially")
    void shouldReturnIdleStatusInitially() {
        // when
        final var status = maintenanceService.getStatus();

        // then
        assertEquals(RefreshStatus.IDLE, status);
    }

    @Test
    @DisplayName("should return null for last result initially")
    void shouldReturnNullForLastResultInitially() {
        // when
        final var result = maintenanceService.getLastResult();

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("should return null for last error initially")
    void shouldReturnNullForLastErrorInitially() {
        // when
        final var error = maintenanceService.getLastError();

        // then
        assertNull(error);
    }

    @Test
    @DisplayName("should start refreshing when status is IDLE")
    void shouldStartRefreshingWhenStatusIsIdle() {
        // given
        when(repository.streamAll()).thenReturn(Flux.<MemoryNode>empty()
                .delaySubscription(Duration.ofMillis(100)));

        // when
        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> {
                    assertTrue(result);
                    // then
                    assertEquals(RefreshStatus.RUNNING, maintenanceService.getStatus());
                })
                .verifyComplete();

        // then
        waitFor(RefreshStatus.COMPLETED);
        verify(repository).streamAll();
    }

    @Test
    @DisplayName("should not start refreshing when already running")
    void shouldNotStartRefreshingWhenAlreadyRunning() {
        // given
        when(repository.streamAll()).thenReturn(Flux.<MemoryNode>empty()
                .delaySubscription(Duration.ofMillis(500)));

        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> assertTrue(result))
                .verifyComplete();

        // when
        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> assertFalse(result))
                .verifyComplete();

        // then
        waitFor(RefreshStatus.COMPLETED);
        verify(repository, times(1)).streamAll();
    }

    @Test
    @DisplayName("should refresh memory embeddings successfully")
    void shouldRefreshMemorySuccessfully() throws InterruptedException {
        // given
        when(repository.streamAll()).thenReturn(Flux.just(testMemory));
        when(observationRefreshService.refreshObservations(anyList()))
                .thenReturn(Mono.empty());
        when(embeddingService.createFromParts(anyString(), anyList()))
                .thenReturn(Mono.just(testEmbedding));
        when(repository.updateMemory(anyString(), anyString(), anyList()))
                .thenReturn(Optional.of(testMemory));

        // when
        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> assertTrue(result))
                .verifyComplete();

        // then
        waitFor(RefreshStatus.COMPLETED);
        assertEquals(1, maintenanceService.getLastResult().memoriesProcessed());
        assertEquals(0, maintenanceService.getLastResult().memoriesFailed());
        assertNull(maintenanceService.getLastError());

        verify(repository).streamAll();
        verify(observationRefreshService).refreshObservations(testMemory.observations());
        verify(embeddingService).createFromParts(
                "Test Memory: Test Description",
                List.of("Test observation 1", "Test observation 2"));
        verify(repository).updateMemory("Test Memory", "Test Description", testEmbedding);
    }

    @Test
    @DisplayName("should handle memory refresh failure")
    void shouldHandleMemoryRefreshFailure() throws InterruptedException {
        // given
        when(repository.streamAll()).thenReturn(Flux.just(testMemory));
        when(observationRefreshService.refreshObservations(anyList()))
                .thenReturn(Mono.empty());
        when(embeddingService.createFromParts(anyString(), anyList()))
                .thenReturn(Mono.just(testEmbedding));
        when(repository.updateMemory(anyString(), anyString(), anyList()))
                .thenReturn(Optional.empty());

        // when
        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> assertTrue(result))
                .verifyComplete();

        // then
        waitFor(RefreshStatus.COMPLETED);
        assertEquals(0, maintenanceService.getLastResult().memoriesProcessed());
        assertEquals(1, maintenanceService.getLastResult().memoriesFailed());

        verify(repository).streamAll();
        verify(observationRefreshService).refreshObservations(testMemory.observations());
        verify(embeddingService).createFromParts(anyString(), anyList());
        verify(repository).updateMemory("Test Memory", "Test Description", testEmbedding);
    }

    @Test
    @DisplayName("should handle embedding service error")
    void shouldHandleEmbeddingServiceError() {
        // given
        when(repository.streamAll()).thenReturn(Flux.just(testMemory));
        when(observationRefreshService.refreshObservations(anyList()))
                .thenReturn(Mono.empty());
        when(embeddingService.createFromParts(anyString(), anyList()))
                .thenReturn(Mono.error(new RuntimeException("Embedding service error")));

        // when
        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> assertTrue(result))
                .verifyComplete();

        // then
        waitFor(RefreshStatus.COMPLETED);
        assertEquals(0, maintenanceService.getLastResult().memoriesProcessed());
        assertEquals(1, maintenanceService.getLastResult().memoriesFailed());

        verify(repository).streamAll();
        verify(observationRefreshService).refreshObservations(testMemory.observations());
        verify(embeddingService, atLeast(1)).createFromParts(anyString(), anyList());
    }

    @Test
    @DisplayName("should handle partial failures in batch")
    void shouldHandlePartialFailuresInBatch() throws InterruptedException {
        // given
        final var memory1 = new MemoryNode(
                UUID.randomUUID(),
                "Memory 1",
                "Description 1",
                Collections.emptyList(),
                List.of(1.0f, 2.0f));

        final var memory2 = new MemoryNode(
                UUID.randomUUID(),
                "Memory 2",
                "Description 2",
                Collections.emptyList(),
                List.of(3.0f, 4.0f));

        when(repository.streamAll()).thenReturn(Flux.just(memory1, memory2));
        when(observationRefreshService.refreshObservations(anyList()))
                .thenReturn(Mono.empty());
        when(embeddingService.createFromParts(anyString(), anyList()))
                .thenReturn(Mono.just(testEmbedding));
        when(repository.updateMemory("Memory 1", "Description 1", testEmbedding))
                .thenReturn(Optional.of(memory1));
        when(repository.updateMemory("Memory 2", "Description 2", testEmbedding))
                .thenReturn(Optional.empty()); // falha na segunda

        // when
        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> assertTrue(result))
                .verifyComplete();

        // then
        waitFor(RefreshStatus.COMPLETED);
        assertEquals(1, maintenanceService.getLastResult().memoriesProcessed());
        assertEquals(1, maintenanceService.getLastResult().memoriesFailed());

        verify(repository).streamAll();
        verify(embeddingService, times(2)).createFromParts(anyString(), anyList());
        verify(repository, times(2)).updateMemory(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("should handle complete stream failure")
    void shouldHandleCompleteStreamFailure() throws InterruptedException {
        // given
        when(repository.streamAll())
                .thenReturn(Flux.error(new RuntimeException("Database connection error")));

        // when
        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> assertTrue(result))
                .verifyComplete();

        // then
        waitFor(RefreshStatus.FAILED);
        assertEquals(0, maintenanceService.getLastResult().memoriesProcessed());
        assertEquals(0, maintenanceService.getLastResult().memoriesFailed());
        assertNotNull(maintenanceService.getLastError());
        assertEquals("Database connection error", maintenanceService.getLastError());

        verify(repository).streamAll();
    }

    @Test
    @DisplayName("should allow restart after completion")
    void shouldAllowRestartAfterCompletion() throws InterruptedException {
        // given
        when(repository.streamAll()).thenReturn(Flux.empty());

        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> assertTrue(result))
                .verifyComplete();
        waitFor(RefreshStatus.COMPLETED);

        // when
        StepVerifier.create(maintenanceService.startRefreshing())
                .assertNext(result -> {
                    assertFalse(result);
                })
                .verifyComplete();

        // then
        verify(repository, times(1)).streamAll();
    }

    private void waitFor(RefreshStatus refreshStatus) {
        final var RETRY_DELAY = 2;
        final var RETRY_ATTEMPTS = 3;
        Awaitility.await()
                .atMost(RETRY_ATTEMPTS * RETRY_DELAY * 10, TimeUnit.SECONDS)
                .until(() -> maintenanceService.getStatus() == refreshStatus);
    }
}
