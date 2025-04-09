package org.rinna.adapter.service;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.repository.OrganizationalUnitRepository;
import org.rinna.domain.repository.WorkItemAssignmentRepository;
import org.rinna.domain.service.CognitiveLoadCalculator;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.OrganizationalUnitService;
import org.rinna.domain.service.dashboard.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of the DashboardService interface.
 * Provides comprehensive dashboards and reports for work items, cognitive load,
 * and organizational performance within the Ryorin-do framework.
 */
public class DefaultDashboardService implements DashboardService {
    
    private final ItemRepository itemRepository;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final WorkItemAssignmentRepository assignmentRepository;
    private final ItemService itemService;
    private final OrganizationalUnitService organizationalUnitService;
    private final CognitiveLoadCalculator cognitiveLoadCalculator;
    
    // Cache for trend data
    private final Map<String, Map<Instant, TrendData>> trendDataCache = new ConcurrentHashMap<>();
    private Instant lastCacheRefresh = Instant.now();
    private static final long CACHE_VALIDITY_MINUTES = 30;
    
    /**
     * Creates a new DefaultDashboardService.
     */
    public DefaultDashboardService(
            ItemRepository itemRepository,
            OrganizationalUnitRepository organizationalUnitRepository,
            WorkItemAssignmentRepository assignmentRepository,
            ItemService itemService,
            OrganizationalUnitService organizationalUnitService,
            CognitiveLoadCalculator cognitiveLoadCalculator) {
        this.itemRepository = itemRepository;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.assignmentRepository = assignmentRepository;
        this.itemService = itemService;
        this.organizationalUnitService = organizationalUnitService;
        this.cognitiveLoadCalculator = cognitiveLoadCalculator;
    }
    
    @Override
    public DashboardData generateDashboard() {
        Instant now = Instant.now();
        List<WorkItem> allItems = itemRepository.findAll();
        List<WorkItem> activeItems = allItems.stream()
                .filter(item -> !item.isCompleted() && !item.isCancelled())
                .collect(Collectors.toList());
        List<WorkItem> blockedItems = activeItems.stream()
                .filter(WorkItem::isBlocked)
                .collect(Collectors.toList());
        List<WorkItem> completedItems = allItems.stream()
                .filter(WorkItem::isCompleted)
                .collect(Collectors.toList());
        
        // Calculate distributions
        Map<CynefinDomain, Integer> cynefinDistribution = calculateCynefinDistribution(allItems);
        Map<WorkParadigm, Integer> paradigmDistribution = calculateWorkParadigmDistribution(allItems);
        Map<WorkItemType, Integer> typeDistribution = calculateTypeDistribution(allItems);
        
        // Calculate cognitive load metrics
        double averageLoad = activeItems.isEmpty() ? 0 : 
                activeItems.stream()
                        .mapToInt(cognitiveLoadCalculator::calculateWorkItemLoad)
                        .average()
                        .orElse(0);
        
        int maxLoad = activeItems.isEmpty() ? 0 :
                activeItems.stream()
                        .mapToInt(cognitiveLoadCalculator::calculateWorkItemLoad)
                        .max()
                        .orElse(0);
        
        int totalAssignedLoad = activeItems.stream()
                .mapToInt(cognitiveLoadCalculator::calculateWorkItemLoad)
                .sum();
        
        // Prepare unit summaries
        List<DashboardData.UnitSummary> unitSummaries = prepareUnitSummaries();
        
        // Identify overload risks and generate recommendations
        List<OverloadRiskData> overloadRisks = identifyOverloadRisks();
        List<ReassignmentRecommendation> recommendations = generateReassignmentRecommendations();
        
        // Gather tag distribution
        Map<String, Integer> tagDistribution = calculateTagDistribution(allItems);
        
        // Calculate load utilization
        int totalCapacity = unitSummaries.stream()
                .mapToInt(DashboardData.UnitSummary::totalCognitiveLoad)
                .sum();
        double loadUtilizationPercentage = totalCapacity > 0 ? 
                (double) totalAssignedLoad / totalCapacity * 100 : 0;
        
        return new DashboardData(
                now,
                allItems.size(),
                activeItems.size(),
                blockedItems.size(),
                completedItems.size(),
                cynefinDistribution,
                paradigmDistribution,
                typeDistribution,
                unitSummaries,
                overloadRisks,
                recommendations,
                tagDistribution,
                averageLoad,
                maxLoad,
                totalAssignedLoad,
                loadUtilizationPercentage
        );
    }
    
    @Override
    public CognitiveLoadDashboardData generateCognitiveLoadDashboard() {
        Instant now = Instant.now();
        List<WorkItem> activeItems = itemRepository.findAll().stream()
                .filter(item -> !item.isCompleted() && !item.isCancelled())
                .collect(Collectors.toList());
        
        // Prepare unit load data
        List<CognitiveLoadDashboardData.UnitLoadData> unitLoadData = prepareUnitLoadData();
        
        // Calculate system-wide metrics
        int totalSystemCapacity = unitLoadData.stream()
                .mapToInt(CognitiveLoadDashboardData.UnitLoadData::totalCapacity)
                .sum();
        
        int totalAssignedLoad = unitLoadData.stream()
                .mapToInt(CognitiveLoadDashboardData.UnitLoadData::assignedLoad)
                .sum();
        
        double systemUtilizationPercentage = totalSystemCapacity > 0 ?
                (double) totalAssignedLoad / totalSystemCapacity * 100 : 0;
        
        // Find members with highest and lowest utilization
        List<CognitiveLoadDashboardData.MemberLoadData> allMembers = unitLoadData.stream()
                .flatMap(unit -> unit.memberLoads().stream())
                .collect(Collectors.toList());
        
        List<CognitiveLoadDashboardData.MemberLoadData> topOverloadedMembers = allMembers.stream()
                .filter(member -> member.utilizationPercentage() > 90)
                .sorted(Comparator.comparing(CognitiveLoadDashboardData.MemberLoadData::utilizationPercentage).reversed())
                .limit(10)
                .collect(Collectors.toList());
        
        List<CognitiveLoadDashboardData.MemberLoadData> topUnderloadedMembers = allMembers.stream()
                .filter(member -> member.capacity() > 0)
                .sorted(Comparator.comparing(CognitiveLoadDashboardData.MemberLoadData::utilizationPercentage))
                .limit(10)
                .collect(Collectors.toList());
        
        // Calculate load by skill set and expertise level
        Map<String, Double> loadBySkillSet = new HashMap<>();
        Map<String, Integer> loadByExpertiseLevel = new HashMap<>();
        
        // In a real implementation, these would be populated based on actual skill and expertise data
        loadBySkillSet.put("Java Development", 35.5);
        loadBySkillSet.put("UX Design", 22.0);
        loadBySkillSet.put("DevOps", 18.5);
        loadBySkillSet.put("Testing", 24.0);
        
        loadByExpertiseLevel.put("Novice", 15);
        loadByExpertiseLevel.put("Intermediate", 35);
        loadByExpertiseLevel.put("Expert", 50);
        
        double averageLoadPerMember = allMembers.isEmpty() ? 0 :
                allMembers.stream()
                        .mapToDouble(CognitiveLoadDashboardData.MemberLoadData::assignedLoad)
                        .average()
                        .orElse(0);
        
        return new CognitiveLoadDashboardData(
                now,
                averageLoadPerMember,
                totalSystemCapacity,
                totalAssignedLoad,
                systemUtilizationPercentage,
                unitLoadData,
                topOverloadedMembers,
                topUnderloadedMembers,
                loadBySkillSet,
                loadByExpertiseLevel
        );
    }
    
