package org.rinna.domain.model.macro;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Defines what a macro should do when triggered by specifying an action type
 * and configuration parameters.
 */
public class MacroAction {
    private String id;                         // Unique identifier
    private ActionType type;                     // Type of action
    private Map<String, Object> configuration;   // Action-specific configuration
    private Integer order;                       // Execution order
    private MacroCondition condition;            // Optional conditional execution
    private WebhookConfig webhookConfig;         // Configuration for webhook actions

    /**
     * Default constructor.
     */
    public MacroAction() {
        this.id = UUID.randomUUID().toString();
        this.configuration = new HashMap<>();
    }

    /**
     * Constructor with type.
     *
     * @param type the action type
     */
    public MacroAction(ActionType type) {
        this();
        this.type = type;
    }

    /**
     * Constructor with type and order.
     *
     * @param type the action type
     * @param order the execution order
     */
    public MacroAction(ActionType type, Integer order) {
        this(type);
        this.order = order;
    }

    /**
     * Constructor with type, configuration, and order.
     *
     * @param type the action type
     * @param configuration the action configuration
     * @param order the execution order
     */
    public MacroAction(ActionType type, Map<String, Object> configuration, Integer order) {
        this(type, order);
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

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }
    
    public WebhookConfig getWebhookConfig() {
        return webhookConfig;
    }

    public void setWebhookConfig(WebhookConfig webhookConfig) {
        this.webhookConfig = webhookConfig;
    }
    
    /**
     * Checks if this is a webhook action.
     *
     * @return true if this is a webhook action
     */
    public boolean isWebhookAction() {
        return type != null && type.isWebhookAction();
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
     * Gets a configuration value as a string.
     *
     * @param key the configuration key
     * @return the value as a string, or null if not found
     */
    public String getConfigValueAsString(String key) {
        Object value = getConfigValue(key);
        return value != null ? value.toString() : null;
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

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public MacroCondition getCondition() {
        return condition;
    }

    public void setCondition(MacroCondition condition) {
        this.condition = condition;
    }

    /**
     * Checks if this action has a condition.
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
        MacroAction that = (MacroAction) o;
        return Objects.equals(id, that.id) &&
               type == that.type &&
               Objects.equals(order, that.order) &&
               Objects.equals(configuration, that.configuration) &&
               Objects.equals(condition, that.condition) &&
               Objects.equals(webhookConfig, that.webhookConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, configuration, order, condition, webhookConfig);
    }
    
    /**
     * Creates a webhook action with the specified type and configuration.
     *
     * @param actionType the specific webhook action type
     * @param url the webhook URL
     * @param config the webhook configuration
     * @return a new MacroAction for webhooks
     */
    public static MacroAction forWebhook(ActionType actionType, String url, WebhookConfig config) {
        if (actionType == null || !actionType.isWebhookAction()) {
            actionType = ActionType.CALL_WEBHOOK;
        }
        
        MacroAction action = new MacroAction(actionType);
        action.setConfigValue("url", url);
        action.setWebhookConfig(config);
        return action;
    }

    @Override
    public String toString() {
        return "MacroAction{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", order=" + order +
                ", hasCondition=" + hasCondition() +
                ", isWebhook=" + isWebhookAction() +
                '}';
    }
}