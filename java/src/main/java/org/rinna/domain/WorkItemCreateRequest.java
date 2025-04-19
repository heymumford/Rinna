package org.rinna.domain;


/**
 * Compatibility class for WorkItemCreateRequest that delegates to the new class.
 * @deprecated Use org.rinna.domain.model.WorkItemCreateRequest instead.
 */
@Deprecated(forRemoval = true)
public class WorkItemCreateRequest {
    private final String title;
    private final String description;
    private final Priority priority;
    private final WorkItemType type;

    /**
     * Constructor for compatibility with old code.
     */
    public WorkItemCreateRequest(String title, String description, Priority priority, WorkItemType type) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.type = type;
    }

    /**
     * Converts to the new request type.
     */
    public org.rinna.domain.model.WorkItemCreateRequest toNewRequest() {
        return new org.rinna.domain.model.WorkItemCreateRequest.Builder()
                .title(title)
                .description(description)
                .priority(priority.toNewPriority())
                .type(type.toNewType())
                .build();
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public WorkItemType getType() {
        return type;
    }

    public static class Builder {
        private String title;
        private String description;
        private Priority priority;
        private WorkItemType type;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder type(WorkItemType type) {
            this.type = type;
            return this;
        }

        public WorkItemCreateRequest build() {
            return new WorkItemCreateRequest(title, description, priority, type);
        }
    }
}
