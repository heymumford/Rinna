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
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockCriticalPathService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command to analyze and display the critical path for a project.
 * This command identifies dependencies and bottlenecks in the workflow.
 * 
 * Usage examples:
 * - rin path
 * - rin path --blockers
 * - rin path --item=WI-123
 * - rin path --format=json
 * - rin path --verbose
 */
public class CriticalPathCommand implements Callable<Integer> {
    
    private boolean showBlockers = false;
    private String itemId;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MockCriticalPathService criticalPathService;
    private final MetadataService metadataService;
    
    /**
     * Creates a new CriticalPathCommand with default services.
     */
    public CriticalPathCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new CriticalPathCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public CriticalPathCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.criticalPathService = serviceManager.getMockCriticalPathService();
        this.metadataService = serviceManager.getMetadataService();
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
        // Generate a unique operation ID for tracking this command execution
        String operationId = generateOperationId();
        
        try {
            // Validate inputs
            validateInputs(operationId);
            
            // Determine output format
            if (isJsonOutput()) {
                return outputJson(operationId);
            } else {
                return outputText(operationId);
            }
        } catch (Exception e) {
            return handleError(operationId, e, "Error executing critical path command: " + e.getMessage());
        }
    }
    
    /**
     * Generates an operation ID for tracking this command execution.
     *
     * @return the operation ID
     */
    private String generateOperationId() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("showBlockers", showBlockers);
        params.put("itemId", itemId != null ? itemId : "");
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        return metadataService.startOperation("critical-path", "READ", params);
    }
    
    /**
     * Validates command inputs and throws exceptions for invalid values.
     *
     * @param operationId the operation ID for tracking
     * @throws IllegalArgumentException if inputs are invalid
     */
    private void validateInputs(String operationId) throws IllegalArgumentException {
        // For now, we don't have specific validation needs for critical path command
        // This method is included for consistency with the ViewCommand pattern
        // Will be extended if validation requirements arise in the future
    }
    
    /**
     * Handles error reporting consistently.
     *
     * @param operationId the operation ID for tracking
     * @param e the exception that occurred
     * @param userMessage the message to display to the user
     * @return 1 to indicate failure
     */
    private int handleError(String operationId, Exception e, String userMessage) {
        if (isJsonOutput()) {
            System.out.println(
                OutputFormatter.formatJsonMessage(
                    "error",
                    e.getMessage(),
                    null
                )
            );
        } else {
            System.err.println("Error: " + userMessage);
            if (verbose) {
                e.printStackTrace();
            }
        }
        
        // Record the failed operation with error details
        metadataService.failOperation(operationId, e);
        
        return 1;
    }
    
    /**
     * Outputs critical path information in JSON format.
     *
     * @param operationId the operation ID for tracking
     * @return 0 for success, non-zero for failure
     */
    private Integer outputJson(String operationId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("result", "success");
            
            if (itemId != null && !itemId.isEmpty()) {
                // Show dependencies for a specific item
                Map<String, Object> itemPath = criticalPathService.getItemCriticalPath(itemId);
                result.put("itemCriticalPath", itemPath);
            } else if (showBlockers) {
                // Show only blocking items
                List<Map<String, Object>> blockers = criticalPathService.getBlockers();
                result.put("blockers", blockers);
                result.put("blockerCount", blockers.size());
                
                if (blockers.isEmpty()) {
                    result.put("message", "No blocking items identified");
                } else {
                    result.put("message", blockers.size() + " blocking items identified");
                }
            } else {
                // Show full critical path
                List<Map<String, Object>> criticalPath = criticalPathService.getCriticalPathWithEstimates();
                Map<String, Object> details = criticalPathService.getCriticalPathDetails();
                
                // Combine into a single response
                result.put("criticalPath", criticalPath);
                result.put("details", details);
                result.put("pathLength", criticalPath.size());
                result.put("message", "Critical path with " + criticalPath.size() + " items");
            }
            
            // Use the OutputFormatter for consistent JSON output
            System.out.println(OutputFormatter.toJson(result, verbose));
            
            // Record the successful operation
            Map<String, Object> operationResult = new HashMap<>();
            operationResult.put("outputFormat", "json");
            if (itemId != null) {
                operationResult.put("itemId", itemId);
            } else if (showBlockers) {
                operationResult.put("showBlockers", true);
                operationResult.put("blockerCount", criticalPathService.getBlockers().size());
            } else {
                Map<String, Object> details = criticalPathService.getCriticalPathDetails();
                operationResult.put("pathLength", criticalPathService.getCriticalPathWithEstimates().size());
                operationResult.put("estimatedCompletion", details.get("estimatedCompletionDate"));
            }
            
            metadataService.completeOperation(operationId, operationResult);
            return 0;
        } catch (Exception e) {
            return handleError(operationId, e, "Error generating JSON output: " + e.getMessage());
        }
    }
    
    /**
     * Outputs critical path information in text format.
     *
     * @param operationId the operation ID for tracking
     * @return 0 for success, non-zero for failure
     */
    private Integer outputText(String operationId) {
        try {
            // Operation result data to be recorded
            Map<String, Object> operationResult = new HashMap<>();
            operationResult.put("outputFormat", "text");
            
            if (itemId != null && !itemId.isEmpty()) {
                // Show dependencies for a specific item
                Map<String, Object> itemPath = criticalPathService.getItemCriticalPath(itemId);
                operationResult.put("itemId", itemId);
                operationResult.put("onCriticalPath", itemPath.get("onCriticalPath"));
                
                System.out.printf("Dependencies for work item: %s\n", itemId);
                System.out.println("--------------------------------------------------------------------------------");
                
                if (!(boolean)itemPath.get("onCriticalPath")) {
                    System.out.println("This item is not on the critical path.");
                    metadataService.completeOperation(operationId, operationResult);
                    return 0;
                }
                
                @SuppressWarnings("unchecked")
                List<String> directDeps = (List<String>)itemPath.get("directDependencies");
                operationResult.put("dependencies", directDeps.size());
                
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
                    operationResult.put("indirectDependencies", indirectDeps.size());
                    
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
                operationResult.put("showBlockers", true);
                operationResult.put("blockerCount", blockers.size());
                
                System.out.println("Blocking items on critical path:");
                System.out.println("--------------------------------------------------------------------------------");
                
                if (blockers.isEmpty()) {
                    System.out.println("No blocking items identified.");
                    metadataService.completeOperation(operationId, operationResult);
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
                
                operationResult.put("pathLength", criticalPath.size());
                operationResult.put("estimatedCompletion", details.get("estimatedCompletionDate"));
                
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
                    operationResult.put("totalEffort", totalEffort);
                    
                    @SuppressWarnings("unchecked")
                    List<String> bottlenecks = (List<String>)details.get("bottlenecks");
                    operationResult.put("bottlenecks", bottlenecks.size());
                    
                    if (!bottlenecks.isEmpty()) {
                        System.out.println("Identified bottlenecks:");
                        for (String bottleneck : bottlenecks) {
                            System.out.printf("  %s\n", bottleneck);
                        }
                    }
                }
            }
            
            // Record the successful operation
            metadataService.completeOperation(operationId, operationResult);
            return 0;
        } catch (Exception e) {
            return handleError(operationId, e, "Error displaying critical path information: " + e.getMessage());
        }
    }
    
    /**
     * Gets whether to show only blocking items.
     *
     * @return true if only blocking items should be shown
     */
    public boolean isShowBlockers() {
        return showBlockers;
    }
    
    /**
     * Sets whether to show only blocking items.
     *
     * @param showBlockers true to show only blocking items
     */
    public void setShowBlockers(boolean showBlockers) {
        this.showBlockers = showBlockers;
    }
    
    /**
     * Gets the specific work item ID to analyze.
     *
     * @return the work item ID
     */
    public String getItemId() {
        return itemId;
    }
    
    /**
     * Sets the specific work item ID to analyze.
     *
     * @param itemId the work item ID
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    /**
     * Gets the output format.
     *
     * @return the output format
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * Sets the output format (text or json).
     *
     * @param format the output format
     */
    public void setFormat(String format) {
        if (format != null && !format.isEmpty()) {
            this.format = format.toLowerCase();
        }
    }
    
    /**
     * Gets whether JSON output is enabled.
     *
     * @return true if JSON output is enabled
     */
    public boolean isJsonOutput() {
        return "json".equalsIgnoreCase(format);
    }
    
    /**
     * Gets whether verbose output is enabled.
     *
     * @return true if verbose output is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }
    
    /**
     * Sets whether verbose output is enabled.
     *
     * @param verbose true to enable verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}