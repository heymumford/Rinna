/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Represents different work management paradigms in Ryorin-do v0.2.
 * Each paradigm is suited to different types of work and different CYNEFIN domains.
 */
public enum WorkParadigm {
    /**
     * Discrete unit of work with clear definition.
     * Best suited for the Obvious domain.
     */
    TASK("Task", "Discrete unit of work with clear definition"),
    
    /**
     * User-focused description of functionality.
     * Best suited for the Complicated domain.
     */
    STORY("Story", "User-focused description of functionality"),
    
    /**
     * Collection of related stories or features.
     * Best suited for the Complicated domain.
     */
    EPIC("Epic", "Collection of related stories or features"),
    
    /**
     * Strategic objective spanning multiple epics.
     * Best suited for the Complicated domain.
     */
    INITIATIVE("Initiative", "Strategic objective spanning multiple epics"),
    
    /**
     * Learning-focused work with defined hypotheses.
     * Best suited for the Complex domain.
     */
    EXPERIMENT("Experiment", "Learning-focused work with defined hypotheses"),
    
    /**
     * Time-boxed investigation or research.
     * Best suited for the Complex or Disorder domains.
     */
    SPIKE("Spike", "Time-boxed investigation or research"),
    
    /**
     * Problem resolution work.
     * Best suited for the Chaotic domain.
     */
    INCIDENT("Incident", "Problem resolution work"),
    
    /**
     * Ongoing work to sustain existing capabilities.
     * Best suited for the Obvious domain.
     */
    MAINTENANCE("Maintenance", "Ongoing work to sustain existing capabilities");
    
    private final String name;
    private final String description;
    
    /**
     * Constructs a new WorkParadigm.
     *
     * @param name the display name of the paradigm
     * @param description a brief description of the paradigm
     */
    WorkParadigm(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    /**
     * Returns the display name of the paradigm.
     *
     * @return the display name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the description of the paradigm.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the recommended CYNEFIN domain for this work paradigm.
     *
     * @return the recommended CYNEFIN domain
     */
    public CynefinDomain getRecommendedDomain() {
        return switch(this) {
            case TASK, MAINTENANCE -> CynefinDomain.OBVIOUS;
            case STORY, EPIC, INITIATIVE -> CynefinDomain.COMPLICATED;
            case EXPERIMENT -> CynefinDomain.COMPLEX;
            case INCIDENT -> CynefinDomain.CHAOTIC;
            case SPIKE -> CynefinDomain.DISORDER;
        };
    }
    
    /**
     * Returns whether this paradigm is suitable for a given CYNEFIN domain.
     *
     * @param domain the CYNEFIN domain to check
     * @return true if suitable, false otherwise
     */
    public boolean isSuitableFor(CynefinDomain domain) {
        for (String paradigm : domain.getAppropriateWorkParadigms()) {
            if (this.name().equals(paradigm)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the typical time horizon for this work paradigm.
     *
     * @return description of the typical time horizon
     */
    public String getTypicalTimeHorizon() {
        return switch(this) {
            case TASK -> "Hours to days";
            case STORY -> "Days to weeks";
            case SPIKE -> "Days to weeks";
            case EPIC -> "Weeks to months";
            case EXPERIMENT -> "Weeks to months";
            case INITIATIVE -> "Months to quarters";
            case INCIDENT -> "Minutes to hours";
            case MAINTENANCE -> "Ongoing";
        };
    }
    
    /**
     * Returns the suggested cognitive load range for this paradigm.
     *
     * @return a string describing the cognitive load range
     */
    public String getSuggestedCognitiveLoadRange() {
        return switch(this) {
            case TASK, MAINTENANCE -> "Low to Medium (1-5)";
            case STORY, SPIKE -> "Medium (3-6)";
            case EPIC, EXPERIMENT -> "Medium to High (5-8)";
            case INITIATIVE -> "High (7-9)";
            case INCIDENT -> "Very High (8-10)";
        };
    }
    
    @Override
    public String toString() {
        return name;
    }
}