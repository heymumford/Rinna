package org.rinna.usecase;

import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * Service for backup and recovery operations.
 */
public interface RecoveryService {
    /**
     * Start a backup operation.
     *
     * @param backupType the type of backup to perform
     * @param targetLocation the target location for the backup
     * @return the ID of the backup job
     */
    String startBackup(BackupType backupType, String targetLocation);
    
    /**
     * Get the status of a backup job.
     *
     * @param backupId the ID of the backup job
     * @return the status of the backup job
     */
    BackupStatus getBackupStatus(String backupId);
    
    /**
     * List available backups.
     *
     * @return list of available backups
     */
    List<Backup> listBackups();
    
    /**
     * Get a backup by ID.
     *
     * @param backupId the ID of the backup
     * @return the backup
     */
    Backup getBackup(String backupId);
    
    /**
     * Create a recovery plan.
     *
     * @param backupId the ID of the backup to recover from
     * @param recoveryOptions the recovery options
     * @return the recovery plan
     */
    RecoveryPlan createRecoveryPlan(String backupId, Map<String, Object> recoveryOptions);
    
    /**
     * Start a recovery operation.
     *
     * @param recoveryPlanId the ID of the recovery plan
     * @return the ID of the recovery job
     */
    String startRecovery(String recoveryPlanId);
    
    /**
     * Get the status of a recovery job.
     *
     * @param recoveryId the ID of the recovery job
     * @return the status of the recovery job
     */
    RecoveryStatus getRecoveryStatus(String recoveryId);
    
    /**
     * Configure backup settings.
     *
     * @param backupLocation the location for backups
     * @param retentionPeriod the retention period in days
     * @param scheduleExpression the schedule expression (e.g., cron expression)
     */
    void configureBackupSettings(String backupLocation, int retentionPeriod, String scheduleExpression);
    
    /**
     * Backup information.
     */
    interface Backup {
        /**
         * Get the backup ID.
         *
         * @return the backup ID
         */
        String getId();
        
        /**
         * Get the backup type.
         *
         * @return the backup type
         */
        BackupType getType();
        
        /**
         * Get the backup location.
         *
         * @return the backup location
         */
        String getLocation();
        
        /**
         * Get the backup creation time.
         *
         * @return the creation time
         */
        Date getCreationTime();
        
        /**
         * Get the backup size in bytes.
         *
         * @return the backup size
         */
        long getSize();
        
        /**
         * Get the backup status.
         *
         * @return the backup status
         */
        BackupStatus getStatus();
    }
    
    /**
     * Recovery plan.
     */
    interface RecoveryPlan {
        /**
         * Get the recovery plan ID.
         *
         * @return the recovery plan ID
         */
        String getId();
        
        /**
         * Get the ID of the backup to recover from.
         *
         * @return the backup ID
         */
        String getBackupId();
        
        /**
         * Get the recovery options.
         *
         * @return the recovery options
         */
        Map<String, Object> getOptions();
        
        /**
         * Get the recovery steps.
         *
         * @return list of recovery steps
         */
        List<String> getSteps();
        
        /**
         * Get the creation time of the recovery plan.
         *
         * @return the creation time
         */
        Date getCreationTime();
    }
    
    /**
     * Backup type.
     */
    enum BackupType {
        FULL,
        INCREMENTAL,
        DIFFERENTIAL
    }
    
    /**
     * Backup status.
     */
    enum BackupStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
    
    /**
     * Recovery status.
     */
    enum RecoveryStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}