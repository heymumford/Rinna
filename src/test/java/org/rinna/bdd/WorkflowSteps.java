/*
 * BDD step definitions for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.usecase.InvalidTransitionException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for workflow-related Cucumber scenarios.
 */
public class WorkflowSteps {
    private final TestContext context;
    
    /**
     * Constructs a new WorkflowSteps with the given test context.
     *
     * @param context the test context
     */
    public WorkflowSteps(TestContext context) {
        this.context = context;
    }
    
    @Given("the Rinna system is initialized")
    public void theRinnaSystemIsInitialized() {
        // This step is handled by the TestContext constructor
        assertNotNull(context.getRinna());
    }
    
    @Given("a new {string} work item with title {string}")
    public void aNewWorkItemWithTitle(String type, String title) {
        WorkItemType workItemType = WorkItemType.valueOf(type.toUpperCase());
        
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .type(workItemType)
                .build();
        
        context.saveCreateRequest("current", request);
    }
    
    @Given("the work item has description {string}")
    public void theWorkItemHasDescription(String description) {
        WorkItemCreateRequest request = context.getCreateRequest("current");
        WorkItemCreateRequest updatedRequest = new WorkItemCreateRequest.Builder()
                .title(request.title())
                .description(description)
                .type(request.type())
                .priority(request.priority())
                .assignee(request.assignee())
                .parentId(request.getParentId().orElse(null))
                .build();
        
        context.saveCreateRequest("current", updatedRequest);
    }
    
    @Given("the work item has priority {string}")
    public void theWorkItemHasPriority(String priority) {
        WorkItemCreateRequest request = context.getCreateRequest("current");
        Priority priorityEnum = Priority.valueOf(priority.toUpperCase());
        
        WorkItemCreateRequest updatedRequest = new WorkItemCreateRequest.Builder()
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .priority(priorityEnum)
                .assignee(request.assignee())
                .parentId(request.getParentId().orElse(null))
                .build();
        
        context.saveCreateRequest("current", updatedRequest);
    }
    
    @Given("the work item is assigned to {string}")
    public void theWorkItemIsAssignedTo(String assignee) {
        WorkItemCreateRequest request = context.getCreateRequest("current");
        
        WorkItemCreateRequest updatedRequest = new WorkItemCreateRequest.Builder()
                .title(request.title())
                .description(request.description())
                .type(request.type())
                .priority(request.priority())
                .assignee(assignee)
                .parentId(request.getParentId().orElse(null))
                .build();
        
        context.saveCreateRequest("current", updatedRequest);
    }
    
    @When("I create the work item")
    public void iCreateTheWorkItem() {
        WorkItemCreateRequest request = context.getCreateRequest("current");
        WorkItem workItem = context.getRinna().items().create(request);
        context.saveWorkItem("current", workItem);
    }
    
    @Then("the work item should be created successfully")
    public void theWorkItemShouldBeCreatedSuccessfully() {
        WorkItem workItem = context.getWorkItem("current");
        assertNotNull(workItem);
        assertNotNull(workItem.getId());
    }
    
    @Then("the work item should have title {string}")
    public void theWorkItemShouldHaveTitle(String title) {
        WorkItem workItem = context.getWorkItem("current");
        assertEquals(title, workItem.getTitle());
    }
    
    @Then("the work item should have description {string}")
    public void theWorkItemShouldHaveDescription(String description) {
        WorkItem workItem = context.getWorkItem("current");
        assertEquals(description, workItem.getDescription());
    }
    
    @Then("the work item should be a {string}")
    public void theWorkItemShouldBeA(String type) {
        WorkItem workItem = context.getWorkItem("current");
        assertEquals(WorkItemType.valueOf(type.toUpperCase()), workItem.getType());
    }
    
    @Then("the work item should have type {string}")
    public void theWorkItemShouldHaveType(String type) {
        WorkItem workItem = context.getWorkItem("current");
        assertEquals(WorkItemType.valueOf(type.toUpperCase()), workItem.getType());
    }
    
    @Then("the work item should have priority {string}")
    public void theWorkItemShouldHavePriority(String priority) {
        WorkItem workItem = context.getWorkItem("current");
        assertEquals(Priority.valueOf(priority.toUpperCase()), workItem.getPriority());
    }
    
    @Then("the work item should be assigned to {string}")
    public void theWorkItemShouldBeAssignedTo(String assignee) {
        WorkItem workItem = context.getWorkItem("current");
        assertEquals(assignee, workItem.getAssignee());
    }
    
    @Then("the work item should be in {string} state")
    public void theWorkItemShouldBeInState(String state) {
        WorkItem workItem = context.getWorkItem("current");
        assertEquals(WorkflowState.valueOf(state.toUpperCase()), workItem.getStatus());
    }
    
