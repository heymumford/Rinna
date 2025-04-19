/*
 * Unit test for the InMemoryMacroRepository
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.macro.ExecutionStatus;
import org.rinna.domain.model.macro.MacroAction;
import org.rinna.domain.model.macro.MacroDefinition;
import org.rinna.domain.model.macro.MacroExecution;
import org.rinna.domain.model.macro.MacroTrigger;
import org.rinna.domain.model.macro.ScheduledExecution;
import org.rinna.domain.model.macro.TriggerContext;
import org.rinna.domain.model.macro.TriggerType;
import org.rinna.domain.repository.MacroRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InMemoryMacroRepository}.
 */
class InMemoryMacroRepositoryTest {

    private MacroRepository repository;
    private MacroDefinition macro1;
    private MacroDefinition macro2;
    private MacroExecution execution1;
    private MacroExecution execution2;
    private ScheduledExecution scheduledExecution1;
    private ScheduledExecution scheduledExecution2;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMacroRepository();
        
        // Create first macro - a manual action
        macro1 = new MacroDefinition();
        macro1.setName("Test Macro 1");
        macro1.setDescription("A test macro for manual execution");
        macro1.setOwner("admin");
        MacroTrigger trigger1 = new MacroTrigger(TriggerType.MANUAL);
        macro1.setTrigger(trigger1);
        
        // Create second macro - scheduled with webhook
        macro2 = new MacroDefinition();
        macro2.setName("Test Macro 2");
        macro2.setDescription("A test macro for scheduled execution");
        macro2.setOwner("user1");
        MacroTrigger trigger2 = new MacroTrigger(TriggerType.SCHEDULED);
        macro2.setTrigger(trigger2);
        macro2.setParameter("key1", "value1");
        macro2.setParameter("key2", "value2");
        
        // Create executions
        TriggerContext context1 = TriggerContext.forManualExecution("admin");
        execution1 = new MacroExecution();
        execution1.setMacroId(macro1.getId());
        execution1.setTriggerContext(context1);
        execution1.setStatus(ExecutionStatus.COMPLETED);
        execution1.setStartTime(LocalDateTime.now().minusMinutes(5));
        execution1.setEndTime(LocalDateTime.now().minusMinutes(4));
        
        TriggerContext context2 = TriggerContext.forScheduledExecution();
        execution2 = new MacroExecution();
        execution2.setMacroId(macro2.getId());
        execution2.setTriggerContext(context2);
        execution2.setStatus(ExecutionStatus.FAILED);
        execution2.setStartTime(LocalDateTime.now().minusMinutes(10));
        execution2.setEndTime(LocalDateTime.now().minusMinutes(9));
        execution2.setErrorMessage("Test error message");
        
