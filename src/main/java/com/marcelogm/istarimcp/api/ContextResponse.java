package com.marcelogm.istarimcp.api;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ContextResponse<T> (String context, T data) {
}