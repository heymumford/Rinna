/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli;

import org.rinna.cli.command.*;
import org.rinna.cli.command.ScheduleCommand;
import org.rinna.cli.messaging.AnsiFormatter;
import org.rinna.cli.messaging.MessageService;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.notifications.NotificationService;
import org.rinna.cli.notifications.NotificationType;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportService;
import org.rinna.cli.report.ReportScheduler;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Main entry point for the Rinna CLI.
 */
public class RinnaCli implements Callable<Integer> {
    
    @Override
    public Integer call() {
        // Default behavior is to show help
        showHelp();
        return 0;
    }
    
    private void showHelp() {
        System.out.println("Rinna CLI - Command line interface for Rinna workflow management system");
        System.out.println("Version: 1.8.1");
        System.out.println();
        System.out.println("Usage: rin-cli [command] [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  view        View details of a work item");
        System.out.println("  list        List work items");
        System.out.println("  add         Add a new work item");
        System.out.println("  update      Update a work item");
        System.out.println("  bug         Create a bug report");
        System.out.println("  path        Show critical path");
        System.out.println("  done        Mark a work item as done");
        System.out.println("  test        Mark a work item as being in test");
        System.out.println("  backlog     Manage your backlog");
        System.out.println("  server      Manage Rinna services");
        System.out.println("  admin       Administrative operations");
        System.out.println("  login       Authenticate with the system");
        System.out.println("  logout      End your current session");
        System.out.println("  access      Manage user permissions and admin access");
        System.out.println("  notify      View and manage notifications");
        System.out.println("  stats       Show project statistics and metrics");
        System.out.println("  import      Import tasks from markdown files");
        System.out.println("  bulk        Perform bulk updates on work items");
        System.out.println("  comment     Add a comment to the current work item in progress");
        System.out.println("  history     View the history of a work item");
        System.out.println("  undo        Undo the last action on a work item (up to 3 steps back)");
        System.out.println("  ls          Linux-style command to list work items (alias for list)");
        System.out.println("  edit        Edit the last viewed work item interactively");
        System.out.println("  grep        Search for text in work items (Linux-style search)");
        System.out.println("  cat         Display work item contents (Linux-style display)");
        System.out.println("  find        Find work items matching criteria (Linux-style find)");
        System.out.println("  msg         Messaging functionality for team communication");
        System.out.println("  report      Generate reports about work items");
        System.out.println("  schedule    Schedule reports to run automatically");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help     Show this help message and exit");
        System.out.println("  -v, --version  Show version information and exit");
    }
    
