package com.marcelogm.istarimcp.api.maintenance;

import java.time.LocalDateTime;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record RefreshStatusResponse(
        RefreshStatus status,
        RefreshResult result,
        String errorMessage) {

    public static RefreshStatusResponse idle() {
        return new RefreshStatusResponse(RefreshStatus.IDLE, null, null);
    }

    public static RefreshStatusResponse running() {
        return new RefreshStatusResponse(RefreshStatus.RUNNING, null, null);
    }

    public static RefreshStatusResponse completed(RefreshResult result) {
        return new RefreshStatusResponse(RefreshStatus.COMPLETED, result, null);
    }

    public static RefreshStatusResponse failed(RefreshResult result, String errorMessage) {
        return new RefreshStatusResponse(RefreshStatus.FAILED, result, errorMessage);
    }

    public enum RefreshStatus {
        IDLE, RUNNING, COMPLETED, FAILED
    }

    @Serdeable
    @Introspected
    public record RefreshResult(
            int memoriesProcessed,
            int memoriesFailed,
            LocalDateTime startTime,
            LocalDateTime endTime) {
    }
}
