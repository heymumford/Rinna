/*
 * Work item models for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package models

import (
	"time"

	"github.com/google/uuid"
)

// WorkItemType represents the type of a work item
type WorkItemType string

// WorkItemType enum
const (
	WorkItemTypeBug     WorkItemType = "BUG"
	WorkItemTypeFeature WorkItemType = "FEATURE"
	WorkItemTypeChore   WorkItemType = "CHORE"
)

// Priority represents the priority of a work item
type Priority string

// Priority enum
const (
	PriorityLow    Priority = "LOW"
	PriorityMedium Priority = "MEDIUM"
	PriorityHigh   Priority = "HIGH"
)

// WorkflowState represents the state of a work item in the workflow
type WorkflowState string

// WorkflowState enum
const (
	WorkflowStateFound   WorkflowState = "FOUND"
	WorkflowStateTriaged WorkflowState = "TRIAGED"
	WorkflowStateInDev   WorkflowState = "IN_DEV"
	WorkflowStateTesting WorkflowState = "TESTING"
	WorkflowStateDone    WorkflowState = "DONE"
	WorkflowStateClosed  WorkflowState = "CLOSED"
)

// WorkItem represents a work item in the Rinna system
type WorkItem struct {
	ID          uuid.UUID              `json:"id"`
	ExternalID  string                 `json:"externalId,omitempty"`
	Title       string                 `json:"title"`
	Description string                 `json:"description,omitempty"`
	Type        WorkItemType           `json:"type"`
	Priority    Priority               `json:"priority"`
	Status      WorkflowState          `json:"status"`
	Assignee    string                 `json:"assignee,omitempty"`
	ProjectID   string                 `json:"projectId,omitempty"`
	ParentID    uuid.UUID              `json:"parentId,omitempty"`
	Metadata    map[string]string      `json:"metadata,omitempty"`
	CreatedAt   time.Time              `json:"createdAt"`
	UpdatedAt   time.Time              `json:"updatedAt"`
}

// WorkItemListResponse represents a paginated list of work items
type WorkItemListResponse struct {
	Items      []WorkItem `json:"items"`
	TotalCount int        `json:"totalCount"`
	Page       int        `json:"page"`
	PageSize   int        `json:"pageSize"`
}

// WorkItemCreateRequest represents a request to create a new work item
type WorkItemCreateRequest struct {
	Title       string                 `json:"title"`
	Description string                 `json:"description,omitempty"`
	Type        WorkItemType           `json:"type"`
	Priority    Priority               `json:"priority"`
	ProjectID   string                 `json:"projectId,omitempty"`
	ParentID    string                 `json:"parentId,omitempty"`
	Metadata    map[string]string      `json:"metadata,omitempty"`
}

// WorkItemUpdateRequest represents a request to update an existing work item
type WorkItemUpdateRequest struct {
	Title       *string                `json:"title,omitempty"`
	Description *string                `json:"description,omitempty"`
	Type        *WorkItemType          `json:"type,omitempty"`
	Priority    *Priority              `json:"priority,omitempty"`
	Status      *WorkflowState         `json:"status,omitempty"`
	Assignee    *string                `json:"assignee,omitempty"`
	Metadata    map[string]string      `json:"metadata,omitempty"`
}

// WorkItemTransitionRequest represents a request to transition a work item
type WorkItemTransitionRequest struct {
	ToState WorkflowState `json:"toState"`
	Comment string        `json:"comment,omitempty"`
}