package com.marcelogm.istarimcp.client;

import io.micronaut.core.async.annotation.SingleResult;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.serde.annotation.Serdeable;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.micronaut.http.HttpHeaders.ACCEPT;
import static io.micronaut.http.HttpHeaders.USER_AGENT;

@Client("embedding")
@Header(name = USER_AGENT, value = "Micronaut HTTP Client")
@Header(name = ACCEPT, value = "application/json")
public interface EmbeddingClient {

    @Serdeable
    record EmbeddingRequest(
            String model,
            String prompt
    ) {
    }

    @Serdeable
    record EmbeddingResponse(
            List<Float> embedding
    ) {
    }

    @Post("/api/embeddings")
    @SingleResult
    Mono<EmbeddingResponse> apply(@Body EmbeddingRequest request);

}