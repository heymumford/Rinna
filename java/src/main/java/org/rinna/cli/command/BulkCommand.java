package org.rinna.cli.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command for bulk updating work items.
 */
public class BulkCommand {
    private static final Logger logger = LoggerFactory.getLogger(BulkCommand.class);
    
    private final ItemService itemService;
    
    public BulkCommand(ItemService itemService) {
        this.itemService = itemService;
    }
    
    /**
     * Execute the bulk command.
     *
     * @param args Command arguments
     * @return Exit code (0 for success, non-zero for failure)
     */
    public int execute(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: At least one filter or update parameter is required");
            printUsage();
            return 1;
        }
        
        try {
            // Parse command arguments
            Map<String, String> params = parseArgs(args);
            
            // Extract filters and update operations
            Map<String, String> filters = extractFilters(params);
            Map<String, String> updates = extractUpdates(params);
            
            // Validate updates
            if (updates.isEmpty()) {
                System.err.println("Error: No update operations specified");
                printUsage();
                return 1;
            }
            
            // Get all work items
            List<WorkItem> allItems = itemService.getAllWorkItems();
            
            // Apply filters
            List<WorkItem> filteredItems = applyFilters(allItems, filters);
            
            if (filteredItems.isEmpty()) {
                System.out.println("Warning: No tasks found matching the filter criteria");
                return 0;
            }
            
            // Apply updates
            int updateCount = applyUpdates(filteredItems, updates);
            
            if (updateCount == 0) {
                System.out.println("Warning: No tasks were updated");
                return 0;
            }
            
            System.out.println("Successfully updated " + filteredItems.size() + " tasks.");
            return 0;
            
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            logger.error("Unexpected error during bulk update", e);
            return 1;
        }
    }
    
    /**
     * Parse command arguments into a map.
     *
     * @param args Command arguments
     * @return Map of parameter name to value
     */
    private Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    params.put(parts[0], parts[1]);
                } else {
                    params.put(parts[0], "true");
                }
            }
        }
        
        return params;
    }
    
    /**
     * Extract filter parameters from the parsed arguments.
     *
     * @param params Parsed parameters
     * @return Map of filter name to value
     */
    private Map<String, String> extractFilters(Map<String, String> params) {
        Map<String, String> filters = new HashMap<>();
        
        // Known filter parameters
        String[] filterParams = {"source", "status", "priority", "type", "assignee"};
        
        for (String param : filterParams) {
            if (params.containsKey(param)) {
                filters.put(param, params.get(param));
            }
        }
        
        return filters;
    }
    
    /**
     * Extract update operations from the parsed arguments.
     *
     * @param params Parsed parameters
     * @return Map of update operation to value
     */
    private Map<String, String> extractUpdates(Map<String, String> params) {
        Map<String, String> updates = new HashMap<>();
        
        // Known update parameters
        String[] updateParams = {"set-status", "set-priority", "set-assignee", "set-type"};
        
        for (String param : updateParams) {
            if (params.containsKey(param)) {
                updates.put(param, params.get(param));
            }
        }
        
        return updates;
    }
    
    /**
     * Apply filters to work items.
     *
     * @param items All work items
     * @param filters Filter parameters
     * @return Filtered list of work items
     */
    private List<WorkItem> applyFilters(List<WorkItem> items, Map<String, String> filters) {
        // Start with all items
        List<WorkItem> filteredItems = items;
        
        // Apply each filter
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String filterName = filter.getKey();
            String filterValue = filter.getValue();
            
            Predicate<WorkItem> predicate = getFilterPredicate(filterName, filterValue);
            filteredItems = filteredItems.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        }
        
        return filteredItems;
    }
    
    /**
     * Get a predicate for filtering work items based on a filter name and value.
     *
     * @param filterName Name of the filter
     * @param filterValue Value of the filter
     * @return Predicate for filtering work items
     */
    private Predicate<WorkItem> getFilterPredicate(String filterName, String filterValue) {
        switch (filterName) {
            case "source":
                return item -> filterValue.equals(item.getSource());
            case "status":
                WorkflowState status = WorkflowState.valueOf(filterValue);
                return item -> status.equals(item.getStatus());
            case "priority":
                Priority priority = Priority.valueOf(filterValue);
                return item -> priority.equals(item.getPriority());
            case "type":
                return item -> filterValue.equals(item.getType().toString());
            case "assignee":
                return item -> filterValue.equals(item.getAssignee());
            default:
                throw new IllegalArgumentException("Unknown filter: " + filterName);
        }
    }
    
    /**
     * Apply updates to work items.
     *
     * @param items Work items to update
     * @param updates Update operations
     * @return Number of fields updated
     */
    private int applyUpdates(List<WorkItem> items, Map<String, String> updates) {
        int updateCount = 0;
        
        for (Map.Entry<String, String> update : updates.entrySet()) {
            String updateName = update.getKey();
            String updateValue = update.getValue();
            
            for (WorkItem item : items) {
                updateCount += applyUpdate(item, updateName, updateValue);
            }
        }
        
        // Save the updates
        for (WorkItem item : items) {
            itemService.updateWorkItem(item);
        }
        
        return updateCount;
    }
    
    /**
     * Apply an update to a work item.
     *
     * @param item Work item to update
     * @param updateName Name of the update operation
     * @param updateValue Value to set
     * @return 1 if the item was updated, 0 otherwise
     */
    private int applyUpdate(WorkItem item, String updateName, String updateValue) {
        switch (updateName) {
            case "set-status":
                WorkflowState status = WorkflowState.valueOf(updateValue);
                item.setStatus(status);
                return 1;
            case "set-priority":
                Priority priority = Priority.valueOf(updateValue);
                item.setPriority(priority);
                return 1;
            case "set-assignee":
                item.setAssignee(updateValue);
                return 1;
            case "set-type":
                // Assuming WorkItemType is an enum
                item.setType(WorkItemType.valueOf(updateValue));
                return 1;
            default:
                throw new IllegalArgumentException("Unknown update operation: " + updateName);
        }
    }
    
    /**
     * Print usage information.
     */
    private void printUsage() {
        System.out.println("Usage: rin bulk [filters] [updates]");
        System.out.println("  Performs bulk updates on work items matching the specified filters.");
        System.out.println();
        System.out.println("Filters:");
        System.out.println("  --source=<source>           Filter by source (e.g., 'imported')");
        System.out.println("  --status=<status>           Filter by status (e.g., 'TO_DO', 'IN_PROGRESS')");
        System.out.println("  --priority=<priority>       Filter by priority (e.g., 'HIGH', 'MEDIUM', 'LOW')");
        System.out.println("  --type=<type>               Filter by type (e.g., 'FEATURE', 'BUG')");
        System.out.println("  --assignee=<assignee>       Filter by assignee");
        System.out.println();
        System.out.println("Updates:");
        System.out.println("  --set-status=<status>       Set status for matching items");
        System.out.println("  --set-priority=<priority>   Set priority for matching items");
        System.out.println("  --set-assignee=<assignee>   Set assignee for matching items");
        System.out.println("  --set-type=<type>           Set type for matching items");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  rin bulk --source=imported --set-status=IN_PROGRESS");
        System.out.println("  rin bulk --status=TO_DO --set-priority=HIGH");
        System.out.println("  rin bulk --source=imported --status=TO_DO --set-assignee=developer1");
    }
}