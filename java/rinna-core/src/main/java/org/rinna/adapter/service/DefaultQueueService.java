/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.domain.model.DefaultWorkQueue;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemMetadata;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkQueue;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.repository.MetadataRepository;
import org.rinna.domain.repository.QueueRepository;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.QueueService;

/**
 * Default implementation of the QueueService interface.
 */
public class DefaultQueueService implements QueueService {
    private final QueueRepository queueRepository;
    private final ItemService itemService;
    private final MetadataRepository metadataRepository;
    private UUID defaultQueueId;

    /**
     * Creates a new DefaultQueueService with the given repositories and services.
     * 
     * @param queueRepository the queue repository
     * @param itemService the item service
     * @param metadataRepository the metadata repository
     */
    public DefaultQueueService(QueueRepository queueRepository, ItemService itemService, 
                              MetadataRepository metadataRepository) {
        this.queueRepository = queueRepository;
        this.itemService = itemService;
        this.metadataRepository = metadataRepository;

        // Defer creation of the default queue to avoid this-escape
        this.defaultQueueId = null;
    }

    /**
     * Initializes the defaultQueueId if it hasn't been set yet.
     * This is to avoid this-escape during construction.
     */
    private synchronized UUID getOrCreateDefaultQueueId() {
        if (defaultQueueId == null) {
            // Use internal method to avoid this-escape
            WorkQueue defaultQueue = findOrCreateDefaultQueue();
            return defaultQueue.getId();
        }
        return defaultQueueId;
    }

    @Override
    public WorkQueue createQueue(String name, String description) {
        WorkQueue queue = new DefaultWorkQueue(name, description);
        return queueRepository.save(queue);
    }

