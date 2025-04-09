/*
 * Unit test for the InMemoryReleaseRepository
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.DefaultRelease;
import org.rinna.domain.model.Release;
import org.rinna.repository.ReleaseRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InMemoryReleaseRepository}.
 */
class InMemoryReleaseRepositoryTest {

    private ReleaseRepository repository;
    private UUID releaseId1;
    private UUID releaseId2;
    private UUID workItemId1;
    private UUID workItemId2;
    private UUID workItemId3;
    private Release release1;
    private Release release2;
    private Instant now;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReleaseRepository();
        releaseId1 = UUID.randomUUID();
        releaseId2 = UUID.randomUUID();
        workItemId1 = UUID.randomUUID();
        workItemId2 = UUID.randomUUID();
        workItemId3 = UUID.randomUUID();
        now = Instant.now();

        // Create first release
        release1 = new DefaultRelease.Builder()
                .id(releaseId1)
                .version("1.0.0")
                .description("First Release")
                .createdAt(now.minusSeconds(3600)) // 1 hour ago
                .workItems(List.of(workItemId1, workItemId2))
                .build();

        // Create second release
        release2 = new DefaultRelease.Builder()
                .id(releaseId2)
                .version("1.1.0")
                .description("Second Release")
                .createdAt(now)
                .workItems(List.of(workItemId2, workItemId3))
                .build();
    }

    @Test
    void testSaveAndFindById() {
        // Save a release
        Release savedRelease = repository.save(release1);
        
        // Verify saved release
        assertNotNull(savedRelease);
        assertEquals(releaseId1, savedRelease.getId());
        assertEquals("1.0.0", savedRelease.getVersion());
        assertEquals("First Release", savedRelease.getDescription());
        
        // Find by ID
        Optional<Release> foundRelease = repository.findById(releaseId1);
        
        // Verify found release
        assertTrue(foundRelease.isPresent());
        assertEquals(releaseId1, foundRelease.get().getId());
        assertEquals("1.0.0", foundRelease.get().getVersion());
        assertEquals("First Release", foundRelease.get().getDescription());
        assertEquals(2, foundRelease.get().getWorkItems().size());
        assertTrue(foundRelease.get().getWorkItems().contains(workItemId1));
        assertTrue(foundRelease.get().getWorkItems().contains(workItemId2));
    }

    @Test
    void testFindByIdNonExistent() {
        // Find by non-existent ID
        Optional<Release> foundRelease = repository.findById(UUID.randomUUID());
        
        // Verify not found
        assertFalse(foundRelease.isPresent());
    }

    @Test
    void testFindByVersion() {
        // Save releases
        repository.save(release1);
        repository.save(release2);
        
        // Find by version
        Optional<Release> foundRelease = repository.findByVersion("1.1.0");
        
        // Verify found release
        assertTrue(foundRelease.isPresent());
        assertEquals(releaseId2, foundRelease.get().getId());
        assertEquals("1.1.0", foundRelease.get().getVersion());
        assertEquals("Second Release", foundRelease.get().getDescription());
    }

    @Test
    void testFindByVersionNonExistent() {
        // Save a release
        repository.save(release1);
        
        // Find by non-existent version
        Optional<Release> foundRelease = repository.findByVersion("2.0.0");
        
        // Verify not found
        assertFalse(foundRelease.isPresent());
    }

    @Test
    void testFindAll() {
        // Initially repository should be empty
        List<Release> allReleases = repository.findAll();
        assertEquals(0, allReleases.size());
        
        // Save releases
        repository.save(release1);
        repository.save(release2);
        
        // Find all releases
        allReleases = repository.findAll();
        
        // Verify found releases
        assertEquals(2, allReleases.size());
        
        // Verify contents (order not guaranteed)
        boolean foundRelease1 = false;
        boolean foundRelease2 = false;
        
        for (Release release : allReleases) {
            if (release.getId().equals(releaseId1)) {
                foundRelease1 = true;
                assertEquals("1.0.0", release.getVersion());
            } else if (release.getId().equals(releaseId2)) {
                foundRelease2 = true;
                assertEquals("1.1.0", release.getVersion());
            }
        }
        
        assertTrue(foundRelease1, "Release 1 should be in the results");
        assertTrue(foundRelease2, "Release 2 should be in the results");
    }

    @Test
    void testSaveUpdatesExistingRelease() {
        // Save a release
        repository.save(release1);
        
        // Create updated release with same ID
        Release updatedRelease = new DefaultRelease.Builder()
                .id(releaseId1)
                .version("1.0.1")
                .description("Updated First Release")
                .workItems(List.of(workItemId1))
                .build();
        
        // Save updated release
        Release savedRelease = repository.save(updatedRelease);
        
        // Verify saved release
        assertEquals(releaseId1, savedRelease.getId());
        assertEquals("1.0.1", savedRelease.getVersion());
        assertEquals("Updated First Release", savedRelease.getDescription());
        assertEquals(1, savedRelease.getWorkItems().size());
        
        // Verify by finding again
        Optional<Release> foundRelease = repository.findById(releaseId1);
        assertTrue(foundRelease.isPresent());
        assertEquals("1.0.1", foundRelease.get().getVersion());
        assertEquals("Updated First Release", foundRelease.get().getDescription());
        assertEquals(1, foundRelease.get().getWorkItems().size());
    }

    @Test
    void testDeleteById() {
        // Save releases
        repository.save(release1);
        repository.save(release2);
        
        // Verify both exist
        assertEquals(2, repository.findAll().size());
        assertTrue(repository.findById(releaseId1).isPresent());
        assertTrue(repository.findById(releaseId2).isPresent());
        
        // Delete one release
        repository.deleteById(releaseId1);
        
        // Verify only one remains
        assertEquals(1, repository.findAll().size());
        assertFalse(repository.findById(releaseId1).isPresent());
        assertTrue(repository.findById(releaseId2).isPresent());
    }

    @Test
    void testDeleteByIdNonExistent() {
        // Save a release
        repository.save(release1);
        
        // Delete non-existent release
        UUID nonExistentId = UUID.randomUUID();
        repository.deleteById(nonExistentId);
        
        // Verify existing release is still there
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(releaseId1).isPresent());
    }

    @Test
    void testSaveNullRelease() {
        // Attempt to save null release
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }
}