package org.rinna.domain.model.macro;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a scheduled execution of a macro.
 */
public class ScheduledExecution {
    private String id;                    // Unique scheduled execution ID
    private String macroId;               // ID of the macro to execute
    private LocalDateTime scheduledTime;  // When to execute the macro
    private Integer executionCount;       // Number of times the macro has been executed (for recurring schedules)
    private Integer maxExecutions;        // Maximum number of executions (for recurring schedules)
    private boolean active;               // Whether the scheduled execution is active

    /**
     * Default constructor.
     */
    public ScheduledExecution() {
        this.id = UUID.randomUUID().toString();
        this.executionCount = 0;
        this.active = true;
    }

    /**
     * Constructor with essential fields.
     *
     * @param macroId the macro ID
     * @param scheduledTime the scheduled execution time
     */
    public ScheduledExecution(String macroId, LocalDateTime scheduledTime) {
        this();
        this.macroId = macroId;
        this.scheduledTime = scheduledTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMacroId() {
        return macroId;
    }

    public void setMacroId(String macroId) {
        this.macroId = macroId;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Integer getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Integer executionCount) {
        this.executionCount = executionCount != null ? executionCount : 0;
    }

    /**
     * Increments the execution count.
     */
    public void incrementExecutionCount() {
        this.executionCount++;
    }

    public Integer getMaxExecutions() {
        return maxExecutions;
    }

    public void setMaxExecutions(Integer maxExecutions) {
        this.maxExecutions = maxExecutions;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Checks if this scheduled execution is due to execute.
     *
     * @param currentTime the current time
     * @return true if the execution is due
     */
    public boolean isDue(LocalDateTime currentTime) {
        if (!active) {
            return false;
        }
        
        if (maxExecutions != null && executionCount >= maxExecutions) {
            return false;
        }
        
        return scheduledTime != null && !currentTime.isBefore(scheduledTime);
    }

    /**
     * Checks if this scheduled execution has reached its maximum execution count.
     *
     * @return true if the maximum execution count has been reached
     */
    public boolean hasReachedMaxExecutions() {
        return maxExecutions != null && executionCount >= maxExecutions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledExecution that = (ScheduledExecution) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ScheduledExecution{" +
                "id='" + id + '\'' +
                ", macroId='" + macroId + '\'' +
                ", scheduledTime=" + scheduledTime +
                ", active=" + active +
                ", executionCount=" + executionCount +
                (maxExecutions != null ? ", maxExecutions=" + maxExecutions : "") +
                '}';
    }
}