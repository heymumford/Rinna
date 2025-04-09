package org.rinna.domain.service.dashboard;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.WorkParadigm;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Trend analysis data for cognitive load over time.
 */
public record TrendAnalysisData(
    Instant generatedAt,
    Instant startDate,
    Instant endDate,
    String interval,
    List<TimeSeriesPoint> systemLoadTrend,
    Map<UUID, List<TimeSeriesPoint>> unitLoadTrends,
    Map<CynefinDomain, List<TimeSeriesPoint>> domainLoadTrends,
    Map<WorkParadigm, List<TimeSeriesPoint>> paradigmLoadTrends,
    List<TimeSeriesPoint> completionRateTrend,
    List<TimeSeriesPoint> newWorkItemsTrend,
    Map<String, List<TimeSeriesPoint>> tagTrends
) {
    
    /**
     * A data point in a time series.
     */
    public record TimeSeriesPoint(
        Instant timestamp,
        double value,
        Map<String, Double> breakdown
    ) {}
}