package com.marcelogm.istarimcp.service;

import com.marcelogm.istarimcp.api.relationship.CreateRelationshipRequest;
import com.marcelogm.istarimcp.domain.converter.ToMemorySummaryResponse;
import com.marcelogm.istarimcp.domain.repository.MemoryRepository;
import com.marcelogm.istarimcp.domain.service.RelationshipService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationshipServiceTest {

    @Mock
    private MemoryRepository repository;

    @Mock
    private ToMemorySummaryResponse toMemorySummaryResponse;

    @InjectMocks
    private RelationshipService relationshipService;

    @Test
    @DisplayName("should create relationship between memories")
    void shouldCreateRelationshipBetweenMemories() {
        final var request = new CreateRelationshipRequest(
                "Memory A",
                "Memory B",
                "RELATED_TO"
        );

        when(repository.createRelationship("Memory A", "Memory B", "RELATED_TO"))
                .thenReturn(true);

        final var response = relationshipService.createRelationship(request);

        assertNotNull(response);
        assertEquals("Memory A", response.sourceMemoryName());
        assertEquals("Memory B", response.targetMemoryName());
        assertEquals("RELATED_TO", response.relationshipType());

        verify(repository).createRelationship("Memory A", "Memory B", "RELATED_TO");
    }

    @Test
    @DisplayName("should throw exception when relationship creation fails")
    void shouldThrowExceptionWhenRelationshipCreationFails() {
        final var request = new CreateRelationshipRequest(
                "Non Existent Memory",
                "Memory B",
                "RELATED_TO"
        );

        when(repository.createRelationship("Non Existent Memory", "Memory B", "RELATED_TO"))
                .thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                relationshipService.createRelationship(request)
        );

        verify(repository).createRelationship("Non Existent Memory", "Memory B", "RELATED_TO");
    }

    @Test
    @DisplayName("should throw exception for invalid relationship type")
    void shouldThrowExceptionForInvalidRelationshipType() {
        final var request = new CreateRelationshipRequest(
                "Memory A",
                "Memory B",
                "INVALID_TYPE"
        );

        assertThrows(IllegalArgumentException.class, () ->
                relationshipService.createRelationship(request)
        );

        verify(repository, never()).createRelationship(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("should get all relationship types")
    void shouldGetAllRelationshipTypes() {
        final var types = relationshipService.getAll();

        assertNotNull(types);
        assertFalse(types.isEmpty());
        assertTrue(types.contains("RELATED_TO"));
        assertTrue(types.contains("HAS"));
        assertTrue(types.contains("IS_A"));
    }
}
