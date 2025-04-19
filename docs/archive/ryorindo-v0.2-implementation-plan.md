# Ryorin-do v0.2 Implementation Plan

This document outlines the architectural plan for implementing Ryorin-do v0.2 principles into the Rinna system.

## 1. Enhanced Domain Model: WorkItem

The primary focus is upgrading the `WorkItem` interface to support the comprehensive structure required by Ryorin-do v0.2:

```java
package org.rinna.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced WorkItem interface supporting Ryorin-do v0.2 principles
 */
public interface WorkItem {
    // Existing fields
    UUID getId();                     // WorkItemID in Ryorin-do spec
    String getTitle();                // Title in Ryorin-do spec
    String getDescription();          // Description in Ryorin-do spec
    WorkflowState getStatus();        // Status in Ryorin-do spec
    Priority getPriority();           // Priority in Ryorin-do spec
    String getAssignee();             // Part of Assignee(s) in Ryorin-do spec
    Instant getCreatedAt();           // CreatedDate in Ryorin-do spec
    Instant getUpdatedAt();           // For tracking changes
    
    // New fields for Ryorin-do v0.2
    Optional<String> getCynefinDomain();      // CYNEFIND Domain in spec
    Optional<String> getWorkParadigm();       // Work Paradigm in spec
    List<String> getAssignees();              // Enhanced Assignee(s) support
    Optional<Instant> getDueDate();           // DueDate in spec
    Optional<Double> getEstimatedEffort();    // EstimatedEffort in spec
    Optional<Double> getActualEffort();       // ActualEffort in spec
    Optional<String> getOutcome();            // Outcome in spec
    List<String> getKeyResults();             // KeyResults in spec
    List<UUID> getDependencies();             // Dependencies in spec
    List<UUID> getRelatedItems();             // RelatedItems in spec
    Optional<Integer> getCognitiveLoadAssessment(); // CognitiveLoadAssessment
    List<String> getAiRecommendations();      // AIRecommendations in spec
    List<String> getKnowledgeLinks();         // KnowledgeLinks in spec
    List<String> getAttachments();            // Attachments in spec
    List<Comment> getComments();              // Notes/Comments in spec
    
    // Workstream integration
    List<UUID> getWorkstreamIds();            // Associated workstreams
    Optional<Integer> getAllocation();        // Resource allocation percentage
    Optional<String> getCategory();           // Work item category (PROD, DEV, etc.)
}
```

## 2. Implementation: WorkItemRecord

Create an immutable record implementation:

```java
package org.rinna.domain.model;

/**
 * Enhanced immutable record implementation supporting Ryorin-do v0.2
 */
public record WorkItemRecord(
    UUID id,
    String title,
    String description,
    WorkflowState status,
    Priority priority,
    String assignee,
    Instant createdAt,
    Instant updatedAt,
    UUID parentId,
    UUID projectId,
    String visibility,
    boolean localOnly,
    
    // Ryorin-do v0.2 specific fields
    String cynefinDomain,
    String workParadigm,
    List<String> assignees,
    Instant dueDate,
    Double estimatedEffort,
    Double actualEffort,
    String outcome,
    List<String> keyResults,
    List<UUID> dependencies,
    List<UUID> relatedItems,
    Integer cognitiveLoadAssessment,
    List<String> aiRecommendations,
    List<String> knowledgeLinks,
    List<String> attachments,
    List<Comment> comments,
    
    // Workstream fields
    List<UUID> workstreamIds,
    Integer allocation,
    String category
) implements WorkItem {
    // With methods for immutable updates and support for 
    // Ryorin-do v0.2 field manipulations
}
```

## 3. Comment Model

For threaded discussions:

```java
package org.rinna.domain.model;

public record Comment(
    UUID id,
    String content,
    String author,
    Instant createdAt,
    UUID parentCommentId
) {}
```

## 4. Enhanced Repository Interface

