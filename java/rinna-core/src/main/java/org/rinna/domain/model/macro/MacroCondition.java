package org.rinna.domain.model.macro;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a condition that can be used to filter trigger events
 * or conditionally execute actions.
 */
public class MacroCondition {
    private ConditionType type;                // Type of condition
    private String field;                      // Field to check (for FIELD_EQUALS, etc.)
    private Object value;                      // Value to compare against
    private List<MacroCondition> subConditions; // For AND/OR conditions
    
    /**
     * Enum representing the different types of conditions.
     */
    public enum ConditionType {
        FIELD_EQUALS,        // Field equals a specific value
        FIELD_NOT_EQUALS,    // Field does not equal a specific value
        FIELD_CONTAINS,      // Field contains a specific value
        FIELD_MATCHES,       // Field matches a regex pattern
        FIELD_GREATER_THAN,  // Field is greater than a specific value
        FIELD_LESS_THAN,     // Field is less than a specific value
        AND,                // All subconditions must be true
        OR,                 // At least one subcondition must be true
        NOT                 // Negates the subcondition
    }
    
    /**
     * Default constructor.
     */
    public MacroCondition() {
        this.subConditions = new ArrayList<>();
    }
    
    /**
     * Constructor for field-based conditions.
     *
     * @param type the condition type
     * @param field the field to check
     * @param value the value to compare against
     */
    public MacroCondition(ConditionType type, String field, Object value) {
        this();
        this.type = type;
        this.field = field;
        this.value = value;
    }
    
    /**
     * Constructor for logical conditions (AND, OR, NOT).
     *
     * @param type the condition type
     * @param subConditions the subconditions
     */
    public MacroCondition(ConditionType type, List<MacroCondition> subConditions) {
        this();
        this.type = type;
        if (subConditions != null) {
            this.subConditions.addAll(subConditions);
        }
    }
    
    public ConditionType getType() {
        return type;
    }
    
    public void setType(ConditionType type) {
        this.type = type;
    }
    
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public List<MacroCondition> getSubConditions() {
        return subConditions;
    }
    
    public void setSubConditions(List<MacroCondition> subConditions) {
        this.subConditions = subConditions != null ? subConditions : new ArrayList<>();
    }
    
    /**
     * Adds a subcondition.
     *
     * @param condition the condition to add
     */
    public void addSubCondition(MacroCondition condition) {
        if (condition != null) {
            this.subConditions.add(condition);
        }
    }
    
    /**
     * Checks if this is a logical condition (AND, OR, NOT).
     *
     * @return true if this is a logical condition
     */
    public boolean isLogicalCondition() {
        return type == ConditionType.AND || type == ConditionType.OR || type == ConditionType.NOT;
    }
    
    /**
     * Checks if this is a field condition.
     *
     * @return true if this is a field condition
     */
    public boolean isFieldCondition() {
        return !isLogicalCondition();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroCondition that = (MacroCondition) o;
        return type == that.type &&
                Objects.equals(field, that.field) &&
                Objects.equals(value, that.value) &&
                Objects.equals(subConditions, that.subConditions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, field, value, subConditions);
    }
    
    @Override
    public String toString() {
        if (isLogicalCondition()) {
            return "MacroCondition{" +
                    "type=" + type +
                    ", subConditions=" + subConditions.size() +
                    '}';
        } else {
            return "MacroCondition{" +
                    "type=" + type +
                    ", field='" + field + '\'' +
                    ", value=" + value +
                    '}';
        }
    }
}