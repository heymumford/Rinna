# Macro Automation System Design

This document outlines the design and implementation plan for the Macro Automation System in Rinna, a Standardized Utility Shell-Based Solution (SUSBS). The system provides a flexible way to automate workflows based on static and dynamic events within the SUSBS framework.

## 1. Core Design Principles

- **Decoupled execution**: Macros operate independently from the triggering events
- **Flexible triggers**: Supports both time-based and event-based triggers
- **Transparent execution**: Full logging and debugging capabilities
- **Clean integration**: Follows clean architecture principles of Rinna as a SUSBS
- **Security-focused**: Permission checks at critical execution points
- **Consistent PUI integration**: Seamless experience in the Pragmatic User Interface
- **SUSBS compliance**: Maintains standardized patterns for shell-based integration

## 2. System Architecture

### 2.1 Component Overview

```
[Trigger System] → [Macro Engine] → [Action System]
       ↑               ↓                 ↓
[Trigger Providers]  [Scheduler]     [Action Providers]
       ↑                                  ↓
[Event Sources] ← ← ← ← ← ← ← ← ← ← [System Effects]
```

### 2.2 Core Components

#### 2.2.1 Macro Engine
The central service managing macro execution and lifecycle.

Responsibilities:
- Load and validate macro definitions
- Process trigger events and match to macros
- Execute macro actions with proper context
- Handle parameter binding and variable substitution
- Track execution history and results
- Provide debugging information

#### 2.2.2 Trigger System
Processes events and determines which macros should be executed.

Types of triggers:
- **Static triggers**: Manual execution, scheduled time-based execution
- **Dynamic triggers**: Work item events (creation, transition, field updates)
- **System triggers**: Application events, integration updates
- **Composite triggers**: Multiple conditions combined with AND/OR logic

#### 2.2.3 Action System
Executes the effects prescribed by macros in a controlled manner.

Types of actions:
- **Work item actions**: Create, update, transition, add relationships
- **Metadata actions**: Add comments, tags, categorization
- **Integration actions**: Send notifications, call webhooks, API interactions
- **Flow control actions**: Conditionals, loops, delays, user prompts

#### 2.2.4 Scheduler
Manages time-based trigger execution.

Responsibilities:
- Schedule macro execution at specific times
- Support recurring execution patterns
- Handle time zone adjustments
- Provide reliable execution even after system restarts

## 3. Domain Model

### 3.1 MacroDefinition

The core entity representing a complete macro automation.

```java
public class MacroDefinition {
    private String id;                     // Unique identifier
    private String name;                   // User-friendly name
    private String description;            // Detailed description
    private MacroTrigger trigger;          // When the macro should execute
    private List<MacroAction> actions;     // What the macro should do
    private Map<String, String> parameters; // Configurable values
    private MacroSchedule schedule;        // For time-based execution
    private boolean enabled;               // Active status
    private String owner;                  // Creator/owner
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MacroExecution> recentExecutions;
}
```

### 3.2 MacroTrigger

Defines when a macro should be executed.

```java
public class MacroTrigger {
    private TriggerType type;              // Type of trigger
    private Map<String, Object> configuration; // Trigger-specific configuration
    private MacroCondition condition;      // Optional filtering condition
}

public enum TriggerType {
    // Static triggers
    MANUAL,                // User-initiated execution
    SCHEDULED,             // Time-based execution
    
    // Dynamic work item triggers
    ITEM_CREATED,         // When a new work item is created
    ITEM_UPDATED,         // When a work item is updated
    ITEM_TRANSITIONED,    // When a work item changes state
    COMMENT_ADDED,        // When a comment is added to a work item
    FIELD_CHANGED,        // When a specific field is modified
    
    // System triggers
    SYSTEM_STARTUP,       // When the system starts
    USER_LOGIN,           // When a user logs in
    INTEGRATION_EVENT,    // When an external integration event occurs
    
    // Composite triggers
    CONDITION_GROUP       // Logical group of conditions
}
```

### 3.3 MacroAction

Defines what the macro should do when triggered.

