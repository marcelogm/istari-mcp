package com.marcelogm.istarimcp.exception;

import com.marcelogm.istarimcp.api.GenericResponse;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

@Produces
@Singleton
@Requires(classes = {IllegalArgumentException.class, ExceptionHandler.class})
public class GlobalExceptionHandler implements ExceptionHandler<IllegalArgumentException, HttpResponse<GenericResponse>> {

    @Override
    public HttpResponse<GenericResponse> handle(HttpRequest request, IllegalArgumentException exception) {
        final var errorResponse = new GenericResponse(
                exception.getMessage()
        );
        return HttpResponse.badRequest(errorResponse);
    }
}
