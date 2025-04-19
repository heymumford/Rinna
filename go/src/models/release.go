/*
 * Release models for the Rinna API
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

// ReleaseStatus represents the status of a release
type ReleaseStatus string

// ReleaseStatus enum
const (
	ReleaseStatusPlanned   ReleaseStatus = "PLANNED"
	ReleaseStatusInProgress ReleaseStatus = "IN_PROGRESS"
	ReleaseStatusReleased   ReleaseStatus = "RELEASED"
	ReleaseStatusCancelled  ReleaseStatus = "CANCELLED"
)

// Release represents a release in the Rinna system
type Release struct {
	ID          uuid.UUID         `json:"id"`
	Name        string            `json:"name"`
	Version     string            `json:"version"`
	Description string            `json:"description,omitempty"`
	Status      ReleaseStatus     `json:"status"`
	StartDate   string            `json:"startDate,omitempty"`
	ReleaseDate string            `json:"releaseDate,omitempty"`
	ProjectKey  string            `json:"projectKey,omitempty"`
	Metadata    map[string]string `json:"metadata,omitempty"`
	CreatedAt   time.Time         `json:"createdAt"`
	UpdatedAt   time.Time         `json:"updatedAt"`
}

// ReleaseCreateRequest represents a request to create a new release
type ReleaseCreateRequest struct {
	Name        string            `json:"name"`
	Version     string            `json:"version"`
	Description string            `json:"description,omitempty"`
	Status      ReleaseStatus     `json:"status"`
	StartDate   string            `json:"startDate,omitempty"`
	ReleaseDate string            `json:"releaseDate,omitempty"`
	ProjectKey  string            `json:"projectKey,omitempty"`
	Metadata    map[string]string `json:"metadata,omitempty"`
}

// ReleaseUpdateRequest represents a request to update an existing release
type ReleaseUpdateRequest struct {
	Name        *string            `json:"name,omitempty"`
	Description *string            `json:"description,omitempty"`
	Status      *ReleaseStatus     `json:"status,omitempty"`
	StartDate   *string            `json:"startDate,omitempty"`
	ReleaseDate *string            `json:"releaseDate,omitempty"`
	Metadata    map[string]string  `json:"metadata,omitempty"`
}

// ReleaseListResponse represents a paginated list of releases
type ReleaseListResponse struct {
	Items      []Release `json:"items"`
	TotalCount int       `json:"totalCount"`
	Page       int       `json:"page"`
	PageSize   int       `json:"pageSize"`
}