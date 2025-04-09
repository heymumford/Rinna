package org.rinna.domain.model.macro;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Defines when a macro should be executed by specifying a trigger type
 * and optional configuration parameters and conditions.
 */
public class MacroTrigger {
    private String id;                           // Unique identifier
    private TriggerType type;                      // Type of trigger
    private Map<String, Object> configuration;     // Trigger-specific configuration
    private MacroCondition condition;              // Optional filtering condition
    private WebhookConfig webhookConfig;           // Configuration for webhook triggers

    /**
     * Default constructor.
     */
    public MacroTrigger() {
        this.id = UUID.randomUUID().toString();
        this.configuration = new HashMap<>();
    }

    /**
     * Constructor with type.
     *
     * @param type the trigger type
     */
    public MacroTrigger(TriggerType type) {
        this();
        this.type = type;
    }

    /**
     * Constructor with type and configuration.
     *
     * @param type the trigger type
     * @param configuration the trigger configuration
     */
    public MacroTrigger(TriggerType type, Map<String, Object> configuration) {
        this(type);
        if (configuration != null) {
            this.configuration = new HashMap<>(configuration);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TriggerType getType() {
        return type;
    }

    public void setType(TriggerType type) {
        this.type = type;
    }
    
    public WebhookConfig getWebhookConfig() {
        return webhookConfig;
    }

    public void setWebhookConfig(WebhookConfig webhookConfig) {
        this.webhookConfig = webhookConfig;
    }
    
    /**
     * Checks if this is a webhook trigger.
     *
     * @return true if this is a webhook trigger
     */
    public boolean isWebhookTrigger() {
        return type != null && type.isWebhookTrigger();
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration != null ? configuration : new HashMap<>();
    }

    /**
     * Gets a configuration value by key.
     *
     * @param key the configuration key
     * @return the value, or null if not found
     */
    public Object getConfigValue(String key) {
        return configuration.get(key);
    }

    /**
     * Sets a configuration value.
     *
     * @param key the configuration key
     * @param value the value to set
     */
    public void setConfigValue(String key, Object value) {
        if (key != null && value != null) {
            this.configuration.put(key, value);
        }
    }

    public MacroCondition getCondition() {
        return condition;
    }

    public void setCondition(MacroCondition condition) {
        this.condition = condition;
    }

    /**
     * Checks if this trigger has a condition.
     *
     * @return true if a condition is set
     */
    public boolean hasCondition() {
        return condition != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroTrigger that = (MacroTrigger) o;
        return Objects.equals(id, that.id) &&
               type == that.type &&
               Objects.equals(configuration, that.configuration) &&
               Objects.equals(condition, that.condition) &&
               Objects.equals(webhookConfig, that.webhookConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, configuration, condition, webhookConfig);
    }
    
    /**
     * Creates a webhook trigger with the specified webhook configuration.
     *
     * @param triggerType the specific webhook trigger type
     * @param config the webhook configuration
     * @return a new MacroTrigger for webhooks
     */
    public static MacroTrigger forWebhook(TriggerType triggerType, WebhookConfig config) {
        if (triggerType == null || !triggerType.isWebhookTrigger()) {
            triggerType = TriggerType.WEBHOOK;
        }
        
        MacroTrigger trigger = new MacroTrigger(triggerType);
        trigger.setWebhookConfig(config);
        return trigger;
    }

    @Override
    public String toString() {
        return "MacroTrigger{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", hasCondition=" + hasCondition() +
                ", isWebhook=" + isWebhookTrigger() +
                '}';
    }
}