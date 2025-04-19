/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.examples.model;

import org.rinna.cli.model.WorkItem;

/**
 * Represents a relationship between work items.
 * This class is used to model different types of relationships like parent-child,
 * dependency, relates-to, etc.
 */
public class WorkItemRelationship {
    
    /**
     * Type of relationship between work items.
     */
    public enum RelationshipType {
        PARENT("Parent"),
        CHILD("Child"),
        BLOCKS("Blocks"),
        BLOCKED_BY("Blocked By"),
        RELATED("Related To"),
        DUPLICATES("Duplicates"),
        DUPLICATED_BY("Duplicated By"),
        FOLLOWS("Follows"),
        PRECEDED_BY("Preceded By");
        
        private final String displayName;
        
        RelationshipType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    private WorkItem sourceItem;
    private WorkItem targetItem;
    private RelationshipType relationshipType;
    private String description;
    
    /**
     * Creates a new work item relationship.
     * 
     * @param sourceItem The source work item
     * @param targetItem The target work item
     * @param relationshipType The type of relationship
     */
    public WorkItemRelationship(WorkItem sourceItem, WorkItem targetItem, RelationshipType relationshipType) {
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;
        this.relationshipType = relationshipType;
    }
    
    /**
     * Creates a new work item relationship with a description.
     * 
     * @param sourceItem The source work item
     * @param targetItem The target work item
     * @param relationshipType The type of relationship
     * @param description A description of the relationship
     */
    public WorkItemRelationship(WorkItem sourceItem, WorkItem targetItem, RelationshipType relationshipType, String description) {
        this(sourceItem, targetItem, relationshipType);
        this.description = description;
    }
    
    /**
     * Gets the source work item.
     * 
     * @return The source work item
     */
    public WorkItem getSourceItem() {
        return sourceItem;
    }
    
    /**
     * Gets the target work item.
     * 
     * @return The target work item
     */
    public WorkItem getTargetItem() {
        return targetItem;
    }
    
    /**
     * Gets the relationship type.
     * 
     * @return The relationship type
     */
    public RelationshipType getRelationshipType() {
        return relationshipType;
    }
    
    /**
     * Gets the relationship description.
     * 
     * @return The relationship description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the relationship description.
     * 
     * @param description The relationship description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Creates a complementary relationship from the target to the source.
     * For example, if A is PARENT of B, then B is CHILD of A.
     * 
     * @return The complementary relationship
     */
    public WorkItemRelationship createComplementaryRelationship() {
        RelationshipType complementaryType = getComplementaryRelationshipType(relationshipType);
        return new WorkItemRelationship(targetItem, sourceItem, complementaryType, description);
    }
    
    /**
     * Gets the complementary relationship type.
     * 
     * @param type The original relationship type
     * @return The complementary relationship type
     */
    private RelationshipType getComplementaryRelationshipType(RelationshipType type) {
        switch (type) {
            case PARENT:
                return RelationshipType.CHILD;
            case CHILD:
                return RelationshipType.PARENT;
            case BLOCKS:
                return RelationshipType.BLOCKED_BY;
            case BLOCKED_BY:
                return RelationshipType.BLOCKS;
            case DUPLICATES:
                return RelationshipType.DUPLICATED_BY;
            case DUPLICATED_BY:
                return RelationshipType.DUPLICATES;
            case FOLLOWS:
                return RelationshipType.PRECEDED_BY;
            case PRECEDED_BY:
                return RelationshipType.FOLLOWS;
            case RELATED:
            default:
                return RelationshipType.RELATED;
        }
    }
    
    @Override
    public String toString() {
        return relationshipType.getDisplayName() + ": " + targetItem.getId() + " - " + targetItem.getTitle();
    }
}