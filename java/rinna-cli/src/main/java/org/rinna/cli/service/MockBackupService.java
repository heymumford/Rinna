/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mock implementation of backup service functionality for CLI use.
 */
public class MockBackupService implements BackupService {
    
    private String backupType = "full";
    private String backupFrequency = "daily";
    private String backupTime = "02:00";
    private int retentionDays = 30;
    private String backupLocation = "/var/backups/rinna";
    
    private String strategy = "incremental";
    private String fullFrequency = "weekly";
    private String incrementalFrequency = "daily";
    
    private boolean encryptionEnabled = true;
    private String encryptionAlgorithm = "AES-256";
    
    private boolean notificationsEnabled = true;
    private List<String> notificationEvents = List.of("all");
    private String notificationRecipients = "admin@example.com";
    
    private final List<String> backupLocations = new ArrayList<>();
    private String locationsStrategy = "synchronized";
    
    private final Map<String, BackupRecord> backups = new HashMap<>();
    
    public MockBackupService() {
        // Add default backup location
        backupLocations.add(backupLocation);
        
        // Add some sample backups
        createSampleBackups();
    }
    
    private void createSampleBackups() {
        BackupRecord backup1 = new BackupRecord(
            "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "full",
            LocalDateTime.now().minusDays(7),
            512 * 1024 * 1024, // 512 MB
            backupLocation,
            "Weekly full backup"
        );
        
        BackupRecord backup2 = new BackupRecord(
            "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "incremental",
            LocalDateTime.now().minusDays(6),
            50 * 1024 * 1024, // 50 MB
            backupLocation,
            "Daily incremental backup"
        );
        
        BackupRecord backup3 = new BackupRecord(
            "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "incremental",
            LocalDateTime.now().minusDays(5),
            75 * 1024 * 1024, // 75 MB
            backupLocation,
            "Daily incremental backup"
        );
        
        BackupRecord backup4 = new BackupRecord(
            "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "incremental",
            LocalDateTime.now().minusDays(4),
            62 * 1024 * 1024, // 62 MB
            backupLocation,
            "Daily incremental backup"
        );
        
        BackupRecord backup5 = new BackupRecord(
            "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "incremental",
            LocalDateTime.now().minusDays(3),
            85 * 1024 * 1024, // 85 MB
            backupLocation,
            "Daily incremental backup"
        );
        
        BackupRecord backup6 = new BackupRecord(
            "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "incremental",
            LocalDateTime.now().minusDays(2),
            48 * 1024 * 1024, // 48 MB
            backupLocation,
            "Daily incremental backup"
        );
        
        BackupRecord backup7 = new BackupRecord(
            "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "incremental",
            LocalDateTime.now().minusDays(1),
            92 * 1024 * 1024, // 92 MB
            backupLocation,
            "Daily incremental backup"
        );
        
        backups.put(backup1.id, backup1);
        backups.put(backup2.id, backup2);
        backups.put(backup3.id, backup3);
        backups.put(backup4.id, backup4);
        backups.put(backup5.id, backup5);
        backups.put(backup6.id, backup6);
        backups.put(backup7.id, backup7);
    }
    
    /**
     * Inner class to represent a backup record.
     */
    private static class BackupRecord {
        private final String id;
        private final String type;
        private final LocalDateTime timestamp;
        private final long size;
        private final String location;
        private final String description;
        
        public BackupRecord(String id, String type, LocalDateTime timestamp, long size, String location, String description) {
            this.id = id;
            this.type = type;
            this.timestamp = timestamp;
            this.size = size;
            this.location = location;
            this.description = description;
        }
    }
    
    @Override
    public boolean configureBackup(String type, String frequency, String time, int retention, String location) {
        this.backupType = type;
        this.backupFrequency = frequency;
        this.backupTime = time;
        this.retentionDays = retention;
        this.backupLocation = location;
        
        // Update the default location in the locations list if it exists
        if (!backupLocations.isEmpty()) {
            backupLocations.set(0, location);
        } else {
            backupLocations.add(location);
        }
        
        return true;
    }
    
    @Override
    public String getBackupStatus() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Backup Status\n");
        sb.append("=============\n\n");
        
