/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.domain;

/**
 * Enum for permission levels in the system.
 */
public enum PermissionLevel {
    /**
     * Read-only access. Can view but not modify.
     */
    READONLY,
    
    /**
     * Regular user access. Can create and modify own items.
     */
    USER,
    
    /**
     * Standard access. Can create, modify, and manage items and projects.
     */
    STANDARD,
    
    /**
     * Admin access. Full system access.
     */
    ADMIN
}