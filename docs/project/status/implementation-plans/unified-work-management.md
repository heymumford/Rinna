# Unified Work Management System Implementation Plan

This document outlines the implementation plan for enhancing the Unified Work Management System in Rinna to meet the requirements specified in the project backlog.

## 1. Overview

The Unified Work Management System aims to eliminate traditional boundaries between different types of work (business, product, engineering, test) by treating all work as part of a single, cohesive system while respecting the unique characteristics of different work types.

## 2. Current Status

The core foundation for the Unified Work Management System is already in place:

- **Domain Models**: 
  - `UnifiedWorkItem` interface
  - `UnifiedWorkItemRecord` implementation
  - Supporting enums: `CynefinDomain`, `OriginCategory`, `WorkParadigm`

- **Service Layer**: 
  - `UnifiedWorkItemService` interface 
  - `DefaultUnifiedWorkItemService` implementation

- **Repository Layer**:
  - `UnifiedWorkItemRepository` interface
  - `InMemoryUnifiedWorkItemRepository` implementation

- **Documentation**:
  - Basic documentation in `unified-work-management.md`
  - Comprehensive guide in `unified-work-management-guide.md`

## 3. Requirements from Backlog

The primary requirements from the backlog include:

1. **Build comprehensive documentation** on unified work management approach
2. **Enhance work item model** to better categorize items without requiring separate workflows
3. **Implement flexible reporting** that combines different work types
4. **Create sample templates** for different work categories
5. **Update UIs** to reflect unified work model principles
6. **Design system to track percentage completion** toward POC milestone goals
7. **Document setup and configuration** for work type vocabulary mapping

## 4. Implementation Plan

### Phase 1: Model Enhancement (Week 1)

1. **Update UnifiedWorkItem Model**:
   - Add support for flexible categorization with tags
   - Enhance metadata model to support custom fields by work type
   - Add vocabulary mapping support via translation layer
   - Add achievement/completion tracking attributes

2. **Enhance Classification System**:
   - Refine the CYNEFIN domain classification
   - Update the origin category definitions
   - Add milestone and POC tracking fields

3. **Implement Vocabulary Mapping System**:
   - Create `VocabularyMap` class with context-specific translations
   - Implement `VocabularyMapService` for term translation
   - Add context-awareness to work item presentation

### Phase 2: Service Enhancement (Week 2)

1. **Update UnifiedWorkItemService**:
   - Add methods for vocabulary mapping
   - Enhance reporting capabilities
   - Implement milestone tracking
   - Add POC completion percentage calculation
   - Implement cognitive load balancing algorithms

2. **Create Specialized Reporting Services**:
   - Implement `UnifiedReportService` for cross-type reporting
   - Create `MilestoneTrackingService` for progress tracking
   - Add `WorkDistributionAnalyzer` for balance analysis

3. **Command Line Interface Updates**:
   - Add new commands for unified work management
   - Implement vocabulary context setting
   - Add completion tracking commands
   - Create reporting commands

### Phase 3: User Interface Updates (Week 3)

1. **Update CLI Interface**:
   - Implement context-aware presentation of work items
   - Add unified reporting commands
   - Create milestone tracking views
   - Implement cognitive load visualization

2. **Enhance Pragmatic UI (PUI)**:
   - Update work item display components with unified model
   - Add context-aware terminology rendering
   - Implement milestone progress visualization
   - Create cognitive load dashboard

3. **Create Templates and Views**:
   - Implement templates for different work categories
   - Create unified board view across all work types
   - Build specialized views for different stakeholders

### Phase 4: Documentation and Testing (Week 4)

1. **Comprehensive Documentation**:
   - Update user guides with unified work approach details
   - Create setup and configuration guide
   - Develop best practices documentation
   - Add examples for common scenarios

2. **Testing**:
   - Implement unit tests for model enhancements
   - Create component tests for service functionality
   - Add integration tests for vocabulary mapping
   - Develop acceptance tests for milestone tracking

3. **Sample Data and Demos**:
   - Create sample datasets demonstrating different work types
   - Build demo scripts for common workflows
   - Implement POC milestone tracking examples

## 5. Detailed Implementation Tasks

### Phase 1: Model Enhancement

#### Task 1.1: Update UnifiedWorkItem Model

