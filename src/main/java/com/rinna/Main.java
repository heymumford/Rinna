package com.rinna;

import com.rinna.model.WorkItem;
import com.rinna.model.WorkItemCreateRequest;
import com.rinna.model.WorkItemType;
import com.rinna.model.WorkflowState;
import com.rinna.service.InvalidTransitionException;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Main class for the Rinna system.
 */
public class Main {
    
    /**
     * Main entry point for the Rinna CLI.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Starting Rinna...");
        Rinna rinna = Rinna.initialize();
        System.out.println("Rinna initialized!");
        
        displayHelp();
        
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.print("\nrinna> ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+");
            String command = parts.length > 0 ? parts[0].toLowerCase() : "";
            
            try {
                switch (command) {
                    case "help":
                        displayHelp();
                        break;
                    case "create":
                        handleCreate(rinna, parts);
                        break;
                    case "list":
                        handleList(rinna);
                        break;
                    case "show":
                        handleShow(rinna, parts);
                        break;
                    case "transition":
                        handleTransition(rinna, parts);
                        break;
                    case "exit":
                    case "quit":
                        running = false;
                        break;
                    default:
                        System.out.println("Unknown command. Type 'help' for available commands.");
                        break;
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        
        System.out.println("Goodbye!");
    }
    
    private static void displayHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  help                      - Display available commands");
        System.out.println("  create <type> <title>     - Create a new work item");
        System.out.println("                              <type> can be: bug, feature, goal, chore");
        System.out.println("  list                      - List all work items");
        System.out.println("  show <id>                 - Show details of a work item");
        System.out.println("  transition <id> <state>   - Change the state of a work item");
        System.out.println("                              <state> can be: found, triaged, todo, inprogress, intest, done");
        System.out.println("  exit / quit               - Exit the application");
    }
    
    private static void handleCreate(Rinna rinna, String[] parts) {
        if (parts.length < 3) {
            System.out.println("Usage: create <type> <title>");
            return;
        }
        
        String typeStr = parts[1].toUpperCase();
        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            titleBuilder.append(parts[i]).append(" ");
        }
        String title = titleBuilder.toString().trim();
        
        WorkItemType type;
        try {
            type = WorkItemType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid type. Valid types are: bug, feature, goal, chore");
            return;
        }
        
        WorkItemCreateRequest request = WorkItemCreateRequest.builder()
                .title(title)
                .type(type)
                .build();
        
        WorkItem item = rinna.items().create(request);
        System.out.println("Created item: " + item.getId() + " - " + item.getTitle());
    }
    
    private static void handleList(Rinna rinna) {
        List<WorkItem> items = rinna.items().findAll();
        if (items.isEmpty()) {
            System.out.println("No items found.");
            return;
        }
        
        System.out.println("Work items:");
        for (WorkItem item : items) {
            System.out.printf("  %s - [%s] [%s] %s%n", 
                    item.getId(), item.getStatus(), item.getType(), item.getTitle());
        }
    }
    
    private static void handleShow(Rinna rinna, String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: show <id>");
            return;
        }
        
        try {
            UUID id = UUID.fromString(parts[1]);
            rinna.items().findById(id)
                    .ifPresentOrElse(
                            item -> {
                                System.out.println("ID: " + item.getId());
                                System.out.println("Title: " + item.getTitle());
                                System.out.println("Type: " + item.getType());
                                System.out.println("Status: " + item.getStatus());
                                System.out.println("Priority: " + item.getPriority());
                                System.out.println("Description: " + item.getDescription());
                                System.out.println("Assignee: " + 
                                        (item.getAssignee() != null ? item.getAssignee() : "Unassigned"));
                                System.out.println("Created: " + item.getCreatedAt());
                                System.out.println("Updated: " + item.getUpdatedAt());
                            },
                            () -> System.out.println("Item not found: " + id)
                    );
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid ID format. Expected UUID.");
        }
    }
    
    private static void handleTransition(Rinna rinna, String[] parts) {
        if (parts.length < 3) {
            System.out.println("Usage: transition <id> <state>");
            return;
        }
        
        try {
            UUID id = UUID.fromString(parts[1]);
            String stateStr = parts[2].toUpperCase();
            
            // Convert from CLI format to enum format
            if (stateStr.equals("TODO")) {
                stateStr = "TO_DO";
            } else if (stateStr.equals("INPROGRESS")) {
                stateStr = "IN_PROGRESS";
            } else if (stateStr.equals("INTEST")) {
                stateStr = "IN_TEST";
            }
            
            WorkflowState state;
            try {
                state = WorkflowState.valueOf(stateStr);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid state. Valid states are: found, triaged, todo, inprogress, intest, done");
                return;
            }
            
            try {
                WorkItem item = rinna.workflow().transition(id, state);
                System.out.println("Transitioned item " + id + " to " + state);
            } catch (InvalidTransitionException e) {
                System.out.println("Invalid transition: " + e.getMessage());
                
                List<WorkflowState> available = rinna.workflow().getAvailableTransitions(id);
                if (available.isEmpty()) {
                    System.out.println("No valid transitions available from current state.");
                } else {
                    System.out.println("Available transitions: " + available);
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid ID format. Expected UUID.");
        }
    }
}