package org.rinna.domain.model.macro;

/**
 * Defines the different types of actions that can be performed by a macro.
 */
public enum ActionType {
    // Work item actions
    CREATE_WORK_ITEM("Create work item"),     // Create a new work item
    UPDATE_WORK_ITEM("Update work item"),     // Update an existing work item
    TRANSITION_WORK_ITEM("Change work item state"), // Change work item state
    ADD_COMMENT("Add comment"),               // Add a comment to a work item
    ADD_RELATIONSHIP("Add relationship"),     // Create a relationship between work items
    
    // System actions
    SEND_NOTIFICATION("Send notification"),   // Send a notification
    CALL_WEBHOOK("Call webhook"),             // Make an HTTP call to an external system
    SEND_WEBHOOK_JSON("Send JSON webhook"),   // Send a JSON payload to a webhook
    SEND_WEBHOOK_FORM("Send form webhook"),   // Send a form data to a webhook
    EXECUTE_COMMAND("Execute command"),       // Run a system command
    
    // Flow control
    CONDITION("Conditional branch"),          // Conditional branching
    LOOP("Loop"),                             // Repeat actions
    DELAY("Delay execution"),                 // Pause execution
    USER_PROMPT("User prompt");               // Request user input

    private final String displayName;

    ActionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this action type is a work item action.
     *
     * @return true if this is a work item action
     */
    public boolean isWorkItemAction() {
        return this == CREATE_WORK_ITEM || this == UPDATE_WORK_ITEM || 
               this == TRANSITION_WORK_ITEM || this == ADD_COMMENT ||
               this == ADD_RELATIONSHIP;
    }

    /**
     * Checks if this action type is a system action.
     *
     * @return true if this is a system action
     */
    public boolean isSystemAction() {
        return this == SEND_NOTIFICATION || this == CALL_WEBHOOK || 
               this == SEND_WEBHOOK_JSON || this == SEND_WEBHOOK_FORM ||
               this == EXECUTE_COMMAND;
    }
    
    /**
     * Checks if this action type is a webhook action.
     *
     * @return true if this is a webhook action
     */
    public boolean isWebhookAction() {
        return this == CALL_WEBHOOK || this == SEND_WEBHOOK_JSON || this == SEND_WEBHOOK_FORM;
    }

    /**
     * Checks if this action type is a flow control action.
     *
     * @return true if this is a flow control action
     */
    public boolean isFlowControlAction() {
        return this == CONDITION || this == LOOP || this == DELAY || 
               this == USER_PROMPT;
    }

    /**
     * Checks if this action requires user interaction.
     *
     * @return true if this action requires user interaction
     */
    public boolean requiresUserInteraction() {
        return this == USER_PROMPT;
    }
}