/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.component.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Execution;
import org.junit.jupiter.api.ExecutionMode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.rinna.base.ComponentTest;
import org.rinna.cli.command.MsgCommand;
import org.rinna.cli.messaging.MessageService;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MockMessageService;
import org.rinna.cli.service.ProjectContext;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Component tests for MsgCommand class.
 */
@Tag("component")
@Tag("smoke")
@Execution(ExecutionMode.CONCURRENT)
class MsgCommandComponentTest extends ComponentTest {

    private MsgCommand msgCommand;
    
    @Mock
    private ConfigurationService configServiceMock;
    
    @Mock
    private ProjectContext projectContextMock;
    
    private MessageService messageService;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private AutoCloseable mocks;
    
    @BeforeEach
    void setUp() {
        // Set up output capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mocks
        mocks = MockitoAnnotations.openMocks(this);
        
        // Use real message service for better component testing
        messageService = new MockMessageService();
        
        // Override ServiceManager.getInstance() to return our mock
        ServiceManager serviceManager = Mockito.mock(ServiceManager.class);
        Mockito.when(serviceManager.getConfigurationService()).thenReturn(configServiceMock);
        Mockito.when(serviceManager.getProjectContext()).thenReturn(projectContextMock);
        Mockito.when(serviceManager.getMessageService()).thenReturn(messageService);
        
        // Use reflection to set the serviceManager singleton
        try {
            java.lang.reflect.Field instance = ServiceManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, serviceManager);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock ServiceManager", e);
        }
        
        // Create command under test
        msgCommand = new MsgCommand();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setErr(originalErr);
        mocks.close();
        
