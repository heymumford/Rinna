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
	"time"

	"github.com/heymumford/rinna/api/internal/models"
	"github.com/heymumford/rinna/api/pkg/config"
)

// JavaClient handles communication with the Java service
type JavaClient struct {
	config     *config.JavaServiceConfig
	httpClient *http.Client
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

// Request sends a request to the Java service
func (c *JavaClient) Request(ctx context.Context, method, path string, payload interface{}, response interface{}) error {
	// Build the URL
	url := fmt.Sprintf("http://%s:%d%s", c.config.Host, c.config.Port, path)

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

	// Send the request
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to send request: %v", err)
	}
	defer resp.Body.Close()

	// Check the status code
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		var errorResponse struct {
			Error string `json:"error"`
		}
		if err := json.NewDecoder(resp.Body).Decode(&errorResponse); err != nil {
			return fmt.Errorf("received non-success status code: %d", resp.StatusCode)
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

// Ping checks if the Java service is reachable
func (c *JavaClient) Ping(ctx context.Context) error {
	ctx, cancel := context.WithTimeout(ctx, time.Duration(c.config.ConnectTimeout)*time.Millisecond)
	defer cancel()

	// Send a request to the health endpoint
	var response struct {
		Status string `json:"status"`
	}
	err := c.Request(ctx, http.MethodGet, "/health", nil, &response)
	if err != nil {
		return fmt.Errorf("failed to ping Java service: %v", err)
	}

	if response.Status != "ok" {
		return fmt.Errorf("Java service reported non-ok status: %s", response.Status)
	}

	return nil
}

// ListWorkItems retrieves a list of work items from the Java service
func (c *JavaClient) ListWorkItems(ctx context.Context, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	// Build query parameters
	query := ""
	if status != "" {
		query = fmt.Sprintf("?status=%s", status)
	}
	if page > 0 {
		if query == "" {
			query = fmt.Sprintf("?page=%d", page)
		} else {
			query = fmt.Sprintf("%s&page=%d", query, page)
		}
	}
	if pageSize > 0 {
		if query == "" {
			query = fmt.Sprintf("?pageSize=%d", pageSize)
		} else {
			query = fmt.Sprintf("%s&pageSize=%d", query, pageSize)
		}
	}

	// Send the request
	var response models.WorkItemListResponse
	err := c.Request(ctx, http.MethodGet, "/api/workitems"+query, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to list work items: %v", err)
	}

	return &response, nil
}

// CreateWorkItem creates a new work item in the Java service
func (c *JavaClient) CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error) {
	// Send the request
	var response models.WorkItem
	err := c.Request(ctx, http.MethodPost, "/api/workitems", request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to create work item: %v", err)
	}

	return &response, nil
}

// GetWorkItem retrieves a work item by ID from the Java service
func (c *JavaClient) GetWorkItem(ctx context.Context, id string) (*models.WorkItem, error) {
	// Send the request
	var response models.WorkItem
	err := c.Request(ctx, http.MethodGet, "/api/workitems/"+id, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to get work item: %v", err)
	}

	return &response, nil
}

// UpdateWorkItem updates a work item in the Java service
func (c *JavaClient) UpdateWorkItem(ctx context.Context, id string, request models.WorkItemUpdateRequest) (*models.WorkItem, error) {
	// Send the request
	var response models.WorkItem
	err := c.Request(ctx, http.MethodPut, "/api/workitems/"+id, request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to update work item: %v", err)
	}

	return &response, nil
}

// TransitionWorkItem transitions a work item in the Java service
func (c *JavaClient) TransitionWorkItem(ctx context.Context, id string, request models.WorkItemTransitionRequest) (*models.WorkItem, error) {
	// Send the request
	var response models.WorkItem
	err := c.Request(ctx, http.MethodPost, "/api/workitems/"+id+"/transitions", request, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to transition work item: %v", err)
	}

	return &response, nil
}