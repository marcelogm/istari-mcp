package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.GenericResponse;
import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.api.memory.UpdateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.UpdateMemoryResponse;
import com.marcelogm.istarimcp.domain.service.MemoryService;
import com.marcelogm.istarimcp.domain.service.MemoryReprocessService;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

@Controller("/memories")
public class MemoryController {

    private final MemoryService memoryService;
    private final MemoryReprocessService reprocessService;

    @Inject
    public MemoryController(MemoryService memoryService, MemoryReprocessService reprocessService) {
        this.memoryService = memoryService;
        this.reprocessService = reprocessService;
    }

    @Post
    public Mono<ContextResponse<CreateMemoryResponse>> create(@Body CreateMemoryRequest request) {
        return memoryService.create(request)
                .map(response -> new ContextResponse<>(
                        "Memory created.",
                        response
                ));
    }

    @Delete
    public Mono<GenericResponse> delete(@QueryValue String name) {
        return memoryService.deleteByName(name)
                .map(deleted -> deleted
                        ? new GenericResponse("Memory deleted.")
                        : new GenericResponse("Memory not found."));
    }

    @Put
    public Mono<ContextResponse<UpdateMemoryResponse>> update(@Body UpdateMemoryRequest request) {
        return memoryService.updateMemory(request)
                .map(response -> new ContextResponse<>(
                        "Memory updated.",
                        response
                ));
    }

    @Post("/reprocess")
    public Mono<GenericResponse> reprocess() {
        return reprocessService.startReprocessing()
                .map(started -> started
                        ? new GenericResponse("Reprocessing started in background.")
                        : new GenericResponse("Reprocessing already in progress."));
    }

    @Get("/reprocess/status")
    public Mono<ContextResponse<MemoryReprocessService.ReprocessStatus>> reprocessStatus() {
        return Mono.just(reprocessService.getStatus())
                .map(status -> new ContextResponse<>(
                        "Reprocessing status.",
                        status
                ));
    }
}