        // Create scheduled executions
        scheduledExecution1 = new ScheduledExecution(macro1.getId(), LocalDateTime.now().plusHours(1));
        scheduledExecution2 = new ScheduledExecution(macro2.getId(), LocalDateTime.now().plusDays(1));
        scheduledExecution2.setMaxExecutions(5);
    }

    @Test
    void testCreateAndFindById() {
        // Create a macro
        MacroDefinition savedMacro = repository.create(macro1);
        
        // Verify saved macro
        assertNotNull(savedMacro);
        assertNotNull(savedMacro.getId());
        assertEquals("Test Macro 1", savedMacro.getName());
        assertEquals("A test macro for manual execution", savedMacro.getDescription());
        assertEquals("admin", savedMacro.getOwner());
        assertNotNull(savedMacro.getCreatedAt());
        assertNotNull(savedMacro.getUpdatedAt());
        
        // Find by ID
        MacroDefinition foundMacro = repository.findById(savedMacro.getId());
        
        // Verify found macro
        assertNotNull(foundMacro);
        assertEquals(savedMacro.getId(), foundMacro.getId());
        assertEquals("Test Macro 1", foundMacro.getName());
        assertEquals(TriggerType.MANUAL, foundMacro.getTrigger().getType());
    }

    @Test
    void testFindByIdNonExistent() {
        // Attempt to find non-existent macro
        MacroDefinition foundMacro = repository.findById(UUID.randomUUID().toString());
        
        // Verify not found
        assertNull(foundMacro);
    }

    @Test
    void testUpdate() {
        // Create a macro
        MacroDefinition savedMacro = repository.create(macro1);
        String originalId = savedMacro.getId();
        
        // Modify the macro
        savedMacro.setName("Updated Macro");
        savedMacro.setDescription("Updated description");
        savedMacro.setEnabled(false);
        
        // Update the macro
        MacroDefinition updatedMacro = repository.update(savedMacro);
        
        // Verify updated macro
        assertNotNull(updatedMacro);
        assertEquals(originalId, updatedMacro.getId());
        assertEquals("Updated Macro", updatedMacro.getName());
        assertEquals("Updated description", updatedMacro.getDescription());
        assertFalse(updatedMacro.isEnabled());
        
        // Verify by finding again
        MacroDefinition foundMacro = repository.findById(originalId);
        assertNotNull(foundMacro);
        assertEquals("Updated Macro", foundMacro.getName());
        assertEquals("Updated description", foundMacro.getDescription());
        assertFalse(foundMacro.isEnabled());
    }

    @Test
    void testUpdateNonExistent() {
        // Create macro with random ID
        macro1.setId(UUID.randomUUID().toString());
        
        // Attempt to update non-existent macro
        assertThrows(IllegalArgumentException.class, () -> repository.update(macro1));
    }

    @Test
    void testDeleteById() {
        // Create macros
        MacroDefinition savedMacro1 = repository.create(macro1);
        MacroDefinition savedMacro2 = repository.create(macro2);
        
        // Verify both exist
        assertNotNull(repository.findById(savedMacro1.getId()));
        assertNotNull(repository.findById(savedMacro2.getId()));
        
        // Delete one macro
        boolean deleted = repository.delete(savedMacro1.getId());
        
        // Verify deletion was successful
        assertTrue(deleted);
        
        // Verify only one remains
        assertNull(repository.findById(savedMacro1.getId()));
        assertNotNull(repository.findById(savedMacro2.getId()));
    }

    @Test
    void testDeleteNonExistent() {
        // Attempt to delete non-existent macro
        boolean deleted = repository.delete(UUID.randomUUID().toString());
        
        // Verify deletion failed
        assertFalse(deleted);
    }

    @Test
    void testFindAll() {
        // Initially empty
        List<MacroDefinition> allMacros = repository.findAll();
        assertTrue(allMacros.isEmpty());
        
        // Create macros
        repository.create(macro1);
        repository.create(macro2);
        
        // Find all macros
        allMacros = repository.findAll();
        
        // Verify found macros
        assertEquals(2, allMacros.size());
    }

    @Test
    void testFindByFilters() {
        // Create macros
        repository.create(macro1);
        repository.create(macro2);
        
        // Create filters
        Map<String, String> filters = new HashMap<>();
        
        // Filter by owner
        filters.put("owner", "admin");
        List<MacroDefinition> filtered = repository.findByFilters(filters);
        assertEquals(1, filtered.size());
        assertEquals("Test Macro 1", filtered.get(0).getName());
        
        // Filter by name (partial match)
        filters.clear();
        filters.put("name", "Macro 2");
        filtered = repository.findByFilters(filters);
        assertEquals(1, filtered.size());
        assertEquals("Test Macro 2", filtered.get(0).getName());
        
        // Filter by enabled
        filters.clear();
        filters.put("enabled", "true");
        filtered = repository.findByFilters(filters);
        assertEquals(2, filtered.size()); // Both are enabled by default
        
        // Filter by parameter
        filters.clear();
        filters.put("key1", "value1");
        filtered = repository.findByFilters(filters);
        assertEquals(1, filtered.size());
        assertEquals("Test Macro 2", filtered.get(0).getName());
        
        // No matches
        filters.clear();
        filters.put("owner", "nonexistent");
        filtered = repository.findByFilters(filters);
        assertTrue(filtered.isEmpty());
        
        // Empty filters should return all
        filtered = repository.findByFilters(new HashMap<>());
        assertEquals(2, filtered.size());
        
        // Null filters should return all
        filtered = repository.findByFilters(null);
        assertEquals(2, filtered.size());
    }

    @Test
    void testFindByTriggerType() {
        // Create macros
        repository.create(macro1);
        repository.create(macro2);
        
        // Find by manual trigger
        List<MacroDefinition> manualMacros = repository.findByTriggerType(TriggerType.MANUAL);
        assertEquals(1, manualMacros.size());
        assertEquals("Test Macro 1", manualMacros.get(0).getName());
        
        // Find by scheduled trigger
        List<MacroDefinition> scheduledMacros = repository.findByTriggerType(TriggerType.SCHEDULED);
        assertEquals(1, scheduledMacros.size());
        assertEquals("Test Macro 2", scheduledMacros.get(0).getName());
        
        // Find by non-existent trigger
        List<MacroDefinition> webhookMacros = repository.findByTriggerType(TriggerType.WEBHOOK);
        assertTrue(webhookMacros.isEmpty());
        
        // Find with null trigger
        List<MacroDefinition> nullTrigger = repository.findByTriggerType(null);
        assertTrue(nullTrigger.isEmpty());
    }

    @Test
    void testSaveAndFindExecution() {
        // Create macro
        MacroDefinition savedMacro = repository.create(macro1);
        
        // Set macro ID for execution
        execution1.setMacroId(savedMacro.getId());
        
        // Save execution
        MacroExecution savedExecution = repository.saveExecution(execution1);
        
        // Verify saved execution
        assertNotNull(savedExecution);
        assertNotNull(savedExecution.getId());
        assertEquals(savedMacro.getId(), savedExecution.getMacroId());
        assertEquals(ExecutionStatus.COMPLETED, savedExecution.getStatus());
        
        // Find execution by ID
        MacroExecution foundExecution = repository.findExecutionById(savedExecution.getId());
        
        // Verify found execution
        assertNotNull(foundExecution);
        assertEquals(savedExecution.getId(), foundExecution.getId());
        assertEquals(savedMacro.getId(), foundExecution.getMacroId());
        assertEquals(ExecutionStatus.COMPLETED, foundExecution.getStatus());
        
        // Verify the execution was added to the macro's recent executions
        MacroDefinition macroWithExecution = repository.findById(savedMacro.getId());
        assertEquals(1, macroWithExecution.getRecentExecutions().size());
        assertEquals(savedExecution.getId(), macroWithExecution.getRecentExecutions().get(0).getId());
    }

    @Test
    void testFindExecutionsByMacroId() {
        // Create macros
        MacroDefinition savedMacro1 = repository.create(macro1);
        MacroDefinition savedMacro2 = repository.create(macro2);
        
        // Set macro IDs for executions
        execution1.setMacroId(savedMacro1.getId());
        execution2.setMacroId(savedMacro2.getId());
        
        // Save executions
        repository.saveExecution(execution1);
        repository.saveExecution(execution2);
        
        // Save another execution for macro1
        MacroExecution anotherExecution = new MacroExecution();
        anotherExecution.setMacroId(savedMacro1.getId());
        anotherExecution.setTriggerContext(TriggerContext.forManualExecution("user2"));
        anotherExecution.setStatus(ExecutionStatus.RUNNING);
        anotherExecution.setStartTime(LocalDateTime.now());
        repository.saveExecution(anotherExecution);
        
        // Find executions for macro1
        List<MacroExecution> macro1Executions = repository.findExecutionsByMacroId(savedMacro1.getId(), 10);
        
        // Verify found executions
        assertEquals(2, macro1Executions.size());
        
        // Find executions for macro2
        List<MacroExecution> macro2Executions = repository.findExecutionsByMacroId(savedMacro2.getId(), 10);
        
        // Verify found executions
        assertEquals(1, macro2Executions.size());
        assertEquals(ExecutionStatus.FAILED, macro2Executions.get(0).getStatus());
        assertEquals("Test error message", macro2Executions.get(0).getErrorMessage());
        
        // Test limit
        List<MacroExecution> limitedExecutions = repository.findExecutionsByMacroId(savedMacro1.getId(), 1);
        assertEquals(1, limitedExecutions.size());
        
        // Test non-existent macro ID
        List<MacroExecution> nonExistentExecutions = repository.findExecutionsByMacroId(UUID.randomUUID().toString(), 10);
        assertTrue(nonExistentExecutions.isEmpty());
        
        // Test null macro ID
        List<MacroExecution> nullIdExecutions = repository.findExecutionsByMacroId(null, 10);
        assertTrue(nullIdExecutions.isEmpty());
    }

    @Test
    void testSaveAndFindScheduledExecution() {
        // Create macro
        MacroDefinition savedMacro = repository.create(macro1);
        
        // Set macro ID for scheduled execution
        scheduledExecution1.setMacroId(savedMacro.getId());
        
        // Save scheduled execution
        ScheduledExecution savedScheduled = repository.saveScheduledExecution(scheduledExecution1);
        
        // Verify saved scheduled execution
        assertNotNull(savedScheduled);
        assertNotNull(savedScheduled.getId());
        assertEquals(savedMacro.getId(), savedScheduled.getMacroId());
        assertEquals(0, savedScheduled.getExecutionCount());
        
        // Find all scheduled executions
        List<ScheduledExecution> allScheduled = repository.findAllScheduledExecutions();
        
        // Verify found scheduled executions
        assertEquals(1, allScheduled.size());
        assertEquals(savedScheduled.getId(), allScheduled.get(0).getId());
    }

    @Test
    void testFindScheduledExecutionsByMacroId() {
        // Create macros
        MacroDefinition savedMacro1 = repository.create(macro1);
        MacroDefinition savedMacro2 = repository.create(macro2);
        
        // Set macro IDs for scheduled executions
        scheduledExecution1.setMacroId(savedMacro1.getId());
        scheduledExecution2.setMacroId(savedMacro2.getId());
        
        // Save scheduled executions
        repository.saveScheduledExecution(scheduledExecution1);
        repository.saveScheduledExecution(scheduledExecution2);
        
        // Save another scheduled execution for macro1
        ScheduledExecution anotherScheduled = new ScheduledExecution(savedMacro1.getId(), LocalDateTime.now().plusHours(2));
        repository.saveScheduledExecution(anotherScheduled);
        
        // Find scheduled executions for macro1
        List<ScheduledExecution> macro1Scheduled = repository.findScheduledExecutionsByMacroId(savedMacro1.getId());
        
        // Verify found scheduled executions
        assertEquals(2, macro1Scheduled.size());
        
        // Find scheduled executions for macro2
        List<ScheduledExecution> macro2Scheduled = repository.findScheduledExecutionsByMacroId(savedMacro2.getId());
        
        // Verify found scheduled executions
        assertEquals(1, macro2Scheduled.size());
        assertEquals(5, macro2Scheduled.get(0).getMaxExecutions());
        
        // Test non-existent macro ID
        List<ScheduledExecution> nonExistentScheduled = repository.findScheduledExecutionsByMacroId(UUID.randomUUID().toString());
        assertTrue(nonExistentScheduled.isEmpty());
        
        // Test null macro ID
        List<ScheduledExecution> nullIdScheduled = repository.findScheduledExecutionsByMacroId(null);
        assertTrue(nullIdScheduled.isEmpty());
    }

    @Test
    void testDeleteScheduledExecution() {
        // Create macro
        MacroDefinition savedMacro = repository.create(macro1);
        
        // Set macro ID for scheduled execution
        scheduledExecution1.setMacroId(savedMacro.getId());
        
        // Save scheduled execution
        ScheduledExecution savedScheduled = repository.saveScheduledExecution(scheduledExecution1);
        
        // Verify saved
        List<ScheduledExecution> allScheduled = repository.findAllScheduledExecutions();
        assertEquals(1, allScheduled.size());
        
        // Delete scheduled execution
        boolean deleted = repository.deleteScheduledExecution(savedScheduled.getId());
        
        // Verify deletion was successful
        assertTrue(deleted);
        
        // Verify no scheduled executions remain
        allScheduled = repository.findAllScheduledExecutions();
        assertTrue(allScheduled.isEmpty());
    }

    @Test
    void testDeleteScheduledExecutionsByMacroId() {
        // Create macros
        MacroDefinition savedMacro1 = repository.create(macro1);
        MacroDefinition savedMacro2 = repository.create(macro2);
        
        // Set macro IDs for scheduled executions
        scheduledExecution1.setMacroId(savedMacro1.getId());
        scheduledExecution2.setMacroId(savedMacro2.getId());
        
        // Save scheduled executions
        repository.saveScheduledExecution(scheduledExecution1);
        repository.saveScheduledExecution(scheduledExecution2);
        
        // Save another scheduled execution for macro1
        ScheduledExecution anotherScheduled = new ScheduledExecution(savedMacro1.getId(), LocalDateTime.now().plusHours(2));
        repository.saveScheduledExecution(anotherScheduled);
        
        // Verify all saved
        assertEquals(3, repository.findAllScheduledExecutions().size());
        
        // Delete scheduled executions for macro1
        int deletedCount = repository.deleteScheduledExecutionsByMacroId(savedMacro1.getId());
        
        // Verify deletion count
        assertEquals(2, deletedCount);
        
        // Verify only macro2's scheduled execution remains
        List<ScheduledExecution> remaining = repository.findAllScheduledExecutions();
        assertEquals(1, remaining.size());
        assertEquals(savedMacro2.getId(), remaining.get(0).getMacroId());
        
        // Test non-existent macro ID
        int nonExistentCount = repository.deleteScheduledExecutionsByMacroId(UUID.randomUUID().toString());
        assertEquals(0, nonExistentCount);
        
        // Test null macro ID
        int nullIdCount = repository.deleteScheduledExecutionsByMacroId(null);
        assertEquals(0, nullIdCount);
    }

    @Test
    void testDeleteMacroDeletesScheduledExecutions() {
        // Create macro
        MacroDefinition savedMacro = repository.create(macro1);
        
        // Set macro ID for scheduled execution
        scheduledExecution1.setMacroId(savedMacro.getId());
        
        // Save scheduled execution
        repository.saveScheduledExecution(scheduledExecution1);
        
        // Verify saved
        assertEquals(1, repository.findAllScheduledExecutions().size());
        
        // Delete macro
        boolean deleted = repository.delete(savedMacro.getId());
        
        // Verify deletion was successful
        assertTrue(deleted);
        
        // Verify scheduled executions were also deleted
        List<ScheduledExecution> remaining = repository.findAllScheduledExecutions();
        assertTrue(remaining.isEmpty());
    }
}