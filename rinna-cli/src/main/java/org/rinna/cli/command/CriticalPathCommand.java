/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.service.MockCriticalPathService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.OutputFormatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command to analyze and display the critical path for a project.
 */
public class CriticalPathCommand implements Callable<Integer> {
    
    private boolean showBlockers = false;
    private String itemId;
    private boolean jsonOutput = false;
    private boolean verbose = false;
    
    private final ServiceManager serviceManager = ServiceManager.getInstance();
    private final MockCriticalPathService criticalPathService;
    
    /**
     * Constructs a new CriticalPathCommand with dependencies.
     */
    public CriticalPathCommand() {
        // Initialize services using ServiceManager
        criticalPathService = serviceManager.getMockCriticalPathService();
        setupDummyData();
    }
    
    /**
     * Sets up dummy data for demonstration. In a real implementation,
     * this data would come from a repository or service.
     */
    private void setupDummyData() {
        // Set up a sample critical path
        criticalPathService.setCriticalPath(List.of("WI-123", "WI-456", "WI-789", "WI-101", "WI-102"));
        
        // Add dependencies
        criticalPathService.addDependency("WI-456", "WI-123");
        criticalPathService.addDependency("WI-789", "WI-456");
        criticalPathService.addDependency("WI-101", "WI-789");
        criticalPathService.addDependency("WI-102", "WI-101");
        
        // Set estimated efforts
        criticalPathService.setEstimatedEffort("WI-123", 8);  // 1 day
        criticalPathService.setEstimatedEffort("WI-456", 16); // 2 days
        criticalPathService.setEstimatedEffort("WI-789", 24); // 3 days
        criticalPathService.setEstimatedEffort("WI-101", 16); // 2 days
        criticalPathService.setEstimatedEffort("WI-102", 8);  // 1 day
    }
    
    @Override
    public Integer call() {
        try {
            serviceManager.getMetadataService().trackOperation("critical-path", 
                    Map.of("showBlockers", showBlockers, "itemId", itemId != null ? itemId : ""));
            
            if (jsonOutput) {
                return outputJson();
            } else {
                return outputText();
            }
        } catch (Exception e) {
            if (verbose) {
                System.err.println("Error executing critical path command: " + e.getMessage());
                e.printStackTrace();
            } else {
                System.err.println("Error: " + e.getMessage());
            }
            return 1;
        }
    }
    
    /**
     * Outputs critical path information in JSON format.
     *
     * @return 0 for success, non-zero for failure
     */
    private Integer outputJson() {
        OutputFormatter formatter = new OutputFormatter(true);
        
        if (itemId != null && !itemId.isEmpty()) {
            // Show dependencies for a specific item
            Map<String, Object> itemPath = criticalPathService.getItemCriticalPath(itemId);
            formatter.outputObject("itemCriticalPath", itemPath);
        } else if (showBlockers) {
            // Show only blocking items
            List<Map<String, Object>> blockers = criticalPathService.getBlockers();
            formatter.outputObject("blockers", blockers);
        } else {
            // Show full critical path
            List<Map<String, Object>> criticalPath = criticalPathService.getCriticalPathWithEstimates();
            Map<String, Object> details = criticalPathService.getCriticalPathDetails();
            
            // Combine into a single response
            Map<String, Object> result = Map.of(
                "criticalPath", criticalPath,
                "details", details
            );
            
            formatter.outputObject("criticalPath", result);
        }
        
        return 0;
    }
    
