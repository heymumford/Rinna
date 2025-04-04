/*
 * Domain service test for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.rinna.adapter.persistence.InMemoryReleaseRepository;
import org.rinna.adapter.service.DefaultReleaseService;
import org.rinna.domain.entity.Release;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.repository.ReleaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the ReleaseService interface.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReleaseServiceTest {
    
    private ReleaseService releaseService;
    
    @Mock
    private ItemService itemService;
    
    private ReleaseRepository releaseRepository;
    
    @BeforeEach
    void setUp() {
        // Create real implementations
        releaseRepository = new InMemoryReleaseRepository();
        releaseService = new DefaultReleaseService(releaseRepository, itemService);
    }
    
    @Test
    @DisplayName("Should create a new release with valid version")
    void shouldCreateNewReleaseWithValidVersion() {
        // Arrange
        String version = "1.0.0";
        String description = "Initial release";
        
        // Act
        Release release = releaseService.createRelease(version, description);
        
        // Assert
        assertNotNull(release);
        assertEquals(version, release.getVersion());
        assertEquals(description, release.getDescription());
        assertNotNull(release.getId());
        assertNotNull(release.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should throw exception when creating a release with invalid version format")
    void shouldThrowExceptionWhenCreatingReleaseWithInvalidVersionFormat() {
        // Arrange
        String version = "1.0";
        String description = "Invalid version format";
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            releaseService.createRelease(version, description);
        });
    }
    
    @Test
    @DisplayName("Should create a minor version from existing release")
    void shouldCreateMinorVersionFromExistingRelease() {
        // Arrange
        String originalVersion = "1.0.0";
        String description = "Initial release";
        Release originalRelease = releaseService.createRelease(originalVersion, description);
        
        // Act
        Release minorRelease = releaseService.createNextMinorVersion(originalRelease.getId(), "Minor release");
        
        // Assert
        assertEquals("1.1.0", minorRelease.getVersion());
    }
    
    @Test
    @DisplayName("Should create a patch version from existing release")
    void shouldCreatePatchVersionFromExistingRelease() {
        // Arrange
        String originalVersion = "1.1.0";
        String description = "Minor release";
        Release originalRelease = releaseService.createRelease(originalVersion, description);
        
        // Act
        Release patchRelease = releaseService.createNextPatchVersion(originalRelease.getId(), "Patch release");
        
        // Assert
        assertEquals("1.1.1", patchRelease.getVersion());
    }
    
    @Test
    @DisplayName("Should create a major version from existing release")
    void shouldCreateMajorVersionFromExistingRelease() {
        // Arrange
        String originalVersion = "1.1.0";
        String description = "Minor release";
        Release originalRelease = releaseService.createRelease(originalVersion, description);
        
        // Act
        Release majorRelease = releaseService.createNextMajorVersion(originalRelease.getId(), "Major release");
        
        // Assert
        assertEquals("2.0.0", majorRelease.getVersion());
    }
    
    @Test
    @DisplayName("Should add a work item to a release")
    void shouldAddWorkItemToRelease() {
        // Arrange
        String version = "1.0.0";
        String description = "Initial release";
        Release release = releaseService.createRelease(version, description);
        
        UUID workItemId = UUID.randomUUID();
        WorkItem workItem = mock(WorkItem.class);
        when(workItem.getId()).thenReturn(workItemId);
        when(itemService.findById(workItemId)).thenReturn(Optional.of(workItem));
        
        // Act
        releaseService.addWorkItem(release.getId(), workItemId);
        
        // Assert
        assertTrue(releaseService.containsWorkItem(release.getId(), workItemId));
    }
    
    @Test
    @DisplayName("Should remove a work item from a release")
    void shouldRemoveWorkItemFromRelease() {
        // Arrange
        String version = "1.0.0";
        String description = "Initial release";
        Release release = releaseService.createRelease(version, description);
        
        UUID workItemId = UUID.randomUUID();
        WorkItem workItem = mock(WorkItem.class);
        when(workItem.getId()).thenReturn(workItemId);
        when(itemService.findById(workItemId)).thenReturn(Optional.of(workItem));
        
        releaseService.addWorkItem(release.getId(), workItemId);
        assertTrue(releaseService.containsWorkItem(release.getId(), workItemId));
        
        // Act
        releaseService.removeWorkItem(release.getId(), workItemId);
        
        // Assert
        assertFalse(releaseService.containsWorkItem(release.getId(), workItemId));
    }
    
    @Test
    @DisplayName("Should find a release by ID")
    void shouldFindReleaseById() {
        // Arrange
        String version = "1.0.0";
        String description = "Initial release";
        Release release = releaseService.createRelease(version, description);
        
        // Act
        Optional<Release> found = releaseService.findById(release.getId());
        
        // Assert
        assertTrue(found.isPresent());
        assertEquals(release.getId(), found.get().getId());
    }
    
    @Test
    @DisplayName("Should find a release by version")
    void shouldFindReleaseByVersion() {
        // Arrange
        String version = "1.0.0";
        String description = "Initial release";
        Release release = releaseService.createRelease(version, description);
        
        // Act
        Optional<Release> found = releaseService.findByVersion(version);
        
        // Assert
        assertTrue(found.isPresent());
        assertEquals(version, found.get().getVersion());
    }
    
    @Test
    @DisplayName("Should find all releases")
    void shouldFindAllReleases() {
        // Arrange
        releaseService.createRelease("1.0.0", "First release");
        releaseService.createRelease("1.1.0", "Second release");
        
        // Act
        List<Release> releases = releaseService.findAll();
        
        // Assert
        assertEquals(2, releases.size());
    }
    
    @Test
    @DisplayName("Should throw exception when patch number exceeds limit")
    void shouldThrowExceptionWhenPatchNumberExceedsLimit() {
        // Arrange
        Release release = releaseService.createRelease("1.0.999", "Max patch version");
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            releaseService.createNextPatchVersion(release.getId(), "Beyond max patch");
        });
    }
    
    @Test
    @DisplayName("Should get work items in a release")
    void shouldGetWorkItemsInRelease() {
        // Arrange
        String version = "1.0.0";
        String description = "Initial release";
        Release release = releaseService.createRelease(version, description);
        
        UUID workItemId1 = UUID.randomUUID();
        UUID workItemId2 = UUID.randomUUID();
        WorkItem workItem1 = mock(WorkItem.class);
        WorkItem workItem2 = mock(WorkItem.class);
        
        when(workItem1.getId()).thenReturn(workItemId1);
        when(workItem2.getId()).thenReturn(workItemId2);
        when(itemService.findById(workItemId1)).thenReturn(Optional.of(workItem1));
        when(itemService.findById(workItemId2)).thenReturn(Optional.of(workItem2));
        
        releaseService.addWorkItem(release.getId(), workItemId1);
        releaseService.addWorkItem(release.getId(), workItemId2);
        
        // Act
        List<WorkItem> items = releaseService.getWorkItems(release.getId());
        
        // Assert
        assertEquals(2, items.size());
        assertTrue(items.contains(workItem1));
        assertTrue(items.contains(workItem2));
    }
}