    @Override
    public DomainDistributionData generateCynefinDistribution() {
        Instant now = Instant.now();
        List<WorkItem> allItems = itemRepository.findAll();
        int totalWorkItems = allItems.size();
        
        // Calculate domain counts
        Map<CynefinDomain, Integer> domainCounts = calculateCynefinDistribution(allItems);
        
        // Calculate domain percentages
        Map<CynefinDomain, Double> domainPercentages = new HashMap<>();
        if (totalWorkItems > 0) {
            for (Map.Entry<CynefinDomain, Integer> entry : domainCounts.entrySet()) {
                domainPercentages.put(entry.getKey(), (double) entry.getValue() / totalWorkItems * 100);
            }
        }
        
        // Group work items by domain
        Map<CynefinDomain, List<UUID>> workItemsByDomain = new HashMap<>();
        for (CynefinDomain domain : CynefinDomain.values()) {
            List<UUID> items = allItems.stream()
                    .filter(item -> item.cynefinDomain() == domain)
                    .map(WorkItem::id)
                    .collect(Collectors.toList());
            workItemsByDomain.put(domain, items);
        }
        
        // Calculate average load by domain
        Map<CynefinDomain, Integer> averageLoadByDomain = new HashMap<>();
        for (CynefinDomain domain : CynefinDomain.values()) {
            List<WorkItem> domainItems = allItems.stream()
                    .filter(item -> item.cynefinDomain() == domain)
                    .collect(Collectors.toList());
            
            if (!domainItems.isEmpty()) {
                int totalLoad = domainItems.stream()
                        .mapToInt(cognitiveLoadCalculator::calculateWorkItemLoad)
                        .sum();
                averageLoadByDomain.put(domain, totalLoad / domainItems.size());
            } else {
                averageLoadByDomain.put(domain, 0);
            }
        }
        
        // Calculate completion rate by domain
        Map<CynefinDomain, Double> completionRateByDomain = new HashMap<>();
        for (CynefinDomain domain : CynefinDomain.values()) {
            List<WorkItem> domainItems = allItems.stream()
                    .filter(item -> item.cynefinDomain() == domain)
                    .collect(Collectors.toList());
            
            if (!domainItems.isEmpty()) {
                long completedCount = domainItems.stream()
                        .filter(WorkItem::isCompleted)
                        .count();
                completionRateByDomain.put(domain, (double) completedCount / domainItems.size() * 100);
            } else {
                completionRateByDomain.put(domain, 0.0);
            }
        }
        
        // Calculate domain distribution by organizational unit
        Map<UUID, Map<CynefinDomain, Integer>> unitDomainCounts = new HashMap<>();
        List<UUID> unitIds = organizationalUnitRepository.findAll().stream()
                .map(unit -> unit.id())
                .collect(Collectors.toList());
        
        for (UUID unitId : unitIds) {
            Map<CynefinDomain, Integer> unitDistribution = new HashMap<>();
            for (CynefinDomain domain : CynefinDomain.values()) {
                unitDistribution.put(domain, 0);
            }
            
            List<UUID> unitWorkItems = organizationalUnitRepository.findWorkItemsForUnit(unitId);
            for (UUID workItemId : unitWorkItems) {
                itemRepository.findById(workItemId).ifPresent(item -> {
                    unitDistribution.compute(item.cynefinDomain(), (k, v) -> v + 1);
                });
            }
            
            unitDomainCounts.put(unitId, unitDistribution);
        }
        
        return new DomainDistributionData(
                now,
                totalWorkItems,
                domainCounts,
                domainPercentages,
                workItemsByDomain,
                averageLoadByDomain,
                completionRateByDomain,
                unitDomainCounts
        );
    }
    
    @Override
    public ParadigmDistributionData generateWorkParadigmDistribution() {
        Instant now = Instant.now();
        List<WorkItem> allItems = itemRepository.findAll();
        int totalWorkItems = allItems.size();
        
        // Calculate paradigm counts
        Map<WorkParadigm, Integer> paradigmCounts = calculateWorkParadigmDistribution(allItems);
        
        // Calculate paradigm percentages
        Map<WorkParadigm, Double> paradigmPercentages = new HashMap<>();
        if (totalWorkItems > 0) {
            for (Map.Entry<WorkParadigm, Integer> entry : paradigmCounts.entrySet()) {
                paradigmPercentages.put(entry.getKey(), (double) entry.getValue() / totalWorkItems * 100);
            }
        }
        
        // Group work items by paradigm
        Map<WorkParadigm, List<UUID>> workItemsByParadigm = new HashMap<>();
        for (WorkParadigm paradigm : WorkParadigm.values()) {
            List<UUID> items = allItems.stream()
                    .filter(item -> item.workParadigm() == paradigm)
                    .map(WorkItem::id)
                    .collect(Collectors.toList());
            workItemsByParadigm.put(paradigm, items);
        }
        
        // Calculate average load by paradigm
        Map<WorkParadigm, Integer> averageLoadByParadigm = new HashMap<>();
        for (WorkParadigm paradigm : WorkParadigm.values()) {
            List<WorkItem> paradigmItems = allItems.stream()
                    .filter(item -> item.workParadigm() == paradigm)
                    .collect(Collectors.toList());
            
            if (!paradigmItems.isEmpty()) {
                int totalLoad = paradigmItems.stream()
                        .mapToInt(cognitiveLoadCalculator::calculateWorkItemLoad)
                        .sum();
                averageLoadByParadigm.put(paradigm, totalLoad / paradigmItems.size());
            } else {
                averageLoadByParadigm.put(paradigm, 0);
            }
        }
        
        // Calculate completion rate by paradigm
        Map<WorkParadigm, Double> completionRateByParadigm = new HashMap<>();
        for (WorkParadigm paradigm : WorkParadigm.values()) {
            List<WorkItem> paradigmItems = allItems.stream()
                    .filter(item -> item.workParadigm() == paradigm)
                    .collect(Collectors.toList());
            
            if (!paradigmItems.isEmpty()) {
                long completedCount = paradigmItems.stream()
                        .filter(WorkItem::isCompleted)
                        .count();
                completionRateByParadigm.put(paradigm, (double) completedCount / paradigmItems.size() * 100);
            } else {
                completionRateByParadigm.put(paradigm, 0.0);
            }
        }
        
        // Calculate paradigm distribution by organizational unit
        Map<UUID, Map<WorkParadigm, Integer>> unitParadigmCounts = new HashMap<>();
        List<UUID> unitIds = organizationalUnitRepository.findAll().stream()
                .map(unit -> unit.id())
                .collect(Collectors.toList());
        
        for (UUID unitId : unitIds) {
            Map<WorkParadigm, Integer> unitDistribution = new HashMap<>();
            for (WorkParadigm paradigm : WorkParadigm.values()) {
                unitDistribution.put(paradigm, 0);
            }
            
            List<UUID> unitWorkItems = organizationalUnitRepository.findWorkItemsForUnit(unitId);
            for (UUID workItemId : unitWorkItems) {
                itemRepository.findById(workItemId).ifPresent(item -> {
                    unitDistribution.compute(item.workParadigm(), (k, v) -> v + 1);
                });
            }
            
            unitParadigmCounts.put(unitId, unitDistribution);
        }
        
        return new ParadigmDistributionData(
                now,
                totalWorkItems,
                paradigmCounts,
                paradigmPercentages,
                workItemsByParadigm,
                averageLoadByParadigm,
                completionRateByParadigm,
                unitParadigmCounts
        );
    }
    