    @Given("a work item in {string} state")
    public void aWorkItemInState(String state) {
        // Create a work item
        aNewWorkItemWithTitle("FEATURE", "Test Feature");
        iCreateTheWorkItem();
        
        // If the state is not FOUND, transition to the target state
        if (!state.equalsIgnoreCase("FOUND")) {
            try {
                // First transition to TRIAGED
                if (!state.equalsIgnoreCase("TRIAGED")) {
                    WorkItem workItem = context.getWorkItem("current");
                    UUID id = workItem.getId();
                    context.getRinna().workflow().transition(id, WorkflowState.TRIAGED);
                }
                
                // Then transition to TO_DO if needed
                if (state.equalsIgnoreCase("TO_DO") || state.equalsIgnoreCase("IN_PROGRESS") || 
                        state.equalsIgnoreCase("IN_TEST") || state.equalsIgnoreCase("DONE")) {
                    WorkItem workItem = context.getWorkItem("current");
                    UUID id = workItem.getId();
                    context.getRinna().workflow().transition(id, WorkflowState.TO_DO);
                }
                
                // Then transition to IN_PROGRESS if needed
                if (state.equalsIgnoreCase("IN_PROGRESS") || state.equalsIgnoreCase("IN_TEST") || 
                        state.equalsIgnoreCase("DONE")) {
                    WorkItem workItem = context.getWorkItem("current");
                    UUID id = workItem.getId();
                    context.getRinna().workflow().transition(id, WorkflowState.IN_PROGRESS);
                }
                
                // Then transition to IN_TEST if needed
                if (state.equalsIgnoreCase("IN_TEST") || state.equalsIgnoreCase("DONE")) {
                    WorkItem workItem = context.getWorkItem("current");
                    UUID id = workItem.getId();
                    context.getRinna().workflow().transition(id, WorkflowState.IN_TEST);
                }
                
                // Finally transition to DONE if needed
                if (state.equalsIgnoreCase("DONE")) {
                    WorkItem workItem = context.getWorkItem("current");
                    UUID id = workItem.getId();
                    context.getRinna().workflow().transition(id, WorkflowState.DONE);
                }
                
                // Refresh the work item
                UUID id = context.getWorkItem("current").getId();
                Optional<WorkItem> updatedItem = context.getRinna().items().findById(id);
                if (updatedItem.isPresent()) {
                    context.saveWorkItem("current", updatedItem.get());
                }
            } catch (InvalidTransitionException e) {
                context.setException(e);
            }
        }
    }
    
    @When("I transition the work item to {string} state")
    public void iTransitionTheWorkItemToState(String state) {
        WorkItem workItem = context.getWorkItem("current");
        UUID id = workItem.getId();
        WorkflowState targetState = WorkflowState.valueOf(state.toUpperCase());
        
        try {
            WorkItem updatedItem = context.getRinna().workflow().transition(id, targetState);
            context.saveWorkItem("current", updatedItem);
        } catch (InvalidTransitionException e) {
            context.setException(e);
        }
    }
    
    @Then("the transition should succeed")
    public void theTransitionShouldSucceed() {
        assertNull(context.getException());
    }
    
    @Then("the transition should fail")
    public void theTransitionShouldFail() {
        assertNotNull(context.getException());
        assertTrue(context.getException() instanceof InvalidTransitionException);
    }
    
    @Then("the available transitions should include {string}")
    public void theAvailableTransitionsShouldInclude(String state) {
        WorkItem workItem = context.getWorkItem("current");
        UUID id = workItem.getId();
        WorkflowState targetState = WorkflowState.valueOf(state.toUpperCase());
        
        List<WorkflowState> availableTransitions = context.getRinna().workflow().getAvailableTransitions(id);
        assertTrue(availableTransitions.contains(targetState));
    }
    
    @Then("the available transitions should not include {string}")
    public void theAvailableTransitionsShouldNotInclude(String state) {
        WorkItem workItem = context.getWorkItem("current");
        UUID id = workItem.getId();
        WorkflowState targetState = WorkflowState.valueOf(state.toUpperCase());
        
        List<WorkflowState> availableTransitions = context.getRinna().workflow().getAvailableTransitions(id);
        assertFalse(availableTransitions.contains(targetState));
    }
    
    @When("the developer creates a new Bug with title {string}")
    public void theDeveloperCreatesANewBugWithTitle(String title) {
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .type(WorkItemType.BUG)
                .build();
        
        WorkItem workItem = context.getRinna().items().create(request);
        context.saveWorkItem("current", workItem);
    }
    
    @Then("the Bug should exist with status {string} and priority {string}")
    public void theBugShouldExistWithStatusAndPriority(String status, String priority) {
        WorkItem workItem = context.getWorkItem("current");
        assertNotNull(workItem);
        assertEquals(WorkItemType.BUG, workItem.getType());
        assertEquals(WorkflowState.valueOf(status.toUpperCase()), workItem.getStatus());
        assertEquals(Priority.valueOf(priority.toUpperCase()), workItem.getPriority());
    }
    
    @Given("a Bug titled {string} exists")
    public void aBugTitledExists(String title) {
        theDeveloperCreatesANewBugWithTitle(title);
    }
    
    @When("the developer updates the Bug status to {string}")
    public void theDeveloperUpdatesTheBugStatusTo(String status) {
        iTransitionTheWorkItemToState(status);
    }
    
    @Then("the Bug's status should be {string}")
    public void theBugStatusShouldBe(String status) {
        theWorkItemShouldBeInState(status);
    }
    
    @When("the developer attempts an invalid status transition to {string}")
    public void theDeveloperAttemptsAnInvalidStatusTransitionTo(String status) {
        iTransitionTheWorkItemToState(status);
    }
    
    @Then("the system should explicitly reject the transition")
    public void theSystemShouldExplicitlyRejectTheTransition() {
        theTransitionShouldFail();
    }
}