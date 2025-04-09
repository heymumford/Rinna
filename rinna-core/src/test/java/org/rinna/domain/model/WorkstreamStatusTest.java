/*
 * Unit tests for the WorkstreamStatus enum
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.base.UnitTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the WorkstreamStatus enum.
 */
@DisplayName("WorkstreamStatus Tests")
public class WorkstreamStatusTest extends UnitTest {
    
    @Test
    @DisplayName("Should have correct display names")
    void shouldHaveCorrectDisplayNames() {
        assertEquals("Draft", WorkstreamStatus.DRAFT.getName());
        assertEquals("Planning", WorkstreamStatus.PLANNING.getName());
        assertEquals("Active", WorkstreamStatus.ACTIVE.getName());
        assertEquals("Paused", WorkstreamStatus.PAUSED.getName());
        assertEquals("Completed", WorkstreamStatus.COMPLETED.getName());
        assertEquals("Cancelled", WorkstreamStatus.CANCELLED.getName());
    }
    
    @Test
    @DisplayName("Should have correct descriptions")
    void shouldHaveCorrectDescriptions() {
        assertTrue(WorkstreamStatus.DRAFT.getDescription().contains("Initial creation"));
        assertTrue(WorkstreamStatus.PLANNING.getDescription().contains("Defining scope"));
        assertTrue(WorkstreamStatus.ACTIVE.getDescription().contains("Work items are actively"));
        assertTrue(WorkstreamStatus.PAUSED.getDescription().contains("Temporarily"));
        assertTrue(WorkstreamStatus.COMPLETED.getDescription().contains("All work items are done"));
        assertTrue(WorkstreamStatus.CANCELLED.getDescription().contains("Discontinued"));
    }
    
    @Test
    @DisplayName("Should correctly identify active status")
    void shouldCorrectlyIdentifyActiveStatus() {
        assertTrue(WorkstreamStatus.ACTIVE.isActive());
        assertFalse(WorkstreamStatus.DRAFT.isActive());
        assertFalse(WorkstreamStatus.PLANNING.isActive());
        assertFalse(WorkstreamStatus.PAUSED.isActive());
        assertFalse(WorkstreamStatus.COMPLETED.isActive());
        assertFalse(WorkstreamStatus.CANCELLED.isActive());
    }
    
    @Test
    @DisplayName("Should correctly identify terminal statuses")
    void shouldCorrectlyIdentifyTerminalStatuses() {
        assertTrue(WorkstreamStatus.COMPLETED.isTerminal());
        assertTrue(WorkstreamStatus.CANCELLED.isTerminal());
        assertFalse(WorkstreamStatus.DRAFT.isTerminal());
        assertFalse(WorkstreamStatus.PLANNING.isTerminal());
        assertFalse(WorkstreamStatus.ACTIVE.isTerminal());
        assertFalse(WorkstreamStatus.PAUSED.isTerminal());
    }
    
