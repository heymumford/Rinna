/*
 * Unit tests for the WorkParadigm enum
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
 * Unit tests for the WorkParadigm enum.
 */
@DisplayName("Work Paradigm Tests")
public class WorkParadigmTest extends UnitTest {

    @Test
    @DisplayName("Work paradigms should have correct names")
    void workParadigmsShouldHaveCorrectNames() {
        assertEquals("Task", WorkParadigm.TASK.getName());
        assertEquals("Story", WorkParadigm.STORY.getName());
        assertEquals("Epic", WorkParadigm.EPIC.getName());
        assertEquals("Initiative", WorkParadigm.INITIATIVE.getName());
        assertEquals("Experiment", WorkParadigm.EXPERIMENT.getName());
        assertEquals("Spike", WorkParadigm.SPIKE.getName());
        assertEquals("Incident", WorkParadigm.INCIDENT.getName());
        assertEquals("Maintenance", WorkParadigm.MAINTENANCE.getName());
    }
    
    @Test
    @DisplayName("Work paradigms should have correct descriptions")
    void workParadigmsShouldHaveCorrectDescriptions() {
        assertTrue(WorkParadigm.TASK.getDescription().contains("Discrete unit"));
        assertTrue(WorkParadigm.STORY.getDescription().contains("User-focused"));
        assertTrue(WorkParadigm.EPIC.getDescription().contains("Collection"));
        assertTrue(WorkParadigm.EXPERIMENT.getDescription().contains("hypotheses"));
    }
    
    @Test
    @DisplayName("Work paradigms should recommend appropriate CYNEFIN domains")
    void workParadigmsShouldRecommendAppropriateDomains() {
        assertEquals(CynefinDomain.OBVIOUS, WorkParadigm.TASK.getRecommendedDomain());
        assertEquals(CynefinDomain.COMPLICATED, WorkParadigm.STORY.getRecommendedDomain());
        assertEquals(CynefinDomain.COMPLEX, WorkParadigm.EXPERIMENT.getRecommendedDomain());
        assertEquals(CynefinDomain.CHAOTIC, WorkParadigm.INCIDENT.getRecommendedDomain());
        assertEquals(CynefinDomain.DISORDER, WorkParadigm.SPIKE.getRecommendedDomain());
    }
    
    @Test
    @DisplayName("Work paradigms should be suitable for their recommended domains")
    void workParadigmsShouldBeSuitableForRecommendedDomains() {
        assertTrue(WorkParadigm.TASK.isSuitableFor(CynefinDomain.OBVIOUS));
        assertTrue(WorkParadigm.STORY.isSuitableFor(CynefinDomain.COMPLICATED));
        assertTrue(WorkParadigm.EXPERIMENT.isSuitableFor(CynefinDomain.COMPLEX));
        assertTrue(WorkParadigm.INCIDENT.isSuitableFor(CynefinDomain.CHAOTIC));
    }
    
    @Test
    @DisplayName("Work paradigms should have appropriate time horizons")
    void workParadigmsShouldHaveAppropriateTimeHorizons() {
        assertTrue(WorkParadigm.TASK.getTypicalTimeHorizon().contains("Hours"));
        assertTrue(WorkParadigm.STORY.getTypicalTimeHorizon().contains("Days"));
        assertTrue(WorkParadigm.EPIC.getTypicalTimeHorizon().contains("Weeks"));
        assertTrue(WorkParadigm.INITIATIVE.getTypicalTimeHorizon().contains("Months"));
        assertTrue(WorkParadigm.INCIDENT.getTypicalTimeHorizon().contains("Minutes"));
        assertEquals("Ongoing", WorkParadigm.MAINTENANCE.getTypicalTimeHorizon());
    }
    
    @Test
    @DisplayName("Work paradigms should have appropriate cognitive load ranges")
    void workParadigmsShouldHaveAppropriateCognitiveLoad() {
        assertTrue(WorkParadigm.TASK.getSuggestedCognitiveLoadRange().contains("Low"));
        assertTrue(WorkParadigm.STORY.getSuggestedCognitiveLoadRange().contains("Medium"));
        assertTrue(WorkParadigm.EPIC.getSuggestedCognitiveLoadRange().contains("Medium to High"));
        assertTrue(WorkParadigm.INITIATIVE.getSuggestedCognitiveLoadRange().contains("High"));
        assertTrue(WorkParadigm.INCIDENT.getSuggestedCognitiveLoadRange().contains("Very High"));
    }
    
    @Test
    @DisplayName("Work paradigm toString() should return the name")
    void workParadigmToStringShouldReturnName() {
        assertEquals("Task", WorkParadigm.TASK.toString());
        assertEquals("Experiment", WorkParadigm.EXPERIMENT.toString());
    }
}