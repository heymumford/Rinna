/*
 * Unit tests for the WorkstreamRecord class
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.base.UnitTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the WorkstreamRecord class.
 */
@DisplayName("WorkstreamRecord Tests")
public class WorkstreamRecordTest extends UnitTest {
    
    @Test
    @DisplayName("Should create a valid workstream record")
    void shouldCreateValidWorkstreamRecord() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant targetDate = now.plus(30, ChronoUnit.DAYS);
        
        // Act
        WorkstreamRecord workstream = new WorkstreamRecord(
            id,
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            now,
            now,
            orgId,
            CynefinDomain.COMPLICATED,
            true,
            targetDate
        );
        
        // Assert
        assertEquals(id, workstream.getId());
        assertEquals("Authentication Workstream", workstream.getName());
        assertEquals("Implement authentication across all products", workstream.getDescription());
        assertEquals("product.owner@example.com", workstream.getOwner());
        assertEquals(WorkstreamStatus.ACTIVE.name(), workstream.getStatus());
        assertEquals(Priority.HIGH, workstream.getPriority());
        assertEquals(now, workstream.getCreatedAt());
        assertEquals(now, workstream.getUpdatedAt());
        assertTrue(workstream.getOrganizationId().isPresent());
        assertEquals(orgId, workstream.getOrganizationId().get());
        assertTrue(workstream.getCynefinDomain().isPresent());
        assertEquals(CynefinDomain.COMPLICATED, workstream.getCynefinDomain().get());
        assertTrue(workstream.isCrossProject());
        assertTrue(workstream.getTargetDate().isPresent());
        assertEquals(targetDate, workstream.getTargetDate().get());
    }
    
    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        // Act
        WorkstreamRecord workstream = new WorkstreamRecord(
            id,
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            now,
            now,
            null, // null organization ID
            null, // null CYNEFIN domain
            true,
            null  // null target date
        );
        
        // Assert
        assertTrue(workstream.getOrganizationId().isEmpty());
        assertTrue(workstream.getCynefinDomain().isEmpty());
        assertTrue(workstream.getTargetDate().isEmpty());
    }
    
    @Test
    @DisplayName("Should fail with null required fields")
    void shouldFailWithNullRequiredFields() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        // Assert that constructor throws NPE for null required fields
        assertThrows(NullPointerException.class, () -> new WorkstreamRecord(
            null, // null ID
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            now,
            now,
            null,
            null,
            true,
            null
        ));
        
        assertThrows(NullPointerException.class, () -> new WorkstreamRecord(
            id,
            null, // null name
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            now,
            now,
            null,
            null,
            true,
            null
        ));
        
        assertThrows(NullPointerException.class, () -> new WorkstreamRecord(
            id,
            "Authentication Workstream",
            "Implement authentication across all products",
            null, // null owner
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            now,
            now,
            null,
            null,
            true,
            null
        ));
        
        assertThrows(NullPointerException.class, () -> new WorkstreamRecord(
            id,
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            null, // null status
            Priority.HIGH,
            now,
            now,
            null,
            null,
            true,
            null
        ));
        
        assertThrows(NullPointerException.class, () -> new WorkstreamRecord(
            id,
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.ACTIVE.name(),
            null, // null priority
            now,
            now,
            null,
            null,
            true,
            null
        ));
        
        assertThrows(NullPointerException.class, () -> new WorkstreamRecord(
            id,
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            null, // null created timestamp
            now,
            null,
            null,
            true,
            null
        ));
        
        assertThrows(NullPointerException.class, () -> new WorkstreamRecord(
            id,
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.ACTIVE.name(),
            Priority.HIGH,
            now,
            null, // null updated timestamp
            null,
            null,
            true,
            null
        ));
    }
    
    @Test
    @DisplayName("Should create updated records with correct timestamps")
    void shouldCreateUpdatedRecordsWithCorrectTimestamps() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant earlier = now.minus(1, ChronoUnit.HOURS);
        
        WorkstreamRecord original = new WorkstreamRecord(
            id,
            "Original Name",
            "Original Description",
            "original.owner@example.com",
            WorkstreamStatus.DRAFT.name(),
            Priority.MEDIUM,
            earlier, // created an hour ago
            earlier, // last updated an hour ago
            null,
            null,
            false,
            null
        );
        
        // Act - update various fields
        WorkstreamRecord updatedName = original.withName("Updated Name");
        WorkstreamRecord updatedDescription = original.withDescription("Updated Description");
        WorkstreamRecord updatedOwner = original.withOwner("new.owner@example.com");
        WorkstreamRecord updatedStatus = original.withStatus(WorkstreamStatus.ACTIVE.name());
        WorkstreamRecord updatedPriority = original.withPriority(Priority.HIGH);
        WorkstreamRecord updatedOrgId = original.withOrganizationId(UUID.randomUUID());
        WorkstreamRecord updatedDomain = original.withCynefinDomain(CynefinDomain.COMPLEX);
        WorkstreamRecord updatedCrossProject = original.withCrossProject(true);
        WorkstreamRecord updatedTargetDate = original.withTargetDate(now.plus(30, ChronoUnit.DAYS));
        
        // Assert - check fields were updated
        assertEquals("Updated Name", updatedName.getName());
        assertEquals("Updated Description", updatedDescription.getDescription());
        assertEquals("new.owner@example.com", updatedOwner.getOwner());
        assertEquals(WorkstreamStatus.ACTIVE.name(), updatedStatus.getStatus());
        assertEquals(Priority.HIGH, updatedPriority.getPriority());
        assertTrue(updatedOrgId.getOrganizationId().isPresent());
        assertTrue(updatedDomain.getCynefinDomain().isPresent());
        assertEquals(CynefinDomain.COMPLEX, updatedDomain.getCynefinDomain().get());
        assertTrue(updatedCrossProject.isCrossProject());
        assertTrue(updatedTargetDate.getTargetDate().isPresent());
        
        // Assert - check immutable fields were preserved
        assertEquals(id, updatedName.getId());
        assertEquals(earlier, updatedName.getCreatedAt());
        
        // Assert - check updated timestamp was updated
        assertTrue(updatedName.getUpdatedAt().isAfter(earlier));
        assertTrue(updatedDescription.getUpdatedAt().isAfter(earlier));
        assertTrue(updatedOwner.getUpdatedAt().isAfter(earlier));
        assertTrue(updatedStatus.getUpdatedAt().isAfter(earlier));
        assertTrue(updatedPriority.getUpdatedAt().isAfter(earlier));
        assertTrue(updatedOrgId.getUpdatedAt().isAfter(earlier));
        assertTrue(updatedDomain.getUpdatedAt().isAfter(earlier));
        assertTrue(updatedCrossProject.getUpdatedAt().isAfter(earlier));
        assertTrue(updatedTargetDate.getUpdatedAt().isAfter(earlier));
    }
}