```java
public class MacroAction {
    private ActionType type;              // Type of action
    private Map<String, Object> configuration; // Action-specific configuration
    private Integer order;                // Execution order
    private MacroCondition condition;     // Optional conditional execution
}

public enum ActionType {
    // Work item actions
    CREATE_WORK_ITEM,     // Create a new work item
    UPDATE_WORK_ITEM,     // Update an existing work item
    TRANSITION_WORK_ITEM, // Change work item state
    ADD_COMMENT,          // Add a comment to a work item
    ADD_RELATIONSHIP,     // Create a relationship between work items
    
    // System actions
    SEND_NOTIFICATION,    // Send a notification
    CALL_WEBHOOK,         // Make an HTTP call to an external system
    EXECUTE_COMMAND,      // Run a system command
    
    // Flow control
    CONDITION,            // Conditional branching
    LOOP,                 // Repeat actions
    DELAY,                // Pause execution
    USER_PROMPT           // Request user input
}
```

### 3.4 MacroExecution

Records the execution of a macro.

```java
public class MacroExecution {
    private String id;                     // Execution ID
    private String macroId;                // Reference to the macro
    private TriggerContext triggerContext; // What triggered the execution
    private ExecutionStatus status;        // Current status
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<ActionResult> actionResults; // Results of each action
    private Map<String, Object> variables;   // Variables during execution
    private String errorMessage;           // If execution failed
}

public enum ExecutionStatus {
    PENDING,              // Scheduled but not started
    RUNNING,              // Currently executing
    COMPLETED,            // Successfully completed
    FAILED,               // Failed execution
    CANCELLED             // Manually cancelled
}
```

## 4. Technical Implementation

### 4.1 Domain Layer

#### 4.1.1 Interfaces

```java
public interface MacroService {
    MacroDefinition createMacro(MacroDefinition macro);
    MacroDefinition getMacro(String id);
    List<MacroDefinition> listMacros(Map<String, String> filters);
    MacroDefinition updateMacro(String id, MacroDefinition macro);
    void deleteMacro(String id);
    void enableMacro(String id);
    void disableMacro(String id);
    
    MacroExecution executeManually(String macroId, Map<String, Object> params);
    MacroExecution getExecution(String executionId);
    List<MacroExecution> getExecutionHistory(String macroId, int limit);
}

public interface TriggerService {
    void registerTriggerProvider(TriggerProvider provider);
    void processEvent(TriggerEvent event);
    List<MacroDefinition> findMatchingMacros(TriggerEvent event);
}

public interface ActionService {
    void registerActionProvider(ActionProvider provider);
    ActionResult executeAction(MacroAction action, ExecutionContext context);
}

public interface SchedulerService {
    void scheduleMacro(String macroId, MacroSchedule schedule);
    void cancelScheduledMacro(String macroId);
    List<ScheduledExecution> getScheduledExecutions();
}
```

#### 4.1.2 Provider Interfaces

```java
public interface TriggerProvider {
    TriggerType getProvidedType();
    boolean matches(TriggerEvent event, MacroTrigger trigger);
}

public interface ActionProvider {
    ActionType getProvidedType();
    ActionResult execute(MacroAction action, ExecutionContext context);
}
```

### 4.2 Clean Architecture Implementation

#### 4.2.1 Use Cases / Application Layer

```java
public class CreateMacroUseCase implements UseCase<MacroDefinition, MacroDefinition> {
    private final MacroRepository repository;
    private final MacroValidator validator;
    
    @Override
    public MacroDefinition execute(MacroDefinition input) {
        // Validate macro definition
        validator.validate(input);
        
        // Create new macro
        MacroDefinition macro = repository.create(input);
        
        // Schedule if needed
        if (input.getSchedule() != null) {
            schedulerService.scheduleMacro(macro.getId(), macro.getSchedule());
        }
        
        return macro;
    }
}

public class ProcessEventUseCase implements UseCase<TriggerEvent, List<MacroExecution>> {
    private final TriggerService triggerService;
    private final MacroExecutor macroExecutor;
    
    @Override
    public List<MacroExecution> execute(TriggerEvent event) {
        // Find matching macros
        List<MacroDefinition> macros = triggerService.findMatchingMacros(event);
        
        // Execute each matching macro
        List<MacroExecution> executions = new ArrayList<>();
        for (MacroDefinition macro : macros) {
            MacroExecution execution = macroExecutor.execute(macro, event);
            executions.add(execution);
        }
        
        return executions;
    }
}
```

#### 4.2.2 Adapters / Infrastructure Layer

