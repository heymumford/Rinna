/*
 * Domain repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import org.rinna.domain.model.Release;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Release entities.
 */
public interface ReleaseRepository {
    
    /**
     * Saves a release.
     *
     * @param release the release to save
     * @return the saved release
     */
    Release save(Release release);
    
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
    
    /**
     * Deletes a release by its ID.
     *
     * @param id the ID of the release to delete
     */
    void deleteById(UUID id);
}