package org.rinna.domain.service.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dashboard data focusing on cognitive load across organizational units.
 */
public record CognitiveLoadDashboardData(
    Instant generatedAt,
    double averageLoadPerMember,
    int totalSystemCapacity,
    int totalAssignedLoad,
    double systemUtilizationPercentage,
    List<UnitLoadData> unitLoadData,
    List<MemberLoadData> topOverloadedMembers,
    List<MemberLoadData> topUnderloadedMembers,
    Map<String, Double> loadBySkillSet,
    Map<String, Integer> loadByExpertiseLevel
) {
    
    /**
     * Cognitive load data for an organizational unit.
     */
    public record UnitLoadData(
        UUID unitId,
        String name,
        int memberCount,
        int totalCapacity,
        int assignedLoad,
        double utilizationPercentage,
        boolean overUtilized,
        List<MemberLoadData> memberLoads
    ) {}
    
    /**
     * Cognitive load data for a team member.
     */
    public record MemberLoadData(
        UUID unitId,
        String memberId,
        String memberName,
        int capacity,
        int assignedLoad,
        double utilizationPercentage,
        boolean overloaded,
        Map<String, Integer> loadBySkill
    ) {}
}