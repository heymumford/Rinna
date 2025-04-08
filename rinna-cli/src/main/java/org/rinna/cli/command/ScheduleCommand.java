/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportScheduler;
import org.rinna.cli.report.ReportScheduler.ScheduleType;
import org.rinna.cli.report.ReportScheduler.ScheduledReport;
import org.rinna.cli.report.ReportService;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.service.MockReportService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.io.File;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Command to manage scheduled reports.
 * This command allows adding, listing, showing, and removing scheduled reports,
 * as well as starting and stopping the report scheduler.
 */
public class ScheduleCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ScheduleCommand.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private String action;
    private String id;
    private String name;
    private String description;
    private String scheduleType;
    private String time;
    private String dayOfWeek;
    private int dayOfMonth;
    private String reportType;
    private String reportFormat;
    private String outputPath;
    private String title;
    private boolean emailEnabled;
    private List<String> emailRecipients = new ArrayList<>();
    private String emailSubject;
    private boolean jsonOutput = false;
    private boolean verbose = false;
    
    private final ServiceManager serviceManager = ServiceManager.getInstance();
    private final MockReportService reportService;
    
    /**
     * Constructs a new schedule command.
     */
    public ScheduleCommand() {
        // Default values
        this.action = "list";
        this.reportService = serviceManager.getMockReportService();
    }
    
    /**
     * Executes the command.
     */
    @Override
    public Integer call() {
        try {
            // Track operation metadata
            Map<String, Object> operationParams = new HashMap<>();
            operationParams.put("action", action);
            if (id != null) operationParams.put("id", id);
            if (name != null) operationParams.put("name", name);
            if (reportType != null) operationParams.put("reportType", reportType);
            
            serviceManager.getMetadataService().trackOperation("schedule", operationParams);
            
            ReportScheduler scheduler = ReportScheduler.getInstance();
            
            switch (action.toLowerCase()) {
                case "list":
                    return listSchedules(scheduler);
                case "add":
                    return addSchedule(scheduler);
                case "remove":
                    return removeSchedule(scheduler);
                case "start":
                    scheduler.start();
                    if (jsonOutput) {
                        OutputFormatter formatter = new OutputFormatter(true);
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", "started");
                        result.put("reportCount", scheduler.getScheduledReports().size());
                        formatter.outputObject("scheduler", result);
                    } else {
                        System.out.println("Report scheduler started");
                    }
                    return 0;
                case "stop":
                    scheduler.stop();
                    if (jsonOutput) {
                        OutputFormatter formatter = new OutputFormatter(true);
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", "stopped");
                        formatter.outputObject("scheduler", result);
                    } else {
                        System.out.println("Report scheduler stopped");
                    }
                    return 0;
                case "show":
                    return showSchedule(scheduler);
                default:
                    if (jsonOutput) {
                        OutputFormatter formatter = new OutputFormatter(true);
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", "Unknown action");
                        error.put("action", action);
                        error.put("validActions", List.of("list", "add", "remove", "start", "stop", "show"));
                        formatter.outputObject("error", error);
                    } else {
                        System.err.println("Error: Unknown action: " + action);
                        System.err.println("Valid actions: list, add, remove, start, stop, show");
                    }
                    return 1;
            }
        } catch (Exception e) {
            if (jsonOutput) {
                OutputFormatter formatter = new OutputFormatter(true);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Error executing schedule command");
                error.put("message", e.getMessage());
                formatter.outputObject("error", error);
            } else {
                if (verbose) {
                    System.err.println("Error executing schedule command: " + e.getMessage());
                    e.printStackTrace();
                } else {
                    System.err.println("Error: " + e.getMessage());
                }
            }
            return 1;
        }
    }
    
    /**
     * Lists all scheduled reports.
     * 
     * @param scheduler the report scheduler
     * @return the exit code
     */
    private int listSchedules(ReportScheduler scheduler) {
        List<ScheduledReport> reports = scheduler.getScheduledReports();
        
        if (jsonOutput) {
            OutputFormatter formatter = new OutputFormatter(true);
            
            Map<String, Object> result = new HashMap<>();
            result.put("count", reports.size());
            
            List<Map<String, Object>> reportData = new ArrayList<>();
            for (ScheduledReport report : reports) {
                Map<String, Object> reportInfo = reportToMap(report);
                reportData.add(reportInfo);
            }
            
            result.put("reports", reportData);
            formatter.outputObject("scheduledReports", result);
            return 0;
        } else {
            if (reports.isEmpty()) {
                System.out.println("No scheduled reports found");
                return 0;
            }
            
            System.out.println("Scheduled Reports:");
            System.out.println("------------------");
            
            for (ScheduledReport report : reports) {
                System.out.println("ID: " + report.getId());
                System.out.println("Name: " + report.getName());
                System.out.println("Type: " + report.getScheduleType());
                System.out.println("Time: " + report.getTime());
                
                if (report.getScheduleType() == ScheduleType.WEEKLY && report.getDayOfWeek() != null) {
                    System.out.println("Day of Week: " + report.getDayOfWeek());
                } else if (report.getScheduleType() == ScheduleType.MONTHLY && report.getDayOfMonth() > 0) {
                    System.out.println("Day of Month: " + report.getDayOfMonth());
                }
                
                ReportConfig config = report.getConfig();
                System.out.println("Report Type: " + config.getType());
                System.out.println("Report Format: " + config.getFormat());
                
                if (config.getOutputPath() != null) {
                    System.out.println("Output Path: " + config.getOutputPath());
                }
                
                if (config.isEmailEnabled() && !config.getEmailRecipients().isEmpty()) {
                    System.out.println("Email Recipients: " + String.join(", ", config.getEmailRecipients()));
                }
                
                // Show more details in verbose mode
                if (verbose) {
                    if (config.getSortField() != null) {
                        System.out.println("Sort Field: " + config.getSortField() + 
                                (config.isAscending() ? " (ascending)" : " (descending)"));
                    }
                    
                    if (config.isGroupByEnabled()) {
                        System.out.println("Group By: " + config.getGroupByField());
                    }
                    
                    if (config.getDescription() != null) {
                        System.out.println("Description: " + config.getDescription());
                    }
                    
                    // Calculate next run time
                    LocalDateTime nextRun = calculateNextRunTime(report);
                    if (nextRun != null) {
                        System.out.println("Next Run: " + nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    }
                }
                
                System.out.println();
            }
            
            return 0;
        }
    }
    
    /**
     * Adds a scheduled report.
     * 
     * @param scheduler the report scheduler
     * @return the exit code
     */
    private int addSchedule(ReportScheduler scheduler) {
        try {
            // Validate required fields
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name is required");
            }
            
            if (scheduleType == null || scheduleType.isEmpty()) {
                throw new IllegalArgumentException("Schedule type is required (daily, weekly, monthly)");
            }
            
            if (time == null || time.isEmpty()) {
                throw new IllegalArgumentException("Time is required (HH:MM format)");
            }
            
            if (reportType == null || reportType.isEmpty()) {
                throw new IllegalArgumentException("Report type is required");
            }
            
            // Parse schedule type
            ScheduleType type;
            try {
                type = ScheduleType.valueOf(scheduleType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid schedule type: " + scheduleType + 
                        ". Valid types: daily, weekly, monthly");
            }
            
            // Validate day of week for weekly schedules
            DayOfWeek day = null;
            if (type == ScheduleType.WEEKLY) {
                if (dayOfWeek == null || dayOfWeek.isEmpty()) {
                    throw new IllegalArgumentException("Day of week is required for weekly schedules");
                }
                
                try {
                    day = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek + 
                            ". Valid days: monday, tuesday, wednesday, thursday, friday, saturday, sunday");
                }
            }
            
            // Validate day of month for monthly schedules
            if (type == ScheduleType.MONTHLY && dayOfMonth <= 0) {
                throw new IllegalArgumentException("Day of month is required for monthly schedules");
            }
            
            // Parse report type using the service
            ReportType rType = reportService.parseReportType(reportType);
            
            // Parse report format
            ReportFormat rFormat = ReportFormat.TEXT;
            if (reportFormat != null && !reportFormat.isEmpty()) {
                rFormat = reportService.parseReportFormat(reportFormat);
            }
            
            // Create the scheduled report
            ScheduledReport report = new ScheduledReport();
            report.setName(name);
            report.setDescription(description);
            report.setScheduleType(type);
            report.setTime(time);
            
            if (type == ScheduleType.WEEKLY) {
                report.setDayOfWeek(day);
            } else if (type == ScheduleType.MONTHLY) {
                report.setDayOfMonth(dayOfMonth);
            }
            
            // Create the report config
            ReportConfig config = ReportConfig.createDefault(rType);
            config.setFormat(rFormat);
            
            if (outputPath != null && !outputPath.isEmpty()) {
                String resolvedPath = resolveOutputPath(outputPath, rType, rFormat);
                config.setOutputPath(resolvedPath);
            }
            
            if (title != null && !title.isEmpty()) {
                config.setTitle(title);
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
            
            report.setConfig(config);
            
            // Add the report
            boolean success = scheduler.addScheduledReport(report);
            
            if (success) {
                if (jsonOutput) {
                    OutputFormatter formatter = new OutputFormatter(true);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("id", report.getId());
                    result.put("name", report.getName());
                    result.put("report", reportToMap(report));
                    formatter.outputObject("addedSchedule", result);
                } else {
                    System.out.println("Scheduled report added successfully");
                    System.out.println("ID: " + report.getId());
                }
                return 0;
            } else {
                throw new IllegalStateException("Error adding scheduled report");
            }
        } catch (Exception e) {
            if (jsonOutput) {
                OutputFormatter formatter = new OutputFormatter(true);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Error adding schedule");
                error.put("message", e.getMessage());
                formatter.outputObject("error", error);
            } else {
                System.err.println("Error: " + e.getMessage());
            }
            return 1;
        }
    }
    
    /**
     * Removes a scheduled report.
     * 
     * @param scheduler the report scheduler
     * @return the exit code
     */
    private int removeSchedule(ReportScheduler scheduler) {
        try {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("ID is required");
            }
            
            // Get the report before removing it (for JSON output)
            ScheduledReport report = scheduler.getScheduledReport(id);
            
            boolean success = scheduler.removeScheduledReport(id);
            
            if (success) {
                if (jsonOutput) {
                    OutputFormatter formatter = new OutputFormatter(true);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("id", id);
                    if (report != null) {
                        result.put("name", report.getName());
                    }
                    formatter.outputObject("removedSchedule", result);
                } else {
                    System.out.println("Scheduled report removed successfully");
                }
                return 0;
            } else {
                throw new IllegalStateException("Scheduled report not found with ID: " + id);
            }
        } catch (Exception e) {
            if (jsonOutput) {
                OutputFormatter formatter = new OutputFormatter(true);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Error removing schedule");
                error.put("message", e.getMessage());
                formatter.outputObject("error", error);
            } else {
                System.err.println("Error: " + e.getMessage());
            }
            return 1;
        }
    }
    
    /**
     * Shows a scheduled report.
     * 
     * @param scheduler the report scheduler
     * @return the exit code
     */
    private int showSchedule(ReportScheduler scheduler) {
        try {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("ID is required");
            }
            
            ScheduledReport report = scheduler.getScheduledReport(id);
            
            if (report == null) {
                throw new IllegalArgumentException("Scheduled report not found with ID: " + id);
            }
            
            if (jsonOutput) {
                OutputFormatter formatter = new OutputFormatter(true);
                Map<String, Object> result = reportToMap(report);
                
                // Add next run time calculation
                LocalDateTime nextRun = calculateNextRunTime(report);
                if (nextRun != null) {
                    result.put("nextRun", nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
                
                formatter.outputObject("scheduledReport", result);
                return 0;
            } else {
                System.out.println("ID: " + report.getId());
                System.out.println("Name: " + report.getName());
                
                if (report.getDescription() != null && !report.getDescription().isEmpty()) {
                    System.out.println("Description: " + report.getDescription());
                }
                
                System.out.println("Schedule Type: " + report.getScheduleType());
                System.out.println("Time: " + report.getTime());
                
                if (report.getScheduleType() == ScheduleType.WEEKLY && report.getDayOfWeek() != null) {
                    System.out.println("Day of Week: " + report.getDayOfWeek());
                } else if (report.getScheduleType() == ScheduleType.MONTHLY && report.getDayOfMonth() > 0) {
                    System.out.println("Day of Month: " + report.getDayOfMonth());
                }
                
                ReportConfig config = report.getConfig();
                System.out.println("Report Type: " + config.getType());
                System.out.println("Report Format: " + config.getFormat());
                
                if (config.getOutputPath() != null) {
                    System.out.println("Output Path: " + config.getOutputPath());
                }
                
                if (config.getTitle() != null) {
                    System.out.println("Report Title: " + config.getTitle());
                }
                
                if (config.isEmailEnabled()) {
                    System.out.println("Email Enabled: Yes");
                    
                    if (!config.getEmailRecipients().isEmpty()) {
                        System.out.println("Email Recipients: " + String.join(", ", config.getEmailRecipients()));
                    }
                    
                    if (config.getEmailSubject() != null) {
                        System.out.println("Email Subject: " + config.getEmailSubject());
                    }
                } else {
                    System.out.println("Email Enabled: No");
                }
                
                // Show more details in verbose mode
                if (verbose) {
                    if (config.getSortField() != null) {
                        System.out.println("Sort Field: " + config.getSortField() + 
                                (config.isAscending() ? " (ascending)" : " (descending)"));
                    }
                    
                    if (config.isGroupByEnabled()) {
                        System.out.println("Group By: " + config.getGroupByField());
                    }
                    
                    Map<String, String> filters = config.getFilters();
                    if (!filters.isEmpty()) {
                        System.out.println("Filters:");
                        for (Map.Entry<String, String> filter : filters.entrySet()) {
                            System.out.println("  " + filter.getKey() + ": " + filter.getValue());
                        }
                    }
                    
                    // Calculate next run time
                    LocalDateTime nextRun = calculateNextRunTime(report);
                    if (nextRun != null) {
                        System.out.println("Next Run: " + nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    }
                }
                
                return 0;
            }
        } catch (Exception e) {
            if (jsonOutput) {
                OutputFormatter formatter = new OutputFormatter(true);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Error showing schedule");
                error.put("message", e.getMessage());
                formatter.outputObject("error", error);
            } else {
                System.err.println("Error: " + e.getMessage());
            }
            return 1;
        }
    }
    
    /**
     * Converts a ScheduledReport to a Map for JSON serialization.
     * 
     * @param report the report to convert
     * @return the map representation
     */
    private Map<String, Object> reportToMap(ScheduledReport report) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", report.getId());
        result.put("name", report.getName());
        
        if (report.getDescription() != null) {
            result.put("description", report.getDescription());
        }
        
        result.put("scheduleType", report.getScheduleType().name());
        result.put("time", report.getTime());
        
        if (report.getScheduleType() == ScheduleType.WEEKLY && report.getDayOfWeek() != null) {
            result.put("dayOfWeek", report.getDayOfWeek().name());
        } else if (report.getScheduleType() == ScheduleType.MONTHLY && report.getDayOfMonth() > 0) {
            result.put("dayOfMonth", report.getDayOfMonth());
        }
        
        ReportConfig config = report.getConfig();
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("type", config.getType().name());
        configMap.put("format", config.getFormat().name());
        
        if (config.getOutputPath() != null) {
            configMap.put("outputPath", config.getOutputPath());
        }
        
        if (config.getTitle() != null) {
            configMap.put("title", config.getTitle());
        }
        
        configMap.put("emailEnabled", config.isEmailEnabled());
        
        if (config.isEmailEnabled() && !config.getEmailRecipients().isEmpty()) {
            configMap.put("emailRecipients", config.getEmailRecipients());
            
            if (config.getEmailSubject() != null) {
                configMap.put("emailSubject", config.getEmailSubject());
            }
        }
        
        result.put("config", configMap);
        
        return result;
    }
    
    /**
     * Calculates the next run time for a scheduled report.
     * 
     * @param report the report
     * @return the next run time, or null if it cannot be calculated
     */
    private LocalDateTime calculateNextRunTime(ScheduledReport report) {
        try {
            // Parse the time
            String[] timeParts = report.getTime().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            // Get the current time
            LocalDateTime now = LocalDateTime.now();
            
            // Calculate the next execution time
            LocalDateTime nextExecution = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
            
            // If the next execution time is in the past, add the period
            if (nextExecution.isBefore(now)) {
                switch (report.getScheduleType()) {
                    case DAILY:
                        nextExecution = nextExecution.plusDays(1);
                        break;
                    case WEEKLY:
                        // Add days until we reach the desired day of week
                        if (report.getDayOfWeek() != null) {
                            while (nextExecution.getDayOfWeek() != report.getDayOfWeek()) {
                                nextExecution = nextExecution.plusDays(1);
                            }
                        }
                        break;
                    case MONTHLY:
                        // Add days until we reach the desired day of month
                        int dayOfMonth = report.getDayOfMonth();
                        if (dayOfMonth > 0) {
                            nextExecution = nextExecution.withDayOfMonth(dayOfMonth);
                            if (nextExecution.isBefore(now)) {
                                nextExecution = nextExecution.plusMonths(1);
                            }
                        }
                        break;
                }
            }
            
            return nextExecution;
        } catch (Exception e) {
            LOGGER.warning("Error calculating next run time: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Resolves the output path for a report, ensuring it's an absolute path.
     * 
     * @param basePath the base path
     * @param type the report type
     * @param format the report format
     * @return the resolved absolute path
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
            return Paths.get(absolutePath, fileName).toString();
        }
        
        // If no extension is provided, add the format extension
        if (!absolutePath.contains(".")) {
            return absolutePath + "." + format.getFileExtension();
        }
        
        // Use the absolute path
        return absolutePath;
    }
    
    /**
     * Sets the action.
     * 
     * @param action the action
     * @return this command for chaining
     */
    public ScheduleCommand setAction(String action) {
        this.action = action;
        return this;
    }
    
    /**
     * Sets the ID.
     * 
     * @param id the ID
     * @return this command for chaining
     */
    public ScheduleCommand setId(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * Sets the name.
     * 
     * @param name the name
     * @return this command for chaining
     */
    public ScheduleCommand setName(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * Sets the description.
     * 
     * @param description the description
     * @return this command for chaining
     */
    public ScheduleCommand setDescription(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * Sets the schedule type.
     * 
     * @param scheduleType the schedule type
     * @return this command for chaining
     */
    public ScheduleCommand setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
        return this;
    }
    
    /**
     * Sets the time.
     * 
     * @param time the time
     * @return this command for chaining
     */
    public ScheduleCommand setTime(String time) {
        this.time = time;
        return this;
    }
    
    /**
     * Sets the day of week.
     * 
     * @param dayOfWeek the day of week
     * @return this command for chaining
     */
    public ScheduleCommand setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        return this;
    }
    
    /**
     * Sets the day of month.
     * 
     * @param dayOfMonth the day of month
     * @return this command for chaining
     */
    public ScheduleCommand setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
        return this;
    }
    
    /**
     * Sets the report type.
     * 
     * @param reportType the report type
     * @return this command for chaining
     */
    public ScheduleCommand setReportType(String reportType) {
        this.reportType = reportType;
        return this;
    }
    
    /**
     * Sets the report format.
     * 
     * @param reportFormat the report format
     * @return this command for chaining
     */
    public ScheduleCommand setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
        return this;
    }
    
    /**
     * Sets the output path.
     * 
     * @param outputPath the output path
     * @return this command for chaining
     */
    public ScheduleCommand setOutputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }
    
    /**
     * Sets the title.
     * 
     * @param title the title
     * @return this command for chaining
     */
    public ScheduleCommand setTitle(String title) {
        this.title = title;
        return this;
    }
    
    /**
     * Sets whether email is enabled.
     * 
     * @param emailEnabled true to enable email
     * @return this command for chaining
     */
    public ScheduleCommand setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
        return this;
    }
    
    /**
     * Sets the email recipients.
     * 
     * @param emailRecipients comma-separated list of email recipients
     * @return this command for chaining
     */
    public ScheduleCommand setEmailRecipients(String emailRecipients) {
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
     * Sets the email subject.
     * 
     * @param emailSubject the email subject
     * @return this command for chaining
     */
    public ScheduleCommand setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
        return this;
    }
    
    /**
     * Sets whether to use JSON output.
     * 
     * @param jsonOutput true to use JSON output
     * @return this command for chaining
     */
    public ScheduleCommand setJsonOutput(boolean jsonOutput) {
        this.jsonOutput = jsonOutput;
        return this;
    }
    
    /**
     * Sets whether to use verbose output.
     * 
     * @param verbose true to use verbose output
     * @return this command for chaining
     */
    public ScheduleCommand setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
    
    /**
     * Prints the help for this command.
     */
    public static void printHelp() {
        System.out.println("Usage: rin schedule <action> [options]");
        System.out.println();
        System.out.println("Actions:");
        System.out.println("  list              List all scheduled reports");
        System.out.println("  add               Add a new scheduled report");
        System.out.println("  remove            Remove a scheduled report");
        System.out.println("  start             Start the report scheduler");
        System.out.println("  stop              Stop the report scheduler");
        System.out.println("  show              Show details of a scheduled report");
        System.out.println();
        System.out.println("Options for 'add':");
        System.out.println("  --name=<name>         Report name (required)");
        System.out.println("  --desc=<description>  Report description");
        System.out.println("  --type=<type>         Schedule type: daily, weekly, monthly (required)");
        System.out.println("  --time=<time>         Time of day in HH:MM format (required)");
        System.out.println("  --day=<day>           Day of week for weekly schedules");
        System.out.println("  --date=<date>         Day of month for monthly schedules");
        System.out.println("  --report=<type>       Report type (required)");
        System.out.println("  --format=<format>     Report format (default: text)");
        System.out.println("  --output=<path>       Output file path");
        System.out.println("  --title=<title>       Report title");
        System.out.println("  --email               Enable email delivery");
        System.out.println("  --email-to=<addr>     Set email recipients (comma-separated)");
        System.out.println("  --email-subject=<s>   Set email subject line");
        System.out.println("  --json                Output in JSON format");
        System.out.println("  --verbose             Show detailed information");
        System.out.println();
        System.out.println("Options for 'remove' and 'show':");
        System.out.println("  --id=<id>             Report ID (required)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  rin schedule list");
        System.out.println("  rin schedule add --name=\"Daily Status\" --type=daily --time=09:00 --report=status");
        System.out.println("  rin schedule add --name=\"Weekly Summary\" --type=weekly --day=monday --time=08:00 --report=summary --format=html --email --email-to=team@example.com");
        System.out.println("  rin schedule remove --id=1234");
        System.out.println("  rin schedule show --id=1234");
        System.out.println("  rin schedule start");
        System.out.println("  rin schedule stop");
    }
}