        // Reset ServiceManager singleton
        try {
            java.lang.reflect.Field instance = ServiceManager.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset ServiceManager", e);
        }
    }
    
    /**
     * Test user login with valid credentials.
     */
    @Test
    void testLoginWithValidCredentials() {
        // Configure authentication to succeed
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(false);
        
        // Set up command arguments
        msgCommand.setSubcommand("login");
        msgCommand.setArgs(new String[]{"eric", "password123"});
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(0, result);
        Assertions.assertTrue(outputCaptor.toString().contains("Authentication successful"));
        
        // Verify configuration was updated
        Mockito.verify(configServiceMock).setCurrentUser("eric");
        Mockito.verify(configServiceMock).setAuthToken("auth-token-eric-123");
    }
    
    /**
     * Test user login with invalid credentials.
     */
    @Test
    void testLoginWithInvalidCredentials() {
        // Configure authentication to fail
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(false);
        
        // Set up command arguments
        msgCommand.setSubcommand("login");
        msgCommand.setArgs(new String[]{"eric", "wrongpassword"});
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(1, result);
        Assertions.assertTrue(errorCaptor.toString().contains("Authentication failed"));
        
        // Verify configuration was not updated
        Mockito.verify(configServiceMock, Mockito.never()).setCurrentUser(Mockito.anyString());
        Mockito.verify(configServiceMock, Mockito.never()).setAuthToken(Mockito.anyString());
    }
    
    /**
     * Test listing messages when authenticated.
     */
    @Test
    void testListMessagesWhenAuthenticated() {
        // Configure authentication
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(true);
        Mockito.when(configServiceMock.getCurrentUser()).thenReturn("eric");
        Mockito.when(configServiceMock.getAuthToken()).thenReturn("auth-token-eric-123");
        
        // Execute command with no subcommand (which should list messages)
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(0, result);
        Assertions.assertTrue(outputCaptor.toString().contains("Messages for eric"));
    }
    
    /**
     * Test listing messages when not authenticated.
     */
    @Test
    void testListMessagesWhenNotAuthenticated() {
        // Configure authentication to fail
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(false);
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(1, result);
        Assertions.assertTrue(errorCaptor.toString().contains("Authentication required"));
    }
    
    /**
     * Test listing unread messages.
     */
    @Test
    void testListUnreadMessages() {
        // Configure authentication
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(true);
        Mockito.when(configServiceMock.getCurrentUser()).thenReturn("eric");
        Mockito.when(configServiceMock.getAuthToken()).thenReturn("auth-token-eric-123");
        
        // Set up command arguments
        msgCommand.setSubcommand("unread");
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(0, result);
        Assertions.assertTrue(outputCaptor.toString().contains("Unread messages for eric"));
    }
    
    /**
     * Test switching to a project.
     */
    @Test
    void testSwitchToProject() {
        // Configure authentication
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(true);
        Mockito.when(configServiceMock.getCurrentUser()).thenReturn("eric");
        Mockito.when(configServiceMock.getAuthToken()).thenReturn("auth-token-eric-123");
        
        // Set up command arguments
        msgCommand.setSubcommand("project");
        msgCommand.setArgs(new String[]{"switch", "Tracer"});
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(0, result);
        Assertions.assertTrue(outputCaptor.toString().contains("Switched to project: Tracer"));
        
        // Verify project context was updated
        Mockito.verify(projectContextMock).setCurrentProject("Tracer");
    }
    
    /**
     * Test sending a message.
     */
    @Test
    void testSendMessage() {
        // Configure authentication
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(true);
        Mockito.when(configServiceMock.getCurrentUser()).thenReturn("eric");
        Mockito.when(configServiceMock.getAuthToken()).thenReturn("auth-token-eric-123");
        
        // Configure project context
        Mockito.when(projectContextMock.isProjectActive()).thenReturn(true);
        Mockito.when(projectContextMock.getCurrentProject()).thenReturn("Tracer");
        Mockito.when(projectContextMock.isProjectMember("Tracer", "steve")).thenReturn(true);
        
        // Set up command arguments
        msgCommand.setSubcommand("steve");
        msgCommand.setArgs(new String[]{"This is a test message"});
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(0, result);
        Assertions.assertTrue(outputCaptor.toString().contains("Message sent to steve"));
    }
    
    /**
     * Test sending a message without active project.
     */
    @Test
    void testSendMessageWithoutActiveProject() {
        // Configure authentication
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(true);
        Mockito.when(configServiceMock.getCurrentUser()).thenReturn("eric");
        Mockito.when(configServiceMock.getAuthToken()).thenReturn("auth-token-eric-123");
        
        // Configure project context
        Mockito.when(projectContextMock.isProjectActive()).thenReturn(false);
        
        // Set up command arguments
        msgCommand.setSubcommand("steve");
        msgCommand.setArgs(new String[]{"This is a test message"});
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(1, result);
        Assertions.assertTrue(errorCaptor.toString().contains("No active project"));
    }
    
    /**
     * Test reading a specific message.
     */
    @Test
    void testReadMessage() {
        // Configure authentication
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(true);
        Mockito.when(configServiceMock.getCurrentUser()).thenReturn("eric");
        Mockito.when(configServiceMock.getAuthToken()).thenReturn("auth-token-eric-123");
        
        // Create a test message
        RinnaMessage testMessage = new RinnaMessage(
            "test-message-id",
            "steve",
            "eric",
            "Test message content",
            "Tracer",
            Instant.now(),
            MessageStatus.UNREAD
        );
        messageService.sendMessage(testMessage);
        
        // Set up command arguments
        msgCommand.setSubcommand("read");
        msgCommand.setArgs(new String[]{"test-message-id"});
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(0, result);
        Assertions.assertTrue(outputCaptor.toString().contains("Test message content"));
    }
    
    /**
     * Test reading a nonexistent message.
     */
    @Test
    void testReadNonexistentMessage() {
        // Configure authentication
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(true);
        Mockito.when(configServiceMock.getCurrentUser()).thenReturn("eric");
        Mockito.when(configServiceMock.getAuthToken()).thenReturn("auth-token-eric-123");
        
        // Set up command arguments
        msgCommand.setSubcommand("read");
        msgCommand.setArgs(new String[]{"nonexistent-message-id"});
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(1, result);
        Assertions.assertTrue(errorCaptor.toString().contains("not found"));
    }
    
    /**
     * Test deleting a message.
     */
    @Test
    void testDeleteMessage() {
        // Configure authentication
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(true);
        Mockito.when(configServiceMock.getCurrentUser()).thenReturn("eric");
        Mockito.when(configServiceMock.getAuthToken()).thenReturn("auth-token-eric-123");
        
        // Create a test message
        RinnaMessage testMessage = new RinnaMessage(
            "test-delete-id",
            "steve",
            "eric",
            "Message to delete",
            "Tracer",
            Instant.now(),
            MessageStatus.UNREAD
        );
        messageService.sendMessage(testMessage);
        
        // Set up command arguments
        msgCommand.setSubcommand("delete");
        msgCommand.setArgs(new String[]{"test-delete-id"});
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(0, result);
        Assertions.assertTrue(outputCaptor.toString().contains("Message deleted"));
        
        // Verify the message was deleted
        RinnaMessage deletedMessage = messageService.getMessage("test-delete-id");
        Assertions.assertNull(deletedMessage);
    }
    
    /**
     * Test replying to a message.
     */
    @Test
    void testReplyToMessage() {
        // Configure authentication
        Mockito.when(configServiceMock.isAuthenticated()).thenReturn(true);
        Mockito.when(configServiceMock.getCurrentUser()).thenReturn("eric");
        Mockito.when(configServiceMock.getAuthToken()).thenReturn("auth-token-eric-123");
        
        // Create a test message
        RinnaMessage testMessage = new RinnaMessage(
            "test-reply-id",
            "steve",
            "eric",
            "Message to reply to",
            "Tracer",
            Instant.now(),
            MessageStatus.UNREAD
        );
        messageService.sendMessage(testMessage);
        
        // Set up command arguments
        msgCommand.setSubcommand("reply");
        msgCommand.setArgs(new String[]{"test-reply-id", "This", "is", "a", "reply"});
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(0, result);
        Assertions.assertTrue(outputCaptor.toString().contains("Reply sent to steve"));
        
        // Get messages for steve and verify reply
        List<RinnaMessage> steveMessages = messageService.getMessagesForUser("steve");
        boolean foundReply = steveMessages.stream()
                .anyMatch(msg -> msg.getContent().equals("This is a reply") && msg.getSender().equals("eric"));
        Assertions.assertTrue(foundReply);
    }
    
    /**
     * Test help command.
     */
    @Test
    void testHelpCommand() {
        // Set up command arguments
        msgCommand.setSubcommand("--help");
        
        // Execute command
        int result = msgCommand.call();
        
        // Verify result
        Assertions.assertEquals(0, result);
        Assertions.assertTrue(outputCaptor.toString().contains("MESSAGING SYSTEM"));
        Assertions.assertTrue(outputCaptor.toString().contains("Usage:"));
        Assertions.assertTrue(outputCaptor.toString().contains("Commands:"));
    }
}