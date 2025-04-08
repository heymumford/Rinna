/*
 * Email service for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ServiceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for sending reports via email.
 */
public final class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static EmailService instance;
    
    private String smtpServer;
    private int smtpPort;
    private String senderEmail;
    private String senderName;
    private boolean authEnabled;
    private String username;
    private String password;
    
    /**
     * Private constructor for singleton pattern.
     */
    private EmailService() {
        loadConfiguration();
    }
    
    /**
     * Gets the singleton instance of EmailService.
     * 
     * @return the instance
     */
    public static synchronized EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }
    
    /**
     * Loads email configuration from the config service.
     */
    private void loadConfiguration() {
        ConfigurationService configService = ServiceManager.getInstance().getConfigurationService();
        
        // Load email settings from configuration
        this.smtpServer = configService.getProperty("email.smtp.server", "localhost");
        this.smtpPort = Integer.parseInt(configService.getProperty("email.smtp.port", "25"));
        this.senderEmail = configService.getProperty("email.sender.address", "rinna@example.com");
        this.senderName = configService.getProperty("email.sender.name", "Rinna System");
        this.authEnabled = Boolean.parseBoolean(configService.getProperty("email.auth.enabled", "false"));
        this.username = configService.getProperty("email.auth.username", "");
        this.password = configService.getProperty("email.auth.password", "");
    }
    
    /**
     * Sends a report to the specified recipients.
     * Depending on configuration, emails are either queued for SMTP delivery
     * or saved to the outbox directory for manual processing.
     * 
     * @param config the report configuration
     * @param reportContent the report content
     * @return true if the report was sent successfully
     */
    public boolean sendReport(ReportConfig config, String reportContent) {
        if (!config.isEmailEnabled() || config.getEmailRecipients().isEmpty()) {
            LOGGER.info("Email not enabled or no recipients specified");
            return false;
        }
        
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String subject = config.getEmailSubject() != null ? config.getEmailSubject() : config.getTitle();
            
            // Format email content
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("From: ").append(senderName).append(" <").append(senderEmail).append(">\n");
            emailContent.append("To: ").append(String.join(", ", config.getEmailRecipients())).append("\n");
            emailContent.append("Subject: ").append(subject).append("\n");
            emailContent.append("Content-Type: text/").append(getContentType(config.getFormat())).append("; charset=UTF-8\n");
            emailContent.append("\n");
            emailContent.append(reportContent);
            
            // Create outbox directory if it doesn't exist
            String outboxDir = System.getProperty("user.home") + File.separator + ".rinna" + File.separator + "email_outbox";
            Files.createDirectories(Paths.get(outboxDir));
            
            // Write email to file (serves as both a backup and for local processing mode)
            String emailFile = outboxDir + File.separator + "report_" + timestamp + "." + config.getFormat().getFileExtension();
            Files.write(
                Paths.get(emailFile), 
                emailContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // If SMTP is configured, queue for delivery
            if (isSMTPConfigured()) {
                queueForDelivery(config.getEmailRecipients(), subject, emailContent.toString(), config.getFormat());
                LOGGER.info("Report email queued for delivery to " + config.getEmailRecipients().size() + " recipients");
            } else {
                LOGGER.info("Report email saved to " + emailFile + " (SMTP not configured)");
            }
            
            // Log recipients for audit trail
            for (String recipient : config.getEmailRecipients()) {
                LOGGER.info("Email destination: " + recipient);
            }
            
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending report email", e);
            return false;
        }
    }
    
    /**
     * Determines if SMTP delivery is configured.
     * 
     * @return true if SMTP is configured for delivery
     */
    private boolean isSMTPConfigured() {
        return smtpServer != null && !smtpServer.equals("localhost") && !smtpServer.isEmpty();
    }
    
    /**
     * Queues an email for SMTP delivery.
     * 
     * @param recipients the email recipients
     * @param subject the email subject
     * @param content the email content
     * @param format the report format
     */
    private void queueForDelivery(List<String> recipients, String subject, String content, ReportFormat format) {
        // Queue the email for asynchronous delivery
        // In a production environment, this would connect to an SMTP server
        // or use a message queue to handle email delivery
        
        // Simulate queuing by writing to a delivery queue file
        try {
            String queueDir = System.getProperty("user.home") + File.separator + ".rinna" + File.separator + "email_queue";
            Files.createDirectories(Paths.get(queueDir));
            
            // Create delivery metadata
            Properties deliveryProps = new Properties();
            deliveryProps.setProperty("timestamp", LocalDateTime.now().toString());
            deliveryProps.setProperty("subject", subject);
            deliveryProps.setProperty("smtp.server", smtpServer);
            deliveryProps.setProperty("smtp.port", String.valueOf(smtpPort));
            deliveryProps.setProperty("sender.email", senderEmail);
            deliveryProps.setProperty("sender.name", senderName);
            deliveryProps.setProperty("format", format.name());
            
            // Add recipients
            for (int i = 0; i < recipients.size(); i++) {
                deliveryProps.setProperty("recipient." + i, recipients.get(i));
            }
            
            // Write metadata
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String metadataFile = queueDir + File.separator + "queue_" + timestamp + ".metadata";
            try (FileOutputStream out = new FileOutputStream(metadataFile)) {
                deliveryProps.store(out, "Email delivery queue metadata");
            }
            
            // Write content separately
            String contentFile = queueDir + File.separator + "queue_" + timestamp + ".content";
            Files.write(Paths.get(contentFile), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error queuing email for delivery", e);
        }
    }
    
    /**
     * Gets the content type for a report format.
     * 
     * @param format the report format
     * @return the content type
     */
    private String getContentType(ReportFormat format) {
        switch (format) {
            case HTML:
                return "html";
            case XML:
                return "xml";
            case JSON:
                return "json";
            case MARKDOWN:
                return "markdown";
            case CSV:
                return "csv";
            case TEXT:
            default:
                return "plain";
        }
    }
    
    /**
     * Gets the SMTP server.
     * 
     * @return the SMTP server
     */
    public String getSmtpServer() {
        return smtpServer;
    }
    
    /**
     * Gets the SMTP port.
     * 
     * @return the SMTP port
     */
    public int getSmtpPort() {
        return smtpPort;
    }
    
    /**
     * Gets the sender email.
     * 
     * @return the sender email
     */
    public String getSenderEmail() {
        return senderEmail;
    }
    
    /**
     * Gets the sender name.
     * 
     * @return the sender name
     */
    public String getSenderName() {
        return senderName;
    }
    
    /**
     * Checks if authentication is enabled.
     * 
     * @return true if authentication is enabled
     */
    public boolean isAuthEnabled() {
        return authEnabled;
    }
    
    /**
     * Gets the username.
     * 
     * @return the username
     */
    public String getUsername() {
        return username;
    }
}