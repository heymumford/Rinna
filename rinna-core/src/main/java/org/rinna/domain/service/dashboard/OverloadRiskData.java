package org.rinna.domain.service.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data about cognitive overload risks for organizational units or team members.
 */
public record OverloadRiskData(
    UUID unitId,
    String unitName,
    String memberId,
    String memberName,
    int currentLoad,
    int capacity,
    double utilizationPercentage,
    String riskLevel,
    Instant projectedOverloadDate,
    List<WorkItemRisk> contributingWorkItems,
    Map<String, Double> riskFactors,
    String recommendation
) {
    
    /**
     * Information about a work item contributing to overload risk.
     */
    public record WorkItemRisk(
        UUID workItemId,
        String title,
        int cognitiveLoad,
        double contributionPercentage,
        Instant dueDate,
        boolean isBlocking,
        int dependentItemsCount
    ) {}
}