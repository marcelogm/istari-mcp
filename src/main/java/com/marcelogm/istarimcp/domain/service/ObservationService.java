package com.marcelogm.istarimcp.domain.service;

import com.marcelogm.istarimcp.api.observation.CreateObservationRequest;
import com.marcelogm.istarimcp.api.observation.CreateObservationResponse;
import com.marcelogm.istarimcp.domain.model.ObservationNode;
import com.marcelogm.istarimcp.domain.repository.ObservationRepository;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Singleton
public class ObservationService {

    private final ObservationRepository repository;
    private final EmbeddingService embeddingService;

    public ObservationService(ObservationRepository repository, EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    public Mono<CreateObservationResponse> addObservation(CreateObservationRequest request) {
        return embeddingService.create(request.observation())
                .map(embedding -> new ObservationNode(
                        UUID.randomUUID(),
                        request.observation(),
                        embedding
                ))
                .flatMap(observationNode -> {
                    var result = repository.addObservationToMemory(request.memoryName(), observationNode);
                    if (result.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("Memory not found: " + request.memoryName()));
                    }
                    return Mono.just(new CreateObservationResponse(
                            observationNode.id(),
                            request.memoryName(),
                            request.observation()
                    ));
                });
    }
}