    /**
     * Handles the view command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleViewCommand(String[] subargs) {
        ViewCommand viewCmd = new ViewCommand();
        if (subargs.length > 0) {
            viewCmd.setId(subargs[0]);
        }
        return viewCmd.call();
    }
    
    /**
     * Handles the list command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleListCommand(String[] subargs) {
        ListCommand listCmd = new ListCommand();
        // Parse list command options
        int i = 0;
        while (i < subargs.length) {
            String arg = subargs[i];
            if (arg.startsWith("--type=")) {
                String type = arg.substring(7);
                listCmd.setType(org.rinna.cli.model.WorkItemType.valueOf(type.toUpperCase()));
                i++;
            } else if ("-t".equals(arg) && i+1 < subargs.length) {
                String type = subargs[i+1];
                listCmd.setType(org.rinna.cli.model.WorkItemType.valueOf(type.toUpperCase()));
                i += 2;
            } else if (arg.startsWith("--priority=")) {
                String priority = arg.substring(11);
                listCmd.setPriority(org.rinna.cli.model.Priority.valueOf(priority.toUpperCase()));
                i++;
            } else if ("-p".equals(arg) && i+1 < subargs.length) {
                String priority = subargs[i+1];
                listCmd.setPriority(org.rinna.cli.model.Priority.valueOf(priority.toUpperCase()));
                i += 2;
            } else if (arg.startsWith("--limit=")) {
                String limit = arg.substring(8);
                listCmd.setLimit(Integer.parseInt(limit));
                i++;
            } else if ("-l".equals(arg) && i+1 < subargs.length) {
                String limit = subargs[i+1];
                listCmd.setLimit(Integer.parseInt(limit));
                i += 2;
            } else {
                i++;
            }
        }
        return listCmd.call();
    }
    
    /**
     * Handles the add command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleAddCommand(String[] subargs) {
        AddCommand addCmd = new AddCommand();
        if (subargs.length > 0) {
            addCmd.setTitle(subargs[0]);
        }
        // Parse add command options
        int i = 1;
        while (i < subargs.length) {
            String arg = subargs[i];
            if (arg.startsWith("--type=")) {
                String type = arg.substring(7);
                addCmd.setType(org.rinna.cli.model.WorkItemType.valueOf(type.toUpperCase()));
                i++;
            } else if ("-t".equals(arg) && i+1 < subargs.length) {
                String type = subargs[i+1];
                addCmd.setType(org.rinna.cli.model.WorkItemType.valueOf(type.toUpperCase()));
                i += 2;
            } else if (arg.startsWith("--priority=")) {
                String priority = arg.substring(11);
                addCmd.setPriority(org.rinna.cli.model.Priority.valueOf(priority.toUpperCase()));
                i++;
            } else if ("-p".equals(arg) && i+1 < subargs.length) {
                String priority = subargs[i+1];
                addCmd.setPriority(org.rinna.cli.model.Priority.valueOf(priority.toUpperCase()));
                i += 2;
            } else {
                i++;
            }
        }
        return addCmd.call();
    }
    
    /**
     * Handles the update command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleUpdateCommand(String[] subargs) {
        UpdateCommand updateCmd = new UpdateCommand();
        if (subargs.length > 0) {
            updateCmd.setId(subargs[0]);
        }
        // Parse update command options
        int i = 1;
        while (i < subargs.length) {
            String arg = subargs[i];
            if (arg.startsWith("--title=") && i+1 < subargs.length) {
                updateCmd.setTitle(subargs[i+1]);
                i += 2;
            } else {
                i++;
            }
        }
        return updateCmd.call();
    }
    
    /**
     * Handles the path command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handlePathCommand(String[] subargs) {
        CriticalPathCommand pathCmd = new CriticalPathCommand();
        // Parse path command options
        int i = 0;
        while (i < subargs.length) {
            String arg = subargs[i];
            if ("--blockers".equals(arg)) {
                pathCmd.setShowBlockers(true);
                i++;
            } else if (arg.startsWith("--item=")) {
                String item = arg.substring(7);
                pathCmd.setItemId(item);
                i++;
            } else if ("--item".equals(arg) && i+1 < subargs.length) {
                String item = subargs[i+1];
                pathCmd.setItemId(item);
                i += 2;
            } else {
                i++;
            }
        }
        return pathCmd.call();
    }
    
    /**
     * Handles the done command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleDoneCommand(String[] subargs) {
        DoneCommand doneCmd = new DoneCommand();
        if (subargs.length > 0) {
            doneCmd.setItemId(subargs[0]);
        }
        return doneCmd.call();
    }
    
    /**
     * Handles the bug command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleBugCommand(String[] subargs) {
        BugCommand bugCmd = new BugCommand();
        if (subargs.length > 0) {
            bugCmd.setTitle(subargs[0]);
        }
        return bugCmd.call();
    }
    
    /**
     * Handles the backlog command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleBacklogCommand(String[] subargs) {
        BacklogCommand backlogCmd = new BacklogCommand();
        // Parse backlog command options
        if (subargs.length > 0) {
            backlogCmd.setAction(subargs[0]);
        }
        return backlogCmd.call();
    }
    
    /**
     * Handles the server command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleServerCommand(String[] subargs) {
        ServerCommand serverCmd = new ServerCommand();
        if (subargs.length > 0) {
            serverCmd.setSubcommand(subargs[0]);
        }
        return serverCmd.call();
    }
    
    /**
     * Handles the test command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleTestCommand(String[] subargs) {
        TestCommand testCmd = new TestCommand();
        if (subargs.length > 0) {
            testCmd.setItemId(subargs[0]);
        }
        return testCmd.call();
    }
    
    /**
     * Handles the import command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleImportCommand(String[] subargs) {
        ImportCommand importCmd = new ImportCommand();
        if (subargs.length > 0) {
            importCmd.setFilePath(subargs[0]);
        }
        return importCmd.call();
    }
    
    /**
     * Handles the bulk command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleBulkCommand(String[] subargs) {
        BulkCommand bulkCmd = new BulkCommand();
        
        // Parse bulk command options
        int i = 0;
        while (i < subargs.length) {
            String arg = subargs[i];
            if (arg.startsWith("--")) {
                String paramName = arg.substring(2);
                String value = null;
                
                // Check if it's in the form --param=value
                int equalsPos = paramName.indexOf('=');
                if (equalsPos > 0) {
                    value = paramName.substring(equalsPos + 1);
                    paramName = paramName.substring(0, equalsPos);
                } else if (i + 1 < subargs.length && !subargs[i + 1].startsWith("--")) {
                    // Check if it's in the form --param value
                    value = subargs[i + 1];
                    i++; // Skip the value in the next loop
                }
                
                // Add as filter or update based on parameter name
                if (paramName.startsWith("set-")) {
                    bulkCmd.setUpdate(paramName, value);
                } else {
                    bulkCmd.setFilter(paramName, value);
                }
            }
            i++;
        }
        
        return bulkCmd.call();
    }
    
    /**
     * Handles the comment command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleCommentCommand(String[] subargs) {
        CommentCommand commentCmd = new CommentCommand();
        
        if (subargs.length == 0) {
            System.err.println("Error: Comment text is required");
            System.err.println("Usage: rin comment <text>");
            System.err.println("       rin comment <item-id> <text>");
            return 1;
        }
        
        // Check if the first argument is an item ID
        try {
            UUID.fromString(subargs[0]);
            // If we get here, the first argument is a valid UUID
            commentCmd.setItemId(subargs[0]);
            
            if (subargs.length < 2) {
                System.err.println("Error: Comment text is required");
                return 1;
            }
            
            // The rest of the arguments form the comment text
            StringBuilder commentText = new StringBuilder();
            for (int i = 1; i < subargs.length; i++) {
                if (i > 1) {
                    commentText.append(" ");
                }
                commentText.append(subargs[i]);
            }
            commentCmd.setComment(commentText.toString());
            
        } catch (IllegalArgumentException e) {
            // Not a UUID, so assume the entire subargs array is the comment text
            StringBuilder commentText = new StringBuilder();
            for (int i = 0; i < subargs.length; i++) {
                if (i > 0) {
                    commentText.append(" ");
                }
                commentText.append(subargs[i]);
            }
            commentCmd.setComment(commentText.toString());
        }
        
        return commentCmd.call();
    }
    
    /**
     * Handles the history command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleHistoryCommand(String[] subargs) {
        HistoryCommand historyCmd = new HistoryCommand();
        
        // Parse history command options
        int i = 0;
        while (i < subargs.length) {
            String arg = subargs[i];
            
            if (arg.startsWith("--")) {
                // Handle options
                String option = arg.substring(2);
                
                if (option.startsWith("last")) {
                    // Time range option
                    historyCmd.setTimeRange(option.substring(4));
                } else if ("no-comments".equals(option)) {
                    historyCmd.setShowComments(false);
                } else if ("no-states".equals(option)) {
                    historyCmd.setShowStateChanges(false);
                } else if ("no-assignments".equals(option)) {
                    historyCmd.setShowAssignments(false);
                } else if ("no-fields".equals(option)) {
                    historyCmd.setShowFieldChanges(false);
                } else if (option.startsWith("user=")) {
                    historyCmd.setUser(option.substring(5));
                }
            } else {
                // First non-option argument is assumed to be the item ID
                try {
                    UUID.fromString(arg);
                    historyCmd.setItemId(arg);
                } catch (IllegalArgumentException e) {
                    System.err.println("Error: Invalid work item ID: " + arg);
                    return 1;
                }
            }
            
            i++;
        }
        
        return historyCmd.call();
    }
    
    /**
     * Handles the undo command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleUndoCommand(String[] subargs) {
        UndoCommand undoCmd = new UndoCommand();
        
        // Parse undo command options
        int i = 0;
        while (i < subargs.length) {
            String arg = subargs[i];
            
            if ("--force".equals(arg) || "-f".equals(arg)) {
                undoCmd.setForce(true);
                i++;
            } else if (arg.startsWith("--item=")) {
                String itemId = arg.substring(7);
                undoCmd.setItemId(itemId);
                i++;
            } else if ("--item".equals(arg) && i + 1 < subargs.length) {
                String itemId = subargs[i + 1];
                undoCmd.setItemId(itemId);
                i += 2;
            } else if (arg.startsWith("--step=")) {
                try {
                    int step = Integer.parseInt(arg.substring(7));
                    undoCmd.setStep(step - 1); // Convert from 1-based (user) to 0-based (internal)
                    i++;
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid step number: " + arg.substring(7));
                    return 1;
                }
            } else if ("--step".equals(arg) && i + 1 < subargs.length) {
                try {
                    int step = Integer.parseInt(subargs[i + 1]);
                    undoCmd.setStep(step - 1); // Convert from 1-based (user) to 0-based (internal)
                    i += 2;
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid step number: " + subargs[i + 1]);
                    return 1;
                }
            } else if (arg.startsWith("--steps=")) {
                // Enable interactive steps selection
                undoCmd.setSteps(true);
                i++;
            } else if ("--steps".equals(arg)) {
                // Enable interactive steps selection
                undoCmd.setSteps(true);
                i++;
            } else {
                i++;
            }
        }
        
        return undoCmd.call();
    }
    
    /**
     * Handles the ls command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleLsCommand(String[] subargs) {
        LsCommand lsCmd = new LsCommand();
        
        // Parse ls command options
        int i = 0;
        boolean longFormat = false;
        boolean allFormat = false;
        
        while (i < subargs.length) {
            String arg = subargs[i];
            
            if ("-l".equals(arg)) {
                longFormat = true;
                i++;
            } else if ("-a".equals(arg)) {
                allFormat = true;
                i++;
            } else if ("-al".equals(arg) || "-la".equals(arg)) {
                longFormat = true;
                allFormat = true;
                i++;
            } else if (arg.startsWith("-")) {
                System.err.println("Error: Unknown option: " + arg);
                return 1;
            } else {
                // First non-option argument is the item ID
                lsCmd.setItemId(arg);
                i++;
            }
        }
        
        lsCmd.setLongFormat(longFormat);
        lsCmd.setAllFormat(allFormat);
        
        return lsCmd.call();
    }
    
    /**
     * Handles the edit command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleEditCommand(String[] subargs) {
        EditCommand editCmd = new EditCommand();
        
        // Parse edit command options
        int i = 0;
        while (i < subargs.length) {
            String arg = subargs[i];
            
            if ("--force".equals(arg) || "-f".equals(arg)) {
                editCmd.setForce(true);
                i++;
            } else if (arg.startsWith("id=")) {
                editCmd.setIdParameter(arg);
                i++;
            } else if (!arg.startsWith("-")) {
                // First non-option argument is the item ID
                editCmd.setItemId(arg);
                i++;
            } else {
                System.err.println("Error: Unknown option: " + arg);
                return 1;
            }
        }
        
        return editCmd.call();
    }
    
    /**
     * Handles the grep command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleGrepCommand(String[] subargs) {
        GrepCommand grepCmd = new GrepCommand();
        
        // Parse grep command options
        int i = 0;
        
        // Check if showing history is requested
        if (subargs.length > 0 && ("--history".equals(subargs[0]) || "-H".equals(subargs[0]))) {
            grepCmd.setShowHistory(true);
            return grepCmd.call();
        }
        
        // Process all other options
        while (i < subargs.length) {
            String arg = subargs[i];
            
            if ("-i".equals(arg)) {
                // Case insensitive (default, but explicit flag supported)
                grepCmd.setCaseSensitive(false);
                i++;
            } else if ("-s".equals(arg)) {
                // Case sensitive
                grepCmd.setCaseSensitive(true);
                i++;
            } else if ("-w".equals(arg)) {
                // Exact word match
                grepCmd.setExactMatch(true);
                i++;
            } else if ("-c".equals(arg)) {
                // Count only mode
                grepCmd.setCountOnly(true);
                i++;
            } else if ("-A".equals(arg) && i + 1 < subargs.length) {
                // Context lines after match
                try {
                    int context = Integer.parseInt(subargs[i + 1]);
                    grepCmd.setContext(context);
                    i += 2;
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid context value: " + subargs[i + 1]);
                    return 1;
                }
            } else if ("-B".equals(arg) && i + 1 < subargs.length) {
                // Context lines before match (treated the same as -A for simplicity)
                try {
                    int context = Integer.parseInt(subargs[i + 1]);
                    grepCmd.setContext(context);
                    i += 2;
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid context value: " + subargs[i + 1]);
                    return 1;
                }
            } else if (arg.startsWith("-")) {
                // Unknown option
                System.err.println("Error: Unknown option: " + arg);
                return 1;
            } else {
                // Non-option argument is the search pattern
                grepCmd.setPattern(arg);
                i++;
            }
        }
        
        // Verify that a pattern was provided
        if (grepCmd.getPattern() == null && !grepCmd.isShowHistory()) {
            System.err.println("Error: Empty search pattern");
            return 1;
        }
        
        return grepCmd.call();
    }
    
    /**
     * Handles the cat command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleCatCommand(String[] subargs) {
        CatCommand catCmd = new CatCommand();
        
        // Parse cat command options
        int i = 0;
        while (i < subargs.length) {
            String arg = subargs[i];
            
            if ("-n".equals(arg)) {
                // Show line numbers
                catCmd.setShowLineNumbers(true);
                i++;
            } else if ("-A".equals(arg)) {
                // Show all formatting
                catCmd.setShowAllFormatting(true);
                i++;
            } else if ("-h".equals(arg)) {
                // Show history
                catCmd.setShowHistory(true);
                i++;
            } else if ("-r".equals(arg)) {
                // Show relationships
                catCmd.setShowRelationships(true);
                i++;
            } else if (arg.startsWith("-")) {
                // Unknown option
                System.err.println("Error: Unknown option: " + arg);
                return 1;
            } else {
                // Non-option argument is the work item ID
                catCmd.setItemId(arg);
                i++;
            }
        }
        
        return catCmd.call();
    }
    
    /**
     * Handles the find command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleFindCommand(String[] subargs) {
        FindCommand findCmd = new FindCommand();
        
        // Parse find command options
        int i = 0;
        while (i < subargs.length) {
            String arg = subargs[i];
            
            if ("-name".equals(arg) && i + 1 < subargs.length) {
                findCmd.setNamePattern(subargs[i + 1]);
                i += 2;
            } else if ("-type".equals(arg) && i + 1 < subargs.length) {
                try {
                    org.rinna.cli.model.WorkItemType type = org.rinna.cli.model.WorkItemType.valueOf(subargs[i + 1].toUpperCase());
                    findCmd.setType(type);
                    i += 2;
                } catch (IllegalArgumentException e) {
                    System.err.println("Error: Invalid work item type: " + subargs[i + 1]);
                    System.err.println("Valid types: " + String.join(", ", getEnumValues(org.rinna.cli.model.WorkItemType.class)));
                    return 1;
                }
            } else if ("-state".equals(arg) && i + 1 < subargs.length) {
                try {
                    org.rinna.cli.model.WorkflowState state = org.rinna.cli.model.WorkflowState.valueOf(subargs[i + 1].toUpperCase());
                    findCmd.setState(state);
                    i += 2;
                } catch (IllegalArgumentException e) {
                    System.err.println("Error: Invalid workflow state: " + subargs[i + 1]);
                    System.err.println("Valid states: " + String.join(", ", getEnumValues(org.rinna.cli.model.WorkflowState.class)));
                    return 1;
                }
            } else if ("-priority".equals(arg) && i + 1 < subargs.length) {
                try {
                    org.rinna.cli.model.Priority priority = org.rinna.cli.model.Priority.valueOf(subargs[i + 1].toUpperCase());
                    findCmd.setPriority(priority);
                    i += 2;
                } catch (IllegalArgumentException e) {
                    System.err.println("Error: Invalid priority: " + subargs[i + 1]);
                    System.err.println("Valid priorities: " + String.join(", ", getEnumValues(org.rinna.cli.model.Priority.class)));
                    return 1;
                }
            } else if ("-assignee".equals(arg) && i + 1 < subargs.length) {
                findCmd.setAssignee(subargs[i + 1]);
                i += 2;
            } else if ("-reporter".equals(arg) && i + 1 < subargs.length) {
                findCmd.setReporter(subargs[i + 1]);
                i += 2;
            } else if ("-newer".equals(arg) && i + 1 < subargs.length) {
                java.time.Instant date = FindCommand.parseDate(subargs[i + 1]);
                if (date == null) {
                    System.err.println("Error: Invalid date format: " + subargs[i + 1]);
                    System.err.println("Expected format: yyyy-MM-dd");
                    return 1;
                }
                findCmd.setCreatedAfter(date);
                i += 2;
            } else if ("-older".equals(arg) && i + 1 < subargs.length) {
                java.time.Instant date = FindCommand.parseDate(subargs[i + 1]);
                if (date == null) {
                    System.err.println("Error: Invalid date format: " + subargs[i + 1]);
                    System.err.println("Expected format: yyyy-MM-dd");
                    return 1;
                }
                findCmd.setCreatedBefore(date);
                i += 2;
            } else if ("-mtime".equals(arg) && i + 1 < subargs.length) {
                try {
                    int days = Integer.parseInt(subargs[i + 1]);
                    if (days < 0) {
                        // Negative value means "less than N days ago" (i.e., within the last N days)
                        findCmd.setUpdatedAfter(FindCommand.daysFromNow(days));
                    } else {
                        // Positive value means "more than N days ago"
                        findCmd.setUpdatedBefore(FindCommand.daysFromNow(-days));
                    }
                    i += 2;
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid number of days: " + subargs[i + 1]);
                    return 1;
                }
            } else if ("-details".equals(arg) || "-l".equals(arg)) {
                findCmd.setPrintDetails(true);
                i++;
            } else if ("-count".equals(arg) || "-c".equals(arg)) {
                findCmd.setCountOnly(true);
                i++;
            } else if (arg.startsWith("-")) {
                System.err.println("Error: Unknown option: " + arg);
                return 1;
            } else {
                i++;
            }
        }
        
        return findCmd.call();
    }
    
    /**
     * Gets the values of an enum as strings.
     *
     * @param enumClass the enum class
     * @return the enum values as strings
     */
    private static <T extends Enum<T>> List<String> getEnumValues(Class<T> enumClass) {
        List<String> values = new ArrayList<>();
        for (T constant : enumClass.getEnumConstants()) {
            values.add(constant.name());
        }
        return values;
    }
    
