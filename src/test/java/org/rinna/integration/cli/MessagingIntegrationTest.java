/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.integration.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Execution;
import org.junit.jupiter.api.ExecutionMode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rinna.base.IntegrationTest;
import org.rinna.cli.RinnaCli;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.List;

/**
 * Integration tests for messaging functionality.
 */
@Tag("integration")
@Tag("smoke")
@Execution(ExecutionMode.SAME_THREAD)
class MessagingIntegrationTest extends IntegrationTest {

    private ByteArrayOutputStream outputCaptor;
    private ByteArrayOutputStream errorCaptor;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    @BeforeEach
    void setUp() {
        // Set up output capturing
        outputCaptor = new ByteArrayOutputStream();
        errorCaptor = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Set up test environment
        ConfigurationService configService = ServiceManager.getInstance().getConfigurationService();
        
        // Clear any existing authentication
        configService.clearAuthentication();
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Test the complete messaging workflow from login to sending and receiving messages.
     */
    @Test
    void testMessagingWorkflow() {
        // 1. Login as Eric
        RinnaCli.main(new String[]{"msg", "login", "eric", "password123"});
        
        // Verify success
        Assertions.assertTrue(outputCaptor.toString().contains("Authentication successful"));
        outputCaptor.reset();
        
        // 2. Switch to a project
        RinnaCli.main(new String[]{"msg", "project", "switch", "Tracer"});
        
        // Verify success
        Assertions.assertTrue(outputCaptor.toString().contains("Switched to project: Tracer"));
        outputCaptor.reset();
        
        // 3. Send a message to Steve
        RinnaCli.main(new String[]{"msg", "steve", "Hello Steve! |GREEN|This is a test message with formatting|"});
        
        // Verify success
        Assertions.assertTrue(outputCaptor.toString().contains("Message sent to steve"));
        outputCaptor.reset();
        
        // 4. List messages to verify our outgoing messages aren't in our inbox
        RinnaCli.main(new String[]{"msg"});
        
        // We should see our existing messages but not the one we just sent
        Assertions.assertFalse(outputCaptor.toString().contains("Hello Steve! "));
        outputCaptor.reset();
        
        // 5. Logout Eric by clearing authentication
        ConfigurationService configService = ServiceManager.getInstance().getConfigurationService();
        configService.clearAuthentication();
        
        // 6. Login as Steve
        RinnaCli.main(new String[]{"msg", "login", "steve", "password456"});
        
        // Verify success and check for message notification
        Assertions.assertTrue(outputCaptor.toString().contains("Authentication successful"));
        Assertions.assertTrue(outputCaptor.toString().contains("MESSAGE NOTIFICATIONS"));
        Assertions.assertTrue(outputCaptor.toString().contains("Hello Steve!"));
        outputCaptor.reset();
        
        // 7. List Steve's messages
        RinnaCli.main(new String[]{"msg"});
        
        // Verify we see Eric's message
        Assertions.assertTrue(outputCaptor.toString().contains("Hello Steve!"));
        Assertions.assertTrue(outputCaptor.toString().contains("From: eric"));
        outputCaptor.reset();
        
        // 8. Extract message ID for reply
        // For integration test, we'll need to query the message service directly
        String messageId = ServiceManager.getInstance().getMessageService()
                .getMessagesForUser("steve").get(0).getId();
        
        // 9. Reply to the message
        RinnaCli.main(new String[]{"msg", "reply", messageId, "Hi Eric! |BLUE|Got your message|"});
        
        // Verify success
        Assertions.assertTrue(outputCaptor.toString().contains("Reply sent to eric"));
        outputCaptor.reset();
        
        // 10. Logout Steve
        configService.clearAuthentication();
        
        // 11. Login back as Eric
        RinnaCli.main(new String[]{"msg", "login", "eric", "password123"});
        
        // Verify success and message notification
        Assertions.assertTrue(outputCaptor.toString().contains("Authentication successful"));
        Assertions.assertTrue(outputCaptor.toString().contains("MESSAGE NOTIFICATIONS"));
        Assertions.assertTrue(outputCaptor.toString().contains("Hi Eric!"));
        outputCaptor.reset();
        
        // 12. List unread messages
        RinnaCli.main(new String[]{"msg", "unread"});
        
        // Verify we see Steve's reply
        Assertions.assertTrue(outputCaptor.toString().contains("Hi Eric!"));
        Assertions.assertTrue(outputCaptor.toString().contains("From: steve"));
        outputCaptor.reset();
    }
    
    /**
     * Test error handling for missing authentication.
     */
    @Test
    void testMissingAuthentication() {
        // Try to list messages without being authenticated
        RinnaCli.main(new String[]{"msg"});
        
        // Verify error
        Assertions.assertTrue(errorCaptor.toString().contains("Authentication required"));
        errorCaptor.reset();
        
        // Try to send a message without being authenticated
        RinnaCli.main(new String[]{"msg", "steve", "This will fail"});
        
        // Verify error
        Assertions.assertTrue(errorCaptor.toString().contains("Authentication required"));
    }
    
    /**
     * Test error handling for missing project context.
     */
    @Test
    void testMissingProjectContext() {
        // 1. Login as Eric
        RinnaCli.main(new String[]{"msg", "login", "eric", "password123"});
        outputCaptor.reset();
        
        // 2. Try to send a message without switching to a project
        RinnaCli.main(new String[]{"msg", "steve", "This will fail"});
        
        // Verify error
        Assertions.assertTrue(errorCaptor.toString().contains("No active project"));
    }
    
    /**
     * Test message formatting with ANSI codes.
     */
    @Test
    void testMessageFormatting() {
        // 1. Login as Eric
        RinnaCli.main(new String[]{"msg", "login", "eric", "password123"});
        outputCaptor.reset();
        
        // 2. Switch to a project
        RinnaCli.main(new String[]{"msg", "project", "switch", "Tracer"});
        outputCaptor.reset();
        
        // 3. Send a message with formatting
        String formattedMessage = "|RED|Red text| |GREEN|Green text| |BLUE|Blue text| |BOLD|Bold text|";
        RinnaCli.main(new String[]{"msg", "steve", formattedMessage});
        outputCaptor.reset();
        
        // 4. Login as Steve
        ConfigurationService configService = ServiceManager.getInstance().getConfigurationService();
        configService.clearAuthentication();
        RinnaCli.main(new String[]{"msg", "login", "steve", "password456"});
        outputCaptor.reset();
        
        // 5. Read messages
        RinnaCli.main(new String[]{"msg"});
        
        // Check that formatting is included in the output
        // The exact ANSI codes will be hard to test, but we can check for presence of the text
        String output = outputCaptor.toString();
        Assertions.assertTrue(output.contains("Red text"));
        Assertions.assertTrue(output.contains("Green text"));
        Assertions.assertTrue(output.contains("Blue text"));
        Assertions.assertTrue(output.contains("Bold text"));
        
        // ANSI escape sequences should be included
        Assertions.assertTrue(output.contains("\u001B["));
    }
    
    /**
     * Test message filtering and search capabilities.
     */
    @Test
    void testMessageFilteringAndSearch() {
        // 1. Login as Eric
        RinnaCli.main(new String[]{"msg", "login", "eric", "password123"});
        outputCaptor.reset();
        
        // 2. Switch to a project
        RinnaCli.main(new String[]{"msg", "project", "switch", "Tracer"});
        outputCaptor.reset();
        
        // 3. Send a message to Steve
        RinnaCli.main(new String[]{"msg", "steve", "Message to Steve in Tracer"});
        outputCaptor.reset();
        
        // 4. Switch to another project
        RinnaCli.main(new String[]{"msg", "project", "switch", "Quantum"});
        outputCaptor.reset();
        
        // 5. Send a message to Maria
        RinnaCli.main(new String[]{"msg", "maria", "Message to Maria in Quantum"});
        outputCaptor.reset();
        
        // 6. Login as Steve
        ConfigurationService configService = ServiceManager.getInstance().getConfigurationService();
        configService.clearAuthentication();
        RinnaCli.main(new String[]{"msg", "login", "steve", "password456"});
        outputCaptor.reset();
        
        // 7. Filter messages from Eric
        RinnaCli.main(new String[]{"msg", "--from=eric"});
        
        // Verify we see only Eric's message
        Assertions.assertTrue(outputCaptor.toString().contains("Message to Steve in Tracer"));
        Assertions.assertTrue(outputCaptor.toString().contains("From: eric"));
        outputCaptor.reset();
        
        // 8. Login as Maria
        configService.clearAuthentication();
        RinnaCli.main(new String[]{"msg", "login", "maria", "password789"});
        outputCaptor.reset();
        
        // 9. Filter messages by project
        RinnaCli.main(new String[]{"msg", "--project=Quantum"});
        
        // Verify we see only Quantum messages
        Assertions.assertTrue(outputCaptor.toString().contains("Message to Maria in Quantum"));
        Assertions.assertTrue(outputCaptor.toString().contains("From: eric"));
        outputCaptor.reset();
    }
}