    /**
     * Outputs critical path information in text format.
     *
     * @return 0 for success, non-zero for failure
     */
    private Integer outputText() {
        if (itemId != null && !itemId.isEmpty()) {
            // Show dependencies for a specific item
            Map<String, Object> itemPath = criticalPathService.getItemCriticalPath(itemId);
            
            System.out.printf("Dependencies for work item: %s\n", itemId);
            System.out.println("--------------------------------------------------------------------------------");
            
            if (!(boolean)itemPath.get("onCriticalPath")) {
                System.out.println("This item is not on the critical path.");
                return 0;
            }
            
            @SuppressWarnings("unchecked")
            List<String> directDeps = (List<String>)itemPath.get("directDependencies");
            for (String depId : directDeps) {
                System.out.printf("Direct dependency: %s\n", depId);
                
                // Get work item details for each dependency if available
                WorkItem item = serviceManager.getMockItemService().getItem(depId);
                if (item != null) {
                    System.out.printf("  Title: %s\n", item.getTitle());
                    System.out.printf("  Status: %s\n", item.getStatus());
                }
            }
            
            if (verbose) {
                @SuppressWarnings("unchecked")
                List<String> indirectDeps = (List<String>)itemPath.get("indirectDependencies");
                if (!indirectDeps.isEmpty()) {
                    System.out.println("\nIndirect dependencies:");
                    for (String depId : indirectDeps) {
                        System.out.printf("  %s\n", depId);
                    }
                }
            }
            
        } else if (showBlockers) {
            // Show only blocking items
            List<Map<String, Object>> blockers = criticalPathService.getBlockers();
            
            System.out.println("Blocking items on critical path:");
            System.out.println("--------------------------------------------------------------------------------");
            
            if (blockers.isEmpty()) {
                System.out.println("No blocking items identified.");
                return 0;
            }
            
            for (Map<String, Object> blocker : blockers) {
                String blockerId = (String)blocker.get("id");
                System.out.printf("%s - ", blockerId);
                
                // Get work item details if available
                WorkItem item = serviceManager.getMockItemService().getItem(blockerId);
                if (item != null) {
                    System.out.printf("%s (%s)\n", item.getTitle(), item.getStatus());
                } else {
                    System.out.println("[Unknown item]");
                }
                
                @SuppressWarnings("unchecked")
                List<String> directlyBlocks = (List<String>)blocker.get("directlyBlocks");
                if (!directlyBlocks.isEmpty()) {
                    System.out.println("  Directly blocks:");
                    for (String id : directlyBlocks) {
                        System.out.printf("    %s\n", id);
                    }
                }
                
                if (verbose) {
                    @SuppressWarnings("unchecked")
                    List<String> totalImpact = (List<String>)blocker.get("totalImpact");
                    System.out.printf("  Total impact: %d work items\n", totalImpact.size());
                }
            }
            
        } else {
            // Show full critical path
            List<Map<String, Object>> criticalPath = criticalPathService.getCriticalPathWithEstimates();
            Map<String, Object> details = criticalPathService.getCriticalPathDetails();
            
            System.out.println("Project critical path:");
            System.out.println("--------------------------------------------------------------------------------");
            
            int position = 1;
            for (Map<String, Object> item : criticalPath) {
                String id = (String)item.get("id");
                int effort = (int)item.get("estimatedEffort");
                
                System.out.printf("%d. %s - ", position++, id);
                
                // Get work item details if available
                WorkItem workItem = serviceManager.getMockItemService().getItem(id);
                if (workItem != null) {
                    System.out.printf("%s (%s)\n", workItem.getTitle(), workItem.getStatus());
                    if (verbose) {
                        System.out.printf("   Estimated effort: %d hours\n", effort);
                    }
                } else {
                    System.out.println("[Unknown item]");
                }
            }
            
            System.out.println();
            LocalDate completionDate = (LocalDate)details.get("estimatedCompletionDate");
            System.out.printf("Estimated completion date: %s\n", 
                    completionDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            if (verbose) {
                int totalEffort = (int)details.get("totalEffort");
                System.out.printf("Total estimated effort: %d hours\n", totalEffort);
                
                @SuppressWarnings("unchecked")
                List<String> bottlenecks = (List<String>)details.get("bottlenecks");
                if (!bottlenecks.isEmpty()) {
                    System.out.println("Identified bottlenecks:");
                    for (String bottleneck : bottlenecks) {
                        System.out.printf("  %s\n", bottleneck);
                    }
                }
            }
        }
        
        return 0;
    }
    
    public boolean isShowBlockers() {
        return showBlockers;
    }
    
    public void setShowBlockers(boolean showBlockers) {
        this.showBlockers = showBlockers;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public boolean isJsonOutput() {
        return jsonOutput;
    }
    
    public void setJsonOutput(boolean jsonOutput) {
        this.jsonOutput = jsonOutput;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}