```java
public class SqliteMacroRepository implements MacroRepository {
    // Implementation details specific to SQLite persistence
}

public class WorkItemTriggerProvider implements TriggerProvider {
    @Override
    public TriggerType getProvidedType() {
        return TriggerType.ITEM_UPDATED;
    }
    
    @Override
    public boolean matches(TriggerEvent event, MacroTrigger trigger) {
        // Implementation specific to work item update triggers
    }
}

public class WorkItemActionProvider implements ActionProvider {
    @Override
    public ActionType getProvidedType() {
        return ActionType.UPDATE_WORK_ITEM;
    }
    
    @Override
    public ActionResult execute(MacroAction action, ExecutionContext context) {
        // Implementation specific to work item update actions
    }
}
```

## 5. PUI Integration

### 5.1 Macro Management UI

The Macro Management UI provides a comprehensive interface for managing macros within the Pragmatic User Interface (PUI).

Features:
- **Macro List View**: Display all macros with status and last execution time
- **Macro Detail View**: Show detailed information about a specific macro
- **Macro Creation Interface**: Step-by-step wizard for creating new macros
- **Execution History**: View execution history with filtering and sorting
- **Real-time Status**: Show currently executing macros and their progress
- **Keyboard-Optimized Workflow**: Efficient keyboard shortcuts for common operations

### 5.2 Trigger Configuration UI

Specialized PUI components for configuring various trigger types with a focus on developer efficiency.

Components:
- **Work Item Trigger Configuration**: Configure work item event triggers
- **Scheduled Trigger Configuration**: Set up time-based triggers
- **System Trigger Configuration**: Configure system event triggers
- **Webhook Trigger Configuration**: Configure authenticated webhook endpoints
- **Composite Trigger Builder**: Visual builder for complex trigger conditions

### 5.3 Action Configuration UI

Specialized PUI components for configuring various action types with pragmatic design principles.

Components:
- **Work Item Action Configuration**: Configure work item operations
- **Notification Action Configuration**: Set up notification dispatch
- **Webhook Action Configuration**: Configure outgoing webhook calls with authentication
- **Integration Action Configuration**: Configure external system interactions
- **Flow Control Configuration**: Set up conditional logic and loops

### 5.4 Execution Monitoring UI

Pragmatic interface for monitoring macro executions in real-time.

Features:
- **Active Executions**: Show currently running macros with progress
- **Execution Timeline**: Visual timeline of recent executions
- **Execution Detail**: Step-by-step breakdown of action execution
- **Debugging Tools**: Variable inspection and action result examination
- **Webhook Inspection**: View incoming and outgoing webhook details

## 6. Implementation Phases

### 6.1 Phase 1: Core Engine Implementation

1. Design and implement the core domain model
2. Create the MacroService and basic repository implementation
3. Implement the trigger matching system
4. Develop the action execution framework
5. Add the basic scheduler for time-based execution
6. Implement permission checks for secure execution

### 6.2 Phase 2: Basic Triggers and Actions

1. Implement work item event triggers (created, updated, transitioned)
2. Add manual execution trigger
3. Create basic scheduled trigger implementation
4. Implement core work item actions (create, update, transition)
5. Add comment and relationship actions
6. Implement notification action

### 6.3 Phase 3: Flow Control and Variables

1. Add conditional action execution
2. Implement looping constructs
3. Create variable system for storing intermediate values
4. Implement expression evaluation for dynamic values
5. Add delay and user prompt actions
6. Create variable binding between triggers and actions

### 6.4 Phase 4: PUI Integration

1. Design and implement the macro list view according to pragmatic design principles
2. Create the macro detail view optimized for developer efficiency
3. Implement the trigger configuration UI with webhook support
4. Add the action configuration UI with webhook configuration capabilities
5. Create the execution monitoring interface with webhook inspection features
6. Implement keyboard shortcuts following pragmatic user interaction patterns

### 6.5 Phase 5: Advanced Features

1. Add template system for reusable macro patterns
2. Implement macro import/export
3. Create integration hooks for external systems
4. Add advanced scheduling options
5. Implement comprehensive permission model
6. Create macro version control and history

## 7. Testing Strategy

### 7.1 Unit Tests

- Test individual triggers and actions in isolation
- Validate trigger matching logic
- Test action execution and result handling
- Verify parameter binding and variable substitution
- Test expression evaluation