```java
package org.rinna.domain.repository;

public interface ItemRepository {
    // Existing methods...
    
    // CYNEFIN specific queries
    List<WorkItem> findByCynefinDomain(String domain);
    
    // Work paradigm queries
    List<WorkItem> findByWorkParadigm(String paradigm);
    
    // Cognitive load related queries
    List<WorkItem> findByCognitiveLoadAbove(int threshold);
    List<WorkItem> findByCognitiveLoadBelow(int threshold);
    
    // Outcome-based queries
    List<WorkItem> findByOutcomeContaining(String pattern);
    List<WorkItem> findByCompletedKeyResults(int minCompleted);
    
    // Knowledge management
    boolean addKnowledgeLink(UUID itemId, String link);
    boolean removeKnowledgeLink(UUID itemId, String link);
    
    // AI integration
    boolean updateAiRecommendations(UUID itemId, List<String> recommendations);
    
    // Enhanced searching capability
    List<WorkItem> searchByMultipleFields(Map<String, Object> criteria);
}
```

## 5. Updated Service Interface

```java
package org.rinna.domain.service;

public interface ItemService {
    // Existing methods...
    
    // Ryorin-do v0.2 specific methods
    
    // CYNEFIN domain management
    WorkItem updateCynefinDomain(UUID id, String domain);
    
    // Outcome-oriented management
    WorkItem defineOutcome(UUID id, String outcome);
    WorkItem addKeyResult(UUID id, String keyResult);
    WorkItem markKeyResultComplete(UUID id, int keyResultIndex);
    
    // Cognitive load management
    WorkItem assessCognitiveLoad(UUID id, int loadLevel);
    List<WorkItem> findTasksExceedingCognitiveLoad(String assignee, int threshold);
    
    // AI augmentation
    WorkItem updateAiRecommendations(UUID id, List<String> recommendations);
    Map<String, Object> generateAiWorkInsights(UUID id);
    
    // Knowledge management
    WorkItem addKnowledgeLink(UUID id, String link);
    WorkItem removeKnowledgeLink(UUID id, String link);
    
    // Dependencies and relationships
    boolean addDependency(UUID itemId, UUID dependencyId);
    boolean removeDependency(UUID itemId, UUID dependencyId);
    boolean addRelatedItem(UUID itemId, UUID relatedId);
    boolean removeRelatedItem(UUID itemId, UUID relatedId);
    
    // Comments and collaboration
    Comment addComment(UUID itemId, String content, String author);
    Comment replyToComment(UUID itemId, UUID parentCommentId, String content, String author);
}
```

## 6. Integration with Project/Workstream Architecture

The WorkItem model now links to both projects and workstreams:

```java
// In WorkItem interface:
Optional<UUID> getProjectId();        // Project association
List<UUID> getWorkstreamIds();        // Workstream associations 
```

This allows a work item to belong to exactly one project but potentially multiple workstreams, reflecting the reality described in your spec where:

1. A project is a classification of work that people participate in with varying levels of focus
2. A workstream is a collection of work items that belong to various projects and people

## 7. CYNEFIN Framework Support

```java
package org.rinna.domain.model;

/**
 * Represents the CYNEFIN domains for context-aware management
 */
public enum CynefinDomain {
    OBVIOUS("Obvious", "Clear cause and effect relationships, best practices apply"),
    COMPLICATED("Complicated", "Cause and effect relationships require analysis, good practices apply"),
    COMPLEX("Complex", "Cause and effect can only be understood in retrospect, emergent practices"),
    CHAOTIC("Chaotic", "No clear cause and effect relationships, novel practices"),
    DISORDER("Disorder", "Domain is unclear or in transition");
    
    private final String name;
    private final String description;
    
    // Constructor and accessor methods
}
```

## 8. Work Paradigm Support

```java
package org.rinna.domain.model;

/**
 * Represents different work management paradigms
 */
public enum WorkParadigm {
    TASK("Task", "Discrete unit of work with clear definition"),
    STORY("Story", "User-focused description of functionality"),
    EPIC("Epic", "Collection of related stories or features"),
    INITIATIVE("Initiative", "Strategic objective spanning multiple epics"),
    EXPERIMENT("Experiment", "Learning-focused work with defined hypotheses"),
    SPIKE("Spike", "Time-boxed investigation or research"),
    INCIDENT("Incident", "Problem resolution work"),
    MAINTENANCE("Maintenance", "Ongoing work to sustain existing capabilities");
    
    private final String name;
    private final String description;
    
    // Constructor and accessor methods
}
```

