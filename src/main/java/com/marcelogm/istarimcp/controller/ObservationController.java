package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.observation.CreateObservationRequest;
import com.marcelogm.istarimcp.api.observation.CreateObservationResponse;
import com.marcelogm.istarimcp.domain.service.ObservationService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

@Controller("/observations")
public class ObservationController {

    private final ObservationService observationService;

    @Inject
    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
    }

    @Post
    public Mono<ContextResponse<CreateObservationResponse>> create(@Body CreateObservationRequest request) {
        return observationService.addObservation(request)
                .map(response -> new ContextResponse<>(
                        "Observation added to memory.",
                        response
                ));
    }
}
