/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import org.rinna.domain.model.Release;
import org.rinna.domain.model.WorkItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing releases.
 * This interface defines the application use cases for release management.
 */
public interface ReleaseService {
    
    /**
     * Creates a new release with the specified version and description.
     * Version must follow semantic versioning (major.minor.patch).
     *
     * @param version the semantic version of the release
     * @param description the description of the release
     * @return the created release
     * @throws IllegalArgumentException if the version format is invalid
     */
    Release createRelease(String version, String description);
    
    /**
     * Creates a new minor version release from an existing release.
     * Increments the minor version number and resets the patch number to 0.
     *
     * @param releaseId the ID of the base release
     * @param description the description of the new release
     * @return the created release
     * @throws IllegalArgumentException if the release with the given ID does not exist
     */
    Release createNextMinorVersion(UUID releaseId, String description);
    
    /**
     * Creates a new patch version release from an existing release.
     * Increments the patch version number.
     * Maximum patch number is 999.
     *
     * @param releaseId the ID of the base release
     * @param description the description of the new release
     * @return the created release
     * @throws IllegalArgumentException if the release with the given ID does not exist
     * @throws IllegalStateException if the patch number would exceed the maximum
     */
    Release createNextPatchVersion(UUID releaseId, String description);
    
    /**
     * Creates a new major version release from an existing release.
     * Increments the major version number and resets the minor and patch numbers to 0.
     *
     * @param releaseId the ID of the base release
     * @param description the description of the new release
     * @return the created release
     * @throws IllegalArgumentException if the release with the given ID does not exist
     */
    Release createNextMajorVersion(UUID releaseId, String description);
    
    /**
     * Adds a work item to a release.
     *
     * @param releaseId the ID of the release
     * @param workItemId the ID of the work item to add
     * @throws IllegalArgumentException if the release or work item does not exist
     */
    void addWorkItem(UUID releaseId, UUID workItemId);
    
    /**
     * Removes a work item from a release.
     *
     * @param releaseId the ID of the release
     * @param workItemId the ID of the work item to remove
     * @throws IllegalArgumentException if the release does not exist
     */
    void removeWorkItem(UUID releaseId, UUID workItemId);
    
    /**
     * Checks if a release contains a specific work item.
     *
     * @param releaseId the ID of the release
     * @param workItemId the ID of the work item
     * @return true if the release contains the work item, false otherwise
     */
    boolean containsWorkItem(UUID releaseId, UUID workItemId);
    
    /**
     * Gets all work items in a release.
     *
     * @param releaseId the ID of the release
     * @return a list of work items in the release
     * @throws IllegalArgumentException if the release does not exist
     */
    List<WorkItem> getWorkItems(UUID releaseId);
    
    /**
     * Finds a release by its ID.
     *
     * @param id the ID of the release
     * @return an Optional containing the release, or empty if not found
     */
    Optional<Release> findById(UUID id);
    
    /**
     * Finds a release by its version.
     *
     * @param version the version of the release
     * @return an Optional containing the release, or empty if not found
     */
    Optional<Release> findByVersion(String version);
    
    /**
     * Finds all releases.
     *
     * @return a list of all releases
     */
    List<Release> findAll();
}