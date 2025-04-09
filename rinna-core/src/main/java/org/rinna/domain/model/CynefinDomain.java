/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Represents the CYNEFIN domains for context-aware management.
 * This is a core concept in Ryorin-do v0.2 that acknowledges different types of work
 * require different management approaches based on the relationship between cause and effect.
 */
public enum CynefinDomain {
    /**
     * The domain where cause and effect relationships are clear, predictable, and generally linear.
     * Best practices apply, and the approach is to sense, categorize, respond.
     */
    OBVIOUS("Obvious", "Clear cause and effect relationships, best practices apply"),
    
    /**
     * The domain where cause and effect relationships exist but may not be immediately apparent.
     * Good practices apply, and the approach is to sense, analyze, respond.
     */
    COMPLICATED("Complicated", "Cause and effect relationships require analysis, good practices apply"),
    
    /**
     * The domain where cause and effect relationships can only be understood in retrospect.
     * Emergent practices apply, and the approach is to probe, sense, respond.
     */
    COMPLEX("Complex", "Cause and effect can only be understood in retrospect, emergent practices"),
    
    /**
     * The domain where no clear cause and effect relationships can be determined.
     * Novel practices apply, and the approach is to act, sense, respond.
     */
    CHAOTIC("Chaotic", "No clear cause and effect relationships, novel practices"),
    
    /**
     * The state of not knowing which domain applies.
     * The approach is to gather more information to move to one of the other domains.
     */
    DISORDER("Disorder", "Domain is unclear or in transition");
    
    private final String name;
    private final String description;
    
    /**
     * Constructs a new CynefinDomain.
     *
     * @param name the display name of the domain
     * @param description a brief description of the domain
     */
    CynefinDomain(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    /**
     * Returns the display name of the domain.
     *
     * @return the display name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the description of the domain.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the recommended approach for handling work in this domain.
     *
     * @return the recommended approach
     */
    public String getRecommendedApproach() {
        return switch(this) {
            case OBVIOUS -> "Sense, categorize, respond - Apply best practices";
            case COMPLICATED -> "Sense, analyze, respond - Apply good practices";
            case COMPLEX -> "Probe, sense, respond - Apply emergent practices";
            case CHAOTIC -> "Act, sense, respond - Apply novel practices";
            case DISORDER -> "Gather more information to determine the dominant domain";
        };
    }
    
    /**
     * Returns whether this domain requires expert analysis.
     *
     * @return true if expert analysis is required, false otherwise
     */
    public boolean requiresExpertAnalysis() {
        return this == COMPLICATED;
    }
    
    /**
     * Returns whether this domain requires experimentation.
     *
     * @return true if experimentation is required, false otherwise
     */
    public boolean requiresExperimentation() {
        return this == COMPLEX;
    }
    
    /**
     * Returns whether this domain requires immediate action.
     *
     * @return true if immediate action is required, false otherwise
     */
    public boolean requiresImmediateAction() {
        return this == CHAOTIC;
    }
    
    /**
     * Returns whether this domain requires further clarification.
     *
     * @return true if further clarification is required, false otherwise
     */
    public boolean requiresFurtherClarification() {
        return this == DISORDER;
    }
    
    /**
     * Returns the appropriate work paradigm types for this domain.
     *
     * @return an array of appropriate work paradigm types
     */
    public String[] getAppropriateWorkParadigms() {
        return switch(this) {
            case OBVIOUS -> new String[]{"TASK", "MAINTENANCE"};
            case COMPLICATED -> new String[]{"STORY", "EPIC", "INITIATIVE"};
            case COMPLEX -> new String[]{"EXPERIMENT", "SPIKE"};
            case CHAOTIC -> new String[]{"INCIDENT"};
            case DISORDER -> new String[]{"SPIKE"};
        };
    }
    
    /**
     * Returns a suggestion for the cognitive load assessment
     * based on the domain complexity.
     *
     * @return suggested cognitive load level (1-10)
     */
    public int getSuggestedCognitiveLoadLevel() {
        return switch(this) {
            case OBVIOUS -> 2;
            case COMPLICATED -> 5;
            case COMPLEX -> 8;
            case CHAOTIC -> 9;
            case DISORDER -> 7;
        };
    }
    
    @Override
    public String toString() {
        return name;
    }
}