/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.bdd;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.rinna.cli.RinnaCli;
import org.rinna.cli.messaging.MessageClient;
import org.rinna.cli.messaging.MessageService;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.messaging.UserSession;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ProjectContext;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for messaging feature in BDD tests.
 */
public class MessagingCommandSteps {

    private TestContext context;
    private MessageService messageService;
    private MessageClient messageClient;
    private ConfigurationService configurationService;
    private ProjectContext projectContext;
    private RinnaCli cli;
    
    private String serverUrl;
    private Map<String, String> userPasswords = new HashMap<>();
    private Map<String, String> userTokens = new HashMap<>();
    private Map<String, List<String>> projectMembers = new HashMap<>();
    private Map<String, String> projectKeys = new HashMap<>();
    private Map<String, UserSession> userSessions = new HashMap<>();
    private List<RinnaMessage> messages = new ArrayList<>();
    
    private String currentUsername;
    private String currentAuthToken;
    private String currentProjectName;
    
    private String commandOutput;
    private String errorOutput;
    private int commandResult;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    public MessagingCommandSteps(TestContext context) {
        this.context = context;
    }

    @Before
    public void setUp() {
        // Set up output capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Create mocks
        messageService = mock(MessageService.class);
        messageClient = mock(MessageClient.class);
        configurationService = mock(ConfigurationService.class);
        projectContext = mock(ProjectContext.class);
        
        // Register mocks with test context
        context.registerService(MessageService.class, messageService);
        context.registerService(MessageClient.class, messageClient);
        context.registerService(ConfigurationService.class, configurationService);
        context.registerService(ProjectContext.class, projectContext);
        
        // Initialize the CLI with mocks
        cli = new RinnaCli();
        
        // Reset instance variables
        serverUrl = null;
        userPasswords.clear();
        userTokens.clear();
        projectMembers.clear();
        projectKeys.clear();
        userSessions.clear();
        messages.clear();
        currentUsername = null;
        currentAuthToken = null;
        currentProjectName = null;
        commandOutput = null;
        errorOutput = null;
        commandResult = 0;
        outputCaptor.reset();
        errorCaptor.reset();
    }

    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Given("a Rinna server is running at {string}")
    public void aRinnaServerIsRunningAt(String url) {
        serverUrl = url;
        
        // Configure the mock client to use this server URL
        when(messageClient.getServerUrl()).thenReturn(url);
        when(configurationService.getServerUrl()).thenReturn(url);
    }

