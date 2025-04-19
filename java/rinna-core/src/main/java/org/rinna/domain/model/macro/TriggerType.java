package org.rinna.domain.model.macro;

/**
 * Defines the different types of triggers that can initiate a macro execution.
 */
public enum TriggerType {
    // Static triggers
    MANUAL("Manual execution"),                // User-initiated execution
    SCHEDULED("Scheduled execution"),          // Time-based execution
    
    // Dynamic work item triggers
    ITEM_CREATED("Item created"),              // When a new work item is created
    ITEM_UPDATED("Item updated"),              // When a work item is updated
    ITEM_TRANSITIONED("Item state changed"),   // When a work item changes state
    COMMENT_ADDED("Comment added"),            // When a comment is added to a work item
    FIELD_CHANGED("Field changed"),            // When a specific field is modified
    
    // System triggers
    SYSTEM_STARTUP("System startup"),          // When the system starts
    USER_LOGIN("User login"),                  // When a user logs in
    INTEGRATION_EVENT("Integration event"),    // When an external integration event occurs
    
    // Webhook triggers
    WEBHOOK("Webhook"),                        // When a webhook is received
    WEBHOOK_JSON("JSON Webhook"),              // When a JSON webhook is received
    WEBHOOK_FORM("Form Webhook"),              // When a form webhook is received
    
    // Composite triggers
    CONDITION_GROUP("Condition group");        // Logical group of conditions

    private final String displayName;

    TriggerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this trigger type is a static trigger.
     *
     * @return true if this is a static trigger
     */
    public boolean isStaticTrigger() {
        return this == MANUAL || this == SCHEDULED;
    }

    /**
     * Checks if this trigger type is a work item related trigger.
     *
     * @return true if this is a work item related trigger
     */
    public boolean isWorkItemTrigger() {
        return this == ITEM_CREATED || this == ITEM_UPDATED || 
               this == ITEM_TRANSITIONED || this == COMMENT_ADDED || 
               this == FIELD_CHANGED;
    }

    /**
     * Checks if this trigger type is a system trigger.
     *
     * @return true if this is a system trigger
     */
    public boolean isSystemTrigger() {
        return this == SYSTEM_STARTUP || this == USER_LOGIN || 
               this == INTEGRATION_EVENT;
    }
    
    /**
     * Checks if this trigger type is a webhook trigger.
     *
     * @return true if this is a webhook trigger
     */
    public boolean isWebhookTrigger() {
        return this == WEBHOOK || this == WEBHOOK_JSON || this == WEBHOOK_FORM;
    }

    /**
     * Checks if this trigger type is a composite trigger.
     *
     * @return true if this is a composite trigger
     */
    public boolean isCompositeTrigger() {
        return this == CONDITION_GROUP;
    }
}