    @Override
    public TrendAnalysisData generateCognitiveLoadTrend(Instant startDate, Instant endDate, String interval) {
        Instant now = Instant.now();
        
        // Determine interval in days
        int intervalDays;
        switch (interval.toLowerCase()) {
            case "daily":
                intervalDays = 1;
                break;
            case "weekly":
                intervalDays = 7;
                break;
            case "monthly":
                intervalDays = 30;
                break;
            default:
                intervalDays = 1;
        }
        
        // Generate time series points for system load
        List<TrendAnalysisData.TimeSeriesPoint> systemLoadTrend = new ArrayList<>();
        Map<UUID, List<TrendAnalysisData.TimeSeriesPoint>> unitLoadTrends = new HashMap<>();
        Map<CynefinDomain, List<TrendAnalysisData.TimeSeriesPoint>> domainLoadTrends = new HashMap<>();
        Map<WorkParadigm, List<TrendAnalysisData.TimeSeriesPoint>> paradigmLoadTrends = new HashMap<>();
        
        // Initialize domain and paradigm trend maps
        for (CynefinDomain domain : CynefinDomain.values()) {
            domainLoadTrends.put(domain, new ArrayList<>());
        }
        
        for (WorkParadigm paradigm : WorkParadigm.values()) {
            paradigmLoadTrends.put(paradigm, new ArrayList<>());
        }
        
        // Generate trend data points
        Instant currentDate = startDate;
        while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
            // For demonstration purposes, we're generating simulated data
            // In a real implementation, this would query historical data from a data store
            
            Instant pointDate = currentDate;
            
            // Simulated system load point
            double systemLoadValue = simulateSystemLoad(pointDate);
            Map<String, Double> systemLoadBreakdown = simulateLoadBreakdown(pointDate);
            systemLoadTrend.add(new TrendAnalysisData.TimeSeriesPoint(pointDate, systemLoadValue, systemLoadBreakdown));
            
            // Simulated domain load trends
            for (CynefinDomain domain : CynefinDomain.values()) {
                double domainLoadValue = simulateDomainLoad(domain, pointDate);
                Map<String, Double> domainLoadBreakdown = simulateDomainLoadBreakdown(domain, pointDate);
                domainLoadTrends.get(domain).add(new TrendAnalysisData.TimeSeriesPoint(
                        pointDate, domainLoadValue, domainLoadBreakdown));
            }
            
            // Simulated paradigm load trends
            for (WorkParadigm paradigm : WorkParadigm.values()) {
                double paradigmLoadValue = simulateParadigmLoad(paradigm, pointDate);
                Map<String, Double> paradigmLoadBreakdown = simulateParadigmLoadBreakdown(paradigm, pointDate);
                paradigmLoadTrends.get(paradigm).add(new TrendAnalysisData.TimeSeriesPoint(
                        pointDate, paradigmLoadValue, paradigmLoadBreakdown));
            }
            
            // Move to next interval
            currentDate = currentDate.plus(intervalDays, ChronoUnit.DAYS);
        }
        
        // Simulated completion rate trend
        List<TrendAnalysisData.TimeSeriesPoint> completionRateTrend = simulateCompletionRateTrend(startDate, endDate, intervalDays);
        
        // Simulated new work items trend
        List<TrendAnalysisData.TimeSeriesPoint> newWorkItemsTrend = simulateNewWorkItemsTrend(startDate, endDate, intervalDays);
        
        // Simulated tag trends
        Map<String, List<TrendAnalysisData.TimeSeriesPoint>> tagTrends = simulateTagTrends(startDate, endDate, intervalDays);
        
        // Simulated unit load trends
        unitLoadTrends = simulateUnitLoadTrends(startDate, endDate, intervalDays);
        