        sb.append("Configuration:\n");
        sb.append("- Type: ").append(backupType).append("\n");
        sb.append("- Frequency: ").append(backupFrequency).append("\n");
        sb.append("- Time: ").append(backupTime).append("\n");
        sb.append("- Retention: ").append(retentionDays).append(" days\n");
        sb.append("- Location: ").append(backupLocation).append("\n");
        
        sb.append("\nStrategy:\n");
        sb.append("- Mode: ").append(strategy).append("\n");
        if (!"full-only".equals(strategy)) {
            sb.append("- Full backup frequency: ").append(fullFrequency).append("\n");
            sb.append("- ").append("incremental".equals(strategy) ? "Incremental" : "Differential")
              .append(" backup frequency: ").append(incrementalFrequency).append("\n");
        }
        
        sb.append("\nSecurity:\n");
        sb.append("- Encryption: ").append(encryptionEnabled ? "Enabled" : "Disabled").append("\n");
        if (encryptionEnabled) {
            sb.append("- Algorithm: ").append(encryptionAlgorithm).append("\n");
        }
        
        sb.append("\nLast Backup:\n");
        BackupRecord lastBackup = getLastBackup();
        if (lastBackup != null) {
            sb.append("- ID: ").append(lastBackup.id).append("\n");
            sb.append("- Type: ").append(lastBackup.type).append("\n");
            sb.append("- Time: ").append(lastBackup.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
            sb.append("- Size: ").append(formatSize(lastBackup.size)).append("\n");
        } else {
            sb.append("- No backups found\n");
        }
        
        sb.append("\nNext Scheduled Backup:\n");
        sb.append("- Type: ").append(getNextBackupType()).append("\n");
        sb.append("- Time: ").append(getNextBackupTime()).append("\n");
        
        return sb.toString();
    }
    
    private BackupRecord getLastBackup() {
        if (backups.isEmpty()) {
            return null;
        }
        
        return backups.values().stream()
                .max((a, b) -> a.timestamp.compareTo(b.timestamp))
                .orElse(null);
    }
    
    private String getNextBackupType() {
        if ("full-only".equals(strategy)) {
            return "full";
        }
        
        // Determine if the next backup should be full or incremental/differential
        BackupRecord lastFullBackup = backups.values().stream()
                .filter(b -> "full".equals(b.type))
                .max((a, b) -> a.timestamp.compareTo(b.timestamp))
                .orElse(null);
        
        if (lastFullBackup == null) {
            return "full";
        }
        
        // Check if it's time for a full backup based on fullFrequency
        LocalDateTime now = LocalDateTime.now();
        if ("weekly".equals(fullFrequency) && lastFullBackup.timestamp.plusDays(7).isBefore(now)) {
            return "full";
        } else if ("monthly".equals(fullFrequency) && lastFullBackup.timestamp.plusMonths(1).isBefore(now)) {
            return "full";
        }
        
        return "incremental".equals(strategy) ? "incremental" : "differential";
    }
    
    private String getNextBackupTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun;
        
        String[] timeParts = backupTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        // Start with today at the specified time
        nextRun = now.withHour(hour).withMinute(minute).withSecond(0);
        
        // If that time has already passed today, move to next occurrence based on frequency
        if (nextRun.isBefore(now)) {
            if ("daily".equals(backupFrequency)) {
                nextRun = nextRun.plusDays(1);
            } else if ("weekly".equals(backupFrequency)) {
                nextRun = nextRun.plusDays(7);
            } else if ("monthly".equals(backupFrequency)) {
                nextRun = nextRun.plusMonths(1);
            }
        }
        
        return nextRun.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
    
    @Override
    public String startBackup(String type) {
        String backupId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long size;
        
        if ("full".equals(type)) {
            size = 500 * 1024 * 1024 + (long)(Math.random() * 100 * 1024 * 1024); // ~500-600 MB
        } else if ("incremental".equals(type)) {
            size = 50 * 1024 * 1024 + (long)(Math.random() * 50 * 1024 * 1024); // ~50-100 MB
        } else {
            size = 100 * 1024 * 1024 + (long)(Math.random() * 100 * 1024 * 1024); // ~100-200 MB
        }
        
        BackupRecord backup = new BackupRecord(
            backupId,
            type,
            LocalDateTime.now(),
            size,
            backupLocation,
            "Manual " + type + " backup"
        );
        
        backups.put(backupId, backup);
        
        return backupId;
    }
    
