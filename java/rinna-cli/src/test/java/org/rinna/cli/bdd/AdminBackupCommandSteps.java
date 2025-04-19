package org.rinna.cli.bdd;

import static org.mockito.Mockito.*;

import java.util.UUID;

import org.rinna.cli.service.MockBackupService;
import org.rinna.cli.service.MockSecurityService;


/**
 * Step definitions for admin backup command tests.
 */
public class AdminBackupCommandSteps {
    
    private final TestContext context;
    private final CommandProcessor commandProcessor;
    private final MockSecurityService mockSecurityService;
    private final MockBackupService mockBackupService;
    
    /**
     * Constructor with test context.
     * 
     * @param context the test context
     */
    public AdminBackupCommandSteps(TestContext context) {
        this.context = context;
        this.commandProcessor = context.getCommandProcessor();
        this.mockSecurityService = context.getMockSecurityService();
        this.mockBackupService = context.getMockBackupService();
        
        // Set up default behavior for mock services
        setupDefaultMockBehavior();
    }
    
    /**
     * Sets up default behavior for mock services.
     */
    private void setupDefaultMockBehavior() {
        // Configure backup settings
        when(mockBackupService.configureBackup(
                anyString(), anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(true);
        
        // Configure backup strategy
        when(mockBackupService.configureBackupStrategy(
                anyString(), anyString(), anyString()))
            .thenReturn(true);
        
        // Configure backup security
        when(mockBackupService.configureBackupSecurity(
                anyBoolean(), anyString(), anyString()))
            .thenReturn(true);
        
        // Configure backup notifications
        when(mockBackupService.configureBackupNotifications(
                anyBoolean(), anyList(), anyString()))
            .thenReturn(true);
        
        // Configure backup locations
        when(mockBackupService.configureBackupLocations(
                anyList(), anyString()))
            .thenReturn(true);
        
        // Start backup
        when(mockBackupService.startBackup("full"))
            .thenReturn("BACKUP-" + UUID.randomUUID().toString().substring(0, 8));
        
        when(mockBackupService.startBackup("incremental"))
            .thenReturn("BACKUP-" + UUID.randomUUID().toString().substring(0, 8));
        
        when(mockBackupService.startBackup("differential"))
            .thenReturn("BACKUP-" + UUID.randomUUID().toString().substring(0, 8));
        
        // Status methods - provide sample output
        when(mockBackupService.getBackupStatus())
            .thenReturn(
                "Backup Configuration Status\n" +
                "==========================\n\n" +
                "Type: full\n" +
                "Frequency: weekly\n" +
                "Time: 02:00\n" +
                "Retention: 30 days\n" +
                "Location: /backup/primary\n" +
                "Last Backup: 2025-04-07 02:00\n" +
                "Next Backup: 2025-04-14 02:00\n" +
                "Active Backups: 4\n" +
                "Total Backup Size: 1.23 GB"
            );
        
        when(mockBackupService.getBackupStrategyStatus())
            .thenReturn(
                "Backup Strategy Status\n" +
                "=====================\n\n" +
                "Strategy: incremental\n" +
                "Full Backup Frequency: weekly\n" +
                "Incremental Backup Frequency: daily\n" +
                "Next Full Backup: 2025-04-14 02:00\n" +
                "Next Incremental Backup: 2025-04-08 02:00"
            );
        
        when(mockBackupService.getBackupSecurityStatus())
            .thenReturn(
                "Backup Security Status\n" +
                "=====================\n\n" +
                "Encryption: enabled\n" +
                "Algorithm: AES-256\n" +
                "Key Rotation: disabled\n" +
                "Last Changed: 2025-04-07 15:30"
            );
        
        when(mockBackupService.getBackupNotificationsStatus())
            .thenReturn(
                "Backup Notifications Status\n" +
                "==========================\n\n" +
                "Notifications: enabled\n" +
                "Events: success, failure, warning\n" +
                "Recipients: admin@example.com, monitor@example.com\n" +
                "Last Notification: 2025-04-07 02:00 (success)"
            );
        
        when(mockBackupService.listBackups())
            .thenReturn(
                "Available Backups\n" +
                "================\n\n" +
                "ID                | Type        | Date                | Status    | Size\n" +
                "------------------|-------------|---------------------|-----------|-------\n" +
                "BACKUP-A1B2C3D4   | full        | 2025-04-07 02:00:00 | complete  | 800 MB\n" +
                "BACKUP-E5F6G7H8   | incremental | 2025-04-06 02:00:00 | complete  | 120 MB\n" +
                "BACKUP-I9J0K1L2   | incremental | 2025-04-05 02:00:00 | complete  | 180 MB\n" +
                "BACKUP-M3N4O5P6   | incremental | 2025-04-04 02:00:00 | complete  | 140 MB"
            );
        
        when(mockBackupService.getBackupHistory())
            .thenReturn(
                "Backup History\n" +
                "==============\n\n" +
                "ID                | Type        | Date                | Status    | Size    | Duration\n" +
                "------------------|-------------|---------------------|-----------|---------|----------\n" +
                "BACKUP-A1B2C3D4   | full        | 2025-04-07 02:00:00 | complete  | 800 MB  | 12m 30s\n" +
                "BACKUP-E5F6G7H8   | incremental | 2025-04-06 02:00:00 | complete  | 120 MB  | 3m 45s\n" +
                "BACKUP-I9J0K1L2   | incremental | 2025-04-05 02:00:00 | complete  | 180 MB  | 5m 20s\n" +
                "BACKUP-M3N4O5P6   | incremental | 2025-04-04 02:00:00 | complete  | 140 MB  | 4m 10s\n" +
                "BACKUP-Q7R8S9T0   | full        | 2025-03-31 02:00:00 | complete  | 750 MB  | 11m 50s"
            );
        
        when(mockBackupService.listBackupLocations())
            .thenReturn(
                "Backup Storage Locations\n" +
                "=======================\n\n" +
                "Location                | Status    | Free Space | Total Space\n" +
                "------------------------|-----------|------------|------------\n" +
                "/backup/primary         | online    | 250.5 GB   | 500.0 GB\n" +
                "/backup/secondary       | online    | 320.0 GB   | 500.0 GB"
            );
        
        when(mockBackupService.verifyBackup("latest"))
            .thenReturn(
                "Backup Verification Report\n" +
                "=========================\n\n" +
                "Backup ID: BACKUP-A1B2C3D4\n" +
                "Verification Status: SUCCESS\n" +
                "Errors Found: 0\n" +
                "Verification Time: 2025-04-08 09:30:45\n" +
                "Duration: 3m 20s\n\n" +
                "Details:\n" +
                "- All files verified successfully\n" +
                "- Checksums verified: 12,543\n" +
                "- Total data verified: 800 MB"
            );
    }
    
    // The remaining necessary step definitions are already defined in other step classes
}