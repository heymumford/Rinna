/*
 * Service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import java.util.List;
import java.util.UUID;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OrganizationalUnit;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkParadigm;

/**
 * Service interface for calculating cognitive load in the Ryorin-do framework.
 * This component provides sophisticated cognitive load assessment algorithms
 * that help prevent team overload and optimize work assignment.
 */
public interface CognitiveLoadCalculator {
    
    /**
     * Calculates the cognitive load of a work item based on its properties.
     *
     * @param workItem the work item to analyze
     * @return the cognitive load value
     */
    int calculateWorkItemLoad(WorkItem workItem);
    
    /**
     * Calculates the total cognitive load for a collection of work items.
     *
     * @param workItems the list of work items
     * @return the total cognitive load
     */
    int calculateTotalLoad(List<WorkItem> workItems);
    
    /**
     * Calculates the cognitive load impact of adding a work item to an organizational unit.
     *
     * @param unit the organizational unit
     * @param workItem the work item to potentially add
     * @return the projected cognitive load
     */
    int calculateImpact(OrganizationalUnit unit, WorkItem workItem);
    
    /**
     * Determines the recommended cognitive capacity for an organizational unit
     * based on its properties (type, size, domain expertise, etc.).
     *
     * @param unit the organizational unit
     * @return the recommended cognitive capacity
     */
    int recommendCapacity(OrganizationalUnit unit);
    
    /**
     * Calculates the utilization percentage of an organizational unit (current load / capacity).
     *
     * @param unit the organizational unit
     * @return the utilization percentage (0-100)
     */
    int calculateUtilizationPercentage(OrganizationalUnit unit);
    
    /**
     * Determines if an organizational unit is overloaded based on its
     * cognitive capacity and current load.
     *
     * @param unit the organizational unit
     * @param thresholdPercentage the overload threshold percentage (typically 80-90%)
     * @return true if the unit is overloaded, false otherwise
     */
    boolean isOverloaded(OrganizationalUnit unit, int thresholdPercentage);
    
    /**
     * Calculates the load factor adjustment for a specific CYNEFIN domain.
     * Different domains require different cognitive approaches and therefore
     * have different load implications.
     *
     * @param domain the CYNEFIN domain
     * @return the load factor adjustment (multiplier)
     */
    double getDomainLoadFactor(CynefinDomain domain);
    
    /**
     * Calculates the load factor adjustment for a specific work paradigm.
     * Different paradigms require different cognitive approaches and therefore
     * have different load implications.
     *
     * @param paradigm the work paradigm
     * @return the load factor adjustment (multiplier)
     */
    double getParadigmLoadFactor(WorkParadigm paradigm);
    
    /**
     * Estimates the cognitive capacity per team member based on various factors.
     *
     * @param domainExpertise list of domains the member has expertise in
     * @param paradigmExpertise list of paradigms the member has expertise in
     * @param experienceLevel the experience level (1-5, with 5 being most experienced)
     * @return the estimated cognitive capacity
     */
    int estimateIndividualCapacity(List<CynefinDomain> domainExpertise, 
                                   List<WorkParadigm> paradigmExpertise,
                                   int experienceLevel);
    
    /**
     * Calculates the load distribution across team members based on work item assignments
     * and individual capacities.
     *
     * @param unitId the organizational unit ID
     * @return a mapping of member IDs to their current cognitive load
     */
    MemberLoadDistribution calculateMemberLoadDistribution(UUID unitId);
    
    /**
     * Suggests the optimal assignment of a work item to a specific team member
     * based on cognitive load balance and expertise match.
     *
     * @param unitId the organizational unit ID
     * @param workItemId the work item ID
     * @return the recommended member ID, or empty if no suitable member is found
     */
    String suggestMemberAssignment(UUID unitId, UUID workItemId);
    
    /**
     * Optimizes work item distribution across an organizational unit to balance cognitive load.
     *
     * @param unitId the organizational unit ID
     * @return true if redistributions were made, false if already optimal
     */
    boolean optimizeWorkDistribution(UUID unitId);
    
    /**
     * Records the actual time spent on a work item to improve future load estimates.
     *
     * @param workItemId the work item ID
     * @param estimatedHours the initially estimated hours
     * @param actualHours the actual hours spent
     * @return true if the system's estimations should be adjusted based on this data
     */
    boolean recordActualEffort(UUID workItemId, int estimatedHours, int actualHours);
    
    /**
     * Class representing the distribution of cognitive load among team members.
     */
    class MemberLoadDistribution {
        private final List<MemberLoad> memberLoads;
        
        public MemberLoadDistribution(List<MemberLoad> memberLoads) {
            this.memberLoads = memberLoads;
        }
        
        public List<MemberLoad> getMemberLoads() {
            return memberLoads;
        }
        
        public double getStandardDeviation() {
            if (memberLoads.isEmpty()) {
                return 0.0;
            }
            
            // Calculate mean
            double mean = memberLoads.stream()
                    .mapToDouble(ml -> (double) ml.getCurrentLoad() / ml.getCapacity() * 100)
                    .average()
                    .orElse(0.0);
            
            // Calculate sum of squared differences
            double sumSquaredDiff = memberLoads.stream()
                    .mapToDouble(ml -> {
                        double utilizationPercent = (double) ml.getCurrentLoad() / ml.getCapacity() * 100;
                        double diff = utilizationPercent - mean;
                        return diff * diff;
                    })
                    .sum();
            
            // Calculate standard deviation
            return Math.sqrt(sumSquaredDiff / memberLoads.size());
        }
        
        public boolean isBalanced(double maxDeviationPercent) {
            return getStandardDeviation() <= maxDeviationPercent;
        }
    }
    
    /**
     * Class representing a team member's cognitive load and capacity.
     */
    class MemberLoad {
        private final String memberId;
        private final int capacity;
        private final int currentLoad;
        private final List<UUID> assignedWorkItems;
        
        public MemberLoad(String memberId, int capacity, int currentLoad, List<UUID> assignedWorkItems) {
            this.memberId = memberId;
            this.capacity = capacity;
            this.currentLoad = currentLoad;
            this.assignedWorkItems = assignedWorkItems;
        }
        
        public String getMemberId() {
            return memberId;
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        public int getCurrentLoad() {
            return currentLoad;
        }
        
        public List<UUID> getAssignedWorkItems() {
            return assignedWorkItems;
        }
        
        public double getUtilizationPercentage() {
            return (double) currentLoad / capacity * 100;
        }
    }
}