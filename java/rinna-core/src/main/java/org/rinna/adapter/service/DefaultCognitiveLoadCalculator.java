/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OrganizationalUnit;
import org.rinna.domain.model.OrganizationalUnitType;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.repository.OrganizationalUnitRepository;
import org.rinna.domain.service.CognitiveLoadCalculator;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.WorkItemAssignmentRepository;

/**
 * Default implementation of the CognitiveLoadCalculator interface.
 * This class provides algorithms for calculating and managing cognitive load
 * in the Ryorin-do framework.
 */
public class DefaultCognitiveLoadCalculator implements CognitiveLoadCalculator {

    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final ItemService itemService;
    private final WorkItemAssignmentRepository assignmentRepository;

    // Historical adjustment factors based on actual vs. estimated effort
    private final Map<WorkItemType, Double> typeAdjustmentFactors = new HashMap<>();
    private final Map<CynefinDomain, Double> domainAdjustmentFactors = new HashMap<>();
    private final Map<WorkParadigm, Double> paradigmAdjustmentFactors = new HashMap<>();

    /**
     * Creates a new DefaultCognitiveLoadCalculator.
     *
     * @param organizationalUnitRepository the organizational unit repository
     * @param itemService the item service
     * @param assignmentRepository the work item assignment repository
     */
    public DefaultCognitiveLoadCalculator(
            OrganizationalUnitRepository organizationalUnitRepository,
            ItemService itemService,
            WorkItemAssignmentRepository assignmentRepository) {
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.itemService = itemService;
        this.assignmentRepository = assignmentRepository;

        // Initialize adjustment factors with default values
        initializeAdjustmentFactors();
    }

    private void initializeAdjustmentFactors() {
        // Default type adjustment factors
        typeAdjustmentFactors.put(WorkItemType.TASK, 1.0);
        typeAdjustmentFactors.put(WorkItemType.BUG, 1.2);
        typeAdjustmentFactors.put(WorkItemType.FEATURE, 2.0);
        typeAdjustmentFactors.put(WorkItemType.EPIC, 3.0);

        // Default domain adjustment factors
        domainAdjustmentFactors.put(CynefinDomain.OBVIOUS, 1.0);
        domainAdjustmentFactors.put(CynefinDomain.COMPLICATED, 1.5);
        domainAdjustmentFactors.put(CynefinDomain.COMPLEX, 2.0);
        domainAdjustmentFactors.put(CynefinDomain.CHAOTIC, 3.0);

        // Default paradigm adjustment factors
        paradigmAdjustmentFactors.put(WorkParadigm.TASK, 1.0);
        paradigmAdjustmentFactors.put(WorkParadigm.STORY, 1.2);
        paradigmAdjustmentFactors.put(WorkParadigm.EPIC, 1.5);
        paradigmAdjustmentFactors.put(WorkParadigm.EXPERIMENT, 2.0);
    }

    @Override
    public int calculateWorkItemLoad(WorkItem workItem) {
        int baseLoad = getBaseLoadForType(workItem.getType());

        // Apply priority adjustment
        double priorityFactor = getPriorityFactor(workItem.getPriority());

        // Apply domain adjustment
        double domainFactor = 1.0;
        CynefinDomain domain = workItem.cynefinDomain();
        if (domain != null) {
            domainFactor = getDomainLoadFactor(domain);
        }

        // Apply paradigm adjustment
        double paradigmFactor = 1.0;
        WorkParadigm paradigm = workItem.workParadigm();
        if (paradigm != null) {
            paradigmFactor = getParadigmLoadFactor(paradigm);
        }

        // Calculate final load
        double adjustedLoad = baseLoad * priorityFactor * domainFactor * paradigmFactor;

        return (int) Math.round(adjustedLoad);
    }

    private int getBaseLoadForType(WorkItemType type) {
        switch (type) {
            case TASK:
                return 5;
            case BUG:
                return 8;
            case CHORE:
                return 13;
            case FEATURE:
                return 20;
            case EPIC:
                return 40;
            case GOAL:
                return 50;
            default:
                return 10; // Default value for unknown types
        }
    }

    private double getPriorityFactor(Priority priority) {
        switch (priority) {
            case LOW:
                return 0.8;
            case MEDIUM:
                return 1.0;
            case HIGH:
                return 1.5; // Higher factor for HIGH since there's no CRITICAL
            default:
                return 1.0; // Default factor for unknown priorities
        }
    }

    @Override
    public int calculateTotalLoad(List<WorkItem> workItems) {
        return workItems.stream()
                .mapToInt(this::calculateWorkItemLoad)
                .sum();
    }