```java
// Add vocabulary context support
public interface UnifiedWorkItem extends WorkItem {
    // Existing methods...
    
    /**
     * Returns the vocabulary context for this work item.
     * This controls the terminology used for display.
     * 
     * @return an Optional containing the vocabulary context, or empty if using default
     */
    Optional<String> getVocabularyContext();
    
    /**
     * Returns the completion percentage for this work item.
     * 
     * @return an Optional containing the completion percentage (0-100), or empty if not tracked
     */
    Optional<Integer> getCompletionPercentage();
    
    /**
     * Returns the milestone associated with this work item, if any.
     * 
     * @return an Optional containing the milestone ID, or empty if not associated with a milestone
     */
    Optional<UUID> getMilestoneId();
}
```

#### Task 1.2: Update UnifiedWorkItemRecord

```java
// Add new fields to the record
public record UnifiedWorkItemRecord(
        // Existing fields...
        Optional<String> vocabularyContext,
        Optional<Integer> completionPercentage,
        Optional<UUID> milestoneId
) implements UnifiedWorkItem {
    // Existing methods...
    
    // Update builder to support new fields
    public static class Builder {
        // Existing fields...
        private Optional<String> vocabularyContext = Optional.empty();
        private Optional<Integer> completionPercentage = Optional.empty();
        private Optional<UUID> milestoneId = Optional.empty();
        
        // Add builder methods for new fields
        public Builder vocabularyContext(String context) {
            this.vocabularyContext = Optional.ofNullable(context);
            return this;
        }
        
        public Builder completionPercentage(Integer percentage) {
            if (percentage != null && (percentage < 0 || percentage > 100)) {
                throw new IllegalArgumentException("Completion percentage must be between 0 and 100");
            }
            this.completionPercentage = Optional.ofNullable(percentage);
            return this;
        }
        
        public Builder milestoneId(UUID milestoneId) {
            this.milestoneId = Optional.ofNullable(milestoneId);
            return this;
        }
        
        // Update build method to include new fields
        @Override
        public UnifiedWorkItemRecord build() {
            // Existing build logic...
            return new UnifiedWorkItemRecord(
                    // Existing fields...
                    vocabularyContext,
                    completionPercentage,
                    milestoneId
            );
        }
    }
}
```

#### Task 1.3: Create VocabularyMap Model

```java
/**
 * Represents a mapping between terms in different contexts.
 */
public record VocabularyMap(
        String sourceContext,
        String targetContext,
        Map<String, String> termMappings
) {
    /**
     * Translates a term from the source context to the target context.
     * 
     * @param sourceTerm the term to translate
     * @return the translated term, or the original if no mapping exists
     */
    public String translateTerm(String sourceTerm) {
        return termMappings.getOrDefault(sourceTerm, sourceTerm);
    }
    
    /**
     * Creates a new builder for VocabularyMap.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for VocabularyMap.
     */
    public static class Builder {
        private String sourceContext;
        private String targetContext;
        private Map<String, String> termMappings = new HashMap<>();
        
        public Builder sourceContext(String sourceContext) {
            this.sourceContext = sourceContext;
            return this;
        }
        
        public Builder targetContext(String targetContext) {
            this.targetContext = targetContext;
            return this;
        }
        
        public Builder addMapping(String sourceTerm, String targetTerm) {
            this.termMappings.put(sourceTerm, targetTerm);
            return this;
        }
        
        public Builder addMappings(Map<String, String> mappings) {
            this.termMappings.putAll(mappings);
            return this;
        }
        
        public VocabularyMap build() {
            return new VocabularyMap(sourceContext, targetContext, termMappings);
        }
    }
}
```

#### Task 1.4: Create Milestone Model

```java
/**
 * Represents a milestone for tracking progress.
 */
public record Milestone(
        UUID id,
        String name,
        String description,
        Instant targetDate,
        Map<WorkItemType, Integer> completionCriteria,
        boolean active
) {
    /**
     * Creates a new builder for Milestone.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for Milestone.
     */
    public static class Builder {
        private UUID id;
        private String name;
        private String description;
        private Instant targetDate;
        private Map<WorkItemType, Integer> completionCriteria = new HashMap<>();
        private boolean active = true;
        
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder targetDate(Instant targetDate) {
            this.targetDate = targetDate;
            return this;
        }
        
        public Builder addCriterion(WorkItemType type, int count) {
            this.completionCriteria.put(type, count);
            return this;
        }
        
        public Builder setCriteria(Map<WorkItemType, Integer> criteria) {
            this.completionCriteria.clear();
            this.completionCriteria.putAll(criteria);
            return this;
        }
        
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public Milestone build() {
            if (id == null) {
                id = UUID.randomUUID();
            }
            return new Milestone(id, name, description, targetDate, completionCriteria, active);
        }
    }
}
```

