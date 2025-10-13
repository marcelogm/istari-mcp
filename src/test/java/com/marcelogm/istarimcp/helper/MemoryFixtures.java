package com.marcelogm.istarimcp.helper;

import com.marcelogm.istarimcp.domain.model.MemoryNode;
import com.marcelogm.istarimcp.domain.model.ObservationNode;

import java.util.Collections;
import java.util.List;

import static java.util.UUID.randomUUID;

public class MemoryFixtures {

    private MemoryFixtures() {
    }

    public static ObservationNode observation(String text, List<Float> embedding) {
        return new ObservationNode(randomUUID(), text, embedding);
    }

    public static MemoryNode memory(String name, String description, List<Float> embedding) {
        return new MemoryNode(randomUUID(), name, description, Collections.emptyList(), embedding);
    }

    public static MemoryNode memoryWithObservations(
            String name,
            String description,
            List<Float> embedding,
            List<ObservationNode> observations
    ) {
        return new MemoryNode(randomUUID(), name, description, observations, embedding);
    }

    public static List<Float> embedding(float... values) {
        List<Float> result = new java.util.ArrayList<>();
        for (float value : values) {
            result.add(value);
        }
        return result;
    }
}
