package org.rinna.domain.service.dashboard;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.model.WorkItemType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dashboard data for a specific organizational unit.
 */
public record UnitDashboardData(
    UUID unitId,
    String unitName,
    String unitDescription,
    Instant generatedAt,
    int memberCount,
    List<MemberData> members,
    int totalCapacity,
    int totalAssignedLoad,
    double utilizationPercentage,
    Map<CynefinDomain, Integer> domainDistribution,
    Map<WorkParadigm, Integer> paradigmDistribution,
    Map<WorkItemType, Integer> typeDistribution,
    Map<String, Integer> tagDistribution,
    List<WorkItemSummary> activeWorkItems,
    List<WorkItemSummary> upcomingWorkItems,
    List<WorkItemSummary> recentlyCompletedItems,
    Map<String, Double> expertiseLevels,
    List<OverloadRiskData> memberOverloadRisks,
    List<DomainExpertise> domainExpertise
) {
    
    /**
     * Data about a team member.
     */
    public record MemberData(
        String memberId,
        String name,
        int capacity,
        int assignedLoad,
        double utilizationPercentage,
        boolean overloaded,
        List<UUID> assignedWorkItems,
        Map<String, Integer> expertiseLevels
    ) {}
    
    /**
     * Summary of a work item.
     */
    public record WorkItemSummary(
        UUID id,
        String title,
        int cognitiveLoad,
        CynefinDomain domain,
        WorkParadigm paradigm,
        WorkItemType type,
        String assignedMemberId,
        String status,
        Instant dueDate,
        boolean isBlocking,
        int blockingItemsCount
    ) {}
    
    /**
     * Domain expertise for an organizational unit.
     */
    public record DomainExpertise(
        String domain,
        int expertiseLevel,
        List<String> expertMembers,
        Map<CynefinDomain, Integer> expertiseByDomain,
        Map<WorkParadigm, Integer> expertiseByParadigm
    ) {}
}