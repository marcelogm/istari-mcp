package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.GenericResponse;
import com.marcelogm.istarimcp.api.maintenance.RefreshStatusResponse.RefreshStatus;
import com.marcelogm.istarimcp.domain.service.maintenance.MaintenanceService;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

@Controller
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @Inject
    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Post("/embeddings/refresh")
    public Mono<GenericResponse> embedding() {
        return maintenanceService.startRefreshing()
                .map(started -> started
                        ? new GenericResponse("Refresh started in background.")
                        : new GenericResponse("Refresh already in progress."));
    }

    @Get("/embeddings/status")
    public Mono<ContextResponse<RefreshStatus>> reprocessStatus() {
        return Mono.just(maintenanceService.getStatus())
                .map(status -> new ContextResponse<>(
                        "Reprocessing status.",
                        status));
    }
}