### Phase 2: Service Enhancement

#### Task 2.1: Create VocabularyMapService

```java
/**
 * Service for managing vocabulary mappings between different contexts.
 */
public interface VocabularyMapService {
    /**
     * Creates a new vocabulary map.
     * 
     * @param map the vocabulary map to create
     * @return the created vocabulary map
     */
    VocabularyMap create(VocabularyMap map);
    
    /**
     * Finds a vocabulary map by source and target contexts.
     * 
     * @param sourceContext the source context
     * @param targetContext the target context
     * @return an Optional containing the vocabulary map, or empty if not found
     */
    Optional<VocabularyMap> findByContexts(String sourceContext, String targetContext);
    
    /**
     * Finds all vocabulary maps with the specified source context.
     * 
     * @param sourceContext the source context
     * @return a list of vocabulary maps
     */
    List<VocabularyMap> findBySourceContext(String sourceContext);
    
    /**
     * Translates a term from one context to another.
     * 
     * @param term the term to translate
     * @param sourceContext the source context
     * @param targetContext the target context
     * @return the translated term, or the original if no mapping exists
     */
    String translateTerm(String term, String sourceContext, String targetContext);
    
    /**
     * Translates a work item's display fields based on the target context.
     * 
     * @param workItem the work item to translate
     * @param targetContext the target context
     * @return a map of translated display fields
     */
    Map<String, String> translateWorkItem(UnifiedWorkItem workItem, String targetContext);
    
    /**
     * Deletes a vocabulary map.
     * 
     * @param sourceContext the source context
     * @param targetContext the target context
     */
    void deleteByContexts(String sourceContext, String targetContext);
}
```

#### Task 2.2: Create MilestoneService

```java
/**
 * Service for managing milestones and tracking progress.
 */
public interface MilestoneService {
    /**
     * Creates a new milestone.
     * 
     * @param milestone the milestone to create
     * @return the created milestone
     */
    Milestone create(Milestone milestone);
    
    /**
     * Finds a milestone by ID.
     * 
     * @param id the milestone ID
     * @return an Optional containing the milestone, or empty if not found
     */
    Optional<Milestone> findById(UUID id);
    
    /**
     * Finds all active milestones.
     * 
     * @return a list of active milestones
     */
    List<Milestone> findActiveMillestones();
    
    /**
     * Adds a work item to a milestone.
     * 
     * @param milestoneId the milestone ID
     * @param workItemId the work item ID
     * @return the updated work item
     */
    UnifiedWorkItem addWorkItemToMilestone(UUID milestoneId, UUID workItemId);
    
    /**
     * Removes a work item from a milestone.
     * 
     * @param workItemId the work item ID
     * @return the updated work item
     */
    UnifiedWorkItem removeWorkItemFromMilestone(UUID workItemId);
    
    /**
     * Finds all work items associated with a milestone.
     * 
     * @param milestoneId the milestone ID
     * @return a list of work items
     */
    List<UnifiedWorkItem> findWorkItemsByMilestone(UUID milestoneId);
    
    /**
     * Calculates the completion percentage for a milestone.
     * 
     * @param milestoneId the milestone ID
     * @return the completion percentage (0-100)
     */
    int calculateCompletionPercentage(UUID milestoneId);
    
    /**
     * Gets a detailed progress report for a milestone.
     * 
     * @param milestoneId the milestone ID
     * @return a map containing progress statistics
     */
    Map<String, Object> getMilestoneProgressReport(UUID milestoneId);
    
    /**
     * Updates the completion criteria for a milestone.
     * 
     * @param milestoneId the milestone ID
     * @param type the work item type
     * @param count the required count
     * @return the updated milestone
     */
    Milestone updateCompletionCriteria(UUID milestoneId, WorkItemType type, int count);
    
    /**
     * Updates the active status of a milestone.
     * 
     * @param milestoneId the milestone ID
     * @param active the new active status
     * @return the updated milestone
     */
    Milestone updateActiveStatus(UUID milestoneId, boolean active);
    
    /**
     * Deletes a milestone.
     * 
     * @param milestoneId the milestone ID
     */
    void deleteById(UUID milestoneId);
}
```

#### Task 2.3: Update UnifiedWorkItemService

