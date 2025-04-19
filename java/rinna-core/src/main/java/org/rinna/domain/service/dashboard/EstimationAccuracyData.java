package org.rinna.domain.service.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkParadigm;

/**
 * Data comparing estimated vs. actual cognitive load for completed work items.
 */
public record EstimationAccuracyData(
    Instant generatedAt,
    Instant startDate,
    Instant endDate,
    int totalItemsAnalyzed,
    double overallAccuracyPercentage,
    double averageEstimationError,
    double estimationBias,
    Map<CynefinDomain, Double> accuracyByDomain,
    Map<WorkParadigm, Double> accuracyByParadigm,
    Map<WorkItemType, Double> accuracyByType,
    Map<UUID, Double> accuracyByUnit,
    Map<String, Double> accuracyByMember,
    List<EstimationComparisonItem> worstEstimations,
    List<EstimationComparisonItem> bestEstimations,
    Map<String, Double> accuracyByTag,
    List<AccuracyTrend> accuracyTrend
) {
    
    /**
     * Comparison between estimated and actual cognitive load for a work item.
     */
    public record EstimationComparisonItem(
        UUID workItemId,
        String title,
        int estimatedLoad,
        int actualLoad,
        double errorPercentage,
        String assignedMember,
        CynefinDomain domain,
        WorkParadigm paradigm,
        WorkItemType type,
        List<String> tags,
        Instant completedAt
    ) {}
    
    /**
     * Trend data point for estimation accuracy over time.
     */
    public record AccuracyTrend(
        Instant timestamp,
        double accuracyPercentage,
        int itemsInPeriod,
        Map<CynefinDomain, Double> domainAccuracies,
        Map<WorkParadigm, Double> paradigmAccuracies
    ) {}
}