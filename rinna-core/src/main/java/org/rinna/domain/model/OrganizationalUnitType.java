/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Represents the types of organizational units in the Rinna system.
 */
public enum OrganizationalUnitType {
    /**
     * A formal department within an organization.
     */
    DEPARTMENT("Department", "A formal department within an organization"),
    
    /**
     * A cross-functional team.
     */
    TEAM("Team", "A cross-functional team"),
    
    /**
     * A product-focused team.
     */
    PRODUCT_TEAM("Product Team", "A team focused on a specific product"),
    
    /**
     * A project-focused team.
     */
    PROJECT_TEAM("Project Team", "A team assembled for a specific project"),
    
    /**
     * A squad in an agile organization.
     */
    SQUAD("Squad", "A small, cross-functional team in an agile organization"),
    
    /**
     * A tribe in an agile organization.
     */
    TRIBE("Tribe", "A collection of squads working on related areas"),
    
    /**
     * A guild in an agile organization.
     */
    GUILD("Guild", "A community of interest across the organization"),
    
    /**
     * A chapter in an agile organization.
     */
    CHAPTER("Chapter", "A functional area in an agile organization"),
    
    /**
     * A business unit.
     */
    BUSINESS_UNIT("Business Unit", "A major division of the organization"),
    
    /**
     * A virtual team that spans organizational boundaries.
     */
    VIRTUAL_TEAM("Virtual Team", "A team that spans organizational boundaries"),
    
    /**
     * A custom organizational unit type.
     */
    CUSTOM("Custom", "A custom organizational unit type");
    
    private final String displayName;
    private final String description;
    
    /**
     * Creates a new OrganizationalUnitType.
     *
     * @param displayName the display name
     * @param description the description
     */
    OrganizationalUnitType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Returns the display name of this organizational unit type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Returns the description of this organizational unit type.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the OrganizationalUnitType for a given display name.
     *
     * @param displayName the display name
     * @return the corresponding OrganizationalUnitType, or CUSTOM if not found
     */
    public static OrganizationalUnitType fromDisplayName(String displayName) {
        for (OrganizationalUnitType type : values()) {
            if (type.getDisplayName().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        return CUSTOM;
    }
    
    /**
     * Returns the OrganizationalUnitType for a given name.
     *
     * @param name the name (case-insensitive)
     * @return the corresponding OrganizationalUnitType, or CUSTOM if not found
     */
    public static OrganizationalUnitType fromName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }
}