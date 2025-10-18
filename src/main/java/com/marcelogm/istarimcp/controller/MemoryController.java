package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.GenericResponse;
import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.api.memory.UpdateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.UpdateMemoryResponse;
import com.marcelogm.istarimcp.domain.service.MemoryService;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

@Controller("/memories")
public class MemoryController {

    private final MemoryService memoryService;

    @Inject
    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
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

}
