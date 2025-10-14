package com.marcelogm.istarimcp.api.memory;

public record ReprocessMemoriesResponse(
        int processed,
        int failed,
        int total
) {
}
