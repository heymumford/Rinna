// +build integration

/*
 * Integration tests for release API endpoints
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

// MockReleaseService implements a mock release service for integration testing
type MockReleaseService struct {
	releases  map[string]*models.Release
	workItems map[string][]models.WorkItem
	mux       *mux.Router
	server    *httptest.Server
}

// Initialize the mock release service with test data
func NewMockReleaseService() *MockReleaseService {
	service := &MockReleaseService{
		releases:  make(map[string]*models.Release),
		workItems: make(map[string][]models.WorkItem),
		mux:       mux.NewRouter(),
	}

	// Register API endpoints
	service.mux.HandleFunc("/api/releases", service.listReleases).Methods(http.MethodGet)
	service.mux.HandleFunc("/api/releases", service.createRelease).Methods(http.MethodPost)
	service.mux.HandleFunc("/api/releases/{id}", service.getRelease).Methods(http.MethodGet)
	service.mux.HandleFunc("/api/releases/{id}", service.updateRelease).Methods(http.MethodPut)
	service.mux.HandleFunc("/api/releases/{id}/workitems", service.getReleaseWorkItems).Methods(http.MethodGet)
	service.mux.HandleFunc("/health", service.healthCheck).Methods(http.MethodGet)

	// Create test server
	service.server = httptest.NewServer(service.mux)

	// Populate with some initial test data
	releaseID1 := uuid.New()
	testRelease1 := &models.Release{
		ID:          releaseID1,
		Name:        "Release 1.0",
		Version:     "1.0.0",
		Description: "First major release",
		Status:      models.ReleaseStatusPlanned,
		StartDate:   "2025-06-01",
		ReleaseDate: "2025-07-01",
		ProjectKey:  "TEST1",
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}
	
	releaseID2 := uuid.New()
	testRelease2 := &models.Release{
		ID:          releaseID2,
		Name:        "Release 1.1",
		Version:     "1.1.0",
		Description: "First minor update",
		Status:      models.ReleaseStatusInProgress,
		StartDate:   "2025-07-15",
		ReleaseDate: "2025-08-15",
		ProjectKey:  "TEST1",
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}
	
	// Add releases to the map
	service.releases[releaseID1.String()] = testRelease1
	service.releases[releaseID2.String()] = testRelease2
	
	// Add work items for Release 1.0
	service.workItems[releaseID1.String()] = []models.WorkItem{
		{
			ID:          uuid.New(),
			Title:       "Implement core feature X",
			Description: "Implement the core feature X for the 1.0 release",
			Type:        models.WorkItemTypeFeature,
			Priority:    models.PriorityHigh,
			Status:      models.WorkflowStateInDev,
			Metadata:    map[string]string{"releaseId": releaseID1.String()},
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
		{
			ID:          uuid.New(),
			Title:       "Fix critical bug Y",
			Description: "Fix the critical bug Y that was found in testing",
			Type:        models.WorkItemTypeBug,
			Priority:    models.PriorityHigh,
			Status:      models.WorkflowStateTesting,
			Metadata:    map[string]string{"releaseId": releaseID1.String()},
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
	}

	return service
}

// Close stops the mock server
func (s *MockReleaseService) Close() {
	if s.server != nil {
		s.server.Close()
	}
}

// healthCheck handles the health check endpoint
func (s *MockReleaseService) healthCheck(w http.ResponseWriter, r *http.Request) {
	response := map[string]string{
		"status": "ok",
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// listReleases handles listing releases with filtering and pagination
func (s *MockReleaseService) listReleases(w http.ResponseWriter, r *http.Request) {
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

	// Filter and paginate releases
	var items []models.Release
	for _, release := range s.releases {
		if status == "" || release.Status == models.ReleaseStatus(status) {
			items = append(items, *release)
		}
	}

	// Create response
	response := models.ReleaseListResponse{
		Items:      items,
		TotalCount: len(items),
		Page:       page,
		PageSize:   pageSize,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// createRelease handles creating a new release
func (s *MockReleaseService) createRelease(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var request models.ReleaseCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Validate request
	if request.Name == "" || request.Version == "" {
		http.Error(w, "Name and version are required", http.StatusBadRequest)
		return
	}

	// Create release object
	releaseID := uuid.New()
	now := time.Now()
	
	// Set default status if not provided
	if request.Status == "" {
		request.Status = models.ReleaseStatusPlanned
	}
	
	release := &models.Release{
		ID:          releaseID,
		Name:        request.Name,
		Version:     request.Version,
		Description: request.Description,
		Status:      request.Status,
		StartDate:   request.StartDate,
		ReleaseDate: request.ReleaseDate,
		ProjectKey:  request.ProjectKey,
		Metadata:    request.Metadata,
		CreatedAt:   now,
		UpdatedAt:   now,
	}

	// Store the release
	s.releases[releaseID.String()] = release

	// Return the created release
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(release)
}

// getRelease handles retrieving a specific release by ID
func (s *MockReleaseService) getRelease(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Look up the release
	release, exists := s.releases[id]
	if !exists {
		http.Error(w, "release not found: "+id, http.StatusNotFound)
		return
	}

	// Return the release
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(release)
}

// updateRelease handles updating an existing release
func (s *MockReleaseService) updateRelease(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Look up the release
	release, exists := s.releases[id]
	if !exists {
		http.Error(w, "release not found: "+id, http.StatusNotFound)
		return
	}

	// Parse request body
	var request models.ReleaseUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Update fields if provided
	if request.Name != nil {
		release.Name = *request.Name
	}
	if request.Description != nil {
		release.Description = *request.Description
	}
	if request.Status != nil {
		release.Status = *request.Status
	}
	if request.StartDate != nil {
		release.StartDate = *request.StartDate
	}
	if request.ReleaseDate != nil {
		release.ReleaseDate = *request.ReleaseDate
	}
	if request.Metadata != nil {
		release.Metadata = request.Metadata
	}

	// Update the timestamp
	release.UpdatedAt = time.Now()

	// Return the updated release
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(release)
}

// getReleaseWorkItems handles retrieving work items for a release
func (s *MockReleaseService) getReleaseWorkItems(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Check if release exists
	if _, exists := s.releases[id]; !exists {
		http.Error(w, "release not found: "+id, http.StatusNotFound)
		return
	}

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

	// Filter and paginate work items
	var items []models.WorkItem
	for _, workItem := range s.workItems[id] {
		if status == "" || workItem.Status == models.WorkflowState(status) {
			items = append(items, workItem)
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

// TestJavaClientForReleases is a custom implementation to work with our mock release service
type TestJavaClientForReleases struct {
	config     *config.JavaServiceConfig
	httpClient *http.Client
}

// Request sends a request to the Java service via the mock transport
func (c *TestJavaClientForReleases) Request(ctx context.Context, method, path string, payload interface{}, response interface{}) error {
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

// ListReleases retrieves a list of releases from the Java service
func (c *TestJavaClientForReleases) ListReleases(ctx context.Context, page, pageSize int, status string) (*models.ReleaseListResponse, error) {
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
	var response models.ReleaseListResponse
	err := c.Request(ctx, http.MethodGet, endpoint+query, nil, &response)
	if err != nil {
		return nil, fmt.Errorf("failed to list releases: %v", err)
	}

	return &response, nil
}

// CreateRelease creates a new release in the Java service
func (c *TestJavaClientForReleases) CreateRelease(ctx context.Context, request models.ReleaseCreateRequest) (*models.Release, error) {
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
func (c *TestJavaClientForReleases) GetRelease(ctx context.Context, id string) (*models.Release, error) {
	endpoint := "/api/releases"
	if c.config.Endpoints != nil && c.config.Endpoints["releases"] != "" {
		endpoint = c.config.Endpoints["releases"]
	}

	// Send the request
	var response models.Release
	err := c.Request(ctx, http.MethodGet, endpoint+"/"+id, nil, &response)
	if err != nil {
		// Check if it's a "not found" error from our mock
		if strings.Contains(err.Error(), "received error response: release not found") {
			return nil, fmt.Errorf("release not found: %s", id)
		}
		return nil, fmt.Errorf("failed to get release: %v", err)
	}

	return &response, nil
}

// UpdateRelease updates a release in the Java service
func (c *TestJavaClientForReleases) UpdateRelease(ctx context.Context, id string, request models.ReleaseUpdateRequest) (*models.Release, error) {
	endpoint := "/api/releases"
	if c.config.Endpoints != nil && c.config.Endpoints["releases"] != "" {
		endpoint = c.config.Endpoints["releases"]
	}

	// Send the request
	var response models.Release
	err := c.Request(ctx, http.MethodPut, endpoint+"/"+id, request, &response)
	if err != nil {
		// Check if it's a "not found" error from our mock
		if strings.Contains(err.Error(), "received error response: release not found") {
			return nil, fmt.Errorf("release not found: %s", id)
		}
		return nil, fmt.Errorf("failed to update release: %v", err)
	}

	return &response, nil
}

// GetReleaseWorkItems retrieves work items for a release from the Java service
func (c *TestJavaClientForReleases) GetReleaseWorkItems(ctx context.Context, id string, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
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
	err := c.Request(ctx, http.MethodGet, endpoint+"/"+id+"/workitems"+query, nil, &response)
	if err != nil {
		// Check if it's a "not found" error from our mock
		if strings.Contains(err.Error(), "received error response: release not found") {
			return nil, fmt.Errorf("release not found: %s", id)
		}
		return nil, fmt.Errorf("failed to get release work items: %v", err)
	}

	return &response, nil
}

// ReleaseHandler is our test implementation of the release handler
type ReleaseHandler struct {
	javaClient *TestJavaClientForReleases
}

// ListReleases handles listing releases
func (h *ReleaseHandler) ListReleases(w http.ResponseWriter, r *http.Request) {
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
	response, err := h.javaClient.ListReleases(r.Context(), page, pageSize, status)
	if err != nil {
		http.Error(w, "Failed to list releases: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// CreateRelease handles creating a release
func (h *ReleaseHandler) CreateRelease(w http.ResponseWriter, r *http.Request) {
	// Parse request body
	var request models.ReleaseCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Validate request
	if request.Name == "" {
		http.Error(w, "Name is required", http.StatusBadRequest)
		return
	}
	if request.Version == "" {
		http.Error(w, "Version is required", http.StatusBadRequest)
		return
	}

	// Set default values if needed
	if request.Status == "" {
		request.Status = models.ReleaseStatusPlanned
	}

	// Call the Java service
	release, err := h.javaClient.CreateRelease(r.Context(), request)
	if err != nil {
		http.Error(w, "Failed to create release: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(release)
}

// GetRelease handles getting a release
func (h *ReleaseHandler) GetRelease(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Call the Java service
	release, err := h.javaClient.GetRelease(r.Context(), id)
	if err != nil {
		if err.Error() == "release not found: "+id {
			http.Error(w, "Release not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get release: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(release)
}

// UpdateRelease handles updating a release
func (h *ReleaseHandler) UpdateRelease(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

	// Parse request body
	var request models.ReleaseUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body: "+err.Error(), http.StatusBadRequest)
		return
	}

	// Call the Java service
	release, err := h.javaClient.UpdateRelease(r.Context(), id, request)
	if err != nil {
		if err.Error() == "release not found: "+id {
			http.Error(w, "Release not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to update release: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(release)
}

// GetReleaseWorkItems handles getting work items for a release
func (h *ReleaseHandler) GetReleaseWorkItems(w http.ResponseWriter, r *http.Request) {
	// Extract the ID from the URL
	vars := mux.Vars(r)
	id := vars["id"]

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
	response, err := h.javaClient.GetReleaseWorkItems(r.Context(), id, status, page, pageSize)
	if err != nil {
		if err.Error() == "release not found: "+id {
			http.Error(w, "Release not found", http.StatusNotFound)
			return
		}
		http.Error(w, "Failed to get release work items: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// mockTransport is a custom http.RoundTripper that redirects requests to the mock service
type mockReleaseTransport struct {
	mockURL string
}

// RoundTrip implements http.RoundTripper
func (t *mockReleaseTransport) RoundTrip(req *http.Request) (*http.Response, error) {
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

// TestReleaseAPIEndpoints_Integration tests all release endpoints with the API handlers
func TestReleaseAPIEndpoints_Integration(t *testing.T) {
	// Set up the mock release service
	mockReleaseService := NewMockReleaseService()
	defer mockReleaseService.Close()

	// Extract the base URL without the trailing slash
	mockServiceURL := mockReleaseService.server.URL

	// Create Java client config
	javaConfig := &config.JavaServiceConfig{
		Host:          "localhost",
		Port:          8080, // This is ignored since we're using the test server URL directly
		ConnectTimeout: 5000,
		RequestTimeout: 5000,
		Endpoints: map[string]string{
			"health":   "/health",
			"releases": "/api/releases",
		},
	}

	// Create custom HTTP client to intercept requests and redirect to mock service
	httpClient := &http.Client{
		Transport: &mockReleaseTransport{
			mockURL: mockServiceURL,
		},
	}
	
	// Create a Java client
	javaClient := &TestJavaClientForReleases{
		config:     javaConfig,
		httpClient: httpClient,
	}

	// Create the API router with our own release handler
	router := mux.NewRouter()
	
	// Create a release handler that uses our test client
	releaseHandler := &ReleaseHandler{javaClient: javaClient}
	
	// Register routes manually (mirroring what handlers.RegisterReleaseRoutes would do)
	router.HandleFunc("/releases", releaseHandler.ListReleases).Methods(http.MethodGet)
	router.HandleFunc("/releases", releaseHandler.CreateRelease).Methods(http.MethodPost)
	router.HandleFunc("/releases/{id}", releaseHandler.GetRelease).Methods(http.MethodGet)
	router.HandleFunc("/releases/{id}", releaseHandler.UpdateRelease).Methods(http.MethodPut)
	router.HandleFunc("/releases/{id}/workitems", releaseHandler.GetReleaseWorkItems).Methods(http.MethodGet)

	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()

	// Test cases
	t.Run("List releases", func(t *testing.T) {
		resp, err := http.Get(server.URL + "/releases?status=PLANNED")
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}

		// Parse response
		var response models.ReleaseListResponse
		if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate response
		if len(response.Items) == 0 {
			t.Errorf("Expected at least 1 planned release, got 0")
		} else {
			// Check that statuses match filter
			for _, release := range response.Items {
				if release.Status != models.ReleaseStatusPlanned {
					t.Errorf("Expected all releases to have status PLANNED, got %s", release.Status)
				}
			}
		}
	})

	t.Run("Create release", func(t *testing.T) {
		// Create request body
		releaseRequest := models.ReleaseCreateRequest{
			Name:        "Release 1.2",
			Version:     "1.2.0",
			Description: "Bugfix release",
			Status:      models.ReleaseStatusPlanned,
			StartDate:   "2025-09-01",
			ReleaseDate: "2025-09-15",
			ProjectKey:  "TEST1",
		}

		bodyBytes, err := json.Marshal(releaseRequest)
		if err != nil {
			t.Fatalf("Failed to marshal request: %v", err)
		}

		// Send POST request
		resp, err := http.Post(server.URL+"/releases", "application/json", bytes.NewBuffer(bodyBytes))
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusCreated {
			t.Errorf("Expected status code %d, got %d", http.StatusCreated, resp.StatusCode)
		}

		// Parse response
		var release models.Release
		if err := json.NewDecoder(resp.Body).Decode(&release); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate response
		if release.Name != "Release 1.2" {
			t.Errorf("Expected release name 'Release 1.2', got '%s'", release.Name)
		}
		if release.Version != "1.2.0" {
			t.Errorf("Expected release version '1.2.0', got '%s'", release.Version)
		}
		if release.ProjectKey != "TEST1" {
			t.Errorf("Expected project key TEST1, got %s", release.ProjectKey)
		}

		// Store this release ID for later retrieval test
		t.Logf("Created release with ID: %s", release.ID)
	})

	// Get a release ID for the remaining tests
	var releaseID string
	listResp, err := http.Get(server.URL + "/releases")
	if err == nil {
		defer listResp.Body.Close()
		var response models.ReleaseListResponse
		if err := json.NewDecoder(listResp.Body).Decode(&response); err == nil && len(response.Items) > 0 {
			releaseID = response.Items[0].ID.String()
		}
	}

	if releaseID == "" {
		// If we couldn't get an ID, skip the remaining tests
		t.Log("Skipping remaining tests - couldn't retrieve a release ID")
		return
	}

	t.Run("Get release", func(t *testing.T) {
		resp, err := http.Get(server.URL + "/releases/" + releaseID)
		if err != nil {
			t.Fatalf("Failed to make request: %v", err)
		}
		defer resp.Body.Close()

		// Check response status
		if resp.StatusCode != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, resp.StatusCode)
		}

		// Parse response
		var release models.Release
		if err := json.NewDecoder(resp.Body).Decode(&release); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate response
		if release.ID.String() != releaseID {
			t.Errorf("Expected release ID %s, got %s", releaseID, release.ID.String())
		}
	})

	t.Run("Update release", func(t *testing.T) {
		// Create request body
		updatedName := "Updated Release Name"
		updatedStatus := models.ReleaseStatusInProgress
		updateRequest := models.ReleaseUpdateRequest{
			Name:   &updatedName,
			Status: &updatedStatus,
		}

		bodyBytes, err := json.Marshal(updateRequest)
		if err != nil {
			t.Fatalf("Failed to marshal request: %v", err)
		}

		// Create request
		req, err := http.NewRequest(http.MethodPut, server.URL+"/releases/"+releaseID, bytes.NewBuffer(bodyBytes))
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
		var release models.Release
		if err := json.NewDecoder(resp.Body).Decode(&release); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Validate response
		if release.Name != updatedName {
			t.Errorf("Expected release name '%s', got '%s'", updatedName, release.Name)
		}
		if release.Status != updatedStatus {
			t.Errorf("Expected status %s, got %s", updatedStatus, release.Status)
		}
	})

	// For work items, we'll test a release we know has work items from our mock setup
	var releaseWithWorkItems string
	for id, items := range mockReleaseService.workItems {
		if len(items) > 0 {
			releaseWithWorkItems = id
			break
		}
	}

	if releaseWithWorkItems != "" {
		t.Run("Get release work items", func(t *testing.T) {
			resp, err := http.Get(server.URL + "/releases/" + releaseWithWorkItems + "/workitems")
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
			if len(response.Items) == 0 {
				t.Errorf("Expected at least 1 work item, got 0")
			}
		})

		t.Run("Get release work items with status filter", func(t *testing.T) {
			resp, err := http.Get(server.URL + "/releases/" + releaseWithWorkItems + "/workitems?status=IN_DEV")
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
}

// TestReleaseAPIPerformance_Integration tests the performance of release endpoints
func TestReleaseAPIPerformance_Integration(t *testing.T) {
	// Set up the mock release service
	mockReleaseService := NewMockReleaseService()
	defer mockReleaseService.Close()

	// Extract the base URL without the trailing slash
	mockServiceURL := mockReleaseService.server.URL

	// Create Java client config
	javaConfig := &config.JavaServiceConfig{
		Host:          "localhost",
		Port:          8080, // This is ignored since we're using the test server URL directly
		ConnectTimeout: 5000,
		RequestTimeout: 5000,
		Endpoints: map[string]string{
			"health":   "/health",
			"releases": "/api/releases",
		},
	}

	// Create custom HTTP client to intercept requests and redirect to mock service
	httpClient := &http.Client{
		Transport: &mockReleaseTransport{
			mockURL: mockServiceURL,
		},
	}
	
	// Create a Java client
	javaClient := &TestJavaClientForReleases{
		config:     javaConfig,
		httpClient: httpClient,
	}

	// Create the API router with our own release handler
	router := mux.NewRouter()
	
	// Create a release handler that uses our test client
	releaseHandler := &ReleaseHandler{javaClient: javaClient}
	
	// Register routes manually
	router.HandleFunc("/releases", releaseHandler.ListReleases).Methods(http.MethodGet)
	router.HandleFunc("/releases", releaseHandler.CreateRelease).Methods(http.MethodPost)
	router.HandleFunc("/releases/{id}", releaseHandler.GetRelease).Methods(http.MethodGet)
	router.HandleFunc("/releases/{id}", releaseHandler.UpdateRelease).Methods(http.MethodPut)
	router.HandleFunc("/releases/{id}/workitems", releaseHandler.GetReleaseWorkItems).Methods(http.MethodGet)

	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()

	// Get a release ID for testing
	var releaseID string
	listResp, err := http.Get(server.URL + "/releases")
	if err == nil {
		defer listResp.Body.Close()
		var response models.ReleaseListResponse
		if err := json.NewDecoder(listResp.Body).Decode(&response); err == nil && len(response.Items) > 0 {
			releaseID = response.Items[0].ID.String()
		}
	}

	if releaseID == "" {
		t.Skip("No release ID available for performance testing")
	}

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
				// Test list releases endpoint
				start := time.Now()
				req, err := http.NewRequestWithContext(ctx, "GET", server.URL+"/releases", nil)
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

				// Test get release endpoint
				start = time.Now()
				req, err = http.NewRequestWithContext(ctx, "GET", server.URL+"/releases/"+releaseID, nil)
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
	t.Logf("Release API Performance Results:")
	t.Logf("  Concurrent users: %d", concurrentUsers)
	t.Logf("  Requests per user: %d", requestsPerUser)
	t.Logf("  Total duration: %v", totalDuration)
	t.Logf("  Total requests: %d", totalRequests)
	t.Logf("  Requests per second: %.2f", float64(totalRequests)/totalDuration.Seconds())
	t.Logf("  List releases endpoint:")
	t.Logf("    Average time: %v", avgListTime)
	t.Logf("    Maximum time: %v", maxListTime)
	t.Logf("  Get release endpoint:")
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