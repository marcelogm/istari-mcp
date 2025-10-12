package com.marcelogm.istarimcp.domain.service;

import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class EmbeddingService {

    // TODO: call an embedding api
    public CompletableFuture<List<Float>> create(String text) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

}
