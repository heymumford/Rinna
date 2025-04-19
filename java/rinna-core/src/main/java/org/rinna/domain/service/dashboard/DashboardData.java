package org.rinna.domain.service.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkParadigm;

/**
 * Comprehensive dashboard data containing information about work items,
 * cognitive load, and organizational units.
 */
public record DashboardData(
    Instant generatedAt,
    int totalWorkItems,
    int totalActiveWorkItems,
    int totalBlockedWorkItems,
    int totalCompletedWorkItems,
    Map<CynefinDomain, Integer> cynefinDistribution,
    Map<WorkParadigm, Integer> paradigmDistribution,
    Map<WorkItemType, Integer> typeDistribution,
    List<UnitSummary> unitSummaries,
    List<OverloadRiskData> overloadRisks,
    List<ReassignmentRecommendation> recommendations,
    Map<String, Integer> tagDistribution,
    double averageCognitiveLoad,
    int maxCognitiveLoad,
    int totalAssignedLoad,
    double loadUtilizationPercentage
) {
    
    /**
     * Summary data for an organizational unit.
     */
    public record UnitSummary(
        UUID unitId,
        String name,
        int memberCount,
        int assignedWorkItems,
        int totalCognitiveLoad,
        double averageMemberLoad,
        boolean overloaded,
        double capacityPercentage
    ) {}
}