    @Override
    public String listBackups() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Available Backups\n");
        sb.append("================\n\n");
        
        sb.append(String.format("%-12s | %-12s | %-19s | %-10s | %-30s | %s\n",
                "ID", "Type", "Date", "Size", "Location", "Description"));
        sb.append(String.format("%-12s-|-%-12s-|-%-19s-|-%-10s-|-%-30s-|-%s\n",
                "------------", "------------", "-------------------", "----------", "------------------------------", "-------------"));
        
        backups.values().stream()
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp)) // Most recent first
                .forEach(backup -> {
                    sb.append(String.format("%-12s | %-12s | %-19s | %-10s | %-30s | %s\n",
                            backup.id,
                            backup.type,
                            backup.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            formatSize(backup.size),
                            truncate(backup.location, 30),
                            backup.description));
                });
        
        sb.append("\nTotal backups: ").append(backups.size());
        sb.append("\nTotal size: ").append(formatSize(getTotalBackupSize()));
        sb.append("\nRetention period: ").append(retentionDays).append(" days");
        
        return sb.toString();
    }
    
    private long getTotalBackupSize() {
        return backups.values().stream()
                .mapToLong(b -> b.size)
                .sum();
    }
    
    @Override
    public boolean configureBackupStrategy(String strategy, String fullFrequency, String incrementalFrequency) {
        this.strategy = strategy;
        this.fullFrequency = fullFrequency;
        this.incrementalFrequency = incrementalFrequency;
        return true;
    }
    
    @Override
    public String getBackupStrategyStatus() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Backup Strategy Configuration\n");
        sb.append("============================\n\n");
        
        sb.append("Current strategy: ").append(strategy).append("\n\n");
        
        if ("full-only".equals(strategy)) {
            sb.append("Only full backups are performed according to the configured schedule.\n");
            sb.append("- Frequency: ").append(backupFrequency).append("\n");
            sb.append("- Time: ").append(backupTime).append("\n");
        } else if ("incremental".equals(strategy)) {
            sb.append("Incremental backup strategy:\n");
            sb.append("- Full backups performed: ").append(fullFrequency).append("\n");
            sb.append("- Incremental backups performed: ").append(incrementalFrequency).append("\n");
            sb.append("\nWith this strategy, each incremental backup contains only the changes\n");
            sb.append("since the last backup (full or incremental). This minimizes backup size\n");
            sb.append("but requires all incremental backups since the last full backup for a\n");
            sb.append("complete restore.\n");
        } else if ("differential".equals(strategy)) {
            sb.append("Differential backup strategy:\n");
            sb.append("- Full backups performed: ").append(fullFrequency).append("\n");
            sb.append("- Differential backups performed: ").append(incrementalFrequency).append("\n");
            sb.append("\nWith this strategy, each differential backup contains all changes\n");
            sb.append("since the last full backup. This increases backup size compared to\n");
            sb.append("incremental backups but simplifies restore operations, requiring only\n");
            sb.append("the last full backup and the most recent differential backup.\n");
        }
        
        return sb.toString();
    }
    
    @Override
    public String getBackupHistory() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Backup History\n");
        sb.append("==============\n\n");
        
        sb.append("Recent Backup Operations:\n");
        sb.append(String.format("%-19s | %-12s | %-10s | %-8s | %s\n",
                "Date", "Type", "Size", "Status", "Notes"));
        sb.append(String.format("%-19s-|-%-12s-|-%-10s-|-%-8s-|-%s\n",
                "-------------------", "------------", "----------", "--------", "-----"));
        
        // Create a fake history with success/failure patterns
        LocalDateTime now = LocalDateTime.now();
        
        sb.append(String.format("%-19s | %-12s | %-10s | %-8s | %s\n",
                now.minusDays(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "incremental", formatSize(92 * 1024 * 1024), "SUCCESS", "Completed in 2 minutes, 15 seconds"));
            
        sb.append(String.format("%-19s | %-12s | %-10s | %-8s | %s\n",
                now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "incremental", formatSize(48 * 1024 * 1024), "SUCCESS", "Completed in 1 minute, 42 seconds"));
            
        sb.append(String.format("%-19s | %-12s | %-10s | %-8s | %s\n",
                now.minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "incremental", formatSize(85 * 1024 * 1024), "SUCCESS", "Completed in 2 minutes, 3 seconds"));
            
        sb.append(String.format("%-19s | %-12s | %-10s | %-8s | %s\n",
                now.minusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "incremental", formatSize(62 * 1024 * 1024), "SUCCESS", "Completed in 1 minute, 55 seconds"));
            
        sb.append(String.format("%-19s | %-12s | %-10s | %-8s | %s\n",
                now.minusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "incremental", "-", "FAILED", "Insufficient disk space"));
            
        sb.append(String.format("%-19s | %-12s | %-10s | %-8s | %s\n",
                now.minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "incremental", formatSize(75 * 1024 * 1024), "SUCCESS", "Completed in 1 minute, 58 seconds"));
            
        sb.append(String.format("%-19s | %-12s | %-10s | %-8s | %s\n",
                now.minusDays(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "incremental", formatSize(50 * 1024 * 1024), "SUCCESS", "Completed in 1 minute, 36 seconds"));
            
        sb.append(String.format("%-19s | %-12s | %-10s | %-8s | %s\n",
                now.minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "full", formatSize(512 * 1024 * 1024), "SUCCESS", "Completed in 10 minutes, 12 seconds"));
        
        sb.append("\nBackup Statistics:\n");
        sb.append("- Success rate: 87.5% (7 of 8 operations)\n");
        sb.append("- Average backup size: ").append(formatSize(132 * 1024 * 1024)).append("\n");
        sb.append("- Average duration: 2 minutes, 31 seconds\n");
        sb.append("- Total backups performed: 24 (last 30 days)\n");
        sb.append("- Total data backed up: ").append(formatSize((long)(3.2 * 1024 * 1024 * 1024))).append(" (last 30 days)\n");
        
        return sb.toString();
    }
    
    @Override
    public boolean configureBackupSecurity(boolean enableEncryption, String algorithm, String passphrase) {
        this.encryptionEnabled = enableEncryption;
        if (enableEncryption && algorithm != null) {
            this.encryptionAlgorithm = algorithm;
        }
        return true;
    }
    
    @Override
    public String getBackupSecurityStatus() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Backup Security Configuration\n");
        sb.append("============================\n\n");
        
        sb.append("Encryption: ").append(encryptionEnabled ? "Enabled" : "Disabled").append("\n");
        
        if (encryptionEnabled) {
            sb.append("Algorithm: ").append(encryptionAlgorithm).append("\n");
            sb.append("Key strength: ").append(getKeyStrength(encryptionAlgorithm)).append("\n");
            sb.append("Passphrase set: Yes\n");
            sb.append("\nBackup access restrictions:\n");
            sb.append("- Authentication required: Yes\n");
            sb.append("- Role-based access control: Enabled\n");
            sb.append("- Access logs: Enabled\n");
        } else {
            sb.append("\nWARNING: Encryption is disabled. Backups are stored in plaintext.\n");
            sb.append("This is not recommended for production environments or sensitive data.\n");
        }
        
        return sb.toString();
    }
    
    private String getKeyStrength(String algorithm) {
        if ("AES-256".equals(algorithm)) {
            return "256-bit";
        } else if ("Twofish".equals(algorithm)) {
            return "256-bit";
        } else if ("ChaCha20".equals(algorithm)) {
            return "256-bit";
        }
        return "Unknown";
    }
    
    @Override
    public String verifyBackup(String backupId) {
        StringBuilder sb = new StringBuilder();
        
        BackupRecord backup = null;
        
        if ("latest".equals(backupId)) {
            backup = getLastBackup();
        } else {
            backup = backups.get(backupId);
        }
        
        if (backup == null) {
            return "Error: Backup not found: " + backupId;
        }
        
        sb.append("Backup Verification Report\n");
        sb.append("=========================\n\n");
        
        sb.append("Backup ID: ").append(backup.id).append("\n");
        sb.append("Type: ").append(backup.type).append("\n");
        sb.append("Created: ").append(backup.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("Size: ").append(formatSize(backup.size)).append("\n");
        sb.append("Location: ").append(backup.location).append("\n\n");
        
        sb.append("Verification Results:\n");
        sb.append("- Integrity check: PASSED\n");
        sb.append("- Encryption check: ").append(encryptionEnabled ? "PASSED" : "N/A").append("\n");
        sb.append("- Completeness check: PASSED\n");
        sb.append("- Restore simulation: PASSED\n");
        
        sb.append("\nFile statistics:\n");
        sb.append("- Total files: 42,351\n");
        sb.append("- Verified files: 42,351\n");
        sb.append("- Corrupted files: 0\n");
        sb.append("- Missing files: 0\n");
        
        sb.append("\nVerification successfully completed. This backup appears to be valid and can be used for restore operations.");
        
        return sb.toString();
    }
    
    @Override
    public boolean configureBackupNotifications(boolean enableNotifications, List<String> events, String recipients) {
        this.notificationsEnabled = enableNotifications;
        if (enableNotifications) {
            if (events != null && !events.isEmpty()) {
                this.notificationEvents = new ArrayList<>(events);
            }
            if (recipients != null && !recipients.isEmpty()) {
                this.notificationRecipients = recipients;
            }
        }
        return true;
    }
    
    @Override
    public String getBackupNotificationsStatus() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Backup Notification Configuration\n");
        sb.append("================================\n\n");
        
        sb.append("Notifications: ").append(notificationsEnabled ? "Enabled" : "Disabled").append("\n");
        
        if (notificationsEnabled) {
            sb.append("\nNotification Events:\n");
            for (String event : notificationEvents) {
                sb.append("- ").append(event).append("\n");
            }
            
            sb.append("\nRecipients:\n");
            String[] recipients = notificationRecipients.split(",");
            for (String recipient : recipients) {
                sb.append("- ").append(recipient.trim()).append("\n");
            }
            
            sb.append("\nDelivery Method: Email\n");
            sb.append("Notification Schedule: Immediate\n");
            sb.append("Include Details: Yes\n");
        }
        
        return sb.toString();
    }
    
    @Override
    public boolean configureBackupLocations(List<String> locations, String strategy) {
        if (locations != null && !locations.isEmpty()) {
            this.backupLocations.clear();
            this.backupLocations.addAll(locations);
            this.backupLocation = locations.get(0); // Set primary location
        }
        
        if (strategy != null && !strategy.isEmpty()) {
            this.locationsStrategy = strategy;
        }
        
        return true;
    }
    
    @Override
    public String listBackupLocations() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Backup Storage Locations\n");
        sb.append("=======================\n\n");
        
        if (backupLocations.isEmpty()) {
            sb.append("No backup locations configured.\n");
        } else {
            sb.append("Mirroring Strategy: ").append(locationsStrategy).append("\n\n");
            
            sb.append(String.format("%-5s | %-50s | %-12s | %s\n",
                    "Index", "Location", "Status", "Space Available"));
            sb.append(String.format("%-5s-|-%-50s-|-%-12s-|-%s\n",
                    "-----", "--------------------------------------------------", "------------", "---------------"));
            
            for (int i = 0; i < backupLocations.size(); i++) {
                String location = backupLocations.get(i);
                boolean isPrimary = i == 0;
                
                sb.append(String.format("%-5d | %-50s | %-12s | %s\n",
                        i + 1,
                        truncate(location, 50),
                        isPrimary ? "PRIMARY" : "SECONDARY",
                        generateRandomSpaceAvailable()));
            }
        }
        
        return sb.toString();
    }
    
    private String generateRandomSpaceAvailable() {
        // Generate a random space between 500 GB and 2 TB
        double spaceGB = 500 + Math.random() * 1500;
        if (spaceGB >= 1000) {
            return String.format("%.2f TB", spaceGB / 1000);
        } else {
            return String.format("%.2f GB", spaceGB);
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}