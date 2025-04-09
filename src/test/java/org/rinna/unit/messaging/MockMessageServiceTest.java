/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.unit.messaging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Execution;
import org.junit.jupiter.api.ExecutionMode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.rinna.base.UnitTest;
import org.rinna.cli.messaging.MessageService;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.service.MockMessageService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Unit tests for MockMessageService class.
 */
@Tag("unit")
@Tag("smoke")
@Execution(ExecutionMode.CONCURRENT)
class MockMessageServiceTest extends UnitTest {

    private MessageService messageService;
    
    @BeforeEach
    void setUp() {
        messageService = new MockMessageService();
    }
    
    /**
     * Test user authentication with valid credentials.
     */
    @Test
    void testAuthenticateValidCredentials() {
        // Test with predefined users from MockMessageService
        String authToken = messageService.authenticate("eric", "password123");
        Assertions.assertNotNull(authToken);
        Assertions.assertEquals("auth-token-eric-123", authToken);
    }
    
    /**
     * Test user authentication with invalid credentials.
     */
    @ParameterizedTest
    @CsvSource({
        "eric, wrongpassword",
        "nonexistent, password123",
        ", password123",
        "eric, "
    })
    void testAuthenticateInvalidCredentials(String username, String password) {
        String authToken = messageService.authenticate(username, password);
        Assertions.assertNull(authToken);
    }
    
    /**
     * Test token validation.
     */
    @Test
    void testValidateToken() {
        // Valid token
        Assertions.assertTrue(messageService.validateToken("auth-token-eric-123"));
        
        // Invalid token
        Assertions.assertFalse(messageService.validateToken("invalid-token"));
        Assertions.assertFalse(messageService.validateToken(null));
        Assertions.assertFalse(messageService.validateToken(""));
    }
    
    /**
     * Test project access check.
     */
    @Test
    void testCanAccessProject() {
        // Valid project membership
        Assertions.assertTrue(messageService.canAccessProject("eric", "Tracer"));
        Assertions.assertTrue(messageService.canAccessProject("steve", "Tracer"));
        Assertions.assertTrue(messageService.canAccessProject("maria", "Quantum"));
        
        // Invalid project membership
        Assertions.assertFalse(messageService.canAccessProject("steve", "Quantum"));
        Assertions.assertFalse(messageService.canAccessProject("nonexistent", "Tracer"));
        Assertions.assertFalse(messageService.canAccessProject("eric", "NonexistentProject"));
    }
    
    /**
     * Test getting messages for a user.
     */
    @Test
    void testGetMessagesForUser() {
        // Test with predefined messages
        List<RinnaMessage> ericMessages = messageService.getMessagesForUser("eric");
        List<RinnaMessage> steveMessages = messageService.getMessagesForUser("steve");
        List<RinnaMessage> nonexistentMessages = messageService.getMessagesForUser("nonexistent");
        
        // Eric should have some messages
        Assertions.assertFalse(ericMessages.isEmpty());
        
        // Steve should have some messages
        Assertions.assertFalse(steveMessages.isEmpty());
        
        // Nonexistent user should have no messages
        Assertions.assertTrue(nonexistentMessages.isEmpty());
    }
    
    /**
     * Test getting unread messages for a user.
     */
    @Test
    void testGetUnreadMessagesForUser() {
        // Initially, all messages are unread in MockMessageService
        List<RinnaMessage> ericUnreadMessages = messageService.getUnreadMessagesForUser("eric");
        
        // Mark all as read
        for (RinnaMessage message : ericUnreadMessages) {
            messageService.markMessageAsRead(message.getId());
        }
        
        // Now there should be no unread messages
        List<RinnaMessage> ericUnreadMessagesAfter = messageService.getUnreadMessagesForUser("eric");
        Assertions.assertTrue(ericUnreadMessagesAfter.isEmpty());
    }
    
    /**
     * Test filtering messages by sender.
     */
    @Test
    void testGetMessagesForUserBySender() {
        // Test initial state
        List<RinnaMessage> messagesFromSteve = messageService.getMessagesForUserBySender("eric", "steve");
        Assertions.assertFalse(messagesFromSteve.isEmpty());
        
        // Verify sender
        for (RinnaMessage message : messagesFromSteve) {
            Assertions.assertEquals("steve", message.getSender());
        }
        
        // Test with nonexistent sender
        List<RinnaMessage> messagesFromNonexistent = messageService.getMessagesForUserBySender("eric", "nonexistent");
        Assertions.assertTrue(messagesFromNonexistent.isEmpty());
    }
    
    /**
     * Test filtering messages by project.
     */
    @Test
    void testGetMessagesForUserByProject() {
        // Test initial state
        List<RinnaMessage> tracerMessages = messageService.getMessagesForUserByProject("eric", "Tracer");
        Assertions.assertFalse(tracerMessages.isEmpty());
        
        // Verify project
        for (RinnaMessage message : tracerMessages) {
            Assertions.assertEquals("Tracer", message.getProject());
        }
        
        // Test with nonexistent project
        List<RinnaMessage> nonexistentProjectMessages = messageService.getMessagesForUserByProject("eric", "NonexistentProject");
        Assertions.assertTrue(nonexistentProjectMessages.isEmpty());
    }
    
