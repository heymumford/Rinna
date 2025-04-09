/*
 * MockHistoryServiceTest for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockHistoryService.HistoryEntryRecord;
import org.rinna.cli.service.MockHistoryService.HistoryEntryType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MockHistoryService class.
 * 
 * This test suite follows best practices:
 * 1. Entry Creation Tests - Testing the creation of different types of history entries
 * 2. Query Tests - Testing retrieval of history entries based on various criteria
 * 3. Time Range Tests - Testing time-based filtering of history entries
 * 4. Record Operations Tests - Testing operations on individual history records
 */
@DisplayName("MockHistoryService Tests")
class MockHistoryServiceTest {
    
    private MockHistoryService historyService;
    private UUID testWorkItemId;
    
    @BeforeEach
    void setUp() {
        historyService = new MockHistoryService();
        testWorkItemId = UUID.randomUUID();
    }
    
    @Nested
    @DisplayName("Entry Creation Tests")
    class EntryCreationTests {
        
        @Test
        @DisplayName("Should record a generic history entry")
        void shouldRecordGenericHistoryEntry() {
            // Given
            String user = "testuser";
            String content = "Test content";
            String additionalData = "Test additional data";
            
            // When
            HistoryEntryRecord entry = historyService.recordHistoryEntry(
                testWorkItemId, HistoryEntryType.CREATION, user, content, additionalData);
            
            // Then
            assertNotNull(entry, "Entry should not be null");
            assertEquals(testWorkItemId, entry.getWorkItemId(), "Work item ID should match");
            assertEquals(HistoryEntryType.CREATION, entry.getType(), "Entry type should match");
            assertEquals(user, entry.getUser(), "User should match");
            assertEquals(content, entry.getContent(), "Content should match");
            assertEquals(additionalData, entry.getAdditionalData(), "Additional data should match");
            assertNotNull(entry.getTimestamp(), "Timestamp should not be null");
            
            // Verify entry is in the history
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            assertEquals(1, history.size(), "History should contain 1 entry");
            assertEquals(entry, history.get(0), "Entry in history should match the created entry");
        }
        
        @Test
        @DisplayName("Should record a state change")
        void shouldRecordStateChange() {
            // Given
            String user = "testuser";
            String oldState = "OPEN";
            String newState = "IN_PROGRESS";
            String comment = "Moving to in progress";
            
            // When
            HistoryEntryRecord entry = historyService.recordStateChange(
                testWorkItemId, user, oldState, newState, comment);
            
            // Then
            assertNotNull(entry, "Entry should not be null");
            assertEquals(testWorkItemId, entry.getWorkItemId(), "Work item ID should match");
            assertEquals(HistoryEntryType.STATE_CHANGE, entry.getType(), "Entry type should match");
            assertEquals(user, entry.getUser(), "User should match");
            assertTrue(entry.getContent().contains(oldState), "Content should contain old state");
            assertTrue(entry.getContent().contains(newState), "Content should contain new state");
            assertEquals(comment, entry.getAdditionalData(), "Comment should be stored as additional data");
            
            // Verify entry is in the history
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            assertEquals(1, history.size(), "History should contain 1 entry");
        }
        
        @Test
        @DisplayName("Should record a field change")
        void shouldRecordFieldChange() {
            // Given
            String user = "testuser";
            String field = "title";
            String oldValue = "Old Title";
            String newValue = "New Title";
            
            // When
            HistoryEntryRecord entry = historyService.recordFieldChange(
                testWorkItemId, user, field, oldValue, newValue);
            
            // Then
            assertNotNull(entry, "Entry should not be null");
            assertEquals(testWorkItemId, entry.getWorkItemId(), "Work item ID should match");
            assertEquals(HistoryEntryType.FIELD_CHANGE, entry.getType(), "Entry type should match");
            assertEquals(user, entry.getUser(), "User should match");
            assertTrue(entry.getContent().contains(field), "Content should contain field name");
            assertTrue(entry.getContent().contains(oldValue), "Content should contain old value");
            assertTrue(entry.getContent().contains(newValue), "Content should contain new value");
            
            // Verify entry is in the history
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            assertEquals(1, history.size(), "History should contain 1 entry");
        }
        
