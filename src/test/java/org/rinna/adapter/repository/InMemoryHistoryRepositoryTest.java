/*
 * Test class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.HistoryEntry;
import org.rinna.domain.model.HistoryEntryRecord;
import org.rinna.domain.model.HistoryEntryType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.HistoryRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the InMemoryHistoryRepository.
 */
public class InMemoryHistoryRepositoryTest {

    private HistoryRepository repository;
    private UUID workItem1Id;
    private UUID workItem2Id;
    private Instant now;

    @BeforeEach
    void setUp() {
        repository = new InMemoryHistoryRepository();
        workItem1Id = UUID.randomUUID();
        workItem2Id = UUID.randomUUID();
        now = Instant.now();
    }

    @Test
    void testSaveAndFindById() {
        // Create a history entry
        HistoryEntry entry = HistoryEntryRecord.createStateChange(
                workItem1Id, "alice", WorkflowState.TO_DO, WorkflowState.IN_PROGRESS, "Starting work");
        
        // Save the entry
        HistoryEntry savedEntry = repository.save(entry);
        assertNotNull(savedEntry);
        assertEquals(entry.id(), savedEntry.id());
        
        // Find the entry by ID
        Optional<HistoryEntry> foundEntry = repository.findById(entry.id());
        assertTrue(foundEntry.isPresent());
        assertEquals(entry.id(), foundEntry.get().id());
        assertEquals(entry.content(), foundEntry.get().content());
        assertEquals(entry.user(), foundEntry.get().user());
        assertEquals(entry.workItemId(), foundEntry.get().workItemId());
        assertEquals(entry.type(), foundEntry.get().type());
        assertEquals(entry.additionalData(), foundEntry.get().additionalData());
    }
    
    @Test
    void testFindByIdNotFound() {
        Optional<HistoryEntry> notFoundEntry = repository.findById(UUID.randomUUID());
        assertFalse(notFoundEntry.isPresent());
    }
    
    @Test
    void testFindByWorkItemId() {
        // Create and save multiple entries for the same work item
        HistoryEntry entry1 = HistoryEntryRecord.createStateChange(
                workItem1Id, "alice", WorkflowState.TO_DO, WorkflowState.IN_PROGRESS, "Starting work");
        HistoryEntry entry2 = HistoryEntryRecord.createFieldChange(
                workItem1Id, "bob", "title", "Old title", "New title");
        HistoryEntry entry3 = HistoryEntryRecord.createStateChange(
                workItem2Id, "charlie", WorkflowState.FOUND, WorkflowState.TRIAGED, "Triage complete");
        
        repository.save(entry1);
        repository.save(entry2);
        repository.save(entry3);
        
        // Find all entries for work item 1
        List<HistoryEntry> workItem1Entries = repository.findByWorkItemId(workItem1Id);
        assertEquals(2, workItem1Entries.size());
        assertTrue(workItem1Entries.stream().anyMatch(e -> e.id().equals(entry1.id())));
        assertTrue(workItem1Entries.stream().anyMatch(e -> e.id().equals(entry2.id())));
        
        // Find all entries for work item 2
        List<HistoryEntry> workItem2Entries = repository.findByWorkItemId(workItem2Id);
        assertEquals(1, workItem2Entries.size());
        assertEquals(entry3.id(), workItem2Entries.get(0).id());
    }
    
    @Test
    void testFindByWorkItemIdAndType() {
        // Create and save entries of different types
        HistoryEntry stateChange = HistoryEntryRecord.createStateChange(
                workItem1Id, "alice", WorkflowState.TO_DO, WorkflowState.IN_PROGRESS, "Starting work");
        HistoryEntry fieldChange = HistoryEntryRecord.createFieldChange(
                workItem1Id, "bob", "title", "Old title", "New title");
        HistoryEntry otherStateChange = HistoryEntryRecord.createStateChange(
                workItem1Id, "charlie", WorkflowState.IN_PROGRESS, WorkflowState.IN_TEST, "Testing");
        
        repository.save(stateChange);
        repository.save(fieldChange);
        repository.save(otherStateChange);
        
        // Find entries by type
        List<HistoryEntry> stateChangeEntries = repository.findByWorkItemIdAndType(workItem1Id, HistoryEntryType.STATE_CHANGE);
        assertEquals(2, stateChangeEntries.size());
        assertTrue(stateChangeEntries.stream().anyMatch(e -> e.id().equals(stateChange.id())));
        assertTrue(stateChangeEntries.stream().anyMatch(e -> e.id().equals(otherStateChange.id())));
        
        List<HistoryEntry> fieldChangeEntries = repository.findByWorkItemIdAndType(workItem1Id, HistoryEntryType.FIELD_CHANGE);
        assertEquals(1, fieldChangeEntries.size());
        assertEquals(fieldChange.id(), fieldChangeEntries.get(0).id());
        
        // Verify no entries of a type that doesn't exist
        List<HistoryEntry> commentEntries = repository.findByWorkItemIdAndType(workItem1Id, HistoryEntryType.COMMENT);
        assertTrue(commentEntries.isEmpty());
    }
    
