/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Represents the possible statuses of a workstream in the Rinna system.
 */
public enum WorkstreamStatus {
    /**
     * The workstream is in the initial drafting phase.
     */
    DRAFT("Draft", "Initial creation, not yet ready for planning"),
    
    /**
     * The workstream is in the planning phase.
     */
    PLANNING("Planning", "Defining scope, outcomes, and key results"),
    
    /**
     * The workstream is active and work is in progress.
     */
    ACTIVE("Active", "Work items are actively being worked on"),
    
    /**
     * The workstream has been paused.
     */
    PAUSED("Paused", "Temporarily on hold"),
    
    /**
     * The workstream has been completed.
     */
    COMPLETED("Completed", "All work items are done and outcomes achieved"),
    
    /**
     * The workstream has been cancelled.
     */
    CANCELLED("Cancelled", "Discontinued without completion");
    
    private final String name;
    private final String description;
    
    /**
     * Constructor.
     *
     * @param name the display name of the status
     * @param description a brief description of the status
     */
    WorkstreamStatus(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    /**
     * Returns the display name of the status.
     *
     * @return the display name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the description of the status.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if the workstream is in an active status.
     *
     * @return true if the workstream is active
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if the workstream is in a terminal status.
     *
     * @return true if the workstream is in a terminal status
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
    
    /**
     * Returns whether a transition to the given status is valid.
     *
     * @param targetStatus the target status
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(WorkstreamStatus targetStatus) {
        if (this == targetStatus) {
            return true; // Can stay in the same status
        }
        
        return switch (this) {
            case DRAFT -> targetStatus == PLANNING || targetStatus == ACTIVE || targetStatus == CANCELLED;
            case PLANNING -> targetStatus == ACTIVE || targetStatus == CANCELLED;
            case ACTIVE -> targetStatus == PAUSED || targetStatus == COMPLETED || targetStatus == CANCELLED;
            case PAUSED -> targetStatus == ACTIVE || targetStatus == CANCELLED;
            case COMPLETED, CANCELLED -> false; // Terminal states
        };
    }
    
    /**
     * Parses a string to a WorkstreamStatus.
     *
     * @param status the string to parse
     * @return the corresponding WorkstreamStatus
     * @throws IllegalArgumentException if the string doesn't match any status
     */
    public static WorkstreamStatus fromString(String status) {
        for (WorkstreamStatus s : values()) {
            if (s.name().equalsIgnoreCase(status) || s.getName().equalsIgnoreCase(status)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown workstream status: " + status);
    }
    
    @Override
    public String toString() {
        return name;
    }
}