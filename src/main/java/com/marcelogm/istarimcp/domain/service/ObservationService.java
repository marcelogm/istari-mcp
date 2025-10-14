package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.api.observation.CreateObservationRequest;
import com.marcelogm.istarimcp.api.observation.CreateObservationResponse;
import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import com.marcelogm.istarimcp.domain.repository.ObservationRepository;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class ObservationService {

    private final ObservationRepository repository;
    private final EmbeddingService embeddingService;
    private final MemoryRepository memoryRepository;

    public ObservationService(ObservationRepository repository,
                              EmbeddingService embeddingService,
                              MemoryRepository memoryRepository) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.memoryRepository = memoryRepository;
    }

    public Mono<CreateObservationResponse> addObservation(CreateObservationRequest request) {
        final var memory = memoryRepository.findByName(request.memoryName());

        if (memory.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Memory not found: " + request.memoryName()));
        }

        return embeddingService.create(request.observation())
                .map(it -> new ObservationNode(UUID.randomUUID(), request.observation(), it))
                .flatMap(it -> persist(it, memory.get()));
    }

    private Mono<CreateObservationResponse> persist(ObservationNode observation, MemoryNode memory) {
        final var result = repository.addObservationToMemory(memory.name(), observation);

        if (result.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Failed to add observation"));
        }

        return updateMemoryEmbedding(memory, observation.observation())
                .thenReturn(new CreateObservationResponse(
                        observation.id(),
                        memory.name(),
                        observation.observation()));
    }

    private Mono<Void> updateMemoryEmbedding(MemoryNode memoryNode, String newObservation) {
        final var mainText = memoryNode.name() + ": " + memoryNode.description();
        final var allObservationTexts = memoryNode.observations().stream()
                .map(ObservationNode::observation)
                .collect(Collectors.toList());

        allObservationTexts.add(newObservation);

        return embeddingService.createFromParts(mainText, allObservationTexts)
                .doOnNext(newEmbedding -> memoryRepository.updateMemory(
                        memoryNode.name(),
                        memoryNode.description(),
                        newEmbedding))
                .then();
    }
}