```java
// Add new methods to UnifiedWorkItemService interface
public interface UnifiedWorkItemService {
    // Existing methods...
    
    /**
     * Updates the vocabulary context of a work item.
     * 
     * @param id the work item ID
     * @param context the new vocabulary context
     * @return the updated work item
     */
    UnifiedWorkItem updateVocabularyContext(UUID id, String context);
    
    /**
     * Updates the completion percentage of a work item.
     * 
     * @param id the work item ID
     * @param percentage the new completion percentage (0-100)
     * @return the updated work item
     */
    UnifiedWorkItem updateCompletionPercentage(UUID id, int percentage);
    
    /**
     * Finds work items by milestone.
     * 
     * @param milestoneId the milestone ID
     * @return a list of work items associated with the milestone
     */
    List<UnifiedWorkItem> findByMilestone(UUID milestoneId);
    
    /**
     * Gets work items with translations for display in the specified context.
     * 
     * @param workItems the work items to translate
     * @param targetContext the target vocabulary context
     * @return a list of maps containing the translated work items
     */
    List<Map<String, Object>> getTranslatedWorkItems(List<UnifiedWorkItem> workItems, String targetContext);
    
    /**
     * Gets a cognitive load report for a team.
     * 
     * @param teamId the team ID
     * @return a map containing cognitive load statistics
     */
    Map<String, Object> getTeamCognitiveLoadReport(String teamId);
}
```

### Phase 3: CLI and UI Updates

#### Task 3.1: Create UnifiedWorkItemCommands

```java
/**
 * CLI command for managing unified work items with vocabulary mapping.
 */
@CommandLineInterface(application = "rin unified")
public class UnifiedWorkItemCommand implements Runnable {
    private final UnifiedWorkItemService workItemService;
    private final VocabularyMapService vocabularyService;
    private final MilestoneService milestoneService;
    
    // Constructor with dependency injection...
    
    @Command(name = "list", description = "List work items with context-specific terminology")
    public void list(
            @Option(names = "--context", description = "Vocabulary context for display") String context,
            @Option(names = "--type", description = "Filter by work item type") String type,
            @Option(names = "--milestone", description = "Filter by milestone") String milestone,
            @Option(names = "--format", description = "Output format (table, json)") String format
    ) {
        // Implementation to list work items with context-specific terminology
    }
    
    @Command(name = "create", description = "Create a new unified work item")
    public void create(
            @Option(names = "--title", required = true, description = "Work item title") String title,
            @Option(names = "--description", description = "Work item description") String description,
            @Option(names = "--type", required = true, description = "Work item type") String type,
            @Option(names = "--category", required = true, description = "Origin category") String category,
            @Option(names = "--domain", required = true, description = "CYNEFIN domain") String domain,
            @Option(names = "--paradigm", required = true, description = "Work paradigm") String paradigm,
            @Option(names = "--context", description = "Vocabulary context") String context,
            @Option(names = "--milestone", description = "Milestone ID") String milestone,
            @Option(names = "--tags", description = "Comma-separated tags") String tags
    ) {
        // Implementation to create a new unified work item
    }
    
    @Command(name = "milestone", description = "Manage milestones")
    public class MilestoneCommand {
        @Command(name = "create", description = "Create a new milestone")
        public void create(
                @Option(names = "--name", required = true, description = "Milestone name") String name,
                @Option(names = "--description", description = "Milestone description") String description,
                @Option(names = "--target-date", description = "Target completion date (yyyy-MM-dd)") String targetDate
        ) {
            // Implementation to create a new milestone
        }
        
        @Command(name = "criteria", description = "Manage milestone completion criteria")
        public void criteria(
                @Option(names = "--milestone", required = true, description = "Milestone ID") String milestone,
                @Option(names = "--type", required = true, description = "Work item type") String type,
                @Option(names = "--count", required = true, description = "Required count") int count
        ) {
            // Implementation to manage milestone criteria
        }
        
        @Command(name = "status", description = "Show milestone status and progress")
        public void status(
                @Option(names = "--milestone", required = true, description = "Milestone ID") String milestone,
                @Option(names = "--format", description = "Output format (table, json)") String format
        ) {
            // Implementation to show milestone status
        }
    }
    
    @Command(name = "vocabulary", description = "Manage vocabulary mappings")
    public class VocabularyCommand {
        @Command(name = "context", description = "Create a vocabulary context")
        public void context(
                @Option(names = "--name", required = true, description = "Context name") String name,
                @Option(names = "--description", description = "Context description") String description
        ) {
            // Implementation to create a vocabulary context
        }
        
        @Command(name = "map", description = "Create term mappings between contexts")
        public void map(
                @Option(names = "--source", required = true, description = "Source context") String source,
                @Option(names = "--target", required = true, description = "Target context") String target,
                @Option(names = "--term", required = true, description = "Source term") String term,
                @Option(names = "--translation", required = true, description = "Target term") String translation
        ) {
            // Implementation to create term mappings
        }
        
        @Command(name = "list", description = "List vocabulary mappings")
        public void list(
                @Option(names = "--source", description = "Source context") String source,
                @Option(names = "--target", description = "Target context") String target
        ) {
            // Implementation to list vocabulary mappings
        }
    }
    
    @Command(name = "report", description = "Generate unified work reports")
    public class ReportCommand {
        @Command(name = "distribution", description = "Show work item distribution")
        public void distribution(
                @Option(names = "--by", description = "Group by (category, domain, paradigm)") String groupBy,
                @Option(names = "--format", description = "Output format (table, json)") String format
        ) {
            // Implementation to show work item distribution
        }
        
        @Command(name = "cognitive-load", description = "Show cognitive load report")
        public void cognitiveLoad(
                @Option(names = "--team", description = "Team ID") String team,
                @Option(names = "--assignee", description = "Assignee") String assignee,
                @Option(names = "--format", description = "Output format (table, json)") String format
        ) {
            // Implementation to show cognitive load report
        }
        
        @Command(name = "milestone-progress", description = "Show milestone progress")
        public void milestoneProgress(
                @Option(names = "--milestone", required = true, description = "Milestone ID") String milestone,
                @Option(names = "--format", description = "Output format (table, json)") String format
        ) {
            // Implementation to show milestone progress
        }
    }
    
    @Override
    public void run() {
        // Default behavior when no subcommand is specified
        System.out.println("Use a subcommand: list, create, milestone, vocabulary, report");
    }
}
```

