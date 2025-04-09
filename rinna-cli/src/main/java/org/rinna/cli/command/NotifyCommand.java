/*
 * Notification command for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.command;

import org.rinna.cli.notifications.Notification;
import org.rinna.cli.notifications.NotificationService;
import org.rinna.cli.notifications.NotificationType;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockNotificationService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Command for managing notifications.
 * Follows the ViewCommand pattern with proper MetadataService integration
 * for tracking notification operations.
 */
public class NotifyCommand implements Callable<Integer> {
    private String action = "list";
    private String[] args = new String[0];
    private UUID notificationId;
    private int days = 30;
    private NotificationType type;
    private String format = "text";
    private boolean verbose = false;
    private String username;
    
    // Services
    private final ServiceManager serviceManager;
    private final ConfigurationService configService;
    private final MetadataService metadataService;
    private final ContextManager contextManager;
    private final SecurityManager securityManager;
    
    // Notification services
    private NotificationService notificationService;
    private MockNotificationService mockNotificationService;
    
    /**
     * Default constructor using singleton service manager.
     */
    public NotifyCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructor with service manager for dependency injection.
     * 
     * @param serviceManager the service manager
     */
    public NotifyCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.configService = serviceManager.getConfigurationService();
        this.metadataService = serviceManager.getMetadataService();
        this.contextManager = ContextManager.getInstance();
        this.securityManager = SecurityManager.getInstance();
        
