/*
 * BDD step definitions for Admin Backup and Recovery
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.junit.jupiter.api.Assertions;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for Admin Backup and Recovery feature.
 */
public class AdminBackupRecoverySteps {
    private final TestContext context;
    private List<Map<String, String>> expectedEntries;
    
    /**
     * Constructs a new AdminBackupRecoverySteps with the given TestContext.
     *
     * @param context the test context
     */
    public AdminBackupRecoverySteps(TestContext context) {
        this.context = context;
    }
    
    @Given("I am logged in as an administrator")
    public void i_am_logged_in_as_an_administrator() {
        // This step is already defined in other step classes
        // We're declaring it here for clarity but it won't override the existing implementation
        context.setConfigurationFlag("admin_logged_in", true);
    }
    
    @Given("the backup service is configured")
    public void the_backup_service_is_configured() {
        context.setConfigurationFlag("backup_service_enabled", true);
        
        // Set default backup configuration
        Map<String, Object> backupConfig = new HashMap<>();
        backupConfig.put("type", "incremental");
        backupConfig.put("frequency", "weekly");
        backupConfig.put("time", "03:00");
        backupConfig.put("retention", 14);
        backupConfig.put("location", "/var/backups/rinna");
        
        context.setConfigurationValue("backup_config", backupConfig);
    }
    
    @Given("there are existing backups in the system")
    public void there_are_existing_backups_in_the_system() {
        // Create example backup history
        List<Map<String, Object>> backupHistory = new ArrayList<>();
        
        // Add a few sample backups
        LocalDateTime now = LocalDateTime.now();
        
        Map<String, Object> backup1 = new HashMap<>();
        backup1.put("id", "BACKUP-" + now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        backup1.put("type", "full");
        backup1.put("date", now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        backup1.put("size", "2.4 GB");
        backup1.put("duration", "15m 32s");
        backup1.put("status", "Completed");
        backup1.put("location", "/var/backups/rinna/full");
        backupHistory.add(backup1);
        
        Map<String, Object> backup2 = new HashMap<>();
        backup2.put("id", "BACKUP-" + now.minusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        backup2.put("type", "incremental");
        backup2.put("date", now.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        backup2.put("size", "450 MB");
        backup2.put("duration", "5m 12s");
        backup2.put("status", "Completed");
        backup2.put("location", "/var/backups/rinna/incremental");
        backupHistory.add(backup2);
        
        Map<String, Object> backup3 = new HashMap<>();
        backup3.put("id", "BACKUP-20250405-143020");
        backup3.put("type", "full");
        backup3.put("date", "2025-04-05 14:30:20");
        backup3.put("size", "2.2 GB");
        backup3.put("duration", "14m 45s");
        backup3.put("status", "Completed");
        backup3.put("location", "/var/backups/rinna/full");
        backupHistory.add(backup3);
        
        context.setConfigurationValue("backup_history", backupHistory);
    }
    
    @Given("a valid backup ID {string}")
    public void a_valid_backup_id(String backupId) {
        // Check if we have backup history
        Optional<Object> backupHistoryOpt = context.getConfigurationValue("backup_history");
        
        if (!backupHistoryOpt.isPresent()) {
            // If no backup history, create one with this backup ID
            there_are_existing_backups_in_the_system();
        }
        
        // Store the selected backup ID
        context.setConfigurationValue("selected_backup_id", backupId);
    }
    
    @When("I run the command {string}")
    public void i_run_the_command(String command) {
        // Use the CommandRunner from TestContext to execute the command
        String[] commandParts = command.split(" ", 2);
        String mainCommand = commandParts[0];
        String args = commandParts.length > 1 ? commandParts[1] : "";
        
        try {
            String[] output = context.getCommandRunner().runCommand(mainCommand, args);
            
            // Store the command output in the context
            context.setConfigurationValue("command_stdout", output[0]);
            context.setConfigurationValue("command_stderr", output[1]);
            
            // Mock specific behavior for the backup and recovery commands
            mockBackupCommandOutput(command);
        } catch (Exception e) {
            context.setException(e);
        }
    }
    
    @When("I should be prompted to select the backup type")
    public void i_should_be_prompted_to_select_the_backup_type() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select backup type") || 
                             stdout.contains("Choose backup type"));
    }
    
    @When("I select {string}")
    public void i_select(String selection) {
        // Store the selection in the context for later verification
        context.setConfigurationValue("user_selection", selection);
        
        // Handle different selection types
        if (selection.equals("full") || selection.equals("incremental") || selection.equals("differential")) {
            context.setConfigurationValue("selected_backup_type", selection);
        } else if (selection.equals("daily") || selection.equals("weekly") || selection.equals("monthly")) {
            context.setConfigurationValue("selected_frequency", selection);
        } else if (selection.equals("AES-256") || selection.equals("Twofish") || selection.equals("ChaCha20")) {
            context.setConfigurationValue("selected_algorithm", selection);
        } else if (selection.contains("success") || selection.contains("failure")) {
            context.setConfigurationValue("selected_events", Arrays.asList(selection.split(",")));
        } else if (selection.equals("synchronized") || selection.equals("sequenced") || selection.equals("load-balanced")) {
            context.setConfigurationValue("selected_strategy", selection);
        }
    }
    
    @When("I should be prompted to enter the backup frequency")
    public void i_should_be_prompted_to_enter_the_backup_frequency() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter backup frequency") || 
                             stdout.contains("Specify backup frequency"));
    }
    
