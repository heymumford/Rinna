package org.rinna.domain.model.macro;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The core entity representing a complete macro automation.
 * A MacroDefinition contains all information needed to determine
 * when and how a macro should be executed.
 */
public class MacroDefinition {
    private String id;                       // Unique identifier
    private String name;                     // User-friendly name
    private String description;              // Detailed description
    private MacroTrigger trigger;            // When the macro should execute
    private List<MacroAction> actions;       // What the macro should do
    private Map<String, String> parameters;  // Configurable values
    private MacroSchedule schedule;          // For time-based execution
    private boolean enabled;                 // Active status
    private String owner;                    // Creator/owner
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MacroExecution> recentExecutions;

    /**
     * Default constructor.
     */
    public MacroDefinition() {
        this.actions = new ArrayList<>();
        this.parameters = new HashMap<>();
        this.recentExecutions = new ArrayList<>();
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructor with essential fields.
     *
     * @param id the unique identifier
     * @param name the user-friendly name
     * @param description the detailed description
     * @param trigger the macro trigger
     */
    public MacroDefinition(String id, String name, String description, MacroTrigger trigger) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.trigger = trigger;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MacroTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(MacroTrigger trigger) {
        this.trigger = trigger;
    }

    public List<MacroAction> getActions() {
        return actions;
    }

    public void setActions(List<MacroAction> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public void addAction(MacroAction action) {
        if (action != null) {
            this.actions.add(action);
        }
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public void setParameter(String key, String value) {
        if (key != null && value != null) {
            this.parameters.put(key, value);
        }
    }

    public MacroSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(MacroSchedule schedule) {
        this.schedule = schedule;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<MacroExecution> getRecentExecutions() {
        return recentExecutions;
    }

    public void setRecentExecutions(List<MacroExecution> recentExecutions) {
        this.recentExecutions = recentExecutions != null ? recentExecutions : new ArrayList<>();
    }

    public void addExecution(MacroExecution execution) {
        if (execution != null) {
            this.recentExecutions.add(execution);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroDefinition that = (MacroDefinition) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MacroDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                ", actions=" + actions.size() +
                '}';
    }
}