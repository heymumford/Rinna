# CLI Module Fixes Implementation Plan

## Problem Statement

The CLI module is currently unable to compile due to incompatibility issues with the core module. The core module has undergone refactoring to follow clean architecture principles, with domain classes and interfaces relocated to proper packages. The CLI module needs to be updated to work with these changes.

## Root Causes

1. **Package Structure Changes**
   - Core domain classes moved from flat organization to clean architecture package structure
   - Interface definitions now properly separated from implementations

2. **Dependency Issues**
   - Maven dependency configuration not correctly set up between modules
   - JAR packaging issues in the core module

3. **Type Compatibility**
   - CLI has its own model classes that don't match the domain models
   - Direct implementation of domain interfaces causing typing issues

## Implementation Strategy

We will use the **Adapter Pattern** to create a bridge between the CLI and domain models. This approach will allow the CLI module to continue using its own model classes while properly implementing the domain interfaces through adapters.

### Revised Approach for Dependency Resolution

Due to Maven dependency issues, we've implemented a modified strategy:

1. **Create local domain interfaces in CLI module**
   - Implement domain-compatible interfaces directly in the CLI module
   - Create local versions of domain model types (DomainWorkItem, etc.)
   - Ensure consistent naming and interface definitions

2. **Enhance adapter classes to use local interfaces**
   - Implement adapters against the local domain interfaces
   - Use consistent bidirectional model conversion
   - Ensure proper exception translation between domains

### 1. Domain Interface Proxies

Create minimal interface proxies in the CLI module that mirror the domain interfaces but use CLI types:

```java
// Example: CLI-specific workflow service interface
package org.rinna.cli.domain;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import java.util.List;
import java.util.Optional;

public interface WorkflowService {
    // CLI-specific methods with CLI types
    WorkItem transition(String itemId, WorkflowState targetState) throws InvalidTransitionException;
    boolean canTransition(String itemId, WorkflowState targetState);
    List<WorkflowState> getAvailableTransitions(String itemId);
    Optional<WorkItem> getCurrentWorkInProgress(String user);
}
```

### 2. Adapter Classes

Create adapter classes that implement domain interfaces but delegate to CLI service implementations:

```java
// Example: Domain-to-CLI workflow service adapter
package org.rinna.cli.adapter;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.StateMapper;
import org.rinna.domain.service.WorkflowService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorkflowServiceAdapter implements WorkflowService {
    private final MockWorkflowService cliService;
    
    public WorkflowServiceAdapter(MockWorkflowService cliService) {
        this.cliService = cliService;
    }
    
    @Override
    public org.rinna.domain.model.WorkItem transition(UUID itemId, org.rinna.domain.model.WorkflowState targetState) 
            throws org.rinna.domain.service.InvalidTransitionException {
        try {
            // Convert from domain to CLI
            WorkflowState cliState = WorkflowState.fromCoreState(targetState.name());
            // Delegate to CLI service
            WorkItem cliItem = cliService.transition(itemId.toString(), cliState);
            // Convert back to domain
            return ModelMapper.toDomainWorkItem(cliItem);
        } catch (org.rinna.cli.service.InvalidTransitionException e) {
            throw new org.rinna.domain.service.InvalidTransitionException(e.getMessage());
        }
    }
    
    // Other adapter methods...
}
```

### 3. Service Factory

Create a service factory that instantiates the proper adapters:

```java
// Example: Service factory for creating the proper adapters
package org.rinna.cli.factory;

import org.rinna.cli.adapter.WorkflowServiceAdapter;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.domain.service.WorkflowService;

public class ServiceFactory {
    public static WorkflowService createWorkflowService() {
        MockWorkflowService cliService = new MockWorkflowService();
        return new WorkflowServiceAdapter(cliService);
    }
    
    // Other factory methods...
}
```

### 4. Service Manager Integration

Update the ServiceManager to use the new adapters:

