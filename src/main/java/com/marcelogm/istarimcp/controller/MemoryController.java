package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.memory.CreateMemoryRequest;
import com.marcelogm.istarimcp.api.memory.CreateMemoryResponse;
import com.marcelogm.istarimcp.domain.service.MemoryService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

@Controller("/memories")
public class MemoryController {

    @Inject
    private final MemoryService memoryService;

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
}
