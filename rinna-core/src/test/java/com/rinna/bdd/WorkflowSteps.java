package com.rinna.bdd;

import com.rinna.Rinna;
import com.rinna.model.Priority;
import com.rinna.model.WorkItem;
import com.rinna.model.WorkItemCreateRequest;
import com.rinna.model.WorkItemType;
import com.rinna.model.WorkflowState;
import com.rinna.service.InvalidTransitionException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Step definitions for workflow management scenarios.
 */
public class WorkflowSteps {
    
    private Rinna rinna;
    private WorkItem currentItem;
    private Exception thrownException;
    private final Map<String, WorkItemType> typeMap = new HashMap<>();
    private final Map<String, Priority> priorityMap = new HashMap<>();
    private final Map<String, WorkflowState> stateMap = new HashMap<>();
    
    public WorkflowSteps() {
        // Initialize type mappings
        typeMap.put("Bug", WorkItemType.BUG);
        typeMap.put("Feature", WorkItemType.FEATURE);
        typeMap.put("Goal", WorkItemType.GOAL);
        typeMap.put("Chore", WorkItemType.CHORE);
        
        // Initialize priority mappings
        priorityMap.put("high", Priority.HIGH);
        priorityMap.put("medium", Priority.MEDIUM);
        priorityMap.put("low", Priority.LOW);
        
        // Initialize state mappings
        stateMap.put("Found", WorkflowState.FOUND);
        stateMap.put("Triaged", WorkflowState.TRIAGED);
        stateMap.put("To Do", WorkflowState.TO_DO);
        stateMap.put("In Progress", WorkflowState.IN_PROGRESS);
        stateMap.put("In Test", WorkflowState.IN_TEST);
        stateMap.put("Done", WorkflowState.DONE);
    }
    
    @Given("the Rinna system is initialized")
    public void the_rinna_system_is_initialized() {
        rinna = Rinna.initialize();
    }
    
    @When("the developer creates a new {word} with title {string}")
    public void the_developer_creates_a_new_item_with_title(String itemType, String title) {
        WorkItemType type = typeMap.get(itemType);
        WorkItemCreateRequest request = WorkItemCreateRequest.builder()
                .title(title)
                .type(type)
                .build();
        
        currentItem = rinna.items().create(request);
    }
    
    @Then("the {word} should exist with status {string} and priority {string}")
    public void the_item_should_exist_with_status_and_priority(String itemType, String status, String priority) {
        WorkItemType type = typeMap.get(itemType);
        WorkflowState state = stateMap.get(status);
        Priority priorityEnum = priorityMap.get(priority);
        
        assertThat(currentItem).isNotNull();
        assertThat(currentItem.getType()).isEqualTo(type);
        assertThat(currentItem.getStatus()).isEqualTo(state);
        assertThat(currentItem.getPriority()).isEqualTo(priorityEnum);
    }
    
    @Given("a {word} titled {string} exists")
    public void an_item_titled_exists(String itemType, String title) {
        the_rinna_system_is_initialized();
        the_developer_creates_a_new_item_with_title(itemType, title);
    }
    
    @When("the developer updates the {word} status to {string}")
    public void the_developer_updates_the_item_status_to(String itemType, String status) {
        try {
            currentItem = rinna.workflow().transition(currentItem.getId(), stateMap.get(status));
        } catch (InvalidTransitionException e) {
            thrownException = e;
        }
    }
    
    @Then("the {word}'s status should be {string}")
    public void the_item_s_status_should_be(String itemType, String status) {
        WorkflowState state = stateMap.get(status);
        assertThat(currentItem.getStatus()).isEqualTo(state);
    }
    
    @When("the developer attempts an invalid status transition to {string}")
    public void the_developer_attempts_an_invalid_status_transition_to(String status) {
        try {
            currentItem = rinna.workflow().transition(currentItem.getId(), stateMap.get(status));
        } catch (InvalidTransitionException e) {
            thrownException = e;
        }
    }
    
    @Then("the system should explicitly reject the transition")
    public void the_system_should_explicitly_reject_the_transition() {
        assertThat(thrownException).isInstanceOf(InvalidTransitionException.class);
    }
}