/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.model;

import org.rinna.cli.service.MockHistoryService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of HistoryEntry that adapts from MockHistoryService.HistoryEntryRecord.
 */
public class CliHistoryEntry implements HistoryEntry {
    
    private final UUID id;
    private final UUID workItemId;
    private final HistoryEntryType type;
    private final String user;
    private final Instant timestamp;
    private final String content;
    private final String additionalData;
    private final Map<String, Object> metadata;
    
    /**
     * Creates a new CliHistoryEntry from a MockHistoryService.HistoryEntryRecord.
     * 
     * @param record the history entry record
     */
    public CliHistoryEntry(MockHistoryService.HistoryEntryRecord record) {
        this.id = UUID.randomUUID(); // Generate a new ID for the CLI entry
        this.workItemId = record.getWorkItemId();
        this.type = mapType(record.getType());
        this.user = record.getUser();
        this.timestamp = record.getTimestamp();
        this.content = record.getContent();
        this.additionalData = record.getAdditionalData();
        this.metadata = extractMetadata(record);
    }
    
    /**
     * Maps a MockHistoryService.HistoryEntryType to a HistoryEntryType.
     * 
     * @param type the MockHistoryService.HistoryEntryType
     * @return the corresponding HistoryEntryType
     */
    private HistoryEntryType mapType(MockHistoryService.HistoryEntryType type) {
        switch (type) {
            case STATE_CHANGE:
                return HistoryEntryType.STATE_CHANGE;
            case FIELD_CHANGE:
                return HistoryEntryType.FIELD_CHANGE;
            case ASSIGNMENT:
                return HistoryEntryType.ASSIGNMENT_CHANGE;
            case COMMENT:
                return HistoryEntryType.COMMENT;
            case LINK:
                return HistoryEntryType.RELATIONSHIP_CHANGE;
            case CREATION:
            default:
                return HistoryEntryType.FIELD_CHANGE;
        }
    }
    
    /**
     * Extracts metadata from a MockHistoryService.HistoryEntryRecord.
     * 
     * @param record the history entry record
     * @return the metadata
     */
    private Map<String, Object> extractMetadata(MockHistoryService.HistoryEntryRecord record) {
        Map<String, Object> metadata = new HashMap<>();
        
        if (record.getType() == MockHistoryService.HistoryEntryType.STATE_CHANGE) {
            String content = record.getContent();
            if (content.startsWith("State changed from ")) {
                String statePart = content.substring("State changed from ".length());
                String[] states = statePart.split(" to ");
                if (states.length == 2) {
                    metadata.put("previousState", states[0]);
                    metadata.put("newState", states[1]);
                }
            }
        } else if (record.getType() == MockHistoryService.HistoryEntryType.FIELD_CHANGE) {
            String content = record.getContent();
            if (content.startsWith("Field '")) {
                int fieldStart = content.indexOf("'") + 1;
                int fieldEnd = content.indexOf("'", fieldStart);
                if (fieldEnd > fieldStart) {
                    String field = content.substring(fieldStart, fieldEnd);
                    metadata.put("field", field);
                    
                    // Extract values
                    int valuesStart = content.indexOf("'", fieldEnd + 1) + 1;
                    int valuesEnd = content.indexOf("'", valuesStart);
                    if (valuesEnd > valuesStart) {
                        String previousValue = content.substring(valuesStart, valuesEnd);
                        metadata.put("previousValue", previousValue);
                        
                        int newValueStart = content.indexOf("'", valuesEnd + 1) + 1;
                        int newValueEnd = content.indexOf("'", newValueStart);
                        if (newValueEnd > newValueStart) {
                            String newValue = content.substring(newValueStart, newValueEnd);
                            metadata.put("newValue", newValue);
                        }
                    }
                }
            }
        }
        
        return metadata;
    }
    
    @Override
    public UUID id() {
        return id;
    }
    
    @Override
    public UUID workItemId() {
        return workItemId;
    }
    
    @Override
    public HistoryEntryType type() {
        return type;
    }
    
    @Override
    public String user() {
        return user;
    }
    
    @Override
    public Instant timestamp() {
        return timestamp;
    }
    
    @Override
    public String content() {
        return content;
    }
    
    @Override
    public String additionalData() {
        return additionalData;
    }
    
    @Override
    public Map<String, Object> metadata() {
        return metadata;
    }
}