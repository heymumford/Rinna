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

import org.rinna.cli.messaging.AnsiFormatter;
import org.rinna.cli.messaging.MessageService;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ProjectContext;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Command for handling messaging operations.
 * This command allows users to send, receive, and manage messages 
 * within projects, similar to an email system.
 * 
 * Usage examples:
 * - rin msg - List all messages
 * - rin msg unread - List unread messages
 * - rin msg read <message-id> - Read a specific message
 * - rin msg <recipient> <message> - Send a message to a recipient
 */
public class MsgCommand implements Callable<Integer> {
    
    private String subcommand;
    private String[] args;
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MessageService messageService;
    private final ProjectContext projectContext;
    private final ConfigurationService configService;
    private final MetadataService metadataService;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
    
    /**
     * Creates a new MsgCommand with default services.
     */
    public MsgCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new MsgCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public MsgCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.messageService = serviceManager.getMessageService();
        this.projectContext = serviceManager.getProjectContext();
        this.configService = serviceManager.getConfigurationService();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    /**
     * Sets the subcommand.
     *
     * @param subcommand the subcommand
     */
    public void setSubcommand(String subcommand) {
        this.subcommand = subcommand;
    }
    
    /**
     * Sets the command arguments.
     *
     * @param args the command arguments
     */
    public void setArgs(String[] args) {
        this.args = args;
    }
    
    /**
     * Sets the verbose flag for more detailed output.
     *
     * @param verbose true for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Gets a project key from a project name.
     * This extracts an abbreviation based on the project name.
     *
     * @param projectName the project name
     * @return the project key (typically 3-4 letters)
     */
    private String getProjectKey(String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            return "UNK";
        }
        
        // Check if the project context already has a mapping for this project
        String existingKey = projectContext.getProjectKey(projectName);
        if (existingKey != null && !existingKey.isEmpty()) {
            return existingKey;
        }
        
        // For "Tracer" and "Quantum" we have predefined keys
        if ("Tracer".equals(projectName)) {
            return "TRC";
        } else if ("Quantum".equals(projectName)) {
            return "QTM";
        }
        
        // Generate a key for other projects
        // Use first 3 characters or generate from first letters of words
        if (projectName.length() <= 3) {
            return projectName.toUpperCase();
        }
        
        // If the name has multiple words, use the first letter of each word
        if (projectName.contains(" ")) {
            StringBuilder key = new StringBuilder();
            for (String word : projectName.split("\\s+")) {
                if (!word.isEmpty()) {
                    key.append(Character.toUpperCase(word.charAt(0)));
                }
            }
            return key.toString();
        }
        
        // Use the first 3 letters for single-word projects
        return projectName.substring(0, 3).toUpperCase();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("subcommand", subcommand != null ? subcommand : "list");
        if (args != null && args.length > 0) {
            params.put("args", String.join(" ", args));
        }
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("msg", "MESSAGING", params);
        