    @Override
    public int calculateImpact(OrganizationalUnit unit, WorkItem workItem) {
        int workItemLoad = calculateWorkItemLoad(workItem);
        return unit.getCurrentCognitiveLoad() + workItemLoad;
    }

    @Override
    public int recommendCapacity(OrganizationalUnit unit) {
        // Base capacity depends on organizational unit type
        int baseCapacity = getBaseCapacityForType(unit.getType());

        // Adjust capacity based on number of members
        int memberCount = unit.getMembers().size();
        int memberBasedCapacity = memberCount * 25;

        // Take the larger of the two capacity calculations
        int capacity = Math.max(baseCapacity, memberBasedCapacity);

        // Adjust based on domain expertise and work paradigms
        double expertiseFactor = calculateExpertiseFactor(unit);

        return (int) Math.round(capacity * expertiseFactor);
    }

    private int getBaseCapacityForType(OrganizationalUnitType type) {
        switch (type) {
            case SQUAD:
                return 75;
            case TEAM:
                return 100;
            case DEPARTMENT:
                return 250;
            case BUSINESS_UNIT:
                return 500;
            case TRIBE:
                return 400;
            case GUILD:
                return 150;
            case CHAPTER:
                return 200;
            case PRODUCT_TEAM:
                return 120;
            case PROJECT_TEAM:
                return 150;
            case VIRTUAL_TEAM:
                return 100;
            case CUSTOM:
                return 100;
            default:
                return 50; // Default capacity for unknown types
        }
    }

    private double calculateExpertiseFactor(OrganizationalUnit unit) {
        // Calculate factor based on domain expertise breadth
        double domainFactor = 1.0 + (unit.getDomainExpertise().size() * 0.05);

        // Calculate factor based on work paradigm breadth
        double paradigmFactor = 1.0 + (unit.getWorkParadigms().size() * 0.05);

        return domainFactor * paradigmFactor;
    }

    @Override
    public int calculateUtilizationPercentage(OrganizationalUnit unit) {
        if (unit.getCognitiveCapacity() == 0) {
            return 0; // Avoid division by zero
        }

        return (int) Math.round((double) unit.getCurrentCognitiveLoad() / unit.getCognitiveCapacity() * 100);
    }

    @Override
    public boolean isOverloaded(OrganizationalUnit unit, int thresholdPercentage) {
        return calculateUtilizationPercentage(unit) >= thresholdPercentage;
    }

    @Override
    public double getDomainLoadFactor(CynefinDomain domain) {
        return domainAdjustmentFactors.getOrDefault(domain, 1.0);
    }

    @Override
    public double getParadigmLoadFactor(WorkParadigm paradigm) {
        return paradigmAdjustmentFactors.getOrDefault(paradigm, 1.0);
    }

    @Override
    public int estimateIndividualCapacity(
            List<CynefinDomain> domainExpertise,
            List<WorkParadigm> paradigmExpertise,
            int experienceLevel) {

        // Base capacity for an individual
        int baseCapacity = 25;

        // Adjust based on experience level (1-5)
        double experienceFactor = 0.8 + (experienceLevel * 0.1);

        // Adjust based on domain expertise breadth
        double domainFactor = 1.0 + (domainExpertise.size() * 0.05);

        // Adjust based on paradigm expertise breadth
        double paradigmFactor = 1.0 + (paradigmExpertise.size() * 0.05);

        return (int) Math.round(baseCapacity * experienceFactor * domainFactor * paradigmFactor);
    }

    @Override
    public MemberLoadDistribution calculateMemberLoadDistribution(UUID unitId) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        if (!unitOptional.isPresent()) {
            return new MemberLoadDistribution(new ArrayList<>());
        }

        OrganizationalUnit unit = unitOptional.get();
        List<String> members = unit.getMembers();

        List<MemberLoad> memberLoads = new ArrayList<>();
        for (String memberId : members) {
            // Get work items assigned to this member
            List<UUID> assignedWorkItemIds = assignmentRepository.findWorkItemsByMember(unitId, memberId);
            List<WorkItem> assignedWorkItems = new ArrayList<>();

            for (UUID workItemId : assignedWorkItemIds) {
                itemService.findById(workItemId).ifPresent(assignedWorkItems::add);
            }

            // Calculate member's load
            int memberLoad = calculateTotalLoad(assignedWorkItems);

            // Estimate member's capacity (simplified version)
            int memberCapacity = 25;

            memberLoads.add(new MemberLoad(memberId, memberCapacity, memberLoad, assignedWorkItemIds));
        }

