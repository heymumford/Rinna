/**
 * Step definitions for Report feature tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.acceptance.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.rinna.cli.command.ReportCommand;
import org.rinna.cli.command.ScheduleCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.report.ReportScheduler;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step definitions for report generation.
 */
public class ReportSteps {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final List<Path> tempFiles = new ArrayList<>();
    private int lastExitCode = 0;
    private ItemService itemService;
    private Map<String, String> reportIdMap = new HashMap<>();

    @Before
    public void setup() {
        // Set test output stream
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
        
        // Get item service instance
        itemService = ServiceManager.getInstance().getItemService();
        
        // Clean existing data
        itemService.clearItems();
        
        // Clean up any existing scheduled reports
        ReportScheduler.getInstance().clearScheduledReports();
    }
    
    @After
    public void teardown() {
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Delete temporary files
        for (Path path : tempFiles) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                System.err.println("Error deleting temp file: " + e.getMessage());
            }
        }
        
        // Clean existing data
        itemService.clearItems();
        
        // Clean up any scheduled reports
        ReportScheduler.getInstance().clearScheduledReports();
    }
    
    @Given("the system contains the following work items:")
    public void systemContainsWorkItems(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        
        for (Map<String, String> row : rows) {
            WorkItem item = new WorkItem();
            
            // Set ID
            item.setId(row.get("ID"));
            
            // Set title
            item.setTitle(row.get("Title"));
            
            // Set type
            String typeStr = row.get("Type");
            WorkItemType type = WorkItemType.valueOf(typeStr);
            item.setType(type);
            
            // Set state
            String stateStr = row.get("State");
            WorkflowState state = WorkflowState.valueOf(stateStr);
            item.setState(state);
            
            // Set priority
            String priorityStr = row.get("Priority");
            Priority priority = Priority.valueOf(priorityStr);
            item.setPriority(priority);
            
            // Set assignee (can be null)
            String assignee = row.get("Assignee");
            if (!"null".equals(assignee)) {
                item.setAssignee(assignee);
            }
            
            // Set creation and update dates
            item.setCreatedAt(LocalDateTime.now().minusDays(10));
            item.setUpdatedAt(LocalDateTime.now().minusDays(2));
            
            // Add item to the service
            itemService.addItem(item);
        }
    }
    
    @Given("template {string} exists")
    public void templateExists(String templateName) {
        // Create templates directory if it doesn't exist
        Path templateDir = Paths.get("templates/reports");
        try {
            Files.createDirectories(templateDir);
            
            // Create template based on its extension
            if (templateName.endsWith(".html")) {
                createHtmlTemplate(templateDir.resolve(templateName));
            } else if (templateName.endsWith(".md")) {
                createMarkdownTemplate(templateDir.resolve(templateName));
            } else {
                createTextTemplate(templateDir.resolve(templateName));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create template: " + e.getMessage(), e);
        }
    }
    
    private void createHtmlTemplate(Path templatePath) throws Exception {
        String content = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>{{ title }}</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; margin: 20px; }\n" +
                "        h1 { color: #336699; }\n" +
                "        .summary { background-color: #f0f0f0; padding: 10px; border-radius: 5px; }\n" +
                "        .count { font-weight: bold; color: #cc3300; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>{{ title }}</h1>\n" +
                "    <div class=\"summary\">\n" +
                "        <p>Total Items: <span class=\"count\">{{ summary.totalItems }}</span></p>\n" +
                "        <p>Completed: <span class=\"count\">{{ summary.completed }}</span></p>\n" +
                "        <p>In Progress: <span class=\"count\">{{ summary.inProgress }}</span></p>\n" +
                "        <p>Not Started: <span class=\"count\">{{ summary.notStarted }}</span></p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
        
        Files.writeString(templatePath, content);
    }
    
    private void createMarkdownTemplate(Path templatePath) throws Exception {
        String content = "# {{ title }}\n\n" +
                "Generated: {{ timestamp }}\n\n" +
                "## Summary\n\n" +
                "- Total Items: **{{ summary.totalItems }}**\n" +
                "- Completed: **{{ summary.completed }}**\n" +
                "- In Progress: **{{ summary.inProgress }}**\n" +
                "- Not Started: **{{ summary.notStarted }}**\n\n" +
                "## Types\n\n" +
                "{{ typeRows }}\n\n" +
                "## Priorities\n\n" +
                "{{ priorityRows }}\n";
        
        Files.writeString(templatePath, content);
    }
    
    private void createTextTemplate(Path templatePath) throws Exception {
        String content = "{{ title }}\n" +
                "{{ underline }}\n\n" +
                "Generated: {{ timestamp }}\n\n" +
                "SUMMARY:\n" +
                "- Total Items: {{ summary.totalItems }}\n" +
                "- Completed: {{ summary.completed }}\n" +
                "- In Progress: {{ summary.inProgress }}\n" +
                "- Not Started: {{ summary.notStarted }}\n\n" +
                "TYPES:\n" +
                "{{ typeRows }}\n" +
                "PRIORITIES:\n" +
                "{{ priorityRows }}\n";
        
        Files.writeString(templatePath, content);
    }
    
    @Given("a scheduled report {string} exists")
    public void scheduledReportExists(String reportName) {
        // Create a scheduled report for testing
        ReportScheduler scheduler = ReportScheduler.getInstance();
        ReportScheduler.ScheduledReport report = new ReportScheduler.ScheduledReport();
        
        report.setName(reportName);
        report.setScheduleType(ReportScheduler.ScheduleType.WEEKLY);
        report.setDayOfWeek(java.time.DayOfWeek.MONDAY);
        report.setTime("09:00");
        
        org.rinna.cli.report.ReportConfig config = 
            org.rinna.cli.report.ReportConfig.createDefault(org.rinna.cli.report.ReportType.SUMMARY);
        config.setEmailEnabled(true);
        config.addEmailRecipient("test@example.com");
        report.setConfig(config);
        
        // Add the report and store its ID
        boolean added = scheduler.addScheduledReport(report);
        if (!added) {
            throw new RuntimeException("Failed to create scheduled report");
        }
        
        // Store the report ID for later use
        reportIdMap.put(reportName, report.getId());
    }
    
    @When("I run the command {string}")
    public void runCommand(String commandLine) {
        // Reset output
        outputStream.reset();
        
        // Parse command and arguments
        String[] parts = commandLine.split(" ", 2);
        String command = parts[0];
        String[] args = parts.length > 1 ? parseCommandArgs(parts[1]) : new String[0];
        
        // Convert rin to actual command
        if ("rin".equals(command)) {
            if (args.length > 0) {
                command = args[0];
                args = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];
            }
        }
        
        // Replace any placeholder tokens in arguments
        args = replaceTokens(args);
        
        // Execute the command
        try {
            if ("report".equals(command)) {
                ReportCommand reportCmd = new ReportCommand();
                parseReportArgs(reportCmd, args);
                lastExitCode = reportCmd.call();
            } else if ("schedule".equals(command)) {
                ScheduleCommand scheduleCmd = new ScheduleCommand();
                parseScheduleArgs(scheduleCmd, args);
                lastExitCode = scheduleCmd.call();
            } else {
                throw new UnsupportedOperationException("Unsupported command: " + command);
            }
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            e.printStackTrace(System.err);
            lastExitCode = 1;
        }
    }
    
    private String[] parseCommandArgs(String argString) {
        List<String> args = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < argString.length(); i++) {
            char c = argString.charAt(i);
            
            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            args.add(current.toString());
        }
        
        return args.toArray(new String[0]);
    }
    
    private String[] replaceTokens(String[] args) {
        String[] result = new String[args.length];
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            // Replace report ID tokens
            if (arg.contains("{report-id}")) {
                // Find the report name from previous arguments
                for (int j = 0; j < i; j++) {
                    if (args[j].equals("--name")) {
                        String reportName = args[j + 1];
                        if (reportIdMap.containsKey(reportName)) {
                            arg = arg.replace("{report-id}", reportIdMap.get(reportName));
                        }
                        break;
                    }
                }
            }
            
            result[i] = arg;
        }
        
        return result;
    }
    
    private void parseReportArgs(ReportCommand cmd, String[] args) {
        if (args.length > 0) {
            cmd.setType(args[0]);
            
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                
                if (arg.startsWith("--format=")) {
                    cmd.setFormat(arg.substring(9));
                } else if (arg.startsWith("--output=")) {
                    String output = arg.substring(9);
                    cmd.setOutput(output);
                    
                    // Remember temporary file
                    tempFiles.add(Paths.get(output));
                } else if (arg.startsWith("--title=")) {
                    cmd.setTitle(arg.substring(8));
                } else if (arg.startsWith("--start=")) {
                    cmd.setStartDate(arg.substring(8));
                } else if (arg.startsWith("--end=")) {
                    cmd.setEndDate(arg.substring(6));
                } else if (arg.startsWith("--project=")) {
                    cmd.setProjectId(arg.substring(10));
                } else if (arg.startsWith("--sort=")) {
                    cmd.setSortField(arg.substring(7));
                } else if ("--desc".equals(arg)) {
                    cmd.setAscending(false);
                } else if (arg.startsWith("--group=")) {
                    cmd.setGroupBy(arg.substring(8));
                } else if (arg.startsWith("--limit=")) {
                    cmd.setLimit(Integer.parseInt(arg.substring(8)));
                } else if ("--no-header".equals(arg)) {
                    cmd.setNoHeader(true);
                } else if ("--no-timestamp".equals(arg)) {
                    cmd.setNoTimestamp(true);
                } else if (arg.startsWith("--filter=")) {
                    String filter = arg.substring(9);
                    int equalsPos = filter.indexOf('=');
                    if (equalsPos > 0) {
                        String field = filter.substring(0, equalsPos);
                        String value = filter.substring(equalsPos + 1);
                        cmd.addFilter(field, value);
                    }
                } else if ("--email".equals(arg)) {
                    cmd.setEmailEnabled(true);
                } else if (arg.startsWith("--email-to=")) {
                    cmd.setEmailRecipients(arg.substring(11));
                } else if (arg.startsWith("--email-subject=")) {
                    cmd.setEmailSubject(arg.substring(16));
                } else if (arg.startsWith("--template=")) {
                    cmd.setTemplateName(arg.substring(11));
                } else if ("--no-template".equals(arg)) {
                    cmd.setNoTemplate(true);
                }
            }
        }
    }
    
    private void parseScheduleArgs(ScheduleCommand cmd, String[] args) {
        if (args.length > 0) {
            cmd.setAction(args[0]);
            
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                
                if (arg.startsWith("--id=")) {
                    cmd.setId(arg.substring(5));
                } else if (arg.startsWith("--name=")) {
                    cmd.setName(arg.substring(7));
                } else if (arg.startsWith("--desc=")) {
                    cmd.setDescription(arg.substring(7));
                } else if (arg.startsWith("--type=")) {
                    cmd.setScheduleType(arg.substring(7));
                } else if (arg.startsWith("--time=")) {
                    cmd.setTime(arg.substring(7));
                } else if (arg.startsWith("--day=")) {
                    cmd.setDayOfWeek(arg.substring(6));
                } else if (arg.startsWith("--date=")) {
                    try {
                        int dayOfMonth = Integer.parseInt(arg.substring(7));
                        cmd.setDayOfMonth(dayOfMonth);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid day of month: " + arg.substring(7));
                    }
                } else if (arg.startsWith("--report=")) {
                    cmd.setReportType(arg.substring(9));
                } else if (arg.startsWith("--format=")) {
                    cmd.setReportFormat(arg.substring(9));
                } else if (arg.startsWith("--output=")) {
                    cmd.setOutputPath(arg.substring(9));
                } else if (arg.startsWith("--title=")) {
                    cmd.setTitle(arg.substring(8));
                } else if ("--email".equals(arg)) {
                    cmd.setEmailEnabled(true);
                } else if (arg.startsWith("--email-to=")) {
                    cmd.setEmailRecipients(arg.substring(11));
                } else if (arg.startsWith("--email-subject=")) {
                    cmd.setEmailSubject(arg.substring(16));
                }
            }
        }
    }
    
    @Then("the command should succeed")
    public void commandShouldSucceed() {
        Assertions.assertEquals(0, lastExitCode, "Command should succeed with exit code 0");
    }
    
    @Then("the output should contain {string}")
    public void outputShouldContain(String text) {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains(text), 
            "Output should contain '" + text + "' but was: " + output);
    }
    
    @Then("the output should contain a breakdown by priority")
    public void outputShouldContainPriorityBreakdown() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("HIGH"), "Output should contain HIGH priority");
        Assertions.assertTrue(output.contains("MEDIUM"), "Output should contain MEDIUM priority");
        Assertions.assertTrue(output.contains("LOW"), "Output should contain LOW priority");
    }
    
    @Then("the output should contain a breakdown by type")
    public void outputShouldContainTypeBreakdown() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("TASK"), "Output should contain TASK type");
        Assertions.assertTrue(output.contains("BUG"), "Output should contain BUG type");
    }
    
    @Then("the output should contain information about {string}")
    public void outputShouldContainItemInfo(String itemId) {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains(itemId), 
            "Output should contain information about " + itemId);
    }
    
    @Then("the output should not contain information about {string}")
    public void outputShouldNotContainItemInfo(String itemId) {
        String output = outputStream.toString();
        Assertions.assertFalse(output.contains(itemId), 
            "Output should not contain information about " + itemId);
    }
    
    @Then("the output should contain sections for each state")
    public void outputShouldContainStateSections() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("DONE"), "Output should contain DONE state section");
        Assertions.assertTrue(output.contains("IN_PROGRESS"), "Output should contain IN_PROGRESS state section");
        Assertions.assertTrue(output.contains("TODO"), "Output should contain TODO state section");
    }
    
    @Then("the {string} section should contain {string} and {string}")
    public void sectionShouldContainItems(String section, String item1, String item2) {
        String output = outputStream.toString();
        
        // Try to find the section and check if it contains both items
        Pattern pattern = Pattern.compile(section + ".*?(?=\\n\\n|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(output);
        
        Assertions.assertTrue(matcher.find(), "Output should contain a section for " + section);
        
        String sectionContent = matcher.group();
        Assertions.assertTrue(sectionContent.contains(item1), 
            section + " section should contain " + item1);
        Assertions.assertTrue(sectionContent.contains(item2), 
            section + " section should contain " + item2);
    }
    
    @Then("the file {string} should be created")
    public void fileShouldBeCreated(String filename) {
        File file = new File(filename);
        Assertions.assertTrue(file.exists(), "File " + filename + " should be created");
        Assertions.assertTrue(file.length() > 0, "File " + filename + " should not be empty");
        
        // Add to temp files list for cleanup
        tempFiles.add(file.toPath());
    }
    
    @Then("the file {string} should contain {string} report content")
    public void fileShouldContainReportContent(String filename, String format) {
        try {
            String content = Files.readString(Paths.get(filename));
            
            if ("HTML".equalsIgnoreCase(format)) {
                Assertions.assertTrue(content.contains("<!DOCTYPE html>") || content.contains("<html>"), 
                    "File should contain HTML content");
            } else if ("CSV".equalsIgnoreCase(format)) {
                Assertions.assertTrue(content.contains(","), 
                    "File should contain CSV content");
            } else if ("JSON".equalsIgnoreCase(format)) {
                Assertions.assertTrue(content.contains("{") && content.contains("}"), 
                    "File should contain JSON content");
            }
        } catch (Exception e) {
            Assertions.fail("Failed to read file: " + e.getMessage());
        }
    }
    
    @Then("the output should indicate filtered by date range from {string} to {string}")
    public void outputShouldIndicateDateRange(String startDate, String endDate) {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains(startDate), 
            "Output should indicate filtering from " + startDate);
        Assertions.assertTrue(output.contains(endDate), 
            "Output should indicate filtering to " + endDate);
    }
    
    @Then("the output should contain a section for {string} with {int} items")
    public void outputShouldContainSectionWithItems(String section, int itemCount) {
        String output = outputStream.toString();
        
        // Try to find the section and count items
        Pattern pattern = Pattern.compile(section + ".*?(?=\\n\\n|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(output);
        
        Assertions.assertTrue(matcher.find(), "Output should contain a section for " + section);
        
        String sectionContent = matcher.group();
        
        // Count WI- patterns in the section
        Pattern itemPattern = Pattern.compile("WI-\\d+");
        matcher = itemPattern.matcher(sectionContent);
        
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        
        Assertions.assertEquals(itemCount, count, 
            section + " section should contain " + itemCount + " items");
    }
    
    @Then("the output should contain content formatted according to the template")
    public void outputShouldContainFormattedContent() {
        String output = outputStream.toString();
        
        // Check for HTML formatting if it's an HTML report
        if (output.contains("<!DOCTYPE html>") || output.contains("<html>")) {
            Assertions.assertTrue(output.contains("<style>"), 
                "Output should contain template's style section");
        }
        // Check for Markdown formatting if it's a Markdown report
        else if (output.contains("# ")) {
            Assertions.assertTrue(output.contains("**"), 
                "Output should contain Markdown formatting");
        }
    }
    
    @Then("template variables should be correctly substituted")
    public void templateVariablesShouldBeSubstituted() {
        String output = outputStream.toString();
        
        // Verify that no unsubstituted variables remain
        Assertions.assertFalse(output.contains("{{"), 
            "Output should not contain unsubstituted template variables");
        Assertions.assertFalse(output.contains("}}"), 
            "Output should not contain unsubstituted template variables");
    }
    
    @Then("the output should indicate that the report was sent via email")
    public void outputShouldIndicateEmailSent() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("sent via email") || output.contains("email sent"), 
            "Output should indicate that the report was sent via email");
    }
    
    @Then("an email should be sent to {string}")
    public void emailShouldBeSent(String recipient) {
        // In a real test, we would check an email service
        // Since we're using a mock email sender that writes to files, check the outbox
        try {
            // Check the simulated email outbox
            File outbox = new File("outbox");
            
            // Create outbox if it doesn't exist - just for test purposes
            if (!outbox.exists()) {
                outbox.mkdir();
                tempFiles.add(outbox.toPath());
            }
            
            File[] emails = outbox.listFiles((dir, name) -> 
                name.contains(recipient.replace('@', '_')));
            
            Assertions.assertTrue(emails != null && emails.length > 0, 
                "Email should be sent to " + recipient);
            
            // Add the email files to temp files for cleanup
            if (emails != null) {
                for (File email : emails) {
                    tempFiles.add(email.toPath());
                }
            }
        } catch (Exception e) {
            Assertions.fail("Failed to check email: " + e.getMessage());
        }
    }
    
    @Then("the email should have subject {string}")
    public void emailShouldHaveSubject(String subject) {
        // This would check the email's subject line
        // Since we're using a mock email sender, this is a simplified check
        File outbox = new File("outbox");
        File[] emails = outbox.listFiles((dir, name) -> name.contains("subject"));
        
        if (emails != null && emails.length > 0) {
            try {
                String content = Files.readString(emails[0].toPath());
                Assertions.assertTrue(content.contains(subject), 
                    "Email should have subject: " + subject);
            } catch (Exception e) {
                Assertions.fail("Failed to read email: " + e.getMessage());
            }
        } else {
            Assertions.fail("No email found to check subject");
        }
    }
    
    @Then("the email should contain the report content")
    public void emailShouldContainReportContent() {
        // This would check the email's content
        // Since we're using a mock email sender, this is a simplified check
        File outbox = new File("outbox");
        File[] emails = outbox.listFiles();
        
        if (emails != null && emails.length > 0) {
            try {
                String content = Files.readString(emails[0].toPath());
                Assertions.assertTrue(content.contains("Work Item") || 
                                   content.contains("Report"), 
                    "Email should contain report content");
            } catch (Exception e) {
                Assertions.fail("Failed to read email: " + e.getMessage());
            }
        } else {
            Assertions.fail("No email found to check content");
        }
    }
    
    @Then("the output should indicate that the report has been scheduled")
    public void outputShouldIndicateReportScheduled() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("scheduled") || output.contains("added successfully"), 
            "Output should indicate that the report has been scheduled");
        
        // Extract and store the report ID for later use
        Pattern pattern = Pattern.compile("ID: ([a-f0-9-]+)");
        Matcher matcher = pattern.matcher(output);
        
        if (matcher.find()) {
            reportIdMap.put("Weekly Status", matcher.group(1));
        }
    }
    
    @Then("the scheduled report details should include:")
    public void scheduledReportDetailsShouldInclude(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        String output = outputStream.toString();
        
        for (Map<String, String> row : rows) {
            String field = row.get("Field");
            String value = row.get("Value");
            
            // Check that the output contains the field and value
            Assertions.assertTrue(output.contains(field) && output.contains(value), 
                "Output should contain " + field + " with value " + value);
        }
    }
    
    @Then("the output should indicate that the report has been removed")
    public void outputShouldIndicateReportRemoved() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("removed") || output.contains("deleted"), 
            "Output should indicate that the report has been removed");
    }
    
    @Then("the output should indicate that the scheduler has been started")
    public void outputShouldIndicateSchedulerStarted() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("started"), 
            "Output should indicate that the scheduler has been started");
    }
    
    @Then("the output should indicate that the scheduler has been stopped")
    public void outputShouldIndicateSchedulerStopped() {
        String output = outputStream.toString();
        Assertions.assertTrue(output.contains("stopped"), 
            "Output should indicate that the scheduler has been stopped");
    }
}