        try {
            // Check if user is authenticated
            if (!isAuthenticated()) {
                System.err.println("Error: Authentication required. Please login first.");
                metadataService.failOperation(operationId, new IllegalStateException("Authentication required"));
                return 1;
            }
            
            int result;
            
            // Handle subcommands
            if (subcommand == null || subcommand.isEmpty()) {
                // No subcommand, list all messages
                result = listMessages();
            } else if ("login".equals(subcommand)) {
                result = login();
            } else if ("unread".equals(subcommand)) {
                result = listUnreadMessages();
            } else if ("project".equals(subcommand)) {
                result = handleProjectCommand();
            } else if ("read".equals(subcommand) && args != null && args.length > 0) {
                result = readMessage(args[0]);
            } else if ("delete".equals(subcommand) && args != null && args.length > 0) {
                result = deleteMessage(args[0]);
            } else if ("reply".equals(subcommand) && args != null && args.length > 1) {
                result = replyToMessage(args[0], 
                    String.join(" ", args).substring(args[0].length()).trim());
            } else if ("from".equals(subcommand) && args != null && args.length > 0) {
                result = listMessagesFromSender(args[0]);
            } else if (subcommand.startsWith("-")) {
                // Handle option flags
                if ("--unread".equals(subcommand)) {
                    result = listUnreadMessages();
                } else if (subcommand.startsWith("--from=")) {
                    String sender = subcommand.substring("--from=".length());
                    result = listMessagesFromSender(sender);
                } else if (subcommand.startsWith("--project=")) {
                    String projectName = subcommand.substring("--project=".length());
                    result = listMessagesFromProject(projectName);
                } else if ("--help".equals(subcommand) || "-h".equals(subcommand)) {
                    showHelp();
                    
                    // Record the successful help operation
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("action", "help");
                    resultData.put("status", "success");
                    metadataService.completeOperation(operationId, resultData);
                    
                    result = 0;
                } else {
                    System.err.println("Error: Unknown option: " + subcommand);
                    showHelp();
                    
                    metadataService.failOperation(operationId, 
                        new IllegalArgumentException("Unknown option: " + subcommand));
                    
                    result = 1;
                }
            } else {
                // Assume it's a direct message to a recipient
                if (!projectContext.isProjectActive()) {
                    System.err.println("Error: No active project. Please switch to a project first.");
                    
                    metadataService.failOperation(operationId, 
                        new IllegalStateException("No active project"));
                    
                    return 1;
                }
                
                String recipient = subcommand;
                String message = args != null && args.length > 0 ? String.join(" ", args) : null;
                
                if (message == null || message.isEmpty()) {
                    System.err.println("Error: Message content is required");
                    
                    metadataService.failOperation(operationId, 
                        new IllegalArgumentException("Message content is required"));
                    
                    return 1;
                }
                
                result = sendMessage(recipient, message);
            }
            
            return result;
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error executing message command: " + e.getMessage();
            System.err.println("Error: " + e.getMessage());
            
            // Record detailed error information if verbose mode is enabled
            if (verbose) {
                e.printStackTrace();
            }
            
            // Record the failed operation with error details
            metadataService.failOperation(operationId, e);
            
            return 1;
        }
    }
    
    /**
     * Checks if the user is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    private boolean isAuthenticated() {
        return configService.isAuthenticated() && 
               messageService.validateToken(configService.getAuthToken());
    }
    
    /**
     * Handles user login.
     *
     * @return exit code
     */
    private int login() {
        if (args == null || args.length < 2) {
            System.err.println("Error: Username and password are required");
            System.err.println("Usage: rin msg login <username> <password>");
            return 1;
        }
        
        String username = args[0];
        String password = args[1];
        
        try {
            String token = messageService.authenticate(username, password);
            if (token != null) {
                // Store authentication info
                configService.setCurrentUser(username);
                configService.setAuthToken(token);
                System.out.println("Authentication successful. Welcome, " + username + "!");
                
                // Check for unread messages
                showUnreadMessageNotifications(username);
                
                return 0;
            } else {
                System.err.println("Authentication failed: Invalid credentials");
                return 1;
            }
        } catch (Exception e) {
            System.err.println("Error during authentication: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles project-related subcommands.
     *
     * @return exit code
     */
    private int handleProjectCommand() {
        if (args == null || args.length == 0) {
            System.err.println("Error: Project subcommand required");
            System.err.println("Available subcommands: list, switch");
            return 1;
        }
        
        String projectCmd = args[0];
        
        if ("list".equals(projectCmd)) {
            // List all projects the user is a member of
            System.out.println("Projects:");
            
            ProjectContext context = ServiceManager.getInstance().getProjectContext();
            String currentProject = context.getCurrentProject();
            
            // Get available projects from the message service
            String currentUser = configService.getCurrentUser();
            Map<String, List<String>> projects = messageService.getAvailableProjects(currentUser);
            
            if (projects.isEmpty()) {
                System.out.println("No projects available for your user account.");
                return 0;
            }
            
            // Sort projects alphabetically for consistent output
            List<String> projectNames = new ArrayList<>(projects.keySet());
            Collections.sort(projectNames);
            
            // Display each project with its key and members
            for (String projectName : projectNames) {
                String projectKey = getProjectKey(projectName);
                boolean isCurrent = (currentProject != null && currentProject.equals(projectName));
                
                StringBuilder projectDisplay = new StringBuilder();
                projectDisplay.append(projectName)
                             .append(" (")
                             .append(projectKey)
                             .append(")")
                             .append(isCurrent ? " (current)" : "");
                
                System.out.println(projectDisplay.toString());
                
                // If in verbose mode, display project members
                if (verbose) {
                    List<String> members = projects.get(projectName);
                    System.out.println("  Members: " + String.join(", ", members));
                }
            }
            
            return 0;
        } else if ("switch".equals(projectCmd) && args.length > 1) {
            // Switch to a project
            String projectName = args[1];
            String currentUser = configService.getCurrentUser();
            
            if (!messageService.canAccessProject(currentUser, projectName)) {
                System.err.println("Error: Access denied: You are not a member of project " + projectName);
                return 1;
            }
            
            projectContext.setCurrentProject(projectName);
            
            if ("Tracer".equals(projectName)) {
                projectContext.setCurrentProjectKey("TRC");
            } else if ("Quantum".equals(projectName)) {
                projectContext.setCurrentProjectKey("QTM");
            }
            
            System.out.println("Switched to project: " + projectName);
            return 0;
        } else {
            System.err.println("Error: Unknown project subcommand: " + projectCmd);
            System.err.println("Available subcommands: list, switch");
            return 1;
        }
    }
    
    /**
     * Lists all messages for the current user (simplified version).
     *
     * @return exit code
     */
    private int listMessages() {
        // Generate a new operation ID
        String operationId = metadataService.startOperation("message", "LIST", new HashMap<>());
        return listMessages(operationId);
    }
    
    /**
     * Lists all messages for the current user.
     *
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int listMessages(String operationId) {
        String currentUser = configService.getCurrentUser();
        List<RinnaMessage> messages = messageService.getMessagesForUser(currentUser);
        
        System.out.println("Messages for " + currentUser + ":");
        
        // Record operation data
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("user", currentUser);
        resultData.put("count", messages.size());
        resultData.put("action", "list");
        
        if (messages.isEmpty()) {
            System.out.println("No messages found.");
            metadataService.completeOperation(operationId, resultData);
            return 0;
        }
        
        for (RinnaMessage message : messages) {
            printMessageSummary(message);
            // Mark messages as read when viewed
            messageService.markMessageAsRead(message.getId());
        }
        
        metadataService.completeOperation(operationId, resultData);
        return 0;
    }
    
    /**
     * Lists unread messages for the current user (simplified version).
     *
     * @return exit code
     */
    private int listUnreadMessages() {
        // Generate a new operation ID
        String operationId = metadataService.startOperation("message", "LIST_UNREAD", new HashMap<>());
        return listUnreadMessages(operationId);
    }
    
    /**
     * Lists unread messages for the current user.
     *
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int listUnreadMessages(String operationId) {
        String currentUser = configService.getCurrentUser();
        List<RinnaMessage> messages = messageService.getUnreadMessagesForUser(currentUser);
        
        System.out.println("Unread messages for " + currentUser + ":");
        
        // Record operation data
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("user", currentUser);
        resultData.put("count", messages.size());
        resultData.put("action", "list_unread");
        
        if (messages.isEmpty()) {
            System.out.println("No unread messages found.");
            metadataService.completeOperation(operationId, resultData);
            return 0;
        }
        
        for (RinnaMessage message : messages) {
            printMessageSummary(message);
            // Don't mark as read here to maintain unread status
        }
        
        metadataService.completeOperation(operationId, resultData);
        return 0;
    }
    
    /**
     * Lists messages from a specific sender (simplified version).
     *
     * @param sender the sender username
     * @return exit code
     */
    private int listMessagesFromSender(String sender) {
        // Generate a new operation ID
        Map<String, Object> params = new HashMap<>();
        params.put("sender", sender);
        String operationId = metadataService.startOperation("message", "LIST_FROM_SENDER", params);
        return listMessagesFromSender(sender, operationId);
    }
    
    /**
     * Lists messages from a specific sender.
     *
     * @param sender the sender username
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int listMessagesFromSender(String sender, String operationId) {
        String currentUser = configService.getCurrentUser();
        List<RinnaMessage> messages = messageService.getMessagesForUserBySender(currentUser, sender);
        
        System.out.println("Messages from " + sender + ":");
        
        // Record operation data
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("user", currentUser);
        resultData.put("sender", sender);
        resultData.put("count", messages.size());
        resultData.put("action", "list_from_sender");
        
        if (messages.isEmpty()) {
            System.out.println("No messages found from " + sender + ".");
            metadataService.completeOperation(operationId, resultData);
            return 0;
        }
        
        for (RinnaMessage message : messages) {
            printMessageSummary(message);
            // Mark messages as read when viewed
            messageService.markMessageAsRead(message.getId());
        }
        
        metadataService.completeOperation(operationId, resultData);
        return 0;
    }
    
    /**
     * Lists messages from a specific project.
     *
     * @param projectName the project name
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int listMessagesFromProject(String projectName, String operationId) {
        String currentUser = configService.getCurrentUser();
        List<RinnaMessage> messages = messageService.getMessagesForUserByProject(currentUser, projectName);
        
        System.out.println("Messages from project " + projectName + ":");
        
        // Record operation data
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("user", currentUser);
        resultData.put("project", projectName);
        resultData.put("count", messages.size());
        resultData.put("action", "list_from_project");
        
        if (messages.isEmpty()) {
            System.out.println("No messages found from project " + projectName + ".");
            metadataService.completeOperation(operationId, resultData);
            return 0;
        }
        
        for (RinnaMessage message : messages) {
            printMessageSummary(message);
            // Mark messages as read when viewed
            messageService.markMessageAsRead(message.getId());
        }
        
        metadataService.completeOperation(operationId, resultData);
        return 0;
    }
    
    /**
     * Lists messages from a specific project (simplified version).
     *
     * @param projectName the project name
     * @return exit code
     */
    private int listMessagesFromProject(String projectName) {
        // Generate a new operation ID
        Map<String, Object> params = new HashMap<>();
        params.put("project", projectName);
        String operationId = metadataService.startOperation("message", "LIST_FROM_PROJECT", params);
        return listMessagesFromProject(projectName, operationId);
    }
    
    /**
     * Reads a specific message.
     *
     * @param messageId the message ID
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int readMessage(String messageId, String operationId) {
        RinnaMessage message = messageService.getMessage(messageId);
        
        if (message == null) {
            System.err.println("Error: Message with ID '" + messageId + "' not found");
            metadataService.failOperation(operationId, new IllegalArgumentException("Message not found: " + messageId));
            return 1;
        }
        
        String currentUser = configService.getCurrentUser();
        if (!message.getRecipient().equals(currentUser)) {
            System.err.println("Error: Cannot read: You don't have permission to read this message");
            metadataService.failOperation(operationId, new SecurityException("Permission denied: cannot read message"));
            return 1;
        }
        
        printMessageDetails(message);
        
        // Mark message as read
        messageService.markMessageAsRead(messageId);
        
        // Record successful operation
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("message_id", messageId);
        resultData.put("sender", message.getSender());
        resultData.put("project", message.getProject());
        resultData.put("action", "read");
        metadataService.completeOperation(operationId, resultData);
        
        return 0;
    }
    
    /**
     * Reads a specific message (simplified version).
     *
     * @param messageId the message ID
     * @return exit code
     */
    private int readMessage(String messageId) {
        RinnaMessage message = messageService.getMessage(messageId);
        
        if (message == null) {
            System.err.println("Error: Message with ID '" + messageId + "' not found");
            return 1;
        }
        
        String currentUser = configService.getCurrentUser();
        if (!message.getRecipient().equals(currentUser)) {
            System.err.println("Error: Cannot read: You don't have permission to read this message");
            return 1;
        }
        
        printMessageDetails(message);
        
        // Mark message as read
        messageService.markMessageAsRead(messageId);
        
        return 0;
    }
    
    /**
     * Deletes a specific message (simplified version).
     *
     * @param messageId the message ID
     * @return exit code
     */
    private int deleteMessage(String messageId) {
        // Generate a new operation ID
        Map<String, Object> params = new HashMap<>();
        params.put("message_id", messageId);
        String operationId = metadataService.startOperation("message", "DELETE", params);
        return deleteMessage(messageId, operationId);
    }
    
    /**
     * Deletes a specific message.
     *
     * @param messageId the message ID
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int deleteMessage(String messageId, String operationId) {
        RinnaMessage message = messageService.getMessage(messageId);
        
        if (message == null) {
            System.err.println("Error: Message with ID '" + messageId + "' not found");
            metadataService.failOperation(operationId, new IllegalArgumentException("Message not found: " + messageId));
            return 1;
        }
        
        String currentUser = configService.getCurrentUser();
        if (!message.getRecipient().equals(currentUser)) {
            System.err.println("Error: Cannot delete: You don't have permission to delete this message");
            metadataService.failOperation(operationId, new SecurityException("Permission denied: cannot delete message"));
            return 1;
        }
        
        // Delete the message
        if (messageService.deleteMessage(messageId, currentUser)) {
            System.out.println("Message deleted");
            
            // Record successful operation
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("message_id", messageId);
            resultData.put("sender", message.getSender());
            resultData.put("action", "delete");
            metadataService.completeOperation(operationId, resultData);
            
            return 0;
        } else {
            System.err.println("Error: Failed to delete message");
            metadataService.failOperation(operationId, new RuntimeException("Failed to delete message"));
            return 1;
        }
    }
    
    /**
     * Replies to a specific message (simplified version).
     *
     * @param messageId    the message ID
     * @param replyContent the reply content
     * @return exit code
     */
    private int replyToMessage(String messageId, String replyContent) {
        // Generate a new operation ID
        Map<String, Object> params = new HashMap<>();
        params.put("message_id", messageId);
        params.put("content_length", replyContent != null ? replyContent.length() : 0);
        String operationId = metadataService.startOperation("message", "REPLY", params);
        return replyToMessage(messageId, replyContent, operationId);
    }
    
    /**
     * Replies to a specific message.
     *
     * @param messageId    the message ID
     * @param replyContent the reply content
     * @param operationId  the operation ID for tracking
     * @return exit code
     */
    private int replyToMessage(String messageId, String replyContent, String operationId) {
        RinnaMessage originalMessage = messageService.getMessage(messageId);
        
        if (originalMessage == null) {
            System.err.println("Error: Message with ID '" + messageId + "' not found");
            metadataService.failOperation(operationId, new IllegalArgumentException("Message not found: " + messageId));
            return 1;
        }
        
        String currentUser = configService.getCurrentUser();
        if (!originalMessage.getRecipient().equals(currentUser)) {
            System.err.println("Error: Cannot reply: You don't have permission to reply to this message");
            metadataService.failOperation(operationId, new SecurityException("Permission denied: cannot reply to message"));
            return 1;
        }
        
        if (replyContent == null || replyContent.isEmpty()) {
            System.err.println("Error: Reply content is required");
            metadataService.failOperation(operationId, new IllegalArgumentException("Reply content is required"));
            return 1;
        }
        
        // Create and send the reply
        RinnaMessage reply = new RinnaMessage(
                "reply-" + UUID.randomUUID().toString().substring(0, 8),
                currentUser,
                originalMessage.getSender(),
                replyContent,
                originalMessage.getProject(),
                Instant.now(),
                MessageStatus.UNREAD
        );
        
        // Set reply reference
        reply.setInReplyTo(messageId);
        
        if (messageService.sendMessage(reply)) {
            System.out.println("Reply sent to " + originalMessage.getSender());
            
            // Record successful operation
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("message_id", reply.getId());
            resultData.put("in_reply_to", messageId);
            resultData.put("recipient", originalMessage.getSender());
            resultData.put("project", originalMessage.getProject());
            resultData.put("action", "reply");
            metadataService.completeOperation(operationId, resultData);
            
            return 0;
        } else {
            System.err.println("Error: Failed to send reply");
            metadataService.failOperation(operationId, new RuntimeException("Failed to send reply"));
            return 1;
        }
    }
    
    /**
     * Sends a direct message to a recipient (simplified version).
     *
     * @param recipient     the recipient username
     * @param messageContent the message content
     * @return exit code
     */
    private int sendMessage(String recipient, String messageContent) {
        // Generate a new operation ID
        Map<String, Object> params = new HashMap<>();
        params.put("recipient", recipient);
        params.put("content_length", messageContent != null ? messageContent.length() : 0);
        String operationId = metadataService.startOperation("message", "SEND", params);
        return sendMessage(recipient, messageContent, operationId);
    }
    
    /**
     * Sends a direct message to a recipient.
     *
     * @param recipient     the recipient username
     * @param messageContent the message content
     * @param operationId  the operation ID for tracking
     * @return exit code
     */
    private int sendMessage(String recipient, String messageContent, String operationId) {
        String currentUser = configService.getCurrentUser();
        String currentProject = projectContext.getCurrentProject();
        
        // Check if the recipient exists and is in the current project
        if (!projectContext.isProjectMember(currentProject, recipient)) {
            System.err.println("Error: Cannot send message: " + recipient + 
                    " is not a member of project " + currentProject);
            metadataService.failOperation(operationId, 
                new IllegalArgumentException("Recipient is not a member of current project"));
            return 1;
        }
        
        // Create the message
        RinnaMessage message = new RinnaMessage(
                "msg-" + UUID.randomUUID().toString().substring(0, 8),
                currentUser,
                recipient,
                messageContent,
                currentProject,
                Instant.now(),
                MessageStatus.UNREAD
        );
        
        // Send the message
        if (messageService.sendMessage(message)) {
            System.out.println("Message sent to " + recipient);
            
            // Record successful operation
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("message_id", message.getId());
            resultData.put("recipient", recipient);
            resultData.put("project", currentProject);
            resultData.put("action", "send");
            metadataService.completeOperation(operationId, resultData);
            
            return 0;
        } else {
            System.err.println("Error: Failed to send message");
            metadataService.failOperation(operationId, new RuntimeException("Failed to send message"));
            return 1;
        }
    }
    
    /**
     * Prints a summary of a message.
     *
     * @param message the message
     */
    private void printMessageSummary(RinnaMessage message) {
        String formattedDate = DATE_FORMATTER.format(message.getTimestamp());
        
        // Style the message based on its status
        if (message.getStatus() == MessageStatus.UNREAD) {
            System.out.println(
                AnsiFormatter.BRIGHT_FG_WHITE + AnsiFormatter.BOLD + "[UNREAD] " + 
                AnsiFormatter.BRIGHT_FG_YELLOW + message.getId() + AnsiFormatter.RESET + " - " + 
                AnsiFormatter.FG_GREEN + formattedDate + AnsiFormatter.RESET + " - " +
                "From: " + AnsiFormatter.BRIGHT_FG_CYAN + AnsiFormatter.BOLD + message.getSender() + AnsiFormatter.RESET + 
                " (" + AnsiFormatter.FG_BLUE + message.getProject() + AnsiFormatter.RESET + ") - " + 
                AnsiFormatter.format(message.getContent())
            );
        } else {
            System.out.println(
                AnsiFormatter.FG_YELLOW + message.getId() + AnsiFormatter.RESET + " - " + 
                AnsiFormatter.FG_GREEN + formattedDate + AnsiFormatter.RESET + " - " +
                "From: " + AnsiFormatter.FG_CYAN + message.getSender() + AnsiFormatter.RESET + 
                " (" + AnsiFormatter.FG_BLUE + message.getProject() + AnsiFormatter.RESET + ") - " + 
                AnsiFormatter.format(message.getContent())
            );
        }
    }
    
    /**
     * Prints detailed information about a message.
     *
     * @param message the message
     */
    private void printMessageDetails(RinnaMessage message) {
        // Use our BBS-style message box for the message content
        String title = "Message " + message.getId();
        
        StringBuilder metadata = new StringBuilder();
        metadata.append(AnsiFormatter.BRIGHT_FG_CYAN)
                .append("From: ")
                .append(AnsiFormatter.RESET)
                .append(AnsiFormatter.BRIGHT_FG_WHITE)
                .append(AnsiFormatter.BOLD)
                .append(message.getSender())
                .append(AnsiFormatter.RESET)
                .append("\n");
        
        metadata.append(AnsiFormatter.BRIGHT_FG_CYAN)
                .append("Project: ")
                .append(AnsiFormatter.RESET)
                .append(AnsiFormatter.FG_BLUE)
                .append(message.getProject())
                .append(AnsiFormatter.RESET)
                .append("\n");
        
        metadata.append(AnsiFormatter.BRIGHT_FG_CYAN)
                .append("Time: ")
                .append(AnsiFormatter.RESET)
                .append(AnsiFormatter.FG_GREEN)
                .append(DATE_FORMATTER.format(message.getTimestamp()))
                .append(AnsiFormatter.RESET)
                .append("\n");
        
        String statusColor = message.getStatus() == MessageStatus.UNREAD ? 
                AnsiFormatter.BRIGHT_FG_YELLOW : AnsiFormatter.FG_GREEN;
        metadata.append(AnsiFormatter.BRIGHT_FG_CYAN)
                .append("Status: ")
                .append(AnsiFormatter.RESET)
                .append(statusColor)
                .append(message.getStatus())
                .append(AnsiFormatter.RESET)
                .append("\n");
        
        if (message.getInReplyTo() != null) {
            metadata.append(AnsiFormatter.BRIGHT_FG_CYAN)
                    .append("In reply to: ")
                    .append(AnsiFormatter.RESET)
                    .append(AnsiFormatter.FG_YELLOW)
                    .append(message.getInReplyTo())
                    .append(AnsiFormatter.RESET)
                    .append("\n");
        }
        
        metadata.append("\n")
                .append(AnsiFormatter.BRIGHT_FG_MAGENTA)
                .append("Content:")
                .append(AnsiFormatter.RESET)
                .append("\n\n")
                .append(AnsiFormatter.format(message.getContent()));
        
        // Display the formatted message box
        System.out.println(AnsiFormatter.createMessageBox(title, metadata.toString(), 70));
    }
    
    /**
     * Displays notifications for unread messages.
     *
     * @param username the username
     */
    private void showUnreadMessageNotifications(String username) {
        List<RinnaMessage> unreadMessages = messageService.getUnreadMessagesForUser(username);
        
        if (!unreadMessages.isEmpty()) {
            System.out.println();
            System.out.println(AnsiFormatter.createBanner("MESSAGE NOTIFICATIONS"));
            for (RinnaMessage message : unreadMessages) {
                String notification = "You have 1 unread message from " + 
                    AnsiFormatter.BOLD + AnsiFormatter.BRIGHT_FG_CYAN + message.getSender() + AnsiFormatter.RESET + 
                    ": '" + AnsiFormatter.BRIGHT_FG_GREEN + message.getContent() + AnsiFormatter.RESET + "'";
                System.out.println(notification);
            }
            System.out.println();
        }
    }
    
    /**
     * Displays help information.
     */
    private void showHelp() {
        // Create a colorful help message header
        System.out.println(AnsiFormatter.BRIGHT_FG_MAGENTA + "╔════════════════════════════════════════════════╗" + AnsiFormatter.RESET);
        System.out.println(AnsiFormatter.BRIGHT_FG_MAGENTA + "║" + AnsiFormatter.RESET + " " + 
                           AnsiFormatter.BOLD + AnsiFormatter.BRIGHT_FG_CYAN + "RINNA CLI MESSAGING SYSTEM" + AnsiFormatter.RESET + 
                           "                     " + AnsiFormatter.BRIGHT_FG_MAGENTA + "║" + AnsiFormatter.RESET);
        System.out.println(AnsiFormatter.BRIGHT_FG_MAGENTA + "╚════════════════════════════════════════════════╝" + AnsiFormatter.RESET);
        System.out.println();
        
        // Command usage with colors
        System.out.println(AnsiFormatter.BRIGHT_FG_WHITE + "Usage:" + AnsiFormatter.RESET + 
                           " rin msg [command] [options]");
        System.out.println();
        
        // Command section with nice formatting
        System.out.println(AnsiFormatter.BOLD + AnsiFormatter.BRIGHT_FG_YELLOW + "Messaging commands:" + AnsiFormatter.RESET);
        
        // Commands list with colors
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg" + AnsiFormatter.RESET +
                           "                       List all messages");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg unread" + AnsiFormatter.RESET +
                           "                List unread messages");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg login" + AnsiFormatter.RESET +
                           " <user> <pwd>    Login to the messaging service");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg project list" + AnsiFormatter.RESET +
                           "          List available projects");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg project switch" + AnsiFormatter.RESET +
                           " <proj> Switch to a project");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg read" + AnsiFormatter.RESET +
                           " <msg-id>         Read a specific message");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg delete" + AnsiFormatter.RESET +
                           " <msg-id>       Delete a message");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg reply" + AnsiFormatter.RESET +
                           " <id> <text>     Reply to a message");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg from" + AnsiFormatter.RESET +
                           " <username>       List messages from a specific user");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_GREEN + "rin msg" + AnsiFormatter.RESET +
                           " <user> <message>      Send message to user in current project");
        System.out.println();
        
        // Options section with ANSI formatting
        System.out.println(AnsiFormatter.BOLD + AnsiFormatter.BRIGHT_FG_YELLOW + "Options:" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_BLUE + "--unread" + AnsiFormatter.RESET +
                           "                      List only unread messages");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_BLUE + "--from=<username>" + AnsiFormatter.RESET +
                           "             Filter messages by sender");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_BLUE + "--project=<project>" + AnsiFormatter.RESET +
                           "           Filter messages by project");
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_BLUE + "-h, --help" + AnsiFormatter.RESET +
                           "                    Show this help message");
        System.out.println();
        
        // Message formatting section
        System.out.println(AnsiFormatter.BOLD + AnsiFormatter.BRIGHT_FG_YELLOW + "Message Formatting:" + AnsiFormatter.RESET);
        System.out.println("  Use " + AnsiFormatter.BRIGHT_FG_WHITE + "|COLOR|" + AnsiFormatter.RESET + 
                           "text" + AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET + 
                           " to add colors and effects to your messages");
        System.out.println();
        
        // Example formatting
        System.out.println(AnsiFormatter.BOLD + AnsiFormatter.BRIGHT_FG_YELLOW + "Examples:" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|RED|" + AnsiFormatter.RESET + 
                           AnsiFormatter.FG_RED + "Red text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|GREEN|" + AnsiFormatter.RESET + 
                           AnsiFormatter.FG_GREEN + "Green text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|BLUE|" + AnsiFormatter.RESET + 
                           AnsiFormatter.FG_BLUE + "Blue text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|CYAN|" + AnsiFormatter.RESET + 
                           AnsiFormatter.FG_CYAN + "Cyan text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|YELLOW|" + AnsiFormatter.RESET + 
                           AnsiFormatter.FG_YELLOW + "Yellow text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|MAGENTA|" + AnsiFormatter.RESET + 
                           AnsiFormatter.FG_MAGENTA + "Magenta text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|WHITE|" + AnsiFormatter.RESET + 
                           AnsiFormatter.FG_WHITE + "White text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|BOLD|" + AnsiFormatter.RESET + 
                           AnsiFormatter.BOLD + "Bold text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|UNDERLINE|" + AnsiFormatter.RESET + 
                           AnsiFormatter.UNDERLINE + "Underlined text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|BLINK|" + AnsiFormatter.RESET + 
                           AnsiFormatter.BLINK + "Blinking text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        System.out.println("  " + AnsiFormatter.BRIGHT_FG_WHITE + "|INVERSE|" + AnsiFormatter.RESET + 
                           AnsiFormatter.INVERSE + "Inverse text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
        
        // Combination examples
        System.out.println();
        System.out.println("  Combined: " + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|BOLD||RED|" + AnsiFormatter.RESET + 
                           AnsiFormatter.BOLD + AnsiFormatter.FG_RED + "Bold red text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET + 
                           " & " + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|BLINK||GREEN|" + AnsiFormatter.RESET + 
                           AnsiFormatter.BLINK + AnsiFormatter.FG_GREEN + "Blinking green text" + AnsiFormatter.RESET + 
                           AnsiFormatter.BRIGHT_FG_WHITE + "|" + AnsiFormatter.RESET);
    }
}