    /**
     * Handles the report command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleReportCommand(String[] subargs) {
        ReportCommand reportCmd = new ReportCommand();
        
        // Set default type to summary if no args
        if (subargs.length > 0) {
            // First argument is the type
            reportCmd.setType(subargs[0]);
            
            // Parse report command options
            for (int i = 1; i < subargs.length; i++) {
                String arg = subargs[i];
                
                if (arg.startsWith("--format=")) {
                    String format = arg.substring(9);
                    reportCmd.setFormat(format);
                } else if (arg.startsWith("--output=")) {
                    String output = arg.substring(9);
                    reportCmd.setOutput(output);
                } else if (arg.startsWith("--title=")) {
                    String title = arg.substring(8);
                    reportCmd.setTitle(title);
                } else if (arg.startsWith("--start=")) {
                    String startDate = arg.substring(8);
                    reportCmd.setStartDate(startDate);
                } else if (arg.startsWith("--end=")) {
                    String endDate = arg.substring(6);
                    reportCmd.setEndDate(endDate);
                } else if (arg.startsWith("--project=")) {
                    String projectId = arg.substring(10);
                    reportCmd.setProjectId(projectId);
                } else if (arg.startsWith("--sort=")) {
                    String sortField = arg.substring(7);
                    reportCmd.setSortField(sortField);
                } else if ("--desc".equals(arg)) {
                    reportCmd.setAscending(false);
                } else if (arg.startsWith("--group=")) {
                    String groupBy = arg.substring(8);
                    reportCmd.setGroupBy(groupBy);
                } else if (arg.startsWith("--limit=")) {
                    try {
                        int limit = Integer.parseInt(arg.substring(8));
                        reportCmd.setLimit(limit);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid limit: " + arg.substring(8));
                        return 1;
                    }
                } else if ("--no-header".equals(arg)) {
                    reportCmd.setNoHeader(true);
                } else if ("--no-timestamp".equals(arg)) {
                    reportCmd.setNoTimestamp(true);
                } else if (arg.startsWith("--filter=")) {
                    String filter = arg.substring(9);
                    int equalsPos = filter.indexOf('=');
                    if (equalsPos > 0) {
                        String field = filter.substring(0, equalsPos);
                        String value = filter.substring(equalsPos + 1);
                        reportCmd.addFilter(field, value);
                    } else {
                        System.err.println("Error: Invalid filter format: " + filter);
                        System.err.println("Expected format: field=value");
                        return 1;
                    }
                } else if (arg.equals("--email")) {
                    reportCmd.setEmailEnabled(true);
                } else if (arg.startsWith("--email-to=")) {
                    String recipients = arg.substring(11);
                    reportCmd.setEmailRecipients(recipients);
                    reportCmd.setEmailEnabled(true);
                } else if (arg.startsWith("--email-subject=")) {
                    String subject = arg.substring(16);
                    reportCmd.setEmailSubject(subject);
                } else if (arg.startsWith("--template=")) {
                    String template = arg.substring(11);
                    reportCmd.setTemplateName(template);
                } else if ("--no-template".equals(arg)) {
                    reportCmd.setNoTemplate(true);
                }
            }
        }
        
        return reportCmd.call();
    }
    
    /**
     * Handles the schedule command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleScheduleCommand(String[] subargs) {
        ScheduleCommand scheduleCmd = new ScheduleCommand();
        
        // If no arguments, default to list action
        if (subargs.length > 0) {
            // First argument is the action
            scheduleCmd.setAction(subargs[0]);
            
            // Parse additional options based on the action
            for (int i = 1; i < subargs.length; i++) {
                String arg = subargs[i];
                
                if (arg.startsWith("--id=")) {
                    String id = arg.substring(5);
                    scheduleCmd.setId(id);
                } else if (arg.startsWith("--name=")) {
                    String name = arg.substring(7);
                    scheduleCmd.setName(name);
                } else if (arg.startsWith("--desc=")) {
                    String description = arg.substring(7);
                    scheduleCmd.setDescription(description);
                } else if (arg.startsWith("--type=")) {
                    String type = arg.substring(7);
                    scheduleCmd.setScheduleType(type);
                } else if (arg.startsWith("--time=")) {
                    String time = arg.substring(7);
                    scheduleCmd.setTime(time);
                } else if (arg.startsWith("--day=")) {
                    String day = arg.substring(6);
                    scheduleCmd.setDayOfWeek(day);
                } else if (arg.startsWith("--date=")) {
                    String date = arg.substring(7);
                    try {
                        int dayOfMonth = Integer.parseInt(date);
                        scheduleCmd.setDayOfMonth(dayOfMonth);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid day of month: " + date);
                        return 1;
                    }
                } else if (arg.startsWith("--report=")) {
                    String reportType = arg.substring(9);
                    scheduleCmd.setReportType(reportType);
                } else if (arg.startsWith("--format=")) {
                    String format = arg.substring(9);
                    scheduleCmd.setReportFormat(format);
                } else if (arg.startsWith("--output=")) {
                    String output = arg.substring(9);
                    scheduleCmd.setOutputPath(output);
                } else if (arg.startsWith("--title=")) {
                    String title = arg.substring(8);
                    scheduleCmd.setTitle(title);
                } else if (arg.equals("--email")) {
                    scheduleCmd.setEmailEnabled(true);
                } else if (arg.startsWith("--email-to=")) {
                    String recipients = arg.substring(11);
                    scheduleCmd.setEmailRecipients(recipients);
                    scheduleCmd.setEmailEnabled(true);
                } else if (arg.startsWith("--email-subject=")) {
                    String subject = arg.substring(16);
                    scheduleCmd.setEmailSubject(subject);
                }
            }
        }
        
        return scheduleCmd.call();
    }
    
    /**
     * Handles the msg command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleMsgCommand(String[] subargs) {
        MsgCommand msgCmd = new MsgCommand();
        
        if (subargs.length > 0) {
            String firstArg = subargs[0];
            if (firstArg.startsWith("-")) {
                // It's an option flag
                msgCmd.setSubcommand(firstArg);
                
                // Parse the rest of the arguments
                if (subargs.length > 1) {
                    String[] restArgs = new String[subargs.length - 1];
                    System.arraycopy(subargs, 1, restArgs, 0, subargs.length - 1);
                    msgCmd.setArgs(restArgs);
                }
            } else {
                // It's a subcommand or recipient
                msgCmd.setSubcommand(firstArg);
                
                // Parse the rest of the arguments
                if (subargs.length > 1) {
                    String[] restArgs = new String[subargs.length - 1];
                    System.arraycopy(subargs, 1, restArgs, 0, subargs.length - 1);
                    msgCmd.setArgs(restArgs);
                }
            }
        }
        
        return msgCmd.call();
    }
    
    /**
     * Handles the notify command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleNotifyCommand(String[] subargs) {
        NotifyCommand notifyCmd = new NotifyCommand();
        
        if (subargs.length > 0) {
            // First argument is the action
            notifyCmd.setAction(subargs[0]);
            
            // Check if there's a notification ID parameter
            if (subargs.length > 1 && !subargs[1].startsWith("--")) {
                try {
                    notifyCmd.setNotificationId(UUID.fromString(subargs[1]));
                } catch (IllegalArgumentException e) {
                    // Not a UUID, must be some other argument
                    System.err.println("Error: Invalid notification ID format: " + subargs[1]);
                    return 1;
                }
            }
            
            // Process additional options
            for (int i = 1; i < subargs.length; i++) {
                String arg = subargs[i];
                
                if (arg.startsWith("--days=")) {
                    try {
                        int days = Integer.parseInt(arg.substring(7));
                        notifyCmd.setDays(days);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid number of days: " + arg.substring(7));
                        return 1;
                    }
                } else if (arg.startsWith("--type=")) {
                    String typeStr = arg.substring(7).toUpperCase();
                    try {
                        NotificationType type = NotificationType.valueOf(typeStr);
                        notifyCmd.setType(type);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error: Invalid notification type: " + typeStr);
                        return 1;
                    }
                }
            }
        }
        
        return notifyCmd.call();
    }
    
    /**
     * Handles the stats command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleStatsCommand(String[] subargs) {
        StatsCommand statsCmd = new StatsCommand();
        
        // Set default type to summary if no args
        if (subargs.length == 0) {
            statsCmd.setType("summary");
        } else {
            // First argument is the type
            statsCmd.setType(subargs[0]);
            
            // Parse additional options
            for (int i = 1; i < subargs.length; i++) {
                String arg = subargs[i];
                
                if (arg.startsWith("--format=")) {
                    String format = arg.substring(9);
                    statsCmd.setFormat(format);
                } else if (arg.startsWith("--limit=")) {
                    try {
                        int limit = Integer.parseInt(arg.substring(8));
                        statsCmd.setLimit(limit);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid limit: " + arg.substring(8));
                        return 1;
                    }
                } else {
                    // Collect remaining args as filter arguments
                    String[] filterArgs = new String[subargs.length - i];
                    System.arraycopy(subargs, i, filterArgs, 0, filterArgs.length);
                    statsCmd.setFilterArgs(filterArgs);
                    break;
                }
            }
        }
        
        return statsCmd.call();
    }
    
    /**
     * Handles the login command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleLoginCommand(String[] subargs) {
        LoginCommand loginCmd = new LoginCommand();
        
        // Parse login command options
        int i = 0;
        while (i < subargs.length) {
            String arg = subargs[i];
            
            if (arg.startsWith("--user=")) {
                String username = arg.substring(7);
                loginCmd.setUsername(username);
                i++;
            } else if ("--user".equals(arg) && i + 1 < subargs.length) {
                String username = subargs[i + 1];
                loginCmd.setUsername(username);
                i += 2;
            } else if (arg.startsWith("--password=")) {
                String password = arg.substring(11);
                loginCmd.setPassword(password);
                i++;
            } else if ("--password".equals(arg) && i + 1 < subargs.length) {
                String password = subargs[i + 1];
                loginCmd.setPassword(password);
                i += 2;
            } else if (!arg.startsWith("-")) {
                // First non-option argument is assumed to be the username
                loginCmd.setUsername(arg);
                i++;
            } else {
                System.err.println("Error: Unknown option: " + arg);
                return 1;
            }
        }
        
        return loginCmd.call();
    }
    
    /**
     * Handles the logout command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleLogoutCommand(String[] subargs) {
        LogoutCommand logoutCmd = new LogoutCommand();
        return logoutCmd.call();
    }
    
    /**
     * Handles the access command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleAccessCommand(String[] subargs) {
        UserAccessCommand accessCmd = new UserAccessCommand();
        
        if (subargs.length > 0) {
            // First argument is the action
            accessCmd.setAction(subargs[0]);
            
            // Process the remaining arguments
            int i = 1;
            while (i < subargs.length) {
                String arg = subargs[i];
                
                if (arg.startsWith("--user=")) {
                    String username = arg.substring(7);
                    accessCmd.setUsername(username);
                    i++;
                } else if ("--user".equals(arg) && i + 1 < subargs.length) {
                    String username = subargs[i + 1];
                    accessCmd.setUsername(username);
                    i += 2;
                } else if (arg.startsWith("--permission=")) {
                    String permission = arg.substring(13);
                    accessCmd.setPermission(permission);
                    i++;
                } else if ("--permission".equals(arg) && i + 1 < subargs.length) {
                    String permission = subargs[i + 1];
                    accessCmd.setPermission(permission);
                    i += 2;
                } else if (arg.startsWith("--area=")) {
                    String area = arg.substring(7);
                    accessCmd.setArea(area);
                    i++;
                } else if ("--area".equals(arg) && i + 1 < subargs.length) {
                    String area = subargs[i + 1];
                    accessCmd.setArea(area);
                    i += 2;
                } else {
                    i++;
                }
            }
        }
        
        return accessCmd.call();
    }
    
    /**
     * Handles the admin command.
     *
     * @param subargs command arguments
     * @return exit code
     */
    private static int handleAdminCommand(String[] subargs) {
        AdminCommand adminCmd = new AdminCommand();
        
        if (subargs.length > 0) {
            adminCmd.setSubcommand(subargs[0]);
            
            if (subargs.length > 1) {
                String[] restArgs = new String[subargs.length - 1];
                System.arraycopy(subargs, 1, restArgs, 0, subargs.length - 1);
                adminCmd.setArgs(restArgs);
            }
        }
        
        return adminCmd.call();
    }
    
