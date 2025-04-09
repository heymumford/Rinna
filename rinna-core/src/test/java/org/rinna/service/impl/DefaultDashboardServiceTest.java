package org.rinna.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.adapter.repository.InMemoryItemRepository;
import org.rinna.adapter.repository.InMemoryOrganizationalUnitRepository;
import org.rinna.adapter.repository.InMemoryWorkItemAssignmentRepository;
import org.rinna.adapter.service.DefaultCognitiveLoadCalculator;
import org.rinna.adapter.service.DefaultDashboardService;
import org.rinna.adapter.service.DefaultItemService;
import org.rinna.adapter.service.DefaultOrganizationalUnitService;
import org.rinna.domain.model.*;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.repository.OrganizationalUnitRepository;
import org.rinna.domain.repository.WorkItemAssignmentRepository;
import org.rinna.domain.service.CognitiveLoadCalculator;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.OrganizationalUnitService;
import org.rinna.domain.service.dashboard.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultDashboardServiceTest {

    private DashboardService dashboardService;
    
    @Mock
    private ItemRepository itemRepository;
    
    @Mock
    private OrganizationalUnitRepository organizationalUnitRepository;
    
    @Mock
    private WorkItemAssignmentRepository assignmentRepository;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private OrganizationalUnitService organizationalUnitService;
    
    @Mock
    private CognitiveLoadCalculator cognitiveLoadCalculator;
    
    @BeforeEach
    void setUp() {
        dashboardService = new DefaultDashboardService(
                itemRepository,
                organizationalUnitRepository,
                assignmentRepository,
                itemService,
                organizationalUnitService,
                cognitiveLoadCalculator
        );
    }
    
    @Test
    @DisplayName("Should generate main dashboard")
    void shouldGenerateMainDashboard() {
        // Given
        WorkItem item1 = createWorkItem("Task 1", WorkItemType.TASK, CynefinDomain.OBVIOUS, WorkParadigm.TASK, false, false);
        WorkItem item2 = createWorkItem("Task 2", WorkItemType.FEATURE, CynefinDomain.COMPLICATED, WorkParadigm.ENGINEERING, false, false);
        WorkItem item3 = createWorkItem("Task 3", WorkItemType.BUG, CynefinDomain.COMPLEX, WorkParadigm.PRODUCT, true, false);
        WorkItem item4 = createWorkItem("Task 4", WorkItemType.EPIC, CynefinDomain.CHAOTIC, WorkParadigm.RESEARCH, false, true);
        
        when(itemRepository.findAll()).thenReturn(List.of(item1, item2, item3, item4));
        when(cognitiveLoadCalculator.calculateWorkItemLoad(any())).thenReturn(5);
        
        OrganizationalUnit unit1 = createOrganizationalUnit("Team Alpha");
        when(organizationalUnitRepository.findAll()).thenReturn(List.of(unit1));
        when(organizationalUnitRepository.findWorkItemsForUnit(any())).thenReturn(List.of(item1.id(), item2.id()));
        
        // When
        DashboardData dashboard = dashboardService.generateDashboard();
        
        // Then
        assertNotNull(dashboard);
        assertEquals(4, dashboard.totalWorkItems());
        assertEquals(3, dashboard.totalActiveWorkItems());
        assertEquals(1, dashboard.totalBlockedWorkItems());
        assertEquals(1, dashboard.totalCompletedWorkItems());
        
        // Verify distribution maps have expected entries
        assertTrue(dashboard.cynefinDistribution().containsKey(CynefinDomain.OBVIOUS));
        assertTrue(dashboard.paradigmDistribution().containsKey(WorkParadigm.TASK));
        assertTrue(dashboard.typeDistribution().containsKey(WorkItemType.TASK));
        
        // Verify service interactions
        verify(itemRepository).findAll();
        verify(organizationalUnitRepository).findAll();
        verify(cognitiveLoadCalculator, atLeastOnce()).calculateWorkItemLoad(any());
    }
    
    @Test
    @DisplayName("Should generate cognitive load dashboard")
    void shouldGenerateCognitiveLoadDashboard() {
        // Given
        when(itemRepository.findAll()).thenReturn(List.of(
                createWorkItem("Task 1", WorkItemType.TASK, CynefinDomain.OBVIOUS, WorkParadigm.TASK, false, false)
        ));
        
        OrganizationalUnit unit1 = createOrganizationalUnit("Team Alpha");
        when(organizationalUnitRepository.findAll()).thenReturn(List.of(unit1));
        
        // When
        CognitiveLoadDashboardData dashboard = dashboardService.generateCognitiveLoadDashboard();
        
        // Then
        assertNotNull(dashboard);
        assertTrue(dashboard.unitLoadData().size() > 0);
        assertNotNull(dashboard.systemUtilizationPercentage());
        assertNotNull(dashboard.loadBySkillSet());
        assertNotNull(dashboard.loadByExpertiseLevel());
    }
    
    @Test
    @DisplayName("Should generate CYNEFIN distribution")
    void shouldGenerateCynefinDistribution() {
        // Given
        WorkItem item1 = createWorkItem("Task 1", WorkItemType.TASK, CynefinDomain.OBVIOUS, WorkParadigm.TASK, false, false);
        WorkItem item2 = createWorkItem("Task 2", WorkItemType.FEATURE, CynefinDomain.COMPLICATED, WorkParadigm.ENGINEERING, false, false);
        
        when(itemRepository.findAll()).thenReturn(List.of(item1, item2));
        when(cognitiveLoadCalculator.calculateWorkItemLoad(any())).thenReturn(5);
        
        OrganizationalUnit unit1 = createOrganizationalUnit("Team Alpha");
        when(organizationalUnitRepository.findAll()).thenReturn(List.of(unit1));
        when(organizationalUnitRepository.findWorkItemsForUnit(any())).thenReturn(List.of(item1.id()));
        
        // When
        DomainDistributionData distribution = dashboardService.generateCynefinDistribution();
        
        // Then
        assertNotNull(distribution);
        assertEquals(2, distribution.totalWorkItems());
        assertTrue(distribution.domainCounts().containsKey(CynefinDomain.OBVIOUS));
        assertTrue(distribution.domainCounts().containsKey(CynefinDomain.COMPLICATED));
        assertEquals(1, distribution.domainCounts().get(CynefinDomain.OBVIOUS));
        assertEquals(1, distribution.domainCounts().get(CynefinDomain.COMPLICATED));
    }
    
    @Test
    @DisplayName("Should generate work paradigm distribution")
    void shouldGenerateWorkParadigmDistribution() {
        // Given
        WorkItem item1 = createWorkItem("Task 1", WorkItemType.TASK, CynefinDomain.OBVIOUS, WorkParadigm.TASK, false, false);
        WorkItem item2 = createWorkItem("Task 2", WorkItemType.FEATURE, CynefinDomain.COMPLICATED, WorkParadigm.ENGINEERING, false, false);
        
        when(itemRepository.findAll()).thenReturn(List.of(item1, item2));
        when(cognitiveLoadCalculator.calculateWorkItemLoad(any())).thenReturn(5);
        
        OrganizationalUnit unit1 = createOrganizationalUnit("Team Alpha");
        when(organizationalUnitRepository.findAll()).thenReturn(List.of(unit1));
        when(organizationalUnitRepository.findWorkItemsForUnit(any())).thenReturn(List.of(item1.id()));
        
        // When
        ParadigmDistributionData distribution = dashboardService.generateWorkParadigmDistribution();
        
        // Then
        assertNotNull(distribution);
        assertEquals(2, distribution.totalWorkItems());
        assertTrue(distribution.paradigmCounts().containsKey(WorkParadigm.TASK));
        assertTrue(distribution.paradigmCounts().containsKey(WorkParadigm.ENGINEERING));
        assertEquals(1, distribution.paradigmCounts().get(WorkParadigm.TASK));
        assertEquals(1, distribution.paradigmCounts().get(WorkParadigm.ENGINEERING));
    }
    
    @Test
    @DisplayName("Should generate cognitive load trend")
    void shouldGenerateCognitiveLoadTrend() {
        // Given
        Instant startDate = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant endDate = Instant.now();
        
        // When
        TrendAnalysisData trend = dashboardService.generateCognitiveLoadTrend(startDate, endDate, "weekly");
        
        // Then
        assertNotNull(trend);
        assertEquals(startDate, trend.startDate());
        assertEquals(endDate, trend.endDate());
        assertEquals("weekly", trend.interval());
        assertTrue(trend.systemLoadTrend().size() > 0);
        assertTrue(trend.domainLoadTrends().size() > 0);
        assertTrue(trend.paradigmLoadTrends().size() > 0);
    }
    
    @Test
    @DisplayName("Should identify overload risks")
    void shouldIdentifyOverloadRisks() {
        // Given
        OrganizationalUnit unit1 = createOrganizationalUnit("Team Alpha");
        when(organizationalUnitRepository.findAll()).thenReturn(List.of(unit1));
        
        when(organizationalUnitService.calculateTotalCapacity(any())).thenReturn(30);
        
        Map<String, Integer> memberLoads = Map.of(
                "member1", 9,
                "member2", 18, // Overloaded
                "member3", 12
        );
        
        when(assignmentRepository.getMemberLoadsForUnit(any())).thenReturn(memberLoads);
        
        // When
        List<OverloadRiskData> risks = dashboardService.identifyOverloadRisks();
        
        // Then
        assertNotNull(risks);
        assertTrue(risks.size() > 0);
        
        // Verify interactions
        verify(organizationalUnitRepository).findAll();
        verify(organizationalUnitService).calculateTotalCapacity(any());
        verify(assignmentRepository).getMemberLoadsForUnit(any());
    }
    
    @Test
    @DisplayName("Should generate reassignment recommendations")
    void shouldGenerateReassignmentRecommendations() {
        // Given
        OrganizationalUnit unit1 = createOrganizationalUnit("Team Alpha");
        when(organizationalUnitRepository.findAll()).thenReturn(List.of(unit1));
        
        when(organizationalUnitService.calculateTotalCapacity(any())).thenReturn(30);
        
        Map<String, Integer> memberLoads = Map.of(
                "member1", 9,
                "member2", 18, // Overloaded
                "member3", 12
        );
        
        when(assignmentRepository.getMemberLoadsForUnit(any())).thenReturn(memberLoads);
        
        // When
        List<ReassignmentRecommendation> recommendations = dashboardService.generateReassignmentRecommendations();
        
        // Then
        assertNotNull(recommendations);
        
        // Verify interactions
        verify(organizationalUnitRepository).findAll();
        verify(organizationalUnitService).calculateTotalCapacity(any());
        verify(assignmentRepository).getMemberLoadsForUnit(any());
    }
    
    @Test
    @DisplayName("Should generate unit dashboard")
    void shouldGenerateUnitDashboard() {
        // Given
        UUID unitId = UUID.randomUUID();
        
        // When
        UnitDashboardData dashboard = dashboardService.generateUnitDashboard(unitId);
        
        // Then
        assertNotNull(dashboard);
        assertEquals(unitId, dashboard.unitId());
        assertNotNull(dashboard.members());
        assertTrue(dashboard.members().size() > 0);
        assertNotNull(dashboard.activeWorkItems());
        assertTrue(dashboard.activeWorkItems().size() > 0);
    }
    
    @Test
    @DisplayName("Should generate estimation accuracy report")
    void shouldGenerateEstimationAccuracyReport() {
        // Given
        Instant startDate = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant endDate = Instant.now();
        
        // When
        EstimationAccuracyData report = dashboardService.generateEstimationAccuracyReport(startDate, endDate);
        
        // Then
        assertNotNull(report);
        assertEquals(startDate, report.startDate());
        assertEquals(endDate, report.endDate());
        assertTrue(report.totalItemsAnalyzed() > 0);
        assertTrue(report.accuracyByDomain().size() > 0);
        assertTrue(report.accuracyByParadigm().size() > 0);
        assertTrue(report.worstEstimations().size() > 0);
        assertTrue(report.bestEstimations().size() > 0);
    }
    
    // Helper methods
    
    private WorkItem createWorkItem(String title, WorkItemType type, CynefinDomain domain, WorkParadigm paradigm, 
                                     boolean blocked, boolean completed) {
        return new WorkItemRecord(
                UUID.randomUUID(),
                title,
                "Description of " + title,
                type,
                Priority.MEDIUM,
                domain,
                paradigm,
                blocked,
                completed,
                false,
                Instant.now(),
                Instant.now().plus(14, ChronoUnit.DAYS),
                null,
                List.of("Tag1", "Tag2"),
                Map.of("key1", "value1")
        );
    }
    
    private OrganizationalUnit createOrganizationalUnit(String name) {
        return new OrganizationalUnitRecord(
                UUID.randomUUID(),
                name,
                "Description of " + name,
                OrganizationalUnitType.TEAM,
                UUID.randomUUID(), // parent ID
                List.of("member1", "member2", "member3"),
                Map.of("Domain1", 4, "Domain2", 3),
                30, // capacity
                Instant.now()
        );
    }
}