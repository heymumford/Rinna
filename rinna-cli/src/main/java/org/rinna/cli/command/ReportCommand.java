/*
 * Report command for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.command;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockReportService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to generate reports about work items.
 * Follows the ViewCommand pattern with operation tracking.
 */
public class ReportCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ReportCommand.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Command parameters
    private String type;
    private String format;
    private String output;
    private String title;
    private String startDate;
    private String endDate;
    private String projectId;
    private String sortField;
    private boolean ascending = true;
    private String groupBy;
    private int limit;
    private boolean noHeader;
    private boolean noTimestamp;
    private Map<String, String> filters = new HashMap<>();
    private boolean emailEnabled;
    private List<String> emailRecipients = new ArrayList<>();
    private String emailSubject;
    private String templateName;
    private boolean noTemplate;
    private boolean verbose = false;
    private String username = System.getProperty("user.name");
    
    // Service dependencies
    private final ServiceManager serviceManager;
    private final MockReportService reportService;
    private final MetadataService metadataService;
    private final ContextManager contextManager;
    
    /**
     * Constructs a new report command with default service manager.
     */
    public ReportCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructs a new report command with the provided service manager.
     * This constructor allows for dependency injection, making the command more testable.
     * 
     * @param serviceManager the service manager to use
     */
    public ReportCommand(ServiceManager serviceManager) {
        // Default values
        this.type = "summary";
        this.format = "text";
        this.limit = 0; // No limit
        this.serviceManager = serviceManager;
        this.reportService = serviceManager.getMockReportService();
        this.metadataService = serviceManager.getMetadataService();
        this.contextManager = ContextManager.getInstance();
    }
    
    /**
     * Executes the command with proper operation tracking.
     */
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("format", format);
        params.put("username", username);
        params.put("verbose", verbose);
        
        // Add optional parameters when present
        if (output != null) params.put("output", output);
        if (title != null) params.put("title", title);
        if (startDate != null) params.put("startDate", startDate);
        if (endDate != null) params.put("endDate", endDate);
        if (projectId != null) params.put("projectId", projectId);
        if (sortField != null) params.put("sortField", sortField);
        if (groupBy != null) params.put("groupBy", groupBy);
        if (limit > 0) params.put("limit", limit);
        if (!filters.isEmpty()) params.put("filters", new HashMap<>(filters));
        if (emailEnabled) params.put("emailEnabled", true);
        
        // Start tracking main operation
        String operationId = metadataService.startOperation("report", "READ", params);
        
        try {
            // Log details in verbose mode
            if (verbose) {
                LOGGER.info("Generating " + type + " report in " + format + " format");
            }
            
            // Parse report type and format
            ReportType reportType = reportService.parseReportType(type);
            ReportFormat reportFormat = reportService.parseReportFormat(format);
            
            // Track report configuration creation
            String configOpId = metadataService.startOperation("report-config", "CREATE", params);
            
            try {
                // Create a configuration
                ReportConfig config = createReportConfig(reportType, reportFormat);
                
                // Record success of config creation
                Map<String, Object> configResult = new HashMap<>();
                configResult.put("reportType", reportType.name());
                configResult.put("reportFormat", reportFormat.name());
                metadataService.completeOperation(configOpId, configResult);
                
                // Start tracking report generation
                Map<String, Object> genParams = new HashMap<>();
                genParams.put("reportType", reportType.name());
                genParams.put("reportFormat", reportFormat.name());
                String genOpId = metadataService.startOperation("report-generate", "EXECUTE", genParams);
                
                try {
                    // Generate the report
                    boolean success = reportService.generateReport(config);
                    
                    // Record success or failure of generation
                    Map<String, Object> genResult = new HashMap<>();
                    genResult.put("success", success);
                    if (config.getOutputPath() != null) {
                        genResult.put("outputPath", config.getOutputPath());
                    }
                    metadataService.completeOperation(genOpId, genResult);
                    
                    // Format and display the output
                    boolean isJsonOutput = "json".equalsIgnoreCase(format);
                    int resultCode = displayReportResult(success, config, isJsonOutput);
                    
                    // Record final result
                    Map<String, Object> finalResult = new HashMap<>();
                    finalResult.put("success", success);
                    finalResult.put("resultCode", resultCode);
                    metadataService.completeOperation(operationId, finalResult);
                    
                    return resultCode;
                } catch (Exception e) {
                    // Record failure of generation
                    metadataService.failOperation(genOpId, e);
                    throw e;
                }
            } catch (Exception e) {
                // Record failure of config creation
                metadataService.failOperation(configOpId, e);
                throw e;
            }
        } catch (Exception e) {
            // Record failure of main operation
            metadataService.failOperation(operationId, e);
            
            // Handle errors based on format and verbosity
            return handleError(e);
        }
    }
    
    /**
     * Displays the report generation result with proper formatting.
     * 
     * @param success whether the report generation was successful
     * @param config the report configuration
     * @param isJsonOutput whether to output in JSON format
     * @return the exit code (0 for success, 1 for failure)
     */
    private int displayReportResult(boolean success, ReportConfig config, boolean isJsonOutput) {
        if (isJsonOutput) {
            displayReportResultAsJson(success, config);
        } else {
            displayReportResultAsText(success, config);
        }
        
        return success ? 0 : 1;
    }
    
    /**
     * Displays the report generation result in JSON format.
     * 
     * @param success whether the report generation was successful
     * @param config the report configuration
     */
    private void displayReportResultAsJson(boolean success, ReportConfig config) {
        OutputFormatter formatter = new OutputFormatter(true);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("type", config.getType().name());
        result.put("format", config.getFormat().name());
        
        if (config.getOutputPath() != null) {
            result.put("outputPath", config.getOutputPath());
            // Check if file exists and add additional information
            File outputFile = new File(config.getOutputPath());
            if (outputFile.exists()) {
                result.put("fileSize", outputFile.length());
                result.put("fileExists", true);
            } else {
                result.put("fileExists", false);
            }
        } else {
            result.put("outputPath", "console");
        }
        
        if (config.getTitle() != null) {
            result.put("title", config.getTitle());
        }
        
        if (config.getStartDate() != null) {
            result.put("startDate", config.getStartDate().format(DATE_FORMATTER));
        }
        
        if (config.getEndDate() != null) {
            result.put("endDate", config.getEndDate().format(DATE_FORMATTER));
        }
        
        result.put("includeHeader", config.isIncludeHeader());
        result.put("includeTimestamp", config.isIncludeTimestamp());
        
        if (config.isEmailEnabled()) {
            result.put("emailEnabled", true);
            result.put("emailRecipients", config.getEmailRecipients());
            if (config.getEmailSubject() != null) {
                result.put("emailSubject", config.getEmailSubject());
            }
        }
        
        formatter.outputObject("report", result);
    }
    
    /**
     * Displays the report generation result in text format.
     * 
     * @param success whether the report generation was successful
     * @param config the report configuration
     */
    private void displayReportResultAsText(boolean success, ReportConfig config) {
        if (success) {
            if (config.getOutputPath() != null) {
                System.out.println("Report generated successfully: " + config.getOutputPath());
            } else {
                System.out.println("Report generated successfully.");
            }
            
            if (config.isEmailEnabled() && !config.getEmailRecipients().isEmpty()) {
                System.out.println("Report sent to: " + String.join(", ", config.getEmailRecipients()));
            }
        } else {
            System.err.println("Failed to generate report.");
        }
    }
    
    /**
     * Handles an error during report generation.
     * 
     * @param exception the exception that occurred
     * @return the exit code (always 1 for error)
     */
    private int handleError(Exception exception) {
        boolean isJsonOutput = "json".equalsIgnoreCase(format);
        
        if (isJsonOutput) {
            OutputFormatter formatter = new OutputFormatter(true);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error executing report command");
            error.put("message", exception.getMessage());
            formatter.outputObject("error", error);
        } else {
            if (verbose) {
                System.err.println("Error executing report command: " + exception.getMessage());
                exception.printStackTrace();
            } else {
                System.err.println("Error: " + exception.getMessage());
            }
        }
        
        return 1;
    }
    
    /**
     * Creates a report configuration based on the command options.
     * 
     * @param reportType the report type
     * @param reportFormat the report format
     * @return the report configuration
     */
    private ReportConfig createReportConfig(ReportType reportType, ReportFormat reportFormat) {
        // Start with default configuration for the type
        ReportConfig config = ReportConfig.createDefault(reportType);
        
        // Set basic options
        config.setFormat(reportFormat);
        config.setIncludeHeader(!noHeader);
        config.setIncludeTimestamp(!noTimestamp);
        
        // Set output path
        String outputPath = resolveOutputPath(output, reportType, reportFormat);
        config.setOutputPath(outputPath);
        
        // Set title if provided
        if (title != null && !title.isEmpty()) {
            config.setTitle(title);
        }
        
        // Set date range if provided
        LocalDate startDateValue = parseDate(startDate);
        if (startDateValue != null) {
            config.setStartDate(startDateValue);
        }
        
        LocalDate endDateValue = parseDate(endDate);
        if (endDateValue != null) {
            config.setEndDate(endDateValue);
        }
        
        // Set project if provided
        if (projectId != null && !projectId.isEmpty()) {
            config.setProjectId(projectId);
        }
        
        // Set sorting if provided
        if (sortField != null && !sortField.isEmpty()) {
            config.setSortField(sortField);
            config.setAscending(ascending);
        }
        
        // Set grouping if provided
        if (groupBy != null && !groupBy.isEmpty()) {
            config.setGroupBy(groupBy);
        }
        
        // Set limit if provided
        if (limit > 0) {
            config.setMaxItems(limit);
        }
        
        // Set filters
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            config.addFilter(filter.getKey(), filter.getValue());
        }
        
        // Set email settings
        if (emailEnabled && !emailRecipients.isEmpty()) {
            config.setEmailEnabled(true);
            
            for (String recipient : emailRecipients) {
                config.addEmailRecipient(recipient);
            }
            
            if (emailSubject != null && !emailSubject.isEmpty()) {
                config.setEmailSubject(emailSubject);
            }
        }
        
        // Set template settings
        if (noTemplate) {
            config.setUseTemplate(false);
        } else if (templateName != null && !templateName.isEmpty()) {
            config.setTemplateName(templateName);
        }
        
        return config;
    }
    
    /**
     * Resolves the output path for a report.
     * 
     * @param basePath the base path (can be null)
     * @param type the report type
     * @param format the report format
     * @return the resolved output path, or null if the report should be output to console
     */
    private String resolveOutputPath(String basePath, ReportType type, ReportFormat format) {
        if (basePath == null || basePath.isEmpty() || basePath.equals("-")) {
            return null;
        }
        
        // Convert to absolute path if needed
        String absolutePath = basePath;
        File baseFile = new File(basePath);
        
        // If not already an absolute path, make it absolute
        if (!baseFile.isAbsolute()) {
            String projectRoot = System.getProperty("user.dir");
            baseFile = new File(projectRoot, basePath);
            absolutePath = baseFile.getAbsolutePath();
        }
        
        // Check if the path is a directory
        if (baseFile.isDirectory() || basePath.endsWith("/") || basePath.endsWith("\\")) {
            // Create a filename based on the report type and format
            String fileName = "rinna_" + type.name().toLowerCase() + "_report." + format.getFileExtension();
            return new File(absolutePath, fileName).getAbsolutePath();
        }
        
        // If no extension is provided, add the format extension
        if (!absolutePath.contains(".")) {
            return absolutePath + "." + format.getFileExtension();
        }
        
        // Use the absolute path
        return absolutePath;
    }
    
    /**
     * Parses a date string to a LocalDate.
     * 
     * @param dateStr the date string
     * @return the parsed date, or null if the string is null, empty, or invalid
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            LOGGER.warning("Invalid date format: " + dateStr + ", expected yyyy-MM-dd");
            return null;
        }
    }
    
    /**
     * Sets the report type.
     * 
     * @param type the report type
     * @return this command for chaining
     */
    public ReportCommand setType(String type) {
        this.type = type;
        return this;
    }
    
    /**
     * Sets the report format.
     * 
     * @param format the report format
     * @return this command for chaining
     */
    public ReportCommand setFormat(String format) {
        this.format = format;
        return this;
    }
    
    /**
     * Sets the output path.
     * 
     * @param output the output path
     * @return this command for chaining
     */
    public ReportCommand setOutput(String output) {
        this.output = output;
        return this;
    }
    
    /**
     * Sets the report title.
     * 
     * @param title the report title
     * @return this command for chaining
     */
    public ReportCommand setTitle(String title) {
        this.title = title;
        return this;
    }
    
    /**
     * Sets the start date for filtering.
     * 
     * @param startDate the start date
     * @return this command for chaining
     */
    public ReportCommand setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }
    
    /**
     * Sets the end date for filtering.
     * 
     * @param endDate the end date
     * @return this command for chaining
     */
    public ReportCommand setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }
    
    /**
     * Sets the project ID for filtering.
     * 
     * @param projectId the project ID
     * @return this command for chaining
     */
    public ReportCommand setProjectId(String projectId) {
        this.projectId = projectId;
        return this;
    }
    
    /**
     * Sets the sort field.
     * 
     * @param sortField the sort field
     * @return this command for chaining
     */
    public ReportCommand setSortField(String sortField) {
        this.sortField = sortField;
        return this;
    }
    
    /**
     * Sets whether sorting is ascending.
     * 
     * @param ascending true for ascending, false for descending
     * @return this command for chaining
     */
    public ReportCommand setAscending(boolean ascending) {
        this.ascending = ascending;
        return this;
    }
    
    /**
     * Sets the group by field.
     * 
     * @param groupBy the group by field
     * @return this command for chaining
     */
    public ReportCommand setGroupBy(String groupBy) {
        this.groupBy = groupBy;
        return this;
    }
    
    /**
     * Sets the maximum number of items to include.
     * 
     * @param limit the maximum number of items
     * @return this command for chaining
     */
    public ReportCommand setLimit(int limit) {
        this.limit = limit;
        return this;
    }
    
    /**
     * Sets whether to include the header.
     * 
     * @param noHeader true to exclude the header
     * @return this command for chaining
     */
    public ReportCommand setNoHeader(boolean noHeader) {
        this.noHeader = noHeader;
        return this;
    }
    
    /**
     * Sets whether to include the timestamp.
     * 
     * @param noTimestamp true to exclude the timestamp
     * @return this command for chaining
     */
    public ReportCommand setNoTimestamp(boolean noTimestamp) {
        this.noTimestamp = noTimestamp;
        return this;
    }
    
    /**
     * Adds a filter.
     * 
     * @param field the field to filter on
     * @param value the filter value
     * @return this command for chaining
     */
    public ReportCommand addFilter(String field, String value) {
        this.filters.put(field, value);
        return this;
    }
    
    /**
     * Sets the filters.
     * 
     * @param filters the filters
     * @return this command for chaining
     */
    public ReportCommand setFilters(Map<String, String> filters) {
        this.filters = new HashMap<>(filters);
        return this;
    }
    
    /**
     * Sets whether to enable email delivery.
     * 
     * @param emailEnabled true to enable email delivery
     * @return this command for chaining
     */
    public ReportCommand setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
        return this;
    }
    
    /**
     * Sets the email recipients.
     * 
     * @param emailRecipients comma-separated list of email recipients
     * @return this command for chaining
     */
    public ReportCommand setEmailRecipients(String emailRecipients) {
        this.emailRecipients.clear();
        if (emailRecipients != null && !emailRecipients.isEmpty()) {
            String[] recipients = emailRecipients.split(",");
            for (String recipient : recipients) {
                String trimmed = recipient.trim();
                if (!trimmed.isEmpty()) {
                    this.emailRecipients.add(trimmed);
                }
            }
        }
        return this;
    }
    
    /**
     * Adds an email recipient.
     * 
     * @param emailRecipient the email recipient to add
     * @return this command for chaining
     */
    public ReportCommand addEmailRecipient(String emailRecipient) {
        if (emailRecipient != null && !emailRecipient.isEmpty()) {
            this.emailRecipients.add(emailRecipient.trim());
        }
        return this;
    }
    
    /**
     * Sets the email subject.
     * 
     * @param emailSubject the email subject
     * @return this command for chaining
     */
    public ReportCommand setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
        return this;
    }
    
    /**
     * Sets the template name.
     * 
     * @param templateName the template name
     * @return this command for chaining
     */
    public ReportCommand setTemplateName(String templateName) {
        this.templateName = templateName;
        return this;
    }
    
    /**
     * Sets whether to disable templates.
     * 
     * @param noTemplate true to disable templates
     * @return this command for chaining
     */
    public ReportCommand setNoTemplate(boolean noTemplate) {
        this.noTemplate = noTemplate;
        return this;
    }
    
    /**
     * Sets whether to output JSON.
     * This is a legacy setter that maps to the format parameter.
     *
     * @param jsonOutput true to output JSON
     * @return this command for chaining
     */
    public ReportCommand setJsonOutput(boolean jsonOutput) {
        if (jsonOutput) {
            this.format = "json";
        }
        return this;
    }
    
    /**
     * Sets the username for operation tracking.
     *
     * @param username the username
     * @return this command for chaining
     */
    public ReportCommand setUsername(String username) {
        this.username = username;
        return this;
    }
    
    /**
     * Sets whether to enable verbose output.
     *
     * @param verbose true to enable verbose output
     * @return this command for chaining
     */
    public ReportCommand setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
    
    /**
     * Prints the help for this command.
     */
    public static void printHelp() {
        System.out.println("Usage: rin report <type> [options]");
        System.out.println();
        System.out.println("Types:");
        System.out.println("  summary      Summary report of all work items (default)");
        System.out.println("  detailed     Detailed report of work items with all fields");
        System.out.println("  status       Status report showing work item state distribution");
        System.out.println("  progress     Progress report showing completion metrics");
        System.out.println("  assignee     Assignee report showing work items by assignee");
        System.out.println("  priority     Priority report showing work items by priority");
        System.out.println("  overdue      Overdue report showing late or at-risk items");
        System.out.println("  timeline     Timeline report showing expected completion times");
        System.out.println("  burndown     Burndown report showing progress over time");
        System.out.println("  activity     Activity report showing recent changes");
        System.out.println("  custom       Custom report with user-defined filters and fields");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --format=<format>    Output format (text, csv, json, md, html, xml)");
        System.out.println("  --output=<path>      Output file path (default: console)");
        System.out.println("  --title=<title>      Report title");
        System.out.println("  --start=<date>       Start date for filtering (yyyy-MM-dd)");
        System.out.println("  --end=<date>         End date for filtering (yyyy-MM-dd)");
        System.out.println("  --project=<id>       Filter by project ID");
        System.out.println("  --sort=<field>       Sort by field");
        System.out.println("  --desc               Sort in descending order");
        System.out.println("  --group=<field>      Group by field");
        System.out.println("  --limit=<n>          Limit to n items");
        System.out.println("  --no-header          Exclude header from report");
        System.out.println("  --no-timestamp       Exclude timestamp from report");
        System.out.println("  --filter=<field=value>  Filter by field value");
        System.out.println("  --email              Enable email delivery of report");
        System.out.println("  --email-to=<addr>    Set email recipients (comma-separated)");
        System.out.println("  --email-subject=<s>  Set email subject line");
        System.out.println("  --template=<n>       Use specific template name");
        System.out.println("  --no-template        Disable template usage");
        System.out.println("  --json               Output results in JSON format (legacy, same as --format=json)");
        System.out.println("  --verbose            Enable verbose output");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  rin report summary --format=html --output=report.html");
        System.out.println("  rin report status --group=priority");
        System.out.println("  rin report overdue --start=2025-01-01 --end=2025-12-31");
        System.out.println("  rin report assignee --filter=state=IN_PROGRESS");
        System.out.println("  rin report summary --email --email-to=team@example.com");
        System.out.println("  rin report summary --json");
    }
}