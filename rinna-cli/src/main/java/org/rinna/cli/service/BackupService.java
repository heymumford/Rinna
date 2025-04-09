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

import java.util.List;

/**
 * Interface for backup services.
 */
public interface BackupService {
    
    /**
     * Configures backup settings.
     *
     * @param type The type of backup (full, incremental, differential)
     * @param frequency The backup frequency (daily, weekly, monthly)
     * @param time The backup time (HH:MM in 24-hour format)
     * @param retention The retention period in days
     * @param location The backup location (directory path)
     * @return True if successful
     */
    boolean configureBackup(String type, String frequency, String time, int retention, String location);
    
    /**
     * Gets the current backup configuration status.
     *
     * @return Formatted status information
     */
    String getBackupStatus();
    
    /**
     * Starts a backup operation.
     *
     * @param type The type of backup to start (full, incremental, differential)
     * @return The backup ID
     */
    String startBackup(String type);
    
    /**
     * Lists available backups.
     *
     * @return Formatted listing of backups
     */
    String listBackups();
    
    /**
     * Configures the backup strategy.
     *
     * @param strategy The backup strategy (full-only, incremental, differential)
     * @param fullFrequency The frequency for full backups (weekly, monthly)
     * @param incrementalFrequency The frequency for incremental/differential backups (daily, bidaily)
     * @return True if successful
     */
    boolean configureBackupStrategy(String strategy, String fullFrequency, String incrementalFrequency);
    
    /**
     * Gets the current backup strategy status.
     *
     * @return Formatted strategy status
     */
    String getBackupStrategyStatus();
    
    /**
     * Gets the backup history.
     *
     * @return Formatted backup history
     */
    String getBackupHistory();
    
    /**
     * Configures backup security settings.
     *
     * @param enableEncryption Whether to enable encryption
     * @param algorithm The encryption algorithm
     * @param passphrase The encryption passphrase
     * @return True if successful
     */
    boolean configureBackupSecurity(boolean enableEncryption, String algorithm, String passphrase);
    
    /**
     * Gets the backup security status.
     *
     * @return Formatted security status
     */
    String getBackupSecurityStatus();
    
    /**
     * Verifies the integrity of a backup.
     *
     * @param backupId The ID of the backup to verify (or 'latest')
     * @return Verification report
     */
    String verifyBackup(String backupId);
    
    /**
     * Configures backup notification settings.
     *
     * @param enableNotifications Whether to enable notifications
     * @param events The events to notify on
     * @param recipients The notification recipients (comma-separated email addresses)
     * @return True if successful
     */
    boolean configureBackupNotifications(boolean enableNotifications, List<String> events, String recipients);
    
    /**
     * Gets the backup notification status.
     *
     * @return Formatted notification status
     */
    String getBackupNotificationsStatus();
    
    /**
     * Configures backup storage locations.
     *
     * @param locations The list of storage locations
     * @param strategy The mirroring strategy
     * @return True if successful
     */
    boolean configureBackupLocations(List<String> locations, String strategy);
    
    /**
     * Lists the configured backup locations.
     *
     * @return Formatted list of backup locations
     */
    String listBackupLocations();
}