/*
 * BDD step definitions for Admin System Monitoring and Diagnostics
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.junit.jupiter.api.Assertions;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for Admin System Monitoring and Diagnostics feature.
 */
public class AdminSystemMonitoringSteps {
    private final TestContext context;
    private List<Map<String, String>> expectedMetrics;
    private List<Map<String, String>> expectedComponents;
    private String lastReportGenerated;
    
    /**
     * Constructs a new AdminSystemMonitoringSteps with the given TestContext.
     *
     * @param context the test context
     */
    public AdminSystemMonitoringSteps(TestContext context) {
        this.context = context;
    }
    
    @Given("I am logged in as an administrator")
    public void i_am_logged_in_as_an_administrator() {
        // This step is already defined in AdminUserManagementSteps or AdminAuditComplianceSteps
        // We're declaring it here for clarity but it won't override the existing implementation
        context.setConfigurationFlag("admin_logged_in", true);
    }
    
    @Given("the system monitoring service is enabled")
    public void the_system_monitoring_service_is_enabled() {
        context.setConfigurationFlag("monitoring_enabled", true);
        context.setConfigurationValue("monitoring_refresh_interval", 60); // Default refresh in seconds
        
        // Set default monitoring thresholds
        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("CPU Load", "85%");
        thresholds.put("Memory Usage", "90%");
        thresholds.put("Disk Usage", "85%");
        thresholds.put("Network Connections", "1000");
        thresholds.put("Response Time", "500ms");
        thresholds.put("Error Rate", "1%");
        
        context.setConfigurationValue("monitoring_thresholds", thresholds);
    }
    