        return new MemberLoadDistribution(memberLoads);
    }

    @Override
    public String suggestMemberAssignment(UUID unitId, UUID workItemId) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        Optional<WorkItem> workItemOptional = itemService.findById(workItemId);

        if (!unitOptional.isPresent() || !workItemOptional.isPresent()) {
            return null;
        }

        OrganizationalUnit unit = unitOptional.get();
        WorkItem workItem = workItemOptional.get();

        // Get load distribution for all members
        MemberLoadDistribution distribution = calculateMemberLoadDistribution(unitId);

        // Find the member with the lowest utilization percentage
        return distribution.getMemberLoads().stream()
                .min(Comparator.comparingDouble(MemberLoad::getUtilizationPercentage))
                .map(MemberLoad::getMemberId)
                .orElse(null);
    }

    @Override
    public boolean optimizeWorkDistribution(UUID unitId) {
        Optional<OrganizationalUnit> unitOptional = organizationalUnitRepository.findById(unitId);
        if (!unitOptional.isPresent()) {
            return false;
        }

        OrganizationalUnit unit = unitOptional.get();
        MemberLoadDistribution distribution = calculateMemberLoadDistribution(unitId);

        // If the distribution is already balanced, no optimization is needed
        if (distribution.isBalanced(10.0)) {
            return false;
        }

        // Get all members sorted by load (most loaded to least loaded)
        List<MemberLoad> sortedLoads = distribution.getMemberLoads().stream()
                .sorted(Comparator.comparingDouble(MemberLoad::getUtilizationPercentage).reversed())
                .collect(Collectors.toList());

        if (sortedLoads.size() < 2) {
            return false; // Need at least 2 members to redistribute
        }

        // Find the most loaded and least loaded members
        MemberLoad mostLoaded = sortedLoads.get(0);
        MemberLoad leastLoaded = sortedLoads.get(sortedLoads.size() - 1);

        // If the difference is significant, redistribute a work item
        if (mostLoaded.getUtilizationPercentage() - leastLoaded.getUtilizationPercentage() > 20) {
            // Find a work item to redistribute
            List<UUID> workItemIds = mostLoaded.getAssignedWorkItems();
            if (workItemIds.isEmpty()) {
                return false;
            }

            // Find the smallest work item to redistribute
            UUID smallestWorkItemId = null;
            int smallestLoad = Integer.MAX_VALUE;

            for (UUID workItemId : workItemIds) {
                Optional<WorkItem> workItemOptional = itemService.findById(workItemId);
                if (workItemOptional.isPresent()) {
                    WorkItem workItem = workItemOptional.get();
                    int load = calculateWorkItemLoad(workItem);
                    if (load < smallestLoad) {
                        smallestLoad = load;
                        smallestWorkItemId = workItemId;
                    }
                }
            }

            if (smallestWorkItemId != null) {
                // Reassign the work item
                assignmentRepository.unassignWorkItem(unitId, mostLoaded.getMemberId(), smallestWorkItemId);
                assignmentRepository.assignWorkItem(unitId, leastLoaded.getMemberId(), smallestWorkItemId);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean recordActualEffort(UUID workItemId, int estimatedHours, int actualHours) {
        if (estimatedHours <= 0 || actualHours <= 0) {
            return false;
        }

        Optional<WorkItem> workItemOptional = itemService.findById(workItemId);
        if (!workItemOptional.isPresent()) {
            return false;
        }

        WorkItem workItem = workItemOptional.get();

        // Calculate the adjustment factor (actual / estimated)
        double adjustmentFactor = (double) actualHours / estimatedHours;

        // Update type adjustment factor
        WorkItemType type = workItem.getType();
        double currentTypeFactor = typeAdjustmentFactors.getOrDefault(type, 1.0);
        // Use a weighted average (90% previous, 10% new)
        double newTypeFactor = (currentTypeFactor * 0.9) + (adjustmentFactor * 0.1);
        typeAdjustmentFactors.put(type, newTypeFactor);

        // Update domain adjustment factor if available
        CynefinDomain domain = workItem.cynefinDomain();
        if (domain != null) {
            double currentDomainFactor = domainAdjustmentFactors.getOrDefault(domain, 1.0);
            double newDomainFactor = (currentDomainFactor * 0.9) + (adjustmentFactor * 0.1);
            domainAdjustmentFactors.put(domain, newDomainFactor);
        }

        // Update paradigm adjustment factor if available
        WorkParadigm paradigm = workItem.workParadigm();
        if (paradigm != null) {
            double currentParadigmFactor = paradigmAdjustmentFactors.getOrDefault(paradigm, 1.0);
            double newParadigmFactor = (currentParadigmFactor * 0.9) + (adjustmentFactor * 0.1);
            paradigmAdjustmentFactors.put(paradigm, newParadigmFactor);
        }

        return true;
    }
}
