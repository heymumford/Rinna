#\!/bin/bash

# Set the default directory path to the current directory
RINNA_DIR="${RINNA_DIR:-/home/emumford/NativeLinuxProjects/Rinna}"

# Timestamp for logging
timestamp() {
  date
}

echo "Creating compatibility classes at $(timestamp)..."

# Create the org.rinna.domain package for backward compatibility
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/domain"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/service"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/service/impl"
mkdir -p "${RINNA_DIR}/src/main/java/org/rinna/persistence"

# Create WorkItem interface
cat > "${RINNA_DIR}/src/main/java/org/rinna/domain/WorkItem.java" << 'WORKITEM'
package org.rinna.domain;

/**
 * Compatibility interface for WorkItem that extends the new interface.
 * This allows old code to use the new implementation and vice versa.
 * @deprecated Use org.rinna.domain.model.WorkItem instead.
 */
@Deprecated(forRemoval = true)
public interface WorkItem extends org.rinna.domain.model.WorkItem {
    // This interface is intentionally empty as it's just a bridge
    // between old code and new code
}
WORKITEM

# Create WorkItemType enum
cat > "${RINNA_DIR}/src/main/java/org/rinna/domain/WorkItemType.java" << 'WORKITEMTYPE'
package org.rinna.domain;

/**
 * Compatibility class for WorkItemType that delegates to the new enum.
 * @deprecated Use org.rinna.domain.model.WorkItemType instead.
 */
@Deprecated(forRemoval = true)
public enum WorkItemType {
    BUG, FEATURE, CHORE, GOAL;

    public org.rinna.domain.model.WorkItemType toNewType() {
        return org.rinna.domain.model.WorkItemType.valueOf(this.name());
    }

    public static WorkItemType fromNewType(org.rinna.domain.model.WorkItemType newType) {
        return WorkItemType.valueOf(newType.name());
    }
}
WORKITEMTYPE

# Create Priority enum
cat > "${RINNA_DIR}/src/main/java/org/rinna/domain/Priority.java" << 'PRIORITY'
package org.rinna.domain;

/**
 * Compatibility class for Priority that delegates to the new enum.
 * @deprecated Use org.rinna.domain.model.Priority instead.
 */
@Deprecated(forRemoval = true)
public enum Priority {
    CRITICAL, HIGH, MEDIUM, LOW;

    public org.rinna.domain.model.Priority toNewPriority() {
        return org.rinna.domain.model.Priority.valueOf(this.name());
    }

    public static Priority fromNewPriority(org.rinna.domain.model.Priority newPriority) {
        return Priority.valueOf(newPriority.name());
    }
}
PRIORITY

# Create WorkflowState enum
cat > "${RINNA_DIR}/src/main/java/org/rinna/domain/WorkflowState.java" << 'WORKFLOWSTATE'
package org.rinna.domain;

/**
 * Compatibility class for WorkflowState that delegates to the new enum.
 * @deprecated Use org.rinna.domain.model.WorkflowState instead.
 */
@Deprecated(forRemoval = true)
public enum WorkflowState {
    NEW, IN_PROGRESS, TESTING, DONE, BLOCKED;

    public org.rinna.domain.model.WorkflowState toNewState() {
        return org.rinna.domain.model.WorkflowState.valueOf(this.name());
    }

    public static WorkflowState fromNewState(org.rinna.domain.model.WorkflowState newState) {
        return WorkflowState.valueOf(newState.name());
    }
}
WORKFLOWSTATE

# Create WorkItemCreateRequest class
cat > "${RINNA_DIR}/src/main/java/org/rinna/domain/WorkItemCreateRequest.java" << 'WORKITEMCREATEREQUEST'
package org.rinna.domain;

import java.util.UUID;

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
WORKITEMCREATEREQUEST

# Create Release interface
cat > "${RINNA_DIR}/src/main/java/org/rinna/domain/Release.java" << 'RELEASE'
package org.rinna.domain;

/**
 * Compatibility interface for Release that extends the new interface.
 * @deprecated Use org.rinna.domain.model.Release instead.
 */
@Deprecated(forRemoval = true)
public interface Release extends org.rinna.domain.model.Release {
    // This interface is intentionally empty as it's just a bridge
    // between old code and new code
}
RELEASE

# Create compatibility service interfaces
cat > "${RINNA_DIR}/src/main/java/org/rinna/usecase/ItemService.java" << 'ITEMSERVICE'
package org.rinna.usecase;

/**
 * Compatibility interface for ItemService.
 * @deprecated Use org.rinna.domain.service.ItemService instead.
 */
@Deprecated(forRemoval = true)
public interface ItemService extends org.rinna.domain.service.ItemService {
    // This interface is intentionally empty as it's just a bridge
}
ITEMSERVICE

cat > "${RINNA_DIR}/src/main/java/org/rinna/usecase/WorkflowService.java" << 'WORKFLOWSERVICE'
package org.rinna.usecase;

/**
 * Compatibility interface for WorkflowService.
 * @deprecated Use org.rinna.domain.service.WorkflowService instead.
 */
@Deprecated(forRemoval = true)
public interface WorkflowService extends org.rinna.domain.service.WorkflowService {
    // This interface is intentionally empty as it's just a bridge
}
WORKFLOWSERVICE

cat > "${RINNA_DIR}/src/main/java/org/rinna/usecase/ReleaseService.java" << 'RELEASESERVICE'
package org.rinna.usecase;

/**
 * Compatibility interface for ReleaseService.
 * @deprecated Use org.rinna.domain.service.ReleaseService instead.
 */
@Deprecated(forRemoval = true)
public interface ReleaseService extends org.rinna.domain.service.ReleaseService {
    // This interface is intentionally empty as it's just a bridge
}
RELEASESERVICE

echo "Compatibility classes created at $(timestamp)"
echo "Now run: cd ${RINNA_DIR}/src && mvn test"