        // Get current user from configuration
        this.username = configService.getCurrentUser();
        if (this.username == null || this.username.isEmpty()) {
            this.username = System.getProperty("user.name");
        }
    }
    
    /**
     * Creates a new NotifyCommand with the specified action.
     * 
     * @param action the notification action to perform
     */
    public NotifyCommand(String action) {
        this();
        if (action != null) {
            this.action = action;
        }
    }
    
    /**
     * Sets the action to perform.
     * 
     * @param action the action
     * @return this command instance for method chaining
     */
    public NotifyCommand setAction(String action) {
        this.action = action;
        return this;
    }
    
    /**
     * Sets the additional arguments.
     * 
     * @param args the arguments
     * @return this command instance for method chaining
     */
    public NotifyCommand setArgs(String[] args) {
        this.args = args;
        return this;
    }
    
    /**
     * Sets the notification ID for the read/delete actions.
     * 
     * @param notificationId the notification ID
     * @return this command instance for method chaining
     */
    public NotifyCommand setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
        return this;
    }
    
    /**
     * Sets the number of days for filtering notifications.
     * 
     * @param days the number of days
     * @return this command instance for method chaining
     */
    public NotifyCommand setDays(int days) {
        this.days = days;
        return this;
    }
    
    /**
     * Sets the notification type for filtering.
     * 
     * @param type the notification type
     * @return this command instance for method chaining
     */
    public NotifyCommand setType(NotificationType type) {
        this.type = type;
        return this;
    }
    
    /**
     * Sets the output format.
     * 
     * @param format the output format ("text" or "json")
     * @return this command instance for method chaining
     */
    public NotifyCommand setFormat(String format) {
        this.format = format;
        return this;
    }
    
    /**
     * Sets the JSON output flag (for backward compatibility).
     * 
     * @param jsonOutput true to output in JSON format, false for text
     * @return this command instance for method chaining
     */
    public NotifyCommand setJsonOutput(boolean jsonOutput) {
        this.format = jsonOutput ? "json" : "text";
        return this;
    }
    
    /**
     * Sets the verbose output flag.
     * 
     * @param verbose true for verbose output, false for normal output
     * @return this command instance for method chaining
     */
    public NotifyCommand setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
    
    /**
     * Sets the username.
     * 
     * @param username the username
     * @return this command instance for method chaining
     */
    public NotifyCommand setUsername(String username) {
        this.username = username;
        return this;
    }
    
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("format", format);
        params.put("verbose", verbose);
        params.put("action", action);
        if (notificationId != null) {
            params.put("notificationId", notificationId.toString());
        }
        params.put("days", days);
        if (type != null) {
            params.put("type", type.toString());
        }
        
        // Start tracking main operation
        String operationId = metadataService.startOperation("notify-command", "MANAGE", params);
        
        try {
            // Check if user is authenticated
            if (!securityManager.isAuthenticated()) {
                // Track authentication failure
                metadataService.failOperation(operationId, 
                    new SecurityException("User not authenticated"));
                
                // Display error message
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("result", "error");
                    error.put("message", "You must be logged in to access notifications");
                    error.put("action", "Use 'rin login' to authenticate");
                    
                    System.out.println(OutputFormatter.toJson(error));
                } else {
                    System.err.println("Error: You must be logged in to access notifications.");
                    System.err.println("Use 'rin login' to authenticate.");
                }
                return 1;
            }
            
            // Track service initialization
            String initOpId = metadataService.startOperation(
                "notify-init-service", "INITIALIZE", 
                Map.of("username", username));
            
            // Initialize the notification services - first try real, fall back to mock
            boolean usingMockService = false;
            try {
                // Try to get the real notification service
                notificationService = NotificationService.getInstance();
                notificationService.initialize();
                
                metadataService.completeOperation(initOpId, 
                    Map.of("serviceType", "real", "success", true));
            } catch (Exception e) {
                // Fall back to the mock service if the real one isn't available
                mockNotificationService = serviceManager.getMockNotificationService();
                
                if (mockNotificationService == null) {
                    // If neither service is available, create a new mock service instance
                    mockNotificationService = MockNotificationService.getInstance();
                }
                
                usingMockService = true;
                
                if (verbose) {
                    System.err.println("Warning: Using mock notification service");
                }
                
                metadataService.completeOperation(initOpId, 
                    Map.of("serviceType", "mock", "fallback", true, "success", true));
            }
            
            // Handle different actions
            boolean success = false;
            switch (action) {
                case "list":
                    String listOpId = metadataService.startOperation(
                        "notify-list", "READ", 
                        Map.of("username", username, "format", format));
                    
                    success = handleListAction(listOpId);
                    break;
                    
                case "unread":
                    String unreadOpId = metadataService.startOperation(
                        "notify-unread", "READ", 
                        Map.of("username", username, "format", format));
                    
                    success = handleUnreadAction(unreadOpId);
                    break;
                    
                case "read":
                case "markread":
                    if (notificationId == null) {
                        displayError("Notification ID required", 
                                    "Usage: rin notify read <notification-id>");
                        
                        metadataService.failOperation(operationId, 
                            new IllegalArgumentException("Notification ID required"));
                        
                        return 1;
                    }
                    
                    String readOpId = metadataService.startOperation(
                        "notify-mark-read", "UPDATE", 
                        Map.of(
                            "username", username, 
                            "notificationId", notificationId.toString()
                        ));
                    
                    success = handleReadAction(readOpId);
                    break;
                    
                case "markall":
                    String markAllOpId = metadataService.startOperation(
                        "notify-mark-all", "UPDATE", 
                        Map.of("username", username));
                    
                    success = handleMarkAllAction(markAllOpId);
                    break;
                    
                case "clear":
                    String clearOpId = metadataService.startOperation(
                        "notify-clear", "DELETE", 
                        Map.of("username", username, "days", days));
                    
                    success = handleClearAction(clearOpId);
                    break;
                    
                case "help":
                    String helpOpId = metadataService.startOperation(
                        "notify-help", "READ", 
                        Map.of("username", username, "format", format));
                    
                    success = handleHelpAction(helpOpId);
                    break;
                    
                default:
                    displayError("Unknown action: " + action, null);
                    
                    metadataService.failOperation(operationId, 
                        new IllegalArgumentException("Unknown action: " + action));
                    
                    // Show help as a fallback
                    String fallbackHelpOpId = metadataService.startOperation(
                        "notify-help-fallback", "READ", 
                        Map.of("username", username, "format", format));
                    
                    handleHelpAction(fallbackHelpOpId);
                    
                    return 1;
            }
            
            // Complete main operation
            Map<String, Object> result = new HashMap<>();
            result.put("action", action);
            result.put("result", success ? "success" : "error");
            result.put("using_mock", usingMockService);
            
            metadataService.completeOperation(operationId, result);
            
            return success ? 0 : 1;
        } catch (Exception e) {
            displayError(e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Display an error message in the appropriate format.
     * 
     * @param message the error message
     * @param details additional details (can be null)
     */
    private void displayError(String message, String details) {
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> error = new HashMap<>();
            error.put("result", "error");
            error.put("message", message);
            
            if (details != null && !details.isEmpty()) {
                error.put("details", details);
            }
            
            System.out.println(OutputFormatter.toJson(error));
        } else {
            System.err.println("Error: " + message);
            
            if (details != null && !details.isEmpty()) {
                System.err.println(details);
            }
        }
    }
    
    /**
     * Handles the 'list' action.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean handleListAction(String operationId) {
        try {
            // Track fetching notifications
            String fetchOpId = metadataService.startOperation(
                "notify-list-fetch", "READ", 
                Map.of("username", username));
            
            List<Notification> notifications;
            
            // Get notifications from appropriate service
            if (notificationService != null) {
                notifications = notificationService.getNotificationsForCurrentUser();
            } else {
                notifications = mockNotificationService.getCurrentUserNotifications();
            }
            
            // Track completion of fetch operation
            Map<String, Object> fetchResult = new HashMap<>();
            fetchResult.put("count", notifications.size());
            metadataService.completeOperation(fetchOpId, fetchResult);
            
            // Track display operation
            String displayOpId = metadataService.startOperation(
                "notify-list-display", "READ", 
                Map.of("username", username, "format", format, "count", notifications.size()));
            
            if ("json".equalsIgnoreCase(format)) {
                displayNotificationsAsJson(notifications, "list");
            } else {
                displayNotificationsAsText(notifications, "Your Notifications");
            }
            
            // Track completion of display operation
            metadataService.completeOperation(displayOpId, Map.of("success", true));
            
            // Complete the main list operation
            Map<String, Object> result = new HashMap<>();
            result.put("count", notifications.size());
            result.put("success", true);
            metadataService.completeOperation(operationId, result);
            
            return true;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            displayError("Error listing notifications: " + e.getMessage(), null);
            return false;
        }
    }
    
    /**
     * Display notifications in JSON format.
     * 
     * @param notifications the list of notifications
     * @param action the action being performed
     */
    private void displayNotificationsAsJson(List<Notification> notifications, String action) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "success");
        response.put("action", action);
        response.put("count", notifications.size());
        
        if (notifications.isEmpty()) {
            response.put("notifications", new Notification[0]);
        } else {
            Map<String, Object>[] notificationsArray = new Map[notifications.size()];
            for (int i = 0; i < notifications.size(); i++) {
                Notification notification = notifications.get(i);
                boolean isRead = (notificationService != null) ? 
                                notification.isRead() : 
                                mockNotificationService.isNotificationRead(notification.getId());
                
                Map<String, Object> notificationMap = new HashMap<>();
                notificationMap.put("id", notification.getId().toString());
                notificationMap.put("type", notification.getType().toString());
                notificationMap.put("sender", notification.getSource());
                notificationMap.put("message", notification.getMessage());
                notificationMap.put("timestamp", notification.getTimestamp().toString());
                notificationMap.put("priority", notification.getPriority().toString());
                
                // Only include read status for 'list' action (not needed for 'unread' action)
                if ("list".equals(action)) {
                    notificationMap.put("read", isRead);
                }
                
                notificationsArray[i] = notificationMap;
            }
            
            response.put("notifications", notificationsArray);
        }
        
        System.out.println(OutputFormatter.toJson(response));
    }
    
    /**
     * Display notifications in text format.
     * 
     * @param notifications the list of notifications
     * @param headerText the header text to display
     */
    private void displayNotificationsAsText(List<Notification> notifications, String headerText) {
        if (notifications.isEmpty()) {
            System.out.println("You have no " + 
                (headerText.toLowerCase().contains("unread") ? "unread " : "") + 
                "notifications.");
            return;
        }
        
        System.out.println("=== " + headerText + " ===");
        for (Notification notification : notifications) {
            System.out.println(notification.format());
        }
        System.out.println("=".repeat(headerText.length() + 8));
        
        if (verbose) {
            System.out.println("\nTotal " + 
                (headerText.toLowerCase().contains("unread") ? "unread " : "") + 
                "notifications: " + notifications.size());
            
            // For "all notifications" view, also show unread count
            if (!headerText.toLowerCase().contains("unread")) {
                long unreadCount;
                
                if (notificationService != null) {
                    unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
                } else {
                    unreadCount = notifications.stream()
                        .filter(n -> !mockNotificationService.isNotificationRead(n.getId()))
                        .count();
                }
                
                System.out.println("Unread notifications: " + unreadCount);
            }
        }
    }
    
    /**
     * Handles the 'unread' action.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean handleUnreadAction(String operationId) {
        try {
            // Track fetching unread notifications
            String fetchOpId = metadataService.startOperation(
                "notify-unread-fetch", "READ", 
                Map.of("username", username));
            
            List<Notification> unreadNotifications;
            
            // Get unread notifications from appropriate service
            if (notificationService != null) {
                unreadNotifications = notificationService.getUnreadNotificationsForCurrentUser();
            } else {
                unreadNotifications = mockNotificationService.getUnreadNotificationsForCurrentUser();
            }
            
            // Track completion of fetch operation
            Map<String, Object> fetchResult = new HashMap<>();
            fetchResult.put("count", unreadNotifications.size());
            metadataService.completeOperation(fetchOpId, fetchResult);
            
            // Track display operation
            String displayOpId = metadataService.startOperation(
                "notify-unread-display", "READ", 
                Map.of("username", username, "format", format, "count", unreadNotifications.size()));
            
            if ("json".equalsIgnoreCase(format)) {
                displayNotificationsAsJson(unreadNotifications, "unread");
            } else {
                displayNotificationsAsText(unreadNotifications, "Unread Notifications");
            }
            
            // Track completion of display operation
            metadataService.completeOperation(displayOpId, Map.of("success", true));
            
            // Complete the main unread operation
            Map<String, Object> result = new HashMap<>();
            result.put("count", unreadNotifications.size());
            result.put("success", true);
            metadataService.completeOperation(operationId, result);
            
            return true;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            displayError("Error listing unread notifications: " + e.getMessage(), null);
            return false;
        }
    }
    
    /**
     * Handles the 'read' action.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean handleReadAction(String operationId) {
        try {
            // Mark notification as read
            String markOpId = metadataService.startOperation(
                "notify-mark-read-action", "UPDATE", 
                Map.of(
                    "username", username, 
                    "notificationId", notificationId.toString()
                ));
            
            boolean marked = false;
            
            // Mark notification as read in appropriate service
            if (notificationService != null) {
                marked = notificationService.markAsRead(notificationId);
            } else {
                marked = mockNotificationService.markAsRead(notificationId);
            }
            
            // Track result of mark operation
            Map<String, Object> markResult = new HashMap<>();
            markResult.put("success", marked);
            markResult.put("notificationId", notificationId.toString());
            metadataService.completeOperation(markOpId, markResult);
            
            if (marked) {
                // Track display operation
                String displayOpId = metadataService.startOperation(
                    "notify-mark-read-display", "READ", 
                    Map.of(
                        "username", username, 
                        "format", format, 
                        "notificationId", notificationId.toString()
                    ));
                
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("result", "success");
                    response.put("action", "read");
                    response.put("id", notificationId.toString());
                    response.put("message", "Notification marked as read");
                    
                    System.out.println(OutputFormatter.toJson(response));
                } else {
                    System.out.println("Notification marked as read.");
                    if (verbose) {
                        System.out.println("Notification ID: " + notificationId);
                    }
                }
                
                // Track completion of display operation
                metadataService.completeOperation(displayOpId, Map.of("success", true));
                
                // Complete the main read operation
                Map<String, Object> result = new HashMap<>();
                result.put("notificationId", notificationId.toString());
                result.put("marked", true);
                result.put("success", true);
                metadataService.completeOperation(operationId, result);
                
                return true;
            } else {
                displayError("Notification not found or already read", "ID: " + notificationId);
                
                // Fail the main operation with a specific error
                metadataService.failOperation(operationId, 
                    new IllegalStateException("Notification not found or already read"));
                
                return false;
            }
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            displayError("Error marking notification as read: " + e.getMessage(), null);
            return false;
        }
    }
    
    /**
     * Handles the 'markall' action.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean handleMarkAllAction(String operationId) {
        try {
            // Track getting unread count operation
            String countOpId = metadataService.startOperation(
                "notify-markall-count", "READ", 
                Map.of("username", username));
            
            int unreadCount = 0;
            
            // Get unread count before marking all as read
            if (verbose || "json".equalsIgnoreCase(format)) {
                if (notificationService != null) {
                    unreadCount = notificationService.getUnreadCount();
                } else {
                    unreadCount = mockNotificationService.getUnreadCountForCurrentUser();
                }
            }
            
            // Complete count operation
            metadataService.completeOperation(countOpId, 
                Map.of("unreadCount", unreadCount));
            
            // Track marking all as read operation
            String markAllOpId = metadataService.startOperation(
                "notify-markall-update", "UPDATE", 
                Map.of(
                    "username", username,
                    "unreadCount", unreadCount
                ));
            
            // Mark all as read in appropriate service
            if (notificationService != null) {
                notificationService.markAllAsRead();
            } else {
                mockNotificationService.markAllAsReadForCurrentUser();
            }
            
            // Complete mark all operation
            metadataService.completeOperation(markAllOpId, 
                Map.of("success", true, "count", unreadCount));
            
            // Track display operation
            String displayOpId = metadataService.startOperation(
                "notify-markall-display", "READ", 
                Map.of(
                    "username", username, 
                    "format", format, 
                    "unreadCount", unreadCount
                ));
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> response = new HashMap<>();
                response.put("result", "success");
                response.put("action", "markall");
                response.put("count", unreadCount);
                response.put("message", "All notifications marked as read");
                
                System.out.println(OutputFormatter.toJson(response));
            } else {
                System.out.println("All notifications marked as read.");
                if (verbose) {
                    System.out.println("Marked " + unreadCount + " notifications as read.");
                }
            }
            
            // Complete display operation
            metadataService.completeOperation(displayOpId, Map.of("success", true));
            
            // Complete the main markall operation
            Map<String, Object> result = new HashMap<>();
            result.put("count", unreadCount);
            result.put("success", true);
            metadataService.completeOperation(operationId, result);
            
            return true;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            displayError("Error marking all notifications as read: " + e.getMessage(), null);
            return false;
        }
    }
    
    /**
     * Handles the 'clear' action.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean handleClearAction(String operationId) {
        try {
            // Default to 30 days if not specified
            int daysToKeep = days > 0 ? days : 30;
            
            // Track clear operation
            String clearOpId = metadataService.startOperation(
                "notify-clear-delete", "DELETE", 
                Map.of(
                    "username", username,
                    "days", daysToKeep
                ));
            
            int deleted = 0;
            
            // Delete old notifications using appropriate service
            if (notificationService != null) {
                deleted = notificationService.deleteOldNotifications(daysToKeep);
            } else {
                deleted = mockNotificationService.clearOldNotifications(daysToKeep);
            }
            
            // Complete clear operation
            metadataService.completeOperation(clearOpId, 
                Map.of("success", true, "deleted", deleted, "days", daysToKeep));
            
            // Track display operation
            String displayOpId = metadataService.startOperation(
                "notify-clear-display", "READ", 
                Map.of(
                    "username", username, 
                    "format", format, 
                    "deleted", deleted,
                    "days", daysToKeep
                ));
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> response = new HashMap<>();
                response.put("result", "success");
                response.put("action", "clear");
                response.put("deleted", deleted);
                response.put("days", daysToKeep);
                
                System.out.println(OutputFormatter.toJson(response));
            } else {
                if (deleted > 0) {
                    System.out.println("Cleared " + deleted + " old notification(s).");
                } else {
                    System.out.println("No old notifications to clear.");
                }
                
                if (verbose) {
                    System.out.println("Retention period: " + daysToKeep + " days");
                }
            }
            
            // Complete display operation
            metadataService.completeOperation(displayOpId, Map.of("success", true));
            
            // Complete the main clear operation
            Map<String, Object> result = new HashMap<>();
            result.put("deleted", deleted);
            result.put("days", daysToKeep);
            result.put("success", true);
            metadataService.completeOperation(operationId, result);
            
            return true;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            displayError("Error clearing notifications: " + e.getMessage(), null);
            return false;
        }
    }
    
    /**
     * Handles the 'help' action.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean handleHelpAction(String operationId) {
        try {
            if ("json".equalsIgnoreCase(format)) {
                displayHelpAsJson();
            } else {
                displayHelpAsText();
            }
            
            // Complete the help operation
            metadataService.completeOperation(operationId, Map.of("success", true));
            
            return true;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            displayError("Error displaying help: " + e.getMessage(), null);
            return false;
        }
    }
    
    /**
     * Display help information in JSON format.
     */
    private void displayHelpAsJson() {
        Map<String, Object> help = new HashMap<>();
        help.put("result", "success");
        help.put("command", "notify");
        help.put("usage", "rin notify [action] [options]");
        
        // Actions
        Map<String, Object>[] actions = new Map[7];
        
        Map<String, Object> listAction = new HashMap<>();
        listAction.put("name", "list");
        listAction.put("description", "Show all notifications");
        listAction.put("usage", "rin notify list");
        actions[0] = listAction;
        
        Map<String, Object> unreadAction = new HashMap<>();
        unreadAction.put("name", "unread");
        unreadAction.put("description", "Show unread notifications");
        unreadAction.put("usage", "rin notify unread");
        actions[1] = unreadAction;
        
        Map<String, Object> readAction = new HashMap<>();
        readAction.put("name", "read");
        readAction.put("description", "Mark a notification as read");
        readAction.put("usage", "rin notify read <id>");
        actions[2] = readAction;
        
        Map<String, Object> markreadAction = new HashMap<>();
        markreadAction.put("name", "markread");
        markreadAction.put("description", "Mark a notification as read");
        markreadAction.put("usage", "rin notify markread <id>");
        actions[3] = markreadAction;
        
        Map<String, Object> markallAction = new HashMap<>();
        markallAction.put("name", "markall");
        markallAction.put("description", "Mark all notifications as read");
        markallAction.put("usage", "rin notify markall");
        actions[4] = markallAction;
        
        Map<String, Object> clearAction = new HashMap<>();
        clearAction.put("name", "clear");
        clearAction.put("description", "Clear old notifications");
        clearAction.put("usage", "rin notify clear [--days=N]");
        actions[5] = clearAction;
        
        Map<String, Object> helpAction = new HashMap<>();
        helpAction.put("name", "help");
        helpAction.put("description", "Show help information");
        helpAction.put("usage", "rin notify help");
        actions[6] = helpAction;
        
        help.put("actions", actions);
        
        // Options
        Map<String, Object>[] options = new Map[3];
        
        Map<String, Object> jsonOpt = new HashMap<>();
        jsonOpt.put("name", "--json");
        jsonOpt.put("description", "Output in JSON format");
        options[0] = jsonOpt;
        
        Map<String, Object> verboseOpt = new HashMap<>();
        verboseOpt.put("name", "--verbose");
        verboseOpt.put("description", "Show verbose output with additional details");
        options[1] = verboseOpt;
        
        Map<String, Object> daysOpt = new HashMap<>();
        daysOpt.put("name", "--days=N");
        daysOpt.put("description", "Used with 'clear' to specify days to keep");
        options[2] = daysOpt;
        
        help.put("options", options);
        
        System.out.println(OutputFormatter.toJson(help));
    }
    
    /**
     * Display help information in text format.
     */
    private void displayHelpAsText() {
        System.out.println("Notification Command Usage:");
        System.out.println("  rin notify                   Show all notifications");
        System.out.println("  rin notify list              Show all notifications");
        System.out.println("  rin notify unread            Show unread notifications");
        System.out.println("  rin notify read <id>         Mark a notification as read");
        System.out.println("  rin notify markread <id>     Mark a notification as read");
        System.out.println("  rin notify markall           Mark all notifications as read");
        System.out.println("  rin notify clear             Clear old notifications");
        System.out.println("  rin notify clear --days=N    Clear notifications older than N days");
        System.out.println("  rin notify help              Show this help");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --json                       Output in JSON format");
        System.out.println("  --verbose                    Show verbose output with additional details");
    }
}