        return new TrendAnalysisData(
                now,
                startDate,
                endDate,
                interval,
                systemLoadTrend,
                unitLoadTrends,
                domainLoadTrends,
                paradigmLoadTrends,
                completionRateTrend,
                newWorkItemsTrend,
                tagTrends
        );
    }
    
    @Override
    public List<OverloadRiskData> identifyOverloadRisks() {
        List<OverloadRiskData> risks = new ArrayList<>();
        
        // Identify organizational units
        organizationalUnitRepository.findAll().forEach(unit -> {
            // For each unit, check if it's overloaded
            int unitCapacity = organizationalUnitService.calculateTotalCapacity(unit.id());
            int assignedLoad = 0;
            
            List<UUID> unitWorkItems = organizationalUnitRepository.findWorkItemsForUnit(unit.id());
            for (UUID workItemId : unitWorkItems) {
                itemRepository.findById(workItemId).ifPresent(item -> {
                    // This would be more efficient with a bulk operation in a real implementation
                });
            }
            
            // Check each member for overload
            Map<String, Integer> memberLoads = assignmentRepository.getMemberLoadsForUnit(unit.id());
            Map<String, Integer> memberCapacities = new HashMap<>(); // Would come from a real repository
            
            // Simulated member capacities
            memberCapacities.put("member1", 10);
            memberCapacities.put("member2", 15);
            memberCapacities.put("member3", 20);
            
            for (Map.Entry<String, Integer> memberEntry : memberLoads.entrySet()) {
                String memberId = memberEntry.getKey();
                int memberLoad = memberEntry.getValue();
                int memberCapacity = memberCapacities.getOrDefault(memberId, 10);
                
                if (memberLoad > memberCapacity * 0.9) { // More than 90% capacity is a risk
                    // This member is at risk of overload
                    List<WorkItemRisk> contributingItems = new ArrayList<>();
                    Map<String, Double> riskFactors = new HashMap<>();
                    
                    // In a real implementation, these would be derived from actual data
                    contributingItems.add(simulateWorkItemRisk("High Priority Task"));
                    contributingItems.add(simulateWorkItemRisk("Complex Integration"));
                    
                    riskFactors.put("Multiple high priority items", 0.4);
                    riskFactors.put("Tasks from different domains", 0.3);
                    riskFactors.put("Recent increase in work", 0.3);
                    
                    risks.add(new OverloadRiskData(
                            unit.id(),
                            unit.name(),
                            memberId,
                            "Team Member " + memberId, // Would come from a real member service
                            memberLoad,
                            memberCapacity,
                            (double) memberLoad / memberCapacity * 100,
                            memberLoad > memberCapacity ? "High" : "Medium",
                            Instant.now().plus(14, ChronoUnit.DAYS), // Simulated projection
                            contributingItems,
                            riskFactors,
                            "Consider redistributing some tasks or adjusting deadlines"
                    ));
                }
            }
        });
        
        return risks;
    }
    
    @Override
    public List<ReassignmentRecommendation> generateReassignmentRecommendations() {
        List<ReassignmentRecommendation> recommendations = new ArrayList<>();
        
        // Get overloaded members
        List<OverloadRiskData> overloadRisks = identifyOverloadRisks();
        
        // Find potential reassignments for each overloaded member
        for (OverloadRiskData risk : overloadRisks) {
            if ("High".equals(risk.riskLevel())) {
                // Find work items that could be reassigned
                for (OverloadRiskData.WorkItemRisk workItemRisk : risk.contributingWorkItems()) {
                    // Find a suitable member to reassign to
                    UUID recommendedUnit = findSuitableUnit(workItemRisk.workItemId());
                    String recommendedMember = findSuitableMember(recommendedUnit, workItemRisk.workItemId());
                    
                    if (recommendedMember != null) {
                        // Calculate utilization changes
                        double currentUtilizationBefore = risk.utilizationPercentage();
                        double currentUtilizationAfter = calculateUtilizationAfterRemoval(
                                risk.unitId(), risk.memberId(), workItemRisk.workItemId());
                        
                        double recommendedUtilizationBefore = calculateCurrentUtilization(
                                recommendedUnit, recommendedMember);
                        double recommendedUtilizationAfter = calculateUtilizationAfterAddition(
                                recommendedUnit, recommendedMember, workItemRisk.workItemId());
                        
                        // Calculate system balance improvement
                        double systemBalanceImprovement = calculateSystemBalanceImprovement(
                                currentUtilizationBefore, currentUtilizationAfter,
                                recommendedUtilizationBefore, recommendedUtilizationAfter);
                        
                        // Generate recommendation
                        ReassignmentRecommendation recommendation = createReassignmentRecommendation(
                                workItemRisk.workItemId(),
                                risk.unitId(), risk.unitName(), risk.memberId(), risk.memberName(),
                                recommendedUnit, recommendedMember,
                                currentUtilizationBefore, currentUtilizationAfter,
                                recommendedUtilizationBefore, recommendedUtilizationAfter,
                                systemBalanceImprovement);
                        
                        recommendations.add(recommendation);
                    }
                }
            }
        }
        
        // Sort recommendations by system balance improvement
        recommendations.sort(Comparator.comparing(ReassignmentRecommendation::systemBalanceImprovement).reversed());
        
        return recommendations.stream().limit(10).collect(Collectors.toList());
    }
    
    @Override
    public UnitDashboardData generateUnitDashboard(UUID unitId) {
        // This would be implemented with actual queries in a real implementation
        Instant now = Instant.now();
        
        // Get unit information
        String unitName = "Engineering Team"; // Would come from actual unit
        String unitDescription = "Software development team responsible for core features";
        
        // Simulated member data
        List<UnitDashboardData.MemberData> members = simulateMembers();
        
        // Simulated cognitive load metrics
        int totalCapacity = members.stream().mapToInt(UnitDashboardData.MemberData::capacity).sum();
        int totalAssignedLoad = members.stream().mapToInt(UnitDashboardData.MemberData::assignedLoad).sum();
        double utilizationPercentage = totalCapacity > 0 ? (double) totalAssignedLoad / totalCapacity * 100 : 0;
        
        // Simulated distributions
        Map<CynefinDomain, Integer> domainDistribution = simulateDomainDistribution();
        Map<WorkParadigm, Integer> paradigmDistribution = simulateParadigmDistribution();
        Map<WorkItemType, Integer> typeDistribution = simulateTypeDistribution();
        Map<String, Integer> tagDistribution = simulateTagDistribution();
        
        // Simulated work item lists
        List<UnitDashboardData.WorkItemSummary> activeWorkItems = simulateActiveWorkItems();
        List<UnitDashboardData.WorkItemSummary> upcomingWorkItems = simulateUpcomingWorkItems();
        List<UnitDashboardData.WorkItemSummary> recentlyCompletedItems = simulateRecentlyCompletedItems();
        
        // Simulated expertise levels
        Map<String, Double> expertiseLevels = simulateExpertiseLevels();
        
        // Get member overload risks
        List<OverloadRiskData> memberOverloadRisks = identifyOverloadRisks().stream()
                .filter(risk -> risk.unitId().equals(unitId))
                .collect(Collectors.toList());
        
        // Simulated domain expertise
        List<UnitDashboardData.DomainExpertise> domainExpertise = simulateDomainExpertise();
        
        return new UnitDashboardData(
                unitId,
                unitName,
                unitDescription,
                now,
                members.size(),
                members,
                totalCapacity,
                totalAssignedLoad,
                utilizationPercentage,
                domainDistribution,
                paradigmDistribution,
                typeDistribution,
                tagDistribution,
                activeWorkItems,
                upcomingWorkItems,
                recentlyCompletedItems,
                expertiseLevels,
                memberOverloadRisks,
                domainExpertise
        );
    }
    
    @Override
    public EstimationAccuracyData generateEstimationAccuracyReport(Instant startDate, Instant endDate) {
        // This would be implemented with actual historical data in a real implementation
        Instant now = Instant.now();
        
        // Simulated metrics
        int totalItemsAnalyzed = 120;
        double overallAccuracyPercentage = 78.5;
        double averageEstimationError = 3.2;
        double estimationBias = 1.5; // Positive means overestimation
        
        // Simulated domain accuracy
        Map<CynefinDomain, Double> accuracyByDomain = new HashMap<>();
        accuracyByDomain.put(CynefinDomain.OBVIOUS, 92.0);
        accuracyByDomain.put(CynefinDomain.COMPLICATED, 85.0);
        accuracyByDomain.put(CynefinDomain.COMPLEX, 65.0);
        accuracyByDomain.put(CynefinDomain.CHAOTIC, 40.0);
        
        // Simulated paradigm accuracy
        Map<WorkParadigm, Double> accuracyByParadigm = new HashMap<>();
        accuracyByParadigm.put(WorkParadigm.TASK, 90.0);
        accuracyByParadigm.put(WorkParadigm.ENGINEERING, 80.0);
        accuracyByParadigm.put(WorkParadigm.PRODUCT, 75.0);
        accuracyByParadigm.put(WorkParadigm.RESEARCH, 60.0);
        
        // Simulated type accuracy
        Map<WorkItemType, Double> accuracyByType = new HashMap<>();
        accuracyByType.put(WorkItemType.TASK, 85.0);
        accuracyByType.put(WorkItemType.BUG, 75.0);
        accuracyByType.put(WorkItemType.FEATURE, 70.0);
        accuracyByType.put(WorkItemType.EPIC, 60.0);
        
        // Simulated unit and member accuracy
        Map<UUID, Double> accuracyByUnit = simulateAccuracyByUnit();
        Map<String, Double> accuracyByMember = simulateAccuracyByMember();
        
        // Simulated best and worst estimations
        List<EstimationAccuracyData.EstimationComparisonItem> worstEstimations = simulateWorstEstimations();
        List<EstimationAccuracyData.EstimationComparisonItem> bestEstimations = simulateBestEstimations();
        
        // Simulated tag accuracy
        Map<String, Double> accuracyByTag = simulateAccuracyByTag();
        
        // Simulated accuracy trend
        List<EstimationAccuracyData.AccuracyTrend> accuracyTrend = simulateAccuracyTrend(startDate, endDate);
        
        return new EstimationAccuracyData(
                now,
                startDate,
                endDate,
                totalItemsAnalyzed,
                overallAccuracyPercentage,
                averageEstimationError,
                estimationBias,
                accuracyByDomain,
                accuracyByParadigm,
                accuracyByType,
                accuracyByUnit,
                accuracyByMember,
                worstEstimations,
                bestEstimations,
                accuracyByTag,
                accuracyTrend
        );
    }
    
    // Private helper methods
    
    private Map<CynefinDomain, Integer> calculateCynefinDistribution(List<WorkItem> items) {
        Map<CynefinDomain, Integer> distribution = new HashMap<>();
        for (CynefinDomain domain : CynefinDomain.values()) {
            distribution.put(domain, 0);
        }
        
        for (WorkItem item : items) {
            distribution.compute(item.cynefinDomain(), (k, v) -> v + 1);
        }
        
        return distribution;
    }
    
    private Map<WorkParadigm, Integer> calculateWorkParadigmDistribution(List<WorkItem> items) {
        Map<WorkParadigm, Integer> distribution = new HashMap<>();
        for (WorkParadigm paradigm : WorkParadigm.values()) {
            distribution.put(paradigm, 0);
        }
        
        for (WorkItem item : items) {
            distribution.compute(item.workParadigm(), (k, v) -> v + 1);
        }
        
        return distribution;
    }
    
    private Map<WorkItemType, Integer> calculateTypeDistribution(List<WorkItem> items) {
        Map<WorkItemType, Integer> distribution = new HashMap<>();
        for (WorkItemType type : WorkItemType.values()) {
            distribution.put(type, 0);
        }
        
        for (WorkItem item : items) {
            distribution.compute(item.type(), (k, v) -> v + 1);
        }
        
        return distribution;
    }
    
    private Map<String, Integer> calculateTagDistribution(List<WorkItem> items) {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (WorkItem item : items) {
            for (String tag : item.tags()) {
                distribution.compute(tag, (k, v) -> v == null ? 1 : v + 1);
            }
        }
        
        return distribution;
    }
    
    private List<DashboardData.UnitSummary> prepareUnitSummaries() {
        List<DashboardData.UnitSummary> summaries = new ArrayList<>();
        
        organizationalUnitRepository.findAll().forEach(unit -> {
            int memberCount = 5; // Would come from unit.members().size()
            int assignedWorkItems = organizationalUnitRepository.findWorkItemsForUnit(unit.id()).size();
            
            // Calculate cognitive load
            int totalCognitiveLoad = 0;
            List<UUID> workItemIds = organizationalUnitRepository.findWorkItemsForUnit(unit.id());
            for (UUID workItemId : workItemIds) {
                itemRepository.findById(workItemId).ifPresent(item -> {
                    // This would be more efficient with a bulk operation in a real implementation
                });
            }
            
            // For demonstration purposes
            totalCognitiveLoad = 28;
            double averageMemberLoad = memberCount > 0 ? (double) totalCognitiveLoad / memberCount : 0;
            boolean overloaded = false;
            double capacityPercentage = 75.0;
            
            summaries.add(new DashboardData.UnitSummary(
                    unit.id(),
                    unit.name(),
                    memberCount,
                    assignedWorkItems,
                    totalCognitiveLoad,
                    averageMemberLoad,
                    overloaded,
                    capacityPercentage
            ));
        });
        
        return summaries;
    }
    
    private List<CognitiveLoadDashboardData.UnitLoadData> prepareUnitLoadData() {
        List<CognitiveLoadDashboardData.UnitLoadData> unitLoadData = new ArrayList<>();
        
        organizationalUnitRepository.findAll().forEach(unit -> {
            // Simulated member load data
            List<CognitiveLoadDashboardData.MemberLoadData> memberLoads = new ArrayList<>();
            
            // In a real implementation, this would query from the assignment repository
            memberLoads.add(createSampleMemberLoadData(unit.id(), "member1", "John Doe"));
            memberLoads.add(createSampleMemberLoadData(unit.id(), "member2", "Jane Smith"));
            memberLoads.add(createSampleMemberLoadData(unit.id(), "member3", "Bob Johnson"));
            
            int totalCapacity = memberLoads.stream()
                    .mapToInt(CognitiveLoadDashboardData.MemberLoadData::capacity)
                    .sum();
            
            int assignedLoad = memberLoads.stream()
                    .mapToInt(CognitiveLoadDashboardData.MemberLoadData::assignedLoad)
                    .sum();
            
            double utilizationPercentage = totalCapacity > 0 ?
                    (double) assignedLoad / totalCapacity * 100 : 0;
            
            boolean overUtilized = utilizationPercentage > 90;
            
            unitLoadData.add(new CognitiveLoadDashboardData.UnitLoadData(
                    unit.id(),
                    unit.name(),
                    memberLoads.size(),
                    totalCapacity,
                    assignedLoad,
                    utilizationPercentage,
                    overUtilized,
                    memberLoads
            ));
        });
        
        return unitLoadData;
    }
    
    private CognitiveLoadDashboardData.MemberLoadData createSampleMemberLoadData(UUID unitId, String memberId, String memberName) {
        // This would be derived from actual data in a real implementation
        int capacity = switch (memberId) {
            case "member1" -> 10;
            case "member2" -> 15;
            case "member3" -> 20;
            default -> 10;
        };
        
        int assignedLoad = switch (memberId) {
            case "member1" -> 8;
            case "member2" -> 16; // Overloaded
            case "member3" -> 12;
            default -> 5;
        };
        
        double utilizationPercentage = (double) assignedLoad / capacity * 100;
        boolean overloaded = utilizationPercentage > 90;
        
        Map<String, Integer> loadBySkill = new HashMap<>();
        loadBySkill.put("Java Development", 5);
        loadBySkill.put("UX Design", 2);
        loadBySkill.put("Testing", 1);
        
        return new CognitiveLoadDashboardData.MemberLoadData(
                unitId,
                memberId,
                memberName,
                capacity,
                assignedLoad,
                utilizationPercentage,
                overloaded,
                loadBySkill
        );
    }
    
    // Trend analysis simulation methods
    
    private double simulateSystemLoad(Instant date) {
        // This would use actual historical data in a real implementation
        // For demonstration, we're simulating a sinusoidal pattern with noise
        long days = ChronoUnit.DAYS.between(Instant.EPOCH, date);
        double baseValue = 50 + 20 * Math.sin(days * Math.PI / 30);
        double noise = 10 * Math.random();
        return baseValue + noise;
    }
    
    private Map<String, Double> simulateLoadBreakdown(Instant date) {
        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("Development", 40.0 + 5 * Math.random());
        breakdown.put("Testing", 30.0 + 3 * Math.random());
        breakdown.put("Documentation", 15.0 + 2 * Math.random());
        breakdown.put("Other", 15.0 + 2 * Math.random());
        return breakdown;
    }
    
    private double simulateDomainLoad(CynefinDomain domain, Instant date) {
        // Base value depends on domain
        double baseValue = switch (domain) {
            case OBVIOUS -> 30;
            case COMPLICATED -> 40;
            case COMPLEX -> 20;
            case CHAOTIC -> 10;
        };
        
        // Add temporal variation
        long days = ChronoUnit.DAYS.between(Instant.EPOCH, date);
        double temporalFactor = Math.sin(days * Math.PI / 45);
        
        return baseValue + 10 * temporalFactor;
    }
    
    private Map<String, Double> simulateDomainLoadBreakdown(CynefinDomain domain, Instant date) {
        Map<String, Double> breakdown = new HashMap<>();
        
        switch (domain) {
            case OBVIOUS:
                breakdown.put("Routine Tasks", 60.0 + 5 * Math.random());
                breakdown.put("Well-defined Work", 40.0 + 5 * Math.random());
                break;
            case COMPLICATED:
                breakdown.put("Analysis", 40.0 + 3 * Math.random());
                breakdown.put("Design", 35.0 + 3 * Math.random());
                breakdown.put("Implementation", 25.0 + 3 * Math.random());
                break;
            case COMPLEX:
                breakdown.put("Experimentation", 50.0 + 5 * Math.random());
                breakdown.put("Adaptation", 30.0 + 3 * Math.random());
                breakdown.put("Collaboration", 20.0 + 2 * Math.random());
                break;
            case CHAOTIC:
                breakdown.put("Crisis Response", 70.0 + 10 * Math.random());
                breakdown.put("Stabilization", 30.0 + 10 * Math.random());
                break;
        }
        
        return breakdown;
    }
    
    private double simulateParadigmLoad(WorkParadigm paradigm, Instant date) {
        // Base value depends on paradigm
        double baseValue = switch (paradigm) {
            case TASK -> 35;
            case ENGINEERING -> 40;
            case PRODUCT -> 15;
            case RESEARCH -> 10;
        };
        
        // Add temporal variation
        long days = ChronoUnit.DAYS.between(Instant.EPOCH, date);
        double temporalFactor = Math.cos(days * Math.PI / 60);
        
        return baseValue + 15 * temporalFactor;
    }
    
    private Map<String, Double> simulateParadigmLoadBreakdown(WorkParadigm paradigm, Instant date) {
        Map<String, Double> breakdown = new HashMap<>();
        
        switch (paradigm) {
            case TASK:
                breakdown.put("Regular Tasks", 70.0 + 5 * Math.random());
                breakdown.put("Maintenance", 30.0 + 5 * Math.random());
                break;
            case ENGINEERING:
                breakdown.put("Development", 50.0 + 3 * Math.random());
                breakdown.put("Design", 30.0 + 3 * Math.random());
                breakdown.put("Testing", 20.0 + 3 * Math.random());
                break;
            case PRODUCT:
                breakdown.put("Market Research", 40.0 + 5 * Math.random());
                breakdown.put("User Testing", 30.0 + 3 * Math.random());
                breakdown.put("Feature Design", 30.0 + 2 * Math.random());
                break;
            case RESEARCH:
                breakdown.put("Exploration", 60.0 + 10 * Math.random());
                breakdown.put("Analysis", 40.0 + 10 * Math.random());
                break;
        }
        
        return breakdown;
    }
    
    private List<TrendAnalysisData.TimeSeriesPoint> simulateCompletionRateTrend(
            Instant startDate, Instant endDate, int intervalDays) {
        List<TrendAnalysisData.TimeSeriesPoint> trend = new ArrayList<>();
        
        Instant currentDate = startDate;
        while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
            // Simulated completion rate with gradual improvement
            long days = ChronoUnit.DAYS.between(startDate, currentDate);
            double completionRate = 60.0 + 10.0 * Math.log10(days + 10) + 5 * Math.random();
            
            Map<String, Double> breakdown = new HashMap<>();
            breakdown.put("On Time", 70.0 + 5 * Math.random());
            breakdown.put("Delayed", 30.0 + 5 * Math.random());
            
            trend.add(new TrendAnalysisData.TimeSeriesPoint(currentDate, completionRate, breakdown));
            
            // Move to next interval
            currentDate = currentDate.plus(intervalDays, ChronoUnit.DAYS);
        }
        
        return trend;
    }
    
    private List<TrendAnalysisData.TimeSeriesPoint> simulateNewWorkItemsTrend(
            Instant startDate, Instant endDate, int intervalDays) {
        List<TrendAnalysisData.TimeSeriesPoint> trend = new ArrayList<>();
        
        Instant currentDate = startDate;
        while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
            // Simulated new work items with weekly pattern
            long days = ChronoUnit.DAYS.between(Instant.EPOCH, currentDate);
            double newItems = 20.0 + 10.0 * Math.sin(days * Math.PI / 7) + 5 * Math.random();
            
            Map<String, Double> breakdown = new HashMap<>();
            breakdown.put("Tasks", 50.0 + 5 * Math.random());
            breakdown.put("Bugs", 30.0 + 5 * Math.random());
            breakdown.put("Features", 20.0 + 5 * Math.random());
            
            trend.add(new TrendAnalysisData.TimeSeriesPoint(currentDate, newItems, breakdown));
            
            // Move to next interval
            currentDate = currentDate.plus(intervalDays, ChronoUnit.DAYS);
        }
        
        return trend;
    }
    
    private Map<String, List<TrendAnalysisData.TimeSeriesPoint>> simulateTagTrends(
            Instant startDate, Instant endDate, int intervalDays) {
        Map<String, List<TrendAnalysisData.TimeSeriesPoint>> trends = new HashMap<>();
        
        // Common tags
        List<String> tags = List.of("UI", "Backend", "Database", "API", "Performance");
        
        for (String tag : tags) {
            List<TrendAnalysisData.TimeSeriesPoint> tagTrend = new ArrayList<>();
            
            Instant currentDate = startDate;
            while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
                // Simulated trend for this tag
                long days = ChronoUnit.DAYS.between(Instant.EPOCH, currentDate);
                double value = 15.0 + 8.0 * Math.sin(days * Math.PI / (15 + tags.indexOf(tag) * 3)) + 3 * Math.random();
                
                Map<String, Double> breakdown = new HashMap<>();
                breakdown.put("Active", 70.0 + 10 * Math.random());
                breakdown.put("Completed", 30.0 + 10 * Math.random());
                
                tagTrend.add(new TrendAnalysisData.TimeSeriesPoint(currentDate, value, breakdown));
                
                // Move to next interval
                currentDate = currentDate.plus(intervalDays, ChronoUnit.DAYS);
            }
            
            trends.put(tag, tagTrend);
        }
        
        return trends;
    }
    
    private Map<UUID, List<TrendAnalysisData.TimeSeriesPoint>> simulateUnitLoadTrends(
            Instant startDate, Instant endDate, int intervalDays) {
        Map<UUID, List<TrendAnalysisData.TimeSeriesPoint>> trends = new HashMap<>();
        
        // Simulated units
        List<UUID> unitIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        
        for (UUID unitId : unitIds) {
            List<TrendAnalysisData.TimeSeriesPoint> unitTrend = new ArrayList<>();
            
            Instant currentDate = startDate;
            while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
                // Simulated trend for this unit
                long days = ChronoUnit.DAYS.between(Instant.EPOCH, currentDate);
                double baseValue = 30.0 + 10.0 * (unitIds.indexOf(unitId) + 1);
                double value = baseValue + 15.0 * Math.sin(days * Math.PI / (20 + unitIds.indexOf(unitId) * 5)) + 5 * Math.random();
                
                Map<String, Double> breakdown = new HashMap<>();
                breakdown.put("Development", 50.0 + 10 * Math.random());
                breakdown.put("Testing", 30.0 + 5 * Math.random());
                breakdown.put("Design", 20.0 + 5 * Math.random());
                
                unitTrend.add(new TrendAnalysisData.TimeSeriesPoint(currentDate, value, breakdown));
                
                // Move to next interval
                currentDate = currentDate.plus(intervalDays, ChronoUnit.DAYS);
            }
            
            trends.put(unitId, unitTrend);
        }
        
        return trends;
    }
    
    // Work item risk methods
    
    private OverloadRiskData.WorkItemRisk simulateWorkItemRisk(String title) {
        return new OverloadRiskData.WorkItemRisk(
                UUID.randomUUID(),
                title,
                8, // Cognitive load
                35.0, // Contribution percentage
                Instant.now().plus(7, ChronoUnit.DAYS), // Due date
                Math.random() > 0.7, // 30% chance of being blocking
                (int) (Math.random() * 3) // 0-2 dependent items
        );
    }
    
    // Reassignment recommendation methods
    
    private UUID findSuitableUnit(UUID workItemId) {
        // This would be implemented with real unit selection logic in a real implementation
        return UUID.randomUUID();
    }
    
    private String findSuitableMember(UUID unitId, UUID workItemId) {
        // This would be implemented with real member selection logic in a real implementation
        return "member" + (int) (Math.random() * 5 + 1);
    }
    
    private double calculateUtilizationAfterRemoval(UUID unitId, String memberId, UUID workItemId) {
        // This would calculate the new utilization after removing a work item
        return 65.0; // Simulated value
    }
    
    private double calculateCurrentUtilization(UUID unitId, String memberId) {
        // This would calculate the current utilization of a member
        return 50.0; // Simulated value
    }
    
    private double calculateUtilizationAfterAddition(UUID unitId, String memberId, UUID workItemId) {
        // This would calculate the new utilization after adding a work item
        return 65.0; // Simulated value
    }
    
    private double calculateSystemBalanceImprovement(
            double currentBefore, double currentAfter,
            double recommendedBefore, double recommendedAfter) {
        // Calculate system balance improvement
        double beforeImbalance = Math.abs(currentBefore - recommendedBefore);
        double afterImbalance = Math.abs(currentAfter - recommendedAfter);
        
        return beforeImbalance - afterImbalance;
    }
    
    private ReassignmentRecommendation createReassignmentRecommendation(
            UUID workItemId,
            UUID currentUnitId, String currentUnitName, String currentMemberId, String currentMemberName,
            UUID recommendedUnitId, String recommendedMember,
            double currentUtilizationBefore, double currentUtilizationAfter,
            double recommendedUtilizationBefore, double recommendedUtilizationAfter,
            double systemBalanceImprovement) {
        
        // This would be implemented with real data in a real implementation
        String workItemTitle = "Sample Work Item";
        int cognitiveLoad = 6;
        String recommendedUnitName = "Team Alpha";
        String recommendedMemberName = "Alice Johnson";
        
        List<String> rationaleFactors = List.of(
                "Better skill match",
                "Reduces overload risk",
                "Balances team workload"
        );
        
        Map<String, Double> skillMatch = new HashMap<>();
        skillMatch.put("Java Development", 85.0);
        skillMatch.put("UI Design", 70.0);
        
        Map<String, Object> additionalMetrics = new HashMap<>();
        additionalMetrics.put("deadlineImpact", "None");
        additionalMetrics.put("qualityRisk", "Low");
        
        return new ReassignmentRecommendation(
                workItemId,
                workItemTitle,
                cognitiveLoad,
                currentUnitId,
                currentUnitName,
                currentMemberId,
                currentMemberName,
                recommendedUnitId,
                recommendedUnitName,
                recommendedMember,
                recommendedMemberName,
                currentUtilizationBefore,
                currentUtilizationAfter,
                recommendedUtilizationBefore,
                recommendedUtilizationAfter,
                systemBalanceImprovement,
                rationaleFactors,
                skillMatch,
                additionalMetrics
        );
    }
    
    // Unit dashboard simulation methods
    
    private List<UnitDashboardData.MemberData> simulateMembers() {
        List<UnitDashboardData.MemberData> members = new ArrayList<>();
        
        members.add(createSampleMember("member1", "John Doe"));
        members.add(createSampleMember("member2", "Jane Smith"));
        members.add(createSampleMember("member3", "Bob Johnson"));
        
        return members;
    }
    
    private UnitDashboardData.MemberData createSampleMember(String memberId, String name) {
        int capacity = 10 + (int) (Math.random() * 10);
        int assignedLoad = (int) (capacity * (0.6 + Math.random() * 0.4));
        double utilizationPercentage = (double) assignedLoad / capacity * 100;
        boolean overloaded = utilizationPercentage > 90;
        
        List<UUID> assignedWorkItems = List.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        
        Map<String, Integer> expertiseLevels = new HashMap<>();
        expertiseLevels.put("Java", 4);
        expertiseLevels.put("Spring", 3);
        expertiseLevels.put("AWS", 2);
        
        return new UnitDashboardData.MemberData(
                memberId,
                name,
                capacity,
                assignedLoad,
                utilizationPercentage,
                overloaded,
                assignedWorkItems,
                expertiseLevels
        );
    }
    
    private Map<CynefinDomain, Integer> simulateDomainDistribution() {
        Map<CynefinDomain, Integer> distribution = new HashMap<>();
        distribution.put(CynefinDomain.OBVIOUS, 10);
        distribution.put(CynefinDomain.COMPLICATED, 15);
        distribution.put(CynefinDomain.COMPLEX, 8);
        distribution.put(CynefinDomain.CHAOTIC, 2);
        return distribution;
    }
    
    private Map<WorkParadigm, Integer> simulateParadigmDistribution() {
        Map<WorkParadigm, Integer> distribution = new HashMap<>();
        distribution.put(WorkParadigm.TASK, 12);
        distribution.put(WorkParadigm.ENGINEERING, 18);
        distribution.put(WorkParadigm.PRODUCT, 3);
        distribution.put(WorkParadigm.RESEARCH, 2);
        return distribution;
    }
    
    private Map<WorkItemType, Integer> simulateTypeDistribution() {
        Map<WorkItemType, Integer> distribution = new HashMap<>();
        distribution.put(WorkItemType.TASK, 20);
        distribution.put(WorkItemType.BUG, 8);
        distribution.put(WorkItemType.FEATURE, 5);
        distribution.put(WorkItemType.EPIC, 2);
        return distribution;
    }
    
    private Map<String, Integer> simulateTagDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("Frontend", 12);
        distribution.put("Backend", 15);
        distribution.put("Database", 8);
        distribution.put("API", 10);
        distribution.put("DevOps", 5);
        return distribution;
    }
    
    private List<UnitDashboardData.WorkItemSummary> simulateActiveWorkItems() {
        List<UnitDashboardData.WorkItemSummary> items = new ArrayList<>();
        
        items.add(createSampleWorkItem("Implement User Authentication", "In Progress"));
        items.add(createSampleWorkItem("Fix Database Connection Issue", "In Progress"));
        items.add(createSampleWorkItem("Add Pagination to Results", "In Progress"));
        
        return items;
    }
    
    private List<UnitDashboardData.WorkItemSummary> simulateUpcomingWorkItems() {
        List<UnitDashboardData.WorkItemSummary> items = new ArrayList<>();
        
        items.add(createSampleWorkItem("Design New Dashboard UI", "Planned"));
        items.add(createSampleWorkItem("Optimize API Performance", "Planned"));
        
        return items;
    }
    
    private List<UnitDashboardData.WorkItemSummary> simulateRecentlyCompletedItems() {
        List<UnitDashboardData.WorkItemSummary> items = new ArrayList<>();
        
        items.add(createSampleWorkItem("Implement Login Screen", "Completed"));
        items.add(createSampleWorkItem("Fix Memory Leak", "Completed"));
        
        return items;
    }
    
    private UnitDashboardData.WorkItemSummary createSampleWorkItem(String title, String status) {
        return new UnitDashboardData.WorkItemSummary(
                UUID.randomUUID(),
                title,
                (int) (Math.random() * 10 + 1),
                getRandomElement(CynefinDomain.values()),
                getRandomElement(WorkParadigm.values()),
                getRandomElement(WorkItemType.values()),
                "member" + (int) (Math.random() * 3 + 1),
                status,
                Instant.now().plus((long) (Math.random() * 14 + 1), ChronoUnit.DAYS),
                Math.random() > 0.8,
                (int) (Math.random() * 3)
        );
    }
    
    private <T> T getRandomElement(T[] array) {
        return array[new Random().nextInt(array.length)];
    }
    
    private Map<String, Double> simulateExpertiseLevels() {
        Map<String, Double> levels = new HashMap<>();
        levels.put("Java Development", 4.2);
        levels.put("Spring Framework", 3.8);
        levels.put("AWS Cloud", 3.0);
        levels.put("UI Design", 2.5);
        levels.put("DevOps", 3.5);
        return levels;
    }
    
    private List<UnitDashboardData.DomainExpertise> simulateDomainExpertise() {
        List<UnitDashboardData.DomainExpertise> expertise = new ArrayList<>();
        
        expertise.add(createSampleDomainExpertise("Backend Development", 4));
        expertise.add(createSampleDomainExpertise("Frontend Development", 3));
        expertise.add(createSampleDomainExpertise("DevOps", 2));
        
        return expertise;
    }
    
    private UnitDashboardData.DomainExpertise createSampleDomainExpertise(String domain, int level) {
        List<String> expertMembers = new ArrayList<>();
        if (level >= 4) {
            expertMembers.add("John Doe");
            expertMembers.add("Jane Smith");
        } else if (level >= 3) {
            expertMembers.add("Jane Smith");
        }
        
        Map<CynefinDomain, Integer> expertiseByDomain = new HashMap<>();
        for (CynefinDomain cynefinDomain : CynefinDomain.values()) {
            expertiseByDomain.put(cynefinDomain, 1 + (int) (Math.random() * 4));
        }
        
        Map<WorkParadigm, Integer> expertiseByParadigm = new HashMap<>();
        for (WorkParadigm paradigm : WorkParadigm.values()) {
            expertiseByParadigm.put(paradigm, 1 + (int) (Math.random() * 4));
        }
        
        return new UnitDashboardData.DomainExpertise(
                domain,
                level,
                expertMembers,
                expertiseByDomain,
                expertiseByParadigm
        );
    }
    
    // Estimation accuracy methods
    
    private Map<UUID, Double> simulateAccuracyByUnit() {
        Map<UUID, Double> accuracy = new HashMap<>();
        accuracy.put(UUID.randomUUID(), 82.5);
        accuracy.put(UUID.randomUUID(), 75.0);
        accuracy.put(UUID.randomUUID(), 68.5);
        return accuracy;
    }
    
    private Map<String, Double> simulateAccuracyByMember() {
        Map<String, Double> accuracy = new HashMap<>();
        accuracy.put("John Doe", 85.0);
        accuracy.put("Jane Smith", 78.5);
        accuracy.put("Bob Johnson", 72.0);
        accuracy.put("Alice Brown", 90.0);
        return accuracy;
    }
    
    private List<EstimationAccuracyData.EstimationComparisonItem> simulateWorstEstimations() {
        List<EstimationAccuracyData.EstimationComparisonItem> items = new ArrayList<>();
        
        items.add(createEstimationComparisonItem("Implement Complex Algorithm", 5, 15, -200.0));
        items.add(createEstimationComparisonItem("Refactor Legacy Code", 8, 20, -150.0));
        items.add(createEstimationComparisonItem("Fix Security Vulnerability", 3, 7, -133.3));
        
        return items;
    }
    
    private List<EstimationAccuracyData.EstimationComparisonItem> simulateBestEstimations() {
        List<EstimationAccuracyData.EstimationComparisonItem> items = new ArrayList<>();
        
        items.add(createEstimationComparisonItem("Add Form Validation", 4, 4, 0.0));
        items.add(createEstimationComparisonItem("Update Documentation", 3, 3, 0.0));
        items.add(createEstimationComparisonItem("Add Button to UI", 2, 2, 0.0));
        
        return items;
    }
    
    private EstimationAccuracyData.EstimationComparisonItem createEstimationComparisonItem(
            String title, int estimatedLoad, int actualLoad, double errorPercentage) {
        return new EstimationAccuracyData.EstimationComparisonItem(
                UUID.randomUUID(),
                title,
                estimatedLoad,
                actualLoad,
                errorPercentage,
                "member" + (int) (Math.random() * 3 + 1),
                getRandomElement(CynefinDomain.values()),
                getRandomElement(WorkParadigm.values()),
                getRandomElement(WorkItemType.values()),
                List.of("Tag1", "Tag2"),
                Instant.now().minus((long) (Math.random() * 30 + 1), ChronoUnit.DAYS)
        );
    }
    
    private Map<String, Double> simulateAccuracyByTag() {
        Map<String, Double> accuracy = new HashMap<>();
        accuracy.put("Frontend", 82.0);
        accuracy.put("Backend", 76.5);
        accuracy.put("Database", 70.0);
        accuracy.put("API", 75.5);
        accuracy.put("DevOps", 65.0);
        return accuracy;
    }
    
    private List<EstimationAccuracyData.AccuracyTrend> simulateAccuracyTrend(Instant startDate, Instant endDate) {
        List<EstimationAccuracyData.AccuracyTrend> trend = new ArrayList<>();
        
        // Weekly points
        Instant currentDate = startDate;
        int weekNumber = 0;
        
        while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
            // Gradually improving accuracy
            double baseAccuracy = 70.0 + weekNumber * 0.5;
            double randomVariation = 5.0 * Math.random();
            double accuracyPercentage = Math.min(baseAccuracy + randomVariation, 95.0);
            
            Map<CynefinDomain, Double> domainAccuracies = new HashMap<>();
            for (CynefinDomain domain : CynefinDomain.values()) {
                double domainBaseAccuracy = switch (domain) {
                    case OBVIOUS -> baseAccuracy + 10.0;
                    case COMPLICATED -> baseAccuracy + 5.0;
                    case COMPLEX -> baseAccuracy - 5.0;
                    case CHAOTIC -> baseAccuracy - 15.0;
                };
                domainAccuracies.put(domain, Math.min(Math.max(domainBaseAccuracy + 5.0 * Math.random(), 40.0), 95.0));
            }
            
            Map<WorkParadigm, Double> paradigmAccuracies = new HashMap<>();
            for (WorkParadigm paradigm : WorkParadigm.values()) {
                double paradigmBaseAccuracy = switch (paradigm) {
                    case TASK -> baseAccuracy + 10.0;
                    case ENGINEERING -> baseAccuracy + 2.0;
                    case PRODUCT -> baseAccuracy - 3.0;
                    case RESEARCH -> baseAccuracy - 10.0;
                };
                paradigmAccuracies.put(paradigm, Math.min(Math.max(paradigmBaseAccuracy + 5.0 * Math.random(), 40.0), 95.0));
            }
            
            trend.add(new EstimationAccuracyData.AccuracyTrend(
                    currentDate,
                    accuracyPercentage,
                    15 + (int) (Math.random() * 10), // 15-25 items per week
                    domainAccuracies,
                    paradigmAccuracies
            ));
            
            currentDate = currentDate.plus(7, ChronoUnit.DAYS);
            weekNumber++;
        }
        
        return trend;
    }
    
    // Trend data storage for caching
    private static class TrendData {
        private final double value;
        private final Map<String, Double> breakdown;
        
        public TrendData(double value, Map<String, Double> breakdown) {
            this.value = value;
            this.breakdown = breakdown;
        }
        
        public double getValue() {
            return value;
        }
        
        public Map<String, Double> getBreakdown() {
            return breakdown;
        }
    }
}