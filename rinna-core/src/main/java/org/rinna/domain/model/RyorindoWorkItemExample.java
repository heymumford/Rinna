/*
 * Example program demonstrating Ryorin-do v0.2 enhanced work items
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A simple example program that demonstrates the use of Ryorin-do v0.2 enhanced work items.
 */
public class RyorindoWorkItemExample {
    
    /**
     * Represents a simplified work item for this example.
     */
    private static class SimpleWorkItem {
        private final UUID id;
        private final String title;
        private final String description;
        private final String type;
        private final String status;
        private final String priority;
        private final String assignee;
        
        public SimpleWorkItem(UUID id, String title, String description, 
                             String type, String status, String priority, String assignee) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.type = type;
            this.status = status;
            this.priority = priority;
            this.assignee = assignee;
        }
        
        public UUID getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getPriority() { return priority; }
        public String getAssignee() { return assignee; }
    }
    
    /**
     * Enhancer interface that adds Ryorin-do v0.2 functionality to a work item.
     */
    private interface WorkItemEnhancer {
        SimpleWorkItem getWorkItem();
    }
    
    /**
     * Implementation of the WorkItemEnhancer interface.
     */
    private static class EnhancedWorkItem implements WorkItemEnhancer {
        private final SimpleWorkItem workItem;
        private CynefinDomain cynefinDomain;
        private WorkParadigm workParadigm;
        private String outcome;
        private Integer cognitiveLoad;
        
        public EnhancedWorkItem(SimpleWorkItem workItem) {
            this.workItem = workItem;
        }
        
        @Override
        public SimpleWorkItem getWorkItem() {
            return workItem;
        }
        
        public void setCynefinDomain(CynefinDomain cynefinDomain) {
            this.cynefinDomain = cynefinDomain;
        }
        
        public CynefinDomain getCynefinDomain() {
            return cynefinDomain;
        }
        
        public void setWorkParadigm(WorkParadigm workParadigm) {
            this.workParadigm = workParadigm;
        }
        
        public WorkParadigm getWorkParadigm() {
            return workParadigm;
        }
        
        public void setOutcome(String outcome) {
            this.outcome = outcome;
        }
        
        public String getOutcome() {
            return outcome;
        }
        
        public void setCognitiveLoad(Integer cognitiveLoad) {
            if (cognitiveLoad < 1 || cognitiveLoad > 10) {
                throw new IllegalArgumentException("Cognitive load must be between 1 and 10");
            }
            this.cognitiveLoad = cognitiveLoad;
        }
        
        public Integer getCognitiveLoad() {
            return cognitiveLoad;
        }
        
        public String getRecommendedApproach() {
            return cynefinDomain != null ? cynefinDomain.getRecommendedApproach() : null;
        }
        
        public boolean requiresExperimentation() {
            return cynefinDomain != null && cynefinDomain.requiresExperimentation();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("Ryorin-do v0.2 Enhanced Work Item Example");
        System.out.println("=========================================");
        
        // Create a simple work item
        SimpleWorkItem task = new SimpleWorkItem(
            UUID.randomUUID(),
            "Implement user authentication",
            "Implement OAuth-based authentication for the web application",
            "TASK",
            "BACKLOG",
            "HIGH",
            "developer@example.com"
        );
        
        // Enhance the work item with Ryorin-do v0.2 concepts
        EnhancedWorkItem enhancedTask = new EnhancedWorkItem(task);
        enhancedTask.setCynefinDomain(CynefinDomain.COMPLICATED);
        enhancedTask.setWorkParadigm(WorkParadigm.STORY);
        enhancedTask.setOutcome("Users can securely log in with their social media accounts");
        enhancedTask.setCognitiveLoad(6);
        
        // Display the enhanced work item
        System.out.println("\nBasic Information:");
        System.out.println("------------------");
        System.out.println("Title: " + enhancedTask.getWorkItem().getTitle());
        System.out.println("Description: " + enhancedTask.getWorkItem().getDescription());
        System.out.println("Type: " + enhancedTask.getWorkItem().getType());
        System.out.println("Status: " + enhancedTask.getWorkItem().getStatus());
        System.out.println("Priority: " + enhancedTask.getWorkItem().getPriority());
        System.out.println("Assignee: " + enhancedTask.getWorkItem().getAssignee());
        
        System.out.println("\nRyorin-do v0.2 Enhancements:");
        System.out.println("---------------------------");
        System.out.println("CYNEFIN Domain: " + enhancedTask.getCynefinDomain().getName());
        System.out.println("Work Paradigm: " + enhancedTask.getWorkParadigm().getName());
        System.out.println("Desired Outcome: " + enhancedTask.getOutcome());
        System.out.println("Cognitive Load: " + enhancedTask.getCognitiveLoad() + "/10");
        
        System.out.println("\nDerived Insights:");
        System.out.println("----------------");
        System.out.println("Recommended Approach: " + enhancedTask.getRecommendedApproach());
        System.out.println("Requires Experimentation: " + enhancedTask.requiresExperimentation());
        System.out.println("Typical Time Horizon: " + enhancedTask.getWorkParadigm().getTypicalTimeHorizon());
        System.out.println("Suggested Cognitive Load Range: " + enhancedTask.getWorkParadigm().getSuggestedCognitiveLoadRange());
        
        // Analyze if the work item is in the appropriate CYNEFIN domain for its paradigm
        boolean isAppropriate = enhancedTask.getWorkParadigm().getRecommendedDomain() == enhancedTask.getCynefinDomain();
        System.out.println("\nParadigm-Domain Alignment: " + (isAppropriate ? "Appropriate" : "Misaligned"));
        
        if (!isAppropriate) {
            System.out.println("Recommended Domain for " + enhancedTask.getWorkParadigm().getName() + 
                              ": " + enhancedTask.getWorkParadigm().getRecommendedDomain().getName());
        }
    }
}