    @Test
    void testFindByWorkItemIdAndTimeRange() {
        // Create entries with specific timestamps
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        
        HistoryEntry recentEntry = new HistoryEntryRecord(
                UUID.randomUUID(), workItem1Id, HistoryEntryType.STATE_CHANGE, "alice",
                now, "Recent change", null);
        
        HistoryEntry yesterdayEntry = new HistoryEntryRecord(
                UUID.randomUUID(), workItem1Id, HistoryEntryType.FIELD_CHANGE, "bob",
                yesterday, "Yesterday change", null);
        
        HistoryEntry oldEntry = new HistoryEntryRecord(
                UUID.randomUUID(), workItem1Id, HistoryEntryType.ASSIGNMENT_CHANGE, "charlie",
                twoDaysAgo, "Old change", null);
        
        HistoryEntry veryOldEntry = new HistoryEntryRecord(
                UUID.randomUUID(), workItem1Id, HistoryEntryType.COMMENT, "dave",
                threeDaysAgo, "Very old change", null);
        
        repository.save(recentEntry);
        repository.save(yesterdayEntry);
        repository.save(oldEntry);
        repository.save(veryOldEntry);
        
        // Find entries in different time ranges
        List<HistoryEntry> dayEntries = repository.findByWorkItemIdAndTimeRange(
                workItem1Id, yesterday.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        assertEquals(2, dayEntries.size());
        assertTrue(dayEntries.stream().anyMatch(e -> e.id().equals(recentEntry.id())));
        assertTrue(dayEntries.stream().anyMatch(e -> e.id().equals(yesterdayEntry.id())));
        
        List<HistoryEntry> weekEntries = repository.findByWorkItemIdAndTimeRange(
                workItem1Id, threeDaysAgo.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        assertEquals(4, weekEntries.size());
    }
    
    @Test
    void testFindByUser() {
        // Create entries from different users
        HistoryEntry aliceEntry1 = HistoryEntryRecord.createStateChange(
                workItem1Id, "alice", WorkflowState.TO_DO, WorkflowState.IN_PROGRESS, "Starting work");
        HistoryEntry aliceEntry2 = HistoryEntryRecord.createFieldChange(
                workItem2Id, "alice", "priority", "LOW", "HIGH");
        HistoryEntry bobEntry = HistoryEntryRecord.createStateChange(
                workItem1Id, "bob", WorkflowState.IN_PROGRESS, WorkflowState.IN_TEST, "Testing");
        
        repository.save(aliceEntry1);
        repository.save(aliceEntry2);
        repository.save(bobEntry);
        
        // Find all entries by Alice
        List<HistoryEntry> aliceEntries = repository.findByUser("alice");
        assertEquals(2, aliceEntries.size());
        assertTrue(aliceEntries.stream().anyMatch(e -> e.id().equals(aliceEntry1.id())));
        assertTrue(aliceEntries.stream().anyMatch(e -> e.id().equals(aliceEntry2.id())));
        
        // Find all entries by Bob
        List<HistoryEntry> bobEntries = repository.findByUser("bob");
        assertEquals(1, bobEntries.size());
        assertEquals(bobEntry.id(), bobEntries.get(0).id());
        
        // Find entries for non-existent user
        List<HistoryEntry> charlieEntries = repository.findByUser("charlie");
        assertTrue(charlieEntries.isEmpty());
    }
    
    @Test
    void testDeleteById() {
        // Create and save an entry
        HistoryEntry entry = HistoryEntryRecord.createStateChange(
                workItem1Id, "alice", WorkflowState.TO_DO, WorkflowState.IN_PROGRESS, "Starting work");
        repository.save(entry);
        
        // Verify the entry exists
        assertTrue(repository.findById(entry.id()).isPresent());
        
        // Delete the entry
        boolean deleted = repository.deleteById(entry.id());
        assertTrue(deleted);
        
        // Verify the entry was deleted
        assertFalse(repository.findById(entry.id()).isPresent());
        
        // Try to delete a non-existent entry
        boolean nonExistentDeleted = repository.deleteById(UUID.randomUUID());
        assertFalse(nonExistentDeleted);
    }
    
    @Test
    void testDeleteByWorkItemId() {
        // Create and save multiple entries for different work items
        HistoryEntry entry1 = HistoryEntryRecord.createStateChange(
                workItem1Id, "alice", WorkflowState.TO_DO, WorkflowState.IN_PROGRESS, "Starting work");
        HistoryEntry entry2 = HistoryEntryRecord.createFieldChange(
                workItem1Id, "bob", "title", "Old title", "New title");
        HistoryEntry entry3 = HistoryEntryRecord.createStateChange(
                workItem2Id, "charlie", WorkflowState.FOUND, WorkflowState.TRIAGED, "Triage complete");
        
        repository.save(entry1);
        repository.save(entry2);
        repository.save(entry3);
        
        // Verify all entries exist
        assertEquals(2, repository.findByWorkItemId(workItem1Id).size());
        assertEquals(1, repository.findByWorkItemId(workItem2Id).size());
        
        // Delete all entries for work item 1
        int deleted = repository.deleteByWorkItemId(workItem1Id);
        assertEquals(2, deleted);
        
        // Verify entries for work item 1 were deleted
        assertTrue(repository.findByWorkItemId(workItem1Id).isEmpty());
        
        // Verify entries for work item 2 still exist
        assertEquals(1, repository.findByWorkItemId(workItem2Id).size());
        
        // Try to delete entries for a work item with no entries
        int nonExistentDeleted = repository.deleteByWorkItemId(UUID.randomUUID());
        assertEquals(0, nonExistentDeleted);
    }
    
    @Test
    void testSaveUpdatesExistingEntry() {
        // Create and save an entry
        HistoryEntry originalEntry = HistoryEntryRecord.createStateChange(
                workItem1Id, "alice", WorkflowState.TO_DO, WorkflowState.IN_PROGRESS, "Starting work");
        repository.save(originalEntry);
        
        // Create a new entry with the same ID but different content
        HistoryEntry updatedEntry = new HistoryEntryRecord(
                originalEntry.id(),
                originalEntry.workItemId(),
                originalEntry.type(),
                originalEntry.user(),
                originalEntry.timestamp(),
                "Updated content",
                originalEntry.additionalData()
        );
        
        // Save the updated entry
        repository.save(updatedEntry);
        
        // Verify the entry was updated
        Optional<HistoryEntry> foundEntry = repository.findById(originalEntry.id());
        assertTrue(foundEntry.isPresent());
        assertEquals("Updated content", foundEntry.get().content());
    }
    
    @Test
    void testEntrySorting() {
        // Create entries with specific timestamps
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);
        
        HistoryEntry entry1 = new HistoryEntryRecord(
                UUID.randomUUID(), workItem1Id, HistoryEntryType.STATE_CHANGE, "alice",
                now, "Recent entry", null);
        
        HistoryEntry entry2 = new HistoryEntryRecord(
                UUID.randomUUID(), workItem1Id, HistoryEntryType.FIELD_CHANGE, "bob",
                oneHourAgo, "One hour old entry", null);
        
        HistoryEntry entry3 = new HistoryEntryRecord(
                UUID.randomUUID(), workItem1Id, HistoryEntryType.ASSIGNMENT_CHANGE, "charlie",
                twoHoursAgo, "Two hours old entry", null);
        
        // Save in reverse chronological order
        repository.save(entry3);
        repository.save(entry2);
        repository.save(entry1);
        
        // Verify entries are returned in most recent first order
        List<HistoryEntry> entries = repository.findByWorkItemId(workItem1Id);
        assertEquals(3, entries.size());
        assertEquals(entry1.id(), entries.get(0).id()); // Most recent first
        assertEquals(entry2.id(), entries.get(1).id());
        assertEquals(entry3.id(), entries.get(2).id());
    }
}