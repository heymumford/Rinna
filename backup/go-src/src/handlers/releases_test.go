/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */

package handlers

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/models"
)

// MockJavaClientForReleases is a mock implementation of the Java client for testing release handlers
type MockJavaClientForReleases struct {
	// Mock responses
	ListReleasesResponse      *models.ReleaseListResponse
	GetReleaseResponse        *models.Release
	CreateReleaseResponse     *models.Release
	UpdateReleaseResponse     *models.Release
	GetReleaseWorkItemsResponse *models.WorkItemListResponse
	
	// Record of calls
	ListReleasesCalled        bool
	GetReleaseCalled          bool
	CreateReleaseCalled       bool
	UpdateReleaseCalled       bool
	GetReleaseWorkItemsCalled bool
	
	// Error responses
	ListReleasesError        error
	GetReleaseError          error
	CreateReleaseError       error
	UpdateReleaseError       error
	GetReleaseWorkItemsError error
	
	// Captured parameters for verification
	CapturedReleaseID        string
	CapturedStatus           string
	CapturedCreateRequest    models.ReleaseCreateRequest
	CapturedUpdateRequest    models.ReleaseUpdateRequest
}

// ListReleases mocks the client's ListReleases method
func (m *MockJavaClientForReleases) ListReleases(ctx context.Context, page, pageSize int, status string) (*models.ReleaseListResponse, error) {
	m.ListReleasesCalled = true
	m.CapturedStatus = status
	if m.ListReleasesError != nil {
		return nil, m.ListReleasesError
	}
	return m.ListReleasesResponse, nil
}

// GetRelease mocks the client's GetRelease method
func (m *MockJavaClientForReleases) GetRelease(ctx context.Context, id string) (*models.Release, error) {
	m.GetReleaseCalled = true
	m.CapturedReleaseID = id
	if m.GetReleaseError != nil {
		return nil, m.GetReleaseError
	}
	return m.GetReleaseResponse, nil
}

// CreateRelease mocks the client's CreateRelease method
func (m *MockJavaClientForReleases) CreateRelease(ctx context.Context, request models.ReleaseCreateRequest) (*models.Release, error) {
	m.CreateReleaseCalled = true
	m.CapturedCreateRequest = request
	if m.CreateReleaseError != nil {
		return nil, m.CreateReleaseError
	}
	return m.CreateReleaseResponse, nil
}

// UpdateRelease mocks the client's UpdateRelease method
func (m *MockJavaClientForReleases) UpdateRelease(ctx context.Context, id string, request models.ReleaseUpdateRequest) (*models.Release, error) {
	m.UpdateReleaseCalled = true
	m.CapturedReleaseID = id
	m.CapturedUpdateRequest = request
	if m.UpdateReleaseError != nil {
		return nil, m.UpdateReleaseError
	}
	return m.UpdateReleaseResponse, nil
}

// GetReleaseWorkItems mocks the client's GetReleaseWorkItems method
func (m *MockJavaClientForReleases) GetReleaseWorkItems(ctx context.Context, id string, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	m.GetReleaseWorkItemsCalled = true
	m.CapturedReleaseID = id
	m.CapturedStatus = status
	if m.GetReleaseWorkItemsError != nil {
		return nil, m.GetReleaseWorkItemsError
	}
	return m.GetReleaseWorkItemsResponse, nil
}

// Create a custom test handler structure to use our mocks
type testReleaseHandler struct {
	javaClient *MockJavaClientForReleases
}