### 7.2 Component Tests

- Test the macro engine with mock triggers and actions
- Validate scheduler functionality
- Test the macro repository implementation
- Verify execution tracking and history

### 7.3 Integration Tests

- Test end-to-end macro execution
- Validate integration with the work item system
- Test TUI integration components
- Verify permission checks and security

### 7.4 Performance and Load Tests

- Test with large numbers of macros
- Validate concurrent execution handling
- Test scheduler performance with many scheduled macros
- Verify system performance under heavy automation load

## 8. Security Considerations

### 8.1 Permission Model

Macros will operate within a comprehensive permission model to ensure secure automation:

- **Ownership**: Macros have an owner who has full control
- **Execution Permission**: Controls who can manually execute a macro
- **Edit Permission**: Controls who can modify a macro
- **View Permission**: Controls who can see a macro's definition
- **Elevated Rights**: Special permission for actions that modify sensitive data

### 8.2 Execution Constraints

To prevent abuse or system overload, the following constraints will be applied:

- **Rate Limiting**: Limits on how frequently macros can execute
- **Resource Quotas**: Limits on resource consumption per macro
- **Execution Timeout**: Maximum execution time for a macro
- **Action Restrictions**: Limitations on certain actions based on user role
- **Approval Workflow**: Optional approval step for sensitive actions

## 9. Migration and Deployment Strategy

### 9.1 Database Schema

New tables will be added to the SQLite database:

- `macro_definitions`: Stores macro definitions
- `macro_executions`: Records execution history
- `macro_actions`: Stores action configurations
- `macro_triggers`: Stores trigger configurations
- `macro_schedules`: Stores scheduling information

### 9.2 Deployment Process

1. Add database schema migration script
2. Deploy core engine components
3. Add API endpoints for macro management
4. Deploy TUI integration components
5. Add documentation and examples

### 9.3 Backward Compatibility

The macro system will be designed as an optional component that doesn't affect existing functionality:

- No changes to existing work item behavior
- Opt-in design for integrating with automation
- Graceful degradation when automation is unavailable

## 10. Documentation Plan

### 10.1 User Documentation

- **Getting Started Guide**: Introduction to the macro system
- **Trigger Reference**: Documentation for all available triggers
- **Action Reference**: Documentation for all available actions
- **TUI Usage Guide**: How to use the macro management interface
- **Best Practices**: Guidelines for effective automation design
- **Tutorial Series**: Step-by-step examples for common use cases

### 10.2 Developer Documentation

- **Architecture Overview**: Detailed explanation of the macro system design
- **Extension Guide**: How to add custom triggers and actions
- **Integration Guide**: How to integrate with the macro system
- **API Reference**: Documentation for the macro management API
- **Testing Guide**: How to test macros and automation

## 11. Example Use Cases

### 11.1 Automatic Sprint Handling

Automatically move work items between sprints based on their status:

```
Trigger: Time-based (Monday morning)
Actions:
1. Find all incomplete items in the current sprint
2. Move them to the next sprint
3. Update their priority based on how many times they've been moved
4. Add a comment with the move history
5. Send notification to item owners
```

### 11.2 Status Update Workflow

Automate follow-up actions when a work item status changes:

```
Trigger: Work item status change to "In Review"
Actions:
1. Add standard review checklist comment
2. Assign to the original reporter for review
3. Set due date for review (3 days from now)
4. Send notification to the reviewer
5. Add to review board
```

### 11.3 Cross-Project Synchronization

Keep related items in sync across multiple projects:

```
Trigger: Work item field change (any field)
Condition: Item has cross-project relationships
Actions:
1. Identify related items in other projects
2. Update corresponding fields in related items
3. Add sync comment to both items
4. Log the synchronization in the audit trail
```

## 12. Future Expansion

### 12.1 Visual Builder

A more advanced visual builder for macros could be implemented in Phase 7:

- Drag-and-drop interface for action sequencing
- Visual representation of flow control
- Live testing and simulation
- Built-in debugging tools

### 12.2 AI Integration

Integration with AI systems in Phase 7 could enhance the macro system:

- AI-suggested macros based on common patterns
- Intelligent parameter defaults
- Anomaly detection in macro execution
- Natural language macro creation