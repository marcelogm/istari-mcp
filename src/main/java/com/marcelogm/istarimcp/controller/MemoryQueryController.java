package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.memory.MemoryResponse;
import com.marcelogm.istarimcp.domain.service.MemoryQueryService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller("/memories")
public class MemoryQueryController {

    private final MemoryQueryService memoryQueryService;

    @Inject
    public MemoryQueryController(MemoryQueryService memoryQueryService) {
        this.memoryQueryService = memoryQueryService;
    }

    @Get
    public Mono<ContextResponse<MemoryResponse>> get(@QueryValue String name) {
        return memoryQueryService.findByName(name)
                .map(response -> new ContextResponse<>("Memory retrieved by name.", response))
                .defaultIfEmpty(new ContextResponse<>("No memory retrieved by name.", null));
    }

    @Get("/search")
    public Mono<ContextResponse<List<MemoryResponse>>> search(@QueryValue String context) {
        return memoryQueryService.similaritySearch(context)
                .collectList()
                .map(response -> new ContextResponse<>(
                        "Memories retrieved by similarity.",
                        response
                ));
    }
}
