/*
 * Mock client for testing the API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package test

import (
	"context"

	"github.com/google/uuid"
	"github.com/heymumford/rinna/api/internal/client"
	"github.com/heymumford/rinna/api/internal/models"
	"github.com/heymumford/rinna/api/pkg/config"
)

// MockJavaClient is a test implementation of the JavaClient interface
type MockJavaClient struct {
	*client.JavaClient
}

// NewMockJavaClient creates a new MockJavaClient for testing
func NewMockJavaClient() *MockJavaClient {
	javaConfig := &config.JavaServiceConfig{
		Host: "localhost",
		Port: 8080,
		Endpoints: map[string]string{
			"workitems": "/api/workitems",
			"health":    "/health",
		},
	}
	return &MockJavaClient{JavaClient: client.NewJavaClient(javaConfig)}
}

// CreateWorkItem is a mock implementation that returns a simulated work item
func (m *MockJavaClient) CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error) {
	// Create a mock work item based on the request
	return &models.WorkItem{
		ID:          uuid.New(),
		Title:       request.Title,
		Description: request.Description,
		Type:        request.Type,
		Priority:    request.Priority,
		Status:      models.WorkflowStateFound,
		ProjectID:   request.ProjectID,
		Metadata:    request.Metadata,
	}, nil
}

// ValidateToken is a mock implementation that returns a project ID for testing
func (m *MockJavaClient) ValidateToken(ctx context.Context, token string) (string, error) {
	// For testing, just return a fixed project ID
	return "test-project", nil
}

// GetWebhookSecret is a mock implementation that returns a fixed secret for testing
func (m *MockJavaClient) GetWebhookSecret(ctx context.Context, projectKey, source string) (string, error) {
	// Return test secret
	return "gh-webhook-secret-1234", nil
}