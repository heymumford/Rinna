/*
 * Unit tests for the CynefinDomain enum
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.base.UnitTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the CynefinDomain enum.
 */
@DisplayName("CYNEFIN Domain Tests")
public class CynefinDomainTest extends UnitTest {

    @Test
    @DisplayName("CYNEFIN domains should have correct names")
    void cynefinDomainsShouldHaveCorrectNames() {
        assertEquals("Obvious", CynefinDomain.OBVIOUS.getName());
        assertEquals("Complicated", CynefinDomain.COMPLICATED.getName());
        assertEquals("Complex", CynefinDomain.COMPLEX.getName());
        assertEquals("Chaotic", CynefinDomain.CHAOTIC.getName());
        assertEquals("Disorder", CynefinDomain.DISORDER.getName());
    }
    
    @Test
    @DisplayName("CYNEFIN domains should have correct descriptions")
    void cynefinDomainsShouldHaveCorrectDescriptions() {
        assertTrue(CynefinDomain.OBVIOUS.getDescription().contains("best practices"));
        assertTrue(CynefinDomain.COMPLICATED.getDescription().contains("analysis"));
        assertTrue(CynefinDomain.COMPLEX.getDescription().contains("retrospect"));
        assertTrue(CynefinDomain.CHAOTIC.getDescription().contains("No clear cause"));
        assertTrue(CynefinDomain.DISORDER.getDescription().contains("unclear"));
    }
    
    @Test
    @DisplayName("CYNEFIN domains should have correct recommended approaches")
    void cynefinDomainsShouldHaveCorrectApproaches() {
        assertTrue(CynefinDomain.OBVIOUS.getRecommendedApproach().contains("categorize"));
        assertTrue(CynefinDomain.COMPLICATED.getRecommendedApproach().contains("analyze"));
        assertTrue(CynefinDomain.COMPLEX.getRecommendedApproach().contains("Probe"));
        assertTrue(CynefinDomain.CHAOTIC.getRecommendedApproach().contains("Act"));
        assertTrue(CynefinDomain.DISORDER.getRecommendedApproach().contains("information"));
    }
    
    @Test
    @DisplayName("CYNEFIN domains should have correct characteristics")
    void cynefinDomainsShouldHaveCorrectCharacteristics() {
        assertFalse(CynefinDomain.OBVIOUS.requiresExpertAnalysis());
        assertTrue(CynefinDomain.COMPLICATED.requiresExpertAnalysis());
        
        assertFalse(CynefinDomain.OBVIOUS.requiresExperimentation());
        assertTrue(CynefinDomain.COMPLEX.requiresExperimentation());
        
        assertFalse(CynefinDomain.OBVIOUS.requiresImmediateAction());
        assertTrue(CynefinDomain.CHAOTIC.requiresImmediateAction());
        
        assertFalse(CynefinDomain.OBVIOUS.requiresFurtherClarification());
        assertTrue(CynefinDomain.DISORDER.requiresFurtherClarification());
    }
    
    @Test
    @DisplayName("CYNEFIN domains should recommend appropriate work paradigms")
    void cynefinDomainsShouldRecommendAppropriateParadigms() {
        String[] obviousParadigms = CynefinDomain.OBVIOUS.getAppropriateWorkParadigms();
        assertTrue(containsString(obviousParadigms, "TASK"));
        assertTrue(containsString(obviousParadigms, "MAINTENANCE"));
        
        String[] complicatedParadigms = CynefinDomain.COMPLICATED.getAppropriateWorkParadigms();
        assertTrue(containsString(complicatedParadigms, "STORY"));
        assertTrue(containsString(complicatedParadigms, "EPIC"));
        
        String[] complexParadigms = CynefinDomain.COMPLEX.getAppropriateWorkParadigms();
        assertTrue(containsString(complexParadigms, "EXPERIMENT"));
        
        String[] chaoticParadigms = CynefinDomain.CHAOTIC.getAppropriateWorkParadigms();
        assertTrue(containsString(chaoticParadigms, "INCIDENT"));
    }
    
    @Test
    @DisplayName("CYNEFIN domains should have appropriate cognitive load levels")
    void cynefinDomainsShouldHaveAppropriateCognitiveLoad() {
        assertTrue(CynefinDomain.OBVIOUS.getSuggestedCognitiveLoadLevel() < 
                  CynefinDomain.COMPLICATED.getSuggestedCognitiveLoadLevel());
        
        assertTrue(CynefinDomain.COMPLICATED.getSuggestedCognitiveLoadLevel() < 
                  CynefinDomain.COMPLEX.getSuggestedCognitiveLoadLevel());
        
        assertTrue(CynefinDomain.CHAOTIC.getSuggestedCognitiveLoadLevel() > 
                  CynefinDomain.OBVIOUS.getSuggestedCognitiveLoadLevel());
    }
    
    @Test
    @DisplayName("CYNEFIN domain toString() should return the name")
    void cynefinDomainToStringShouldReturnName() {
        assertEquals("Obvious", CynefinDomain.OBVIOUS.toString());
        assertEquals("Complex", CynefinDomain.COMPLEX.toString());
    }
    
    // Helper method
    private boolean containsString(String[] array, String target) {
        for (String s : array) {
            if (s.equals(target)) {
                return true;
            }
        }
        return false;
    }
}