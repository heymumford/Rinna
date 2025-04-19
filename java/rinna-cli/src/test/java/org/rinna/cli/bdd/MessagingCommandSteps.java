/*
 * MessagingCommandSteps - Step definitions for messaging commands
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.bdd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rinna.cli.command.MsgCommand;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MockMessageService;
import org.rinna.cli.service.ProjectContext;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for messaging command tests.
 */
public class MessagingCommandSteps {
    
    private static final String TEST_MESSAGE_ID = "msg-12345678";
    private static final String TEST_AUTH_TOKEN = "valid-auth-token";
    
    private final TestContext testContext = TestContext.getInstance();
    
    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String username) {
        ConfigurationService configService = testContext.getMockConfigService();
        when(configService.isAuthenticated()).thenReturn(true);
        when(configService.getCurrentUser()).thenReturn(username);
        when(configService.getAuthToken()).thenReturn(TEST_AUTH_TOKEN);
        
        MockMessageService messageService = testContext.getMockMessageService();
        when(messageService.validateToken(TEST_AUTH_TOKEN)).thenReturn(true);
    }
    
    @Given("I am working on project {string}")
    public void iAmWorkingOnProject(String projectName) {
        ProjectContext projectContext = testContext.getMockProjectContext();
        when(projectContext.isProjectActive()).thenReturn(true);
        when(projectContext.getCurrentProject()).thenReturn(projectName);
        
        if ("Tracer".equals(projectName)) {
            when(projectContext.getCurrentProjectKey()).thenReturn("TRC");
        } else if ("Quantum".equals(projectName)) {
            when(projectContext.getCurrentProjectKey()).thenReturn("QTM");
        }
    }
    
    @Given("I have unread messages in my inbox")
    public void iHaveUnreadMessagesInMyInbox() {
        ConfigurationService configService = testContext.getMockConfigService();
        String currentUser = configService.getCurrentUser();
        String project = testContext.getMockProjectContext().getCurrentProject();
        
        List<RinnaMessage> unreadMessages = new ArrayList<>();
        unreadMessages.add(createTestMessage("msg-1", "sender1", currentUser, "Unread message 1", project, MessageStatus.UNREAD));
        unreadMessages.add(createTestMessage("msg-2", "sender2", currentUser, "Unread message 2", project, MessageStatus.UNREAD));
        
        MockMessageService messageService = testContext.getMockMessageService();
        when(messageService.getMessagesForUser(currentUser)).thenReturn(unreadMessages);
        when(messageService.getUnreadMessagesForUser(currentUser)).thenReturn(unreadMessages);
    }
    
    @Given("I have a message with ID {string} in my inbox")
    public void iHaveAMessageWithIdInMyInbox(String messageId) {
        ConfigurationService configService = testContext.getMockConfigService();
        String currentUser = configService.getCurrentUser();
        String project = testContext.getMockProjectContext().getCurrentProject();
        
        RinnaMessage message = createTestMessage(messageId, "sender1", currentUser, "Test message content", project, MessageStatus.UNREAD);
        
        MockMessageService messageService = testContext.getMockMessageService();
        when(messageService.getMessage(messageId)).thenReturn(message);
    }
    
    @Given("I have a message with ID {string} from {string} in my inbox")
    public void iHaveAMessageWithIdFromInMyInbox(String messageId, String sender) {
        ConfigurationService configService = testContext.getMockConfigService();
        String currentUser = configService.getCurrentUser();
        String project = testContext.getMockProjectContext().getCurrentProject();
        
        RinnaMessage message = createTestMessage(messageId, sender, currentUser, "Test message from " + sender, project, MessageStatus.UNREAD);
        
        MockMessageService messageService = testContext.getMockMessageService();
        when(messageService.getMessage(messageId)).thenReturn(message);
    }
    
    @Given("I have messages from different senders in my inbox")
    public void iHaveMessagesFromDifferentSendersInMyInbox() {
        ConfigurationService configService = testContext.getMockConfigService();
        String currentUser = configService.getCurrentUser();
        String project = testContext.getMockProjectContext().getCurrentProject();
        
        List<RinnaMessage> allMessages = new ArrayList<>();
        allMessages.add(createTestMessage("msg-1", "sender1", currentUser, "Message from sender1", project, MessageStatus.READ));
        allMessages.add(createTestMessage("msg-2", "sender2", currentUser, "Message from sender2", project, MessageStatus.READ));
        allMessages.add(createTestMessage("msg-3", "sender1", currentUser, "Another message from sender1", project, MessageStatus.UNREAD));
        
        List<RinnaMessage> sender1Messages = new ArrayList<>();
        sender1Messages.add(allMessages.get(0));
        sender1Messages.add(allMessages.get(2));
        
        MockMessageService messageService = testContext.getMockMessageService();
        when(messageService.getMessagesForUser(currentUser)).thenReturn(allMessages);
        when(messageService.getMessagesForUserBySender(eq(currentUser), eq("sender1"))).thenReturn(sender1Messages);
    }
    
    @Given("I have messages from different projects in my inbox")
    public void iHaveMessagesFromDifferentProjectsInMyInbox() {
        ConfigurationService configService = testContext.getMockConfigService();
        String currentUser = configService.getCurrentUser();
        
        List<RinnaMessage> allMessages = new ArrayList<>();
        allMessages.add(createTestMessage("msg-1", "teammate1", currentUser, "Message from Tracer", "Tracer", MessageStatus.READ));
        allMessages.add(createTestMessage("msg-2", "teammate2", currentUser, "Message from Quantum", "Quantum", MessageStatus.READ));
        
        List<RinnaMessage> tracerMessages = new ArrayList<>();
        tracerMessages.add(allMessages.get(0));
        
        List<RinnaMessage> quantumMessages = new ArrayList<>();
        quantumMessages.add(allMessages.get(1));
        
        MockMessageService messageService = testContext.getMockMessageService();
        when(messageService.getMessagesForUser(currentUser)).thenReturn(allMessages);
        when(messageService.getMessagesForUserByProject(eq(currentUser), eq("Tracer"))).thenReturn(tracerMessages);
        when(messageService.getMessagesForUserByProject(eq(currentUser), eq("Quantum"))).thenReturn(quantumMessages);
    }
    
    @Given("the user {string} exists in my current project")
    public void theUserExistsInMyCurrentProject(String username) {
        String project = testContext.getMockProjectContext().getCurrentProject();
        when(testContext.getMockProjectContext().isProjectMember(project, username)).thenReturn(true);
    }
    
    @Given("the user {string} does not exist in my current project")
    public void theUserDoesNotExistInMyCurrentProject(String username) {
        String project = testContext.getMockProjectContext().getCurrentProject();
        when(testContext.getMockProjectContext().isProjectMember(project, username)).thenReturn(false);
    }
    
    @Given("there is a message with ID {string} belonging to another user")
    public void thereIsAMessageWithIdBelongingToAnotherUser(String messageId) {
        String project = testContext.getMockProjectContext().getCurrentProject();
        
        RinnaMessage message = createTestMessage(messageId, "sender1", "another.user", "Message for another user", project, MessageStatus.UNREAD);
        
        MockMessageService messageService = testContext.getMockMessageService();
        when(messageService.getMessage(messageId)).thenReturn(message);
    }
    
    @When("I run the command {string}")
    public void iRunTheCommand(String commandLine) {
        testContext.resetCapturedOutput();
        
        String[] commandParts = commandLine.split("\\s+");
        
        if (commandParts.length < 2 || !commandParts[0].equals("rin") || !commandParts[1].equals("msg")) {
            throw new IllegalArgumentException("Command must be a 'rin msg' command");
        }
        
        MsgCommand command = new MsgCommand();
        
        if (commandParts.length > 2) {
            command.setSubcommand(commandParts[2]);
            
            if (commandParts.length > 3) {
                String[] args = Arrays.copyOfRange(commandParts, 3, commandParts.length);
                command.setArgs(args);
            }
        }
        
        int exitCode = command.call();
        testContext.setLastCommandExitCode(exitCode);
        testContext.setLastCommandOutput(testContext.getStandardOutput());
    }
    
    @Then("the command should execute successfully")
    public void theCommandShouldExecuteSuccessfully() {
        assertEquals(0, testContext.getLastCommandExitCode(), 
                "Command should return success code (0) but returned " + testContext.getLastCommandExitCode());
    }
    
    @Then("the command should fail with error code {int}")
    public void theCommandShouldFailWithErrorCode(int expectedErrorCode) {
        assertEquals(expectedErrorCode, testContext.getLastCommandExitCode(), 
                "Command should return error code " + expectedErrorCode + " but returned " + testContext.getLastCommandExitCode());
    }
    
    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedOutput) {
        assertTrue(testContext.getStandardOutput().contains(expectedOutput), 
                "Output should contain '" + expectedOutput + "' but doesn't.\nActual output: " + testContext.getStandardOutput());
    }
    
    @Then("the error output should contain {string}")
    public void theErrorOutputShouldContain(String expectedError) {
        assertTrue(testContext.getErrorOutput().contains(expectedError), 
                "Error output should contain '" + expectedError + "' but doesn't.\nActual error output: " + testContext.getErrorOutput());
    }
    
    @Then("all messages should be marked as read")
    public void allMessagesShouldBeMarkedAsRead() {
        MockMessageService messageService = testContext.getMockMessageService();
        verify(messageService, times(2)).markMessageAsRead(anyString());
    }
    
    @Then("the messages should remain marked as unread")
    public void theMessagesShouldRemainMarkedAsUnread() {
        MockMessageService messageService = testContext.getMockMessageService();
        verify(messageService, never()).markMessageAsRead(anyString());
    }
    
    @Then("the message should be marked as read")
    public void theMessageShouldBeMarkedAsRead() {
        MockMessageService messageService = testContext.getMockMessageService();
        verify(messageService).markMessageAsRead(TEST_MESSAGE_ID);
    }
    
    @Then("the message should no longer exist in my inbox")
    public void theMessageShouldNoLongerExistInMyInbox() {
        MockMessageService messageService = testContext.getMockMessageService();
        ConfigurationService configService = testContext.getMockConfigService();
        String currentUser = configService.getCurrentUser();
        
        verify(messageService).deleteMessage(eq(TEST_MESSAGE_ID), eq(currentUser));
    }
    
    @Then("a reply should be sent to {string} with content {string}")
    public void aReplyShouldBeSentToWithContent(String recipient, String content) {
        MockMessageService messageService = testContext.getMockMessageService();
        ConfigurationService configService = testContext.getMockConfigService();
        String currentUser = configService.getCurrentUser();
        
        verify(messageService).sendMessage(argThat(message -> 
            message.getSender().equals(currentUser) &&
            message.getRecipient().equals(recipient) &&
            message.getContent().equals(content) &&
            message.getInReplyTo() != null &&
            message.getInReplyTo().equals(TEST_MESSAGE_ID)
        ));
    }
    
    @Then("a message should be sent to {string} with content {string}")
    public void aMessageShouldBeSentToWithContent(String recipient, String content) {
        MockMessageService messageService = testContext.getMockMessageService();
        ConfigurationService configService = testContext.getMockConfigService();
        String currentUser = configService.getCurrentUser();
        String project = testContext.getMockProjectContext().getCurrentProject();
        
        verify(messageService).sendMessage(argThat(message -> 
            message.getSender().equals(currentUser) &&
            message.getRecipient().equals(recipient) &&
            message.getContent().equals(content) &&
            message.getProject().equals(project)
        ));
    }
    
    @Then("only messages from {string} should be displayed")
    public void onlyMessagesFromShouldBeDisplayed(String sender) {
        assertTrue(testContext.getStandardOutput().contains("Message from " + sender), 
                "Output should contain message from " + sender);
    }
    
    @Then("only messages from project {string} should be displayed")
    public void onlyMessagesFromProjectShouldBeDisplayed(String project) {
        assertTrue(testContext.getStandardOutput().contains("Message from " + project), 
                "Output should contain message from project " + project);
    }
    
    @Then("the current project should be set to {string}")
    public void theCurrentProjectShouldBeSetTo(String project) {
        verify(testContext.getMockProjectContext()).setCurrentProject(project);
        
        if ("Tracer".equals(project)) {
            verify(testContext.getMockProjectContext()).setCurrentProjectKey("TRC");
        } else if ("Quantum".equals(project)) {
            verify(testContext.getMockProjectContext()).setCurrentProjectKey("QTM");
        }
    }
    
    /**
     * Creates a test message with the specified properties.
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
}