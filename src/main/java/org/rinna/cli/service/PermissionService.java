/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.rinna.domain.PermissionLevel;

/**
 * Service for handling permissions in the system.
 */
public interface PermissionService {
    
    /**
     * Gets the permission level of the current user.
     *
     * @return the permission level
     */
    PermissionLevel getCurrentUserPermissionLevel();
    
    /**
     * Gets the permission level of a specific user.
     *
     * @param username the username
     * @return the permission level
     */
    PermissionLevel getUserPermissionLevel(String username);
    
    /**
     * Checks if the current user can perform a specific action.
     *
     * @param action the action to check
     * @return true if the user can perform the action, false otherwise
     */
    boolean canPerform(String action);
    
    /**
     * Checks if a specific user can perform a specific action.
     *
     * @param username the username
     * @param action the action to check
     * @return true if the user can perform the action, false otherwise
     */
    boolean canUserPerform(String username, String action);
}