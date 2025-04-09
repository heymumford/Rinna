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

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Interface for system recovery services.
 */
public interface RecoveryService {
    
    /**
     * Start a recovery process from a backup.
     *
     * @param backupId the ID of the backup to recover from
     * @return true if the recovery was started successfully, false otherwise
     */
    boolean startRecovery(String backupId);
    
    /**
     * Get the current recovery status.
     *
     * @return formatted recovery status information
     */
    String getRecoveryStatus();
    
    /**
     * Generate a recovery plan.
     *
     * @return the path to the generated recovery plan
     */
    String generateRecoveryPlan();
    
    /**
     * Test a recovery plan.
     *
     * @param simulation whether to run a simulation or a live test
     * @return formatted test results
     */
    String testRecoveryPlan(boolean simulation);
}