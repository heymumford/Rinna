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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.cli.domain.model.CliHistoryEntry;
import org.rinna.cli.domain.model.HistoryEntry;
import org.rinna.cli.domain.model.HistoryEntryType;
import org.rinna.cli.service.MockHistoryService.HistoryEntryRecord;
import org.rinna.cli.usecase.HistoryService;

/**
 * Mock implementation of the HistoryService interface that adapts between the core and CLI domains.
 */
public class MockCliHistoryService implements HistoryService {
    
    private final MockHistoryService mockHistoryService;
    
    /**
     * Creates a new MockCliHistoryService.
     * 
     * @param mockHistoryService the underlying mock history service
     */
    public MockCliHistoryService(MockHistoryService mockHistoryService) {
        this.mockHistoryService = mockHistoryService;
    }
    
    @Override
    public void recordStateChange(UUID workItemId, String user, String previousState, String newState) {
        mockHistoryService.recordStateChange(workItemId, user, previousState, newState, "");
    }
    
    @Override
    public void recordStateChange(UUID workItemId, String user, String previousState, String newState, String comment) {
        mockHistoryService.recordStateChange(workItemId, user, previousState, newState, comment);
    }
    
    @Override
    public void recordFieldChange(UUID workItemId, String user, String field, String oldValue, String newValue) {
        mockHistoryService.recordFieldChange(workItemId, user, field, oldValue, newValue);
    }
    
    @Override
    public void recordAssignmentChange(UUID workItemId, String user, String previousAssignee, String newAssignee) {
        mockHistoryService.recordAssignment(workItemId, user, previousAssignee, newAssignee);
    }
    
    @Override
    public void recordPriorityChange(UUID workItemId, String user, String previousPriority, String newPriority) {
        mockHistoryService.recordPriorityChange(workItemId, user, previousPriority, newPriority);
    }
    
    @Override
    public void recordComment(UUID workItemId, String user, String comment) {
        mockHistoryService.addComment(workItemId, user, comment);
    }
    
    @Override
    public void recordRelationshipChange(UUID workItemId, String user, String changeType, UUID relatedItemId) {
        mockHistoryService.recordLink(workItemId, user, changeType, relatedItemId.toString());
    }
    
    @Override
    public List<HistoryEntry> getHistory(UUID workItemId) {
        List<HistoryEntryRecord> records = mockHistoryService.getHistory(workItemId);
        return records.stream()
                .map(CliHistoryEntry::new)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<HistoryEntry> getHistoryByType(UUID workItemId, HistoryEntryType type) {
        // We need to convert the CLI-specific type to the MockHistoryService type
        MockHistoryService.HistoryEntryType mockType = 
            MockHistoryService.HistoryEntryType.valueOf(type.name());
            
        List<HistoryEntryRecord> records = mockHistoryService.getHistoryByType(workItemId, mockType);
        return records.stream()
                .map(CliHistoryEntry::new)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<HistoryEntry> getHistoryFromLastHours(UUID workItemId, int hours) {
        Instant startTime = Instant.now().minus(hours, ChronoUnit.HOURS);
        Instant endTime = Instant.now();
        
        List<HistoryEntryRecord> records = mockHistoryService.getHistoryByTimeRange(workItemId, startTime, endTime);
        return records.stream()
                .map(CliHistoryEntry::new)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<HistoryEntry> getHistoryFromLastDays(UUID workItemId, int days) {
        Instant startTime = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant endTime = Instant.now();
        
        List<HistoryEntryRecord> records = mockHistoryService.getHistoryByTimeRange(workItemId, startTime, endTime);
        return records.stream()
                .map(CliHistoryEntry::new)
                .collect(Collectors.toList());
    }
}