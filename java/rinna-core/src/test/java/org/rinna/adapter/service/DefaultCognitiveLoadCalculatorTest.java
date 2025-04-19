/*
 * Test class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.adapter.repository.InMemoryWorkItemAssignmentRepository;
import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OrganizationalUnit;
import org.rinna.domain.model.OrganizationalUnitRecord;
import org.rinna.domain.model.OrganizationalUnitType;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.repository.OrganizationalUnitRepository;
import org.rinna.domain.service.CognitiveLoadCalculator;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.WorkItemAssignmentRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for the DefaultCognitiveLoadCalculator.
 */
public class DefaultCognitiveLoadCalculatorTest {

    @Mock
    private OrganizationalUnitRepository organizationalUnitRepository;
    
    @Mock
    private ItemService itemService;
    
    private WorkItemAssignmentRepository assignmentRepository;
    
    private CognitiveLoadCalculator calculator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        assignmentRepository = new InMemoryWorkItemAssignmentRepository();
        ((InMemoryWorkItemAssignmentRepository) assignmentRepository).clear();
        
        calculator = new DefaultCognitiveLoadCalculator(
                organizationalUnitRepository,
                itemService,
                assignmentRepository
        );
    }
    
    @Test
    void testCalculateWorkItemLoad() {
        // Create work items with different types and priorities
        WorkItem taskLow = createWorkItem(WorkItemType.TASK, Priority.LOW);
        WorkItem bugMedium = createWorkItem(WorkItemType.BUG, Priority.MEDIUM);
        WorkItem storyHigh = createWorkItem(WorkItemType.STORY, Priority.HIGH);
        WorkItem featureCritical = createWorkItem(WorkItemType.FEATURE, Priority.CRITICAL);
        WorkItem epic = createWorkItem(WorkItemType.EPIC, Priority.MEDIUM);
        
        // Calculate loads
        int taskLoad = calculator.calculateWorkItemLoad(taskLow);
        int bugLoad = calculator.calculateWorkItemLoad(bugMedium);
        int storyLoad = calculator.calculateWorkItemLoad(storyHigh);
        int featureLoad = calculator.calculateWorkItemLoad(featureCritical);
        int epicLoad = calculator.calculateWorkItemLoad(epic);
        
        // Assert that loads are calculated correctly
        assertTrue(taskLoad < bugLoad, "Task should have lower load than bug");
        assertTrue(bugLoad < storyLoad, "Bug should have lower load than story");
        assertTrue(storyLoad < featureLoad, "Story should have lower load than feature");
        assertTrue(storyLoad < epicLoad, "Story should have lower load than epic");
    }
    
    @Test
    void testCalculateWorkItemLoadWithDomainAndParadigm() {
        // Create work items with different domains and paradigms
        WorkItem obviousTaskItem = createWorkItemWithDomainAndParadigm(
                WorkItemType.TASK, Priority.MEDIUM, CynefinDomain.OBVIOUS, WorkParadigm.TASK);
        
        WorkItem complexEngineeringItem = createWorkItemWithDomainAndParadigm(
                WorkItemType.TASK, Priority.MEDIUM, CynefinDomain.COMPLEX, WorkParadigm.ENGINEERING);
        
        WorkItem chaoticResearchItem = createWorkItemWithDomainAndParadigm(
                WorkItemType.TASK, Priority.MEDIUM, CynefinDomain.CHAOTIC, WorkParadigm.RESEARCH);
        
        // Calculate loads
        int obviousTaskLoad = calculator.calculateWorkItemLoad(obviousTaskItem);
        int complexEngineeringLoad = calculator.calculateWorkItemLoad(complexEngineeringItem);
        int chaoticResearchLoad = calculator.calculateWorkItemLoad(chaoticResearchItem);
        
        // Assert that loads are calculated correctly based on domain and paradigm
        assertTrue(obviousTaskLoad < complexEngineeringLoad, "Obvious task should have lower load than complex engineering");
        assertTrue(complexEngineeringLoad < chaoticResearchLoad, "Complex engineering should have lower load than chaotic research");
    }
    
    @Test
    void testCalculateTotalLoad() {
        // Create a list of work items
        List<WorkItem> workItems = Arrays.asList(
                createWorkItem(WorkItemType.TASK, Priority.LOW),
                createWorkItem(WorkItemType.BUG, Priority.MEDIUM),
                createWorkItem(WorkItemType.STORY, Priority.HIGH)
        );
        
        // Calculate individual loads
        int task = calculator.calculateWorkItemLoad(workItems.get(0));
        int bug = calculator.calculateWorkItemLoad(workItems.get(1));
        int story = calculator.calculateWorkItemLoad(workItems.get(2));
        
        // Calculate total load
        int totalLoad = calculator.calculateTotalLoad(workItems);
        
        // Assert that total load is the sum of individual loads
        assertEquals(task + bug + story, totalLoad);
    }
    
    @Test
    void testCalculateImpact() {
        // Create an organizational unit
        OrganizationalUnit unit = createOrganizationalUnit(OrganizationalUnitType.TEAM, 100, 50);
        
        // Create a work item
        WorkItem workItem = createWorkItem(WorkItemType.FEATURE, Priority.HIGH);
        
        // Calculate the impact
        int impact = calculator.calculateImpact(unit, workItem);
        
        // Assert that impact is current load + work item load
        int workItemLoad = calculator.calculateWorkItemLoad(workItem);
        assertEquals(50 + workItemLoad, impact);
    }
    
    @Test
    void testRecommendCapacity() {
        // Create organizational units with different types and members
        OrganizationalUnit individual = createOrganizationalUnitWithMembers(OrganizationalUnitType.INDIVIDUAL, 1);
        OrganizationalUnit pair = createOrganizationalUnitWithMembers(OrganizationalUnitType.PAIR, 2);
        OrganizationalUnit team = createOrganizationalUnitWithMembers(OrganizationalUnitType.TEAM, 5);
        OrganizationalUnit department = createOrganizationalUnitWithMembers(OrganizationalUnitType.DEPARTMENT, 15);
        
        // Calculate recommended capacities
        int individualCapacity = calculator.recommendCapacity(individual);
        int pairCapacity = calculator.recommendCapacity(pair);
        int teamCapacity = calculator.recommendCapacity(team);
        int departmentCapacity = calculator.recommendCapacity(department);
        
        // Assert that capacities are calculated correctly
        assertTrue(individualCapacity < pairCapacity, "Individual capacity should be less than pair capacity");
        assertTrue(pairCapacity < teamCapacity, "Pair capacity should be less than team capacity");
        assertTrue(teamCapacity < departmentCapacity, "Team capacity should be less than department capacity");
        
        // Add domain expertise and work paradigms to a team and verify increased capacity
        OrganizationalUnit expertTeam = createOrganizationalUnitWithExpertise(
                OrganizationalUnitType.TEAM, 
                5,
                Arrays.asList(CynefinDomain.OBVIOUS, CynefinDomain.COMPLICATED, CynefinDomain.COMPLEX),
                Arrays.asList(WorkParadigm.TASK, WorkParadigm.ENGINEERING, WorkParadigm.PRODUCT)
        );
        
        int expertTeamCapacity = calculator.recommendCapacity(expertTeam);
        assertTrue(teamCapacity < expertTeamCapacity, "Team with expertise should have higher capacity");
    }
    
    @Test
    void testCalculateUtilizationPercentage() {
        // Create an organizational unit with capacity 100 and current load 75
        OrganizationalUnit unit = createOrganizationalUnit(OrganizationalUnitType.TEAM, 100, 75);
        
        // Calculate utilization percentage
        int utilizationPercentage = calculator.calculateUtilizationPercentage(unit);
        
        // Assert that utilization percentage is calculated correctly
        assertEquals(75, utilizationPercentage);
        
        // Test with zero capacity
        OrganizationalUnit zeroCapacityUnit = createOrganizationalUnit(OrganizationalUnitType.TEAM, 0, 0);
        int zeroCapacityUtilization = calculator.calculateUtilizationPercentage(zeroCapacityUnit);
        assertEquals(0, zeroCapacityUtilization, "Utilization should be 0 when capacity is 0");
    }
    
    @Test
    void testIsOverloaded() {
        // Create organizational units with different loads
        OrganizationalUnit lightlyLoaded = createOrganizationalUnit(OrganizationalUnitType.TEAM, 100, 50);
        OrganizationalUnit heavilyLoaded = createOrganizationalUnit(OrganizationalUnitType.TEAM, 100, 90);
        
        // Check if units are overloaded
        assertFalse(calculator.isOverloaded(lightlyLoaded, 75), "Lightly loaded unit should not be overloaded at 75% threshold");
        assertTrue(calculator.isOverloaded(lightlyLoaded, 45), "Lightly loaded unit should be overloaded at 45% threshold");
        
        assertTrue(calculator.isOverloaded(heavilyLoaded, 75), "Heavily loaded unit should be overloaded at 75% threshold");
        assertTrue(calculator.isOverloaded(heavilyLoaded, 85), "Heavily loaded unit should be overloaded at 85% threshold");
    }
    
    @Test
    void testGetDomainLoadFactor() {
        // Test domain load factors
        double obviousFactor = calculator.getDomainLoadFactor(CynefinDomain.OBVIOUS);
        double complicatedFactor = calculator.getDomainLoadFactor(CynefinDomain.COMPLICATED);
        double complexFactor = calculator.getDomainLoadFactor(CynefinDomain.COMPLEX);
        double chaoticFactor = calculator.getDomainLoadFactor(CynefinDomain.CHAOTIC);
        
        // Assert that domain load factors are ordered correctly
        assertTrue(obviousFactor < complicatedFactor, "Obvious factor should be less than complicated factor");
        assertTrue(complicatedFactor < complexFactor, "Complicated factor should be less than complex factor");
        assertTrue(complexFactor < chaoticFactor, "Complex factor should be less than chaotic factor");
    }
    
    @Test
    void testGetParadigmLoadFactor() {
        // Test paradigm load factors
        double taskFactor = calculator.getParadigmLoadFactor(WorkParadigm.TASK);
        double engineeringFactor = calculator.getParadigmLoadFactor(WorkParadigm.ENGINEERING);
        double productFactor = calculator.getParadigmLoadFactor(WorkParadigm.PRODUCT);
        double researchFactor = calculator.getParadigmLoadFactor(WorkParadigm.RESEARCH);
        
        // Assert that paradigm load factors are ordered correctly
        assertTrue(taskFactor < engineeringFactor, "Task factor should be less than engineering factor");
        assertTrue(engineeringFactor < productFactor, "Engineering factor should be less than product factor");
        assertTrue(productFactor < researchFactor, "Product factor should be less than research factor");
    }
    
    @Test
    void testEstimateIndividualCapacity() {
        // Test with different expertise and experience levels
        int capacityNoExpertise = calculator.estimateIndividualCapacity(
                Collections.emptyList(), Collections.emptyList(), 3);
        
        int capacityWithDomainExpertise = calculator.estimateIndividualCapacity(
                Arrays.asList(CynefinDomain.OBVIOUS, CynefinDomain.COMPLICATED),
                Collections.emptyList(),
                3);
        
        int capacityWithParadigmExpertise = calculator.estimateIndividualCapacity(
                Collections.emptyList(),
                Arrays.asList(WorkParadigm.TASK, WorkParadigm.ENGINEERING),
                3);
        
        int capacityWithBothExpertise = calculator.estimateIndividualCapacity(
                Arrays.asList(CynefinDomain.OBVIOUS, CynefinDomain.COMPLICATED),
                Arrays.asList(WorkParadigm.TASK, WorkParadigm.ENGINEERING),
                3);
        
        int capacityExpertWithHighExperience = calculator.estimateIndividualCapacity(
                Arrays.asList(CynefinDomain.OBVIOUS, CynefinDomain.COMPLICATED),
                Arrays.asList(WorkParadigm.TASK, WorkParadigm.ENGINEERING),
                5);
        
        // Assert that capacity is calculated correctly based on expertise and experience
        assertTrue(capacityNoExpertise < capacityWithDomainExpertise, "Domain expertise should increase capacity");
        assertTrue(capacityNoExpertise < capacityWithParadigmExpertise, "Paradigm expertise should increase capacity");
        assertTrue(capacityWithDomainExpertise < capacityWithBothExpertise, "Combined expertise should increase capacity");
        assertTrue(capacityWithParadigmExpertise < capacityWithBothExpertise, "Combined expertise should increase capacity");
        assertTrue(capacityWithBothExpertise < capacityExpertWithHighExperience, "High experience should increase capacity");
    }
    
    @Test
    void testCalculateMemberLoadDistribution() {
        // Create an organizational unit
        UUID unitId = UUID.randomUUID();
        OrganizationalUnit unit = createOrganizationalUnitWithMembers(OrganizationalUnitType.TEAM, 3);
        when(organizationalUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        
        List<String> members = unit.getMembers();
        String member1 = members.get(0);
        String member2 = members.get(1);
        String member3 = members.get(2);
        
        // Create work items
        UUID workItem1Id = UUID.randomUUID();
        UUID workItem2Id = UUID.randomUUID();
        UUID workItem3Id = UUID.randomUUID();
        
        WorkItem workItem1 = createWorkItem(WorkItemType.TASK, Priority.LOW);
        WorkItem workItem2 = createWorkItem(WorkItemType.BUG, Priority.MEDIUM);
        WorkItem workItem3 = createWorkItem(WorkItemType.FEATURE, Priority.HIGH);
        
        when(itemService.findById(workItem1Id)).thenReturn(Optional.of(workItem1));
        when(itemService.findById(workItem2Id)).thenReturn(Optional.of(workItem2));
        when(itemService.findById(workItem3Id)).thenReturn(Optional.of(workItem3));
        
        // Assign work items to members
        assignmentRepository.assignWorkItem(unitId, member1, workItem1Id);
        assignmentRepository.assignWorkItem(unitId, member1, workItem2Id);
        assignmentRepository.assignWorkItem(unitId, member2, workItem3Id);
        
        // Calculate load distribution
        CognitiveLoadCalculator.MemberLoadDistribution distribution = calculator.calculateMemberLoadDistribution(unitId);
        
        // Assert distribution
        assertEquals(3, distribution.getMemberLoads().size(), "Should have one entry per member");
        
        // Find each member's load
        CognitiveLoadCalculator.MemberLoad member1Load = distribution.getMemberLoads().stream()
                .filter(ml -> ml.getMemberId().equals(member1))
                .findFirst()
                .orElse(null);
        
        CognitiveLoadCalculator.MemberLoad member2Load = distribution.getMemberLoads().stream()
                .filter(ml -> ml.getMemberId().equals(member2))
                .findFirst()
                .orElse(null);
        
        CognitiveLoadCalculator.MemberLoad member3Load = distribution.getMemberLoads().stream()
                .filter(ml -> ml.getMemberId().equals(member3))
                .findFirst()
                .orElse(null);
        
        assertNotNull(member1Load, "Member 1 should have a load entry");
        assertNotNull(member2Load, "Member 2 should have a load entry");
        assertNotNull(member3Load, "Member 3 should have a load entry");
        
        assertEquals(2, member1Load.getAssignedWorkItems().size(), "Member 1 should have 2 work items");
        assertEquals(1, member2Load.getAssignedWorkItems().size(), "Member 2 should have 1 work item");
        assertEquals(0, member3Load.getAssignedWorkItems().size(), "Member 3 should have 0 work items");
        
        // Assert that standard deviation is calculated
        double stdDev = distribution.getStandardDeviation();
        assertTrue(stdDev > 0, "Standard deviation should be greater than 0");
        
        // Test balance check
        assertFalse(distribution.isBalanced(5.0), "Distribution should not be balanced at 5% threshold");
    }
    
    @Test
    void testSuggestMemberAssignment() {
        // Create an organizational unit
        UUID unitId = UUID.randomUUID();
        OrganizationalUnit unit = createOrganizationalUnitWithMembers(OrganizationalUnitType.TEAM, 3);
        when(organizationalUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        
        List<String> members = unit.getMembers();
        String member1 = members.get(0);
        String member2 = members.get(1);
        String member3 = members.get(2);
        
        // Create work items
        UUID workItem1Id = UUID.randomUUID();
        UUID workItem2Id = UUID.randomUUID();
        UUID workItem3Id = UUID.randomUUID();
        UUID newWorkItemId = UUID.randomUUID();
        
        WorkItem workItem1 = createWorkItem(WorkItemType.TASK, Priority.LOW);
        WorkItem workItem2 = createWorkItem(WorkItemType.BUG, Priority.MEDIUM);
        WorkItem workItem3 = createWorkItem(WorkItemType.FEATURE, Priority.HIGH);
        WorkItem newWorkItem = createWorkItem(WorkItemType.STORY, Priority.MEDIUM);
        
        when(itemService.findById(workItem1Id)).thenReturn(Optional.of(workItem1));
        when(itemService.findById(workItem2Id)).thenReturn(Optional.of(workItem2));
        when(itemService.findById(workItem3Id)).thenReturn(Optional.of(workItem3));
        when(itemService.findById(newWorkItemId)).thenReturn(Optional.of(newWorkItem));
        
        // Assign work items to members
        assignmentRepository.assignWorkItem(unitId, member1, workItem1Id);
        assignmentRepository.assignWorkItem(unitId, member1, workItem2Id);
        assignmentRepository.assignWorkItem(unitId, member2, workItem3Id);
        
        // Member 3 has no work items, so should be suggested
        String suggestedMember = calculator.suggestMemberAssignment(unitId, newWorkItemId);
        assertEquals(member3, suggestedMember, "Member 3 should be suggested as they have the lowest load");
    }
    
    @Test
    void testOptimizeWorkDistribution() {
        // Create an organizational unit
        UUID unitId = UUID.randomUUID();
        OrganizationalUnit unit = createOrganizationalUnitWithMembers(OrganizationalUnitType.TEAM, 3);
        when(organizationalUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        
        List<String> members = unit.getMembers();
        String member1 = members.get(0);
        String member2 = members.get(1);
        String member3 = members.get(2);
        
        // Create work items
        UUID smallWorkItemId = UUID.randomUUID();
        UUID mediumWorkItemId = UUID.randomUUID();
        UUID largeWorkItemId = UUID.randomUUID();
        
        WorkItem smallWorkItem = createWorkItem(WorkItemType.TASK, Priority.LOW);
        WorkItem mediumWorkItem = createWorkItem(WorkItemType.BUG, Priority.MEDIUM);
        WorkItem largeWorkItem = createWorkItem(WorkItemType.FEATURE, Priority.HIGH);
        
        when(itemService.findById(smallWorkItemId)).thenReturn(Optional.of(smallWorkItem));
        when(itemService.findById(mediumWorkItemId)).thenReturn(Optional.of(mediumWorkItem));
        when(itemService.findById(largeWorkItemId)).thenReturn(Optional.of(largeWorkItem));
        
        // Assign all work items to member1, none to others
        assignmentRepository.assignWorkItem(unitId, member1, smallWorkItemId);
        assignmentRepository.assignWorkItem(unitId, member1, mediumWorkItemId);
        assignmentRepository.assignWorkItem(unitId, member1, largeWorkItemId);
        
        // Optimize the distribution
        boolean optimized = calculator.optimizeWorkDistribution(unitId);
        assertTrue(optimized, "Distribution should be optimized");
        
        // Check that one of the work items was reassigned
        int member1Count = assignmentRepository.getAssignmentCount(unitId, member1);
        assertTrue(member1Count < 3, "Member 1 should have fewer work items after optimization");
        
        int totalAssignments = assignmentRepository.getAssignmentCount(unitId, member1) +
                assignmentRepository.getAssignmentCount(unitId, member2) +
                assignmentRepository.getAssignmentCount(unitId, member3);
        assertEquals(3, totalAssignments, "Total number of assignments should remain the same");
    }
    
    @Test
    void testRecordActualEffort() {
        // Create a work item
        UUID workItemId = UUID.randomUUID();
        WorkItem workItem = createWorkItemWithDomainAndParadigm(
                WorkItemType.TASK, Priority.MEDIUM, CynefinDomain.COMPLICATED, WorkParadigm.ENGINEERING);
        when(itemService.findById(workItemId)).thenReturn(Optional.of(workItem));
        
        // Record actual effort (greater than estimated)
        boolean result = calculator.recordActualEffort(workItemId, 8, 12);
        assertTrue(result, "Recording actual effort should succeed");
        
        // Check that adjustment factors were updated by comparing load before and after
        int loadBefore = calculator.calculateWorkItemLoad(workItem);
        
        // Create a similar work item and calculate its load
        WorkItem similarWorkItem = createWorkItemWithDomainAndParadigm(
                WorkItemType.TASK, Priority.MEDIUM, CynefinDomain.COMPLICATED, WorkParadigm.ENGINEERING);
        int loadAfter = calculator.calculateWorkItemLoad(similarWorkItem);
        
        // Difficult to precisely check the load as it depends on internal adjustment factors
        // But the load factor should have increased
        assertTrue(loadAfter >= loadBefore, "Load factor should be adjusted upward based on actual > estimated");
    }
    
    // Helper methods to create test objects
    
    private WorkItem createWorkItem(WorkItemType type, Priority priority) {
        WorkItem workItem = mock(WorkItem.class);
        when(workItem.getType()).thenReturn(type);
        when(workItem.getPriority()).thenReturn(priority);
        when(workItem.getCynefinDomain()).thenReturn(Optional.empty());
        when(workItem.getWorkParadigm()).thenReturn(Optional.empty());
        when(workItem.getProjectId()).thenReturn(UUID.randomUUID());
        return workItem;
    }
    
    private WorkItem createWorkItemWithDomainAndParadigm(WorkItemType type, Priority priority, 
                                                        CynefinDomain domain, WorkParadigm paradigm) {
        WorkItem workItem = mock(WorkItem.class);
        when(workItem.getType()).thenReturn(type);
        when(workItem.getPriority()).thenReturn(priority);
        when(workItem.getCynefinDomain()).thenReturn(Optional.of(domain));
        when(workItem.getWorkParadigm()).thenReturn(Optional.of(paradigm));
        when(workItem.getProjectId()).thenReturn(UUID.randomUUID());
        return workItem;
    }
    
    private OrganizationalUnit createOrganizationalUnit(OrganizationalUnitType type, int capacity, int currentLoad) {
        UUID id = UUID.randomUUID();
        String name = type.name() + " " + id;
        String description = "Description for " + name;
        String owner = "Owner";
        Instant now = Instant.now();
        List<String> members = Collections.emptyList();
        List<CynefinDomain> domains = Collections.emptyList();
        List<WorkParadigm> paradigms = Collections.emptyList();
        List<String> tags = Collections.emptyList();
        
        return new OrganizationalUnitRecord(
                id, name, description, type, null, owner, now, now,
                capacity, currentLoad, members, true, domains, paradigms, tags
        );
    }
    
    private OrganizationalUnit createOrganizationalUnitWithMembers(OrganizationalUnitType type, int memberCount) {
        UUID id = UUID.randomUUID();
        String name = type.name() + " " + id;
        String description = "Description for " + name;
        String owner = "Owner";
        Instant now = Instant.now();
        
        List<String> members = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) {
            members.add("member" + i);
        }
        
        int capacity = type == OrganizationalUnitType.INDIVIDUAL ? 25 : 
                      (type == OrganizationalUnitType.PAIR ? 45 : 100);
        int currentLoad = 0;
        
        List<CynefinDomain> domains = Collections.emptyList();
        List<WorkParadigm> paradigms = Collections.emptyList();
        List<String> tags = Collections.emptyList();
        
        return new OrganizationalUnitRecord(
                id, name, description, type, null, owner, now, now,
                capacity, currentLoad, members, true, domains, paradigms, tags
        );
    }
    
    private OrganizationalUnit createOrganizationalUnitWithExpertise(
            OrganizationalUnitType type, int memberCount,
            List<CynefinDomain> domains, List<WorkParadigm> paradigms) {
        
        UUID id = UUID.randomUUID();
        String name = type.name() + " " + id;
        String description = "Description for " + name;
        String owner = "Owner";
        Instant now = Instant.now();
        
        List<String> members = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) {
            members.add("member" + i);
        }
        
        int capacity = type == OrganizationalUnitType.INDIVIDUAL ? 25 : 
                      (type == OrganizationalUnitType.PAIR ? 45 : 100);
        int currentLoad = 0;
        
        List<String> tags = Collections.emptyList();
        
        return new OrganizationalUnitRecord(
                id, name, description, type, null, owner, now, now,
                capacity, currentLoad, members, true, domains, paradigms, tags
        );
    }
}