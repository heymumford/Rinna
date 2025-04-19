/*
 * API client for Rinna services
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package client

import (
	"context"
	"fmt"
	"net/http"
	"time"

	"github.com/heymumford/rinna/api/internal/models"
	"github.com/heymumford/rinna/api/pkg/config"
)

// ProjectIDKey is the context key for the project ID
type ProjectIDKey struct{}

// UserIDKey is the context key for the user ID
type UserIDKey struct{}

// APIClient provides a simplified interface for interacting with the Rinna API
type APIClient struct {
	javaClient *JavaClient
	config     *config.RinnaConfig
}

// NewAPIClient creates a new API client
func NewAPIClient(cfg *config.RinnaConfig) (*APIClient, error) {
	if cfg == nil {
		var err error
		cfg, err = config.GetConfig()
		if err != nil {
			return nil, fmt.Errorf("failed to get configuration: %w", err)
		}
	}

	javaClient := NewJavaClient(&cfg.Java).WithFullConfig(cfg)

	// Initialize token manager
	if err := javaClient.InitializeTokenManager(); err != nil {
		return nil, fmt.Errorf("failed to initialize token manager: %w", err)
	}

	return &APIClient{
		javaClient: javaClient,
		config:     cfg,
	}, nil
}

// WithProjectID adds a project ID to the context
func WithProjectID(ctx context.Context, projectID string) context.Context {
	return context.WithValue(ctx, ProjectIDKey{}, projectID)
}

// WithUserID adds a user ID to the context
func WithUserID(ctx context.Context, userID string) context.Context {
	return context.WithValue(ctx, UserIDKey{}, userID)
}

// GetProject gets a project by key
func (c *APIClient) GetProject(ctx context.Context, key string) (*models.Project, error) {
	// Add projectID to the context for secure token retrieval
	ctx = context.WithValue(ctx, "projectID", key)
	
	return c.javaClient.GetProject(ctx, key)
}

// ListProjects lists all projects
func (c *APIClient) ListProjects(ctx context.Context, page, pageSize int, activeOnly bool) (*models.ProjectListResponse, error) {
	// For listing projects, we'd typically use a system token or admin token
	// Here we'll extract project ID from context if available
	projectID := ctx.Value(ProjectIDKey{})
	if projectID != nil {
		ctx = context.WithValue(ctx, "projectID", projectID.(string))
	}

	return c.javaClient.ListProjects(ctx, page, pageSize, activeOnly)
}

// CreateProject creates a new project
func (c *APIClient) CreateProject(ctx context.Context, request models.ProjectCreateRequest) (*models.Project, error) {
	// For creating projects, we'd typically use a system token or admin token
	// Here we'll extract project ID from context if available
	projectID := ctx.Value(ProjectIDKey{})
	if projectID != nil {
		ctx = context.WithValue(ctx, "projectID", projectID.(string))
	}

	return c.javaClient.CreateProject(ctx, request)
}

// GetWorkItem gets a work item by ID
func (c *APIClient) GetWorkItem(ctx context.Context, id string) (*models.WorkItem, error) {
	// Extract project ID from context
	projectID := ctx.Value(ProjectIDKey{})
	if projectID == nil {
		return nil, fmt.Errorf("project ID not provided in context")
	}

	ctx = context.WithValue(ctx, "projectID", projectID.(string))
	return c.javaClient.GetWorkItem(ctx, id)
}

// ListWorkItems lists all work items
func (c *APIClient) ListWorkItems(ctx context.Context, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	// Extract project ID from context
	projectID := ctx.Value(ProjectIDKey{})
	if projectID == nil {
		return nil, fmt.Errorf("project ID not provided in context")
	}

	ctx = context.WithValue(ctx, "projectID", projectID.(string))
	return c.javaClient.ListWorkItems(ctx, status, page, pageSize)
}

// GetProjectWorkItems gets work items for a project
func (c *APIClient) GetProjectWorkItems(ctx context.Context, projectKey string, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	// Use the provided project key for this operation
	ctx = context.WithValue(ctx, "projectID", projectKey)
	return c.javaClient.GetProjectWorkItems(ctx, projectKey, status, page, pageSize)
}

// CreateWorkItem creates a new work item
func (c *APIClient) CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error) {
	// Extract project ID from context
	projectID := ctx.Value(ProjectIDKey{})
	if projectID == nil {
		return nil, fmt.Errorf("project ID not provided in context")
	}

	ctx = context.WithValue(ctx, "projectID", projectID.(string))
	return c.javaClient.CreateWorkItem(ctx, request)
}

// GenerateToken generates a new API token
func (c *APIClient) GenerateToken(ctx context.Context, projectID, scope string, expirationDays int) (string, error) {
	// This would typically be an administrative operation requiring special permissions
	// For simplicity, we'll just pass the project ID in the context
	ctx = context.WithValue(ctx, "projectID", projectID)

	// Request a token using the token generation endpoint
	url := "/api/auth/token/generate"
	if c.config.Java.Endpoints != nil && c.config.Java.Endpoints["token_generate"] != "" {
		url = c.config.Java.Endpoints["token_generate"]
	}

	request := struct {
		ProjectID    string `json:"projectId"`
		Scope        string `json:"scope"`
		DurationDays int    `json:"durationDays"`
	}{
		ProjectID:    projectID,
		Scope:        scope,
		DurationDays: expirationDays,
	}

	var response struct {
		Token     string    `json:"token"`
		ExpiresAt time.Time `json:"expiresAt"`
	}

	err := c.javaClient.Request(ctx, http.MethodPost, url, request, &response)
	if err != nil {
		return "", fmt.Errorf("failed to generate token: %w", err)
	}

	// Store the token in the token manager
	tokenManager, err := c.javaClient.GetTokenManager()
	if err != nil {
		return "", fmt.Errorf("failed to get token manager: %w", err)
	}

	// Add token to cache
	tokenManager.mu.Lock()
	tokenManager.tokens[response.Token] = TokenInfo{
		Token:       response.Token,
		ProjectID:   projectID,
		TokenType:   "prod",
		Scope:       scope,
		IssuedAt:    time.Now(),
		ExpiresAt:   response.ExpiresAt,
		LastChecked: time.Now(),
		Valid:       true,
	}
	tokenManager.mu.Unlock()

	// Save tokens to file
	if err := tokenManager.saveTokens(); err != nil {
		fmt.Printf("Warning: Failed to save tokens: %v\n", err)
	}

	return response.Token, nil
}

// RevokeToken revokes an API token
func (c *APIClient) RevokeToken(ctx context.Context, token string) error {
	tokenManager, err := c.javaClient.GetTokenManager()
	if err != nil {
		return fmt.Errorf("failed to get token manager: %w", err)
	}

	return tokenManager.RevokeToken(ctx, token)
}

// ListTokens lists all API tokens
func (c *APIClient) ListTokens(ctx context.Context) ([]TokenInfo, error) {
	tokenManager, err := c.javaClient.GetTokenManager()
	if err != nil {
		return nil, fmt.Errorf("failed to get token manager: %w", err)
	}

	return tokenManager.ListTokens(), nil
}

// CleanupTokens removes expired tokens
func (c *APIClient) CleanupTokens(ctx context.Context) error {
	tokenManager, err := c.javaClient.GetTokenManager()
	if err != nil {
		return fmt.Errorf("failed to get token manager: %w", err)
	}

	tokenManager.CleanExpiredTokens()
	return nil
}