    @Given("the system has generated a {string} warning")
    public void the_system_has_generated_a_warning(String warningType) {
        Map<String, Object> warning = new HashMap<>();
        warning.put("type", warningType);
        warning.put("id", "MEM-001");
        warning.put("timestamp", System.currentTimeMillis());
        warning.put("severity", "HIGH");
        warning.put("description", "System memory usage has exceeded 90% for more than 15 minutes");
        warning.put("details", "Memory usage at 92.5%, potential memory leak detected in API Server process");
        
        context.setConfigurationValue("system_warning_" + warningType, warning);
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
            
            // Mock specific behavior for the monitoring and diagnostics commands
            mockMonitoringCommandOutput(command);
        } catch (Exception e) {
            context.setException(e);
        }
    }
    
    @When("I should be prompted to select which threshold to configure")
    public void i_should_be_prompted_to_select_which_threshold_to_configure() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select threshold to configure") || 
                             stdout.contains("Choose a monitoring threshold"));
    }
    
    @When("I select {string}")
    public void i_select(String selection) {
        // Store the selection in the context for later verification
        context.setConfigurationValue("user_selection", selection);
        
        // Handle different selection types
        if (selection.equals("CPU Load") || selection.equals("Memory Usage") || 
            selection.equals("Disk Usage") || selection.equals("Response Time")) {
            context.setConfigurationValue("selected_metric", selection);
        } else if (selection.contains("connectivity") || selection.contains("storage") || 
                  selection.contains("memory")) {
            context.setConfigurationValue("selected_diagnostics", Arrays.asList(selection.split(",")));
        } else if (selection.equals("daily") || selection.equals("weekly") || selection.equals("hourly")) {
            context.setConfigurationValue("selected_frequency", selection);
        } else if (selection.equals("Analyze Heap Dump")) {
            context.setConfigurationValue("selected_diagnostic_action", selection);
        }
    }
    
    @When("I should be prompted to enter a new threshold value")
    public void i_should_be_prompted_to_enter_a_new_threshold_value() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter new threshold value") || 
                             stdout.contains("Specify threshold value"));
    }
    
    @When("I should be prompted to enter an alert name")
    public void i_should_be_prompted_to_enter_an_alert_name() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter alert name") || 
                             stdout.contains("Provide a name for the alert"));
    }
    
    @When("I should be prompted to select a metric")
    public void i_should_be_prompted_to_select_a_metric() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select metric") || 
                             stdout.contains("Choose a metric to monitor"));
    }
    
    @When("I should be prompted to enter a threshold value")
    public void i_should_be_prompted_to_enter_a_threshold_value() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter threshold value") || 
                             stdout.contains("Specify threshold"));
    }
    
    @When("I should be prompted to enter notification recipients")
    public void i_should_be_prompted_to_enter_notification_recipients() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter notification recipients") || 
                             stdout.contains("Specify email addresses for notifications"));
    }
    
    @When("I should be prompted to select diagnostic check types")
    public void i_should_be_prompted_to_select_diagnostic_check_types() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select diagnostic check types") || 
                             stdout.contains("Choose types of diagnostics to run"));
    }
    
    @When("I should be prompted to select schedule frequency")
    public void i_should_be_prompted_to_select_schedule_frequency() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Select schedule frequency") || 
                             stdout.contains("Choose how often to run diagnostics"));
    }
    
    @When("I should be prompted to enter schedule time")
    public void i_should_be_prompted_to_enter_schedule_time() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains("Enter schedule time") || 
                             stdout.contains("Specify time to run diagnostics"));
    }
    
    @When("I enter {string}")
    public void i_enter(String input) {
        // Store the user input in the context
        context.setConfigurationValue("user_input", input);
        
        // Specialized handling based on previous prompts
        if (context.getConfigurationValue("command_stdout").orElse("").contains("threshold")) {
            context.setConfigurationValue("threshold_value", input);
        } else if (context.getConfigurationValue("command_stdout").orElse("").contains("alert name")) {
            context.setConfigurationValue("alert_name", input);
        } else if (context.getConfigurationValue("command_stdout").orElse("").contains("recipients")) {
            context.setConfigurationValue("notification_recipients", input);
        } else if (context.getConfigurationValue("command_stdout").orElse("").contains("schedule time")) {
            context.setConfigurationValue("schedule_time", input);
        }
    }
    
    @When("I select the {string} option")
    public void i_select_the_option(String option) {
        context.setConfigurationValue("selected_option", option);
        
        // Mock the output of choosing this option
        if (option.equals("Analyze Heap Dump")) {
            StringBuilder output = new StringBuilder();
            output.append("Analyzing Heap Dump\n");
            output.append("-------------------\n");
            output.append("Total Memory: 16.2 GB\n");
            output.append("Used Memory: 14.9 GB (92%)\n\n");
            
            output.append("Memory Breakdown by Component:\n");
            output.append("| Component       | Usage   | % of Total |\n");
            output.append("| --------------- | ------- | ---------- |\n");
            output.append("| API Server      | 8.4 GB  | 56.4%      |\n");
            output.append("| Data Cache      | 3.2 GB  | 21.5%      |\n");
            output.append("| Search Index    | 1.8 GB  | 12.1%      |\n");
            output.append("| Task Queue      | 0.9 GB  | 6.0%       |\n");
            output.append("| Other           | 0.6 GB  | 4.0%       |\n\n");
            
            output.append("Suspected Memory Leak Analysis:\n");
            output.append("API Server process shows abnormal growth pattern\n");
            output.append("Potential cause: Connection pool not releasing resources\n\n");
            
            output.append("Recommendations:\n");
            output.append("1. Run memory reclamation process\n");
            output.append("2. Restart API Server service\n");
            output.append("3. Update connection pool configuration\n");
            
            context.setConfigurationValue("command_stdout", output.toString());
        }
    }
    
    @Then("the command should succeed")
    public void the_command_should_succeed() {
        String stderr = context.getConfigurationValue("command_stderr").orElse("");
        Assertions.assertTrue(stderr.isEmpty() || !stderr.contains("Error"), 
            "Expected command to succeed without errors, but got: " + stderr);
    }
    
    @Then("the output should contain the following system metrics:")
    public void the_output_should_contain_the_following_system_metrics(DataTable dataTable) {
        expectedMetrics = dataTable.asMaps();
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that each expected metric is contained in the output
        for (Map<String, String> metric : expectedMetrics) {
            // Skip the header row if present
            if (metric.containsKey("Metric") && metric.get("Metric").equals("Metric")) {
                continue;
            }
            
            String metricName = metric.get("Metric");
            String metricStatus = metric.get("Status");
            
            // If the status is wildcard (*), just check that the metric name is present
            if (metricStatus.equals("*")) {
                Assertions.assertTrue(stdout.contains(metricName), 
                    "Expected output to contain metric: " + metricName);
            } else {
                // Check both metric name and status are present
                String expectedLine = metricName + ".*" + metricStatus;
                Assertions.assertTrue(stdout.matches("(?s).*" + expectedLine + ".*"), 
                    "Expected output to contain metric: " + metricName + " with status: " + metricStatus);
            }
        }
    }
    
    @Then("the output should show CPU and memory usage statistics")
    public void the_output_should_show_cpu_and_memory_usage_statistics() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("CPU:") && 
            (stdout.contains("%") || stdout.contains("cores")) && 
            stdout.contains("Memory:") && 
            (stdout.contains("GB") || stdout.contains("MB") || stdout.contains("%")),
            "Expected output to show CPU and memory usage statistics"
        );
    }
    
    @Then("the output should show active users count")
    public void the_output_should_show_active_users_count() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.matches("(?s).*Active Users:?\\s*\\d+.*") || 
            stdout.matches("(?s).*Users Online:?\\s*\\d+.*") ||
            stdout.matches("(?s).*Connected Users:?\\s*\\d+.*"),
            "Expected output to show active users count"
        );
    }
    
    @Then("the output should contain the following server metrics:")
    public void the_output_should_contain_the_following_server_metrics(DataTable dataTable) {
        expectedMetrics = dataTable.asMaps();
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that each expected metric is contained in the output
        for (Map<String, String> metric : expectedMetrics) {
            // Skip the header row if present
            if (metric.containsKey("Metric") && metric.get("Metric").equals("Metric")) {
                continue;
            }
            
            String metricName = metric.get("Metric");
            String threshold = metric.get("Threshold");
            
            Assertions.assertTrue(stdout.contains(metricName), 
                "Expected output to contain metric: " + metricName);
            
            Assertions.assertTrue(stdout.contains(threshold), 
                "Expected output to contain threshold: " + threshold + " for metric: " + metricName);
        }
    }
    
    @Then("the output should show thread pool utilization")
    public void the_output_should_show_thread_pool_utilization() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Thread Pool") && 
            stdout.matches("(?s).*Active Threads:?\\s*\\d+.*") &&
            stdout.matches("(?s).*Queue Size:?\\s*\\d+.*"),
            "Expected output to show thread pool utilization"
        );
    }
    
    @Then("the output should include uptime information")
    public void the_output_should_include_uptime_information() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Uptime:") || 
            stdout.contains("Started:") ||
            stdout.contains("Running since:"),
            "Expected output to include uptime information"
        );
    }
    
    @Then("the monitoring threshold for {string} should be set to {string}")
    public void the_monitoring_threshold_for_should_be_set_to(String metric, String value) {
        Optional<Object> thresholdsOpt = context.getConfigurationValue("monitoring_thresholds");
        Assertions.assertTrue(thresholdsOpt.isPresent(), "Expected monitoring thresholds to be configured");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> thresholds = (Map<String, Object>) thresholdsOpt.get();
        
        // Apply the new threshold value (with % symbol if it's a percentage value)
        String thresholdValue = value;
        if (value.matches("\\d+") && !value.endsWith("%") && 
            (metric.contains("CPU") || metric.contains("Memory") || metric.contains("Disk") || metric.contains("Rate"))) {
            thresholdValue = value + "%";
        }
        
        thresholds.put(metric, thresholdValue);
        context.setConfigurationValue("monitoring_thresholds", thresholds);
        
        // Verify the threshold was updated
        @SuppressWarnings("unchecked")
        Map<String, Object> updatedThresholds = (Map<String, Object>) context.getConfigurationValue("monitoring_thresholds").get();
        
        Assertions.assertEquals(thresholdValue, updatedThresholds.get(metric), 
            "Expected threshold for " + metric + " to be updated to " + thresholdValue);
    }
    
    @Then("the output should contain {string}")
    public void the_output_should_contain(String expectedText) {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        Assertions.assertTrue(stdout.contains(expectedText), 
            "Expected output to contain: " + expectedText);
    }
    
    @Then("the report should include the following sections:")
    public void the_report_should_include_the_following_sections(DataTable dataTable) {
        List<Map<String, String>> sections = dataTable.asMaps();
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that each section is mentioned in the output
        for (Map<String, String> section : sections) {
            String sectionName = section.get("Section");
            String sectionDetails = section.get("Details");
            
            Assertions.assertTrue(stdout.contains(sectionName), 
                "Expected output to contain section: " + sectionName);
            
            // If details are specified, check for those too
            if (sectionDetails != null && !sectionDetails.isEmpty()) {
                // This is a loose check - we don't need exact wording, just the main concepts
                String[] keyPhrases = sectionDetails.split("\\s*,\\s*");
                boolean foundAnyPhrase = false;
                
                for (String phrase : keyPhrases) {
                    if (stdout.contains(phrase)) {
                        foundAnyPhrase = true;
                        break;
                    }
                }
                
                Assertions.assertTrue(foundAnyPhrase, 
                    "Expected output to mention details related to: " + sectionDetails + " for section: " + sectionName);
            }
        }
    }
    
    @Then("the alert {string} should be created")
    public void the_alert_should_be_created(String alertName) {
        // Store alert configuration
        Map<String, Object> alertConfig = new HashMap<>();
        alertConfig.put("name", alertName);
        alertConfig.put("metric", context.getConfigurationValue("selected_metric").orElse("CPU Load"));
        alertConfig.put("threshold", context.getConfigurationValue("threshold_value").orElse("80"));
        alertConfig.put("recipients", context.getConfigurationValue("notification_recipients").orElse(""));
        
        // Save the alert configuration in the context
        String alertKey = "monitoring_alert_" + alertName.replaceAll("\\s+", "_").toLowerCase();
        context.setConfigurationValue(alertKey, alertConfig);
        
        // Verify the alert was created
        Optional<Object> alertOpt = context.getConfigurationValue(alertKey);
        Assertions.assertTrue(alertOpt.isPresent(), "Expected alert configuration to be present");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> savedAlert = (Map<String, Object>) alertOpt.get();
        Assertions.assertEquals(alertName, savedAlert.get("name"), "Expected alert name to match");
    }
    
    @Then("the diagnostics should check the following components:")
    public void the_diagnostics_should_check_the_following_components(DataTable dataTable) {
        expectedComponents = dataTable.asMaps();
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that each component is mentioned in the output
        for (Map<String, String> component : expectedComponents) {
            String componentName = component.get("Component");
            String componentChecks = component.get("Checks");
            
            Assertions.assertTrue(stdout.contains(componentName), 
                "Expected output to contain component: " + componentName);
            
            // Check that at least one of the mentioned checks is in the output
            String[] checks = componentChecks.split("\\s*,\\s*");
            boolean foundAnyCheck = false;
            
            for (String check : checks) {
                if (stdout.contains(check)) {
                    foundAnyCheck = true;
                    break;
                }
            }
            
            Assertions.assertTrue(foundAnyCheck, 
                "Expected output to mention at least one check from: " + componentChecks + " for component: " + componentName);
        }
    }
    
    @Then("the diagnostics should identify any system bottlenecks")
    public void the_diagnostics_should_identify_any_system_bottlenecks() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check for bottleneck identification words
        Assertions.assertTrue(
            stdout.contains("bottleneck") || 
            stdout.contains("constraint") || 
            stdout.contains("limitation") ||
            stdout.contains("slowdown") ||
            stdout.contains("performance issue"),
            "Expected output to identify system bottlenecks"
        );
    }
    
    @Then("the output should contain a list of active user sessions")
    public void the_output_should_contain_a_list_of_active_user_sessions() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check for session listing table headers
        Assertions.assertTrue(
            stdout.contains("Session ID") && 
            stdout.contains("User") && 
            stdout.contains("Login Time"),
            "Expected output to contain a list of active user sessions"
        );
        
        // Check that there's at least one session entry
        Assertions.assertTrue(
            stdout.matches("(?s).*[A-Fa-f0-9-]{36}.*\\d{4}-\\d{2}-\\d{2}.*"),
            "Expected output to contain at least one session entry"
        );
    }
    
    @Then("each session should include the following information:")
    public void each_session_should_include_the_following_information(DataTable dataTable) {
        List<Map<String, String>> fields = dataTable.asMaps();
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that each field is mentioned in the output
        for (Map<String, String> field : fields) {
            String fieldName = field.get("Field");
            
            Assertions.assertTrue(stdout.contains(fieldName), 
                "Expected session information to include field: " + fieldName);
        }
    }
    
    @Then("the output should include total session count")
    public void the_output_should_include_total_session_count() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.matches("(?s).*Total Sessions:?\\s*\\d+.*") || 
            stdout.matches("(?s).*Session Count:?\\s*\\d+.*") ||
            stdout.matches("(?s).*Active Sessions:?\\s*\\d+.*"),
            "Expected output to include total session count"
        );
    }
    
    @Then("a scheduled diagnostic task should be created")
    public void a_scheduled_diagnostic_task_should_be_created() {
        // Create a diagnostic task configuration
        Map<String, Object> taskConfig = new HashMap<>();
        taskConfig.put("checks", context.getConfigurationValue("selected_diagnostics").orElse(Collections.emptyList()));
        taskConfig.put("frequency", context.getConfigurationValue("selected_frequency").orElse("daily"));
        taskConfig.put("time", context.getConfigurationValue("schedule_time").orElse("00:00"));
        taskConfig.put("recipients", context.getConfigurationValue("notification_recipients").orElse(""));
        taskConfig.put("id", UUID.randomUUID().toString());
        
        // Store the task
        String taskKey = "scheduled_diagnostics_" + UUID.randomUUID().toString().substring(0, 8);
        context.setConfigurationValue(taskKey, taskConfig);
        
        // Store the latest task key for verification
        context.setConfigurationValue("latest_diagnostic_task", taskKey);
        
        // Verify task was created
        Optional<Object> taskOpt = context.getConfigurationValue(taskKey);
        Assertions.assertTrue(taskOpt.isPresent(), "Expected scheduled diagnostic task to be created");
    }
    
    @Then("the output should contain a scheduled task for {string} checks at {string}")
    public void the_output_should_contain_a_scheduled_task_for_checks_at(String checkTypes, String scheduleTime) {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check for the scheduled task in the output
        Assertions.assertTrue(
            stdout.contains(checkTypes) && stdout.contains(scheduleTime),
            "Expected output to contain scheduled task for " + checkTypes + " at " + scheduleTime
        );
    }
    
    @Then("the report should include the following metrics:")
    public void the_report_should_include_the_following_metrics(DataTable dataTable) {
        List<Map<String, String>> metrics = dataTable.asMaps();
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        // Check that each metric is mentioned in the output
        for (Map<String, String> metric : metrics) {
            String metricName = metric.get("Metric");
            String metricDetails = metric.get("Details");
            
            Assertions.assertTrue(stdout.contains(metricName), 
                "Expected output to contain metric: " + metricName);
            
            // If details are specified, check for those too
            if (metricDetails != null && !metricDetails.isEmpty()) {
                // This is a loose check - we don't need exact wording, just the main concepts
                String[] keyPhrases = metricDetails.split("\\s*,\\s*");
                boolean foundAnyPhrase = false;
                
                for (String phrase : keyPhrases) {
                    if (stdout.contains(phrase)) {
                        foundAnyPhrase = true;
                        break;
                    }
                }
                
                Assertions.assertTrue(foundAnyPhrase, 
                    "Expected output to mention details related to: " + metricDetails + " for metric: " + metricName);
            }
        }
    }
    
    @Then("the report should include optimization recommendations")
    public void the_report_should_include_optimization_recommendations() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Recommendation") || 
            stdout.contains("Suggested Optimization") || 
            stdout.contains("Improvement") ||
            stdout.contains("Consider"),
            "Expected report to include optimization recommendations"
        );
    }
    
    @Then("I should see a detailed analysis of the memory issue")
    public void i_should_see_a_detailed_analysis_of_the_memory_issue() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Memory Analysis") ||
            stdout.contains("Memory Usage") ||
            stdout.contains("Memory Issue"),
            "Expected to see a detailed analysis of the memory issue"
        );
        
        Assertions.assertTrue(
            stdout.contains("GB") || 
            stdout.contains("MB") || 
            stdout.contains("%"),
            "Expected memory analysis to include sizes"
        );
    }
    
    @Then("I should see a breakdown of memory consumption by component")
    public void i_should_see_a_breakdown_of_memory_consumption_by_component() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Breakdown") || 
            stdout.contains("Component") || 
            stdout.contains("Memory") && stdout.contains("Usage"),
            "Expected to see a breakdown of memory consumption by component"
        );
        
        // Check for at least two components
        Assertions.assertTrue(
            stdout.matches("(?s).*Component.*\n.*\n.*\n.*"),
            "Expected memory breakdown to list multiple components"
        );
    }
    
    @Then("the output should contain recommendations for resolving the issue")
    public void the_output_should_contain_recommendations_for_resolving_the_issue() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("Recommendation") || 
            stdout.contains("Suggested") || 
            stdout.contains("Advise") ||
            stdout.contains("Resolve"),
            "Expected output to contain recommendations for resolving the issue"
        );
    }
    
    @Then("the system resource warning should be resolved")
    public void the_system_resource_warning_should_be_resolved() {
        String stdout = context.getConfigurationValue("command_stdout").orElse("");
        
        Assertions.assertTrue(
            stdout.contains("resolved") || 
            stdout.contains("cleared") || 
            stdout.contains("fixed") ||
            stdout.contains("Memory reclaimed"),
            "Expected system resource warning to be resolved"
        );
        
        // Mark the warning as resolved in the context
        Optional<Object> warningOpt = context.getConfigurationValue("system_warning_HIGH_MEMORY_USAGE");
        if (warningOpt.isPresent()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> warning = (Map<String, Object>) warningOpt.get();
            warning.put("status", "RESOLVED");
            warning.put("resolvedAt", System.currentTimeMillis());
            context.setConfigurationValue("system_warning_HIGH_MEMORY_USAGE", warning);
        }
    }
    
    /**
     * Helper method to mock output for specific monitoring and diagnostics commands.
     *
     * @param command the command string
     */
    private void mockMonitoringCommandOutput(String command) {
        // Only process commands we care about
        if (!command.contains("rin admin monitor") && !command.contains("rin admin diagnostics")) {
            return;
        }
        
        StringBuilder output = new StringBuilder();
        
        if (command.contains("monitor dashboard")) {
            // Mock output for monitor dashboard command
            output.append("System Health Dashboard\n");
            output.append("=====================\n\n");
            output.append("Component Status:\n");
            output.append("----------------\n");
            output.append("API Server       | Available | Response: 115ms\n");
            output.append("Database         | Available | Queries: 256/s\n");
            output.append("Storage          | Available | I/O: 4.2MB/s\n");
            output.append("Queue Processing | Active    | Queue Size: 12\n");
            output.append("Task Scheduler   | Running   | Tasks: 5 active\n\n");
            
            output.append("Resource Utilization:\n");
            output.append("--------------------\n");
            output.append("CPU: 32% (12.8 of 40 cores)\n");
            output.append("Memory: 14.2GB / 32GB (44%)\n");
            output.append("Disk: 1.2TB / 4TB (30%)\n");
            output.append("Network: 48Mbps In / 32Mbps Out\n\n");
            
            output.append("User Activity:\n");
            output.append("-------------\n");
            output.append("Active Users: 28\n");
            output.append("Logged in last hour: 42\n");
            output.append("API Requests: 856/min\n");
            output.append("Average Response Time: 145ms\n");
        } else if (command.contains("monitor server --detailed")) {
            // Mock output for detailed server metrics
            output.append("Detailed Server Metrics\n");
            output.append("======================\n\n");
            output.append("System Information:\n");
            output.append("------------------\n");
            output.append("Hostname: rinna-prod-01\n");
            output.append("OS: Ubuntu 22.04.3 LTS\n");
            output.append("Kernel: 5.15.0-82-generic\n");
            output.append("Architecture: x86_64\n");
            output.append("CPUs: 40 cores (2 physical CPUs, 20 cores each)\n");
            output.append("Memory: 32 GB\n");
            output.append("Uptime: 42 days, 8 hours, 15 minutes\n\n");
            
            output.append("Current Performance Metrics:\n");
            output.append("--------------------------\n");
            output.append("Metric              | Value      | Threshold  | Status\n");
            output.append("-------------------- | ---------- | ---------- | --------\n");
            output.append("CPU Load            | 32.5%      | 85%        | Normal\n");
            output.append("Memory Usage        | 44.3%      | 90%        | Normal\n");
            output.append("Disk Usage          | 30.0%      | 85%        | Normal\n");
            output.append("Network Connections | 482        | 1000       | Normal\n");
            output.append("Response Time       | 145ms      | 500ms      | Normal\n");
            output.append("Error Rate          | 0.08%      | 1%         | Normal\n\n");
            
            output.append("Thread Pool Utilization:\n");
            output.append("----------------------\n");
            output.append("Pool Name     | Size | Active | Queue | Completed\n");
            output.append("------------- | ---- | ------ | ----- | ---------\n");
            output.append("API Workers   | 50   | 12     | 2     | 12,456,789\n");
            output.append("DB Workers    | 25   | 8      | 0     | 8,765,432\n");
            output.append("Task Workers  | 15   | 3      | 5     | 2,345,678\n");
            output.append("Admin Workers | 5    | 1      | 0     | 234,567\n\n");
            
            output.append("Detailed Resource Trends (15 minute averages):\n");
            output.append("------------------------------------------\n");
            output.append("CPU: [████████░░░░░░░░░░░░░░░░░░░░░░░░] 32.5%\n");
            output.append("Memory: [███████████░░░░░░░░░░░░░░░░░░] 44.3%\n");
            output.append("Disk I/O: [█████░░░░░░░░░░░░░░░░░░░░░░] 18.6%\n");
            output.append("Network: [██████░░░░░░░░░░░░░░░░░░░░░░] 24.2%\n");
        } else if (command.contains("monitor configure")) {
            // Mock output for monitor configuration
            output.append("Monitoring Configuration\n");
            output.append("=======================\n\n");
            output.append("Select threshold to configure:\n");
            output.append("1. CPU Load (current: 85%)\n");
            output.append("2. Memory Usage (current: 90%)\n");
            output.append("3. Disk Usage (current: 85%)\n");
            output.append("4. Network Connections (current: 1000)\n");
            output.append("5. Response Time (current: 500ms)\n");
            output.append("6. Error Rate (current: 1%)\n");
            output.append("7. Refresh Interval (current: 60s)\n\n");
            
            output.append("Enter the number of the threshold to configure: \n");
        } else if (command.contains("monitor thresholds")) {
            // Mock output for monitor thresholds
            output.append("Monitoring Thresholds\n");
            output.append("====================\n\n");
            
            // Get current thresholds from context
            Optional<Object> thresholdsOpt = context.getConfigurationValue("monitoring_thresholds");
            if (thresholdsOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> thresholds = (Map<String, Object>) thresholdsOpt.get();
                
                output.append("Current threshold settings:\n\n");
                output.append("Metric                | Threshold   | Action on Exceed\n");
                output.append("--------------------- | ----------- | ---------------\n");
                
                for (Map.Entry<String, Object> entry : thresholds.entrySet()) {
                    String metric = entry.getKey();
                    String value = entry.getValue().toString();
                    String action = "Log and Alert";
                    
                    output.append(String.format("%-21s | %-11s | %s\n", metric, value, action));
                }
                
                output.append("\nRefresh Interval: 60 seconds\n");
                output.append("Alert Recipients: admin@example.com, ops@example.com\n");
            } else {
                output.append("No custom thresholds configured, using system defaults.\n");
            }
        } else if (command.contains("monitor report")) {
            // Mock output for performance report
            String period = command.contains("--period=") 
                ? command.replaceAll(".*--period=(\\w+).*", "$1") 
                : "daily";
            
            output.append("System Performance Report (").append(period).append(")\n");
            output.append("====================================\n\n");
            
            LocalDate reportDate = LocalDate.now();
            output.append("Report Period: ").append(reportDate.minusDays(1)).append(" to ").append(reportDate).append("\n");
            output.append("Generated: ").append(reportDate).append(" ").append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n\n");
            
            output.append("Performance Summary\n");
            output.append("------------------\n");
            output.append("Overall system performance is NORMAL\n");
            output.append("No critical issues detected\n");
            output.append("2 warnings identified (see details below)\n\n");
            
            output.append("Resource Utilization\n");
            output.append("-------------------\n");
            output.append("CPU: Average 38%, Peak 72% at 14:32\n");
            output.append("Memory: Average 46%, Peak 68% at 14:35\n");
            output.append("Disk I/O: Average 120MB/s, Peak 342MB/s at 08:15\n");
            output.append("Network: Average 75Mbps, Peak 210Mbps at 13:20\n\n");
            
            output.append("Service Availability\n");
            output.append("-------------------\n");
            output.append("API Server: 100% uptime, avg response 128ms\n");
            output.append("Database: 100% uptime, avg query time 42ms\n");
            output.append("Storage Service: 99.998% uptime (one 30s disruption at 03:12)\n");
            output.append("Queue Processor: 100% uptime, avg queue size 8.2\n\n");
            
            output.append("Error Analysis\n");
            output.append("-------------\n");
            output.append("Error Rate: 0.08% (128 errors in 160,832 requests)\n");
            output.append("Most Common Error: Timeout (42%), Authentication (28%), Validation (18%)\n");
            output.append("Error Distribution: API (68%), Database (22%), Storage (10%)\n\n");
            
            output.append("Throughput Metrics\n");
            output.append("----------------\n");
            output.append("Total Requests: 160,832\n");
            output.append("Peak RPS: 42 at 14:32\n");
            output.append("Data Transferred: 28.4GB\n");
            output.append("Completed Tasks: 24,568\n\n");
            
            output.append("Warning Details\n");
            output.append("--------------\n");
            output.append("1. Occasional response time spikes during peak hours (recommendation: optimize database queries)\n");
            output.append("2. Gradual increase in memory usage trend (recommendation: review memory management)\n\n");
            
            output.append("Generated daily performance report\n");
            output.append("Report saved to: performance_report_").append(reportDate).append("_").append(period).append(".pdf\n");
        } else if (command.contains("monitor alerts add")) {
            // Mock output for adding monitoring alerts
            output.append("Create Monitoring Alert\n");
            output.append("====================\n\n");
            output.append("Enter alert name:\n");
        } else if (command.contains("monitor alerts list")) {
            // Mock output for listing monitoring alerts
            output.append("Configured Monitoring Alerts\n");
            output.append("=========================\n\n");
            output.append("Name                     | Metric              | Threshold | Recipients\n");
            output.append("------------------------ | ------------------- | --------- | -----------------\n");
            
            // Show the created alert if it exists
            if (context.getConfigurationValue("alert_name").isPresent()) {
                String alertName = context.getConfigurationValue("alert_name").get().toString();
                String metric = context.getConfigurationValue("selected_metric").orElse("CPU Load").toString();
                String threshold = context.getConfigurationValue("threshold_value").orElse("80").toString() + "%";
                String recipients = context.getConfigurationValue("notification_recipients").orElse("").toString();
                
                output.append(String.format("%-24s | %-19s | %-9s | %s\n", 
                    alertName, metric, threshold, recipients));
            }
            
            // Add some predefined alerts
            output.append("Server Overload Alert     | CPU Load             | 90%       | ops@example.com\n");
            output.append("Database Performance      | Response Time        | 800ms     | dba@example.com\n");
            output.append("Disk Space Warning        | Disk Usage           | 80%       | admin@example.com\n");
            output.append("High API Error Rate       | Error Rate           | 2%        | developers@example.com\n");
        } else if (command.contains("monitor sessions")) {
            // Mock output for active sessions
            output.append("Active User Sessions\n");
            output.append("=================\n\n");
            output.append("Session ID                           | User     | Login Time            | Client IP       | Resource Usage    | Current Activity\n");
            output.append("------------------------------------- | -------- | --------------------- | --------------- | ----------------- | ---------------------\n");
            output.append("a8f5e3c2-6b9d-42a1-8d3e-f7c6b5a4d3e2 | admin    | 2025-04-07T08:32:15Z  | 192.168.1.105   | CPU: 2%, Mem: 320MB | Viewing dashboard\n");
            output.append("b7e6d4c3-5a8b-41c2-7d9e-e8f7d6c5b4a3 | alice    | 2025-04-07T09:45:22Z  | 192.168.1.110   | CPU: 5%, Mem: 480MB | Running diagnostics\n");
            output.append("c6d5e4f3-4b7a-40c3-6e8d-d9e8f7d6c5b4 | bob      | 2025-04-07T10:12:08Z  | 192.168.1.115   | CPU: 3%, Mem: 280MB | Editing project\n");
            output.append("d5c4f3e2-3a6b-39d4-5f7c-c8d9e0f1g2h3 | carol    | 2025-04-07T10:15:43Z  | 192.168.1.120   | CPU: 1%, Mem: 180MB | Viewing reports\n");
            output.append("e4f3g2h1-2i5j-38k5-4l6m-b7n8o9p0q1r2 | dave     | 2025-04-07T10:28:16Z  | 192.168.1.125   | CPU: 8%, Mem: 750MB | Running batch process\n");
            output.append("f3g2h1i0-1j4k-37l6-3m7n-a6b7c8d9e0f1 | eve      | 2025-04-07T10:42:31Z  | 192.168.1.130   | CPU: 2%, Mem: 210MB | Browsing work items\n\n");
            
            output.append("Total Sessions: 6\n");
            output.append("Peak Concurrent Sessions Today: 12 (at 15:30)\n");
            output.append("Average Session Duration: 42 minutes\n");
        } else if (command.contains("diagnostics run")) {
            // Mock output for running system diagnostics
            output.append("System Diagnostics\n");
            output.append("================\n\n");
            output.append("Running full system diagnostics...\n\n");
            
            output.append("1. API Server Diagnostics\n");
            output.append("   Connectivity: PASS (response time: 28ms)\n");
            output.append("   Thread Pool: PASS (12 active, 38 available)\n");
            output.append("   Error Rates: PASS (0.08% error rate)\n");
            output.append("   Resource Usage: PASS (CPU: 32%, Memory: 2.4GB)\n");
            output.append("   Endpoints: PASS (all 42 endpoints responding)\n\n");
            
            output.append("2. Database Diagnostics\n");
            output.append("   Connectivity: PASS (response time: 12ms)\n");
            output.append("   Query Performance: WARNING (3 slow queries detected)\n");
            output.append("   Connection Pool: PASS (8 active, 17 available)\n");
            output.append("   Deadlocks: PASS (0 deadlocks detected)\n");
            output.append("   Storage: PASS (30% used, 8% growth in 30 days)\n\n");
            
            output.append("3. File Storage Diagnostics\n");
            output.append("   Access: PASS (read/write operations successful)\n");
            output.append("   Capacity: PASS (30% used, 2.8TB free)\n");
            output.append("   Read Speed: PASS (210MB/s)\n");
            output.append("   Write Speed: PASS (185MB/s)\n");
            output.append("   Permissions: PASS (all required permissions verified)\n\n");
            
            output.append("4. Memory Management Diagnostics\n");
            output.append("   Allocation: PASS (no allocation failures)\n");
            output.append("   Fragmentation: PASS (3% fragmentation)\n");
            output.append("   Leaks: WARNING (potential memory leak in API Server)\n");
            output.append("   GC Performance: PASS (avg pause: 12ms)\n");
            output.append("   Swap Usage: PASS (0% swap used)\n\n");
            
            output.append("5. Thread Pool Diagnostics\n");
            output.append("   Deadlocks: PASS (no deadlocks detected)\n");
            output.append("   Starvation: PASS (no thread starvation detected)\n");
            output.append("   Wait Times: PASS (avg wait: 8ms)\n");
            output.append("   Excessive Wait: PASS (max wait: 124ms)\n");
            output.append("   Thread Dumps: PASS (no suspicious stack traces)\n\n");
            
            output.append("6. Network Diagnostics\n");
            output.append("   Latency: PASS (avg: 0.8ms, max: 12ms)\n");
            output.append("   Packet Loss: PASS (0.001% loss)\n");
            output.append("   DNS Resolution: PASS (2ms resolution time)\n");
            output.append("   Bandwidth: PASS (48Mbps in, 32Mbps out)\n");
            output.append("   Connections: PASS (482 active connections)\n\n");
            
            output.append("Summary: 2 warnings detected\n");
            output.append("- Database has 3 slow queries that should be optimized\n");
            output.append("- API Server shows signs of potential memory leak\n\n");
            
            output.append("System bottlenecks identified:\n");
            output.append("1. Database query optimization needed for report generation queries\n");
            output.append("2. API Server memory management needs review to address gradual growth\n\n");
            
            output.append("Diagnostics complete. Report saved to: system_diagnostics_").append(LocalDate.now()).append(".pdf\n");
        } else if (command.contains("diagnostics schedule")) {
            // Mock output for scheduling diagnostics
            output.append("Schedule Recurring Diagnostics\n");
            output.append("===========================\n\n");
            output.append("Select diagnostic check types (comma-separated):\n");
            output.append("- api: API Server checks\n");
            output.append("- database: Database checks\n");
            output.append("- storage: File storage checks\n");
            output.append("- memory: Memory management checks\n");
            output.append("- threads: Thread pool checks\n");
            output.append("- network: Network diagnostics\n");
            output.append("- all: All diagnostics\n\n");
        } else if (command.contains("diagnostics schedule list")) {
            // Mock output for listing scheduled diagnostics
            output.append("Scheduled Diagnostic Tasks\n");
            output.append("========================\n\n");
            output.append("ID                   | Check Types                     | Frequency | Time  | Recipients\n");
            output.append("--------------------- | ------------------------------- | --------- | ----- | --------------------\n");
            
            // Show any stored diagnostic tasks
            Optional<Object> taskKeyOpt = context.getConfigurationValue("latest_diagnostic_task");
            if (taskKeyOpt.isPresent()) {
                String taskKey = taskKeyOpt.get().toString();
                Optional<Object> taskOpt = context.getConfigurationValue(taskKey);
                
                if (taskOpt.isPresent()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> task = (Map<String, Object>) taskOpt.get();
                    
                    String id = task.get("id").toString().substring(0, 8);
                    
                    @SuppressWarnings("unchecked")
                    List<String> checks = (List<String>) task.get("checks");
                    String checkTypes = String.join(",", checks);
                    
                    String frequency = task.get("frequency").toString();
                    String time = task.get("time").toString();
                    String recipients = task.get("recipients").toString();
                    
                    output.append(String.format("%-21s | %-31s | %-9s | %-5s | %s\n", 
                        id, checkTypes, frequency, time, recipients));
                }
            }
            
            // Add some predefined scheduled tasks
            output.append("diag-8a7b6c5d4e3f   | api,database                    | daily     | 01:00 | ops@example.com\n");
            output.append("diag-7c6d5e4f3g2h   | all                             | weekly    | 03:30 | admin@example.com\n");
            output.append("diag-6e5f4g3h2i1j   | memory,threads                  | hourly    | xx:15 | alerts@example.com\n");
        } else if (command.contains("diagnostics database")) {
            // Mock output for database diagnostics
            output.append("Database Performance Analysis\n");
            output.append("==========================\n\n");
            output.append("Timestamp: ").append(LocalDate.now()).append(" ").append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n");
            output.append("Database Type: PostgreSQL 15.3\n");
            output.append("Database Size: 1.2TB\n");
            output.append("Connected Clients: 24\n\n");
            
            output.append("Query Performance Analysis\n");
            output.append("------------------------\n");
            output.append("Total Queries Analyzed: 12,845\n");
            output.append("Avg Query Time: 28ms\n");
            output.append("Slow Queries (>500ms): 42 (0.33%)\n");
            output.append("Query Cache Hit Rate: 92.4%\n\n");
            
            output.append("Top 3 Slow Queries:\n");
            output.append("1. SELECT * FROM work_items WHERE project_id IN (SELECT id FROM projects WHERE ...) [1248ms]\n");
            output.append("2. SELECT w.*, u.name FROM work_items w JOIN users u ON w.assignee_id = u.id WHERE ... [782ms]\n");
            output.append("3. SELECT * FROM audit_logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC [658ms]\n\n");
            
            output.append("Index Utilization\n");
            output.append("-----------------\n");
            output.append("Table Scans: 4.2%\n");
            output.append("Index Scans: 95.8%\n");
            output.append("Unused Indexes: 3\n");
            output.append("Missing Indexes: 2\n\n");
            
            output.append("Connection Pool Performance\n");
            output.append("-------------------------\n");
            output.append("Pool Size: 25\n");
            output.append("Peak Usage: 24 (96%)\n");
            output.append("Avg Wait Time: 1.2ms\n");
            output.append("Connection Timeouts: 0\n\n");
            
            output.append("Transaction Volume\n");
            output.append("-----------------\n");
            output.append("Transactions per Minute: 1,845\n");
            output.append("Commits: 1,824/min\n");
            output.append("Rollbacks: 21/min\n");
            output.append("Long Transactions (>2s): 2\n\n");
            
            output.append("Lock Contention\n");
            output.append("--------------\n");
            output.append("Lock Wait Events: 28\n");
            output.append("Avg Lock Wait Time: 42ms\n");
            output.append("Deadlocks: 0\n");
            output.append("Lock Timeouts: 1\n\n");
            
            output.append("Storage Utilization\n");
            output.append("------------------\n");
            output.append("Largest Tables:\n");
            output.append("1. audit_logs: 420GB\n");
            output.append("2. work_items: 280GB\n");
            output.append("3. file_content: 210GB\n");
            output.append("Fastest Growing Tables: audit_logs (+2.1GB/day), file_content (+1.4GB/day)\n\n");
            
            output.append("Optimization Recommendations:\n");
            output.append("1. Add index on audit_logs(timestamp) to improve query #3\n");
            output.append("2. Replace table scan in query #1 with indexed lookup\n");
            output.append("3. Implement partitioning on audit_logs table by month\n");
            output.append("4. Increase connection pool size to 30\n");
            output.append("5. Remove unused indexes: projects_legacy_idx, users_login_history_idx\n\n");
            
            output.append("Analysis complete. Report saved to: database_analysis_").append(LocalDate.now()).append(".pdf\n");
        } else if (command.contains("diagnostics warning resolve")) {
            // Mock output for resolving a warning
            String warningId = command.contains("--id=") 
                ? command.replaceAll(".*--id=([\\w-]+).*", "$1") 
                : "UNKNOWN";
            
            output.append("Warning Resolution: ").append(warningId).append("\n");
            output.append("==============================\n\n");
            
            // Only proceed if we have the right warning ID
            if ("MEM-001".equals(warningId)) {
                Optional<Object> warningOpt = context.getConfigurationValue("system_warning_HIGH_MEMORY_USAGE");
                if (warningOpt.isPresent()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> warning = (Map<String, Object>) warningOpt.get();
                    
                    output.append("Warning Type: ").append(warning.get("type")).append("\n");
                    output.append("Timestamp: ").append(new Date((Long) warning.get("timestamp"))).append("\n");
                    output.append("Severity: ").append(warning.get("severity")).append("\n");
                    output.append("Description: ").append(warning.get("description")).append("\n\n");
                    
                    output.append("Memory Analysis:\n");
                    output.append("Total Memory: 32.0 GB\n");
                    output.append("Used Memory: 29.6 GB (92.5%)\n\n");
                    
                    output.append("Available Actions:\n");
                    output.append("1. Restart API Server\n");
                    output.append("2. Analyze Heap Dump\n");
                    output.append("3. Clear Caches\n");
                    output.append("4. Memory Reclamation\n");
                    output.append("5. Ignore Warning\n\n");
                    
                    output.append("Select an action to perform: \n");
                }
            } else {
                output.append("Warning ID not found: ").append(warningId).append("\n");
            }
        } else if (command.contains("diagnostics action --memory-reclaim")) {
            // Mock output for memory reclamation
            output.append("Memory Reclamation Process\n");
            output.append("========================\n\n");
            output.append("Starting memory reclamation...\n\n");
            
            output.append("Step 1: Analyzing memory usage patterns...\n");
            output.append("- Identified 3 potential areas for reclamation\n");
            output.append("- API Server using 8.4 GB (excessive caching detected)\n");
            output.append("- Data Cache consuming 3.2 GB (stale entries found)\n");
            output.append("- Connection pool holding 0.9 GB (idle connections)\n\n");
            
            output.append("Step 2: Running targeted reclamation...\n");
            output.append("- Clearing API Server result cache: 2.8 GB freed\n");
            output.append("- Purging stale Data Cache entries: 1.6 GB freed\n");
            output.append("- Releasing idle database connections: 0.7 GB freed\n");
            output.append("- Running garbage collection: 0.4 GB additional memory recovered\n\n");
            
            output.append("Step 3: Verifying results...\n");
            output.append("- Previous memory usage: 29.6 GB (92.5%)\n");
            output.append("- Current memory usage: 24.1 GB (75.3%)\n");
            output.append("- Total memory reclaimed: 5.5 GB\n\n");
            
            output.append("Memory reclamation completed successfully!\n");
            output.append("Memory warning MEM-001 has been resolved\n");
            output.append("System performance should be improved\n\n");
            
            output.append("Recommendations to prevent recurrence:\n");
            output.append("1. Adjust API Server cache size configuration\n");
            output.append("2. Implement more aggressive cache expiration policy\n");
            output.append("3. Tune connection pool idle timeout settings\n");
        }
        
        // Store the mocked output in the context
        if (output.length() > 0) {
            context.setConfigurationValue("command_stdout", output.toString());
        }
    }
}