        @Test
        @DisplayName("Should record an assignment change")
        void shouldRecordAssignmentChange() {
            // Given
            String user = "testuser";
            String previousAssignee = "olduser";
            String newAssignee = "newuser";
            
            // When
            HistoryEntryRecord entry = historyService.recordAssignment(
                testWorkItemId, user, previousAssignee, newAssignee);
            
            // Then
            assertNotNull(entry, "Entry should not be null");
            assertEquals(testWorkItemId, entry.getWorkItemId(), "Work item ID should match");
            assertEquals(HistoryEntryType.ASSIGNMENT, entry.getType(), "Entry type should match");
            assertEquals(user, entry.getUser(), "User should match");
            assertTrue(entry.getContent().contains(previousAssignee), "Content should contain previous assignee");
            assertTrue(entry.getContent().contains(newAssignee), "Content should contain new assignee");
            
            // Verify entry is in the history
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            assertEquals(1, history.size(), "History should contain 1 entry");
        }
        
        @Test
        @DisplayName("Should record a priority change")
        void shouldRecordPriorityChange() {
            // Given
            String user = "testuser";
            String previousPriority = "LOW";
            String newPriority = "HIGH";
            
            // When
            HistoryEntryRecord entry = historyService.recordPriorityChange(
                testWorkItemId, user, previousPriority, newPriority);
            
            // Then
            assertNotNull(entry, "Entry should not be null");
            assertEquals(testWorkItemId, entry.getWorkItemId(), "Work item ID should match");
            assertEquals(HistoryEntryType.FIELD_CHANGE, entry.getType(), "Entry type should match");
            assertEquals(user, entry.getUser(), "User should match");
            assertTrue(entry.getContent().contains(previousPriority), "Content should contain previous priority");
            assertTrue(entry.getContent().contains(newPriority), "Content should contain new priority");
            assertEquals("Priority", entry.getAdditionalData(), "Additional data should be 'Priority'");
            
            // Verify entry is in the history
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            assertEquals(1, history.size(), "History should contain 1 entry");
        }
        
        @Test
        @DisplayName("Should add a comment")
        void shouldAddComment() {
            // Given
            String user = "testuser";
            String comment = "This is a test comment";
            
            // When
            HistoryEntryRecord entry = historyService.addComment(testWorkItemId, user, comment);
            
            // Then
            assertNotNull(entry, "Entry should not be null");
            assertEquals(testWorkItemId, entry.getWorkItemId(), "Work item ID should match");
            assertEquals(HistoryEntryType.COMMENT, entry.getType(), "Entry type should match");
            assertEquals(user, entry.getUser(), "User should match");
            assertEquals(comment, entry.getContent(), "Content should be the comment text");
            
            // Verify entry is in the history
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            assertEquals(1, history.size(), "History should contain 1 entry");
        }
        
        @Test
        @DisplayName("Should record a link change")
        void shouldRecordLinkChange() {
            // Given
            String user = "testuser";
            String changeType = "Added";
            String relatedItemId = UUID.randomUUID().toString();
            
            // When
            HistoryEntryRecord entry = historyService.recordLink(
                testWorkItemId, user, changeType, relatedItemId);
            
            // Then
            assertNotNull(entry, "Entry should not be null");
            assertEquals(testWorkItemId, entry.getWorkItemId(), "Work item ID should match");
            assertEquals(HistoryEntryType.LINK, entry.getType(), "Entry type should match");
            assertEquals(user, entry.getUser(), "User should match");
            assertTrue(entry.getContent().contains(changeType), "Content should contain change type");
            assertTrue(entry.getContent().contains(relatedItemId), "Content should contain related item ID");
            assertEquals(relatedItemId, entry.getAdditionalData(), "Additional data should be the related item ID");
            
            // Verify entry is in the history
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            assertEquals(1, history.size(), "History should contain 1 entry");
        }
        
