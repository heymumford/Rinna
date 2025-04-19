/*
 * Java service client for the Rinna API server
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package client

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/heymumford/rinna/api/internal/models"
	"github.com/heymumford/rinna/api/pkg/config"
)

// JavaClient handles communication with the Java service
type JavaClient struct {
	config       *config.JavaServiceConfig
	httpClient   *http.Client
	tokenManager *TokenManager
	fullConfig   *config.RinnaConfig
}

// NewJavaClient creates a new Java client
func NewJavaClient(config *config.JavaServiceConfig) *JavaClient {
	// Set a reasonable default timeout if none or invalid is provided
	requestTimeout := time.Duration(config.RequestTimeout) * time.Millisecond
	if config.RequestTimeout <= 0 {
		requestTimeout = 30 * time.Second // Default to 30 seconds
	}

	return &JavaClient{
		config: config,
		httpClient: &http.Client{
			Timeout: requestTimeout,
		},
	}
}

// WithFullConfig sets the full configuration on the Java client
func (c *JavaClient) WithFullConfig(cfg *config.RinnaConfig) *JavaClient {
	c.fullConfig = cfg
	return c
}

// InitializeTokenManager initializes the token manager if not already initialized
func (c *JavaClient) InitializeTokenManager() error {
	if c.tokenManager != nil {
		return nil
	}

	if c.fullConfig == nil {
		var err error
		c.fullConfig, err = config.GetConfig()
		if err != nil {
			return fmt.Errorf("failed to get configuration: %w", err)
		}
	}

	tokenManager, err := NewTokenManager(c.fullConfig, c)
	if err != nil {
		return fmt.Errorf("failed to create token manager: %w", err)
	}

	c.tokenManager = tokenManager
	return nil
}

// GetTokenManager returns the token manager
func (c *JavaClient) GetTokenManager() (*TokenManager, error) {
	if c.tokenManager == nil {
		if err := c.InitializeTokenManager(); err != nil {
			return nil, err
		}
	}
	return c.tokenManager, nil
}

// Request sends a request to the Java service
func (c *JavaClient) Request(ctx context.Context, method, path string, payload interface{}, response interface{}) error {
	// Build the URL
	url := fmt.Sprintf("http://%s:%d%s", c.config.Host, c.config.Port, path)
	fmt.Printf("Sending request to Java service: %s %s\n", method, url)

	// Marshall the payload
	var body []byte
	var err error
	if payload != nil {
		body, err = json.Marshal(payload)
		if err != nil {
			return fmt.Errorf("failed to marshal request payload: %v", err)
		}
	}

	// Create the request
	req, err := http.NewRequestWithContext(ctx, method, url, bytes.NewReader(body))
	if err != nil {
		return fmt.Errorf("failed to create request: %v", err)
	}

	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")

	// Add authentication if token manager is available and path is not the token generation endpoint
	if c.tokenManager != nil && !strings.Contains(path, "/token/generate") && !strings.Contains(path, "/token/validate") {
		// Check for project ID in the context
		projectID := ctx.Value("projectID")
		if projectID != nil {
			// Get token for the project
			token, err := c.tokenManager.GetToken(ctx, projectID.(string))
			if err == nil {
				req.Header.Set("Authorization", "Bearer "+token)
			} else {
				fmt.Printf("Warning: Failed to get token for project %s: %v\n", projectID, err)
			}
		}
	}

	// Send the request
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to send request: %v", err)
	}
	defer resp.Body.Close()

	// Check for token expiration warning
	if expiringHeader := resp.Header.Get("X-Token-Expiring-Soon"); expiringHeader == "true" {
		expiresIn := resp.Header.Get("X-Token-Expires-In")
		fmt.Printf("Warning: API token is expiring soon. Expires in %s hours\n", expiresIn)
	}

	// Check the status code
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		var errorResponse struct {
			Error string `json:"error"`
		}
		if err := json.NewDecoder(resp.Body).Decode(&errorResponse); err != nil {
			return fmt.Errorf("received non-success status code: %d", resp.StatusCode)
		}
		
		// Handle unauthorized errors - token might be invalid
		if resp.StatusCode == http.StatusUnauthorized && c.tokenManager != nil {
			// Check for auth token in the request
			if authHeader := req.Header.Get("Authorization"); strings.HasPrefix(authHeader, "Bearer ") {
				token := strings.TrimPrefix(authHeader, "Bearer ")
				
				// Mark token as invalid
				if tokenInfo, err := c.tokenManager.GetTokenInfo(token); err == nil {
					// Only log for non-validation endpoints to avoid circular logging
					if !strings.Contains(path, "/token/validate") {
						fmt.Printf("Token %s for project %s is no longer valid. Removing from cache.\n", 
							truncateToken(token), tokenInfo.ProjectID)
					}
					
					// Revoke the token locally
					c.tokenManager.RevokeToken(ctx, token)
				}
			}
		}
		
		return fmt.Errorf("received error response: %s", errorResponse.Error)
	}

	// Decode the response
	if response != nil {
		if err := json.NewDecoder(resp.Body).Decode(response); err != nil {
			return fmt.Errorf("failed to decode response: %v", err)
		}
	}

	return nil
}

// truncateToken returns a truncated token for logging (security)
func truncateToken(token string) string {
	if len(token) <= 12 {
		return "********"
	}
	return token[:8] + "..." + token[len(token)-4:]
}

// Ping checks if the Java service is reachable
func (c *JavaClient) Ping(ctx context.Context) error {
	ctx, cancel := context.WithTimeout(ctx, time.Duration(c.config.ConnectTimeout)*time.Millisecond)
	defer cancel()

	// Get the health endpoint from config, or use default
	endpoint := "/health"
	if c.config.Endpoints != nil && c.config.Endpoints["health"] != "" {
		endpoint = c.config.Endpoints["health"]
	}

	// Send a request to the health endpoint
	var response struct {
		Status string `json:"status"`
	}
	err := c.Request(ctx, http.MethodGet, endpoint, nil, &response)
	if err != nil {
		return fmt.Errorf("failed to ping Java service: %v", err)
	}

	if response.Status != "ok" {
		return fmt.Errorf("Java service reported non-ok status: %s", response.Status)
	}

	return nil
}

// buildQueryParameters constructs a query string from common pagination parameters
func buildQueryParameters(params map[string]string) string {
	if len(params) == 0 {
		return ""
	}

	query := "?"
	first := true
	for key, value := range params {
		if value == "" {
			continue
		}
		if !first {
			query += "&"
		}
		query += fmt.Sprintf("%s=%s", key, value)
		first = false
	}

	return query
}

// === Work Item Operations ===

// ListWorkItems retrieves a list of work items from the Java service
func (c *JavaClient) ListWorkItems(ctx context.Context, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	// Get the workitems endpoint from config, or use default
	endpoint := "/api/workitems"
	if c.config.Endpoints != nil && c.config.Endpoints["workitems"] != "" {
		endpoint = c.config.Endpoints["workitems"]
	}

	// Build query parameters
	params := make(map[string]string)
	if status != "" {
		params["status"] = status
	}
	if page > 0 {
		params["page"] = strconv.Itoa(page)
	}
	if pageSize > 0 {
		params["pageSize"] = strconv.Itoa(pageSize)
	}
	
	query := buildQueryParameters(params)

	// Send the request
	var response models.WorkItemListResponse
	err := c.Request(ctx, http.MethodGet, endpoint+query, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to list work items: %v", err)
	}

	return &response, nil
}

// CreateWorkItem creates a new work item in the Java service
func (c *JavaClient) CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error) {
	// Get the workitems endpoint from config, or use default
	endpoint := "/api/workitems"
	if c.config.Endpoints != nil && c.config.Endpoints["workitems"] != "" {
		endpoint = c.config.Endpoints["workitems"]
	}

	// Send the request
	var response models.WorkItem
	err := c.Request(ctx, http.MethodPost, endpoint, request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to create work item: %v", err)
	}

	return &response, nil
}

// GetWorkItem retrieves a work item by ID from the Java service
func (c *JavaClient) GetWorkItem(ctx context.Context, id string) (*models.WorkItem, error) {
	// Get the workitems endpoint from config, or use default
	endpoint := "/api/workitems"
	if c.config.Endpoints != nil && c.config.Endpoints["workitems"] != "" {
		endpoint = c.config.Endpoints["workitems"]
	}

	// Send the request
	var response models.WorkItem
	err := c.Request(ctx, http.MethodGet, endpoint+"/"+id, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to get work item: %v", err)
	}

	return &response, nil
}

// UpdateWorkItem updates a work item in the Java service
func (c *JavaClient) UpdateWorkItem(ctx context.Context, id string, request models.WorkItemUpdateRequest) (*models.WorkItem, error) {
	// Get the workitems endpoint from config, or use default
	endpoint := "/api/workitems"
	if c.config.Endpoints != nil && c.config.Endpoints["workitems"] != "" {
		endpoint = c.config.Endpoints["workitems"]
	}

	// Send the request
	var response models.WorkItem
	err := c.Request(ctx, http.MethodPut, endpoint+"/"+id, request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to update work item: %v", err)
	}

	return &response, nil
}

// TransitionWorkItem transitions a work item in the Java service
func (c *JavaClient) TransitionWorkItem(ctx context.Context, id string, request models.WorkItemTransitionRequest) (*models.WorkItem, error) {
	// Get the workitems endpoint from config, or use default
	endpoint := "/api/workitems"
	if c.config.Endpoints != nil && c.config.Endpoints["workitems"] != "" {
		endpoint = c.config.Endpoints["workitems"]
	}

	// Send the request
	var response models.WorkItem
	err := c.Request(ctx, http.MethodPost, endpoint+"/"+id+"/transitions", request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to transition work item: %v", err)
	}

	return &response, nil
}

// === Project Operations ===

// ListProjects retrieves a list of projects from the Java service
func (c *JavaClient) ListProjects(ctx context.Context, page, pageSize int, activeOnly bool) (*models.ProjectListResponse, error) {
	// Get the projects endpoint from config, or use default
	endpoint := "/api/projects"
	if c.config.Endpoints != nil && c.config.Endpoints["projects"] != "" {
		endpoint = c.config.Endpoints["projects"]
	}

	// Build query parameters
	params := make(map[string]string)
	if page > 0 {
		params["page"] = strconv.Itoa(page)
	}
	if pageSize > 0 {
		params["pageSize"] = strconv.Itoa(pageSize)
	}
	if activeOnly {
		params["activeOnly"] = "true"
	}
	
	query := buildQueryParameters(params)

	// Send the request
	var response models.ProjectListResponse
	err := c.Request(ctx, http.MethodGet, endpoint+query, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to list projects: %v", err)
	}

	return &response, nil
}

// CreateProject creates a new project in the Java service
func (c *JavaClient) CreateProject(ctx context.Context, request models.ProjectCreateRequest) (*models.Project, error) {
	// Get the projects endpoint from config, or use default
	endpoint := "/api/projects"
	if c.config.Endpoints != nil && c.config.Endpoints["projects"] != "" {
		endpoint = c.config.Endpoints["projects"]
	}

	// Send the request
	var response models.Project
	err := c.Request(ctx, http.MethodPost, endpoint, request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to create project: %v", err)
	}

	return &response, nil
}

// GetProject retrieves a project by key from the Java service
func (c *JavaClient) GetProject(ctx context.Context, key string) (*models.Project, error) {
	// Get the projects endpoint from config, or use default
	endpoint := "/api/projects"
	if c.config.Endpoints != nil && c.config.Endpoints["projects"] != "" {
		endpoint = c.config.Endpoints["projects"]
	}

	// Send the request
	var response models.Project
	err := c.Request(ctx, http.MethodGet, endpoint+"/"+key, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to get project: %v", err)
	}

	return &response, nil
}

// UpdateProject updates a project in the Java service
func (c *JavaClient) UpdateProject(ctx context.Context, key string, request models.ProjectUpdateRequest) (*models.Project, error) {
	// Get the projects endpoint from config, or use default
	endpoint := "/api/projects"
	if c.config.Endpoints != nil && c.config.Endpoints["projects"] != "" {
		endpoint = c.config.Endpoints["projects"]
	}

	// Send the request
	var response models.Project
	err := c.Request(ctx, http.MethodPut, endpoint+"/"+key, request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to update project: %v", err)
	}

	return &response, nil
}

// GetProjectWorkItems retrieves work items for a project from the Java service
func (c *JavaClient) GetProjectWorkItems(ctx context.Context, key string, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	// Get the projects endpoint from config, or use default
	endpoint := "/api/projects"
	if c.config.Endpoints != nil && c.config.Endpoints["projects"] != "" {
		endpoint = c.config.Endpoints["projects"]
	}

	// Build query parameters
	params := make(map[string]string)
	if status != "" {
		params["status"] = status
	}
	if page > 0 {
		params["page"] = strconv.Itoa(page)
	}
	if pageSize > 0 {
		params["pageSize"] = strconv.Itoa(pageSize)
	}
	
	query := buildQueryParameters(params)

	// Send the request
	var response models.WorkItemListResponse
	err := c.Request(ctx, http.MethodGet, endpoint+"/"+key+"/workitems"+query, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to get project work items: %v", err)
	}

	return &response, nil
}

// === Authentication Operations ===

// ValidateToken validates an API token with the Java service
func (c *JavaClient) ValidateToken(ctx context.Context, token string) (string, error) {
	// Get the auth endpoint from config, or use default
	endpoint := "/api/auth/token/validate"
	if c.config.Endpoints != nil && c.config.Endpoints["auth_token"] != "" {
		endpoint = c.config.Endpoints["auth_token"]
	}

	// Send the request
	var response struct {
		ProjectID string `json:"projectId"`
		Valid     bool   `json:"valid"`
	}
	
	// Create request structure
	request := struct {
		Token string `json:"token"`
	}{
		Token: token,
	}
	
	err := c.Request(ctx, http.MethodPost, endpoint, request, &response)
	if err != nil {
		return "", fmt.Errorf("failed to validate token: %v", err)
	}

	if !response.Valid {
		return "", fmt.Errorf("token is not valid")
	}

	return response.ProjectID, nil
}

// GetWebhookSecret retrieves the webhook secret for a project from the Java service
func (c *JavaClient) GetWebhookSecret(ctx context.Context, projectKey, source string) (string, error) {
	// Get the webhook secret endpoint from config, or use default
	endpoint := "/api/projects/webhooks/secret"
	if c.config.Endpoints != nil && c.config.Endpoints["webhook_secret"] != "" {
		endpoint = c.config.Endpoints["webhook_secret"]
	}

	// Build query parameters
	params := make(map[string]string)
	params["projectKey"] = projectKey
	params["source"] = source
	
	query := buildQueryParameters(params)

	// Send the request
	var response struct {
		Secret string `json:"secret"`
	}
	err := c.Request(ctx, http.MethodGet, endpoint+query, nil, &response)
	if err != nil {
		return "", fmt.Errorf("failed to get webhook secret: %v", err)
	}

	return response.Secret, nil
}

// === Release Operations ===

// ListReleases retrieves a list of releases from the Java service
func (c *JavaClient) ListReleases(ctx context.Context, page, pageSize int, status string) (*models.ReleaseListResponse, error) {
	// Get the releases endpoint from config, or use default
	endpoint := "/api/releases"
	if c.config.Endpoints != nil && c.config.Endpoints["releases"] != "" {
		endpoint = c.config.Endpoints["releases"]
	}

	// Build query parameters
	params := make(map[string]string)
	if status != "" {
		params["status"] = status
	}
	if page > 0 {
		params["page"] = strconv.Itoa(page)
	}
	if pageSize > 0 {
		params["pageSize"] = strconv.Itoa(pageSize)
	}
	
	query := buildQueryParameters(params)

	// Send the request
	var response models.ReleaseListResponse
	err := c.Request(ctx, http.MethodGet, endpoint+query, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to list releases: %v", err)
	}

	return &response, nil
}

// CreateRelease creates a new release in the Java service
func (c *JavaClient) CreateRelease(ctx context.Context, request models.ReleaseCreateRequest) (*models.Release, error) {
	// Get the releases endpoint from config, or use default
	endpoint := "/api/releases"
	if c.config.Endpoints != nil && c.config.Endpoints["releases"] != "" {
		endpoint = c.config.Endpoints["releases"]
	}

	// Send the request
	var response models.Release
	err := c.Request(ctx, http.MethodPost, endpoint, request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to create release: %v", err)
	}

	return &response, nil
}

// GetRelease retrieves a release by ID from the Java service
func (c *JavaClient) GetRelease(ctx context.Context, id string) (*models.Release, error) {
	// Get the releases endpoint from config, or use default
	endpoint := "/api/releases"
	if c.config.Endpoints != nil && c.config.Endpoints["releases"] != "" {
		endpoint = c.config.Endpoints["releases"]
	}

	// Send the request
	var response models.Release
	err := c.Request(ctx, http.MethodGet, endpoint+"/"+id, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to get release: %v", err)
	}

	return &response, nil
}

// UpdateRelease updates a release in the Java service
func (c *JavaClient) UpdateRelease(ctx context.Context, id string, request models.ReleaseUpdateRequest) (*models.Release, error) {
	// Get the releases endpoint from config, or use default
	endpoint := "/api/releases"
	if c.config.Endpoints != nil && c.config.Endpoints["releases"] != "" {
		endpoint = c.config.Endpoints["releases"]
	}

	// Send the request
	var response models.Release
	err := c.Request(ctx, http.MethodPut, endpoint+"/"+id, request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to update release: %v", err)
	}

	return &response, nil
}

// GetReleaseWorkItems retrieves work items for a release from the Java service
func (c *JavaClient) GetReleaseWorkItems(ctx context.Context, id string, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	// Get the releases endpoint from config, or use default
	endpoint := "/api/releases"
	if c.config.Endpoints != nil && c.config.Endpoints["releases"] != "" {
		endpoint = c.config.Endpoints["releases"]
	}

	// Build query parameters
	params := make(map[string]string)
	if status != "" {
		params["status"] = status
	}
	if page > 0 {
		params["page"] = strconv.Itoa(page)
	}
	if pageSize > 0 {
		params["pageSize"] = strconv.Itoa(pageSize)
	}
	
	query := buildQueryParameters(params)

	// Send the request
	var response models.WorkItemListResponse
	err := c.Request(ctx, http.MethodGet, endpoint+"/"+id+"/workitems"+query, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to get release work items: %v", err)
	}

	return &response, nil
}