    @Override
    public void addWorkItemToQueue(UUID queueId, UUID workItemId) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        WorkItem workItem = itemService.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + workItemId));

        queue.addItem(workItem);
        queueRepository.save(queue);
    }

    @Override
    public boolean removeWorkItemFromQueue(UUID queueId, UUID workItemId) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        boolean removed = queue.removeItem(workItemId);
        if (removed) {
            queueRepository.save(queue);
        }

        return removed;
    }

    @Override
    public Optional<WorkItem> getNextWorkItem(UUID queueId) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        return queue.getNextItem();
    }

    @Override
    public List<WorkItem> getQueueItems(UUID queueId) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        return queue.getItems();
    }

    @Override
    public List<WorkItem> getQueueItemsByType(UUID queueId, WorkItemType type) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        return queue.getItemsByType(type);
    }

    @Override
    public List<WorkItem> getQueueItemsByState(UUID queueId, WorkflowState state) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        return queue.getItemsByState(state);
    }

    @Override
    public List<WorkItem> getQueueItemsByPriority(UUID queueId, Priority priority) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        return queue.getItemsByPriority(priority);
    }

    @Override
    public List<WorkItem> getQueueItemsByAssignee(UUID queueId, String assignee) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        return queue.getItemsByAssignee(assignee);
    }

    @Override
    public void reprioritizeQueue(UUID queueId) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        queue.reprioritize();
        queueRepository.save(queue);
    }

    @Override
    public void reprioritizeQueueWithWeights(UUID queueId, Map<String, Integer> weights) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        // Get all items from the queue
        List<WorkItem> items = queue.getItems();

        // Get weight values from the map with defaults
        int priorityWeight = weights.getOrDefault("priority", 10);
        int typeWeight = weights.getOrDefault("type", 5);
        int ageWeight = weights.getOrDefault("age", 2);
        int urgencyWeight = weights.getOrDefault("urgent", 20);

        // Custom prioritization using a Comparator
        items.sort(Comparator
                .comparingInt((WorkItem item) -> getPriorityValue(item.getPriority()) * priorityWeight)
                .thenComparingInt(item -> getTypeWeight(item.getType()) * typeWeight)
                .thenComparing(WorkItem::getCreatedAt, (a, b) -> ageWeight * a.compareTo(b))
                .thenComparing(item -> isUrgent(item.getId()) ? -urgencyWeight : 0)
        );

        // Update the queue with the new order
        queue.getItems().forEach(item -> queue.removeItem(item.getId()));
        items.forEach(queue::addItem);

        queueRepository.save(queue);
    }

    private static final Map<WorkItemType, Integer> TYPE_WEIGHTS = Map.of(
        WorkItemType.BUG, 0,
        WorkItemType.FEATURE, 1,
        WorkItemType.CHORE, 2,
        WorkItemType.GOAL, 3
    );

    private int getTypeWeight(WorkItemType type) {
        return TYPE_WEIGHTS.getOrDefault(type, 4);
    }

    /**
     * Gets a numeric value for a priority level.
     * Higher priority = lower value (for sorting).
     * 
     * @param priority the priority
     * @return the numeric value
     */
    private int getPriorityValue(Priority priority) {
        if (priority == null) {
            return 3; // Default to lowest priority
        }

        switch (priority) {
            case HIGH:
                return 0;
            case MEDIUM:
                return 1;
            case LOW:
                return 2;
            default:
                return 3;
        }
    }

    @Override
    public void reprioritizeQueueByCapacity(UUID queueId, int teamCapacity) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        // Get all items in priority order
        List<WorkItem> items = queue.getItems();

        // Total story points allocated so far
        int allocatedPoints = 0;

        // Mark items as "capacity_included" if they fit within capacity
        for (WorkItem item : items) {
            int points = getStoryPoints(item.getId());
            String included = (allocatedPoints + points <= teamCapacity) ? "true" : "false";

            metadataRepository.save(new WorkItemMetadata(item.getId(), "capacity_included", included));

            if ("true".equals(included)) {
                allocatedPoints += points;
            }
        }

        // Reprioritize queue with capacity-included items first
        items.sort(Comparator
                .<WorkItem>comparingInt(item -> {
                    return metadataRepository
                            .findByWorkItemIdAndKey(item.getId(), "capacity_included")
                            .filter(meta -> "true".equals(meta.getValue()))
                            .isPresent() ? 0 : 1;
                })
                .thenComparing(WorkItem::getPriority)
                .thenComparing(item -> getTypeWeight(item.getType()))
                .thenComparing(WorkItem::getCreatedAt));

        // Update the queue with the new order
        queue.getItems().forEach(item -> queue.removeItem(item.getId()));
        items.forEach(queue::addItem);

        queueRepository.save(queue);
    }

    @Override
    public void activateQueue(UUID queueId) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        if (queue instanceof DefaultWorkQueue defaultQueue) {
            defaultQueue.setActive(true);
            queueRepository.save(queue);
        } else {
            throw new UnsupportedOperationException("Queue implementation does not support activation");
        }
    }

    @Override
    public void deactivateQueue(UUID queueId) {
        WorkQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Queue not found: " + queueId));

        if (queue instanceof DefaultWorkQueue defaultQueue) {
            defaultQueue.setActive(false);
            queueRepository.save(queue);
        } else {
            throw new UnsupportedOperationException("Queue implementation does not support deactivation");
        }
    }

    @Override
    public Optional<WorkQueue> findById(UUID id) {
        return queueRepository.findById(id);
    }

    @Override
    public Optional<WorkQueue> findByName(String name) {
        return queueRepository.findByName(name);
    }

    @Override
    public List<WorkQueue> findAllQueues() {
        return queueRepository.findAll();
    }

    @Override
    public List<WorkQueue> findActiveQueues() {
        return queueRepository.findByActive(true);
    }

    @Override
    public WorkItem submitProductionIncident(String title, String description) {
        // Create the work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .description(description)
                .type(WorkItemType.BUG)
                .priority(Priority.HIGH)
                .build();

        WorkItem workItem = itemService.create(request);

        // Set metadata
        metadataRepository.save(new WorkItemMetadata(workItem.getId(), "source", "incident"));
        metadataRepository.save(new WorkItemMetadata(workItem.getId(), "urgent", "true"));

        // Add to the default queue
        UUID queueId = defaultQueueId;
        if (queueId == null) {
            queueId = getOrCreateDefaultQueueId();
            defaultQueueId = queueId; // Cache the ID
        }
        addWorkItemToQueue(queueId, workItem.getId());

        return workItem;
    }

    @Override
    public WorkItem submitFeatureRequest(String title, String description, Priority priority) {
        Priority effectivePriority = priority != null ? priority : Priority.MEDIUM;

        // Create the work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .description(description)
                .type(WorkItemType.FEATURE)
                .priority(effectivePriority)
                .build();

        WorkItem workItem = itemService.create(request);

        // Set metadata
        metadataRepository.save(new WorkItemMetadata(workItem.getId(), "source", "feature_request"));

        // Add to the default queue
        UUID queueId = defaultQueueId;
        if (queueId == null) {
            queueId = getOrCreateDefaultQueueId();
            defaultQueueId = queueId; // Cache the ID
        }
        addWorkItemToQueue(queueId, workItem.getId());

        return workItem;
    }

    @Override
    public WorkItem submitTechnicalTask(String title, String description, Priority priority) {
        Priority effectivePriority = priority != null ? priority : Priority.LOW;

        // Create the work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .description(description)
                .type(WorkItemType.CHORE)
                .priority(effectivePriority)
                .build();

        WorkItem workItem = itemService.create(request);

        // Set metadata
        metadataRepository.save(new WorkItemMetadata(workItem.getId(), "source", "technical_task"));

        // Add to the default queue
        UUID queueId = defaultQueueId;
        if (queueId == null) {
            queueId = getOrCreateDefaultQueueId();
            defaultQueueId = queueId; // Cache the ID
        }
        addWorkItemToQueue(queueId, workItem.getId());

        return workItem;
    }

    @Override
    public WorkItem submitChildWorkItem(String title, WorkItemType type, UUID parentId, 
                                       String description, Priority priority) {
        // Check if parent exists
        WorkItem parent = itemService.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent work item not found: " + parentId));

        // Check if parent-child relationship is valid
        if (!canHaveChildOfType(parent.getType(), type)) {
            throw new IllegalArgumentException(
                    "Invalid parent-child relationship: " + parent.getType() + " -> " + type);
        }

        // Inherit priority from parent if not specified
        Priority effectivePriority = priority;
        if (effectivePriority == null) {
            effectivePriority = parent.getPriority();
        }

        // Create the work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .description(description)
                .type(type)
                .priority(effectivePriority)
                .parentId(parentId)
                .build();

        WorkItem workItem = itemService.create(request);

        // Set metadata
        metadataRepository.save(new WorkItemMetadata(workItem.getId(), "source", "child_item"));

        // Add to the default queue
        UUID queueId = defaultQueueId;
        if (queueId == null) {
            queueId = getOrCreateDefaultQueueId();
            defaultQueueId = queueId; // Cache the ID
        }
        addWorkItemToQueue(queueId, workItem.getId());

        return workItem;
    }

    /**
     * Internal method to find or create the default queue.
     * This avoids potential this-escape issues in the constructor.
     * 
     * @return the default queue
     */
    private WorkQueue findOrCreateDefaultQueue() {
        Optional<WorkQueue> existing = queueRepository.findByName("Default Queue");
        if (existing.isPresent()) {
            return existing.get();
        }

        return queueRepository.save(new DefaultWorkQueue("Default Queue", 
                "Default queue for all work items"));
    }

    @Override
    public WorkQueue createDefaultQueue() {
        WorkQueue queue = findOrCreateDefaultQueue();
        // Make sure we update our default queue ID
        if (defaultQueueId == null) {
            defaultQueueId = queue.getId();
        }
        return queue;
    }

    @Override
    public WorkQueue getDefaultQueue() {
        UUID queueId = defaultQueueId;
        if (queueId == null) {
            queueId = getOrCreateDefaultQueueId();
            defaultQueueId = queueId; // Cache the ID
        }
        final UUID finalQueueId = queueId;
        return queueRepository.findById(finalQueueId)
                .orElseThrow(() -> new IllegalStateException("Default queue not found with ID: " + finalQueueId));
    }

    @Override
    public boolean isUrgent(UUID workItemId) {
        Optional<WorkItemMetadata> urgentMetadata = 
                metadataRepository.findByWorkItemIdAndKey(workItemId, "urgent");

        return urgentMetadata.isPresent() && "true".equals(urgentMetadata.get().getValue());
    }

    @Override
    public void setUrgent(UUID workItemId, boolean urgent) {
        itemService.findById(workItemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + workItemId));

        metadataRepository.save(new WorkItemMetadata(workItemId, "urgent", urgent ? "true" : "false"));

        // Reprioritize the default queue to account for the change
        UUID queueId = defaultQueueId;
        if (queueId == null) {
            queueId = getOrCreateDefaultQueueId();
            defaultQueueId = queueId; // Cache the ID
        }
        reprioritizeQueue(queueId);
    }

    @Override
    public List<WorkItem> findUrgentItems() {
        // Find all items with urgent=true metadata
        Map<UUID, String> urgentItemIds = new HashMap<>();
        metadataRepository.findAll().stream()
                .filter(meta -> "urgent".equals(meta.getKey()) && "true".equals(meta.getValue()))
                .forEach(meta -> urgentItemIds.put(meta.getWorkItemId(), meta.getValue()));

        // Get the actual WorkItem objects for these IDs
        return urgentItemIds.keySet().stream()
                .map(itemService::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Get the story points for a work item, defaulting to 1 if not specified.
     * 
     * @param workItemId the ID of the work item
     * @return the story points
     */
    private int getStoryPoints(UUID workItemId) {
        Optional<WorkItemMetadata> pointsMetadata = 
                metadataRepository.findByWorkItemIdAndKey(workItemId, "story_points");

        if (pointsMetadata.isPresent()) {
            try {
                return Integer.parseInt(pointsMetadata.get().getValue());
            } catch (NumberFormatException e) {
                return 1;
            }
        }

        return 1;
    }

    /**
     * Checks if a parent work item type can have a child of the specified type.
     * 
     * @param parentType the parent work item type
     * @param childType the child work item type
     * @return true if the parent can have a child of the specified type, false otherwise
     */
    private boolean canHaveChildOfType(WorkItemType parentType, WorkItemType childType) {
        // Implement simple validation logic based on work item type hierarchy
        switch (parentType) {
            case EPIC:
                // Epics can have features, tasks, or bugs as children
                return childType == WorkItemType.FEATURE || 
                       childType == WorkItemType.TASK || 
                       childType == WorkItemType.BUG;
            case FEATURE:
                // Features can have tasks or bugs as children
                return childType == WorkItemType.TASK || 
                       childType == WorkItemType.BUG;
            case GOAL:
                // Goals can have epics or features as children
                return childType == WorkItemType.EPIC || 
                       childType == WorkItemType.FEATURE;
            case TASK:
            case BUG:
            case CHORE:
                // Tasks, bugs, and chores cannot have children
                return false;
            default:
                // Default to false for unknown types
                return false;
        }
    }
}
