/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.messaging;

import java.util.List;

/**
 * Service interface for message handling operations.
 */
public interface MessageService {
    
    /**
     * Authenticates a user and returns an authentication token.
     *
     * @param username the username
     * @param password the password
     * @return the authentication token or null if authentication fails
     */
    String authenticate(String username, String password);
    
    /**
     * Validates an authentication token.
     *
     * @param token the token to validate
     * @return true if the token is valid, false otherwise
     */
    boolean validateToken(String token);
    
    /**
     * Checks if a user can access a specific project.
     *
     * @param username    the username
     * @param projectName the project name
     * @return true if the user can access the project, false otherwise
     */
    boolean canAccessProject(String username, String projectName);
    
    /**
     * Gets all messages for a user.
     *
     * @param username the username
     * @return list of all messages for the user
     */
    List<RinnaMessage> getMessagesForUser(String username);
    
    /**
     * Gets only unread messages for a user.
     *
     * @param username the username
     * @return list of unread messages for the user
     */
    List<RinnaMessage> getUnreadMessagesForUser(String username);
    
    /**
     * Gets messages for a user filtered by sender.
     *
     * @param username the recipient username
     * @param sender   the sender username
     * @return list of messages from the specified sender
     */
    List<RinnaMessage> getMessagesForUserBySender(String username, String sender);
    
    /**
     * Gets messages for a user filtered by project.
     *
     * @param username    the username
     * @param projectName the project name
     * @return list of messages related to the specified project
     */
    List<RinnaMessage> getMessagesForUserByProject(String username, String projectName);
    
    /**
     * Gets a specific message by ID.
     *
     * @param messageId the message ID
     * @return the message or null if not found
     */
    RinnaMessage getMessage(String messageId);
    
    /**
     * Sends a message.
     *
     * @param message the message to send
     * @return true if the message was sent successfully, false otherwise
     */
    boolean sendMessage(RinnaMessage message);
    
    /**
     * Marks a message as read.
     *
     * @param messageId the message ID
     * @return true if the operation was successful, false otherwise
     */
    boolean markMessageAsRead(String messageId);
    
    /**
     * Deletes a message.
     *
     * @param messageId the message ID
     * @param username  the username of the user deleting the message
     * @return true if the operation was successful, false otherwise
     */
    boolean deleteMessage(String messageId, String username);
    
    /**
     * Gets the available projects for a user.
     *
     * @param username the username
     * @return a map of project names to lists of members
     */
    java.util.Map<String, List<String>> getAvailableProjects(String username);
}