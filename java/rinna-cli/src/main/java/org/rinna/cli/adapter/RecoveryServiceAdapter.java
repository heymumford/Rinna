/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.adapter;

import org.rinna.cli.domain.service.RecoveryService;
import org.rinna.cli.service.MockRecoveryService;

/**
 * Adapter for the RecoveryService that bridges between the domain RecoveryService and
 * the CLI MockRecoveryService. This adapter implements the domain RecoveryService interface
 * while delegating to the CLI MockRecoveryService implementation.
 */
public class RecoveryServiceAdapter implements RecoveryService {
    
    private final MockRecoveryService mockRecoveryService;
    
    /**
     * Constructs a new RecoveryServiceAdapter with the specified mock recovery service.
     *
     * @param mockRecoveryService the mock recovery service to delegate to
     */
    public RecoveryServiceAdapter(MockRecoveryService mockRecoveryService) {
        this.mockRecoveryService = mockRecoveryService;
    }
    
    @Override
    public boolean startRecovery(String backupId) {
        return mockRecoveryService.startRecovery(backupId);
    }
    
    @Override
    public String getRecoveryStatus() {
        return mockRecoveryService.getRecoveryStatus();
    }
    
    @Override
    public String generateRecoveryPlan() {
        return mockRecoveryService.generateRecoveryPlan();
    }
    
    @Override
    public String testRecoveryPlan(boolean simulation) {
        return mockRecoveryService.testRecoveryPlan(simulation);
    }
}