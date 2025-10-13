package com.marcelogm.istarimcp.controller;

import com.marcelogm.istarimcp.api.ContextResponse;
import com.marcelogm.istarimcp.api.relationship.CreateRelationshipRequest;
import com.marcelogm.istarimcp.api.relationship.CreateRelationshipResponse;
import com.marcelogm.istarimcp.domain.service.RelationshipService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;

import java.util.List;

@Controller("/relationships")
public class RelationshipController {

    private final RelationshipService relationshipService;

    @Inject
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

    @Post
    public ContextResponse<CreateRelationshipResponse> create(@Body CreateRelationshipRequest request) {
        return new ContextResponse<>(
                "Relationship created between memories.",
                relationshipService.createRelationship(request)
        );
    }
}
