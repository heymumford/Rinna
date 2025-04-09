/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.rinna.cli.service.MockRecoveryService;
import org.rinna.cli.service.MockSecurityService;
import org.rinna.cli.service.ServiceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Step definitions for admin recovery command tests.
 */
public class AdminRecoveryCommandSteps {
    
    private final TestContext context;
    private final CommandProcessor commandProcessor;
    private final MockSecurityService mockSecurityService;
    private final MockRecoveryService mockRecoveryService;
    
    /**
     * Constructor with test context.
     * 
     * @param context the test context
     */
    public AdminRecoveryCommandSteps(TestContext context) {
        this.context = context;
        this.commandProcessor = context.getCommandProcessor();
        this.mockSecurityService = context.getMockSecurityService();
        this.mockRecoveryService = mock(MockRecoveryService.class);
        
        // Set up default behavior for mock services
        setupDefaultMockBehavior();
        
        // Register the mock recovery service in the test context
        ServiceManager serviceManager = ServiceManager.getInstance();
        try {
            java.lang.reflect.Field recoveryServiceField = serviceManager.getClass().getDeclaredField("recoveryService");
            recoveryServiceField.setAccessible(true);
            recoveryServiceField.set(serviceManager, mockRecoveryService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up mock recovery service", e);
        }
    }
    
    /**
     * Sets up default behavior for mock services.
     */
    private void setupDefaultMockBehavior() {
        // Mock recovery status
        when(mockRecoveryService.getRecoveryStatus()).thenReturn(
            "Recovery Status\n" +
            "==============\n\n" +
            "Last Recovery: 2025-04-05 14:30:00\n" +
            "Status: Complete\n" +
            "Recovery Plan: Disaster Recovery Plan v1.2\n" +
            "Backup Used: BACKUP-12345678\n" +
            "Duration: 15m 42s\n" +
            "Items Recovered: 1,423\n" +
            "Data Size: 2.4 GB\n\n" +
            "Verification Status: Passed\n" +
            "Integrity Checks: 100%\n" +
            "Recovery Points Available: 5"
        );
        
        // Mock recovery plan generation
        when(mockRecoveryService.generateRecoveryPlan()).thenReturn(
            "/home/emumford/NativeLinuxProjects/Rinna/target/recovery/recovery-plan-12345678.json"
        );
        
        // Mock recovery plan testing
        when(mockRecoveryService.testRecoveryPlan(true)).thenReturn(
            "Recovery Plan Test Results\n" +
            "=========================\n\n" +
            "Mode: Simulation\n" +
            "Test Time: 2025-04-08 10:15:30\n" +
            "Plan Version: v1.2\n" +
            "Duration: 3m 24s\n\n" +
            "Overall Result: PASSED\n\n" +
            "Details:\n" +
            "- Database Recovery: PASSED\n" +
            "- File System Recovery: PASSED\n" +
            "- Configuration Recovery: PASSED\n" +
            "- User Data Recovery: PASSED\n" +
            "- Integration Tests: SKIPPED\n\n" +
            "Notes:\n" +
            "- All recovery operations completed successfully\n" +
            "- Average recovery time within expected parameters\n" +
            "- No data loss detected"
        );
        
        when(mockRecoveryService.testRecoveryPlan(false)).thenReturn(
            "Recovery Plan Test Results\n" +
            "=========================\n\n" +
            "Mode: Live Test\n" +
            "Test Time: 2025-04-08 10:30:45\n" +
            "Plan Version: v1.2\n" +
            "Duration: 8m 15s\n\n" +
            "Overall Result: PASSED\n\n" +
            "Details:\n" +
            "- Database Recovery: PASSED\n" +
            "- File System Recovery: PASSED\n" +
            "- Configuration Recovery: PASSED\n" +
            "- User Data Recovery: PASSED\n" +
            "- Integration Tests: PASSED\n\n" +
            "Notes:\n" +
            "- All recovery operations completed successfully\n" +
            "- Average recovery time within expected parameters\n" +
            "- No data loss detected"
        );
        
        // Mock recovery start success
        when(mockRecoveryService.startRecovery("BACKUP-12345678")).thenReturn(true);
        
        // Mock recovery start failure for invalid backup ID
        when(mockRecoveryService.startRecovery("INVALID-BACKUP")).thenReturn(false);
    }
    
    /**
     * Define a backup ID exists.
     * 
     * @param backupId the backup ID
     */
    @Given("a backup with ID {string} exists")
    public void aBackupWithIDExists(String backupId) {
        // This is handled by the mock configuration in setupDefaultMockBehavior
        context.storeState("backupId", backupId);
    }
    
    /**
     * Run a command with input.
     * 
     * @param command the command to run
     * @param input the input to provide
     */
    @When("I run {string} with input:")
    public void iRunCommandWithInput(String command, String input) {
        commandProcessor.executeWithInput(command, input);
    }
}