```java
// Example: Updated ServiceManager with domain adapter support
package org.rinna.cli.service;

import org.rinna.cli.adapter.WorkflowServiceAdapter;
import org.rinna.cli.factory.ServiceFactory;
import org.rinna.domain.service.WorkflowService;

public class ServiceManager {
    private static ServiceManager instance;
    private final MockWorkflowService cliWorkflowService;
    private final WorkflowService domainWorkflowService;
    
    private ServiceManager() {
        // Initialize CLI services
        cliWorkflowService = new MockWorkflowService();
        
        // Create domain adapters
        domainWorkflowService = new WorkflowServiceAdapter(cliWorkflowService);
    }
    
    // Methods to get CLI services
    public MockWorkflowService getCliWorkflowService() {
        return cliWorkflowService;
    }
    
    // Methods to get domain services
    public WorkflowService getDomainWorkflowService() {
        return domainWorkflowService;
    }
    
    // Singleton accessor
    public static ServiceManager getInstance() {
        if (instance == null) {
            instance = new ServiceManager();
        }
        return instance;
    }
}
```

## Implementation Steps

1. **Fix the Core Module JAR Packaging** ✅ (Completed with Alternative Approach)
   - Update the assembly plugin configuration to not override the main artifact ✅
   - Ensure all domain interfaces and classes are properly packaged ✅
   - Created local domain interfaces in CLI module as a workaround ✅
   - Manually installed the core JAR to local Maven repository ✅
   
   **Build Configuration Issues Addressed:**
   - Created local domain model classes to resolve compilation errors
   - Implemented CLI-specific versions of the domain interfaces
   - Added bidirectional model conversion for all types
   - Enhanced adapter classes to work with local domain interfaces

2. **Create CLI-Specific Domain Models** ✅
   - Create CLI-specific versions of all required domain interfaces
   - Implement model conversion utilities

3. **Implement Adapter Classes** ✅ (Completed)
   - Create adapter classes for each domain service interface ✅ (8 of 8 completed)
     - ✅ WorkflowServiceAdapter
     - ✅ BacklogServiceAdapter
     - ✅ ItemServiceAdapter
     - ✅ CommentServiceAdapter
     - ✅ HistoryServiceAdapter
     - ✅ SearchServiceAdapter
     - ✅ MonitoringServiceAdapter
     - ✅ RecoveryServiceAdapter
   - Implement bidirectional model conversion ✅

4. **Update Service Manager** ✅ (Completed)
   - Modify the ServiceManager to expose both CLI and domain services ✅
   - Use composition to delegate operations between CLI and domain models ✅
   - Ensure all services are properly initialized ✅ (8 of 8 completed)

5. **Update Command Implementations** ⏳ (In Progress)
   - Updated key CLI commands to use ServiceManager and the new architecture
     - ✅ ViewCommand
     - ✅ DoneCommand
     - ✅ AddCommand
     - 🔄 Remaining commands
   - Added proper error handling in command implementations
   - Added consistent output formatting
   - Implemented bidirectional model conversion in command methods

## Testing Plan

1. **Compile Tests** ⏳ (In Progress)
   - Ensure the CLI module compiles successfully
   - Verify all service adapters implement their interfaces correctly ⏳ (7 of 8 completed)

2. **Unit Tests** 🔄 (Not Started)
   - Create unit tests for the adapter classes
   - Test bidirectional model conversion
   - Priority testing for:
     - MonitoringServiceAdapter (Newest implementation)
     - WorkflowServiceAdapter (Core functionality)
     - ItemServiceAdapter (Core functionality)

3. **Integration Tests** 🔄 (Not Started)
   - Test the integration of CLI commands with the service adapters
   - Verify correct behavior of all adapted domain interfaces

## Success Criteria

1. The CLI module compiles successfully without errors
2. CLI commands can work with both CLI and domain models
3. All service adapters correctly implement their respective domain interfaces
4. Model conversion utilities correctly handle the mapping between CLI and domain types
5. The system meets all existing functionality requirements