    /**
     * Test getting a specific message.
     */
    @Test
    void testGetMessage() {
        // Get all messages for a user
        List<RinnaMessage> ericMessages = messageService.getMessagesForUser("eric");
        Assertions.assertFalse(ericMessages.isEmpty());
        
        // Get a specific message
        RinnaMessage firstMessage = ericMessages.get(0);
        RinnaMessage retrievedMessage = messageService.getMessage(firstMessage.getId());
        
        Assertions.assertNotNull(retrievedMessage);
        Assertions.assertEquals(firstMessage.getId(), retrievedMessage.getId());
        Assertions.assertEquals(firstMessage.getSender(), retrievedMessage.getSender());
        Assertions.assertEquals(firstMessage.getContent(), retrievedMessage.getContent());
        
        // Test with nonexistent message ID
        RinnaMessage nonexistentMessage = messageService.getMessage("nonexistent-id");
        Assertions.assertNull(nonexistentMessage);
    }
    
    /**
     * Test sending a message.
     */
    @Test
    void testSendMessage() {
        // Create a new message
        RinnaMessage newMessage = new RinnaMessage(
            "msg-test-" + UUID.randomUUID().toString().substring(0, 8),
            "eric",
            "maria",
            "Test message content",
            "Quantum",
            Instant.now(),
            MessageStatus.UNREAD
        );
        
        // Send the message
        boolean sent = messageService.sendMessage(newMessage);
        Assertions.assertTrue(sent);
        
        // Verify the message was added
        RinnaMessage retrievedMessage = messageService.getMessage(newMessage.getId());
        Assertions.assertNotNull(retrievedMessage);
        Assertions.assertEquals(newMessage.getId(), retrievedMessage.getId());
        Assertions.assertEquals(newMessage.getContent(), retrievedMessage.getContent());
        
        // Verify it appears in the recipient's messages
        List<RinnaMessage> mariaMessages = messageService.getMessagesForUser("maria");
        boolean found = mariaMessages.stream()
                .anyMatch(m -> m.getId().equals(newMessage.getId()));
        Assertions.assertTrue(found);
    }
    
    /**
     * Test marking a message as read.
     */
    @Test
    void testMarkMessageAsRead() {
        // Get all messages for a user
        List<RinnaMessage> ericMessages = messageService.getMessagesForUser("eric");
        Assertions.assertFalse(ericMessages.isEmpty());
        
        // Get a specific unread message
        RinnaMessage unreadMessage = ericMessages.stream()
                .filter(m -> m.getStatus() == MessageStatus.UNREAD)
                .findFirst()
                .orElse(null);
        
        if (unreadMessage != null) {
            // Mark the message as read
            boolean marked = messageService.markMessageAsRead(unreadMessage.getId());
            Assertions.assertTrue(marked);
            
            // Verify the message status was updated
            RinnaMessage retrievedMessage = messageService.getMessage(unreadMessage.getId());
            Assertions.assertNotNull(retrievedMessage);
            Assertions.assertEquals(MessageStatus.READ, retrievedMessage.getStatus());
        }
        
        // Test with nonexistent message ID
        boolean markedNonexistent = messageService.markMessageAsRead("nonexistent-id");
        Assertions.assertFalse(markedNonexistent);
    }
    
    /**
     * Test deleting a message.
     */
    @Test
    void testDeleteMessage() {
        // Create and send a new message
        RinnaMessage newMessage = new RinnaMessage(
            "msg-test-delete-" + UUID.randomUUID().toString().substring(0, 8),
            "eric",
            "maria",
            "Message to be deleted",
            "Quantum",
            Instant.now(),
            MessageStatus.UNREAD
        );
        messageService.sendMessage(newMessage);
        
        // Verify the message exists
        RinnaMessage retrievedMessage = messageService.getMessage(newMessage.getId());
        Assertions.assertNotNull(retrievedMessage);
        
        // Delete the message
        boolean deleted = messageService.deleteMessage(newMessage.getId(), "maria");
        Assertions.assertTrue(deleted);
        
        // Verify the message was deleted
        RinnaMessage deletedMessage = messageService.getMessage(newMessage.getId());
        Assertions.assertNull(deletedMessage);
        
        // Verify it's not in the recipient's messages anymore
        List<RinnaMessage> mariaMessages = messageService.getMessagesForUser("maria");
        boolean stillExists = mariaMessages.stream()
                .anyMatch(m -> m.getId().equals(newMessage.getId()));
        Assertions.assertFalse(stillExists);
    }
    
    /**
     * Test deleting a message with invalid permissions.
     */
    @Test
    void testDeleteMessageInvalidPermissions() {
        // Create and send a new message
        RinnaMessage newMessage = new RinnaMessage(
            "msg-test-delete-invalid-" + UUID.randomUUID().toString().substring(0, 8),
            "eric",
            "maria",
            "Message that shouldn't be deleted by wrong user",
            "Quantum",
            Instant.now(),
            MessageStatus.UNREAD
        );
        messageService.sendMessage(newMessage);
        
        // Try to delete as the wrong user (not the recipient)
        boolean deleted = messageService.deleteMessage(newMessage.getId(), "steve");
        Assertions.assertFalse(deleted);
        
        // Verify the message still exists
        RinnaMessage retrievedMessage = messageService.getMessage(newMessage.getId());
        Assertions.assertNotNull(retrievedMessage);
    }
}