    @Test
    @DisplayName("Should validate valid status transitions")
    void shouldValidateValidStatusTransitions() {
        // DRAFT can transition to PLANNING, ACTIVE, CANCELLED, or itself
        assertTrue(WorkstreamStatus.DRAFT.canTransitionTo(WorkstreamStatus.DRAFT));
        assertTrue(WorkstreamStatus.DRAFT.canTransitionTo(WorkstreamStatus.PLANNING));
        assertTrue(WorkstreamStatus.DRAFT.canTransitionTo(WorkstreamStatus.ACTIVE));
        assertTrue(WorkstreamStatus.DRAFT.canTransitionTo(WorkstreamStatus.CANCELLED));
        assertFalse(WorkstreamStatus.DRAFT.canTransitionTo(WorkstreamStatus.PAUSED));
        assertFalse(WorkstreamStatus.DRAFT.canTransitionTo(WorkstreamStatus.COMPLETED));
        
        // PLANNING can transition to ACTIVE, CANCELLED, or itself
        assertTrue(WorkstreamStatus.PLANNING.canTransitionTo(WorkstreamStatus.PLANNING));
        assertTrue(WorkstreamStatus.PLANNING.canTransitionTo(WorkstreamStatus.ACTIVE));
        assertTrue(WorkstreamStatus.PLANNING.canTransitionTo(WorkstreamStatus.CANCELLED));
        assertFalse(WorkstreamStatus.PLANNING.canTransitionTo(WorkstreamStatus.DRAFT));
        assertFalse(WorkstreamStatus.PLANNING.canTransitionTo(WorkstreamStatus.PAUSED));
        assertFalse(WorkstreamStatus.PLANNING.canTransitionTo(WorkstreamStatus.COMPLETED));
        
        // ACTIVE can transition to PAUSED, COMPLETED, CANCELLED, or itself
        assertTrue(WorkstreamStatus.ACTIVE.canTransitionTo(WorkstreamStatus.ACTIVE));
        assertTrue(WorkstreamStatus.ACTIVE.canTransitionTo(WorkstreamStatus.PAUSED));
        assertTrue(WorkstreamStatus.ACTIVE.canTransitionTo(WorkstreamStatus.COMPLETED));
        assertTrue(WorkstreamStatus.ACTIVE.canTransitionTo(WorkstreamStatus.CANCELLED));
        assertFalse(WorkstreamStatus.ACTIVE.canTransitionTo(WorkstreamStatus.DRAFT));
        assertFalse(WorkstreamStatus.ACTIVE.canTransitionTo(WorkstreamStatus.PLANNING));
        
        // PAUSED can transition to ACTIVE, CANCELLED, or itself
        assertTrue(WorkstreamStatus.PAUSED.canTransitionTo(WorkstreamStatus.PAUSED));
        assertTrue(WorkstreamStatus.PAUSED.canTransitionTo(WorkstreamStatus.ACTIVE));
        assertTrue(WorkstreamStatus.PAUSED.canTransitionTo(WorkstreamStatus.CANCELLED));
        assertFalse(WorkstreamStatus.PAUSED.canTransitionTo(WorkstreamStatus.DRAFT));
        assertFalse(WorkstreamStatus.PAUSED.canTransitionTo(WorkstreamStatus.PLANNING));
        assertFalse(WorkstreamStatus.PAUSED.canTransitionTo(WorkstreamStatus.COMPLETED));
        
        // COMPLETED is terminal, can only transition to itself
        assertTrue(WorkstreamStatus.COMPLETED.canTransitionTo(WorkstreamStatus.COMPLETED));
        assertFalse(WorkstreamStatus.COMPLETED.canTransitionTo(WorkstreamStatus.DRAFT));
        assertFalse(WorkstreamStatus.COMPLETED.canTransitionTo(WorkstreamStatus.PLANNING));
        assertFalse(WorkstreamStatus.COMPLETED.canTransitionTo(WorkstreamStatus.ACTIVE));
        assertFalse(WorkstreamStatus.COMPLETED.canTransitionTo(WorkstreamStatus.PAUSED));
        assertFalse(WorkstreamStatus.COMPLETED.canTransitionTo(WorkstreamStatus.CANCELLED));
        
        // CANCELLED is terminal, can only transition to itself
        assertTrue(WorkstreamStatus.CANCELLED.canTransitionTo(WorkstreamStatus.CANCELLED));
        assertFalse(WorkstreamStatus.CANCELLED.canTransitionTo(WorkstreamStatus.DRAFT));
        assertFalse(WorkstreamStatus.CANCELLED.canTransitionTo(WorkstreamStatus.PLANNING));
        assertFalse(WorkstreamStatus.CANCELLED.canTransitionTo(WorkstreamStatus.ACTIVE));
        assertFalse(WorkstreamStatus.CANCELLED.canTransitionTo(WorkstreamStatus.PAUSED));
        assertFalse(WorkstreamStatus.CANCELLED.canTransitionTo(WorkstreamStatus.COMPLETED));
    }
    
    @Test
    @DisplayName("Should correctly parse from string")
    void shouldCorrectlyParseFromString() {
        // Parse by enum name
        assertEquals(WorkstreamStatus.DRAFT, WorkstreamStatus.fromString("DRAFT"));
        assertEquals(WorkstreamStatus.PLANNING, WorkstreamStatus.fromString("PLANNING"));
        assertEquals(WorkstreamStatus.ACTIVE, WorkstreamStatus.fromString("ACTIVE"));
        assertEquals(WorkstreamStatus.PAUSED, WorkstreamStatus.fromString("PAUSED"));
        assertEquals(WorkstreamStatus.COMPLETED, WorkstreamStatus.fromString("COMPLETED"));
        assertEquals(WorkstreamStatus.CANCELLED, WorkstreamStatus.fromString("CANCELLED"));
        
        // Parse by display name
        assertEquals(WorkstreamStatus.DRAFT, WorkstreamStatus.fromString("Draft"));
        assertEquals(WorkstreamStatus.PLANNING, WorkstreamStatus.fromString("Planning"));
        assertEquals(WorkstreamStatus.ACTIVE, WorkstreamStatus.fromString("Active"));
        assertEquals(WorkstreamStatus.PAUSED, WorkstreamStatus.fromString("Paused"));
        assertEquals(WorkstreamStatus.COMPLETED, WorkstreamStatus.fromString("Completed"));
        assertEquals(WorkstreamStatus.CANCELLED, WorkstreamStatus.fromString("Cancelled"));
        
        // Case insensitive
        assertEquals(WorkstreamStatus.DRAFT, WorkstreamStatus.fromString("draft"));
        assertEquals(WorkstreamStatus.PLANNING, WorkstreamStatus.fromString("planning"));
        
        // Unknown status
        assertThrows(IllegalArgumentException.class, () -> WorkstreamStatus.fromString("UNKNOWN"));
    }
    
    @Test
    @DisplayName("Should use name in toString()")
    void shouldUseNameInToString() {
        assertEquals("Draft", WorkstreamStatus.DRAFT.toString());
        assertEquals("Planning", WorkstreamStatus.PLANNING.toString());
        assertEquals("Active", WorkstreamStatus.ACTIVE.toString());
        assertEquals("Paused", WorkstreamStatus.PAUSED.toString());
        assertEquals("Completed", WorkstreamStatus.COMPLETED.toString());
        assertEquals("Cancelled", WorkstreamStatus.CANCELLED.toString());
    }
}