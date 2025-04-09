package org.rinna.domain.service.dashboard;

import org.rinna.domain.model.CynefinDomain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Distribution data for CYNEFIN domains across work items.
 */
public record DomainDistributionData(
    Instant generatedAt,
    int totalWorkItems,
    Map<CynefinDomain, Integer> domainCounts,
    Map<CynefinDomain, Double> domainPercentages,
    Map<CynefinDomain, List<UUID>> workItemsByDomain,
    Map<CynefinDomain, Integer> averageLoadByDomain,
    Map<CynefinDomain, Double> completionRateByDomain,
    Map<UUID, Map<CynefinDomain, Integer>> unitDomainCounts
) {}