## 9. AI Integration Service

```java
package org.rinna.domain.service;

import org.rinna.domain.model.WorkItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for AI augmentation of work management
 */
public interface AIWorkAugmentationService {
    /**
     * Assesses cognitive load for a work item
     */
    int assessCognitiveLoad(WorkItem workItem);
    
    /**
     * Generates recommendations for a work item
     */
    List<String> generateRecommendations(WorkItem workItem);
    
    /**
     * Recommends work paradigm based on item characteristics
     */
    String recommendWorkParadigm(WorkItem workItem);
    
    /**
     * Suggests CYNEFIN domain categorization
     */
    String suggestCynefinDomain(WorkItem workItem);
    
    /**
     * Analyzes work item text to extract potential key results
     */
    List<String> suggestKeyResults(String outcomeText);
    
    /**
     * Identifies potential dependencies between work items
     */
    List<UUID> identifyPotentialDependencies(UUID workItemId);
    
    /**
     * Generates insights about work patterns
     */
    Map<String, Object> generateWorkInsights(List<WorkItem> workItems);
}
```

## 10. Implementation: DefaultAIWorkAugmentationService

```java
package org.rinna.adapter.service;

import org.rinna.domain.model.WorkItem;
import org.rinna.domain.service.AIWorkAugmentationService;

/**
 * Default implementation of AI augmentation service
 */
public class DefaultAIWorkAugmentationService implements AIWorkAugmentationService {
    // Implementation methods that provide AI-based analysis and recommendations
    // Initially, this could use simple heuristics and be expanded with
    // real AI/ML capabilities in future versions
}
```

## 11. Database Schema Migration

The implementation will require database schema updates:

1. New columns for all Ryorin-do v0.2 fields in the work_items table
2. New tables for:
   - comments (for threaded discussions)
   - knowledge_links (many-to-many relationship)
   - key_results (with completion status)
   - work_item_dependencies (for tracking dependencies)
   - work_item_relationships (for related items)
   - work_item_workstreams (many-to-many relationship)

## 12. API Enhancements

The REST API will need new endpoints to expose the Ryorin-do v0.2 functionality:

1. CYNEFIN domain management endpoints
2. Work paradigm endpoints
3. Cognitive load assessment endpoints
4. Outcome and key results endpoints
5. Knowledge management endpoints
6. AI recommendation endpoints

## 13. Front-End Considerations

While outside the scope of this architecture plan, the front-end will need to be enhanced to:

1. Display CYNEFIN domains and work paradigms
2. Show cognitive load assessments visually
3. Track outcomes and key results
4. Present AI recommendations
5. Manage knowledge links
6. Support threaded comments

## 14. Implementation Phases

### Phase 1: Core Model Enhancement
- Implement enhanced WorkItem interface
- Create CynefinDomain and WorkParadigm enums
- Update WorkItemRecord implementation
- Create Comment model

### Phase 2: Repository and Service Layer
- Enhance ItemRepository interface
- Update ItemService interface
- Create AIWorkAugmentationService interface
- Implement DefaultAIWorkAugmentationService

### Phase 3: Persistence and Testing
- Database schema migrations
- Update in-memory implementations for testing
- Create acceptance tests for new functionality

### Phase 4: Integration and API
- Integrate with Project/Workstream architecture
- Implement REST API enhancements
- Create administrative CLI tools for Ryorin-do v0.2

## 15. Migration Strategy

For existing work items, we will need a migration strategy:

1. Default values for required Ryorin-do v0.2 fields
2. AI-assisted suggestion of appropriate CYNEFIN domains and work paradigms
3. Tools for bulk-updating items with appropriate values
4. Backward compatibility for clients not yet upgraded to support v0.2 fields