### Phase 4: Documentation and Testing

#### Task 4.1: Update Unified Work Management Documentation

Enhance the existing documentation with:

1. **Setup and Configuration Guide**:
   - Document the CLI commands for setting up the Unified Work Management system
   - Provide examples of different configuration scenarios
   - Include vocabulary mapping setup instructions

2. **Best Practices**:
   - Document recommended approaches for categorizing work
   - Provide guidance on cognitive load management
   - Explain milestone tracking strategies

3. **Example Workflows**:
   - Document end-to-end examples of managing different work types
   - Show how to track progress toward POC goals
   - Demonstrate cross-functional reporting

#### Task 4.2: Create Unit Tests

Create comprehensive unit tests for all new functionality:

1. **Model Tests**:
   - Tests for `UnifiedWorkItemRecord` with new fields
   - Tests for `VocabularyMap`
   - Tests for `Milestone`

2. **Service Tests**:
   - Tests for enhanced `UnifiedWorkItemService`
   - Tests for `VocabularyMapService`
   - Tests for `MilestoneService`

3. **Command Tests**:
   - Tests for `UnifiedWorkItemCommand` and subcommands

## 6. Milestone Tracking

### Milestone 1: Model Enhancement (Week 1)
- [ ] Enhanced UnifiedWorkItem model
- [ ] Vocabulary mapping model
- [ ] Milestone tracking model
- [ ] Repository implementations

### Milestone 2: Service Enhancement (Week 2)
- [ ] VocabularyMapService implementation
- [ ] MilestoneService implementation
- [ ] Enhanced UnifiedWorkItemService
- [ ] Reporting services

### Milestone 3: UI and CLI (Week 3)
- [ ] CLI commands for unified work management
- [ ] CLI commands for vocabulary mapping
- [ ] CLI commands for milestone tracking
- [ ] PUI components for unified work

### Milestone 4: Documentation and Testing (Week 4)
- [ ] Comprehensive documentation
- [ ] Unit tests
- [ ] Component tests
- [ ] Integration tests
- [ ] Sample data and examples

## 7. Conclusion

The Unified Work Management System will provide a cohesive approach to managing all work types while respecting their unique characteristics. This implementation plan outlines the steps to enhance the existing foundation with vocabulary mapping, milestone tracking, and improved reporting capabilities.

By following this plan, we'll deliver a system that eliminates traditional boundaries between work types, enables cross-functional collaboration, and provides clear visibility into progress toward POC goals.