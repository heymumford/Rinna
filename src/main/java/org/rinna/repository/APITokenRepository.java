/*
 * Domain repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import org.rinna.domain.APIToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for APIToken entities.
 */
public interface APITokenRepository {
    
    /**
     * Saves an API token.
     *
     * @param token the token to save
     * @return the saved token
     */
    APIToken save(APIToken token);
    
    /**
     * Finds a token by ID.
     *
     * @param id the token ID
     * @return the token, if found
     */
    Optional<APIToken> findById(UUID id);
    
    /**
     * Finds a token by value.
     *
     * @param token the token value
     * @return the token, if found
     */
    Optional<APIToken> findByToken(String token);
    
    /**
     * Returns all tokens for a project.
     *
     * @param projectId the project ID
     * @return a list of tokens for the project
     */
    List<APIToken> findByProjectId(UUID projectId);
    
    /**
     * Returns all tokens.
     *
     * @return a list of all tokens
     */
    List<APIToken> findAll();
    
    /**
     * Returns all active tokens.
     *
     * @return a list of all active tokens
     */
    List<APIToken> findAllActive();
    
    /**
     * Deletes a token by ID.
     *
     * @param id the token ID
     */
    void deleteById(UUID id);
    
    /**
     * Deletes a token by value.
     *
     * @param token the token value
     */
    void deleteByToken(String token);
}