        @Test
        @DisplayName("Should add a custom mock entry")
        void shouldAddCustomMockEntry() {
            // Given
            HistoryEntryRecord customEntry = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.CREATION, "testuser", 
                "Custom content", "Custom data", Instant.now());
            
            // When
            historyService.addMockEntry(customEntry);
            
            // Then
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            assertEquals(1, history.size(), "History should contain 1 entry");
            assertEquals(customEntry, history.get(0), "Entry in history should match the custom entry");
        }
        
        @Test
        @DisplayName("Should add multiple entries in the correct order")
        void shouldAddMultipleEntriesInCorrectOrder() {
            // Given
            String user = "testuser";
            
            // When: Add entries in chronological order
            HistoryEntryRecord entry1 = historyService.recordHistoryEntry(
                testWorkItemId, HistoryEntryType.CREATION, user, "Created", null);
            HistoryEntryRecord entry2 = historyService.recordFieldChange(
                testWorkItemId, user, "title", "Old", "New");
            HistoryEntryRecord entry3 = historyService.addComment(
                testWorkItemId, user, "Comment");
            
            // Then: Entries should be in reverse chronological order (newest first)
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            assertEquals(3, history.size(), "History should contain 3 entries");
            assertEquals(entry3, history.get(0), "First entry should be the most recent (comment)");
            assertEquals(entry2, history.get(1), "Second entry should be the field change");
            assertEquals(entry1, history.get(2), "Third entry should be the creation");
        }
    }
    
    @Nested
    @DisplayName("Query Tests")
    class QueryTests {
        
        @Test
        @DisplayName("Should get history for a work item")
        void shouldGetHistoryForWorkItem() {
            // Given
            String user = "testuser";
            historyService.recordHistoryEntry(testWorkItemId, HistoryEntryType.CREATION, user, "Created", null);
            historyService.addComment(testWorkItemId, user, "Comment 1");
            historyService.addComment(testWorkItemId, user, "Comment 2");
            
            // When
            List<HistoryEntryRecord> history = historyService.getHistory(testWorkItemId);
            
            // Then
            assertEquals(3, history.size(), "History should contain 3 entries");
            
            // Create a different work item with its own history
            UUID anotherWorkItemId = UUID.randomUUID();
            historyService.recordHistoryEntry(anotherWorkItemId, HistoryEntryType.CREATION, user, "Created", null);
            
            // Verify each work item has its own history
            List<HistoryEntryRecord> firstHistory = historyService.getHistory(testWorkItemId);
            List<HistoryEntryRecord> secondHistory = historyService.getHistory(anotherWorkItemId);
            
            assertEquals(3, firstHistory.size(), "First work item should have 3 entries");
            assertEquals(1, secondHistory.size(), "Second work item should have 1 entry");
        }
        
        @Test
        @DisplayName("Should get empty history for non-existent work item")
        void shouldGetEmptyHistoryForNonExistentWorkItem() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            
            // When
            List<HistoryEntryRecord> history = historyService.getHistory(nonExistentId);
            
            // Then
            assertNotNull(history, "History should not be null");
            assertTrue(history.isEmpty(), "History should be empty");
        }
        
        @Test
        @DisplayName("Should get history entries by type")
        void shouldGetHistoryEntriesByType() {
            // Given
            String user = "testuser";
            historyService.recordHistoryEntry(testWorkItemId, HistoryEntryType.CREATION, user, "Created", null);
            historyService.addComment(testWorkItemId, user, "Comment 1");
            historyService.addComment(testWorkItemId, user, "Comment 2");
            historyService.recordFieldChange(testWorkItemId, user, "title", "Old", "New");
            
            // When
            List<HistoryEntryRecord> comments = historyService.getHistoryByType(testWorkItemId, HistoryEntryType.COMMENT);
            List<HistoryEntryRecord> creations = historyService.getHistoryByType(testWorkItemId, HistoryEntryType.CREATION);
            List<HistoryEntryRecord> fieldChanges = historyService.getHistoryByType(testWorkItemId, HistoryEntryType.FIELD_CHANGE);
            List<HistoryEntryRecord> stateChanges = historyService.getHistoryByType(testWorkItemId, HistoryEntryType.STATE_CHANGE);
            
            // Then
            assertEquals(2, comments.size(), "Should have 2 comment entries");
            assertEquals(1, creations.size(), "Should have 1 creation entry");
            assertEquals(1, fieldChanges.size(), "Should have 1 field change entry");
            assertEquals(0, stateChanges.size(), "Should have 0 state change entries");
            
            for (HistoryEntryRecord entry : comments) {
                assertEquals(HistoryEntryType.COMMENT, entry.getType(), "Entry type should be COMMENT");
            }
            
            for (HistoryEntryRecord entry : creations) {
                assertEquals(HistoryEntryType.CREATION, entry.getType(), "Entry type should be CREATION");
            }
            
            for (HistoryEntryRecord entry : fieldChanges) {
                assertEquals(HistoryEntryType.FIELD_CHANGE, entry.getType(), "Entry type should be FIELD_CHANGE");
            }
        }
    }
    
    @Nested
    @DisplayName("Time Range Tests")
    class TimeRangeTests {
        
        @Test
        @DisplayName("Should get history entries within a specific time range")
        void shouldGetHistoryEntriesWithinSpecificTimeRange() {
            // Given
            String user = "testuser";
            
            // Create entries with specific timestamps
            Instant now = Instant.now();
            Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
            Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);
            Instant threeHoursAgo = now.minus(3, ChronoUnit.HOURS);
            
            HistoryEntryRecord entry1 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.CREATION, user, "Created", null, threeHoursAgo);
            HistoryEntryRecord entry2 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.FIELD_CHANGE, user, "Changed title", null, twoHoursAgo);
            HistoryEntryRecord entry3 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.COMMENT, user, "Comment", null, oneHourAgo);
            HistoryEntryRecord entry4 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.COMMENT, user, "Another comment", null, now);
            
            historyService.addMockEntry(entry1);
            historyService.addMockEntry(entry2);
            historyService.addMockEntry(entry3);
            historyService.addMockEntry(entry4);
            
            // When: Query for entries between 2.5 hours ago and 30 minutes ago
            Instant from = now.minus(2, ChronoUnit.HOURS).minus(30, ChronoUnit.MINUTES);
            Instant to = now.minus(30, ChronoUnit.MINUTES);
            List<HistoryEntryRecord> entriesInRange = historyService.getHistoryInTimeRange(testWorkItemId, from, to);
            
            // Then
            assertEquals(2, entriesInRange.size(), "Should have 2 entries in the specified time range");
            assertTrue(entriesInRange.contains(entry2), "Should contain entry from two hours ago");
            assertTrue(entriesInRange.contains(entry3), "Should contain entry from one hour ago");
            assertFalse(entriesInRange.contains(entry1), "Should not contain entry from three hours ago");
            assertFalse(entriesInRange.contains(entry4), "Should not contain entry from now");
        }
        
        @Test
        @DisplayName("Should get history entries from the last N hours")
        void shouldGetHistoryEntriesFromLastNHours() {
            // Given
            String user = "testuser";
            
            // Create entries with specific timestamps
            Instant now = Instant.now();
            Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
            Instant threeHoursAgo = now.minus(3, ChronoUnit.HOURS);
            
            HistoryEntryRecord entry1 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.CREATION, user, "Created", null, threeHoursAgo);
            HistoryEntryRecord entry2 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.COMMENT, user, "Comment", null, oneHourAgo);
            
            historyService.addMockEntry(entry1);
            historyService.addMockEntry(entry2);
            
            // When
            List<HistoryEntryRecord> lastTwoHours = historyService.getHistoryFromLastHours(testWorkItemId, 2);
            List<HistoryEntryRecord> lastFourHours = historyService.getHistoryFromLastHours(testWorkItemId, 4);
            
            // Then
            assertEquals(1, lastTwoHours.size(), "Should have 1 entry from the last 2 hours");
            assertEquals(2, lastFourHours.size(), "Should have 2 entries from the last 4 hours");
            assertTrue(lastTwoHours.contains(entry2), "Last 2 hours should contain the recent entry");
            assertFalse(lastTwoHours.contains(entry1), "Last 2 hours should not contain the older entry");
            assertTrue(lastFourHours.contains(entry1), "Last 4 hours should contain the older entry");
            assertTrue(lastFourHours.contains(entry2), "Last 4 hours should contain the recent entry");
        }
        
        @Test
        @DisplayName("Should get history entries from the last N days")
        void shouldGetHistoryEntriesFromLastNDays() {
            // Given
            String user = "testuser";
            
            // Create entries with specific timestamps
            Instant now = Instant.now();
            Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);
            Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
            
            HistoryEntryRecord entry1 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.CREATION, user, "Created", null, threeDaysAgo);
            HistoryEntryRecord entry2 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.COMMENT, user, "Comment", null, oneDayAgo);
            
            historyService.addMockEntry(entry1);
            historyService.addMockEntry(entry2);
            
            // When
            List<HistoryEntryRecord> lastTwoDays = historyService.getHistoryFromLastDays(testWorkItemId, 2);
            List<HistoryEntryRecord> lastFourDays = historyService.getHistoryFromLastDays(testWorkItemId, 4);
            
            // Then
            assertEquals(1, lastTwoDays.size(), "Should have 1 entry from the last 2 days");
            assertEquals(2, lastFourDays.size(), "Should have 2 entries from the last 4 days");
            assertTrue(lastTwoDays.contains(entry2), "Last 2 days should contain the recent entry");
            assertFalse(lastTwoDays.contains(entry1), "Last 2 days should not contain the older entry");
            assertTrue(lastFourDays.contains(entry1), "Last 4 days should contain the older entry");
            assertTrue(lastFourDays.contains(entry2), "Last 4 days should contain the recent entry");
        }
        
        @Test
        @DisplayName("Should get history entries from the last N weeks")
        void shouldGetHistoryEntriesFromLastNWeeks() {
            // Given
            String user = "testuser";
            
            // Create entries with specific timestamps
            Instant now = Instant.now();
            Instant oneWeekAgo = now.minus(7, ChronoUnit.DAYS);
            Instant threeWeeksAgo = now.minus(21, ChronoUnit.DAYS);
            
            HistoryEntryRecord entry1 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.CREATION, user, "Created", null, threeWeeksAgo);
            HistoryEntryRecord entry2 = new HistoryEntryRecord(
                testWorkItemId, HistoryEntryType.COMMENT, user, "Comment", null, oneWeekAgo);
            
            historyService.addMockEntry(entry1);
            historyService.addMockEntry(entry2);
            
            // When
            List<HistoryEntryRecord> lastTwoWeeks = historyService.getHistoryFromLastWeeks(testWorkItemId, 2);
            List<HistoryEntryRecord> lastFourWeeks = historyService.getHistoryFromLastWeeks(testWorkItemId, 4);
            
            // Then
            assertEquals(1, lastTwoWeeks.size(), "Should have 1 entry from the last 2 weeks");
            assertEquals(2, lastFourWeeks.size(), "Should have 2 entries from the last 4 weeks");
            assertTrue(lastTwoWeeks.contains(entry2), "Last 2 weeks should contain the recent entry");
            assertFalse(lastTwoWeeks.contains(entry1), "Last 2 weeks should not contain the older entry");
            assertTrue(lastFourWeeks.contains(entry1), "Last 4 weeks should contain the older entry");
            assertTrue(lastFourWeeks.contains(entry2), "Last 4 weeks should contain the recent entry");
        }
        
        @Test
        @DisplayName("Should get same results from getHistoryInTimeRange and getHistoryByTimeRange")
        void shouldGetSameResultsFromBothTimeRangeMethods() {
            // Given
            String user = "testuser";
            historyService.recordHistoryEntry(testWorkItemId, HistoryEntryType.CREATION, user, "Created", null);
            historyService.addComment(testWorkItemId, user, "Comment");
            
            Instant now = Instant.now();
            Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
            
            // When
            List<HistoryEntryRecord> inTimeRange = historyService.getHistoryInTimeRange(testWorkItemId, oneHourAgo, now);
            List<HistoryEntryRecord> byTimeRange = historyService.getHistoryByTimeRange(testWorkItemId, oneHourAgo, now);
            
            // Then
            assertEquals(inTimeRange.size(), byTimeRange.size(), "Both methods should return the same number of entries");
            assertEquals(inTimeRange, byTimeRange, "Both methods should return the same entries");
        }
    }
    
    @Nested
    @DisplayName("Record Operations Tests")
    class RecordOperationsTests {
        
        @Test
        @DisplayName("Should create state change record with correct format")
        void shouldCreateStateChangeRecordWithCorrectFormat() {
            // Given
            UUID workItemId = UUID.randomUUID();
            String user = "testuser";
            WorkflowState oldState = WorkflowState.OPEN;
            WorkflowState newState = WorkflowState.IN_PROGRESS;
            String comment = "Moving to in progress";
            
            // When
            HistoryEntryRecord record = HistoryEntryRecord.createStateChange(
                workItemId, user, oldState, newState, comment);
            
            // Then
            assertNotNull(record, "Record should not be null");
            assertEquals(workItemId, record.getWorkItemId(), "Work item ID should match");
            assertEquals(HistoryEntryType.STATE_CHANGE, record.getType(), "Type should be STATE_CHANGE");
            assertEquals(user, record.getUser(), "User should match");
            assertTrue(record.getContent().contains("State changed from"), "Content should start with 'State changed from'");
            assertTrue(record.getContent().contains(oldState.toString()), "Content should contain old state");
            assertTrue(record.getContent().contains(newState.toString()), "Content should contain new state");
            assertEquals(comment, record.getAdditionalData(), "Additional data should be the comment");
            assertNotNull(record.getTimestamp(), "Timestamp should not be null");
        }
        
        @Test
        @DisplayName("Should create field change record with correct format")
        void shouldCreateFieldChangeRecordWithCorrectFormat() {
            // Given
            UUID workItemId = UUID.randomUUID();
            String user = "testuser";
            String field = "title";
            String oldValue = "Old Title";
            String newValue = "New Title";
            
            // When
            HistoryEntryRecord record = HistoryEntryRecord.createFieldChange(
                workItemId, user, field, oldValue, newValue);
            
            // Then
            assertNotNull(record, "Record should not be null");
            assertEquals(workItemId, record.getWorkItemId(), "Work item ID should match");
            assertEquals(HistoryEntryType.FIELD_CHANGE, record.getType(), "Type should be FIELD_CHANGE");
            assertEquals(user, record.getUser(), "User should match");
            assertTrue(record.getContent().contains("Field '" + field + "' changed"), "Content should mention the field name");
            assertTrue(record.getContent().contains("'" + oldValue + "'"), "Content should contain old value");
            assertTrue(record.getContent().contains("'" + newValue + "'"), "Content should contain new value");
            assertNull(record.getAdditionalData(), "Additional data should be null");
            assertNotNull(record.getTimestamp(), "Timestamp should not be null");
        }
        
        @Test
        @DisplayName("Should create generic record with correct attributes")
        void shouldCreateGenericRecordWithCorrectAttributes() {
            // Given
            UUID workItemId = UUID.randomUUID();
            HistoryEntryType type = HistoryEntryType.CREATION;
            String user = "testuser";
            String content = "Test content";
            String additionalData = "Test additional data";
            
            // When
            HistoryEntryRecord record = HistoryEntryRecord.create(
                workItemId, type, user, content, additionalData);
            
            // Then
            assertNotNull(record, "Record should not be null");
            assertEquals(workItemId, record.getWorkItemId(), "Work item ID should match");
            assertEquals(type, record.getType(), "Type should match");
            assertEquals(user, record.getUser(), "User should match");
            assertEquals(content, record.getContent(), "Content should match");
            assertEquals(additionalData, record.getAdditionalData(), "Additional data should match");
            assertNotNull(record.getTimestamp(), "Timestamp should not be null");
        }
    }
}