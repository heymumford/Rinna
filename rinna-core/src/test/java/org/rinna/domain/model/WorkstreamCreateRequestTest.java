/*
 * Unit tests for the WorkstreamCreateRequest class
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
 * Unit tests for the WorkstreamCreateRequest class.
 */
@DisplayName("WorkstreamCreateRequest Tests")
public class WorkstreamCreateRequestTest extends UnitTest {
    
    @Test
    @DisplayName("Should create a valid request using constructor")
    void shouldCreateValidRequestUsingConstructor() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        Instant targetDate = Instant.now().plus(30, ChronoUnit.DAYS);
        
        // Act
        WorkstreamCreateRequest request = new WorkstreamCreateRequest(
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.DRAFT.name(),
            Priority.HIGH,
            orgId,
            CynefinDomain.COMPLICATED,
            true,
            targetDate
        );
        
        // Assert
        assertEquals("Authentication Workstream", request.name());
        assertEquals("Implement authentication across all products", request.description());
        assertEquals("product.owner@example.com", request.owner());
        assertEquals(WorkstreamStatus.DRAFT.name(), request.status());
        assertEquals(Priority.HIGH, request.priority());
        assertTrue(request.getOrganizationId().isPresent());
        assertEquals(orgId, request.getOrganizationId().get());
        assertTrue(request.getCynefinDomain().isPresent());
        assertEquals(CynefinDomain.COMPLICATED, request.getCynefinDomain().get());
        assertTrue(request.crossProject());
        assertTrue(request.getTargetDate().isPresent());
        assertEquals(targetDate, request.getTargetDate().get());
    }
    
    @Test
    @DisplayName("Should fail with null required fields")
    void shouldFailWithNullRequiredFields() {
        // Assert that constructor throws NPE for null required fields
        assertThrows(NullPointerException.class, () -> new WorkstreamCreateRequest(
            null, // null name
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.DRAFT.name(),
            Priority.HIGH,
            null,
            null,
            true,
            null
        ));
        
        assertThrows(NullPointerException.class, () -> new WorkstreamCreateRequest(
            "Authentication Workstream",
            "Implement authentication across all products",
            null, // null owner
            WorkstreamStatus.DRAFT.name(),
            Priority.HIGH,
            null,
            null,
            true,
            null
        ));
        
        assertThrows(NullPointerException.class, () -> new WorkstreamCreateRequest(
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            null, // null status
            Priority.HIGH,
            null,
            null,
            true,
            null
        ));
        
        assertThrows(NullPointerException.class, () -> new WorkstreamCreateRequest(
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.DRAFT.name(),
            null, // null priority
            null,
            null,
            true,
            null
        ));
    }
    
    @Test
    @DisplayName("Should handle null optional fields")
    void shouldHandleNullOptionalFields() {
        // Act
        WorkstreamCreateRequest request = new WorkstreamCreateRequest(
            "Authentication Workstream",
            "Implement authentication across all products",
            "product.owner@example.com",
            WorkstreamStatus.DRAFT.name(),
            Priority.HIGH,
            null, // null organization ID
            null, // null CYNEFIN domain
            true,
            null  // null target date
        );
        
        // Assert
        assertTrue(request.getOrganizationId().isEmpty());
        assertTrue(request.getCynefinDomain().isEmpty());
        assertTrue(request.getTargetDate().isEmpty());
    }
    
    @Test
    @DisplayName("Should create a valid request using builder")
    void shouldCreateValidRequestUsingBuilder() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        Instant targetDate = Instant.now().plus(30, ChronoUnit.DAYS);
        
        // Act
        WorkstreamCreateRequest request = new WorkstreamCreateRequest.Builder()
            .name("Authentication Workstream")
            .description("Implement authentication across all products")
            .owner("product.owner@example.com")
            .status(WorkstreamStatus.PLANNING)
            .priority(Priority.HIGH)
            .organizationId(orgId)
            .cynefinDomain(CynefinDomain.COMPLICATED)
            .crossProject(true)
            .targetDate(targetDate)
            .build();
        
        // Assert
        assertEquals("Authentication Workstream", request.name());
        assertEquals("Implement authentication across all products", request.description());
        assertEquals("product.owner@example.com", request.owner());
        assertEquals(WorkstreamStatus.PLANNING.name(), request.status());
        assertEquals(Priority.HIGH, request.priority());
        assertTrue(request.getOrganizationId().isPresent());
        assertEquals(orgId, request.getOrganizationId().get());
        assertTrue(request.getCynefinDomain().isPresent());
        assertEquals(CynefinDomain.COMPLICATED, request.getCynefinDomain().get());
        assertTrue(request.crossProject());
        assertTrue(request.getTargetDate().isPresent());
        assertEquals(targetDate, request.getTargetDate().get());
    }
    
    @Test
    @DisplayName("Builder should set default values")
    void builderShouldSetDefaultValues() {
        // Act - use minimal required fields
        WorkstreamCreateRequest request = new WorkstreamCreateRequest.Builder()
            .name("Authentication Workstream")
            .owner("product.owner@example.com")
            .build();
        
        // Assert default values were applied
        assertEquals(WorkstreamStatus.DRAFT.name(), request.status());
        assertEquals(Priority.MEDIUM, request.priority());
        assertFalse(request.crossProject());
        assertTrue(request.getOrganizationId().isEmpty());
        assertTrue(request.getCynefinDomain().isEmpty());
        assertTrue(request.getTargetDate().isEmpty());
    }
    
    @Test
    @DisplayName("Builder should throw when required fields are missing")
    void builderShouldThrowWhenRequiredFieldsAreMissing() {
        // Missing name
        WorkstreamCreateRequest.Builder builderMissingName = new WorkstreamCreateRequest.Builder()
            .owner("product.owner@example.com");
        assertThrows(NullPointerException.class, builderMissingName::build);
        
        // Missing owner
        WorkstreamCreateRequest.Builder builderMissingOwner = new WorkstreamCreateRequest.Builder()
            .name("Authentication Workstream");
        assertThrows(NullPointerException.class, builderMissingOwner::build);
    }
}