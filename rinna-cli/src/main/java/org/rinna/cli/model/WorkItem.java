package org.rinna.cli.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model class representing a work item in the CLI.
 */
public class WorkItem {
    private String id;
    private String title;
    private String description;
    private WorkItemType type;
    private Priority priority;
    private WorkflowState status; // Using status name for compatibility with AdminCommands
    private String assignee;
    private String reporter;
    private String project;
    private LocalDateTime created;
    private LocalDateTime updated;
    private LocalDate dueDate;
    private String version;

    /**
     * Default constructor for a new, empty work item.
     */
    public WorkItem() {
        // Initialize a new, empty work item
    }

    /**
     * Constructor for creating a work item with basic fields.
     *
     * @param id The unique identifier of the work item
     * @param title The title of the work item
     * @param type The type of work item (e.g., TASK, BUG, FEATURE)
     * @param priority The priority level of the work item
     * @param status The current workflow status of the work item
     */
    public WorkItem(String id, String title, WorkItemType type, Priority priority, WorkflowState status) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.created = LocalDateTime.now();
        this.updated = LocalDateTime.now();
    }
    
    /**
     * Constructor that accepts a UUID for the ID.
     *
     * @param id The unique identifier as a UUID
     * @param title The title of the work item
     * @param type The type of work item
     * @param priority The priority level
     * @param status The workflow state
     */
    public WorkItem(UUID id, String title, WorkItemType type, Priority priority, WorkflowState status) {
        this(id.toString(), title, type, priority, status);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public void setId(UUID id) {
        this.id = id.toString();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkItemType getType() {
        return type;
    }

    public void setType(WorkItemType type) {
        this.type = type;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public WorkflowState getStatus() {
        return status;
    }

    public void setStatus(WorkflowState status) {
        this.status = status;
        this.updated = LocalDateTime.now();
    }
    
    // For backward compatibility
    public WorkflowState getState() {
        return status;
    }

    public void setState(WorkflowState state) {
        this.status = state;
        this.updated = LocalDateTime.now();
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getReporter() {
        return reporter;
    }
    
    public void setReporter(String reporter) {
        this.reporter = reporter;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getProjectId() {
        return project;
    }
    
    public void setProjectId(String projectId) {
        this.project = projectId;
    }
    
    /**
     * Checks if this work item is visible to the specified user.
     * This is a simple implementation that just checks if the item exists.
     *
     * @param user the user to check visibility for
     * @return true if the item is visible to the user, false otherwise
     */
    public boolean isVisible(String user) {
        // In a real implementation, this would check permissions and visibility rules
        // For now, all items are visible to all users
        return true;
    }

    @Override
    public String toString() {
        return "WorkItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", priority=" + priority +
                ", status=" + status +
                ", assignee='" + assignee + '\'' +
                ", project='" + project + '\'' +
                ", dueDate=" + dueDate +
                '}';
    }
}