    /**
     * Checks for unread messages and notifications and displays them.
     */
    private static void checkUnreadMessages() {
        try {
            // Check for unread notifications
            try {
                org.rinna.cli.security.SecurityManager securityManager = org.rinna.cli.security.SecurityManager.getInstance();
                if (securityManager.isAuthenticated()) {
                    // Display unread notifications
                    NotificationService notificationService = NotificationService.getInstance();
                    notificationService.displayUnreadNotifications();
                }
            } catch (Exception e) {
                // Silently ignore any errors in the notification system
            }
            
            // Check for unread messages (traditional messaging system)
            try {
                // Get config and message services
                ConfigurationService configService = ServiceManager.getInstance().getConfigurationService();
                
                // Only proceed if user is authenticated
                if (configService.isAuthenticated()) {
                    String currentUser = configService.getCurrentUser();
                    String authToken = configService.getAuthToken();
                    
                    if (currentUser != null && authToken != null) {
                        MessageService messageService = ServiceManager.getInstance().getMessageService();
                        
                        // Check if token is valid
                        if (messageService.validateToken(authToken)) {
                            // Get unread messages
                            List<RinnaMessage> unreadMessages = messageService.getUnreadMessagesForUser(currentUser);
                            
                            // Display notifications
                            if (!unreadMessages.isEmpty()) {
                                System.out.println();
                                System.out.println(AnsiFormatter.createBanner("MESSAGE NOTIFICATIONS"));
                                for (RinnaMessage message : unreadMessages) {
                                    String content = AnsiFormatter.format(message.getContent());
                                    String notification = "You have 1 unread message from " + 
                                        AnsiFormatter.BOLD + AnsiFormatter.BRIGHT_FG_CYAN + message.getSender() + AnsiFormatter.RESET + 
                                        ": '" + AnsiFormatter.BRIGHT_FG_GREEN + content + AnsiFormatter.RESET + "'";
                                    System.out.println(notification);
                                }
                                System.out.println();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Silently ignore any errors in the messaging system
            }
        } catch (Exception e) {
            // Silently ignore any errors in the overall notification system
            // We don't want to break CLI functionality if notifications fail
        }
    }
    
    /**
     * Main method to start the CLI.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Simple command router without using Picocli
        if (args == null || args.length == 0) {
            new RinnaCli().call();
            System.exit(0);
        }
        
        // Check for unread messages if user is authenticated
        checkUnreadMessages();
        
        String command = args[0];
        String[] subargs = new String[args.length - 1];
        System.arraycopy(args, 1, subargs, 0, args.length - 1);
        
        int exitCode;
        
        switch (command) {
            case "view":
                exitCode = handleViewCommand(subargs);
                break;
            case "list":
                exitCode = handleListCommand(subargs);
                break;
            case "add":
                exitCode = handleAddCommand(subargs);
                break;
            case "update":
                exitCode = handleUpdateCommand(subargs);
                break;
            case "path":
                exitCode = handlePathCommand(subargs);
                break;
            case "done":
                exitCode = handleDoneCommand(subargs);
                break;
            case "bug":
                exitCode = handleBugCommand(subargs);
                break;
            case "backlog":
                exitCode = handleBacklogCommand(subargs);
                break;
            case "server":
                exitCode = handleServerCommand(subargs);
                break;
            case "test":
                exitCode = handleTestCommand(subargs);
                break;
            case "admin":
                exitCode = handleAdminCommand(subargs);
                break;
            case "login":
                exitCode = handleLoginCommand(subargs);
                break;
            case "logout":
                exitCode = handleLogoutCommand(subargs);
                break;
            case "access":
                exitCode = handleAccessCommand(subargs);
                break;
            case "notify":
                exitCode = handleNotifyCommand(subargs);
                break;
            case "stats":
                exitCode = handleStatsCommand(subargs);
                break;
            case "import":
                exitCode = handleImportCommand(subargs);
                break;
            case "bulk":
                exitCode = handleBulkCommand(subargs);
                break;
            case "comment":
                exitCode = handleCommentCommand(subargs);
                break;
            case "history":
                exitCode = handleHistoryCommand(subargs);
                break;
            case "undo":
                exitCode = handleUndoCommand(subargs);
                break;
            case "ls":
                exitCode = handleLsCommand(subargs);
                break;
            case "edit":
                exitCode = handleEditCommand(subargs);
                break;
            case "grep":
                exitCode = handleGrepCommand(subargs);
                break;
            case "cat":
                exitCode = handleCatCommand(subargs);
                break;
            case "find":
                exitCode = handleFindCommand(subargs);
                break;
            case "msg":
                exitCode = handleMsgCommand(subargs);
                break;
            case "report":
                exitCode = handleReportCommand(subargs);
                break;
            case "schedule":
                exitCode = handleScheduleCommand(subargs);
                break;
            case "-h":
            case "--help":
                new RinnaCli().showHelp();
                exitCode = 0;
                break;
            case "-v":
            case "--version":
                System.out.println("Rinna CLI version 1.8.1");
                exitCode = 0;
                break;
            default:
                System.out.println("Unknown command: " + command);
                new RinnaCli().showHelp();
                exitCode = 1;
        }
        
        System.exit(exitCode);
    }
}