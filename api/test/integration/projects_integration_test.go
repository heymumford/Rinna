// +build integration

/*
 * Integration tests for project API endpoints
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
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/models"
	"github.com/heymumford/rinna/api/pkg/config"
)

// MockJavaService implements a mock Java service for integration testing
type MockJavaService struct {
	projects map[string]*models.Project
	workItems map[string][]models.WorkItem
	mux      *mux.Router
	server   *httptest.Server
}

// Initialize the mock Java service with test data
func NewMockJavaService() *MockJavaService {
	service := &MockJavaService{
		projects:  make(map[string]*models.Project),
		workItems: make(map[string][]models.WorkItem),
		mux:       mux.NewRouter(),
	}

	// Register API endpoints
	service.mux.HandleFunc("/api/projects", service.listProjects).Methods(http.MethodGet)
	service.mux.HandleFunc("/api/projects", service.createProject).Methods(http.MethodPost)
	service.mux.HandleFunc("/api/projects/{key}", service.getProject).Methods(http.MethodGet)
	service.mux.HandleFunc("/api/projects/{key}", service.updateProject).Methods(http.MethodPut)
	service.mux.HandleFunc("/api/projects/{key}/workitems", service.getProjectWorkItems).Methods(http.MethodGet)
	service.mux.HandleFunc("/health", service.healthCheck).Methods(http.MethodGet)

	// Create test server
	service.server = httptest.NewServer(service.mux)

	// Populate with some initial test data
	testProject1 := &models.Project{
		ID:          uuid.New(),
		Key:         "TEST1",
		Name:        "Test Project 1",
		Description: "This is a test project for integration testing",
		Active:      true,
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}
	
	testProject2 := &models.Project{
		ID:          uuid.New(),
		Key:         "TEST2",
		Name:        "Test Project 2",
		Description: "This is another test project for integration testing",
		Active:      true,
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}
	
	// Add projects to the map
	service.projects[testProject1.Key] = testProject1
	service.projects[testProject2.Key] = testProject2
	
	// Add work items for TEST1 project
	service.workItems["TEST1"] = []models.WorkItem{
		{
			ID:          uuid.New(),
			Title:       "TEST1-1: Implement feature X",
			Description: "Implement feature X for the product",
			Type:        models.WorkItemTypeFeature,
			Priority:    models.PriorityHigh,
			Status:      models.WorkflowStateInDev,
			ProjectID:   "TEST1",
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
		{
			ID:          uuid.New(),
			Title:       "TEST1-2: Fix bug in component Y",
			Description: "There's a bug in component Y that needs to be fixed",
			Type:        models.WorkItemTypeBug,
			Priority:    models.PriorityMedium,
			Status:      models.WorkflowStateTesting,
			ProjectID:   "TEST1",
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
	}

	return service
}

// Close stops the mock server
func (s *MockJavaService) Close() {
	if s.server != nil {
		s.server.Close()
	}
}

// healthCheck handles the health check endpoint
func (s *MockJavaService) healthCheck(w http.ResponseWriter, r *http.Request) {
	response := map[string]string{
		"status": "ok",
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// listProjects handles listing projects
func (s *MockJavaService) listProjects(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters
	page := 1
	pageSize := 10
	activeOnly := false

	if pageStr := r.URL.Query().Get("page"); pageStr != "" {
		if parsed, err := parseInt(pageStr); err == nil && parsed > 0 {
			page = parsed
		}
	}

	if pageSizeStr := r.URL.Query().Get("pageSize"); pageSizeStr != "" {
		if parsed, err := parseInt(pageSizeStr); err == nil && parsed > 0 && parsed <= 100 {
			pageSize = parsed
		}
	}

	if activeOnlyStr := r.URL.Query().Get("activeOnly"); activeOnlyStr != "" {
		if parsed, err := parseBool(activeOnlyStr); err == nil {
			activeOnly = parsed
		}
	}

	// Filter and paginate projects
	var items []models.Project
	for _, project := range s.projects {
		if !activeOnly || project.Active {
			items = append(items, *project)
		}
	}

	// Create response
	response := models.ProjectListResponse{
		Items:      items,
		TotalCount: len(items),
		Page:       page,
		PageSize:   pageSize,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// createProject handles creating a project
func (s *MockJavaService) createProject(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var request models.ProjectCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "Invalid request body"})
		return
	}

	// Validate request
	if request.Key == "" || request.Name == "" {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "Key and name are required"})
		return
	}

	// Check if project with this key already exists
	if _, exists := s.projects[request.Key]; exists {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusConflict)
		json.NewEncoder(w).Encode(map[string]string{"error": "Project with this key already exists"})
		return
	}

	// Create the project
	now := time.Now()
	project := &models.Project{
		ID:          uuid.New(),
		Key:         request.Key,
		Name:        request.Name,
		Description: request.Description,
		Active:      true,
		Metadata:    request.Metadata,
		CreatedAt:   now,
		UpdatedAt:   now,
	}

	// Store the project
	s.projects[project.Key] = project

	// Return the created project
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(project)
}

// getProject handles getting a project
func (s *MockJavaService) getProject(w http.ResponseWriter, r *http.Request) {
	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Look up the project
	project, exists := s.projects[key]
	if !exists {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "project not found: " + key})
		return
	}

	// Return the project
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(project)
}

// updateProject handles updating a project
func (s *MockJavaService) updateProject(w http.ResponseWriter, r *http.Request) {
	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Look up the project
	project, exists := s.projects[key]
	if !exists {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "project not found: " + key})
		return
	}

	// Parse request body
	var request models.ProjectUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "Invalid request body"})
		return
	}

	// Update the project
	if request.Name != nil {
		project.Name = *request.Name
	}
	if request.Description != nil {
		project.Description = *request.Description
	}
	if request.Active != nil {
		project.Active = *request.Active
	}
	if request.Metadata != nil {
		project.Metadata = request.Metadata
	}

	project.UpdatedAt = time.Now()

	// Return the updated project
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(project)
}

// getProjectWorkItems handles getting work items for a project
func (s *MockJavaService) getProjectWorkItems(w http.ResponseWriter, r *http.Request) {
	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Check if project exists
	if _, exists := s.projects[key]; !exists {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{"error": "project not found: " + key})
		return
	}

	// Parse query parameters
	status := r.URL.Query().Get("status")
	page := 1
	pageSize := 10

	if pageStr := r.URL.Query().Get("page"); pageStr != "" {
		if parsed, err := parseInt(pageStr); err == nil && parsed > 0 {
			page = parsed
		}
	}

	if pageSizeStr := r.URL.Query().Get("pageSize"); pageSizeStr != "" {
		if parsed, err := parseInt(pageSizeStr); err == nil && parsed > 0 && parsed <= 100 {
			pageSize = parsed
		}
	}

	// Filter and paginate work items
	var items []models.WorkItem
	if workItems, ok := s.workItems[key]; ok {
		for _, workItem := range workItems {
			if status == "" || workItem.Status == models.WorkflowState(status) {
				items = append(items, workItem)
			}
		}
	}

	// Create response
	response := models.WorkItemListResponse{
		Items:      items,
		TotalCount: len(items),
		Page:       page,
		PageSize:   pageSize,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// Helper function to parse int with error handling
func parseInt(s string) (int, error) {
	var value int
	_, err := fmt.Sscanf(s, "%d", &value)
	return value, err
}

// Helper function to parse bool with error handling
func parseBool(s string) (bool, error) {
	var value bool
	_, err := fmt.Sscanf(s, "%t", &value)
	return value, err
}

// mockTransport is a custom http.RoundTripper that redirects requests to the mock service
type mockTransport struct {
	mockURL string
}

// RoundTrip implements http.RoundTripper
func (t *mockTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	// Replace the host with the mock service host
	url := t.mockURL + req.URL.Path
	if req.URL.RawQuery != "" {
		url += "?" + req.URL.RawQuery
	}

	// Create a new request with the same method, body, and headers
	newReq, err := http.NewRequestWithContext(
		req.Context(),
		req.Method,
		url,
		req.Body,
	)
	if err != nil {
		return nil, err
	}

	// Copy headers
	newReq.Header = req.Header

	// Use the default transport to make the actual request
	return http.DefaultTransport.RoundTrip(newReq)
}

// TestJavaClient is a custom implementation of client.JavaClient for testing
type TestJavaClient struct {
	config     *config.JavaServiceConfig
	httpClient *http.Client
}

// Request sends a request to the Java service via the mock transport
func (c *TestJavaClient) Request(ctx context.Context, method, path string, payload interface{}, response interface{}) error {
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

	// Send the request using our custom HTTP client
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

// ListProjects retrieves a list of projects from the Java service
func (c *TestJavaClient) ListProjects(ctx context.Context, page, pageSize int, activeOnly bool) (*models.ProjectListResponse, error) {
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
	
	query := ""
	if len(params) > 0 {
		query = "?"
		first := true
		for key, value := range params {
			if !first {
				query += "&"
			}
			query += fmt.Sprintf("%s=%s", key, value)
			first = false
		}
	}

	// Send the request
	var response models.ProjectListResponse
	err := c.Request(ctx, http.MethodGet, endpoint+query, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to list projects: %v", err)
	}

	return &response, nil
}

// CreateProject creates a new project in the Java service
func (c *TestJavaClient) CreateProject(ctx context.Context, request models.ProjectCreateRequest) (*models.Project, error) {
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
func (c *TestJavaClient) GetProject(ctx context.Context, key string) (*models.Project, error) {
	endpoint := "/api/projects"
	if c.config.Endpoints != nil && c.config.Endpoints["projects"] != "" {
		endpoint = c.config.Endpoints["projects"]
	}

	// Send the request
	var response models.Project
	err := c.Request(ctx, http.MethodGet, endpoint+"/"+key, nil, &response)
	if err != nil {
		// Check if it's a "not found" error from our mock
		if strings.Contains(err.Error(), "received error response: project not found") {
			return nil, fmt.Errorf("project not found: %s", key)
		}
		return nil, fmt.Errorf("failed to get project: %v", err)
	}

	return &response, nil
}

// UpdateProject updates a project in the Java service
func (c *TestJavaClient) UpdateProject(ctx context.Context, key string, request models.ProjectUpdateRequest) (*models.Project, error) {
	endpoint := "/api/projects"
	if c.config.Endpoints != nil && c.config.Endpoints["projects"] != "" {
		endpoint = c.config.Endpoints["projects"]
	}

	// Send the request
	var response models.Project
	err := c.Request(ctx, http.MethodPut, endpoint+"/"+key, request, &response)
	if err != nil {
		// Check if it's a "not found" error from our mock
		if strings.Contains(err.Error(), "received error response: project not found") {
			return nil, fmt.Errorf("project not found: %s", key)
		}
		return nil, fmt.Errorf("failed to update project: %v", err)
	}

	return &response, nil
}

// GetProjectWorkItems retrieves work items for a project from the Java service
func (c *TestJavaClient) GetProjectWorkItems(ctx context.Context, key string, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
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
	
	query := ""
	if len(params) > 0 {
		query = "?"
		first := true
		for key, value := range params {
			if !first {
				query += "&"
			}
			query += fmt.Sprintf("%s=%s", key, value)
			first = false
		}
	}

	// Send the request
	var response models.WorkItemListResponse
	err := c.Request(ctx, http.MethodGet, endpoint+"/"+key+"/workitems"+query, nil, &response)
	if err != nil {
		// Check if it's a "not found" error from our mock
		if strings.Contains(err.Error(), "received error response: project not found") {
			return nil, fmt.Errorf("project not found: %s", key)
		}
		return nil, fmt.Errorf("failed to get project work items: %v", err)
	}

	return &response, nil
}

// ValidateToken is not used in these tests but required for the interface
func (c *TestJavaClient) ValidateToken(ctx context.Context, token string) (string, error) {
	return "", fmt.Errorf("not implemented for tests")
}

// GetWebhookSecret is not used in these tests but required for the interface
func (c *TestJavaClient) GetWebhookSecret(ctx context.Context, projectKey, source string) (string, error) {
	return "", fmt.Errorf("not implemented for tests")
}

// ProjectHandler is a test implementation of the projects handler
type ProjectHandler struct {
	javaClient *TestJavaClient
}

// ListProjects handles listing projects
func (h *ProjectHandler) ListProjects(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters
	activeOnlyStr := r.URL.Query().Get("activeOnly")
	pageStr := r.URL.Query().Get("page")
	pageSizeStr := r.URL.Query().Get("pageSize")

	// Set default values
	page := 1
	pageSize := 10
	activeOnly := false

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

	// Parse activeOnly parameter
	if activeOnlyStr != "" {
		parsedActiveOnly, err := strconv.ParseBool(activeOnlyStr)
		if err == nil {
			activeOnly = parsedActiveOnly
		}
	}

	// Call the Java service
	response, err := h.javaClient.ListProjects(r.Context(), page, pageSize, activeOnly)
	if err != nil {
		http.Error(w, "Failed to list projects: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// CreateProject handles creating a project
func (h *ProjectHandler) CreateProject(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var request models.ProjectCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Validate request
	if request.Key == "" {
		http.Error(w, "Key is required", http.StatusBadRequest)
		return
	}
	if request.Name == "" {
		http.Error(w, "Name is required", http.StatusBadRequest)
		return
	}

	// Call the Java service
	project, err := h.javaClient.CreateProject(r.Context(), request)
	if err != nil {
		http.Error(w, "Failed to create project: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(project)
}

// GetProject handles getting a project
func (h *ProjectHandler) GetProject(w http.ResponseWriter, r *http.Request) {
	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Call the Java service
	project, err := h.javaClient.GetProject(r.Context(), key)
	if err != nil {
		if err.Error() == "project not found: "+key {
			http.Error(w, "Project not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get project: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(project)
}

// UpdateProject handles updating a project
func (h *ProjectHandler) UpdateProject(w http.ResponseWriter, r *http.Request) {
	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Parse request body
	var request models.ProjectUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Call the Java service
	project, err := h.javaClient.UpdateProject(r.Context(), key, request)
	if err != nil {
		if err.Error() == "project not found: "+key {
			http.Error(w, "Project not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to update project: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(project)
}

// GetProjectWorkItems handles getting work items for a project
func (h *ProjectHandler) GetProjectWorkItems(w http.ResponseWriter, r *http.Request) {
	// Extract the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

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
	response, err := h.javaClient.GetProjectWorkItems(r.Context(), key, status, page, pageSize)
	if err != nil {
		if err.Error() == "project not found: "+key {
			http.Error(w, "Project not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get project work items: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// TestProjectAPIEndpoints_Integration tests all project endpoints with the API handlers
func TestProjectAPIEndpoints_Integration(t *testing.T) {
	// Set up the mock Java service
	mockJavaService := NewMockJavaService()
	defer mockJavaService.Close()

	// Extract the base URL without the trailing slash
	mockServiceURL := mockJavaService.server.URL

	// Create Java client config
	javaConfig := &config.JavaServiceConfig{
		Host:          "localhost",
		Port:          8080, // This is ignored since we're using the test server URL directly
		ConnectTimeout: 5000,
		RequestTimeout: 5000,
		Endpoints: map[string]string{
			"health":   "/health",
			"projects": "/api/projects",
		},
	}

	// Create custom HTTP client to intercept requests and redirect to mock service
	httpClient := &http.Client{
		Transport: &mockTransport{
			mockURL: mockServiceURL,
		},
	}
	
	// Create our test Java client that uses the custom HTTP client
	javaClient := &TestJavaClient{
		config:     javaConfig,
		httpClient: httpClient,
	}

	// Create the API router with our own project handler since we can't use RegisterProjectRoutes directly
	router := mux.NewRouter()
	
	// Create a project handler that uses our test client
	projectHandler := &ProjectHandler{javaClient: javaClient}
	
	// Register routes manually (mirroring what handlers.RegisterProjectRoutes would do)
	router.HandleFunc("/projects", projectHandler.ListProjects).Methods(http.MethodGet)
	router.HandleFunc("/projects", projectHandler.CreateProject).Methods(http.MethodPost)
	router.HandleFunc("/projects/{key}", projectHandler.GetProject).Methods(http.MethodGet)
	router.HandleFunc("/projects/{key}", projectHandler.UpdateProject).Methods(http.MethodPut)
	router.HandleFunc("/projects/{key}/workitems", projectHandler.GetProjectWorkItems).Methods(http.MethodGet)

	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()

	// Test cases
	t.Run("List projects", func(t *testing.T) {
		resp, err := http.Get(server.URL + "/projects?activeOnly=true")
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}

		// Parse response
		var response models.ProjectListResponse
		if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate response
		if len(response.Items) < 2 {
			t.Errorf("Expected at least 2 items, got %d", len(response.Items))
		}
	})

	t.Run("Get project", func(t *testing.T) {
		resp, err := http.Get(server.URL + "/projects/TEST1")
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}

		// Parse response
		var project models.Project
		if err := json.NewDecoder(resp.Body).Decode(&project); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate response
		if project.Key != "TEST1" {
			t.Errorf("Expected project key TEST1, got %s", project.Key)
		}
	})

	t.Run("Get non-existent project", func(t *testing.T) {
		resp, err := http.Get(server.URL + "/projects/NONEXISTENT")
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusNotFound {
			t.Errorf("Expected status code %d, got %d", http.StatusNotFound, resp.StatusCode)
		}
	})

	t.Run("Create project", func(t *testing.T) {
		// Create request body
		projectRequest := models.ProjectCreateRequest{
			Key:         "TEST3",
			Name:        "Test Project 3",
			Description: "This is a new test project created during integration testing",
		}

		bodyBytes, err := json.Marshal(projectRequest)
		if err != nil {
			t.Fatalf("Failed to marshal request: %v", err)
		}

		// Send POST request
		resp, err := http.Post(server.URL+"/projects", "application/json", bytes.NewBuffer(bodyBytes))
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusCreated {
			t.Errorf("Expected status code %d, got %d", http.StatusCreated, resp.StatusCode)
		}

		// Parse response
		var project models.Project
		if err := json.NewDecoder(resp.Body).Decode(&project); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate response
		if project.Key != "TEST3" {
			t.Errorf("Expected project key TEST3, got %s", project.Key)
		}
		if project.Name != "Test Project 3" {
			t.Errorf("Expected project name 'Test Project 3', got '%s'", project.Name)
		}
	})

	t.Run("Update project", func(t *testing.T) {
		// Create request body
		updatedName := "Updated Test Project 1"
		projectRequest := models.ProjectUpdateRequest{
			Name: &updatedName,
		}

		bodyBytes, err := json.Marshal(projectRequest)
		if err != nil {
			t.Fatalf("Failed to marshal request: %v", err)
		}

		// Create request
		req, err := http.NewRequest(http.MethodPut, server.URL+"/projects/TEST1", bytes.NewBuffer(bodyBytes))
		if err != nil {
			t.Fatalf("Failed to create request: %v", err)
		}
		req.Header.Set("Content-Type", "application/json")

		// Send PUT request
		client := &http.Client{}
		resp, err := client.Do(req)
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}

		// Parse response
		var project models.Project
		if err := json.NewDecoder(resp.Body).Decode(&project); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate response
		if project.Key != "TEST1" {
			t.Errorf("Expected project key TEST1, got %s", project.Key)
		}
		if project.Name != updatedName {
			t.Errorf("Expected project name '%s', got '%s'", updatedName, project.Name)
		}
	})

	t.Run("Get project work items", func(t *testing.T) {
		resp, err := http.Get(server.URL + "/projects/TEST1/workitems")
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}

		// Parse response
		var response models.WorkItemListResponse
		if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate response
		if len(response.Items) < 2 {
			t.Errorf("Expected at least 2 work items, got %d", len(response.Items))
		}
	})

	t.Run("Get project work items with filter", func(t *testing.T) {
		resp, err := http.Get(server.URL + "/projects/TEST1/workitems?status=IN_DEV")
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}

		// Parse response
		var response models.WorkItemListResponse
		if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate all items have IN_DEV status
		for _, item := range response.Items {
			if item.Status != models.WorkflowStateInDev {
				t.Errorf("Expected all items to have status IN_DEV, got %s", item.Status)
			}
		}
	})
}

// TestProjectAPIPerformance_Integration tests the performance of project endpoints
func TestProjectAPIPerformance_Integration(t *testing.T) {
	// Set up the mock Java service
	mockJavaService := NewMockJavaService()
	defer mockJavaService.Close()

	// Extract the base URL without the trailing slash
	mockServiceURL := mockJavaService.server.URL

	// Create Java client config
	javaConfig := &config.JavaServiceConfig{
		Host:          "localhost",
		Port:          8080, // This is ignored since we're using the test server URL directly
		ConnectTimeout: 5000,
		RequestTimeout: 5000,
		Endpoints: map[string]string{
			"health":   "/health",
			"projects": "/api/projects",
		},
	}

	// Create custom HTTP client to intercept requests and redirect to mock service
	httpClient := &http.Client{
		Transport: &mockTransport{
			mockURL: mockServiceURL,
		},
	}
	
	// Create our test Java client that uses the custom HTTP client
	javaClient := &TestJavaClient{
		config:     javaConfig,
		httpClient: httpClient,
	}

	// Create the API router with our own project handler since we can't use RegisterProjectRoutes directly
	router := mux.NewRouter()
	
	// Create a project handler that uses our test client
	projectHandler := &ProjectHandler{javaClient: javaClient}
	
	// Register routes manually (mirroring what handlers.RegisterProjectRoutes would do)
	router.HandleFunc("/projects", projectHandler.ListProjects).Methods(http.MethodGet)
	router.HandleFunc("/projects", projectHandler.CreateProject).Methods(http.MethodPost)
	router.HandleFunc("/projects/{key}", projectHandler.GetProject).Methods(http.MethodGet)
	router.HandleFunc("/projects/{key}", projectHandler.UpdateProject).Methods(http.MethodPut)
	router.HandleFunc("/projects/{key}/workitems", projectHandler.GetProjectWorkItems).Methods(http.MethodGet)

	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()

	// Performance test settings
	const (
		concurrentUsers = 10
		requestsPerUser = 5
	)

	// Create channels to collect metrics
	listTimes := make(chan time.Duration, concurrentUsers*requestsPerUser)
	getTimes := make(chan time.Duration, concurrentUsers*requestsPerUser)

	// Start concurrent users
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	// Create a wait group to track completion
	var wg sync.WaitGroup
	wg.Add(concurrentUsers)

	// Start test
	startTime := time.Now()

	for i := 0; i < concurrentUsers; i++ {
		go func(userID int) {
			defer wg.Done()

			// Create a client with a timeout
			client := &http.Client{
				Timeout: 5 * time.Second,
			}

			for j := 0; j < requestsPerUser; j++ {
				// Test list projects endpoint
				start := time.Now()
				req, err := http.NewRequestWithContext(ctx, "GET", server.URL+"/projects", nil)
				if err != nil {
					t.Errorf("Failed to create request: %v", err)
					continue
				}

				resp, err := client.Do(req)
				listTime := time.Since(start)
				listTimes <- listTime

				if err != nil {
					t.Errorf("Failed to make list request: %v", err)
					continue
				}
				resp.Body.Close()

				// Test get project endpoint
				start = time.Now()
				req, err = http.NewRequestWithContext(ctx, "GET", server.URL+"/projects/TEST1", nil)
				if err != nil {
					t.Errorf("Failed to create request: %v", err)
					continue
				}

				resp, err = client.Do(req)
				getTime := time.Since(start)
				getTimes <- getTime

				if err != nil {
					t.Errorf("Failed to make get request: %v", err)
					continue
				}
				resp.Body.Close()
			}
		}(i)
	}

	// Wait for all users to finish
	wg.Wait()
	totalDuration := time.Since(startTime)

	// Close channels
	close(listTimes)
	close(getTimes)

	// Calculate metrics
	var totalListTime, totalGetTime time.Duration
	var maxListTime, maxGetTime time.Duration
	listCount, getCount := 0, 0

	for t := range listTimes {
		totalListTime += t
		if t > maxListTime {
			maxListTime = t
		}
		listCount++
	}

	for t := range getTimes {
		totalGetTime += t
		if t > maxGetTime {
			maxGetTime = t
		}
		getCount++
	}

	// Calculate average times
	avgListTime := totalListTime / time.Duration(listCount)
	avgGetTime := totalGetTime / time.Duration(getCount)
	totalRequests := listCount + getCount

	// Report results
	t.Logf("Project API Performance Results:")
	t.Logf("  Concurrent users: %d", concurrentUsers)
	t.Logf("  Requests per user: %d", requestsPerUser)
	t.Logf("  Total duration: %v", totalDuration)
	t.Logf("  Total requests: %d", totalRequests)
	t.Logf("  Requests per second: %.2f", float64(totalRequests)/totalDuration.Seconds())
	t.Logf("  List projects endpoint:")
	t.Logf("    Average time: %v", avgListTime)
	t.Logf("    Maximum time: %v", maxListTime)
	t.Logf("  Get project endpoint:")
	t.Logf("    Average time: %v", avgGetTime)
	t.Logf("    Maximum time: %v", maxGetTime)

	// Assert performance meets requirements
	const maxAvgResponseTime = 50 * time.Millisecond
	if avgListTime > maxAvgResponseTime {
		t.Errorf("Average list time too high: %v (threshold: %v)", avgListTime, maxAvgResponseTime)
	}
	if avgGetTime > maxAvgResponseTime {
		t.Errorf("Average get time too high: %v (threshold: %v)", avgGetTime, maxAvgResponseTime)
	}
}