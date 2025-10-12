package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.domain.service.RelationshipService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;

import java.util.List;

@Controller("/relationships")
public class RelationshipController {

    @Inject
    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @Get
    public ContextResponse<List<String>> get() {
        return new ContextResponse<>(
                "Use the following relationships to create correlations between memories.",
                relationshipService.getAll()
        );
    }
}
