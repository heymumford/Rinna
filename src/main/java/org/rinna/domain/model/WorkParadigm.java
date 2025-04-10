/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Identifies the methodological approach best suited for the work item.
 * This helps determine how to structure and manage different types of work.
 */
public enum WorkParadigm {
    /**
     * Discrete activities with clear completion criteria.
     * Best for well-understood technical work.
     */
    TASK("Task-based", "Well-understood technical work"),
    
    /**
     * User-centered descriptions of needs.
     * Best for feature development with user focus.
     */
    STORY("Story-based", "Feature development with user focus"),
    
    /**
     * Outcome-oriented with flexible implementation.
     * Best for innovation requiring exploration.
     */
    GOAL("Goal-based", "Innovation requiring exploration"),
    
    /**
     * Testing hypotheses and learning.
     * Best for high uncertainty work.
     */
    EXPERIMENT("Experiment-based", "High uncertainty work");
    
    private final String displayName;
    private final String applicability;
    
    WorkParadigm(String displayName, String applicability) {
        this.displayName = displayName;
        this.applicability = applicability;
    }
    
    /**
     * Returns a user-friendly display name for the paradigm.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Returns a description of when this paradigm is most applicable.
     * 
     * @return the applicability description
     */
    public String getApplicability() {
        return applicability;
    }
}