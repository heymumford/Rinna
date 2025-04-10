/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Represents the CYNEFIN complexity domains for classifying work items.
 * This classification helps determine the appropriate approach for handling different types of work.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Cynefin_framework">CYNEFIN Framework</a>
 */
public enum CynefinDomain {
    /**
     * Clear cause-effect relationships where best practices apply.
     * Approach: Sense → Categorize → Respond
     */
    CLEAR("Clear", "Sense → Categorize → Respond", "Best practices"),
    
    /**
     * Known unknowns requiring expertise to analyze.
     * Approach: Sense → Analyze → Respond
     */
    COMPLICATED("Complicated", "Sense → Analyze → Respond", "Good practices"),
    
    /**
     * Unknown unknowns where cause and effect are only discernible in retrospect.
     * Approach: Probe → Sense → Respond
     */
    COMPLEX("Complex", "Probe → Sense → Respond", "Emergent practices"),
    
    /**
     * No clear cause-effect relationships, requiring immediate action.
     * Approach: Act → Sense → Respond
     */
    CHAOTIC("Chaotic", "Act → Sense → Respond", "Novel practices");
    
    private final String displayName;
    private final String approach;
    private final String practices;
    
    CynefinDomain(String displayName, String approach, String practices) {
        this.displayName = displayName;
        this.approach = approach;
        this.practices = practices;
    }
    
    /**
     * Returns a user-friendly display name for the domain.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Returns the recommended approach for this domain.
     * 
     * @return the approach
     */
    public String getApproach() {
        return approach;
    }
    
    /**
     * Returns the type of practices appropriate for this domain.
     * 
     * @return the practices
     */
    public String getPractices() {
        return practices;
    }
}