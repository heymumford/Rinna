/*
 * MsgCommandTest - Tests for the MsgCommand class
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.command.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.MsgCommand;
import org.rinna.cli.messaging.MessageService;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.DefaultConfigurationService;
import org.rinna.cli.service.MockMessageService;
import org.rinna.cli.service.ProjectContext;
import org.rinna.cli.service.ServiceManager;

/**
 * Comprehensive test class for the MsgCommand functionality.
 * Tests all aspects of the command following TDD principles.
 */
@DisplayName("MsgCommand Tests")
class MsgCommandTest {

    private static final String TEST_USER = "test.user";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_AUTH_TOKEN = "valid-auth-token";
    private static final String TEST_MESSAGE_ID = "msg-12345678";
    private static final String TEST_PROJECT = "Tracer";
    private static final String TEST_PROJECT_KEY = "TRC";
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private InputStream originalIn;
    
    private ServiceManager mockServiceManager;
    private MessageService mockMessageService;
    private DefaultConfigurationService mockConfigService;
    private ProjectContext mockProjectContext;
    
    private MsgCommand command;
    
    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        // Set up System.out/err capture
        originalOut = System.out;
        originalErr = System.err;
        originalIn = System.in;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Create mocks
        mockServiceManager = Mockito.mock(ServiceManager.class);
        mockMessageService = Mockito.mock(MessageService.class);
        mockConfigService = Mockito.mock(DefaultConfigurationService.class);
        mockProjectContext = Mockito.mock(ProjectContext.class);
        
        // Configure mocks
        Mockito.when(mockServiceManager.getMessageService()).thenReturn(mockMessageService);
        Mockito.when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        Mockito.when(mockServiceManager.getProjectContext()).thenReturn(mockProjectContext);
        
        // Default authenticated state
        Mockito.when(mockConfigService.isAuthenticated()).thenReturn(true);
        Mockito.when(mockConfigService.getCurrentUser()).thenReturn(TEST_USER);
        Mockito.when(mockConfigService.getAuthToken()).thenReturn(TEST_AUTH_TOKEN);
        Mockito.when(mockMessageService.validateToken(TEST_AUTH_TOKEN)).thenReturn(true);
        
        // Default project context
        Mockito.when(mockProjectContext.isProjectActive()).thenReturn(true);
        Mockito.when(mockProjectContext.getCurrentProject()).thenReturn(TEST_PROJECT);
        Mockito.when(mockProjectContext.getCurrentProjectKey()).thenReturn(TEST_PROJECT_KEY);
        Mockito.when(mockProjectContext.isProjectMember(TEST_PROJECT, TEST_USER)).thenReturn(true);
        
