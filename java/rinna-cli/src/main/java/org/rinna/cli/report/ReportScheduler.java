/*
 * Report scheduler for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Service for scheduling automatic report generation.
 */
public final class ReportScheduler {
    private static final Logger LOGGER = Logger.getLogger(ReportScheduler.class.getName());
    // Use project-relative path for schedules in the target directory
    private static final String CONFIG_DIR = System.getProperty("report.scheduler.config.dir", 
            System.getProperty("user.dir") + File.separator + "target" + File.separator + "schedules");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static ReportScheduler instance;
    
    private final ScheduledExecutorService scheduler;
    private final List<ScheduledReport> scheduledReports;
    private boolean isRunning;
    
    /**
     * Private constructor for singleton pattern.
     */
    private ReportScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduledReports = new ArrayList<>();
        isRunning = false;
        
        // Create config directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating schedules directory", e);
        }
    }
    
    /**
     * Package-private constructor for testing purposes.
     * 
     * @param forTesting indicates this is for testing only
     */
    ReportScheduler(boolean forTesting) {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduledReports = new ArrayList<>();
        isRunning = false;
        
        // Create config directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating schedules directory", e);
        }
        
        // Load existing schedules
        if (!forTesting) {
            loadSchedules();
        }
    }
    
    /**
     * Gets the singleton instance of ReportScheduler.
     * 
     * @return the instance
     */
    public static synchronized ReportScheduler getInstance() {
        if (instance == null) {
            instance = new ReportScheduler();
        }
        return instance;
    }
    
    /**
     * Starts the scheduler.
     */
    public void start() {
        if (!isRunning) {
            loadSchedules();
            scheduleReports();
            isRunning = true;
            LOGGER.info("Report scheduler started with " + scheduledReports.size() + " scheduled reports");
        }
    }
    
    /**
     * Stops the scheduler.
     */
    public void stop() {
        if (isRunning) {
            scheduler.shutdown();
            isRunning = false;
            LOGGER.info("Report scheduler stopped");
        }
    }
    
    /**
     * Adds a scheduled report.
     * 
     * @param report the scheduled report to add
     * @return true if the report was added successfully
     */
    public boolean addScheduledReport(ScheduledReport report) {
        // Check if a report with the same ID already exists
        for (ScheduledReport existingReport : scheduledReports) {
            if (existingReport.getId().equals(report.getId())) {
                return false;
            }
        }
        
        // Add the report
        scheduledReports.add(report);
        
        // Save the report
        saveSchedule(report);
        
        // Schedule the report if the scheduler is running
        if (isRunning) {
            scheduleReport(report);
        }
        
        return true;
    }
    
    /**
     * Removes a scheduled report.
     * 
     * @param id the ID of the report to remove
     * @return true if the report was removed successfully
     */
    public boolean removeScheduledReport(String id) {
        // Find the report
        ScheduledReport reportToRemove = null;
        for (ScheduledReport report : scheduledReports) {
            if (report.getId().equals(id)) {
                reportToRemove = report;
                break;
            }
        }
        
        // Remove the report
        if (reportToRemove != null) {
            scheduledReports.remove(reportToRemove);
            
            // Delete the report file
            Path reportFile = Paths.get(CONFIG_DIR, id + ".properties");
            try {
                Files.deleteIfExists(reportFile);
                return true;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error deleting schedule file", e);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Gets all scheduled reports.
     * 
     * @return the list of scheduled reports
     */
    public List<ScheduledReport> getScheduledReports() {
        return new ArrayList<>(scheduledReports);
    }
    
    /**
     * Gets a scheduled report by ID.
     * 
     * @param id the ID of the report to get
     * @return the scheduled report, or null if not found
     */
    public ScheduledReport getScheduledReport(String id) {
        for (ScheduledReport report : scheduledReports) {
            if (report.getId().equals(id)) {
                return report;
            }
        }
        return null;
    }
    
    /**
     * Loads the schedules from disk.
     */
    private void loadSchedules() {
        File scheduleDir = new File(CONFIG_DIR);
        File[] scheduleFiles = scheduleDir.listFiles((dir, name) -> name.endsWith(".properties"));
        
        if (scheduleFiles != null) {
            for (File scheduleFile : scheduleFiles) {
                try {
                    Properties props = new Properties();
                    try (InputStreamReader reader = new InputStreamReader(
                            Files.newInputStream(scheduleFile.toPath()), StandardCharsets.UTF_8)) {
                        props.load(reader);
                    }
                    
                    // Parse the schedule
                    ScheduledReport report = new ScheduledReport();
                    report.setId(scheduleFile.getName().replace(".properties", ""));
                    report.setName(props.getProperty("name"));
                    report.setDescription(props.getProperty("description"));
                    report.setScheduleType(ScheduleType.valueOf(props.getProperty("scheduleType")));
                    report.setTime(props.getProperty("time"));
                    report.setDayOfWeek(props.getProperty("dayOfWeek") != null ? 
                            DayOfWeek.valueOf(props.getProperty("dayOfWeek")) : null);
                    report.setDayOfMonth(props.getProperty("dayOfMonth") != null ? 
                            Integer.parseInt(props.getProperty("dayOfMonth")) : 0);
                    
                    // Parse the report config
                    ReportConfig config = new ReportConfig();
                    config.setType(ReportType.valueOf(props.getProperty("reportType")));
                    config.setFormat(ReportFormat.valueOf(props.getProperty("reportFormat")));
                    config.setOutputPath(props.getProperty("outputPath"));
                    config.setTitle(props.getProperty("title"));
                    
                    // Parse email settings
                    boolean emailEnabled = Boolean.parseBoolean(props.getProperty("emailEnabled", "false"));
                    config.setEmailEnabled(emailEnabled);
                    
                    if (emailEnabled) {
                        String recipients = props.getProperty("emailRecipients");
                        if (recipients != null && !recipients.isEmpty()) {
                            String[] recipientArray = recipients.split(",");
                            for (String recipient : recipientArray) {
                                config.addEmailRecipient(recipient.trim());
                            }
                        }
                        
                        config.setEmailSubject(props.getProperty("emailSubject"));
                    }
                    
                    report.setConfig(config);
                    
                    // Add the report
                    scheduledReports.add(report);
                    
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading schedule file: " + scheduleFile.getName(), e);
                }
            }
        }
    }
    
    /**
     * Saves a schedule to disk.
     * 
     * @param report the report to save
     */
    private void saveSchedule(ScheduledReport report) {
        try {
            Properties props = new Properties();
            
            // Save basic properties
            props.setProperty("name", report.getName());
            if (report.getDescription() != null) {
                props.setProperty("description", report.getDescription());
            }
            props.setProperty("scheduleType", report.getScheduleType().name());
            props.setProperty("time", report.getTime());
            
            if (report.getDayOfWeek() != null) {
                props.setProperty("dayOfWeek", report.getDayOfWeek().name());
            }
            
            if (report.getDayOfMonth() > 0) {
                props.setProperty("dayOfMonth", String.valueOf(report.getDayOfMonth()));
            }
            
            // Save report config
            ReportConfig config = report.getConfig();
            props.setProperty("reportType", config.getType().name());
            props.setProperty("reportFormat", config.getFormat().name());
            
            if (config.getOutputPath() != null) {
                props.setProperty("outputPath", config.getOutputPath());
            }
            
            if (config.getTitle() != null) {
                props.setProperty("title", config.getTitle());
            }
            
            // Save email settings
            props.setProperty("emailEnabled", String.valueOf(config.isEmailEnabled()));
            
            if (config.isEmailEnabled()) {
                List<String> recipients = config.getEmailRecipients();
                if (!recipients.isEmpty()) {
                    props.setProperty("emailRecipients", String.join(",", recipients));
                }
                
                if (config.getEmailSubject() != null) {
                    props.setProperty("emailSubject", config.getEmailSubject());
                }
            }
            
            // Write to file
            Path scheduleFile = Paths.get(CONFIG_DIR, report.getId() + ".properties");
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    Files.newOutputStream(scheduleFile), StandardCharsets.UTF_8)) {
                props.store(writer, "Scheduled report configuration");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving schedule", e);
        }
    }
    
    /**
     * Schedules all reports.
     */
    private void scheduleReports() {
        for (ScheduledReport report : scheduledReports) {
            scheduleReport(report);
        }
    }
    
    /**
     * Schedules a single report.
     * 
     * @param report the report to schedule
     */
    private void scheduleReport(ScheduledReport report) {
        LOGGER.info("Scheduling report: " + report.getName());
        
        // Calculate initial delay
        long initialDelay = calculateInitialDelay(report);
        
        // Calculate period
        long period = calculatePeriod(report);
        
        // Schedule the task
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LOGGER.info("Executing scheduled report: " + report.getName());
                ReportService reportService = ReportService.getInstance();
                reportService.generateReport(report.getConfig());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error executing scheduled report: " + report.getName(), e);
            }
        }, initialDelay, period, TimeUnit.MINUTES);
    }
    
    /**
     * Calculates the initial delay for a scheduled report.
     * 
     * @param report the report
     * @return the initial delay in minutes
     */
    private long calculateInitialDelay(ScheduledReport report) {
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
                    while (nextExecution.getDayOfWeek() != report.getDayOfWeek()) {
                        nextExecution = nextExecution.plusDays(1);
                    }
                    break;
                case MONTHLY:
                    // Add days until we reach the desired day of month
                    nextExecution = nextExecution.withDayOfMonth(report.getDayOfMonth());
                    if (nextExecution.isBefore(now)) {
                        nextExecution = nextExecution.plusMonths(1);
                    }
                    break;
            }
        }
        
        // Calculate the delay in minutes
        return java.time.Duration.between(now, nextExecution).toMinutes();
    }
    
    /**
     * Calculates the period for a scheduled report.
     * 
     * @param report the report
     * @return the period in minutes
     */
    private long calculatePeriod(ScheduledReport report) {
        switch (report.getScheduleType()) {
            case DAILY:
                return TimeUnit.DAYS.toMinutes(1);
            case WEEKLY:
                return TimeUnit.DAYS.toMinutes(7);
            case MONTHLY:
                // For monthly schedules, we'll reschedule after each execution
                return TimeUnit.DAYS.toMinutes(28); // Approximate
            default:
                return TimeUnit.DAYS.toMinutes(1);
        }
    }
    
    /**
     * Clears all scheduled reports.
     */
    public void clearScheduledReports() {
        // Get a copy of the IDs to avoid concurrent modification
        List<String> reportIds = new ArrayList<>();
        for (ScheduledReport report : scheduledReports) {
            reportIds.add(report.getId());
        }
        
        // Remove each report
        for (String id : reportIds) {
            removeScheduledReport(id);
        }
    }
    
    /**
     * Checks if a report should run now.
     * 
     * @param report the report to check
     * @return true if the report should run now
     */
    public boolean shouldRunNow(ScheduledReport report) {
        // Get current time
        LocalDateTime now = LocalDateTime.now();
        
        // Parse report time
        String[] timeParts = report.getTime().split(":");
        int reportHour = Integer.parseInt(timeParts[0]);
        int reportMinute = Integer.parseInt(timeParts[1]);
        
        // Check if the time matches
        if (now.getHour() != reportHour || now.getMinute() != reportMinute) {
            return false;
        }
        
        // Check schedule type
        switch (report.getScheduleType()) {
            case DAILY:
                // Daily reports should run every day at the specified time
                return true;
                
            case WEEKLY:
                // Weekly reports should run on the specified day of the week
                return now.getDayOfWeek() == report.getDayOfWeek();
                
            case MONTHLY:
                // Monthly reports should run on the specified day of the month
                return now.getDayOfMonth() == report.getDayOfMonth();
                
            default:
                return false;
        }
    }
    
    /**
     * Checks if the scheduler is running.
     * 
     * @return true if the scheduler is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Scheduled report.
     */
    public static class ScheduledReport {
        private String id;
        private String name;
        private String description;
        private ScheduleType scheduleType;
        private String time; // HH:mm format
        private DayOfWeek dayOfWeek; // For weekly schedules
        private int dayOfMonth; // For monthly schedules
        private ReportConfig config;
        
        /**
         * Constructs a new scheduled report.
         */
        public ScheduledReport() {
            this.id = java.util.UUID.randomUUID().toString();
            this.scheduleType = ScheduleType.DAILY;
            this.time = "08:00";
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public ScheduleType getScheduleType() {
            return scheduleType;
        }
        
        public void setScheduleType(ScheduleType scheduleType) {
            this.scheduleType = scheduleType;
        }
        
        public String getTime() {
            return time;
        }
        
        public void setTime(String time) {
            this.time = time;
        }
        
        public DayOfWeek getDayOfWeek() {
            return dayOfWeek;
        }
        
        public void setDayOfWeek(DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }
        
        public int getDayOfMonth() {
            return dayOfMonth;
        }
        
        public void setDayOfMonth(int dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
        }
        
        public ReportConfig getConfig() {
            return config;
        }
        
        public void setConfig(ReportConfig config) {
            this.config = config;
        }
    }
    
    /**
     * Schedule type.
     */
    public enum ScheduleType {
        DAILY,
        WEEKLY,
        MONTHLY
    }
}