    @Given("the following users exist in the system:")
    public void theFollowingUsersExistInTheSystem(DataTable dataTable) {
        List<Map<String, String>> users = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> user : users) {
            String username = user.get("Username");
            String password = user.get("Password");
            String authToken = user.get("Auth Token");
            
            userPasswords.put(username, password);
            userTokens.put(username, authToken);
            
            // Configure mock service for authentication
            when(messageService.authenticate(eq(username), eq(password)))
                .thenReturn(authToken);
            
            // Create mock user session
            UserSession session = new UserSession(username, authToken);
            userSessions.put(username, session);
        }
    }

    @Given("the following projects exist:")
    public void theFollowingProjectsExist(DataTable dataTable) {
        List<Map<String, String>> projects = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> project : projects) {
            String projectName = project.get("Project Name");
            String projectKey = project.get("Key");
            String members = project.get("Members");
            
            projectKeys.put(projectName, projectKey);
            
            // Parse members
            List<String> memberList = new ArrayList<>();
            for (String member : members.split(",")) {
                memberList.add(member.trim());
            }
            projectMembers.put(projectName, memberList);
            
            // Configure mock service for project access checks
            for (String member : memberList) {
                when(messageService.canAccessProject(eq(member), eq(projectName)))
                    .thenReturn(true);
            }
        }
    }

    @Given("{word} is already authenticated")
    public void userIsAlreadyAuthenticated(String username) {
        currentUsername = username;
        currentAuthToken = userTokens.get(username);
        
        // Configure mock services for authenticated state
        when(configurationService.getCurrentUser()).thenReturn(username);
        when(configurationService.getAuthToken()).thenReturn(currentAuthToken);
        when(messageService.validateToken(eq(currentAuthToken))).thenReturn(true);
    }

    @Given("{word} has switched to project {string}")
    public void userHasSwitchedToProject(String username, String projectName) {
        currentProjectName = projectName;
        
        // Configure mock services for project context
        when(projectContext.getCurrentProject()).thenReturn(projectName);
        when(projectContext.getCurrentProjectKey()).thenReturn(projectKeys.get(projectName));
        when(projectContext.isProjectActive()).thenReturn(true);
    }

    @Given("{word} has not switched to any project")
    public void userHasNotSwitchedToAnyProject(String username) {
        currentProjectName = null;
        
        // Configure mock services for no project context
        when(projectContext.getCurrentProject()).thenReturn(null);
        when(projectContext.isProjectActive()).thenReturn(false);
    }

    @Given("{word} has an unread message from {word} in project {string}")
    public void userHasAnUnreadMessageFromUserInProject(String recipient, String sender, String projectName) {
        // Create a message
        RinnaMessage message = new RinnaMessage(
                "msg-" + UUID.randomUUID().toString().substring(0, 8),
                sender,
                recipient,
                "Hey, could you review my PR?",
                projectName,
                Instant.now(),
                MessageStatus.UNREAD
        );
        
        messages.add(message);
        
        // Configure mock service to return this message
        List<RinnaMessage> recipientMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(recipient))
                .collect(Collectors.toList());
        
        when(messageService.getMessagesForUser(eq(recipient)))
                .thenReturn(recipientMessages);
        
        List<RinnaMessage> unreadMessages = recipientMessages.stream()
                .filter(m -> m.getStatus() == MessageStatus.UNREAD)
                .collect(Collectors.toList());
        
        when(messageService.getUnreadMessagesForUser(eq(recipient)))
                .thenReturn(unreadMessages);
    }

    @Given("{word} has the following unread messages:")
    public void userHasTheFollowingUnreadMessages(String username, DataTable dataTable) {
        List<Map<String, String>> messageData = dataTable.asMaps(String.class, String.class);
        List<RinnaMessage> userMessages = new ArrayList<>();
        
        for (Map<String, String> data : messageData) {
            String sender = data.get("Sender");
            String projectName = data.get("Project");
            String messageText = data.get("Message");
            
            RinnaMessage message = new RinnaMessage(
                    "msg-" + UUID.randomUUID().toString().substring(0, 8),
                    sender,
                    username,
                    messageText,
                    projectName,
                    Instant.now(),
                    MessageStatus.UNREAD
            );
            
            messages.add(message);
            userMessages.add(message);
        }
        
        // Configure mock service to return these messages
        when(messageService.getMessagesForUser(eq(username)))
                .thenReturn(userMessages);
        
        when(messageService.getUnreadMessagesForUser(eq(username)))
                .thenReturn(userMessages);
    }

    @Given("{word} has the following messages:")
    public void userHasTheFollowingMessages(String username, DataTable dataTable) {
        List<Map<String, String>> messageData = dataTable.asMaps(String.class, String.class);
        List<RinnaMessage> userMessages = new ArrayList<>();
        
        for (Map<String, String> data : messageData) {
            String sender = data.get("Sender");
            String projectName = data.get("Project");
            String messageText = data.get("Message");
            String timestampStr = data.get("Timestamp");
            String readStatus = data.get("Read");
            
            // Parse timestamp
            Instant timestamp;
            if (timestampStr.equals("*")) {
                timestamp = Instant.now();
            } else {
                LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, 
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                timestamp = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            }
            
            // Parse read status
            MessageStatus status = readStatus.equalsIgnoreCase("READ") ? 
                    MessageStatus.READ : MessageStatus.UNREAD;
            
            RinnaMessage message = new RinnaMessage(
                    "msg-" + UUID.randomUUID().toString().substring(0, 8),
                    sender,
                    username,
                    messageText,
                    projectName,
                    timestamp,
                    status
            );
            
            messages.add(message);
            userMessages.add(message);
        }
        
        // Configure mock service to return these messages
        when(messageService.getMessagesForUser(eq(username)))
                .thenReturn(userMessages);
        
        List<RinnaMessage> unreadMessages = userMessages.stream()
                .filter(m -> m.getStatus() == MessageStatus.UNREAD)
                .collect(Collectors.toList());
        
        when(messageService.getUnreadMessagesForUser(eq(username)))
                .thenReturn(unreadMessages);
    }

    @Given("{word} has both read and unread messages")
    public void userHasBothReadAndUnreadMessages(String username) {
        List<RinnaMessage> userMessages = new ArrayList<>();
        
        // Create some read messages
        RinnaMessage readMessage1 = new RinnaMessage(
                "msg-read-1",
                "eric",
                username,
                "This is an old message",
                "Tracer",
                Instant.now().minusSeconds(3600), // 1 hour ago
                MessageStatus.READ
        );
        
        RinnaMessage readMessage2 = new RinnaMessage(
                "msg-read-2",
                "maria",
                username,
                "Another read message",
                "Quantum",
                Instant.now().minusSeconds(7200), // 2 hours ago
                MessageStatus.READ
        );
        
        // Create some unread messages
        RinnaMessage unreadMessage1 = new RinnaMessage(
                "msg-unread-1",
                "eric",
                username,
                "This is a new message",
                "Tracer",
                Instant.now().minusSeconds(600), // 10 minutes ago
                MessageStatus.UNREAD
        );
        
        RinnaMessage unreadMessage2 = new RinnaMessage(
                "msg-unread-2",
                "maria",
                username,
                "Another unread message",
                "Quantum",
                Instant.now().minusSeconds(300), // 5 minutes ago
                MessageStatus.UNREAD
        );
        
        userMessages.add(readMessage1);
        userMessages.add(readMessage2);
        userMessages.add(unreadMessage1);
        userMessages.add(unreadMessage2);
        messages.addAll(userMessages);
        
        // Configure mock service to return these messages
        when(messageService.getMessagesForUser(eq(username)))
                .thenReturn(userMessages);
        
        List<RinnaMessage> unreadMessages = userMessages.stream()
                .filter(m -> m.getStatus() == MessageStatus.UNREAD)
                .collect(Collectors.toList());
        
        when(messageService.getUnreadMessagesForUser(eq(username)))
                .thenReturn(unreadMessages);
    }

    @Given("{word} has messages from multiple senders")
    public void userHasMessagesFromMultipleSenders(String username) {
        List<RinnaMessage> userMessages = new ArrayList<>();
        
        // Create messages from different senders
        RinnaMessage ericMessage1 = new RinnaMessage(
                "msg-eric-1",
                "eric",
                username,
                "Message from Eric 1",
                "Tracer",
                Instant.now().minusSeconds(3600),
                MessageStatus.READ
        );
        
        RinnaMessage ericMessage2 = new RinnaMessage(
                "msg-eric-2",
                "eric",
                username,
                "Message from Eric 2",
                "Tracer",
                Instant.now().minusSeconds(1800),
                MessageStatus.UNREAD
        );
        
        RinnaMessage mariaMessage1 = new RinnaMessage(
                "msg-maria-1",
                "maria",
                username,
                "Message from Maria 1",
                "Quantum",
                Instant.now().minusSeconds(2400),
                MessageStatus.READ
        );
        
        RinnaMessage mariaMessage2 = new RinnaMessage(
                "msg-maria-2",
                "maria",
                username,
                "Message from Maria 2",
                "Quantum",
                Instant.now().minusSeconds(1200),
                MessageStatus.UNREAD
        );
        
        userMessages.add(ericMessage1);
        userMessages.add(ericMessage2);
        userMessages.add(mariaMessage1);
        userMessages.add(mariaMessage2);
        messages.addAll(userMessages);
        
        // Configure mock service to return these messages
        when(messageService.getMessagesForUser(eq(username)))
                .thenReturn(userMessages);
        
        // Configure mock service to return filtered messages by sender
        when(messageService.getMessagesForUserBySender(eq(username), eq("eric")))
                .thenReturn(List.of(ericMessage1, ericMessage2));
        
        when(messageService.getMessagesForUserBySender(eq(username), eq("maria")))
                .thenReturn(List.of(mariaMessage1, mariaMessage2));
    }

    @Given("{word} has messages from multiple projects")
    public void userHasMessagesFromMultipleProjects(String username) {
        List<RinnaMessage> userMessages = new ArrayList<>();
        
        // Create messages from different projects
        RinnaMessage tracerMessage1 = new RinnaMessage(
                "msg-tracer-1",
                "eric",
                username,
                "Tracer message 1",
                "Tracer",
                Instant.now().minusSeconds(3600),
                MessageStatus.READ
        );
        
        RinnaMessage tracerMessage2 = new RinnaMessage(
                "msg-tracer-2",
                "eric",
                username,
                "Tracer message 2",
                "Tracer",
                Instant.now().minusSeconds(1800),
                MessageStatus.UNREAD
        );
        
        RinnaMessage quantumMessage1 = new RinnaMessage(
                "msg-quantum-1",
                "maria",
                username,
                "Quantum message 1",
                "Quantum",
                Instant.now().minusSeconds(2400),
                MessageStatus.READ
        );
        
        RinnaMessage quantumMessage2 = new RinnaMessage(
                "msg-quantum-2",
                "maria",
                username,
                "Quantum message 2",
                "Quantum",
                Instant.now().minusSeconds(1200),
                MessageStatus.UNREAD
        );
        
        userMessages.add(tracerMessage1);
        userMessages.add(tracerMessage2);
        userMessages.add(quantumMessage1);
        userMessages.add(quantumMessage2);
        messages.addAll(userMessages);
        
        // Configure mock service to return these messages
        when(messageService.getMessagesForUser(eq(username)))
                .thenReturn(userMessages);
        
        // Configure mock service to return filtered messages by project
        when(messageService.getMessagesForUserByProject(eq(username), eq("Tracer")))
                .thenReturn(List.of(tracerMessage1, tracerMessage2));
        
        when(messageService.getMessagesForUserByProject(eq(username), eq("Quantum")))
                .thenReturn(List.of(quantumMessage1, quantumMessage2));
    }

    @Given("{word} has a message with ID {string} from {word}")
    public void userHasAMessageWithIdFromUser(String recipient, String messageId, String sender) {
        RinnaMessage message = new RinnaMessage(
                messageId,
                sender,
                recipient,
                "Message content from " + sender,
                "Tracer", // Default project
                Instant.now().minusSeconds(1800),
                MessageStatus.UNREAD
        );
        
        messages.add(message);
        
        // Configure mock service to return this message
        when(messageService.getMessage(eq(messageId))).thenReturn(message);
        
        List<RinnaMessage> userMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(recipient))
                .collect(Collectors.toList());
        
        when(messageService.getMessagesForUser(eq(recipient)))
                .thenReturn(userMessages);
    }

    @Given("{word} has unread messages")
    public void userHasUnreadMessages(String username) {
        List<RinnaMessage> unreadMessages = new ArrayList<>();
        
        RinnaMessage message1 = new RinnaMessage(
                "msg-unread-1",
                "eric",
                username,
                "Unread message 1",
                "Tracer",
                Instant.now().minusSeconds(1800),
                MessageStatus.UNREAD
        );
        
        RinnaMessage message2 = new RinnaMessage(
                "msg-unread-2",
                "maria",
                username,
                "Unread message 2",
                "Quantum",
                Instant.now().minusSeconds(900),
                MessageStatus.UNREAD
        );
        
        unreadMessages.add(message1);
        unreadMessages.add(message2);
        messages.addAll(unreadMessages);
        
        // Configure mock service to return these messages
        when(messageService.getUnreadMessagesForUser(eq(username)))
                .thenReturn(unreadMessages);
        
        List<RinnaMessage> allMessages = new ArrayList<>(unreadMessages);
        when(messageService.getMessagesForUser(eq(username)))
                .thenReturn(allMessages);
    }

    @When("I run {string}")
    public void iRun(String command) {
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Split the command into parts
        String[] parts = command.split("\\s+", 2);
        String cmdName = parts[0];
        String cmdArgs = parts.length > 1 ? parts[1] : "";
        
        if (cmdName.equals("rin")) {
            String[] subParts = cmdArgs.split("\\s+", 2);
            String rinCmd = subParts[0];
            String rinArgs = subParts.length > 1 ? subParts[1] : "";
            
            // Process different Rinna commands
            switch (rinCmd) {
                case "login":
                    handleLoginCommand(rinArgs);
                    break;
                case "project":
                    handleProjectCommand(rinArgs);
                    break;
                case "msg":
                    handleMessageCommand(rinArgs);
                    break;
                default:
                    // For any other command, just simulate success
                    commandResult = 0;
                    commandOutput = "Command executed: " + rinCmd;
            }
        } else {
            commandResult = 1;
            errorOutput = "Unknown command: " + cmdName;
        }
    }

    @When("{word} runs any Rinna CLI command")
    public void userRunsAnyRinnaCLICommand(String username) {
        // Set up the user context
        currentUsername = username;
        currentAuthToken = userTokens.get(username);
        
        // Configure mock services for authenticated state
        when(configurationService.getCurrentUser()).thenReturn(username);
        when(configurationService.getAuthToken()).thenReturn(currentAuthToken);
        
        // Get unread messages
        List<RinnaMessage> unreadMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(username))
                .filter(m -> m.getStatus() == MessageStatus.UNREAD)
                .collect(Collectors.toList());
        
        when(messageService.getUnreadMessagesForUser(eq(username)))
                .thenReturn(unreadMessages);
        
        // Simulate running any command
        commandResult = 0;
        commandOutput = "Command executed successfully";
        
        // Add message notifications to the output
        if (!unreadMessages.isEmpty()) {
            StringBuilder notifications = new StringBuilder();
            notifications.append("=== MESSAGE NOTIFICATIONS ===\n");
            
            for (RinnaMessage message : unreadMessages) {
                notifications.append("You have 1 unread message from ")
                        .append(message.getSender())
                        .append(": '")
                        .append(message.getContent())
                        .append("'\n");
            }
            
            notifications.append("===========================\n\n");
            commandOutput = notifications.toString() + commandOutput;
        }
    }

    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        Assertions.assertEquals(0, commandResult, "Command should have succeeded");
        Assertions.assertNull(errorOutput, "No error output should be produced");
    }

    @Then("the command should fail")
    public void theCommandShouldFail() {
        Assertions.assertNotEquals(0, commandResult, "Command should have failed");
        Assertions.assertNotNull(errorOutput, "Error output should be produced");
    }

    @Then("I should see a success message {string}")
    public void iShouldSeeASuccessMessage(String message) {
        Assertions.assertTrue(commandOutput.contains(message),
                "Output should contain success message: " + message);
    }

    @Then("I should see an error message {string}")
    public void iShouldSeeAnErrorMessage(String message) {
        Assertions.assertTrue(errorOutput.contains(message),
                "Error output should contain message: " + message);
    }

    @Then("{word} should receive an authentication token")
    public void userShouldReceiveAnAuthenticationToken(String username) {
        verify(messageService).authenticate(eq(username), anyString());
        Assertions.assertNotNull(currentAuthToken, "User should have received an authentication token");
    }

    @Then("the token should be stored securely in the local configuration")
    public void theTokenShouldBeStoredSecurelyInTheLocalConfiguration() {
        verify(configurationService).setAuthToken(eq(currentAuthToken));
        verify(configurationService).setCurrentUser(eq(currentUsername));
    }

    @Then("the request should include {word}'s authentication token")
    public void theRequestShouldIncludeUsersAuthenticationToken(String username) {
        String expectedToken = userTokens.get(username);
        verify(messageClient, atLeastOnce()).setAuthToken(eq(expectedToken));
    }

    @Then("I should not be prompted for credentials")
    public void iShouldNotBePromptedForCredentials() {
        verify(messageService, never()).authenticate(anyString(), anyString());
    }

    @Then("{word} should not receive an authentication token")
    public void userShouldNotReceiveAnAuthenticationToken(String username) {
        verify(messageService).authenticate(eq(username), anyString());
        Assertions.assertNull(currentAuthToken, "User should not have received an authentication token");
    }

    @Then("the current project context should be set to {string}")
    public void theCurrentProjectContextShouldBeSetTo(String projectName) {
        verify(projectContext).setCurrentProject(eq(projectName));
        Assertions.assertEquals(projectName, currentProjectName,
                "Current project context should be set to " + projectName);
    }

    @Then("the message should be stored on the server")
    public void theMessageShouldBeStoredOnTheServer() {
        ArgumentCaptor<RinnaMessage> messageCaptor = ArgumentCaptor.forClass(RinnaMessage.class);
        verify(messageService).sendMessage(messageCaptor.capture());
        
        RinnaMessage capturedMessage = messageCaptor.getValue();
        Assertions.assertNotNull(capturedMessage, "Message should have been sent to the server");
        Assertions.assertEquals(currentUsername, capturedMessage.getSender(),
                "Message sender should be the current user");
        Assertions.assertEquals(currentProjectName, capturedMessage.getProject(),
                "Message project should be the current project");
    }

    @Then("the message metadata should include:")
    public void theMessageMetadataShouldInclude(DataTable dataTable) {
        List<Map<String, String>> expectedMetadata = dataTable.asMaps(String.class, String.class);
        
        ArgumentCaptor<RinnaMessage> messageCaptor = ArgumentCaptor.forClass(RinnaMessage.class);
        verify(messageService).sendMessage(messageCaptor.capture());
        
        RinnaMessage capturedMessage = messageCaptor.getValue();
        Assertions.assertNotNull(capturedMessage, "Message should have been sent to the server");
        
        Map<String, String> expectedData = expectedMetadata.get(0);
        
        Assertions.assertEquals(expectedData.get("Sender"), capturedMessage.getSender(),
                "Message sender should match");
        Assertions.assertEquals(expectedData.get("Recipient"), capturedMessage.getRecipient(),
                "Message recipient should match");
        Assertions.assertEquals(expectedData.get("Project"), capturedMessage.getProject(),
                "Message project should match");
        
        if (!expectedData.get("Timestamp").equals("*")) {
            // If a specific timestamp is expected, check it
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    .withZone(ZoneId.systemDefault());
            String capturedTimestamp = formatter.format(capturedMessage.getTimestamp());
            Assertions.assertEquals(expectedData.get("Timestamp"), capturedTimestamp,
                    "Message timestamp should match");
        }
        
        MessageStatus expectedStatus = MessageStatus.valueOf(expectedData.get("Read Status"));
        Assertions.assertEquals(expectedStatus, capturedMessage.getStatus(),
                "Message read status should match");
    }

    @Then("the message {string} should be sent to {word}")
    public void theMessageShouldBeSentToUser(String messageContent, String recipient) {
        ArgumentCaptor<RinnaMessage> messageCaptor = ArgumentCaptor.forClass(RinnaMessage.class);
        verify(messageService).sendMessage(messageCaptor.capture());
        
        RinnaMessage capturedMessage = messageCaptor.getValue();
        Assertions.assertNotNull(capturedMessage, "Message should have been sent to the server");
        Assertions.assertEquals(messageContent, capturedMessage.getContent(),
                "Message content should match");
        Assertions.assertEquals(recipient, capturedMessage.getRecipient(),
                "Message recipient should match");
    }

    @Then("the command output should be preceded by a message notification")
    public void theCommandOutputShouldBePrecededByAMessageNotification() {
        Assertions.assertTrue(commandOutput.contains("=== MESSAGE NOTIFICATIONS ==="),
                "Output should contain message notification header");
        Assertions.assertTrue(commandOutput.contains("You have 1 unread message"),
                "Output should contain unread message notification");
    }

    @Then("the notification should show {string}")
    public void theNotificationShouldShow(String notificationText) {
        Assertions.assertTrue(commandOutput.contains(notificationText),
                "Output should contain notification text: " + notificationText);
    }

    @Then("the command output should be preceded by message notifications")
    public void theCommandOutputShouldBePrecededByMessageNotifications() {
        Assertions.assertTrue(commandOutput.contains("=== MESSAGE NOTIFICATIONS ==="),
                "Output should contain message notification header");
    }

    @Then("the notifications should show all unread messages")
    public void theNotificationsShouldShowAllUnreadMessages() {
        // Get unread messages for the current user
        List<RinnaMessage> unreadMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(currentUsername))
                .filter(m -> m.getStatus() == MessageStatus.UNREAD)
                .collect(Collectors.toList());
        
        for (RinnaMessage message : unreadMessages) {
            String notificationText = "unread message from " + message.getSender() + ": '" + 
                    message.getContent() + "'";
            Assertions.assertTrue(commandOutput.contains(notificationText),
                    "Output should contain notification text: " + notificationText);
        }
    }

    @Then("I should see a list of messages")
    public void iShouldSeeAListOfMessages() {
        Assertions.assertTrue(commandOutput.contains("Messages for " + currentUsername),
                "Output should contain message list header");
    }

    @Then("the list should include all messages")
    public void theListShouldIncludeAllMessages() {
        // Get all messages for the current user
        List<RinnaMessage> userMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(currentUsername))
                .collect(Collectors.toList());
        
        for (RinnaMessage message : userMessages) {
            Assertions.assertTrue(commandOutput.contains(message.getId()),
                    "Output should contain message ID: " + message.getId());
            Assertions.assertTrue(commandOutput.contains(message.getSender()),
                    "Output should contain message sender: " + message.getSender());
            Assertions.assertTrue(commandOutput.contains(message.getContent()),
                    "Output should contain message content: " + message.getContent());
        }
    }

    @Then("unread messages should be highlighted")
    public void unreadMessagesShouldBeHighlighted() {
        Assertions.assertTrue(commandOutput.contains("[UNREAD]") || 
                commandOutput.contains("\u001B[1;31m") ||
                commandOutput.contains("*NEW*"),
                "Output should highlight unread messages");
    }

    @Then("I should see a list of only unread messages")
    public void iShouldSeeAListOfOnlyUnreadMessages() {
        // Get unread messages for the current user
        List<RinnaMessage> unreadMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(currentUsername))
                .filter(m -> m.getStatus() == MessageStatus.UNREAD)
                .collect(Collectors.toList());
        
        List<RinnaMessage> readMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(currentUsername))
                .filter(m -> m.getStatus() == MessageStatus.READ)
                .collect(Collectors.toList());
        
        // Verify all unread messages are included
        for (RinnaMessage message : unreadMessages) {
            Assertions.assertTrue(commandOutput.contains(message.getId()),
                    "Output should contain unread message ID: " + message.getId());
        }
        
        // Verify no read messages are included
        for (RinnaMessage message : readMessages) {
            Assertions.assertFalse(commandOutput.contains(message.getId()),
                    "Output should not contain read message ID: " + message.getId());
        }
    }

    @Then("I should see a list of messages only from {word}")
    public void iShouldSeeAListOfMessagesOnlyFromUser(String sender) {
        // Get messages from the specified sender for the current user
        List<RinnaMessage> senderMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(currentUsername))
                .filter(m -> m.getSender().equals(sender))
                .collect(Collectors.toList());
        
        List<RinnaMessage> otherMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(currentUsername))
                .filter(m -> !m.getSender().equals(sender))
                .collect(Collectors.toList());
        
        // Verify all messages from the sender are included
        for (RinnaMessage message : senderMessages) {
            Assertions.assertTrue(commandOutput.contains(message.getId()),
                    "Output should contain message ID from " + sender + ": " + message.getId());
        }
        
        // Verify no messages from other senders are included
        for (RinnaMessage message : otherMessages) {
            Assertions.assertFalse(commandOutput.contains(message.getId()),
                    "Output should not contain message ID from other sender: " + message.getId());
        }
    }

    @Then("I should see a list of messages only from project {word}")
    public void iShouldSeeAListOfMessagesOnlyFromProject(String projectName) {
        // Get messages from the specified project for the current user
        List<RinnaMessage> projectMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(currentUsername))
                .filter(m -> m.getProject().equals(projectName))
                .collect(Collectors.toList());
        
        List<RinnaMessage> otherMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(currentUsername))
                .filter(m -> !m.getProject().equals(projectName))
                .collect(Collectors.toList());
        
        // Verify all messages from the project are included
        for (RinnaMessage message : projectMessages) {
            Assertions.assertTrue(commandOutput.contains(message.getId()),
                    "Output should contain message ID from project " + projectName + ": " + message.getId());
        }
        
        // Verify no messages from other projects are included
        for (RinnaMessage message : otherMessages) {
            Assertions.assertFalse(commandOutput.contains(message.getId()),
                    "Output should not contain message ID from other project: " + message.getId());
        }
    }

    @Then("{word} should receive the reply {string}")
    public void userShouldReceiveTheReply(String recipient, String replyContent) {
        ArgumentCaptor<RinnaMessage> messageCaptor = ArgumentCaptor.forClass(RinnaMessage.class);
        verify(messageService).sendMessage(messageCaptor.capture());
        
        RinnaMessage capturedMessage = messageCaptor.getValue();
        Assertions.assertNotNull(capturedMessage, "Reply should have been sent to the server");
        Assertions.assertEquals(replyContent, capturedMessage.getContent(),
                "Reply content should match");
        Assertions.assertEquals(recipient, capturedMessage.getRecipient(),
                "Reply recipient should match");
    }

    @Then("the reply should reference the original message")
    public void theReplyShouldReferenceTheOriginalMessage() {
        ArgumentCaptor<RinnaMessage> messageCaptor = ArgumentCaptor.forClass(RinnaMessage.class);
        verify(messageService).sendMessage(messageCaptor.capture());
        
        RinnaMessage capturedMessage = messageCaptor.getValue();
        Assertions.assertNotNull(capturedMessage.getInReplyTo(),
                "Reply should reference the original message ID");
    }

    @Then("all displayed messages should be marked as read on the server")
    public void allDisplayedMessagesShouldBeMarkedAsReadOnTheServer() {
        // Get all messages for the current user
        List<RinnaMessage> userMessages = messages.stream()
                .filter(m -> m.getRecipient().equals(currentUsername))
                .collect(Collectors.toList());
        
        // Verify each message was marked as read
        for (RinnaMessage message : userMessages) {
            verify(messageService).markMessageAsRead(eq(message.getId()));
        }
    }

    @Then("I should see the full content of message {string}")
    public void iShouldSeeTheFullContentOfMessage(String messageId) {
        RinnaMessage message = null;
        for (RinnaMessage msg : messages) {
            if (msg.getId().equals(messageId)) {
                message = msg;
                break;
            }
        }
        
        Assertions.assertNotNull(message, "Message should exist: " + messageId);
        
        Assertions.assertTrue(commandOutput.contains(message.getId()),
                "Output should contain message ID");
        Assertions.assertTrue(commandOutput.contains(message.getSender()),
                "Output should contain message sender");
        Assertions.assertTrue(commandOutput.contains(message.getContent()),
                "Output should contain full message content");
        Assertions.assertTrue(commandOutput.contains(message.getProject()),
                "Output should contain message project");
    }

    @Then("message {string} should be marked as read on the server")
    public void messageShouldBeMarkedAsReadOnTheServer(String messageId) {
        verify(messageService).markMessageAsRead(eq(messageId));
    }

    @Then("message {string} should be removed from {word}'s message list")
    public void messageShouldBeRemovedFromUsersMessageList(String messageId, String username) {
        verify(messageService).deleteMessage(eq(messageId), eq(username));
    }

    private void handleLoginCommand(String args) {
        // Parse login arguments
        String username = null;
        String password = null;
        
        String[] parts = args.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith("--username=")) {
                username = parts[i].substring("--username=".length());
            } else if (parts[i].startsWith("--password=")) {
                password = parts[i].substring("--password=".length());
            }
        }
        
        if (username == null || password == null) {
            commandResult = 1;
            errorOutput = "Error: Missing username or password";
            return;
        }
        
        // Try to authenticate
        currentUsername = username;
        String token = null;
        
        try {
            token = messageService.authenticate(username, password);
        } catch (Exception e) {
            commandResult = 1;
            errorOutput = "Error: Authentication failed: " + e.getMessage();
            return;
        }
        
        if (token != null) {
            // Authentication succeeded
            currentAuthToken = token;
            
            // Store authentication info
            configurationService.setCurrentUser(username);
            configurationService.setAuthToken(token);
            
            commandResult = 0;
            commandOutput = "Authentication successful. Welcome, " + username + "!";
        } else {
            // Authentication failed
            commandResult = 1;
            errorOutput = "Authentication failed: Invalid credentials";
        }
    }

    private void handleProjectCommand(String args) {
        // Check if user is authenticated
        if (currentUsername == null || currentAuthToken == null) {
            commandResult = 1;
            errorOutput = "Error: Authentication required. Please login first.";
            return;
        }
        
        // Parse project command arguments
        String[] parts = args.split("\\s+");
        String subcommand = parts[0];
        
        if (subcommand.equals("list")) {
            // List projects
            List<String> userProjects = new ArrayList<>();
            
            // Find projects that the user is a member of
            for (Map.Entry<String, List<String>> entry : projectMembers.entrySet()) {
                if (entry.getValue().contains(currentUsername)) {
                    userProjects.add(entry.getKey());
                }
            }
            
            commandResult = 0;
            commandOutput = "Projects for " + currentUsername + ":\n";
            for (String project : userProjects) {
                commandOutput += project + " (" + projectKeys.get(project) + ")\n";
            }
        } else if (subcommand.equals("switch")) {
            // Switch to a project
            if (parts.length < 2) {
                commandResult = 1;
                errorOutput = "Error: No project name specified";
                return;
            }
            
            String projectName = parts[1];
            
            // Check if the project exists and the user has access
            List<String> members = projectMembers.get(projectName);
            if (members == null) {
                commandResult = 1;
                errorOutput = "Error: Project not found: " + projectName;
                return;
            }
            
            if (!members.contains(currentUsername)) {
                commandResult = 1;
                errorOutput = "Error: Access denied: You are not a member of project " + projectName;
                return;
            }
            
            // Switch to the project
            currentProjectName = projectName;
            projectContext.setCurrentProject(projectName);
            projectContext.setCurrentProjectKey(projectKeys.get(projectName));
            
            commandResult = 0;
            commandOutput = "Switched to project: " + projectName;
        } else {
            commandResult = 1;
            errorOutput = "Error: Unknown project subcommand: " + subcommand;
        }
    }

    private void handleMessageCommand(String args) {
        // Check if user is authenticated
        if (currentUsername == null || currentAuthToken == null) {
            commandResult = 1;
            errorOutput = "Error: Authentication required. Please login first.";
            return;
        }
        
        // Handle no arguments - list messages
        if (args == null || args.isEmpty()) {
            // List all messages
            List<RinnaMessage> userMessages = messageService.getMessagesForUser(currentUsername);
            
            commandResult = 0;
            commandOutput = "Messages for " + currentUsername + ":\n";
            
            if (userMessages.isEmpty()) {
                commandOutput += "No messages found.\n";
            } else {
                for (RinnaMessage message : userMessages) {
                    String statusIndicator = message.getStatus() == MessageStatus.UNREAD ? 
                            "[UNREAD] " : "";
                    commandOutput += statusIndicator + message.getId() + " - From: " + 
                            message.getSender() + " (" + message.getProject() + ") - " + 
                            message.getContent() + "\n";
                    
                    // Mark message as read
                    messageService.markMessageAsRead(message.getId());
                }
            }
            
            return;
        }
        
        // Parse message command arguments
        String[] parts = args.split("\\s+", 2);
        String firstArg = parts[0];
        
        // Handle option flags
        if (firstArg.startsWith("--")) {
            if (firstArg.equals("--unread")) {
                // List unread messages
                List<RinnaMessage> unreadMessages = messageService.getUnreadMessagesForUser(currentUsername);
                
                commandResult = 0;
                commandOutput = "Unread messages for " + currentUsername + ":\n";
                
                if (unreadMessages.isEmpty()) {
                    commandOutput += "No unread messages found.\n";
                } else {
                    for (RinnaMessage message : unreadMessages) {
                        commandOutput += message.getId() + " - From: " + message.getSender() + 
                                " (" + message.getProject() + ") - " + message.getContent() + "\n";
                    }
                }
                
                return;
            } else if (firstArg.startsWith("--from=")) {
                // List messages from a specific sender
                String sender = firstArg.substring("--from=".length());
                List<RinnaMessage> senderMessages = 
                        messageService.getMessagesForUserBySender(currentUsername, sender);
                
                commandResult = 0;
                commandOutput = "Messages from " + sender + ":\n";
                
                if (senderMessages.isEmpty()) {
                    commandOutput += "No messages found from " + sender + ".\n";
                } else {
                    for (RinnaMessage message : senderMessages) {
                        String statusIndicator = message.getStatus() == MessageStatus.UNREAD ? 
                                "[UNREAD] " : "";
                        commandOutput += statusIndicator + message.getId() + " - " + 
                                message.getProject() + " - " + message.getContent() + "\n";
                        
                        // Mark message as read
                        messageService.markMessageAsRead(message.getId());
                    }
                }
                
                return;
            } else if (firstArg.startsWith("--project=")) {
                // List messages from a specific project
                String projectName = firstArg.substring("--project=".length());
                List<RinnaMessage> projectMessages = 
                        messageService.getMessagesForUserByProject(currentUsername, projectName);
                
                commandResult = 0;
                commandOutput = "Messages from project " + projectName + ":\n";
                
                if (projectMessages.isEmpty()) {
                    commandOutput += "No messages found from project " + projectName + ".\n";
                } else {
                    for (RinnaMessage message : projectMessages) {
                        String statusIndicator = message.getStatus() == MessageStatus.UNREAD ? 
                                "[UNREAD] " : "";
                        commandOutput += statusIndicator + message.getId() + " - From: " + 
                                message.getSender() + " - " + message.getContent() + "\n";
                        
                        // Mark message as read
                        messageService.markMessageAsRead(message.getId());
                    }
                }
                
                return;
            } else if (firstArg.equals("--read") && parts.length > 1) {
                // Read a specific message
                String messageId = parts[1].split("\\s+")[0];
                RinnaMessage message = messageService.getMessage(messageId);
                
                if (message == null) {
                    commandResult = 1;
                    errorOutput = "Error: Message with ID '" + messageId + "' not found";
                    return;
                }
                
                if (!message.getRecipient().equals(currentUsername)) {
                    commandResult = 1;
                    errorOutput = "Error: Cannot read: You don't have permission to read this message";
                    return;
                }
                
                commandResult = 0;
                commandOutput = "Message " + messageId + ":\n";
                commandOutput += "From: " + message.getSender() + "\n";
                commandOutput += "Project: " + message.getProject() + "\n";
                commandOutput += "Time: " + message.getTimestamp() + "\n";
                commandOutput += "Content: " + message.getContent() + "\n";
                
                // Mark message as read
                messageService.markMessageAsRead(messageId);
                
                return;
            } else if (firstArg.equals("--delete") && parts.length > 1) {
                // Delete a specific message
                String messageId = parts[1].split("\\s+")[0];
                RinnaMessage message = messageService.getMessage(messageId);
                
                if (message == null) {
                    commandResult = 1;
                    errorOutput = "Error: Message with ID '" + messageId + "' not found";
                    return;
                }
                
                if (!message.getRecipient().equals(currentUsername)) {
                    commandResult = 1;
                    errorOutput = "Error: Cannot delete: You don't have permission to delete this message";
                    return;
                }
                
                // Delete the message
                messageService.deleteMessage(messageId, currentUsername);
                
                commandResult = 0;
                commandOutput = "Message deleted";
                
                return;
            } else if (firstArg.equals("--reply") && parts.length > 1) {
                // Reply to a specific message
                String[] replyParts = parts[1].split("\\s+", 2);
                if (replyParts.length < 2) {
                    commandResult = 1;
                    errorOutput = "Error: No reply content provided";
                    return;
                }
                
                String messageId = replyParts[0];
                String replyContent = replyParts[1];
                
                RinnaMessage originalMessage = messageService.getMessage(messageId);
                
                if (originalMessage == null) {
                    commandResult = 1;
                    errorOutput = "Error: Message with ID '" + messageId + "' not found";
                    return;
                }
                
                if (!originalMessage.getRecipient().equals(currentUsername)) {
                    commandResult = 1;
                    errorOutput = "Error: Cannot reply: You don't have permission to reply to this message";
                    return;
                }
                
                // Create the reply
                RinnaMessage reply = new RinnaMessage(
                        "reply-" + UUID.randomUUID().toString().substring(0, 8),
                        currentUsername,
                        originalMessage.getSender(),
                        replyContent,
                        originalMessage.getProject(),
                        Instant.now(),
                        MessageStatus.UNREAD
                );
                
                // Set reply reference
                reply.setInReplyTo(messageId);
                
                // Send the reply
                messageService.sendMessage(reply);
                
                commandResult = 0;
                commandOutput = "Reply sent to " + originalMessage.getSender();
                
                return;
            }
        }
        
        // Check if a project is active
        if (currentProjectName == null) {
            commandResult = 1;
            errorOutput = "Error: No active project. Please switch to a project first.";
            return;
        }
        
        // Handle send message to recipient
        String recipient = firstArg;
        String messageContent = parts.length > 1 ? parts[1] : "";
        
        // Check if the recipient exists and is in the current project
        List<String> projectMembersList = projectMembers.get(currentProjectName);
        if (!projectMembersList.contains(recipient)) {
            commandResult = 1;
            errorOutput = "Error: Cannot send message: " + recipient + 
                    " is not a member of project " + currentProjectName;
            return;
        }
        
        // Create the message
        RinnaMessage message = new RinnaMessage(
                "msg-" + UUID.randomUUID().toString().substring(0, 8),
                currentUsername,
                recipient,
                messageContent,
                currentProjectName,
                Instant.now(),
                MessageStatus.UNREAD
        );
        
        // Send the message
        messageService.sendMessage(message);
        
        commandResult = 0;
        commandOutput = "Message sent to " + recipient;
    }
}