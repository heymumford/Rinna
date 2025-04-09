/*
 * MsgCommandComponentTest - Component tests for the MsgCommand
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.MsgCommand;
import org.rinna.cli.messaging.MessageService;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ProjectContext;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Component tests for MsgCommand, focusing on integration with
 * dependent services and operation tracking.
 */
@DisplayName("MsgCommand Component Tests")
public class MsgCommandComponentTest {

    private static final String TEST_USER = "test.user";
    private static final String TEST_AUTH_TOKEN = "valid-auth-token";
    private static final String TEST_PROJECT = "Tracer";
    private static final String TEST_PROJECT_KEY = "TRC";

    private AutoCloseable closeable;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @Mock
    private ServiceManager mockServiceManager;

    @Mock
    private MessageService mockMessageService;

    @Mock
    private ConfigurationService mockConfigService;

    @Mock
    private ProjectContext mockProjectContext;

    @Mock
    private MetadataService mockMetadataService;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));

        // Configure mocks
        when(mockServiceManager.getMessageService()).thenReturn(mockMessageService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getProjectContext()).thenReturn(mockProjectContext);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);

        // Default authentication state
        when(mockConfigService.isAuthenticated()).thenReturn(true);
        when(mockConfigService.getCurrentUser()).thenReturn(TEST_USER);
        when(mockConfigService.getAuthToken()).thenReturn(TEST_AUTH_TOKEN);
        when(mockMessageService.validateToken(TEST_AUTH_TOKEN)).thenReturn(true);

        // Default project context
        when(mockProjectContext.isProjectActive()).thenReturn(true);
        when(mockProjectContext.getCurrentProject()).thenReturn(TEST_PROJECT);
        when(mockProjectContext.getCurrentProjectKey()).thenReturn(TEST_PROJECT_KEY);
        when(mockProjectContext.isProjectMember(TEST_PROJECT, TEST_USER)).thenReturn(true);
        
        // Operation tracking mock
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("op-123");
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setErr(originalErr);
        closeable.close();
    }

    @Test
    @DisplayName("Should integrate with MessageService to list messages")
    void shouldIntegrateWithMessageServiceToListMessages() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // Given
            List<RinnaMessage> messages = createTestMessages(3);
            when(mockMessageService.getMessagesForUser(TEST_USER)).thenReturn(messages);

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode, "Command should succeed");
            verify(mockMessageService).getMessagesForUser(TEST_USER);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("msg"), eq("MESSAGING"), any());
            verify(mockMetadataService).startOperation(eq("message"), eq("LIST"), any());
            verify(mockMetadataService).completeOperation(anyString(), argThat(map -> 
                map.containsKey("user") && map.get("user").equals(TEST_USER) &&
                map.containsKey("count") && ((int)map.get("count")) == 3
            ));

            // Verify messages are marked as read
            verify(mockMessageService, times(3)).markMessageAsRead(anyString());
        }
    }

    @Test
    @DisplayName("Should integrate with MessageService to list unread messages")
    void shouldIntegrateWithMessageServiceToListUnreadMessages() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // Given
            List<RinnaMessage> unreadMessages = new ArrayList<>();
            unreadMessages.add(createTestMessage("msg-1", "sender1", TEST_USER, "Unread message", TEST_PROJECT, MessageStatus.UNREAD));
            when(mockMessageService.getUnreadMessagesForUser(TEST_USER)).thenReturn(unreadMessages);

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            command.setSubcommand("unread");
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode, "Command should succeed");
            verify(mockMessageService).getUnreadMessagesForUser(TEST_USER);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("message"), eq("LIST_UNREAD"), any());
            
            // Should NOT mark messages as read when viewing unread messages
            verify(mockMessageService, never()).markMessageAsRead(anyString());
        }
    }

    @Test
    @DisplayName("Should integrate with MessageService to read a specific message")
    void shouldIntegrateWithMessageServiceToReadSpecificMessage() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // Given
            String messageId = "msg-123";
            RinnaMessage message = createTestMessage(messageId, "sender1", TEST_USER, "Test message content", TEST_PROJECT, MessageStatus.UNREAD);
            when(mockMessageService.getMessage(messageId)).thenReturn(message);

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            command.setSubcommand("read");
            command.setArgs(new String[]{messageId});
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode, "Command should succeed");
            verify(mockMessageService).getMessage(messageId);
            verify(mockMessageService).markMessageAsRead(messageId);
            
            // Verify output contains message details
            String output = outputStream.toString();
            assertTrue(output.contains(messageId), "Output should contain message ID");
            assertTrue(output.contains("sender1"), "Output should contain sender");
            assertTrue(output.contains("Test message content"), "Output should contain message content");
        }
    }

    @Test
    @DisplayName("Should integrate with MessageService to delete a message")
    void shouldIntegrateWithMessageServiceToDeleteMessage() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // Given
            String messageId = "msg-123";
            RinnaMessage message = createTestMessage(messageId, "sender1", TEST_USER, "Test message content", TEST_PROJECT, MessageStatus.READ);
            when(mockMessageService.getMessage(messageId)).thenReturn(message);
            when(mockMessageService.deleteMessage(messageId, TEST_USER)).thenReturn(true);

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            command.setSubcommand("delete");
            command.setArgs(new String[]{messageId});
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode, "Command should succeed");
            verify(mockMessageService).deleteMessage(messageId, TEST_USER);
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("message"), eq("DELETE"), argThat(map -> 
                map.containsKey("message_id") && map.get("message_id").equals(messageId)
            ));
            verify(mockMetadataService).completeOperation(anyString(), argThat(map -> 
                map.containsKey("action") && map.get("action").equals("delete")
            ));
        }
    }

    @Test
    @DisplayName("Should integrate with MessageService to send a direct message")
    void shouldIntegrateWithMessageServiceToSendDirectMessage() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // Given
            String recipient = "team.member";
            String messageContent = "Direct message content";
            
            when(mockProjectContext.isProjectMember(TEST_PROJECT, recipient)).thenReturn(true);
            when(mockMessageService.sendMessage(any(RinnaMessage.class))).thenReturn(true);

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            command.setSubcommand(recipient);
            command.setArgs(new String[]{messageContent});
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode, "Command should succeed");
            
            // Capture and verify the message
            ArgumentCaptor<RinnaMessage> messageCaptor = ArgumentCaptor.forClass(RinnaMessage.class);
            verify(mockMessageService).sendMessage(messageCaptor.capture());
            
            RinnaMessage sentMessage = messageCaptor.getValue();
            assertEquals(TEST_USER, sentMessage.getSender(), "Sender should be current user");
            assertEquals(recipient, sentMessage.getRecipient(), "Recipient should match");
            assertEquals(messageContent, sentMessage.getContent(), "Content should match");
            assertEquals(TEST_PROJECT, sentMessage.getProject(), "Project should match current project");
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("message"), eq("SEND"), any());
            verify(mockMetadataService).completeOperation(anyString(), argThat(map -> 
                map.containsKey("recipient") && map.get("recipient").equals(recipient) &&
                map.containsKey("action") && map.get("action").equals("send")
            ));
        }
    }

    @Test
    @DisplayName("Should integrate with MessageService to reply to a message")
    void shouldIntegrateWithMessageServiceToReplyToMessage() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // Given
            String messageId = "msg-123";
            String sender = "original.sender";
            String replyContent = "Reply message content";
            
            RinnaMessage originalMessage = createTestMessage(messageId, sender, TEST_USER, "Original message", TEST_PROJECT, MessageStatus.READ);
            when(mockMessageService.getMessage(messageId)).thenReturn(originalMessage);
            when(mockMessageService.sendMessage(any(RinnaMessage.class))).thenReturn(true);

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            command.setSubcommand("reply");
            command.setArgs(new String[]{messageId, replyContent});
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode, "Command should succeed");
            
            // Capture and verify the reply
            ArgumentCaptor<RinnaMessage> messageCaptor = ArgumentCaptor.forClass(RinnaMessage.class);
            verify(mockMessageService).sendMessage(messageCaptor.capture());
            
            RinnaMessage sentReply = messageCaptor.getValue();
            assertEquals(TEST_USER, sentReply.getSender(), "Sender should be current user");
            assertEquals(sender, sentReply.getRecipient(), "Recipient should be original sender");
            assertEquals(replyContent, sentReply.getContent(), "Content should match");
            assertEquals(messageId, sentReply.getInReplyTo(), "Should reference original message");
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("message"), eq("REPLY"), any());
            verify(mockMetadataService).completeOperation(anyString(), argThat(map -> 
                map.containsKey("in_reply_to") && map.get("in_reply_to").equals(messageId) &&
                map.containsKey("action") && map.get("action").equals("reply")
            ));
        }
    }

    @Test
    @DisplayName("Should handle authentication failure gracefully")
    void shouldHandleAuthenticationFailureGracefully() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // Given
            when(mockConfigService.isAuthenticated()).thenReturn(false);

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            int exitCode = command.call();

            // Then
            assertEquals(1, exitCode, "Command should fail");
            String error = errorStream.toString();
            assertTrue(error.contains("Error: Authentication required"), 
                    "Error should mention authentication");
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("msg"), eq("MESSAGING"), any());
            verify(mockMetadataService).failOperation(anyString(), any(IllegalStateException.class));
        }
    }

    @Test
    @DisplayName("Should track failed operations properly")
    void shouldTrackFailedOperationsProperly() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // Given
            String messageId = "msg-123";
            when(mockMessageService.getMessage(messageId)).thenReturn(null); // Message not found

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            command.setSubcommand("read");
            command.setArgs(new String[]{messageId});
            int exitCode = command.call();

            // Then
            assertEquals(1, exitCode, "Command should fail");
            
            // Verify error message
            String error = errorStream.toString();
            assertTrue(error.contains("Error: Message with ID '" + messageId + "' not found"), 
                    "Error should mention message not found");
            
            // Verify operation tracking for failure
            verify(mockMetadataService).failOperation(anyString(), any(IllegalArgumentException.class));
        }
    }

    @Test
    @DisplayName("Should handle complex operation tracking with nested operations")
    void shouldHandleComplexOperationTrackingWithNestedOperations() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // Given - Different operation IDs for nested operations
            when(mockMetadataService.startOperation(eq("msg"), anyString(), any())).thenReturn("op-parent");
            when(mockMetadataService.startOperation(eq("message"), anyString(), any())).thenReturn("op-child");
            
            List<RinnaMessage> messages = createTestMessages(2);
            when(mockMessageService.getMessagesForUser(TEST_USER)).thenReturn(messages);

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode, "Command should succeed");
            
            // Verify parent operation
            verify(mockMetadataService).startOperation(eq("msg"), eq("MESSAGING"), any());
            verify(mockMetadataService, never()).completeOperation(eq("op-parent"), any()); // Parent should not be completed directly
            
            // Verify child operation
            verify(mockMetadataService).startOperation(eq("message"), eq("LIST"), any());
            verify(mockMetadataService).completeOperation(eq("op-child"), any()); // Child operation is completed
        }
    }

    @Test
    @DisplayName("Should track help operations without user actions")
    void shouldTrackHelpOperationsWithoutUserActions() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);

            // When
            MsgCommand command = new MsgCommand(mockServiceManager);
            command.setSubcommand("--help");
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode, "Command should succeed");
            
            // Verify operation tracking for help
            verify(mockMetadataService).startOperation(eq("msg"), eq("MESSAGING"), 
                argThat(map -> map.containsKey("subcommand") && map.get("subcommand").equals("--help")));
            verify(mockMetadataService).completeOperation(anyString(), argThat(map -> 
                map.containsKey("action") && map.get("action").equals("help")
            ));
            
            // No interaction with message service for help
            verify(mockMessageService, never()).getMessagesForUser(anyString());
        }
    }

    // Helper methods to create test messages
    private List<RinnaMessage> createTestMessages(int count) {
        List<RinnaMessage> messages = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String id = "msg-" + (i + 1);
            String sender = "sender" + (i + 1);
            String content = "Test message " + (i + 1);
            MessageStatus status = (i % 2 == 0) ? MessageStatus.READ : MessageStatus.UNREAD;
            
            messages.add(createTestMessage(id, sender, TEST_USER, content, TEST_PROJECT, status));
        }
        
        return messages;
    }
    
    private RinnaMessage createTestMessage(String id, String sender, String recipient, 
                                         String content, String project, MessageStatus status) {
        return new RinnaMessage(
            id,
            sender,
            recipient,
            content,
            project,
            Instant.now(),
            status
        );
    }
}