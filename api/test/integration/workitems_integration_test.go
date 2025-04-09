/*
 * Integration tests for work item API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package integration

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strconv"
	"sync"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/models"
)

// MockWorkItemService implements a mock work item service for integration testing
type MockWorkItemService struct {
	workItems map[string]*models.WorkItem
	mutex     sync.RWMutex
	mux       *mux.Router
	server    *httptest.Server
}

// NewMockWorkItemService creates a new mock work item service
func NewMockWorkItemService() *MockWorkItemService {
	service := &MockWorkItemService{
		workItems: make(map[string]*models.WorkItem),
		mutex:     sync.RWMutex{},
		mux:       mux.NewRouter(),
	}

	// Register routes
	service.setupRoutes()

	// Start server
	service.server = httptest.NewServer(service.mux)

	return service
}

// Close closes the mock service
func (m *MockWorkItemService) Close() {
	if m.server != nil {
		m.server.Close()
	}
}

// GetServerURL returns the URL of the mock server
func (m *MockWorkItemService) GetServerURL() string {
	return m.server.URL
}

// setupRoutes sets up the routes for the mock service
func (m *MockWorkItemService) setupRoutes() {
	// List work items
	m.mux.HandleFunc("/api/workitems", func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodGet {
			m.handleListWorkItems(w, r)
			return
		}
		if r.Method == http.MethodPost {
			m.handleCreateWorkItem(w, r)
			return
		}
		http.NotFound(w, r)
	}).Methods(http.MethodGet, http.MethodPost)

	// Get, update work item
	m.mux.HandleFunc("/api/workitems/{id}", func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodGet {
			m.handleGetWorkItem(w, r)
			return
		}
		if r.Method == http.MethodPut {
			m.handleUpdateWorkItem(w, r)
			return
		}
		http.NotFound(w, r)
	}).Methods(http.MethodGet, http.MethodPut)

	// Transition work item
	m.mux.HandleFunc("/api/workitems/{id}/transitions", m.handleTransitionWorkItem).Methods(http.MethodPost)
}

// handleListWorkItems handles list work items requests
func (m *MockWorkItemService) handleListWorkItems(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters
	query := r.URL.Query()
	status := query.Get("status")
	pageStr := query.Get("page")
	pageSizeStr := query.Get("pageSize")

	// Set default values
	page := 1
	pageSize := 10

	// Parse page parameter
	if pageStr != "" {
		fmt.Sscanf(pageStr, "%d", &page)
	}

	// Parse pageSize parameter
	if pageSizeStr != "" {
		fmt.Sscanf(pageSizeStr, "%d", &pageSize)
	}

	// Filter work items
	m.mutex.RLock()
	var items []models.WorkItem
	for _, item := range m.workItems {
		if status == "" || string(item.Status) == status {
			items = append(items, *item)
		}
	}
	m.mutex.RUnlock()

	// Create response
	response := models.WorkItemListResponse{
		Items:      items,
		TotalCount: len(items),
		Page:       page,
		PageSize:   pageSize,
	}

	// Return response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// handleCreateWorkItem handles create work item requests
func (m *MockWorkItemService) handleCreateWorkItem(w http.ResponseWriter, r *http.Request) {
	// Parse request
	var request models.WorkItemCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Validate request
	if request.Title == "" {
		http.Error(w, "Title is required", http.StatusBadRequest)
		return
	}

	// Create work item
	now := time.Now()
	item := &models.WorkItem{
		ID:          uuid.New(),
		Title:       request.Title,
		Description: request.Description,
		Type:        request.Type,
		Priority:    request.Priority,
		Status:      models.WorkflowStateFound,
		ProjectID:   request.ProjectID,
		Metadata:    request.Metadata,
		CreatedAt:   now,
		UpdatedAt:   now,
	}

	// Set default values
	if item.Type == "" {
		item.Type = models.WorkItemTypeFeature
	}
	if item.Priority == "" {
		item.Priority = models.PriorityMedium
	}

	// Store work item
	m.mutex.Lock()
	m.workItems[item.ID.String()] = item
	m.mutex.Unlock()

	// Return response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(item)
}

// handleGetWorkItem handles get work item requests
func (m *MockWorkItemService) handleGetWorkItem(w http.ResponseWriter, r *http.Request) {
	// Get ID from URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Get work item
	m.mutex.RLock()
	item, exists := m.workItems[id]
	m.mutex.RUnlock()

	if !exists {
		http.Error(w, fmt.Sprintf("work item not found: %s", id), http.StatusNotFound)
		return
	}

	// Return response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(item)
}

// handleUpdateWorkItem handles update work item requests
func (m *MockWorkItemService) handleUpdateWorkItem(w http.ResponseWriter, r *http.Request) {
	// Get ID from URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Get work item
	m.mutex.Lock()
	defer m.mutex.Unlock()

	item, exists := m.workItems[id]
	if !exists {
		http.Error(w, fmt.Sprintf("work item not found: %s", id), http.StatusNotFound)
		return
	}

	// Parse request
	var request models.WorkItemUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Update work item
	if request.Title != nil {
		item.Title = *request.Title
	}
	if request.Description != nil {
		item.Description = *request.Description
	}
	if request.Type != nil {
		item.Type = *request.Type
	}
	if request.Priority != nil {
		item.Priority = *request.Priority
	}
	if request.Status != nil {
		item.Status = *request.Status
	}
	if request.Assignee != nil {
		item.Assignee = *request.Assignee
	}
	if request.Metadata != nil {
		item.Metadata = request.Metadata
	}

	// Update timestamp
	item.UpdatedAt = time.Now()

	// Return response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(item)
}

// handleTransitionWorkItem handles transition work item requests
func (m *MockWorkItemService) handleTransitionWorkItem(w http.ResponseWriter, r *http.Request) {
	// Get ID from URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Get work item
	m.mutex.Lock()
	defer m.mutex.Unlock()

	item, exists := m.workItems[id]
	if !exists {
		http.Error(w, fmt.Sprintf("work item not found: %s", id), http.StatusNotFound)
		return
	}

	// Parse request
	var request models.WorkItemTransitionRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Validate request
	if request.ToState == "" {
		http.Error(w, "ToState is required", http.StatusBadRequest)
		return
	}

	// Check if transition is valid
	if !isValidTransition(item.Status, request.ToState) {
		http.Error(w, fmt.Sprintf("invalid transition: %s", request.ToState), http.StatusBadRequest)
		return
	}

	// Update status
	item.Status = request.ToState

	// Update timestamp
	item.UpdatedAt = time.Now()

	// Return response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(item)
}

// isValidTransition checks if a transition is valid
func isValidTransition(fromState, toState models.WorkflowState) bool {
	// Simplified transition rules
	validTransitions := map[models.WorkflowState][]models.WorkflowState{
		models.WorkflowStateFound:   {models.WorkflowStateTriaged, models.WorkflowStateInDev, models.WorkflowStateClosed},
		models.WorkflowStateTriaged: {models.WorkflowStateInDev, models.WorkflowStateClosed},
		models.WorkflowStateInDev:   {models.WorkflowStateTesting, models.WorkflowStateClosed},
		models.WorkflowStateTesting: {models.WorkflowStateDone, models.WorkflowStateInDev, models.WorkflowStateClosed},
		models.WorkflowStateDone:    {models.WorkflowStateClosed, models.WorkflowStateInDev},
		models.WorkflowStateClosed:  {models.WorkflowStateFound, models.WorkflowStateInDev},
	}

	transitions, exists := validTransitions[fromState]
	if !exists {
		return false
	}

	for _, state := range transitions {
		if state == toState {
			return true
		}
	}

	return false
}

// CustomTransport is a http.RoundTripper that redirects requests to our mock service
type CustomTransport struct {
	mockURL     string
	originalURL string
}

// RoundTrip implements the http.RoundTripper interface
func (t *CustomTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	// Redirect the request to our mock service
	if req.URL.Host == t.originalURL {
		req.URL.Scheme = "http"
		req.URL.Host = t.mockURL
	}

	// Use the default transport to actually execute the request
	return http.DefaultTransport.RoundTrip(req)
}

// TestClient is a test implementation of the Java client interface
type TestClient struct {
	BaseURL        string
	RequestTimeout time.Duration
	HTTPClient     *http.Client
}

// NewTestClient creates a new test client that redirects to our mock service
func NewTestClient(mockURL string) *TestClient {
	// Extract host and port from mockURL (remove http:// prefix)
	mockURLWithoutScheme := mockURL[7:] // Remove "http://"

	// Create custom transport
	transport := &CustomTransport{
		mockURL:     mockURLWithoutScheme,
		originalURL: "localhost:8080",
	}
	
	// Create HTTP client with custom transport
	httpClient := &http.Client{
		Transport: transport,
		Timeout:   30 * time.Second,
	}
	
	return &TestClient{
		BaseURL:        "http://localhost:8080",
		RequestTimeout: 30 * time.Second,
		HTTPClient:     httpClient,
	}
}

// ListWorkItems retrieves a list of work items
func (c *TestClient) ListWorkItems(ctx context.Context, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	// Build URL
	url := fmt.Sprintf("%s/api/workitems", c.BaseURL)
	
	// Add query parameters
	queryParams := make(map[string]string)
	if status != "" {
		queryParams["status"] = status
	}
	if page > 0 {
		queryParams["page"] = fmt.Sprintf("%d", page)
	}
	if pageSize > 0 {
		queryParams["pageSize"] = fmt.Sprintf("%d", pageSize)
	}
	
	// Add query parameters to URL
	if len(queryParams) > 0 {
		url += "?"
		for key, value := range queryParams {
			url += fmt.Sprintf("%s=%s&", key, value)
		}
		url = url[:len(url)-1] // Remove trailing "&"
	}
	
	// Create request
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return nil, err
	}
	
	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	
	// Send request
	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	
	// Check response
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("unexpected status code: %d", resp.StatusCode)
	}
	
	// Decode response
	var result models.WorkItemListResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}
	
	return &result, nil
}

// GetWorkItem retrieves a work item by ID
func (c *TestClient) GetWorkItem(ctx context.Context, id string) (*models.WorkItem, error) {
	// Build URL
	url := fmt.Sprintf("%s/api/workitems/%s", c.BaseURL, id)
	
	// Create request
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return nil, err
	}
	
	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	
	// Send request
	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	
	// Check response
	if resp.StatusCode == http.StatusNotFound {
		return nil, fmt.Errorf("work item not found: %s", id)
	}
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("unexpected status code: %d", resp.StatusCode)
	}
	
	// Decode response
	var result models.WorkItem
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}
	
	return &result, nil
}

// CreateWorkItem creates a new work item
func (c *TestClient) CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error) {
	// Build URL
	url := fmt.Sprintf("%s/api/workitems", c.BaseURL)
	
	// Encode request body
	body, err := json.Marshal(request)
	if err != nil {
		return nil, err
	}
	
	// Create request
	req, err := http.NewRequest("POST", url, bytes.NewReader(body))
	if err != nil {
		return nil, err
	}
	
	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	
	// Send request
	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	
	// Check response
	if resp.StatusCode != http.StatusCreated {
		return nil, fmt.Errorf("unexpected status code: %d", resp.StatusCode)
	}
	
	// Decode response
	var result models.WorkItem
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}
	
	return &result, nil
}

// UpdateWorkItem updates a work item
func (c *TestClient) UpdateWorkItem(ctx context.Context, id string, request models.WorkItemUpdateRequest) (*models.WorkItem, error) {
	// Build URL
	url := fmt.Sprintf("%s/api/workitems/%s", c.BaseURL, id)
	
	// Encode request body
	body, err := json.Marshal(request)
	if err != nil {
		return nil, err
	}
	
	// Create request
	req, err := http.NewRequest("PUT", url, bytes.NewReader(body))
	if err != nil {
		return nil, err
	}
	
	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	
	// Send request
	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	
	// Check response
	if resp.StatusCode == http.StatusNotFound {
		return nil, fmt.Errorf("work item not found: %s", id)
	}
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("unexpected status code: %d", resp.StatusCode)
	}
	
	// Decode response
	var result models.WorkItem
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}
	
	return &result, nil
}

// TransitionWorkItem transitions a work item
func (c *TestClient) TransitionWorkItem(ctx context.Context, id string, request models.WorkItemTransitionRequest) (*models.WorkItem, error) {
	// Build URL
	url := fmt.Sprintf("%s/api/workitems/%s/transitions", c.BaseURL, id)
	
	// Encode request body
	body, err := json.Marshal(request)
	if err != nil {
		return nil, err
	}
	
	// Create request
	req, err := http.NewRequest("POST", url, bytes.NewReader(body))
	if err != nil {
		return nil, err
	}
	
	// Set headers
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	
	// Send request
	resp, err := c.HTTPClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	
	// Check response
	if resp.StatusCode == http.StatusNotFound {
		return nil, fmt.Errorf("work item not found: %s", id)
	}
	if resp.StatusCode == http.StatusBadRequest {
		return nil, fmt.Errorf("invalid transition: %s", string(request.ToState))
	}
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("unexpected status code: %d", resp.StatusCode)
	}
	
	// Decode response
	var result models.WorkItem
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}
	
	return &result, nil
}

// testWorkItemHandler is a test implementation of the work item handler
type testWorkItemHandler struct {
	javaClient *TestClient
}

// ListWorkItems handles listing work items
func (h *testWorkItemHandler) ListWorkItems(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters
	status := r.URL.Query().Get("status")
	pageStr := r.URL.Query().Get("page")
	pageSizeStr := r.URL.Query().Get("pageSize")

	// Set default values
	page := 1
	pageSize := 10

	// Parse page parameter
	if pageStr != "" {
		parsedPage, err := strconv.Atoi(pageStr)
		if err != nil || parsedPage < 1 {
			http.Error(w, "Invalid page parameter", http.StatusBadRequest)
			return
		}
		page = parsedPage
	}

	// Parse pageSize parameter
	if pageSizeStr != "" {
		parsedPageSize, err := strconv.Atoi(pageSizeStr)
		if err != nil || parsedPageSize < 1 || parsedPageSize > 100 {
			http.Error(w, "Invalid pageSize parameter (must be between 1 and 100)", http.StatusBadRequest)
			return
		}
		pageSize = parsedPageSize
	}

	// Call the Java service
	response, err := h.javaClient.ListWorkItems(r.Context(), status, page, pageSize)
	if err != nil {
		http.Error(w, "Failed to list work items: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetWorkItem handles getting a work item by ID
func (h *testWorkItemHandler) GetWorkItem(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Call the Java service
	workItem, err := h.javaClient.GetWorkItem(r.Context(), id)
	if err != nil {
		if err.Error() == "work item not found: "+id {
			http.Error(w, "Work item not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(workItem)
}

// CreateWorkItem handles creating a new work item
func (h *testWorkItemHandler) CreateWorkItem(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var request models.WorkItemCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Validate request
	if request.Title == "" {
		http.Error(w, "Title is required", http.StatusBadRequest)
		return
	}

	// Set default values
	if request.Type == "" {
		request.Type = models.WorkItemTypeFeature
	}
	if request.Priority == "" {
		request.Priority = models.PriorityMedium
	}

	// Call the Java service
	workItem, err := h.javaClient.CreateWorkItem(r.Context(), request)
	if err != nil {
		http.Error(w, "Failed to create work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(workItem)
}

// UpdateWorkItem handles updating a work item
func (h *testWorkItemHandler) UpdateWorkItem(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Parse request body
	var request models.WorkItemUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Call the Java service
	workItem, err := h.javaClient.UpdateWorkItem(r.Context(), id, request)
	if err != nil {
		if err.Error() == "work item not found: "+id {
			http.Error(w, "Work item not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to update work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(workItem)
}

// TransitionWorkItem handles transitioning a work item to a new state
func (h *testWorkItemHandler) TransitionWorkItem(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Parse request body
	var request models.WorkItemTransitionRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Validate request
	if request.ToState == "" {
		http.Error(w, "ToState is required", http.StatusBadRequest)
		return
	}

	// Call the Java service
	workItem, err := h.javaClient.TransitionWorkItem(r.Context(), id, request)
	if err != nil {
		if err.Error() == "work item not found: "+id {
			http.Error(w, "Work item not found", http.StatusNotFound)
			return
		}
		if err.Error() == "invalid transition: "+string(request.ToState) {
			http.Error(w, "Invalid transition", http.StatusBadRequest)
			return
		}
		http.Error(w, "Failed to transition work item: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(workItem)
}

// TestWorkItemAPI_Integration tests the work item API integration
func TestWorkItemAPI_Integration(t *testing.T) {
	// Create mock service
	mockService := NewMockWorkItemService()
	defer mockService.Close()

	// Create test client that redirects to mock service
	testClient := NewTestClient(mockService.GetServerURL())

	// Set up router
	router := mux.NewRouter()
	handler := &testWorkItemHandler{
		javaClient: testClient,
	}
	
	// Register routes
	router.HandleFunc("/workitems", handler.ListWorkItems).Methods("GET")
	router.HandleFunc("/workitems", handler.CreateWorkItem).Methods("POST")
	router.HandleFunc("/workitems/{id}", handler.GetWorkItem).Methods("GET") 
	router.HandleFunc("/workitems/{id}", handler.UpdateWorkItem).Methods("PUT")
	router.HandleFunc("/workitems/{id}/transitions", handler.TransitionWorkItem).Methods("POST")

	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()

	// Test create work item
	t.Run("CreateWorkItem", func(t *testing.T) {
		// Create request body
		requestBody := models.WorkItemCreateRequest{
			Title:       "Integration Test Work Item",
			Description: "This is an integration test work item",
			Type:        models.WorkItemTypeFeature,
			Priority:    models.PriorityMedium,
		}

		// Convert request body to JSON
		bodyBytes, err := json.Marshal(requestBody)
		if err != nil {
			t.Fatal(err)
		}

		// Create request
		req, err := http.NewRequest("POST", server.URL+"/workitems", bytes.NewBuffer(bodyBytes))
		if err != nil {
			t.Fatal(err)
		}
		req.Header.Set("Content-Type", "application/json")

		// Send request
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatal(err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusCreated {
			t.Errorf("expected status Created, got %v", resp.StatusCode)
		}

		// Parse response
		var workItem models.WorkItem
		if err := json.NewDecoder(resp.Body).Decode(&workItem); err != nil {
			t.Fatal(err)
		}

		// Verify response
		if workItem.Title != requestBody.Title {
			t.Errorf("expected title %q, got %q", requestBody.Title, workItem.Title)
		}
		if workItem.Type != requestBody.Type {
			t.Errorf("expected type %q, got %q", requestBody.Type, workItem.Type)
		}
		if workItem.Status != models.WorkflowStateFound {
			t.Errorf("expected status %q, got %q", models.WorkflowStateFound, workItem.Status)
		}

		// Save work item ID for subsequent tests
		workItemID := workItem.ID.String()

		// Test get work item
		t.Run("GetWorkItem", func(t *testing.T) {
			// Create request
			req, err := http.NewRequest("GET", server.URL+"/workitems/"+workItemID, nil)
			if err != nil {
				t.Fatal(err)
			}

			// Send request
			resp, err := http.DefaultClient.Do(req)
			if err != nil {
				t.Fatal(err)
			}
			defer resp.Body.Close()

			// Check response status
			if resp.StatusCode != http.StatusOK {
				t.Errorf("expected status OK, got %v", resp.StatusCode)
			}

			// Parse response
			var workItem models.WorkItem
			if err := json.NewDecoder(resp.Body).Decode(&workItem); err != nil {
				t.Fatal(err)
			}

			// Verify response
			if workItem.ID.String() != workItemID {
				t.Errorf("expected ID %q, got %q", workItemID, workItem.ID.String())
			}
			if workItem.Title != requestBody.Title {
				t.Errorf("expected title %q, got %q", requestBody.Title, workItem.Title)
			}
		})

		// Test update work item
		t.Run("UpdateWorkItem", func(t *testing.T) {
			// Create new title and description
			newTitle := "Updated Integration Test Work Item"
			newDescription := "This work item has been updated"
			newPriority := models.PriorityHigh

			// Create request body
			updateRequestBody := models.WorkItemUpdateRequest{
				Title:       &newTitle,
				Description: &newDescription,
				Priority:    &newPriority,
			}

			// Convert request body to JSON
			bodyBytes, err := json.Marshal(updateRequestBody)
			if err != nil {
				t.Fatal(err)
			}

			// Create request
			req, err := http.NewRequest("PUT", server.URL+"/workitems/"+workItemID, bytes.NewBuffer(bodyBytes))
			if err != nil {
				t.Fatal(err)
			}
			req.Header.Set("Content-Type", "application/json")

			// Send request
			resp, err := http.DefaultClient.Do(req)
			if err != nil {
				t.Fatal(err)
			}
			defer resp.Body.Close()

			// Check response status
			if resp.StatusCode != http.StatusOK {
				t.Errorf("expected status OK, got %v", resp.StatusCode)
			}

			// Parse response
			var workItem models.WorkItem
			if err := json.NewDecoder(resp.Body).Decode(&workItem); err != nil {
				t.Fatal(err)
			}

			// Verify response
			if workItem.Title != newTitle {
				t.Errorf("expected title %q, got %q", newTitle, workItem.Title)
			}
			if workItem.Description != newDescription {
				t.Errorf("expected description %q, got %q", newDescription, workItem.Description)
			}
			if workItem.Priority != newPriority {
				t.Errorf("expected priority %q, got %q", newPriority, workItem.Priority)
			}
		})

		// Test transition work item
		t.Run("TransitionWorkItem", func(t *testing.T) {
			// Create request body
			transitionRequestBody := models.WorkItemTransitionRequest{
				ToState: models.WorkflowStateInDev,
				Comment: "Moving to development",
			}

			// Convert request body to JSON
			bodyBytes, err := json.Marshal(transitionRequestBody)
			if err != nil {
				t.Fatal(err)
			}

			// Create request
			req, err := http.NewRequest("POST", server.URL+"/workitems/"+workItemID+"/transitions", bytes.NewBuffer(bodyBytes))
			if err != nil {
				t.Fatal(err)
			}
			req.Header.Set("Content-Type", "application/json")

			// Send request
			resp, err := http.DefaultClient.Do(req)
			if err != nil {
				t.Fatal(err)
			}
			defer resp.Body.Close()

			// Check response status
			if resp.StatusCode != http.StatusOK {
				t.Errorf("expected status OK, got %v", resp.StatusCode)
			}

			// Parse response
			var workItem models.WorkItem
			if err := json.NewDecoder(resp.Body).Decode(&workItem); err != nil {
				t.Fatal(err)
			}

			// Verify response
			if workItem.Status != models.WorkflowStateInDev {
				t.Errorf("expected status %q, got %q", models.WorkflowStateInDev, workItem.Status)
			}
		})

		// Test invalid transition
		t.Run("InvalidTransition", func(t *testing.T) {
			// Create request body with invalid transition
			transitionRequestBody := models.WorkItemTransitionRequest{
				ToState: models.WorkflowStateDone, // Cannot go directly from IN_DEV to DONE
				Comment: "Invalid transition",
			}

			// Convert request body to JSON
			bodyBytes, err := json.Marshal(transitionRequestBody)
			if err != nil {
				t.Fatal(err)
			}

			// Create request
			req, err := http.NewRequest("POST", server.URL+"/workitems/"+workItemID+"/transitions", bytes.NewBuffer(bodyBytes))
			if err != nil {
				t.Fatal(err)
			}
			req.Header.Set("Content-Type", "application/json")

			// Send request
			resp, err := http.DefaultClient.Do(req)
			if err != nil {
				t.Fatal(err)
			}
			defer resp.Body.Close()

			// Check response status
			if resp.StatusCode != http.StatusBadRequest {
				t.Errorf("expected status BadRequest, got %v", resp.StatusCode)
			}
		})
	})

	// Test list work items
	t.Run("ListWorkItems", func(t *testing.T) {
		// Create request
		req, err := http.NewRequest("GET", server.URL+"/workitems", nil)
		if err != nil {
			t.Fatal(err)
		}

		// Send request
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatal(err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusOK {
			t.Errorf("expected status OK, got %v", resp.StatusCode)
		}

		// Parse response
		var workItems models.WorkItemListResponse
		if err := json.NewDecoder(resp.Body).Decode(&workItems); err != nil {
			t.Fatal(err)
		}

		// Verify response
		if workItems.TotalCount == 0 {
			t.Errorf("expected at least 1 work item, got %d", workItems.TotalCount)
		}
	})

	// Test get non-existent work item
	t.Run("GetNonExistentWorkItem", func(t *testing.T) {
		// Create request with non-existent ID
		nonExistentID := uuid.New().String()
		req, err := http.NewRequest("GET", server.URL+"/workitems/"+nonExistentID, nil)
		if err != nil {
			t.Fatal(err)
		}

		// Send request
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatal(err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusNotFound {
			t.Errorf("expected status NotFound, got %v", resp.StatusCode)
		}
	})
}

// TestWorkItemAPIPerformance_Integration tests the performance of work item endpoints
func TestWorkItemAPIPerformance_Integration(t *testing.T) {
	// Skip in short mode
	if testing.Short() {
		t.Skip("skipping test in short mode.")
	}

	// Create mock service
	mockService := NewMockWorkItemService()
	defer mockService.Close()

	// Create test client that redirects to mock service
	testClient := NewTestClient(mockService.GetServerURL())

	// Set up router
	router := mux.NewRouter()
	handler := &testWorkItemHandler{
		javaClient: testClient,
	}
	
	// Register routes
	router.HandleFunc("/workitems", handler.ListWorkItems).Methods("GET")
	router.HandleFunc("/workitems", handler.CreateWorkItem).Methods("POST")
	router.HandleFunc("/workitems/{id}", handler.GetWorkItem).Methods("GET") 
	router.HandleFunc("/workitems/{id}", handler.UpdateWorkItem).Methods("PUT")
	router.HandleFunc("/workitems/{id}/transitions", handler.TransitionWorkItem).Methods("POST")

	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()

	// Create some test work items
	createdIDs := make([]string, 0)
	for i := 0; i < 10; i++ {
		// Create request body
		requestBody := models.WorkItemCreateRequest{
			Title:       fmt.Sprintf("Performance Test Work Item %d", i),
			Description: "This is a performance test work item",
			Type:        models.WorkItemTypeFeature,
			Priority:    models.PriorityMedium,
		}

		// Convert request body to JSON
		bodyBytes, err := json.Marshal(requestBody)
		if err != nil {
			t.Fatal(err)
		}

		// Create request
		req, err := http.NewRequest("POST", server.URL+"/workitems", bytes.NewBuffer(bodyBytes))
		if err != nil {
			t.Fatal(err)
		}
		req.Header.Set("Content-Type", "application/json")

		// Send request
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatal(err)
		}

		// Parse response
		var workItem models.WorkItem
		if err := json.NewDecoder(resp.Body).Decode(&workItem); err != nil {
			resp.Body.Close()
			t.Fatal(err)
		}
		resp.Body.Close()

		// Save work item ID
		createdIDs = append(createdIDs, workItem.ID.String())
	}

	// Performance test settings
	const (
		concurrentUsers = 10
		requestsPerUser = 5
	)

	// Create channels to collect metrics
	listTimes := make(chan time.Duration, concurrentUsers*requestsPerUser)
	getTimes := make(chan time.Duration, concurrentUsers*requestsPerUser)
	updateTimes := make(chan time.Duration, concurrentUsers*requestsPerUser)
	transitionTimes := make(chan time.Duration, concurrentUsers*requestsPerUser)

	// Start concurrent users
	done := make(chan struct{})
	defer close(done)

	// Create a wait group to track completion
	var wg sync.WaitGroup
	wg.Add(concurrentUsers)

	// Start test
	startTime := time.Now()

	for i := 0; i < concurrentUsers; i++ {
		go func(userID int) {
			defer wg.Done()

			// Run requests
			for j := 0; j < requestsPerUser; j++ {
				// Test list
				startList := time.Now()
				req, _ := http.NewRequest("GET", server.URL+"/workitems", nil)
				resp, err := http.DefaultClient.Do(req)
				if err == nil {
					resp.Body.Close()
					listTimes <- time.Since(startList)
				}

				// Test get
				if len(createdIDs) > 0 {
					// Pick a random ID
					idx := (userID + j) % len(createdIDs)
					id := createdIDs[idx]

					// Get request
					startGet := time.Now()
					req, _ = http.NewRequest("GET", server.URL+"/workitems/"+id, nil)
					resp, err = http.DefaultClient.Do(req)
					if err == nil {
						resp.Body.Close()
						getTimes <- time.Since(startGet)
					}

					// Update request
					newTitle := fmt.Sprintf("Updated Performance Item %d-%d", userID, j)
					updateReq := models.WorkItemUpdateRequest{
						Title: &newTitle,
					}
					bodyBytes, _ := json.Marshal(updateReq)

					startUpdate := time.Now()
					req, _ = http.NewRequest("PUT", server.URL+"/workitems/"+id, bytes.NewBuffer(bodyBytes))
					req.Header.Set("Content-Type", "application/json")
					resp, err = http.DefaultClient.Do(req)
					if err == nil {
						resp.Body.Close()
						updateTimes <- time.Since(startUpdate)
					}

					// Transition request
					transitionReq := models.WorkItemTransitionRequest{
						ToState: models.WorkflowStateInDev,
						Comment: "Performance test transition",
					}
					bodyBytes, _ = json.Marshal(transitionReq)

					startTransition := time.Now()
					req, _ = http.NewRequest("POST", server.URL+"/workitems/"+id+"/transitions", bytes.NewBuffer(bodyBytes))
					req.Header.Set("Content-Type", "application/json")
					resp, err = http.DefaultClient.Do(req)
					if err == nil {
						resp.Body.Close()
						transitionTimes <- time.Since(startTransition)
					}
				}
			}
		}(i)
	}

	// Wait for all users to finish
	wg.Wait()
	totalDuration := time.Since(startTime)

	// Calculate metrics
	close(listTimes)
	close(getTimes)
	close(updateTimes)
	close(transitionTimes)

	// List metrics
	var totalListTime time.Duration
	listCount := 0
	for t := range listTimes {
		totalListTime += t
		listCount++
	}
	avgListTime := totalListTime / time.Duration(listCount)

	// Get metrics
	var totalGetTime time.Duration
	getCount := 0
	for t := range getTimes {
		totalGetTime += t
		getCount++
	}
	avgGetTime := totalGetTime / time.Duration(getCount)

	// Update metrics
	var totalUpdateTime time.Duration
	updateCount := 0
	for t := range updateTimes {
		totalUpdateTime += t
		updateCount++
	}
	avgUpdateTime := totalUpdateTime / time.Duration(updateCount)

	// Transition metrics
	var totalTransitionTime time.Duration
	transitionCount := 0
	for t := range transitionTimes {
		totalTransitionTime += t
		transitionCount++
	}
	avgTransitionTime := totalTransitionTime / time.Duration(transitionCount)

	// Log performance metrics
	t.Logf("Performance Test Results:")
	t.Logf("  Total Duration: %v", totalDuration)
	t.Logf("  Concurrent Users: %d", concurrentUsers)
	t.Logf("  Requests Per User: %d", requestsPerUser)
	t.Logf("  List Work Items: Avg=%v, Count=%d", avgListTime, listCount)
	t.Logf("  Get Work Item: Avg=%v, Count=%d", avgGetTime, getCount)
	t.Logf("  Update Work Item: Avg=%v, Count=%d", avgUpdateTime, updateCount)
	t.Logf("  Transition Work Item: Avg=%v, Count=%d", avgTransitionTime, transitionCount)

	// Assert performance meets requirements (adjust thresholds as needed)
	const maxAvgResponseTime = 50 * time.Millisecond
	if avgListTime > maxAvgResponseTime {
		t.Errorf("Average list time too high: %v (threshold: %v)", avgListTime, maxAvgResponseTime)
	}
	if avgGetTime > maxAvgResponseTime {
		t.Errorf("Average get time too high: %v (threshold: %v)", avgGetTime, maxAvgResponseTime)
	}
	if avgUpdateTime > maxAvgResponseTime {
		t.Errorf("Average update time too high: %v (threshold: %v)", avgUpdateTime, maxAvgResponseTime)
	}
	if avgTransitionTime > maxAvgResponseTime {
		t.Errorf("Average transition time too high: %v (threshold: %v)", avgTransitionTime, maxAvgResponseTime)
	}
}