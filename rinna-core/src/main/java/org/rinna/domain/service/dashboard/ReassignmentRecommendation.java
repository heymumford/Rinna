package org.rinna.domain.service.dashboard;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recommendation for work item reassignments to optimize cognitive load balance.
 */
public record ReassignmentRecommendation(
    UUID workItemId,
    String workItemTitle,
    int cognitiveLoad,
    UUID currentUnitId,
    String currentUnitName,
    String currentMemberId,
    String currentMemberName,
    UUID recommendedUnitId,
    String recommendedUnitName,
    String recommendedMemberId,
    String recommendedMemberName,
    double currentUtilizationBefore,
    double currentUtilizationAfter,
    double recommendedUtilizationBefore,
    double recommendedUtilizationAfter,
    double systemBalanceImprovement,
    List<String> rationaleFactors,
    Map<String, Double> skillMatch,
    Map<String, Object> additionalMetrics
) {}