        // Create command instance for testing
        command = new MsgCommand();
    }
    
    /**
     * Tears down the test environment after each test.
     */
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }
    
    /**
     * Helper method to set up input for interactive tests.
     *
     * @param input the input string
     */
    private void setInput(String input) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);
    }
    
    /**
     * Helper method to create a test message for use in tests.
     */
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

    /**
     * Tests for the help documentation of the MsgCommand.
     */
    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        @Test
        @DisplayName("Should display help information for help operation")
        void shouldDisplayHelpInformationForHelpOperation() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("--help");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("RINNA CLI MESSAGING SYSTEM"), 
                        "Help message should contain command name");
                assertTrue(outContent.toString().contains("Usage:"), 
                        "Help message should contain usage section");
                assertTrue(outContent.toString().contains("Messaging commands:"), 
                        "Help message should contain commands section");
                assertTrue(outContent.toString().contains("Message Formatting:"), 
                        "Help message should contain formatting section");
            }
        }
        
        @Test
        @DisplayName("Should display help information for -h operation")
        void shouldDisplayHelpInformationForHOperation() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("-h");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("RINNA CLI MESSAGING SYSTEM"), 
                        "Help message should contain command name");
            }
        }
        
        @Test
        @DisplayName("Should display error for unknown option")
        void shouldDisplayErrorForUnknownOption() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("--unknown-option");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Unknown option"), 
                        "Should show error message for unknown option");
                assertTrue(outContent.toString().contains("RINNA CLI MESSAGING SYSTEM"), 
                        "Should display help after error");
            }
        }
        
        @Test
        @DisplayName("Should show error for missing project subcommand")
        void shouldShowErrorForMissingProjectSubcommand() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("project");
                command.setArgs(new String[0]);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Project subcommand required"), 
                        "Should show error message for missing project subcommand");
                assertTrue(errContent.toString().contains("Available subcommands: list, switch"), 
                        "Should show available project subcommands");
            }
        }
    }

    /**
     * Tests for the positive scenarios of the MsgCommand.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should list messages when no subcommand specified")
        void shouldListMessagesWhenNoSubcommandSpecified() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                List<RinnaMessage> testMessages = new ArrayList<>();
                testMessages.add(createTestMessage("msg-1", "sender1", TEST_USER, "Test message 1", TEST_PROJECT, MessageStatus.READ));
                testMessages.add(createTestMessage("msg-2", "sender2", TEST_USER, "Test message 2", TEST_PROJECT, MessageStatus.UNREAD));
                
                Mockito.when(mockMessageService.getMessagesForUser(TEST_USER)).thenReturn(testMessages);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Messages for " + TEST_USER), 
                        "Should display messages header");
                assertTrue(outContent.toString().contains("Test message 1"), 
                        "Should display first message content");
                assertTrue(outContent.toString().contains("Test message 2"), 
                        "Should display second message content");
                verify(mockMessageService).markMessageAsRead("msg-1");
                verify(mockMessageService).markMessageAsRead("msg-2");
            }
        }
        
        @Test
        @DisplayName("Should show empty messages list")
        void shouldShowEmptyMessagesList() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                Mockito.when(mockMessageService.getMessagesForUser(TEST_USER))
                      .thenReturn(Collections.emptyList());
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("No messages found."), 
                        "Should show empty messages message");
            }
        }
        
        @Test
        @DisplayName("Should list unread messages successfully")
        void shouldListUnreadMessagesSuccessfully() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("unread");
                
                List<RinnaMessage> unreadMessages = new ArrayList<>();
                unreadMessages.add(createTestMessage("msg-2", "sender2", TEST_USER, "Unread message", TEST_PROJECT, MessageStatus.UNREAD));
                
                Mockito.when(mockMessageService.getUnreadMessagesForUser(TEST_USER))
                      .thenReturn(unreadMessages);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Unread messages for " + TEST_USER), 
                        "Should display unread messages header");
                assertTrue(outContent.toString().contains("Unread message"), 
                        "Should display unread message content");
                // Should not mark as read when listing unread messages
                verify(mockMessageService, never()).markMessageAsRead(anyString());
            }
        }
        
        @Test
        @DisplayName("Should list unread messages with --unread flag")
        void shouldListUnreadMessagesWithUnreadFlag() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("--unread");
                
                List<RinnaMessage> unreadMessages = new ArrayList<>();
                unreadMessages.add(createTestMessage("msg-2", "sender2", TEST_USER, "Unread message", TEST_PROJECT, MessageStatus.UNREAD));
                
                Mockito.when(mockMessageService.getUnreadMessagesForUser(TEST_USER))
                      .thenReturn(unreadMessages);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Unread messages for " + TEST_USER), 
                        "Should display unread messages header");
            }
        }
        
        @Test
        @DisplayName("Should log in successfully")
        void shouldLogInSuccessfully() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("login");
                command.setArgs(new String[]{TEST_USER, TEST_PASSWORD});
                
                Mockito.when(mockMessageService.authenticate(TEST_USER, TEST_PASSWORD))
                      .thenReturn(TEST_AUTH_TOKEN);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Authentication successful"), 
                        "Should display successful authentication message");
                verify(mockConfigService).setCurrentUser(TEST_USER);
                verify(mockConfigService).setAuthToken(TEST_AUTH_TOKEN);
            }
        }
        
        @Test
        @DisplayName("Should list projects successfully")
        void shouldListProjectsSuccessfully() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("project");
                command.setArgs(new String[]{"list"});
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Projects:"), 
                        "Should display projects header");
                assertTrue(outContent.toString().contains("Tracer (TRC)"), 
                        "Should display Tracer project");
                assertTrue(outContent.toString().contains("Quantum (QTM)"), 
                        "Should display Quantum project");
            }
        }
        
        @Test
        @DisplayName("Should switch projects successfully")
        void shouldSwitchProjectsSuccessfully() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("project");
                command.setArgs(new String[]{"switch", "Quantum"});
                
                Mockito.when(mockMessageService.canAccessProject(TEST_USER, "Quantum"))
                      .thenReturn(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Switched to project: Quantum"), 
                        "Should display project switch message");
                verify(mockProjectContext).setCurrentProject("Quantum");
                verify(mockProjectContext).setCurrentProjectKey("QTM");
            }
        }
        
        @Test
        @DisplayName("Should read message successfully")
        void shouldReadMessageSuccessfully() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("read");
                command.setArgs(new String[]{TEST_MESSAGE_ID});
                
                RinnaMessage testMessage = createTestMessage(
                    TEST_MESSAGE_ID, "sender1", TEST_USER, "Test message content", TEST_PROJECT, MessageStatus.UNREAD
                );
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(testMessage);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Message " + TEST_MESSAGE_ID), 
                        "Should display message ID");
                assertTrue(outContent.toString().contains("From: sender1"), 
                        "Should display sender");
                assertTrue(outContent.toString().contains("Test message content"), 
                        "Should display message content");
                verify(mockMessageService).markMessageAsRead(TEST_MESSAGE_ID);
            }
        }
        
        @Test
        @DisplayName("Should delete message successfully")
        void shouldDeleteMessageSuccessfully() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("delete");
                command.setArgs(new String[]{TEST_MESSAGE_ID});
                
                RinnaMessage testMessage = createTestMessage(
                    TEST_MESSAGE_ID, "sender1", TEST_USER, "Test message content", TEST_PROJECT, MessageStatus.READ
                );
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(testMessage);
                
                Mockito.when(mockMessageService.deleteMessage(TEST_MESSAGE_ID, TEST_USER))
                      .thenReturn(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Message deleted"), 
                        "Should display message deleted confirmation");
                verify(mockMessageService).deleteMessage(TEST_MESSAGE_ID, TEST_USER);
            }
        }
        
        @Test
        @DisplayName("Should reply to message successfully")
        void shouldReplyToMessageSuccessfully() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("reply");
                command.setArgs(new String[]{TEST_MESSAGE_ID, "This", "is", "a", "reply"});
                
                RinnaMessage testMessage = createTestMessage(
                    TEST_MESSAGE_ID, "sender1", TEST_USER, "Original message", TEST_PROJECT, MessageStatus.READ
                );
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(testMessage);
                
                Mockito.when(mockMessageService.sendMessage(any(RinnaMessage.class)))
                      .thenReturn(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Reply sent to sender1"), 
                        "Should display reply sent confirmation");
                verify(mockMessageService).sendMessage(argThat(message -> 
                    message.getRecipient().equals("sender1") &&
                    message.getSender().equals(TEST_USER) &&
                    message.getContent().equals("This is a reply") &&
                    message.getInReplyTo().equals(TEST_MESSAGE_ID)
                ));
            }
        }
        
        @Test
        @DisplayName("Should list messages from specific sender")
        void shouldListMessagesFromSpecificSender() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("from");
                command.setArgs(new String[]{"sender1"});
                
                List<RinnaMessage> senderMessages = new ArrayList<>();
                senderMessages.add(createTestMessage("msg-1", "sender1", TEST_USER, "Message from sender1", TEST_PROJECT, MessageStatus.READ));
                
                Mockito.when(mockMessageService.getMessagesForUserBySender(TEST_USER, "sender1"))
                      .thenReturn(senderMessages);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Messages from sender1:"), 
                        "Should display messages from sender header");
                assertTrue(outContent.toString().contains("Message from sender1"), 
                        "Should display message content");
                verify(mockMessageService).markMessageAsRead("msg-1");
            }
        }
        
        @Test
        @DisplayName("Should list messages from specific sender with --from flag")
        void shouldListMessagesFromSpecificSenderWithFromFlag() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("--from=sender1");
                
                List<RinnaMessage> senderMessages = new ArrayList<>();
                senderMessages.add(createTestMessage("msg-1", "sender1", TEST_USER, "Message from sender1", TEST_PROJECT, MessageStatus.READ));
                
                Mockito.when(mockMessageService.getMessagesForUserBySender(TEST_USER, "sender1"))
                      .thenReturn(senderMessages);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Messages from sender1:"), 
                        "Should display messages from sender header");
            }
        }
        
        @Test
        @DisplayName("Should list messages from specific project with --project flag")
        void shouldListMessagesFromSpecificProjectWithProjectFlag() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("--project=Tracer");
                
                List<RinnaMessage> projectMessages = new ArrayList<>();
                projectMessages.add(createTestMessage("msg-1", "sender1", TEST_USER, "Project message", "Tracer", MessageStatus.READ));
                
                Mockito.when(mockMessageService.getMessagesForUserByProject(TEST_USER, "Tracer"))
                      .thenReturn(projectMessages);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Messages from project Tracer:"), 
                        "Should display messages from project header");
                assertTrue(outContent.toString().contains("Project message"), 
                        "Should display project message content");
            }
        }
        
        @Test
        @DisplayName("Should send direct message successfully")
        void shouldSendDirectMessageSuccessfully() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                String recipient = "recipient1";
                command.setSubcommand(recipient);
                command.setArgs(new String[]{"Hello", "team", "member"});
                
                Mockito.when(mockProjectContext.isProjectMember(TEST_PROJECT, recipient))
                      .thenReturn(true);
                
                Mockito.when(mockMessageService.sendMessage(any(RinnaMessage.class)))
                      .thenReturn(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode, "Should return success code");
                assertTrue(outContent.toString().contains("Message sent to " + recipient), 
                        "Should display message sent confirmation");
                verify(mockMessageService).sendMessage(argThat(message -> 
                    message.getSender().equals(TEST_USER) &&
                    message.getRecipient().equals(recipient) &&
                    message.getContent().equals("Hello team member") &&
                    message.getProject().equals(TEST_PROJECT)
                ));
            }
        }
    }

    /**
     * Tests for the negative scenarios of the MsgCommand.
     */
    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("Should show error when authentication is required")
        void shouldShowErrorWhenAuthenticationIsRequired() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                Mockito.when(mockConfigService.isAuthenticated()).thenReturn(false);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Authentication required"), 
                        "Should show authentication required error");
            }
        }
        
        @Test
        @DisplayName("Should show error when token validation fails")
        void shouldShowErrorWhenTokenValidationFails() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                Mockito.when(mockConfigService.isAuthenticated()).thenReturn(true);
                Mockito.when(mockMessageService.validateToken(any())).thenReturn(false);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Authentication required"), 
                        "Should show authentication required error");
            }
        }
        
        @Test
        @DisplayName("Should show error when login credentials are missing")
        void shouldShowErrorWhenLoginCredentialsAreMissing() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("login");
                command.setArgs(new String[]{"username"}); // Missing password
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Username and password are required"), 
                        "Should show missing credentials error");
            }
        }
        
        @Test
        @DisplayName("Should show error when login authentication fails")
        void shouldShowErrorWhenLoginAuthenticationFails() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("login");
                command.setArgs(new String[]{TEST_USER, "wrong-password"});
                
                Mockito.when(mockMessageService.authenticate(TEST_USER, "wrong-password"))
                      .thenReturn(null); // Authentication failure
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Authentication failed: Invalid credentials"), 
                        "Should show authentication failed error");
            }
        }
        
        @Test
        @DisplayName("Should show error when login throws exception")
        void shouldShowErrorWhenLoginThrowsException() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("login");
                command.setArgs(new String[]{TEST_USER, TEST_PASSWORD});
                
                Mockito.when(mockMessageService.authenticate(TEST_USER, TEST_PASSWORD))
                      .thenThrow(new RuntimeException("Connection error"));
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error during authentication"), 
                        "Should show authentication error");
            }
        }
        
        @Test
        @DisplayName("Should show error when project switch access is denied")
        void shouldShowErrorWhenProjectSwitchAccessIsDenied() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("project");
                command.setArgs(new String[]{"switch", "SecretProject"});
                
                Mockito.when(mockMessageService.canAccessProject(TEST_USER, "SecretProject"))
                      .thenReturn(false); // No access
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Access denied"), 
                        "Should show access denied error");
            }
        }
        
        @Test
        @DisplayName("Should show error when message to read is not found")
        void shouldShowErrorWhenMessageToReadIsNotFound() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("read");
                command.setArgs(new String[]{"non-existent-message"});
                
                Mockito.when(mockMessageService.getMessage("non-existent-message"))
                      .thenReturn(null); // Message not found
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Message with ID 'non-existent-message' not found"), 
                        "Should show message not found error");
            }
        }
        
        @Test
        @DisplayName("Should show error when trying to read message belonging to another user")
        void shouldShowErrorWhenTryingToReadMessageBelongingToAnotherUser() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("read");
                command.setArgs(new String[]{TEST_MESSAGE_ID});
                
                RinnaMessage testMessage = createTestMessage(
                    TEST_MESSAGE_ID, "sender1", "another.user", "Test message content", TEST_PROJECT, MessageStatus.UNREAD
                );
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(testMessage);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Cannot read: You don't have permission"), 
                        "Should show permission error for reading message");
            }
        }
        
        @Test
        @DisplayName("Should show error when message to delete is not found")
        void shouldShowErrorWhenMessageToDeleteIsNotFound() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("delete");
                command.setArgs(new String[]{"non-existent-message"});
                
                Mockito.when(mockMessageService.getMessage("non-existent-message"))
                      .thenReturn(null); // Message not found
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Message with ID 'non-existent-message' not found"), 
                        "Should show message not found error");
            }
        }
        
        @Test
        @DisplayName("Should show error when trying to delete message belonging to another user")
        void shouldShowErrorWhenTryingToDeleteMessageBelongingToAnotherUser() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("delete");
                command.setArgs(new String[]{TEST_MESSAGE_ID});
                
                RinnaMessage testMessage = createTestMessage(
                    TEST_MESSAGE_ID, "sender1", "another.user", "Test message content", TEST_PROJECT, MessageStatus.READ
                );
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(testMessage);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Cannot delete: You don't have permission"), 
                        "Should show permission error for deleting message");
            }
        }
        
        @Test
        @DisplayName("Should show error when delete operation fails")
        void shouldShowErrorWhenDeleteOperationFails() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("delete");
                command.setArgs(new String[]{TEST_MESSAGE_ID});
                
                RinnaMessage testMessage = createTestMessage(
                    TEST_MESSAGE_ID, "sender1", TEST_USER, "Test message content", TEST_PROJECT, MessageStatus.READ
                );
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(testMessage);
                
                Mockito.when(mockMessageService.deleteMessage(TEST_MESSAGE_ID, TEST_USER))
                      .thenReturn(false); // Delete operation failed
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Failed to delete message"), 
                        "Should show delete failure error");
            }
        }
        
        @Test
        @DisplayName("Should show error when message to reply to is not found")
        void shouldShowErrorWhenMessageToReplyToIsNotFound() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("reply");
                command.setArgs(new String[]{"non-existent-message", "Reply content"});
                
                Mockito.when(mockMessageService.getMessage("non-existent-message"))
                      .thenReturn(null); // Message not found
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Message with ID 'non-existent-message' not found"), 
                        "Should show message not found error");
            }
        }
        
        @Test
        @DisplayName("Should show error when trying to reply to message belonging to another user")
        void shouldShowErrorWhenTryingToReplyToMessageBelongingToAnotherUser() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("reply");
                command.setArgs(new String[]{TEST_MESSAGE_ID, "Reply content"});
                
                RinnaMessage testMessage = createTestMessage(
                    TEST_MESSAGE_ID, "sender1", "another.user", "Test message content", TEST_PROJECT, MessageStatus.READ
                );
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(testMessage);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Cannot reply: You don't have permission"), 
                        "Should show permission error for replying to message");
            }
        }
        
        @Test
        @DisplayName("Should show error when reply content is missing")
        void shouldShowErrorWhenReplyContentIsMissing() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("reply");
                command.setArgs(new String[]{TEST_MESSAGE_ID}); // Missing reply content
                
                RinnaMessage testMessage = createTestMessage(
                    TEST_MESSAGE_ID, "sender1", TEST_USER, "Test message content", TEST_PROJECT, MessageStatus.READ
                );
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(testMessage);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Reply content is required"), 
                        "Should show missing reply content error");
            }
        }
        
        @Test
        @DisplayName("Should show error when send operation fails")
        void shouldShowErrorWhenSendOperationFails() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("reply");
                command.setArgs(new String[]{TEST_MESSAGE_ID, "Reply content"});
                
                RinnaMessage testMessage = createTestMessage(
                    TEST_MESSAGE_ID, "sender1", TEST_USER, "Test message content", TEST_PROJECT, MessageStatus.READ
                );
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(testMessage);
                
                Mockito.when(mockMessageService.sendMessage(any(RinnaMessage.class)))
                      .thenReturn(false); // Send operation failed
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Failed to send reply"), 
                        "Should show send failure error");
            }
        }
        
        @Test
        @DisplayName("Should show error when no active project for sending direct message")
        void shouldShowErrorWhenNoActiveProjectForSendingDirectMessage() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("recipient");
                command.setArgs(new String[]{"Message content"});
                
                Mockito.when(mockProjectContext.isProjectActive())
                      .thenReturn(false); // No active project
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: No active project"), 
                        "Should show no active project error");
            }
        }
        
        @Test
        @DisplayName("Should show error when message content is missing for direct message")
        void shouldShowErrorWhenMessageContentIsMissingForDirectMessage() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("recipient");
                command.setArgs(new String[0]); // Missing message content
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Message content is required"), 
                        "Should show missing message content error");
            }
        }
        
        @Test
        @DisplayName("Should show error when recipient is not a project member")
        void shouldShowErrorWhenRecipientIsNotAProjectMember() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                String nonMemberRecipient = "non-member";
                command.setSubcommand(nonMemberRecipient);
                command.setArgs(new String[]{"Message content"});
                
                Mockito.when(mockProjectContext.isProjectMember(TEST_PROJECT, nonMemberRecipient))
                      .thenReturn(false); // Not a project member
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Cannot send message: " + nonMemberRecipient + 
                        " is not a member of project " + TEST_PROJECT), 
                        "Should show not a project member error");
            }
        }
        
        @Test
        @DisplayName("Should show error when direct message send operation fails")
        void shouldShowErrorWhenDirectMessageSendOperationFails() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                String recipient = "recipient";
                command.setSubcommand(recipient);
                command.setArgs(new String[]{"Message content"});
                
                Mockito.when(mockProjectContext.isProjectMember(TEST_PROJECT, recipient))
                      .thenReturn(true);
                
                Mockito.when(mockMessageService.sendMessage(any(RinnaMessage.class)))
                      .thenReturn(false); // Send operation failed
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode, "Should return error code");
                assertTrue(errContent.toString().contains("Error: Failed to send message"), 
                        "Should show send failure error");
            }
        }
    }

    /**
     * Tests for the contract between MsgCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call ConfigurationService.isAuthenticated and MessageService.validateToken")
        void shouldCallConfigurationServiceIsAuthenticatedAndMessageServiceValidateToken() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // When
                command.call();
                
                // Then
                verify(mockConfigService).isAuthenticated();
                verify(mockMessageService).validateToken(TEST_AUTH_TOKEN);
            }
        }
        
        @Test
        @DisplayName("Should call MessageService.getMessagesForUser with current user")
        void shouldCallMessageServiceGetMessagesForUserWithCurrentUser() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // When
                command.call();
                
                // Then
                verify(mockMessageService).getMessagesForUser(TEST_USER);
            }
        }
        
        @Test
        @DisplayName("Should call MessageService.getUnreadMessagesForUser with current user")
        void shouldCallMessageServiceGetUnreadMessagesForUserWithCurrentUser() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("unread");
                
                // When
                command.call();
                
                // Then
                verify(mockMessageService).getUnreadMessagesForUser(TEST_USER);
            }
        }
        
        @Test
        @DisplayName("Should call MessageService.authenticate with provided credentials")
        void shouldCallMessageServiceAuthenticateWithProvidedCredentials() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("login");
                command.setArgs(new String[]{TEST_USER, TEST_PASSWORD});
                
                // When
                command.call();
                
                // Then
                verify(mockMessageService).authenticate(TEST_USER, TEST_PASSWORD);
            }
        }
        
        @Test
        @DisplayName("Should call ProjectContext.setCurrentProject and setCurrentProjectKey")
        void shouldCallProjectContextSetCurrentProjectAndSetCurrentProjectKey() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Given
                command.setSubcommand("project");
                command.setArgs(new String[]{"switch", "Tracer"});
                
                Mockito.when(mockMessageService.canAccessProject(TEST_USER, "Tracer"))
                      .thenReturn(true);
                
                // When
                command.call();
                
                // Then
                verify(mockProjectContext).setCurrentProject("Tracer");
                verify(mockProjectContext).setCurrentProjectKey("TRC");
            }
        }
    }

    /**
     * Tests for integration scenarios of the MsgCommand.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should handle full message workflow")
        void shouldHandleFullMessageWorkflow() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // 1. Login
                command.setSubcommand("login");
                command.setArgs(new String[]{TEST_USER, TEST_PASSWORD});
                
                Mockito.when(mockMessageService.authenticate(TEST_USER, TEST_PASSWORD))
                      .thenReturn(TEST_AUTH_TOKEN);
                
                int loginResult = command.call();
                assertEquals(0, loginResult, "Login should succeed");
                
                // Clear output buffer for next step
                outContent.reset();
                
                // 2. Switch to a project
                command.setSubcommand("project");
                command.setArgs(new String[]{"switch", "Tracer"});
                
                Mockito.when(mockMessageService.canAccessProject(TEST_USER, "Tracer"))
                      .thenReturn(true);
                
                int switchResult = command.call();
                assertEquals(0, switchResult, "Project switch should succeed");
                
                // Clear output buffer for next step
                outContent.reset();
                
                // 3. Send a message
                String recipient = "team.member";
                command.setSubcommand(recipient);
                command.setArgs(new String[]{"Hello from integration test"});
                
                Mockito.when(mockProjectContext.isProjectMember("Tracer", recipient))
                      .thenReturn(true);
                
                Mockito.when(mockMessageService.sendMessage(any(RinnaMessage.class)))
                      .thenReturn(true);
                
                int sendResult = command.call();
                assertEquals(0, sendResult, "Message send should succeed");
                assertTrue(outContent.toString().contains("Message sent to " + recipient), 
                        "Should confirm message was sent");
                
                // Verify the entire workflow
                verify(mockConfigService).setCurrentUser(TEST_USER);
                verify(mockConfigService).setAuthToken(TEST_AUTH_TOKEN);
                verify(mockProjectContext).setCurrentProject("Tracer");
                verify(mockProjectContext).setCurrentProjectKey("TRC");
                verify(mockMessageService).sendMessage(argThat(message -> 
                    message.getSender().equals(TEST_USER) &&
                    message.getRecipient().equals(recipient) &&
                    message.getContent().equals("Hello from integration test") &&
                    message.getProject().equals("Tracer")
                ));
            }
        }
        
        @Test
        @DisplayName("Should handle message reply workflow")
        void shouldHandleMessageReplyWorkflow() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // 1. List messages
                List<RinnaMessage> messages = new ArrayList<>();
                String sender = "team.lead";
                RinnaMessage incomingMessage = createTestMessage(
                    TEST_MESSAGE_ID, sender, TEST_USER, "Can you update the documentation?", TEST_PROJECT, MessageStatus.UNREAD
                );
                messages.add(incomingMessage);
                
                Mockito.when(mockMessageService.getMessagesForUser(TEST_USER))
                      .thenReturn(messages);
                
                int listResult = command.call();
                assertEquals(0, listResult, "Message listing should succeed");
                assertTrue(outContent.toString().contains("Can you update the documentation?"), 
                        "Should display the incoming message");
                
                // Clear output buffer for next step
                outContent.reset();
                
                // 2. Read the specific message
                command.setSubcommand("read");
                command.setArgs(new String[]{TEST_MESSAGE_ID});
                
                Mockito.when(mockMessageService.getMessage(TEST_MESSAGE_ID))
                      .thenReturn(incomingMessage);
                
                int readResult = command.call();
                assertEquals(0, readResult, "Message read should succeed");
                assertTrue(outContent.toString().contains("Message " + TEST_MESSAGE_ID), 
                        "Should display the message details");
                
                // Clear output buffer for next step
                outContent.reset();
                
                // 3. Reply to the message
                command.setSubcommand("reply");
                command.setArgs(new String[]{TEST_MESSAGE_ID, "I'll update it by tomorrow"});
                
                Mockito.when(mockMessageService.sendMessage(any(RinnaMessage.class)))
                      .thenReturn(true);
                
                int replyResult = command.call();
                assertEquals(0, replyResult, "Message reply should succeed");
                assertTrue(outContent.toString().contains("Reply sent to " + sender), 
                        "Should confirm reply was sent");
                
                // Verify the entire workflow
                verify(mockMessageService).markMessageAsRead(TEST_MESSAGE_ID);
                verify(mockMessageService, atLeastOnce()).getMessage(TEST_MESSAGE_ID);
                verify(mockMessageService).sendMessage(argThat(message -> 
                    message.getSender().equals(TEST_USER) &&
                    message.getRecipient().equals(sender) &&
                    message.getContent().equals("I'll update it by tomorrow") &&
                    message.getInReplyTo().equals(TEST_MESSAGE_ID)
                ));
            }
        }
        
        @Test
        @DisplayName("Should work with real MockMessageService implementation")
        void shouldWorkWithRealMockMessageServiceImplementation() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                // Create real mock implementations
                MessageService realMessageService = new MockMessageService();
                ConfigurationService realConfigService = Mockito.mock(ConfigurationService.class);
                ProjectContext realProjectContext = Mockito.mock(ProjectContext.class);
                ServiceManager realServiceManager = Mockito.mock(ServiceManager.class);
                
                // Configure real service manager
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(realServiceManager);
                when(realServiceManager.getMessageService()).thenReturn(realMessageService);
                when(realServiceManager.getConfigurationService()).thenReturn(realConfigService);
                when(realServiceManager.getProjectContext()).thenReturn(realProjectContext);
                
                // Configure dependencies
                when(realConfigService.isAuthenticated()).thenReturn(true);
                when(realConfigService.getCurrentUser()).thenReturn(TEST_USER);
                when(realConfigService.getAuthToken()).thenReturn(TEST_AUTH_TOKEN);
                when(realProjectContext.isProjectActive()).thenReturn(true);
                when(realProjectContext.getCurrentProject()).thenReturn(TEST_PROJECT);
                when(realProjectContext.getCurrentProjectKey()).thenReturn(TEST_PROJECT_KEY);
                when(realProjectContext.isProjectMember(anyString(), anyString())).thenReturn(true);
                
                // 1. First send a message
                MsgCommand sendCommand = new MsgCommand();
                sendCommand.setSubcommand("recipient.user");
                sendCommand.setArgs(new String[]{"Hello from real mock service"});
                
                int sendResult = sendCommand.call();
                assertEquals(0, sendResult, "Message send should succeed");
                
                // 2. List messages (should include the one we just sent)
                outContent.reset();
                MsgCommand listCommand = new MsgCommand();
                
                int listResult = listCommand.call();
                assertEquals(0, listResult, "Message listing should succeed");
                assertTrue(outContent.toString().contains("Hello from real mock service"), 
                        "Message list should include our sent message");
                
                // 3. Read an unknown message (should fail)
                outContent.reset();
                MsgCommand readCommand = new MsgCommand();
                readCommand.setSubcommand("read");
                readCommand.setArgs(new String[]{"non-existent-message"});
                
                int readResult = readCommand.call();
                assertEquals(1, readResult, "Reading non-existent message should fail");
                assertTrue(errContent.toString().contains("Error: Message with ID 'non-existent-message' not found"), 
                        "Should show error for non-existent message");
            }
        }
        
        @Test
        @DisplayName("Should display messages with different statuses correctly")
        void shouldDisplayMessagesWithDifferentStatusesCorrectly() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create messages with different statuses
                List<RinnaMessage> messages = new ArrayList<>();
                RinnaMessage readMessage = createTestMessage(
                    "msg-read", "sender1", TEST_USER, "This message has been read", TEST_PROJECT, MessageStatus.READ
                );
                RinnaMessage unreadMessage = createTestMessage(
                    "msg-unread", "sender2", TEST_USER, "This message is unread", TEST_PROJECT, MessageStatus.UNREAD
                );
                messages.add(readMessage);
                messages.add(unreadMessage);
                
                // Configure mock
                Mockito.when(mockMessageService.getMessagesForUser(TEST_USER))
                      .thenReturn(messages);
                
                // Execute command
                int result = command.call();
                
                // Verify results
                assertEquals(0, result, "Command should execute successfully");
                String output = outContent.toString();
                
                // The unread message should have [UNREAD] marker
                assertTrue(output.contains("[UNREAD]"), 
                        "Output should contain [UNREAD] marker for unread message");
                
                // Different formatting for read and unread messages
                assertTrue(output.contains("This message has been read"), 
                        "Output should contain read message content");
                assertTrue(output.contains("This message is unread"), 
                        "Output should contain unread message content");
                
                // Both messages should be marked as read afterwards
                verify(mockMessageService).markMessageAsRead("msg-read");
                verify(mockMessageService).markMessageAsRead("msg-unread");
            }
        }
    }
}