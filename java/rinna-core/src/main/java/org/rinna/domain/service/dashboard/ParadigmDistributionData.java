package org.rinna.domain.service.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.domain.model.WorkParadigm;

/**
 * Distribution data for work paradigms across work items.
 */
public record ParadigmDistributionData(
    Instant generatedAt,
    int totalWorkItems,
    Map<WorkParadigm, Integer> paradigmCounts,
    Map<WorkParadigm, Double> paradigmPercentages,
    Map<WorkParadigm, List<UUID>> workItemsByParadigm,
    Map<WorkParadigm, Integer> averageLoadByParadigm,
    Map<WorkParadigm, Double> completionRateByParadigm,
    Map<UUID, Map<WorkParadigm, Integer>> unitParadigmCounts
) {}