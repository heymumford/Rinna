/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Categorizes work items by their originating domain.
 * This helps in organizing and filtering work items based on their core focus area.
 */
public enum OriginCategory {
    /**
     * Product-focused work, typically centered on customer value and features.
     */
    PROD("Product"),
    
    /**
     * Architecture-focused work, dealing with system design and patterns.
     */
    ARCH("Architecture"),
    
    /**
     * Development-focused work, related to implementation and code.
     */
    DEV("Development"),
    
    /**
     * Test-focused work, related to verification and quality.
     */
    TEST("Testing"),
    
    /**
     * Operations-focused work, related to deployment and monitoring.
     */
    OPS("Operations"),
    
    /**
     * Documentation-focused work.
     */
    DOC("Documentation"),
    
    /**
     * Cross-functional work that spans multiple domains.
     */
    CROSS("Cross-functional");
    
    private final String displayName;
    
    OriginCategory(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Returns a user-friendly display name for the category.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}