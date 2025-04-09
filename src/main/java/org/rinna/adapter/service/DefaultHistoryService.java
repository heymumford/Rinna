/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.model.HistoryEntry;
import org.rinna.domain.model.HistoryEntryRecord;
import org.rinna.domain.model.HistoryEntryType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.HistoryRepository;
import org.rinna.usecase.HistoryService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of the HistoryService interface.
 */
public class DefaultHistoryService implements HistoryService {
    private final HistoryRepository historyRepository;
    
    /**
     * Constructs a DefaultHistoryService with the given repository.
     *
     * @param historyRepository the history repository to use
     */
    public DefaultHistoryService(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }
    
    @Override
    public HistoryEntry recordHistoryEntry(UUID workItemId, HistoryEntryType type, String user, String content, String additionalData) {
        HistoryEntry entry = HistoryEntryRecord.create(workItemId, type, user, content, additionalData);
        return historyRepository.save(entry);
    }
    
    @Override
    public HistoryEntry recordStateChange(UUID workItemId, String user, String oldState, String newState, String comment) {
        WorkflowState oldStateEnum = WorkflowState.valueOf(oldState);
        WorkflowState newStateEnum = WorkflowState.valueOf(newState);
        HistoryEntry entry = HistoryEntryRecord.createStateChange(workItemId, user, oldStateEnum, newStateEnum, comment);
        return historyRepository.save(entry);
    }
    
    @Override
    public HistoryEntry recordFieldChange(UUID workItemId, String user, String field, String oldValue, String newValue) {
        HistoryEntry entry = HistoryEntryRecord.createFieldChange(workItemId, user, field, oldValue, newValue);
        return historyRepository.save(entry);
    }
    
    @Override
    public List<HistoryEntry> getHistory(UUID workItemId) {
        return historyRepository.findByWorkItemId(workItemId);
    }
    
    @Override
    public List<HistoryEntry> getHistoryByType(UUID workItemId, HistoryEntryType type) {
        return historyRepository.findByWorkItemIdAndType(workItemId, type);
    }
    
    @Override
    public List<HistoryEntry> getHistoryInTimeRange(UUID workItemId, Instant from, Instant to) {
        return historyRepository.findByWorkItemIdAndTimeRange(workItemId, from, to);
    }
    
    @Override
    public List<HistoryEntry> getHistoryFromLastHours(UUID workItemId, int hours) {
        Instant now = Instant.now();
        Instant from = now.minus(hours, ChronoUnit.HOURS);
        return getHistoryInTimeRange(workItemId, from, now);
    }
    
    @Override
    public List<HistoryEntry> getHistoryFromLastDays(UUID workItemId, int days) {
        Instant now = Instant.now();
        Instant from = now.minus(days, ChronoUnit.DAYS);
        return getHistoryInTimeRange(workItemId, from, now);
    }
    
    @Override
    public List<HistoryEntry> getHistoryFromLastWeeks(UUID workItemId, int weeks) {
        Instant now = Instant.now();
        Instant from = now.minus(weeks * 7, ChronoUnit.DAYS);
        return getHistoryInTimeRange(workItemId, from, now);
    }
}
