/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.cli.messaging.MessageService;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;

/**
 * Mock implementation of MessageService for testing.
 */
public class MockMessageService implements MessageService {
    
    private final Map<String, String> userPasswords;
    private final Map<String, String> userTokens;
    private final Map<String, List<String>> projectMembers;
    private final Map<String, List<RinnaMessage>> userMessages;
    private final Map<String, RinnaMessage> messagesById;
    
    /**
     * Constructs a new MockMessageService with some sample data.
     */
    public MockMessageService() {
        userPasswords = new ConcurrentHashMap<>();
        userTokens = new ConcurrentHashMap<>();
        projectMembers = new ConcurrentHashMap<>();
        userMessages = new ConcurrentHashMap<>();
        messagesById = new ConcurrentHashMap<>();
        
        // Add some test users
        addUser("eric", "password123", "auth-token-eric-123");
        addUser("steve", "password456", "auth-token-steve-456");
        addUser("maria", "password789", "auth-token-maria-789");
        
        // Add some test projects and members
        addProject("Tracer", List.of("eric", "steve"));
        addProject("Quantum", List.of("maria", "eric"));
        
        // Add some test messages
        addTestMessage("eric", "steve", "Can you review my PR for Tracer?", "Tracer");
        addTestMessage("steve", "eric", "I'll take a look at it this afternoon.", "Tracer");
        addTestMessage("maria", "eric", "Meeting at 3pm about Quantum?", "Quantum");
    }
    
    private void addUser(String username, String password, String token) {
        userPasswords.put(username, password);
        userTokens.put(username, token);
    }
    
    private void addProject(String projectName, List<String> members) {
        projectMembers.put(projectName, new ArrayList<>(members));
    }
    
    private void addTestMessage(String sender, String recipient, String content, String project) {
        RinnaMessage message = new RinnaMessage(
                "msg-" + UUID.randomUUID().toString().substring(0, 8),
                sender,
                recipient,
                content,
                project,
                Instant.now().minusSeconds((long) (Math.random() * 3600)),
                MessageStatus.UNREAD
        );
        
        // Add to user's message list
        userMessages.computeIfAbsent(recipient, k -> new ArrayList<>()).add(message);
        
        // Add to messages by ID map
        messagesById.put(message.getId(), message);
    }
    
    @Override
    public String authenticate(String username, String password) {
        String storedPassword = userPasswords.get(username);
        if (storedPassword != null && storedPassword.equals(password)) {
            return userTokens.get(username);
        }
        return null;
    }
    
    @Override
    public boolean validateToken(String token) {
        return userTokens.containsValue(token);
    }
    
    @Override
    public boolean canAccessProject(String username, String projectName) {
        List<String> members = projectMembers.get(projectName);
        return members != null && members.contains(username);
    }
    
    @Override
    public List<RinnaMessage> getMessagesForUser(String username) {
        return userMessages.getOrDefault(username, new ArrayList<>());
    }
    
    @Override
    public List<RinnaMessage> getUnreadMessagesForUser(String username) {
        List<RinnaMessage> messages = userMessages.getOrDefault(username, new ArrayList<>());
        return messages.stream()
                .filter(message -> message.getStatus() == MessageStatus.UNREAD)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RinnaMessage> getMessagesForUserBySender(String username, String sender) {
        List<RinnaMessage> messages = userMessages.getOrDefault(username, new ArrayList<>());
        return messages.stream()
                .filter(message -> message.getSender().equals(sender))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RinnaMessage> getMessagesForUserByProject(String username, String projectName) {
        List<RinnaMessage> messages = userMessages.getOrDefault(username, new ArrayList<>());
        return messages.stream()
                .filter(message -> message.getProject().equals(projectName))
                .collect(Collectors.toList());
    }
    
    @Override
    public RinnaMessage getMessage(String messageId) {
        return messagesById.get(messageId);
    }
    
    @Override
    public boolean sendMessage(RinnaMessage message) {
        // Add to user's message list
        userMessages.computeIfAbsent(message.getRecipient(), k -> new ArrayList<>()).add(message);
        
        // Add to messages by ID map
        messagesById.put(message.getId(), message);
        
        return true;
    }
    
    @Override
    public boolean markMessageAsRead(String messageId) {
        RinnaMessage message = messagesById.get(messageId);
        if (message != null) {
            message.setStatus(MessageStatus.READ);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean deleteMessage(String messageId, String username) {
        RinnaMessage message = messagesById.get(messageId);
        if (message != null && message.getRecipient().equals(username)) {
            List<RinnaMessage> userMsgs = userMessages.get(username);
            if (userMsgs != null) {
                userMsgs.remove(message);
            }
            messagesById.remove(messageId);
            return true;
        }
        return false;
    }
    
    @Override
    public Map<String, List<String>> getAvailableProjects(String username) {
        Map<String, List<String>> userProjects = new HashMap<>();
        
        // Return only projects that the user is a member of
        projectMembers.forEach((project, members) -> {
            if (members.contains(username)) {
                userProjects.put(project, new ArrayList<>(members));
            }
        });
        
        return userProjects;
    }
}