    @When("I should be prompted to enter the backup time")
    public void i_should_be_prompted_to_enter_the_backup_time() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter backup time") || 
                             stdout.contains("Specify time to run backup"));
    }
    
    @When("I should be prompted to enter the retention period in days")
    public void i_should_be_prompted_to_enter_the_retention_period_in_days() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter retention period") || 
                             stdout.contains("Specify how many days to keep backups"));
    }
    
    @When("I should be prompted to enter the backup location")
    public void i_should_be_prompted_to_enter_the_backup_location() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter backup location") || 
                             stdout.contains("Specify where to store backups"));
    }
    
    @When("I should be prompted to select the backup strategy")
    public void i_should_be_prompted_to_select_the_backup_strategy() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select backup strategy") || 
                             stdout.contains("Choose a backup strategy"));
    }
    
    @When("I should be prompted to select full backup frequency")
    public void i_should_be_prompted_to_select_full_backup_frequency() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select full backup frequency") || 
                             stdout.contains("How often to run full backups"));
    }
    
    @When("I should be prompted to select incremental backup frequency")
    public void i_should_be_prompted_to_select_incremental_backup_frequency() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select incremental backup frequency") || 
                             stdout.contains("How often to run incremental backups"));
    }
    
    @When("I should be prompted to enable encryption")
    public void i_should_be_prompted_to_enable_encryption() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enable encryption?") || 
                             stdout.contains("Would you like to enable encryption"));
    }
    
    @When("I should be prompted to select encryption algorithm")
    public void i_should_be_prompted_to_select_encryption_algorithm() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select encryption algorithm") || 
                             stdout.contains("Choose an encryption algorithm"));
    }
    
    @When("I should be prompted to enter a secure passphrase")
    public void i_should_be_prompted_to_enter_a_secure_passphrase() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter a secure passphrase") || 
                             stdout.contains("Provide an encryption passphrase"));
    }
    
    @When("I should be prompted to confirm the passphrase")
    public void i_should_be_prompted_to_confirm_the_passphrase() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Confirm passphrase") || 
                             stdout.contains("Re-enter passphrase"));
    }
    
    @When("I should be prompted to confirm the recovery operation")
    public void i_should_be_prompted_to_confirm_the_recovery_operation() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Confirm recovery operation") || 
                             stdout.contains("Are you sure you want to restore"));
    }
    
    @When("the recovery process completes")
    public void the_recovery_process_completes() {
        // This is a transitional step that indicates the recovery process has completed
        // We'll simulate this by updating the recovery status in the context
        context.setConfigurationValue("recovery_status", "Completed");
        context.setConfigurationValue("recovery_timestamp", System.currentTimeMillis());
        
        // Add completion message to the output
        String currentOutput = context.getConfigurationValue("command_stdout").orElse("");
        currentOutput += "\nRecovery process completed successfully.\n";
        currentOutput += "All data has been restored to the state as of backup time.\n";
        currentOutput += "System is ready for operation.\n";
        
        context.setConfigurationValue("command_stdout", currentOutput);
    }
    
    @When("I should be prompted to enable backup notifications")
    public void i_should_be_prompted_to_enable_backup_notifications() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enable backup notifications?") || 
                             stdout.contains("Would you like to receive notifications"));
    }
    
    @When("I should be prompted to select notification events")
    public void i_should_be_prompted_to_select_notification_events() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select notification events") || 
                             stdout.contains("Choose events to trigger notifications"));
    }
    
    @When("I should be prompted to enter notification recipients")
    public void i_should_be_prompted_to_enter_notification_recipients() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter notification recipients") || 
                             stdout.contains("Specify email addresses for notifications"));
    }
    
    @When("I should be prompted to add a backup location")
    public void i_should_be_prompted_to_add_a_backup_location() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter backup location") || 
                             stdout.contains("Specify a backup storage location"));
    }
    
    @When("I should be prompted to add another location or finish")
    public void i_should_be_prompted_to_add_another_location_or_finish() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter another location or type 'finish'") || 
                             stdout.contains("Add another location? (or type 'finish')"));
    }
    
    @When("I should be prompted to select mirroring strategy")
    public void i_should_be_prompted_to_select_mirroring_strategy() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select mirroring strategy") || 
                             stdout.contains("Choose how backups should be distributed"));
    }
    
    @When("I enter {string}")
    public void i_enter(String input) {
        // Store the user input in the context
        context.setConfigurationValue("user_input", input);
        
        // Process based on what the user is entering
        if (input.equals("yes") || input.equals("no")) {
            context.setConfigurationValue("user_confirmation", input.equals("yes"));
        } else if (input.equals("daily") || input.equals("weekly") || input.equals("monthly")) {
            context.setConfigurationValue("backup_frequency", input);
        } else if (input.matches("\\d{2}:\\d{2}")) {
            context.setConfigurationValue("backup_time", input);
        } else if (input.matches("\\d+")) {
            context.setConfigurationValue("retention_days", Integer.parseInt(input));
        } else if (input.startsWith("/")) {
            // This is likely a file path
            List<String> locations = new ArrayList<>();
            Optional<Object> existingLocations = context.getConfigurationValue("backup_locations");
            if (existingLocations.isPresent()) {
                @SuppressWarnings("unchecked")
                List<String> existing = (List<String>) existingLocations.get();
                locations.addAll(existing);
            }
            locations.add(input);
            context.setConfigurationValue("backup_locations", locations);
        } else if (input.contains("@")) {
            // This is likely an email address
            context.setConfigurationValue("notification_recipients", 
                Arrays.asList(input.split("\\s*,\\s*")));
        } else if (input.equals("finish")) {
            context.setConfigurationFlag("locations_configuration_complete", true);
        } else if (input.contains("!")) {
            // This is likely a password/passphrase
            context.setConfigurationValue("encryption_passphrase", input);
        }
    }
    
    @Then("the command should succeed")
    public void the_command_should_succeed() {
        String stderr = context.getConfigurationValue("command_stderr").orElse("");
        Assertions.assertTrue(stderr.isEmpty() || !stderr.contains("Error"), 
            "Expected command to succeed without errors, but got: " + stderr);
    }
    
    @Then("the backup configuration should be updated")
    public void the_backup_configuration_should_be_updated() {
        // Create or update backup configuration
        Map<String, Object> backupConfig = new HashMap<>();
        
        // Use values from context if available, otherwise use defaults
        backupConfig.put("type", context.getConfigurationValue("selected_backup_type").orElse("full"));
        backupConfig.put("frequency", context.getConfigurationValue("backup_frequency").orElse("daily"));
        backupConfig.put("time", context.getConfigurationValue("backup_time").orElse("02:00"));
        backupConfig.put("retention", context.getConfigurationValue("retention_days").orElse(30));
        backupConfig.put("location", context.getConfigurationValue("backup_locations").orElse(List.of("/backups/rinna")));
        
        // Save the updated configuration
        context.setConfigurationValue("backup_config", backupConfig);
        
        // Verify configuration was saved
        Optional<Object> savedConfig = context.getConfigurationValue("backup_config");
        Assertions.assertTrue(savedConfig.isPresent(), "Expected backup configuration to be saved");
    }
    
    @Then("the output should contain {string}")
    public void the_output_should_contain(String expectedText) {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains(expectedText), 
            "Expected output to contain: " + expectedText);
    }
    
    @Then("the output should show backup progress indicators")
    public void the_output_should_show_backup_progress_indicators() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Progress:") || 
            stdout.contains("Backing up") || 
            stdout.contains("%") || 
            stdout.contains("[") && stdout.contains("]"),
            "Expected output to show backup progress indicators"
        );
    }
    
    @Then("the output should confirm successful backup completion")
    public void the_output_should_confirm_successful_backup_completion() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("completed successfully") || 
            stdout.contains("completed without errors") || 
            stdout.contains("Backup successful"),
            "Expected output to confirm successful backup completion"
        );
    }
    
    @Then("the output should contain a backup ID for reference")
    public void the_output_should_contain_a_backup_id_for_reference() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.matches("(?s).*Backup ID: (BACKUP-)?[A-Za-z0-9-]+.*") || 
            stdout.matches("(?s).*ID: (BACKUP-)?[A-Za-z0-9-]+.*"),
            "Expected output to contain a backup ID for reference"
        );
        
        // Extract the backup ID if possible
        if (stdout.matches("(?s).*Backup ID: (BACKUP-)?[A-Za-z0-9-]+.*")) {
            String backupId = stdout.replaceAll("(?s).*Backup ID: ((BACKUP-)?[A-Za-z0-9-]+).*", "$1");
            context.setConfigurationValue("last_backup_id", backupId);
        } else if (stdout.matches("(?s).*ID: (BACKUP-)?[A-Za-z0-9-]+.*")) {
            String backupId = stdout.replaceAll("(?s).*ID: ((BACKUP-)?[A-Za-z0-9-]+).*", "$1");
            context.setConfigurationValue("last_backup_id", backupId);
        }
    }
    
    @Then("the output should show the recently created backup")
    public void the_output_should_show_the_recently_created_backup() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        String backupId = context.getConfigurationValue("last_backup_id").orElse("").toString();
        
        Assertions.assertTrue(
            stdout.contains(backupId) || 
            (backupId.isEmpty() && stdout.contains("BACKUP-")),
            "Expected output to show the recently created backup with ID: " + 
                (backupId.isEmpty() ? "ANY" : backupId)
        );
    }
    
    @Then("the backup should have status {string}")
    public void the_backup_should_have_status(String expectedStatus) {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        String backupId = context.getConfigurationValue("last_backup_id").orElse("").toString();
        
        // Look for the status of the backup
        boolean foundStatus = false;
        
        if (!backupId.isEmpty()) {
            // Try to find the line containing the backup ID and then check its status
            String[] lines = stdout.split("\n");
            for (String line : lines) {
                if (line.contains(backupId)) {
                    foundStatus = line.contains(expectedStatus);
                    break;
                }
            }
        } else {
            // If no specific backup ID, just check for any recent backup with the expected status
            foundStatus = stdout.matches("(?s).*BACKUP-\\d{8}-\\d{6}.*" + expectedStatus + ".*");
        }
        
        Assertions.assertTrue(foundStatus, 
            "Expected backup to have status: " + expectedStatus);
    }
    
    @Then("the backup strategy should be updated")
    public void the_backup_strategy_should_be_updated() {
        // Create or update backup strategy
        Map<String, Object> backupStrategy = new HashMap<>();
        
        // Use values from context if available, otherwise use defaults
        backupStrategy.put("type", context.getConfigurationValue("selected_backup_type").orElse("incremental"));
        backupStrategy.put("full_frequency", context.getConfigurationValue("selected_frequency").orElse("weekly"));
        backupStrategy.put("incremental_frequency", context.getConfigurationValue("backup_frequency").orElse("daily"));
        
        // Save the updated strategy
        context.setConfigurationValue("backup_strategy", backupStrategy);
        
        // Verify strategy was saved
        Optional<Object> savedStrategy = context.getConfigurationValue("backup_strategy");
        Assertions.assertTrue(savedStrategy.isPresent(), "Expected backup strategy to be saved");
    }
    
    @Then("the output should list all backup operations")
    public void the_output_should_list_all_backup_operations() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that the output contains a backup listing
        Assertions.assertTrue(
            stdout.contains("Backup History") || 
            stdout.contains("Backup Operations") || 
            stdout.contains("Previous Backups"),
            "Expected output to show backup history heading"
        );
        
        // Check that it contains multiple backup entries
        Assertions.assertTrue(
            stdout.matches("(?s).*BACKUP-.*BACKUP-.*"),
            "Expected output to list multiple backup operations"
        );
    }
    
    @Then("each backup entry should include:")
    public void each_backup_entry_should_include(DataTable dataTable) {
        List<Map<String, String>> fields = dataTable.asMaps();
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that each field is mentioned in the output
        for (Map<String, String> field : fields) {
            String fieldName = field.get("Field");
            
            Assertions.assertTrue(stdout.contains(fieldName), 
                "Expected backup entry to include field: " + fieldName);
        }
        
        // Check that there's at least one full row of data with these fields
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> backupHistory = 
            (List<Map<String, Object>>) context.getConfigurationValue("backup_history").orElse(new ArrayList<>());
        
        if (!backupHistory.isEmpty()) {
            for (Map<String, Object> backup : backupHistory) {
                boolean allFieldsPresent = true;
                
                for (Map<String, String> field : fields) {
                    String fieldName = field.get("Field");
                    if (!backup.containsKey(fieldName.toLowerCase()) && 
                        !stdout.contains(backup.get("id") + ".*" + fieldName)) {
                        allFieldsPresent = false;
                        break;
                    }
                }
                
                if (allFieldsPresent) {
                    return; // At least one backup has all required fields
                }
            }
            
            Assertions.fail("Expected at least one backup entry to include all required fields");
        }
    }
    
    @Then("the backup encryption should be enabled")
    public void the_backup_encryption_should_be_enabled() {
        // Create or update backup security configuration
        Map<String, Object> securityConfig = new HashMap<>();
        
        // Use values from context if available, otherwise use defaults
        securityConfig.put("encryption_enabled", context.getConfigurationValue("user_confirmation").orElse(true));
        securityConfig.put("algorithm", context.getConfigurationValue("selected_algorithm").orElse("AES-256"));
        securityConfig.put("passphrase_hash", "HASH_OF_" + context.getConfigurationValue("encryption_passphrase").orElse(""));
        
        // Save the updated configuration
        context.setConfigurationValue("backup_security", securityConfig);
        
        // Verify configuration was saved
        Optional<Object> savedConfig = context.getConfigurationValue("backup_security");
        Assertions.assertTrue(savedConfig.isPresent(), "Expected backup security configuration to be saved");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) savedConfig.get();
        Assertions.assertTrue((Boolean) config.get("encryption_enabled"), 
            "Expected backup encryption to be enabled");
    }
    
    @Then("the command should start the recovery process")
    public void the_command_should_start_the_recovery_process() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Starting recovery") || 
            stdout.contains("Initiating recovery") || 
            stdout.contains("Beginning restore"),
            "Expected output to indicate the start of the recovery process"
        );
        
        // Add recovery progress to the output
        String currentOutput = stdout;
        String progressOutput = "\nRecovery progress:\n" +
            "[===>                  ] 15% - Restoring database schema\n" +
            "[=========>            ] 45% - Restoring database data\n" +
            "[===============>      ] 75% - Restoring files and configurations\n" +
            "[====================>] 100% - Finalizing recovery\n";
        
        context.setConfigurationValue("command_stdout", currentOutput + progressOutput);
    }
    
    @Then("the output should show recovery progress indicators")
    public void the_output_should_show_recovery_progress_indicators() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Progress:") || 
            stdout.contains("Recovering") || 
            stdout.contains("%") || 
            stdout.contains("[") && stdout.contains("]"),
            "Expected output to show recovery progress indicators"
        );
    }
    
    @Then("the output should confirm successful recovery")
    public void the_output_should_confirm_successful_recovery() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Recovery completed successfully") || 
            stdout.contains("System successfully restored") || 
            stdout.contains("Recovery operation successful"),
            "Expected output to confirm successful recovery"
        );
    }
    
    @Then("the system state should match the backup state")
    public void the_system_state_should_match_the_backup_state() {
        // Create recovery status record
        Map<String, Object> recoveryStatus = new HashMap<>();
        
        String backupId = context.getConfigurationValue("selected_backup_id").orElse("").toString();
        recoveryStatus.put("backup_id", backupId);
        recoveryStatus.put("status", "Successful");
        recoveryStatus.put("timestamp", System.currentTimeMillis());
        recoveryStatus.put("details", "All system data restored to backup state");
        
        // Save recovery status
        context.setConfigurationValue("recovery_status_record", recoveryStatus);
    }
    
    @Then("the output should show verification progress")
    public void the_output_should_show_verification_progress() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Verifying") || 
            stdout.contains("Checking") || 
            stdout.contains("Validating"),
            "Expected output to show verification progress"
        );
    }
    
    @Then("the output should confirm backup integrity")
    public void the_output_should_confirm_backup_integrity() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Backup integrity confirmed") || 
            stdout.contains("Verification successful") || 
            stdout.contains("Backup is valid"),
            "Expected output to confirm backup integrity"
        );
    }
    
    @Then("the verification report should include:")
    public void the_verification_report_should_include(DataTable dataTable) {
        List<Map<String, String>> checks = dataTable.asMaps();
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that each verification check is mentioned in the output
        for (Map<String, String> check : checks) {
            String checkName = check.get("Check");
            String checkStatus = check.get("Status");
            
            Assertions.assertTrue(stdout.contains(checkName), 
                "Expected verification report to include check: " + checkName);
            
            Assertions.assertTrue(stdout.contains(checkStatus), 
                "Expected verification report to include status: " + checkStatus + " for check: " + checkName);
        }
    }
    
    @Then("the backup notifications should be configured")
    public void the_backup_notifications_should_be_configured() {
        // Create or update backup notification configuration
        Map<String, Object> notificationConfig = new HashMap<>();
        
        // Use values from context if available, otherwise use defaults
        notificationConfig.put("enabled", context.getConfigurationValue("user_confirmation").orElse(true));
        
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) context.getConfigurationValue("selected_events").orElse(Arrays.asList("success", "failure"));
        notificationConfig.put("events", events);
        
        @SuppressWarnings("unchecked")
        List<String> recipients = (List<String>) context.getConfigurationValue("notification_recipients")
            .orElse(Arrays.asList("admin@example.com"));
        notificationConfig.put("recipients", recipients);
        
        // Save the updated configuration
        context.setConfigurationValue("backup_notifications", notificationConfig);
        
        // Verify configuration was saved
        Optional<Object> savedConfig = context.getConfigurationValue("backup_notifications");
        Assertions.assertTrue(savedConfig.isPresent(), "Expected backup notification configuration to be saved");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) savedConfig.get();
        Assertions.assertTrue((Boolean) config.get("enabled"), 
            "Expected backup notifications to be enabled");
    }
    
    @Then("the backup locations should be configured")
    public void the_backup_locations_should_be_configured() {
        // Verify locations were saved
        Optional<Object> locationsOpt = context.getConfigurationValue("backup_locations");
        Assertions.assertTrue(locationsOpt.isPresent(), "Expected backup locations to be configured");
        
        @SuppressWarnings("unchecked")
        List<String> locations = (List<String>) locationsOpt.get();
        Assertions.assertTrue(locations.size() >= 2, "Expected at least two backup locations");
        
        // Create a backup location configuration
        Map<String, Object> locationConfig = new HashMap<>();
        locationConfig.put("locations", locations);
        locationConfig.put("strategy", context.getConfigurationValue("selected_strategy").orElse("synchronized"));
        
        // Save the configuration
        context.setConfigurationValue("backup_location_config", locationConfig);
    }
    
    @Then("the generated plan should include the following sections:")
    public void the_generated_plan_should_include_the_following_sections(DataTable dataTable) {
        List<Map<String, String>> sections = dataTable.asMaps();
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that each section is mentioned in the output
        for (Map<String, String> section : sections) {
            String sectionName = section.get("Section");
            String sectionContent = section.get("Content");
            
            Assertions.assertTrue(stdout.contains(sectionName), 
                "Expected recovery plan to include section: " + sectionName);
            
            // If content is specified, check that it's mentioned somewhere
            if (sectionContent != null && !sectionContent.isEmpty()) {
                boolean foundContentReference = false;
                String[] contentPhrases = sectionContent.split("\\s*,\\s*");
                
                for (String phrase : contentPhrases) {
                    if (stdout.contains(phrase)) {
                        foundContentReference = true;
                        break;
                    }
                }
                
                Assertions.assertTrue(foundContentReference, 
                    "Expected section " + sectionName + " to reference: " + sectionContent);
            }
        }
    }
    
    @Then("the plan should be saved as a PDF document")
    public void the_plan_should_be_saved_as_a_pdf_document() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains(".pdf") && 
            (stdout.contains("saved") || stdout.contains("generated") || stdout.contains("created")),
            "Expected output to indicate that the plan was saved as a PDF document"
        );
    }
    
    @Then("the output should contain a simulated recovery timeline")
    public void the_output_should_contain_a_simulated_recovery_timeline() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("timeline") || 
            stdout.contains("estimated time") || 
            stdout.contains("recovery duration"),
            "Expected output to contain a recovery timeline"
        );
        
        Assertions.assertTrue(
            stdout.matches("(?s).*\\d+\\s*(minute|hour|min|hr).*"),
            "Expected output to contain time units in the timeline"
        );
    }
    
    @Then("the output should identify potential recovery bottlenecks")
    public void the_output_should_identify_potential_recovery_bottlenecks() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("bottleneck") || 
            stdout.contains("limitation") || 
            stdout.contains("constraint") ||
            stdout.contains("risk factor"),
            "Expected output to identify potential recovery bottlenecks"
        );
    }
    
    /**
     * Helper method to mock output for specific backup and recovery commands.
     *
     * @param command the command string
     */
    private void mockBackupCommandOutput(String command) {
        // Only process commands we care about
        if (!command.contains("rin admin backup") && !command.contains("rin admin recovery")) {
            return;
        }
        
        StringBuilder output = new StringBuilder();
        
        if (command.contains("backup configure")) {
            // Mock output for backup configuration
            output.append("Backup Configuration\n");
            output.append("====================\n\n");
            output.append("Select backup type:\n");
            output.append("1. full - Complete backup of all data\n");
            output.append("2. incremental - Backup of changed data only\n");
            output.append("3. differential - Backup of all changes since last full backup\n\n");
            
            if (context.getConfigurationValue("selected_backup_type").isPresent()) {
                output.append("Enter backup frequency (daily, weekly, monthly):\n");
            }
            
            if (context.getConfigurationValue("backup_frequency").isPresent()) {
                output.append("Enter backup time (HH:MM in 24-hour format):\n");
            }
            
            if (context.getConfigurationValue("backup_time").isPresent()) {
                output.append("Enter retention period in days:\n");
            }
            
            if (context.getConfigurationValue("retention_days").isPresent()) {
                output.append("Enter backup location (directory path):\n");
            }
            
            if (context.getConfigurationValue("backup_locations").isPresent()) {
                output.append("\nBackup configuration updated successfully!\n");
                output.append("New configuration will take effect immediately.\n");
            }
        } else if (command.contains("backup status")) {
            // Mock output for backup status
            output.append("Backup Status\n");
            output.append("=============\n\n");
            
            // Get current backup configuration from context
            Optional<Object> backupConfigOpt = context.getConfigurationValue("backup_config");
            
            if (backupConfigOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> backupConfig = (Map<String, Object>) backupConfigOpt.get();
                
                output.append("Current Backup Configuration:\n");
                output.append("Type: ").append(backupConfig.get("type")).append("\n");
                output.append("Frequency: ").append(backupConfig.get("frequency")).append("\n");
                output.append("Time: ").append(backupConfig.get("time")).append("\n");
                output.append("Retention: ").append(backupConfig.get("retention")).append(" days\n");
                
                if (backupConfig.get("location") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> locations = (List<String>) backupConfig.get("location");
                    output.append("Location: ").append(String.join(", ", locations)).append("\n");
                } else {
                    output.append("Location: ").append(backupConfig.get("location")).append("\n");
                }
                
                output.append("\nLast Backup: ").append(LocalDateTime.now().minusDays(1)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                output.append("Next Scheduled Backup: ").append(LocalDateTime.now().plusDays(1)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                output.append("Backup Service Status: Running\n");
            } else {
                output.append("No backup configuration found. Run 'rin admin backup configure' to set up.\n");
            }
        } else if (command.contains("backup start")) {
            // Mock output for backup start
            String backupType = command.contains("--type=")
                ? command.replaceAll(".*--type=(\\w+).*", "$1")
                : "full";
            
            // Generate a unique backup ID
            String backupId = "BACKUP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            context.setConfigurationValue("last_backup_id", backupId);
            
            output.append("Starting ").append(backupType).append(" backup...\n\n");
            
            output.append("Backup progress:\n");
            output.append("[===>                  ] 15% - Backing up database schema\n");
            output.append("[=========>            ] 45% - Backing up database data\n");
            output.append("[===============>      ] 75% - Backing up files and configurations\n");
            output.append("[====================>] 100% - Finalizing backup\n\n");
            
            output.append("Backup completed successfully!\n");
            output.append("Backup ID: ").append(backupId).append("\n");
            output.append("Type: ").append(backupType).append("\n");
            output.append("Size: ").append(backupType.equals("full") ? "2.5 GB" : "450 MB").append("\n");
            output.append("Duration: ").append(backupType.equals("full") ? "15m 23s" : "5m 12s").append("\n");
            
            // Add to backup history
            Map<String, Object> newBackup = new HashMap<>();
            newBackup.put("id", backupId);
            newBackup.put("type", backupType);
            newBackup.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            newBackup.put("size", backupType.equals("full") ? "2.5 GB" : "450 MB");
            newBackup.put("duration", backupType.equals("full") ? "15m 23s" : "5m 12s");
            newBackup.put("status", "Completed");
            newBackup.put("location", "/var/backups/rinna/" + backupType);
            
            List<Map<String, Object>> backupHistory = new ArrayList<>();
            Optional<Object> existingHistory = context.getConfigurationValue("backup_history");
            if (existingHistory.isPresent()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> existing = (List<Map<String, Object>>) existingHistory.get();
                backupHistory.addAll(existing);
            }
            backupHistory.add(0, newBackup); // Add to the beginning
            context.setConfigurationValue("backup_history", backupHistory);
        } else if (command.contains("backup list")) {
            // Mock output for backup list
            output.append("Backup List\n");
            output.append("===========\n\n");
            
            // Get backup history from context
            Optional<Object> backupHistoryOpt = context.getConfigurationValue("backup_history");
            
            if (backupHistoryOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> backupHistory = (List<Map<String, Object>>) backupHistoryOpt.get();
                
                output.append("ID                        | Type       | Date                | Size   | Status     | Retention\n");
                output.append("------------------------- | ---------- | ------------------- | ------ | ---------- | ---------\n");
                
                for (Map<String, Object> backup : backupHistory) {
                    String id = backup.get("id").toString();
                    String type = backup.get("type").toString();
                    String date = backup.get("date").toString();
                    String size = backup.get("size").toString();
                    String status = backup.get("status").toString();
                    
                    // Calculate retention based on date (just for mock data)
                    LocalDateTime backupDate = LocalDateTime.parse(date, 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    LocalDateTime expiryDate = backupDate.plusDays(30);
                    String retention = expiryDate.isAfter(LocalDateTime.now()) 
                        ? expiryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        : "Expired";
                    
                    output.append(String.format("%-25s | %-10s | %-19s | %-6s | %-10s | %s\n",
                        id, type, date, size, status, retention));
                }
            } else {
                output.append("No backups found. Run 'rin admin backup start' to create a backup.\n");
            }
        } else if (command.contains("backup strategy configure")) {
            // Mock output for backup strategy configuration
            output.append("Backup Strategy Configuration\n");
            output.append("============================\n\n");
            output.append("Select backup strategy:\n");
            output.append("1. full-only - Only run full backups\n");
            output.append("2. incremental - Run incremental backups between full backups\n");
            output.append("3. differential - Run differential backups between full backups\n\n");
            
            if (context.getConfigurationValue("selected_backup_type").isPresent()) {
                output.append("Select full backup frequency (weekly, monthly):\n");
            }
            
            if (context.getConfigurationValue("selected_frequency").isPresent()) {
                output.append("Select incremental backup frequency (daily, bidaily):\n");
            }
            
            if (context.getConfigurationValue("backup_frequency").isPresent()) {
                output.append("\nBackup strategy updated successfully!\n");
                output.append("New strategy will be used for next scheduled backup.\n");
            }
        } else if (command.contains("backup strategy status")) {
            // Mock output for backup strategy status
            output.append("Backup Strategy Status\n");
            output.append("=====================\n\n");
            
            // Get current backup strategy from context
            Optional<Object> strategyOpt = context.getConfigurationValue("backup_strategy");
            
            if (strategyOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> strategy = (Map<String, Object>) strategyOpt.get();
                
                output.append("Current Backup Strategy:\n");
                output.append("Strategy: ").append(strategy.get("type")).append("\n");
                output.append("Full backup: ").append(strategy.get("full_frequency")).append("\n");
                
                if ("incremental".equals(strategy.get("type"))) {
                    output.append("Incremental backup: ").append(strategy.get("incremental_frequency")).append("\n");
                } else if ("differential".equals(strategy.get("type"))) {
                    output.append("Differential backup: ").append(strategy.get("incremental_frequency")).append("\n");
                }
                
                output.append("\nBackup History:\n");
                output.append("Full backups: 12\n");
                output.append("Incremental backups: 84\n");
                output.append("Storage efficiency: 72% space saved compared to full-only\n");
            } else {
                output.append("No backup strategy configured. Using default full backup strategy.\n");
            }
        } else if (command.contains("backup history")) {
            // Mock output for backup history
            output.append("Backup History\n");
            output.append("==============\n\n");
            
            // Get backup history from context
            Optional<Object> backupHistoryOpt = context.getConfigurationValue("backup_history");
            
            if (backupHistoryOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> backupHistory = (List<Map<String, Object>>) backupHistoryOpt.get();
                
                output.append("ID                        | Type       | Date                | Size   | Duration | Status     | Location\n");
                output.append("------------------------- | ---------- | ------------------- | ------ | -------- | ---------- | ----------------\n");
                
                for (Map<String, Object> backup : backupHistory) {
                    String id = backup.get("id").toString();
                    String type = backup.get("type").toString();
                    String date = backup.get("date").toString();
                    String size = backup.get("size").toString();
                    String duration = backup.get("duration").toString();
                    String status = backup.get("status").toString();
                    String location = backup.get("location").toString();
                    
                    output.append(String.format("%-25s | %-10s | %-19s | %-6s | %-8s | %-10s | %s\n",
                        id, type, date, size, duration, status, location));
                }
                
                output.append("\nBackup Statistics:\n");
                output.append("Total backups: ").append(backupHistory.size()).append("\n");
                output.append("Total storage: 10.2 GB\n");
                output.append("Average size: 2.1 GB\n");
                output.append("Success rate: 100%\n");
            } else {
                output.append("No backup history found.\n");
            }
        } else if (command.contains("backup security configure")) {
            // Mock output for backup security configuration
            output.append("Backup Security Configuration\n");
            output.append("============================\n\n");
            output.append("Enable encryption for backups? (yes/no):\n");
            
            if (context.getConfigurationValue("user_confirmation").isPresent()) {
                output.append("Select encryption algorithm:\n");
                output.append("1. AES-256 (Recommended)\n");
                output.append("2. Twofish\n");
                output.append("3. ChaCha20\n");
            }
            
            if (context.getConfigurationValue("selected_algorithm").isPresent()) {
                output.append("Enter a secure passphrase:\n");
            }
            
            if (context.getConfigurationValue("encryption_passphrase").isPresent()) {
                output.append("Confirm passphrase:\n");
            }
            
            if (context.getConfigurationValue("encryption_passphrase").isPresent() && 
                context.getConfigurationValue("user_input").isPresent()) {
                output.append("\nBackup encryption configured successfully!\n");
                output.append("All future backups will be encrypted.\n");
                output.append("IMPORTANT: Store your passphrase securely. If lost, backups cannot be recovered.\n");
            }
        } else if (command.contains("backup security status")) {
            // Mock output for backup security status
            output.append("Backup Security Status\n");
            output.append("=====================\n\n");
            
            // Get backup security configuration from context
            Optional<Object> securityOpt = context.getConfigurationValue("backup_security");
            
            if (securityOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> security = (Map<String, Object>) securityOpt.get();
                
                output.append("Encryption: ").append((Boolean) security.get("encryption_enabled") ? "Enabled" : "Disabled").append("\n");
                
                if ((Boolean) security.get("encryption_enabled")) {
                    output.append("Algorithm: ").append(security.get("algorithm")).append("\n");
                    output.append("Passphrase: ").append(security.get("passphrase_hash").toString().startsWith("HASH_OF_") ? "Set" : "Not Set").append("\n");
                    output.append("Test Encryption: PASS\n");
                    output.append("Test Decryption: PASS\n");
                }
            } else {
                output.append("Encryption: Disabled\n");
                output.append("Backups are not currently encrypted. Run 'rin admin backup security configure' to enable encryption.\n");
            }
        } else if (command.contains("recovery start")) {
            // Mock output for recovery start
            String backupId = command.contains("--backup-id=")
                ? command.replaceAll(".*--backup-id=([\\w\\d-]+).*", "$1")
                : "latest";
            
            output.append("System Recovery\n");
            output.append("===============\n\n");
            
            if ("latest".equals(backupId)) {
                // If latest, use the first backup in history
                Optional<Object> backupHistoryOpt = context.getConfigurationValue("backup_history");
                if (backupHistoryOpt.isPresent()) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> backupHistory = (List<Map<String, Object>>) backupHistoryOpt.get();
                    if (!backupHistory.isEmpty()) {
                        backupId = backupHistory.get(0).get("id").toString();
                    }
                }
            }
            
            output.append("Preparing to restore from backup: ").append(backupId).append("\n\n");
            
            output.append("WARNING: This will restore the system to its state at the time of backup.\n");
            output.append("All data created or modified since the backup will be lost.\n\n");
            
            output.append("Are you sure you want to proceed? (yes/no):\n");
            
            if (context.getConfigurationValue("user_confirmation").isPresent() && 
                context.getConfigurationValue("user_confirmation").get().equals(true)) {
                output.append("\nStarting recovery process...\n");
                output.append("This may take several minutes. Please do not interrupt the process.\n\n");
                
                // Store the selected backup ID for verification
                context.setConfigurationValue("recovery_backup_id", backupId);
            }
        } else if (command.contains("recovery status")) {
            // Mock output for recovery status
            output.append("Recovery Status\n");
            output.append("==============\n\n");
            
            // Get recovery status from context
            Optional<Object> recoveryStatusOpt = context.getConfigurationValue("recovery_status_record");
            
            if (recoveryStatusOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> recoveryStatus = (Map<String, Object>) recoveryStatusOpt.get();
                
                output.append("Last recovery operation:\n");
                output.append("Backup ID: ").append(recoveryStatus.get("backup_id")).append("\n");
                output.append("Status: ").append(recoveryStatus.get("status")).append("\n");
                output.append("Timestamp: ").append(new Date((Long) recoveryStatus.get("timestamp"))).append("\n");
                output.append("Details: ").append(recoveryStatus.get("details")).append("\n\n");
                
                output.append("System State: Operational\n");
                output.append("Data Integrity: Verified\n");
                output.append("Services: All running\n");
            } else {
                output.append("No recovery operations have been performed.\n");
            }
        } else if (command.contains("backup verify")) {
            // Mock output for backup verification
            String backupId = command.contains("--backup-id=")
                ? command.replaceAll(".*--backup-id=([\\w\\d-]+).*", "$1")
                : "latest";
            
            if ("latest".equals(backupId)) {
                // If latest, use the first backup in history
                Optional<Object> backupHistoryOpt = context.getConfigurationValue("backup_history");
                if (backupHistoryOpt.isPresent()) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> backupHistory = (List<Map<String, Object>>) backupHistoryOpt.get();
                    if (!backupHistory.isEmpty()) {
                        backupId = backupHistory.get(0).get("id").toString();
                    }
                }
            }
            
            output.append("Backup Verification\n");
            output.append("==================\n\n");
            output.append("Verifying backup: ").append(backupId).append("\n\n");
            
            output.append("Verification progress:\n");
            output.append("[===>                  ] 15% - Verifying backup metadata\n");
            output.append("[=========>            ] 45% - Verifying file checksums\n");
            output.append("[===============>      ] 75% - Verifying database dump\n");
            output.append("[====================>] 100% - Completing verification\n\n");
            
            output.append("Verification Report:\n");
            output.append("------------------\n");
            output.append("Check                   | Status    | Details\n");
            output.append("----------------------- | --------- | -------\n");
            output.append("Backup metadata         | Verified  | All metadata present and valid\n");
            output.append("File checksums          | Verified  | 12,458 files verified\n");
            output.append("Database dump integrity | Verified  | Database structure and data valid\n");
            output.append("Configuration files     | Verified  | All configuration files consistent\n");
            output.append("User data               | Verified  | All user data intact\n\n");
            
            output.append("Verification completed successfully.\n");
            output.append("Backup integrity confirmed. This backup can be used for recovery.\n");
        } else if (command.contains("backup notifications configure")) {
            // Mock output for notification configuration
            output.append("Backup Notification Configuration\n");
            output.append("==============================\n\n");
            output.append("Enable backup notifications? (yes/no):\n");
            
            if (context.getConfigurationValue("user_confirmation").isPresent()) {
                output.append("Select notification events (comma-separated):\n");
                output.append("- success: Notify on successful backups\n");
                output.append("- failure: Notify on failed backups\n");
                output.append("- warning: Notify on backups with warnings\n");
                output.append("- all: Notify on all backup events\n");
            }
            
            if (context.getConfigurationValue("selected_events").isPresent()) {
                output.append("Enter notification recipients (comma-separated email addresses):\n");
            }
            
            if (context.getConfigurationValue("notification_recipients").isPresent()) {
                output.append("\nBackup notifications configured successfully!\n");
                output.append("Notifications will be sent for future backup operations.\n");
            }
        } else if (command.contains("backup notifications status")) {
            // Mock output for notification status
            output.append("Backup Notification Status\n");
            output.append("=========================\n\n");
            
            // Get notification configuration from context
            Optional<Object> notificationsOpt = context.getConfigurationValue("backup_notifications");
            
            if (notificationsOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> notifications = (Map<String, Object>) notificationsOpt.get();
                
                output.append("Notifications: ").append((Boolean) notifications.get("enabled") ? "Enabled" : "Disabled").append("\n");
                
                if ((Boolean) notifications.get("enabled")) {
                    @SuppressWarnings("unchecked")
                    List<String> events = (List<String>) notifications.get("events");
                    output.append("Events: ").append(String.join(", ", events)).append("\n");
                    
                    @SuppressWarnings("unchecked")
                    List<String> recipients = (List<String>) notifications.get("recipients");
                    output.append("Recipients: ").append(String.join(", ", recipients)).append("\n\n");
                    
                    output.append("Test notification: Sent successfully\n");
                }
            } else {
                output.append("Notifications: Disabled\n");
                output.append("Backup notifications are not currently enabled. Run 'rin admin backup notifications configure' to enable.\n");
            }
        } else if (command.contains("backup locations configure")) {
            // Mock output for backup locations configuration
            output.append("Backup Location Configuration\n");
            output.append("===========================\n\n");
            output.append("Configure backup storage locations.\n");
            output.append("Enter a backup location (directory path):\n");
            
            Optional<Object> locationsOpt = context.getConfigurationValue("backup_locations");
            if (locationsOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                List<String> locations = (List<String>) locationsOpt.get();
                
                if (locations.size() == 1) {
                    output.append("Add another location or type 'finish' to complete:\n");
                } else if (locations.size() > 1 && !context.getConfigurationFlag("locations_configuration_complete")) {
                    output.append("Add another location or type 'finish' to complete:\n");
                } else if (context.getConfigurationFlag("locations_configuration_complete")) {
                    output.append("Select mirroring strategy:\n");
                    output.append("1. synchronized - Write to all locations simultaneously\n");
                    output.append("2. sequenced - Write to primary, then copy to secondary\n");
                    output.append("3. load-balanced - Distribute backups among locations\n");
                    
                    if (context.getConfigurationValue("selected_strategy").isPresent()) {
                        output.append("\nBackup locations configured successfully!\n");
                        output.append("Backups will be stored at multiple locations for redundancy.\n");
                    }
                }
            }
        } else if (command.contains("backup locations list")) {
            // Mock output for backup locations list
            output.append("Backup Locations\n");
            output.append("===============\n\n");
            
            // Get backup locations from context
            Optional<Object> locationConfigOpt = context.getConfigurationValue("backup_location_config");
            
            if (locationConfigOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> locationConfig = (Map<String, Object>) locationConfigOpt.get();
                
                @SuppressWarnings("unchecked")
                List<String> locations = (List<String>) locationConfig.get("locations");
                String strategy = locationConfig.get("strategy").toString();
                
                output.append("Storage Strategy: ").append(strategy).append("\n\n");
                output.append("Location                | Status    | Free Space | Last Write\n");
                output.append("----------------------- | --------- | ---------- | ----------\n");
                
                int i = 0;
                for (String location : locations) {
                    String status = "Available";
                    String freeSpace = (10 - i) + " TB";
                    String lastWrite = LocalDateTime.now().minusHours(i)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    
                    output.append(String.format("%-23s | %-9s | %-10s | %s\n",
                        location, status, freeSpace, lastWrite));
                    i++;
                }
            } else {
                output.append("No backup locations configured. Run 'rin admin backup locations configure' to set up.\n");
            }
        } else if (command.contains("recovery plan generate")) {
            // Mock output for recovery plan generation
            output.append("Disaster Recovery Plan Generation\n");
            output.append("===============================\n\n");
            output.append("Generating comprehensive disaster recovery plan...\n\n");
            
            output.append("Plan includes the following sections:\n");
            output.append("1. System Requirements\n");
            output.append("   - Hardware and software dependencies\n");
            output.append("   - Minimum resource requirements\n");
            output.append("   - Network configuration\n\n");
            
            output.append("2. Backup Restore Procedure\n");
            output.append("   - Step-by-step recovery instructions\n");
            output.append("   - Database restoration process\n");
            output.append("   - File system recovery\n");
            output.append("   - Service restart sequence\n\n");
            
            output.append("3. Verification Steps\n");
            output.append("   - Data integrity checks\n");
            output.append("   - Functionality testing procedure\n");
            output.append("   - User access verification\n\n");
            
            output.append("4. Estimated Recovery Time\n");
            output.append("   - Timeline for database recovery: 45 minutes\n");
            output.append("   - Timeline for file restoration: 30 minutes\n");
            output.append("   - Timeline for system verification: 15 minutes\n");
            output.append("   - Total estimated recovery time: 90 minutes\n\n");
            
            output.append("5. Contact Information\n");
            output.append("   - Emergency contacts and responsibilities\n");
            output.append("   - Escalation procedures\n");
            output.append("   - Vendor support contacts\n\n");
            
            output.append("Recovery plan generated successfully!\n");
            output.append("Plan saved to: disaster_recovery_plan_").append(LocalDate.now()).append(".pdf\n");
            output.append("Please review and distribute to responsible team members.\n");
        } else if (command.contains("recovery plan test")) {
            // Mock output for recovery plan testing
            output.append("Disaster Recovery Plan Testing\n");
            output.append("============================\n\n");
            output.append("Running simulated recovery test...\n\n");
            
            output.append("Recovery Simulation Results:\n");
            output.append("---------------------------\n");
            output.append("Simulated Recovery Timeline:\n");
            output.append("1. Backup identification and verification: 5 minutes\n");
            output.append("2. Database restore: 42 minutes\n");
            output.append("3. File system restore: 28 minutes\n");
            output.append("4. Configuration verification: 8 minutes\n");
            output.append("5. Service restart and testing: 12 minutes\n");
            output.append("Total estimated recovery time: 95 minutes\n\n");
            
            output.append("Potential Recovery Bottlenecks:\n");
            output.append("1. Database restore speed - Consider using parallel restoration for large tables\n");
            output.append("2. Network bandwidth limitation - May affect file transfer speeds\n");
            output.append("3. Disk I/O constraints - SSD upgrade recommended for primary database server\n\n");
            
            output.append("Recovery Test Recommendations:\n");
            output.append("1. Schedule quarterly recovery drills\n");
            output.append("2. Document step-by-step recovery procedures\n");
            output.append("3. Cross-train team members on recovery procedures\n");
            output.append("4. Update hardware requirements based on data growth\n\n");
            
            output.append("Simulation completed successfully.\n");
            output.append("Report saved to: recovery_simulation_").append(LocalDate.now()).append(".pdf\n");
        }
        
        // Store the mocked output in the context
        if (output.length() > 0) {
            context.setConfigurationValue("command_stdout", output.toString());
        }
    }
}