// ListReleases handles listing releases
func (h *testReleaseHandler) ListReleases(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters
	status := r.URL.Query().Get("status")
	pageStr := r.URL.Query().Get("page")
	pageSizeStr := r.URL.Query().Get("pageSize")

	// Set default values
	page := 1
	pageSize := 10

	// Parse page parameter
	if pageStr != "" {
		parsedPage, err := parseInt(pageStr)
		if err != nil || parsedPage < 1 {
			http.Error(w, "Invalid page parameter", http.StatusBadRequest)
			return
		}
		page = parsedPage
	}

	// Parse pageSize parameter
	if pageSizeStr != "" {
		parsedPageSize, err := parseInt(pageSizeStr)
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
func (h *testReleaseHandler) CreateRelease(w http.ResponseWriter, r *http.Request) {
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
func (h *testReleaseHandler) GetRelease(w http.ResponseWriter, r *http.Request) {
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
func (h *testReleaseHandler) UpdateRelease(w http.ResponseWriter, r *http.Request) {
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
func (h *testReleaseHandler) GetReleaseWorkItems(w http.ResponseWriter, r *http.Request) {
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
		parsedPage, err := parseInt(pageStr)
		if err != nil || parsedPage < 1 {
			http.Error(w, "Invalid page parameter", http.StatusBadRequest)
			return
		}
		page = parsedPage
	}

	// Parse pageSize parameter
	if pageSizeStr != "" {
		parsedPageSize, err := parseInt(pageSizeStr)
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

// TestListReleases tests the ListReleases handler
func TestListReleases(t *testing.T) {
	// Create mock releases
	mockReleases := []models.Release{
		{
			ID:          uuid.New(),
			Name:        "Release 1.0",
			Version:     "1.0.0",
			Description: "First major release",
			Status:      models.ReleaseStatusPlanned,
			StartDate:   "2025-06-01",
			ReleaseDate: "2025-07-01",
			ProjectKey:  "PROJ1",
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
		{
			ID:          uuid.New(),
			Name:        "Release 1.1",
			Version:     "1.1.0",
			Description: "Minor update",
			Status:      models.ReleaseStatusInProgress,
			StartDate:   "2025-07-15",
			ReleaseDate: "2025-08-15",
			ProjectKey:  "PROJ1",
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
	}

	// Create mock client
	mockClient := &MockJavaClientForReleases{
		ListReleasesResponse: &models.ReleaseListResponse{
			Items:      mockReleases,
			TotalCount: len(mockReleases),
			Page:       1,
			PageSize:   10,
		},
	}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create request with status filter
	req, err := http.NewRequest("GET", "/releases?status=PLANNED", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListReleases(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	// Check that the client method was called with correct parameters
	if !mockClient.ListReleasesCalled {
		t.Error("ListReleases method was not called")
	}
	if mockClient.CapturedStatus != "PLANNED" {
		t.Errorf("Status parameter not correctly passed: got %v want %v", mockClient.CapturedStatus, "PLANNED")
	}

	// Check the response body
	var response models.ReleaseListResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Verify response content
	if len(response.Items) != len(mockReleases) {
		t.Errorf("expected %d releases, got %d", len(mockReleases), len(response.Items))
	}
}

// TestListReleases_InvalidPage tests the ListReleases handler with invalid page parameter
func TestListReleases_InvalidPage(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForReleases{}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create request with invalid page
	req, err := http.NewRequest("GET", "/releases?page=invalid", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListReleases(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.ListReleasesCalled {
		t.Error("ListReleases method was called despite invalid page parameter")
	}
}

// TestListReleases_InvalidPageSize tests the ListReleases handler with invalid pageSize parameter
func TestListReleases_InvalidPageSize(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForReleases{}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create request with invalid pageSize (too large)
	req, err := http.NewRequest("GET", "/releases?pageSize=101", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListReleases(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.ListReleasesCalled {
		t.Error("ListReleases method was called despite invalid pageSize parameter")
	}
}

// TestListReleases_Error tests the ListReleases handler when an error occurs
func TestListReleases_Error(t *testing.T) {
	// Create mock client with error
	mockClient := &MockJavaClientForReleases{
		ListReleasesError: fmt.Errorf("database connection error"),
	}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create request
	req, err := http.NewRequest("GET", "/releases", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListReleases(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusInternalServerError {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusInternalServerError)
	}
}

// TestCreateRelease tests the CreateRelease handler with valid input
func TestCreateRelease(t *testing.T) {
	// Create a mock release
	releaseID := uuid.New()
	mockRelease := &models.Release{
		ID:          releaseID,
		Name:        "Release 1.2",
		Version:     "1.2.0",
		Description: "Bug fix release",
		Status:      models.ReleaseStatusPlanned,
		StartDate:   "2025-09-01",
		ReleaseDate: "2025-09-15",
		ProjectKey:  "PROJ1",
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}

	// Create mock client
	mockClient := &MockJavaClientForReleases{
		CreateReleaseResponse: mockRelease,
	}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create request body
	requestBody := models.ReleaseCreateRequest{
		Name:        "Release 1.2",
		Version:     "1.2.0",
		Description: "Bug fix release",
		Status:      models.ReleaseStatusPlanned,
		StartDate:   "2025-09-01",
		ReleaseDate: "2025-09-15",
		ProjectKey:  "PROJ1",
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create request
	req, err := http.NewRequest("POST", "/releases", bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateRelease(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusCreated {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusCreated)
	}

	// Check that the client method was called
	if !mockClient.CreateReleaseCalled {
		t.Error("CreateRelease method was not called")
	}

	// Check that the captured request matches what we sent
	if mockClient.CapturedCreateRequest.Name != requestBody.Name {
		t.Errorf("Expected release name %s, got %s", requestBody.Name, mockClient.CapturedCreateRequest.Name)
	}
	if mockClient.CapturedCreateRequest.Version != requestBody.Version {
		t.Errorf("Expected release version %s, got %s", requestBody.Version, mockClient.CapturedCreateRequest.Version)
	}

	// Parse the response
	var response models.Release
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("Failed to parse response body: %v", err)
	}

	// Verify the response
	if response.ID != releaseID {
		t.Errorf("Expected release ID %s, got %s", releaseID, response.ID)
	}
	if response.Name != mockRelease.Name {
		t.Errorf("Expected release name %s, got %s", mockRelease.Name, response.Name)
	}
}

// TestCreateRelease_InvalidBody tests the CreateRelease handler with invalid request body
func TestCreateRelease_InvalidBody(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForReleases{}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create request with invalid JSON
	req, err := http.NewRequest("POST", "/releases", bytes.NewBuffer([]byte("invalid json")))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateRelease(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.CreateReleaseCalled {
		t.Error("CreateRelease method was called despite invalid request body")
	}
}

// TestCreateRelease_MissingName tests the CreateRelease handler with missing name
func TestCreateRelease_MissingName(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForReleases{}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create request body without name
	requestBody := models.ReleaseCreateRequest{
		Version:     "1.2.0",
		Description: "Bug fix release",
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create request
	req, err := http.NewRequest("POST", "/releases", bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateRelease(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.CreateReleaseCalled {
		t.Error("CreateRelease method was called despite missing name")
	}
}

// TestCreateRelease_MissingVersion tests the CreateRelease handler with missing version
func TestCreateRelease_MissingVersion(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForReleases{}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create request body without version
	requestBody := models.ReleaseCreateRequest{
		Name:        "Release 1.2",
		Description: "Bug fix release",
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create request
	req, err := http.NewRequest("POST", "/releases", bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateRelease(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.CreateReleaseCalled {
		t.Error("CreateRelease method was called despite missing version")
	}
}

// TestGetRelease tests the GetRelease handler
func TestGetRelease(t *testing.T) {
	// Create a mock release
	releaseID := uuid.New()
	mockRelease := &models.Release{
		ID:          releaseID,
		Name:        "Release 1.0",
		Version:     "1.0.0",
		Description: "First major release",
		Status:      models.ReleaseStatusReleased,
		StartDate:   "2025-06-01",
		ReleaseDate: "2025-07-01",
		ProjectKey:  "PROJ1",
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}

	// Create mock client
	mockClient := &MockJavaClientForReleases{
		GetReleaseResponse: mockRelease,
	}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/releases/{id}", handler.GetRelease).Methods("GET")

	// Create request
	req, err := http.NewRequest("GET", "/releases/"+releaseID.String(), nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	// Check that the client method was called with correct ID
	if !mockClient.GetReleaseCalled {
		t.Error("GetRelease method was not called")
	}
	if mockClient.CapturedReleaseID != releaseID.String() {
		t.Errorf("Expected release ID %s, got %s", releaseID.String(), mockClient.CapturedReleaseID)
	}

	// Parse the response
	var response models.Release
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("Failed to parse response body: %v", err)
	}

	// Verify the response
	if response.ID != releaseID {
		t.Errorf("Expected release ID %s, got %s", releaseID, response.ID)
	}
	if response.Name != mockRelease.Name {
		t.Errorf("Expected release name %s, got %s", mockRelease.Name, response.Name)
	}
}

// TestGetRelease_NotFound tests the GetRelease handler when the release is not found
func TestGetRelease_NotFound(t *testing.T) {
	// Create a non-existent release ID
	releaseID := uuid.New().String()

	// Create mock client with error
	mockClient := &MockJavaClientForReleases{
		GetReleaseError: fmt.Errorf("release not found: %s", releaseID),
	}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/releases/{id}", handler.GetRelease).Methods("GET")

	// Create request
	req, err := http.NewRequest("GET", "/releases/"+releaseID, nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusNotFound)
	}

	// Check that the client method was called with correct ID
	if !mockClient.GetReleaseCalled {
		t.Error("GetRelease method was not called")
	}
	if mockClient.CapturedReleaseID != releaseID {
		t.Errorf("Expected release ID %s, got %s", releaseID, mockClient.CapturedReleaseID)
	}
}

// TestUpdateRelease tests the UpdateRelease handler
func TestUpdateRelease(t *testing.T) {
	// Create a mock release ID
	releaseID := uuid.New()
	updatedName := "Updated Release Name"
	updatedStatus := models.ReleaseStatusInProgress

	// Create mock updated release
	mockRelease := &models.Release{
		ID:          releaseID,
		Name:        updatedName,
		Version:     "1.0.0",
		Description: "First major release",
		Status:      updatedStatus,
		StartDate:   "2025-06-01",
		ReleaseDate: "2025-07-01",
		ProjectKey:  "PROJ1",
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}

	// Create mock client
	mockClient := &MockJavaClientForReleases{
		UpdateReleaseResponse: mockRelease,
	}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create update request
	updateRequest := models.ReleaseUpdateRequest{
		Name:   &updatedName,
		Status: &updatedStatus,
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(updateRequest)
	if err != nil {
		t.Fatal(err)
	}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/releases/{id}", handler.UpdateRelease).Methods("PUT")

	// Create request
	req, err := http.NewRequest("PUT", "/releases/"+releaseID.String(), bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	// Check that the client method was called with correct parameters
	if !mockClient.UpdateReleaseCalled {
		t.Error("UpdateRelease method was not called")
	}
	if mockClient.CapturedReleaseID != releaseID.String() {
		t.Errorf("Expected release ID %s, got %s", releaseID.String(), mockClient.CapturedReleaseID)
	}
	if *mockClient.CapturedUpdateRequest.Name != updatedName {
		t.Errorf("Expected updated name %s, got %s", updatedName, *mockClient.CapturedUpdateRequest.Name)
	}
	if *mockClient.CapturedUpdateRequest.Status != updatedStatus {
		t.Errorf("Expected updated status %s, got %s", updatedStatus, *mockClient.CapturedUpdateRequest.Status)
	}

	// Parse the response
	var response models.Release
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("Failed to parse response body: %v", err)
	}

	// Verify the response
	if response.Name != updatedName {
		t.Errorf("Expected release name %s, got %s", updatedName, response.Name)
	}
	if response.Status != updatedStatus {
		t.Errorf("Expected release status %s, got %s", updatedStatus, response.Status)
	}
}

// TestUpdateRelease_InvalidBody tests the UpdateRelease handler with invalid request body
func TestUpdateRelease_InvalidBody(t *testing.T) {
	// Create a release ID
	releaseID := uuid.New().String()

	// Create mock client
	mockClient := &MockJavaClientForReleases{}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/releases/{id}", handler.UpdateRelease).Methods("PUT")

	// Create request with invalid JSON
	req, err := http.NewRequest("PUT", "/releases/"+releaseID, bytes.NewBuffer([]byte("invalid json")))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.UpdateReleaseCalled {
		t.Error("UpdateRelease method was called despite invalid request body")
	}
}

// TestUpdateRelease_NotFound tests the UpdateRelease handler when the release is not found
func TestUpdateRelease_NotFound(t *testing.T) {
	// Create a non-existent release ID
	releaseID := uuid.New().String()
	updatedName := "Updated Release Name"

	// Create mock client with error
	mockClient := &MockJavaClientForReleases{
		UpdateReleaseError: fmt.Errorf("release not found: %s", releaseID),
	}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create update request
	updateRequest := models.ReleaseUpdateRequest{
		Name: &updatedName,
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(updateRequest)
	if err != nil {
		t.Fatal(err)
	}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/releases/{id}", handler.UpdateRelease).Methods("PUT")

	// Create request
	req, err := http.NewRequest("PUT", "/releases/"+releaseID, bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusNotFound)
	}

	// Check that the client method was called with correct parameters
	if !mockClient.UpdateReleaseCalled {
		t.Error("UpdateRelease method was not called")
	}
	if mockClient.CapturedReleaseID != releaseID {
		t.Errorf("Expected release ID %s, got %s", releaseID, mockClient.CapturedReleaseID)
	}
}

// TestGetReleaseWorkItems tests the GetReleaseWorkItems handler
func TestGetReleaseWorkItems(t *testing.T) {
	// Create a mock release ID
	releaseID := uuid.New()

	// Create mock work items
	mockWorkItems := []models.WorkItem{
		{
			ID:          uuid.New(),
			Title:       "Work Item 1",
			Description: "This is work item 1",
			Type:        models.WorkItemTypeFeature,
			Priority:    models.PriorityMedium,
			Status:      models.WorkflowStateInDev,
			Metadata:    map[string]string{"releaseId": releaseID.String()},
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
		{
			ID:          uuid.New(),
			Title:       "Work Item 2",
			Description: "This is work item 2",
			Type:        models.WorkItemTypeBug,
			Priority:    models.PriorityHigh,
			Status:      models.WorkflowStateTesting,
			Metadata:    map[string]string{"releaseId": releaseID.String()},
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
	}

	// Create mock client
	mockClient := &MockJavaClientForReleases{
		GetReleaseWorkItemsResponse: &models.WorkItemListResponse{
			Items:      mockWorkItems,
			TotalCount: len(mockWorkItems),
			Page:       1,
			PageSize:   10,
		},
	}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/releases/{id}/workitems", handler.GetReleaseWorkItems).Methods("GET")

	// Create request with status filter
	req, err := http.NewRequest("GET", "/releases/"+releaseID.String()+"/workitems?status=IN_DEV", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
	}

	// Check that the client method was called with correct parameters
	if !mockClient.GetReleaseWorkItemsCalled {
		t.Error("GetReleaseWorkItems method was not called")
	}
	if mockClient.CapturedReleaseID != releaseID.String() {
		t.Errorf("Expected release ID %s, got %s", releaseID.String(), mockClient.CapturedReleaseID)
	}
	if mockClient.CapturedStatus != "IN_DEV" {
		t.Errorf("Expected status IN_DEV, got %s", mockClient.CapturedStatus)
	}

	// Parse the response
	var response models.WorkItemListResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("Failed to parse response body: %v", err)
	}

	// Verify the response
	if len(response.Items) != len(mockWorkItems) {
		t.Errorf("Expected %d work items, got %d", len(mockWorkItems), len(response.Items))
	}
}

// TestGetReleaseWorkItems_ReleaseNotFound tests the GetReleaseWorkItems handler when the release is not found
func TestGetReleaseWorkItems_ReleaseNotFound(t *testing.T) {
	// Create a non-existent release ID
	releaseID := uuid.New().String()

	// Create mock client with error
	mockClient := &MockJavaClientForReleases{
		GetReleaseWorkItemsError: fmt.Errorf("release not found: %s", releaseID),
	}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/releases/{id}/workitems", handler.GetReleaseWorkItems).Methods("GET")

	// Create request
	req, err := http.NewRequest("GET", "/releases/"+releaseID+"/workitems", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusNotFound)
	}

	// Check that the client method was called with correct parameters
	if !mockClient.GetReleaseWorkItemsCalled {
		t.Error("GetReleaseWorkItems method was not called")
	}
	if mockClient.CapturedReleaseID != releaseID {
		t.Errorf("Expected release ID %s, got %s", releaseID, mockClient.CapturedReleaseID)
	}
}

// TestGetReleaseWorkItems_InvalidPage tests the GetReleaseWorkItems handler with invalid page parameter
func TestGetReleaseWorkItems_InvalidPage(t *testing.T) {
	// Create a release ID
	releaseID := uuid.New().String()

	// Create mock client
	mockClient := &MockJavaClientForReleases{}

	// Create handler
	handler := &testReleaseHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/releases/{id}/workitems", handler.GetReleaseWorkItems).Methods("GET")

	// Create request with invalid page
	req, err := http.NewRequest("GET", "/releases/"+releaseID+"/workitems?page=invalid", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.GetReleaseWorkItemsCalled {
		t.Error("GetReleaseWorkItems method was called despite invalid page parameter")
	}
}

// Helper function to parse int with error handling
func parseInt(s string) (int, error) {
	var value int
	_, err := fmt.Sscanf(s, "%d", &value)
	return value, err
}