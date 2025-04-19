/*
 * Tests for project handlers
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strconv"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/go/src/models"
)

// Define our own client interface for testing
type JavaClientInterface interface {
	ListProjects(ctx context.Context, page, pageSize int, activeOnly bool) (*models.ProjectListResponse, error)
	GetProject(ctx context.Context, key string) (*models.Project, error)
	CreateProject(ctx context.Context, request models.ProjectCreateRequest) (*models.Project, error)
	UpdateProject(ctx context.Context, key string, request models.ProjectUpdateRequest) (*models.Project, error)
	GetProjectWorkItems(ctx context.Context, key string, status string, page, pageSize int) (*models.WorkItemListResponse, error)
}

// MockJavaClientForProjects is a mock implementation of the Java client for testing
type MockJavaClientForProjects struct {
	// Mock responses
	ListProjectsResponse       *models.ProjectListResponse
	GetProjectResponse         *models.Project
	CreateProjectResponse      *models.Project
	UpdateProjectResponse      *models.Project
	GetProjectWorkItemsResponse *models.WorkItemListResponse
	
	// Record of calls
	ListProjectsCalled       bool
	GetProjectCalled         bool
	CreateProjectCalled      bool
	UpdateProjectCalled      bool
	GetProjectWorkItemsCalled bool
	
	// Error responses
	ListProjectsError       error
	GetProjectError         error
	CreateProjectError      error
	UpdateProjectError      error
	GetProjectWorkItemsError error
	
	// Captured parameters for verification
	CapturedProjectKey         string
	CapturedListActiveOnly     bool
	CapturedCreateRequest      models.ProjectCreateRequest
	CapturedUpdateRequest      models.ProjectUpdateRequest
	CapturedStatus             string
}

// ListProjects mocks the client's ListProjects method
func (m *MockJavaClientForProjects) ListProjects(ctx context.Context, page, pageSize int, activeOnly bool) (*models.ProjectListResponse, error) {
	m.ListProjectsCalled = true
	m.CapturedListActiveOnly = activeOnly
	if m.ListProjectsError != nil {
		return nil, m.ListProjectsError
	}
	return m.ListProjectsResponse, nil
}

// GetProject mocks the client's GetProject method
func (m *MockJavaClientForProjects) GetProject(ctx context.Context, key string) (*models.Project, error) {
	m.GetProjectCalled = true
	m.CapturedProjectKey = key
	if m.GetProjectError != nil {
		return nil, m.GetProjectError
	}
	return m.GetProjectResponse, nil
}

// CreateProject mocks the client's CreateProject method
func (m *MockJavaClientForProjects) CreateProject(ctx context.Context, request models.ProjectCreateRequest) (*models.Project, error) {
	m.CreateProjectCalled = true
	m.CapturedCreateRequest = request
	if m.CreateProjectError != nil {
		return nil, m.CreateProjectError
	}
	return m.CreateProjectResponse, nil
}

// UpdateProject mocks the client's UpdateProject method
func (m *MockJavaClientForProjects) UpdateProject(ctx context.Context, key string, request models.ProjectUpdateRequest) (*models.Project, error) {
	m.UpdateProjectCalled = true
	m.CapturedProjectKey = key
	m.CapturedUpdateRequest = request
	if m.UpdateProjectError != nil {
		return nil, m.UpdateProjectError
	}
	return m.UpdateProjectResponse, nil
}

// GetProjectWorkItems mocks the client's GetProjectWorkItems method
func (m *MockJavaClientForProjects) GetProjectWorkItems(ctx context.Context, key string, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	m.GetProjectWorkItemsCalled = true
	m.CapturedProjectKey = key
	m.CapturedStatus = status
	if m.GetProjectWorkItemsError != nil {
		return nil, m.GetProjectWorkItemsError
	}
	return m.GetProjectWorkItemsResponse, nil
}

// Create a test project handler that uses our interface instead of the concrete client type
type testProjectHandler struct {
	javaClient JavaClientInterface
}

// listProjects handles listing projects
func (h *testProjectHandler) ListProjects(w http.ResponseWriter, r *http.Request) {
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

// createProject handles creating a project
func (h *testProjectHandler) CreateProject(w http.ResponseWriter, r *http.Request) {
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

// getProject handles getting a project
func (h *testProjectHandler) GetProject(w http.ResponseWriter, r *http.Request) {
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

// updateProject handles updating a project
func (h *testProjectHandler) UpdateProject(w http.ResponseWriter, r *http.Request) {
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

// getProjectWorkItems handles getting work items for a project
func (h *testProjectHandler) GetProjectWorkItems(w http.ResponseWriter, r *http.Request) {
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

// TestListProjects tests the ListProjects handler - positive case
func TestListProjects(t *testing.T) {
	// Create mock projects
	mockProjects := []models.Project{
		{
			ID:          uuid.New(),
			Key:         "PROJ1",
			Name:        "Project One",
			Description: "First test project",
			Active:      true,
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
		{
			ID:          uuid.New(),
			Key:         "PROJ2",
			Name:        "Project Two",
			Description: "Second test project",
			Active:      true,
			CreatedAt:   time.Now(),
			UpdatedAt:   time.Now(),
		},
	}

	// Create mock client
	mockClient := &MockJavaClientForProjects{
		ListProjectsResponse: &models.ProjectListResponse{
			Items:      mockProjects,
			TotalCount: len(mockProjects),
			Page:       1,
			PageSize:   10,
		},
	}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request
	req, err := http.NewRequest("GET", "/api/v1/projects?page=1&pageSize=10&activeOnly=true", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListProjects(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusOK)
	}

	// Check that the client method was called with correct parameters
	if !mockClient.ListProjectsCalled {
		t.Error("ListProjects method was not called")
	}
	if !mockClient.CapturedListActiveOnly {
		t.Error("activeOnly parameter not correctly parsed")
	}

	// Check the response body
	var response models.ProjectListResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Verify response content
	if len(response.Items) != len(mockProjects) {
		t.Errorf("expected %d projects, got %d", len(mockProjects), len(response.Items))
	}

	// Verify first project data
	if response.Items[0].Key != mockProjects[0].Key {
		t.Errorf("expected project key %s, got %s", mockProjects[0].Key, response.Items[0].Key)
	}
}

// TestListProjects_InvalidPage tests the ListProjects handler with invalid page parameter
func TestListProjects_InvalidPage(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForProjects{}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request with invalid page
	req, err := http.NewRequest("GET", "/api/v1/projects?page=invalid", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListProjects(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.ListProjectsCalled {
		t.Error("ListProjects method was called despite invalid page parameter")
	}
}

// TestListProjects_InvalidPageSize tests the ListProjects handler with invalid pageSize parameter
func TestListProjects_InvalidPageSize(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForProjects{}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request with invalid pageSize (too large)
	req, err := http.NewRequest("GET", "/api/v1/projects?pageSize=101", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListProjects(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.ListProjectsCalled {
		t.Error("ListProjects method was called despite invalid pageSize parameter")
	}
}

// TestListProjects_Error tests the ListProjects handler when an error occurs
func TestListProjects_Error(t *testing.T) {
	// Create mock client with error
	mockClient := &MockJavaClientForProjects{
		ListProjectsError: fmt.Errorf("database connection error"),
	}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request
	req, err := http.NewRequest("GET", "/api/v1/projects", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListProjects(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusInternalServerError {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusInternalServerError)
	}
}

// TestCreateProject tests the CreateProject handler - positive case
func TestCreateProject(t *testing.T) {
	// Create mock project
	mockProject := &models.Project{
		ID:          uuid.New(),
		Key:         "TEST",
		Name:        "Test Project",
		Description: "This is a test project",
		Active:      true,
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}

	// Create mock client
	mockClient := &MockJavaClientForProjects{
		CreateProjectResponse: mockProject,
	}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request body
	requestBody := models.ProjectCreateRequest{
		Key:         "TEST",
		Name:        "Test Project",
		Description: "This is a test project",
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create request
	req, err := http.NewRequest("POST", "/api/v1/projects", bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateProject(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusCreated {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusCreated)
	}

	// Check that the client method was called
	if !mockClient.CreateProjectCalled {
		t.Error("CreateProject method was not called")
	}

	// Check that the captured request matches what we sent
	if mockClient.CapturedCreateRequest.Key != requestBody.Key {
		t.Errorf("expected request key %s, got %s", requestBody.Key, mockClient.CapturedCreateRequest.Key)
	}

	// Check the response body
	var response models.Project
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Verify response content
	if response.Key != mockProject.Key {
		t.Errorf("expected project key %s, got %s", mockProject.Key, response.Key)
	}
}

// TestCreateProject_InvalidBody tests the CreateProject handler with invalid request body
func TestCreateProject_InvalidBody(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForProjects{}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request with invalid JSON
	req, err := http.NewRequest("POST", "/api/v1/projects", bytes.NewBuffer([]byte("invalid json")))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateProject(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.CreateProjectCalled {
		t.Error("CreateProject method was called despite invalid request body")
	}
}

// TestCreateProject_MissingKey tests the CreateProject handler with missing key
func TestCreateProject_MissingKey(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForProjects{}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request body without key
	requestBody := models.ProjectCreateRequest{
		Name:        "Test Project",
		Description: "This is a test project",
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create request
	req, err := http.NewRequest("POST", "/api/v1/projects", bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateProject(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.CreateProjectCalled {
		t.Error("CreateProject method was called despite missing key")
	}
}

// TestCreateProject_MissingName tests the CreateProject handler with missing name
func TestCreateProject_MissingName(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForProjects{}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request body without name
	requestBody := models.ProjectCreateRequest{
		Key:         "TEST",
		Description: "This is a test project",
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create request
	req, err := http.NewRequest("POST", "/api/v1/projects", bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateProject(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.CreateProjectCalled {
		t.Error("CreateProject method was called despite missing name")
	}
}

// TestGetProject tests the GetProject handler - positive case
func TestGetProject(t *testing.T) {
	// Create mock project
	projectKey := "TEST"
	mockProject := &models.Project{
		ID:          uuid.New(),
		Key:         projectKey,
		Name:        "Test Project",
		Description: "This is a test project",
		Active:      true,
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}

	// Create mock client
	mockClient := &MockJavaClientForProjects{
		GetProjectResponse: mockProject,
	}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/projects/{key}", handler.GetProject).Methods("GET")

	// Create request
	req, err := http.NewRequest("GET", "/api/v1/projects/"+projectKey, nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusOK)
	}

	// Check that the client method was called with correct parameter
	if !mockClient.GetProjectCalled {
		t.Error("GetProject method was not called")
	}
	if mockClient.CapturedProjectKey != projectKey {
		t.Errorf("expected project key %s, got %s", projectKey, mockClient.CapturedProjectKey)
	}

	// Check the response body
	var response models.Project
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Verify response content
	if response.Key != projectKey {
		t.Errorf("expected project key %s, got %s", projectKey, response.Key)
	}
}

// TestGetProject_NotFound tests the GetProject handler when the project is not found
func TestGetProject_NotFound(t *testing.T) {
	// Create non-existent project key
	projectKey := "NOTFOUND"

	// Create mock client with error
	mockClient := &MockJavaClientForProjects{
		GetProjectError: fmt.Errorf("project not found: %s", projectKey),
	}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/projects/{key}", handler.GetProject).Methods("GET")

	// Create request
	req, err := http.NewRequest("GET", "/api/v1/projects/"+projectKey, nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusNotFound)
	}

	// Check that the client method was called with correct parameter
	if !mockClient.GetProjectCalled {
		t.Error("GetProject method was not called")
	}
	if mockClient.CapturedProjectKey != projectKey {
		t.Errorf("expected project key %s, got %s", projectKey, mockClient.CapturedProjectKey)
	}
}

// TestUpdateProject tests the UpdateProject handler - positive case
func TestUpdateProject(t *testing.T) {
	// Create project key and mock project
	projectKey := "TEST"
	updatedName := "Updated Project Name"
	
	mockProject := &models.Project{
		ID:          uuid.New(),
		Key:         projectKey,
		Name:        updatedName,
		Description: "This is an updated test project",
		Active:      true,
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}

	// Create mock client
	mockClient := &MockJavaClientForProjects{
		UpdateProjectResponse: mockProject,
	}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request body
	requestBody := models.ProjectUpdateRequest{
		Name: &updatedName,
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/projects/{key}", handler.UpdateProject).Methods("PUT")

	// Create request
	req, err := http.NewRequest("PUT", "/api/v1/projects/"+projectKey, bytes.NewBuffer(bodyBytes))
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
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusOK)
	}

	// Check that the client method was called with correct parameters
	if !mockClient.UpdateProjectCalled {
		t.Error("UpdateProject method was not called")
	}
	if mockClient.CapturedProjectKey != projectKey {
		t.Errorf("expected project key %s, got %s", projectKey, mockClient.CapturedProjectKey)
	}
	if *mockClient.CapturedUpdateRequest.Name != updatedName {
		t.Errorf("expected updated name %s, got %s", updatedName, *mockClient.CapturedUpdateRequest.Name)
	}

	// Check the response body
	var response models.Project
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Verify response content
	if response.Name != updatedName {
		t.Errorf("expected project name %s, got %s", updatedName, response.Name)
	}
}

// TestUpdateProject_InvalidBody tests the UpdateProject handler with invalid request body
func TestUpdateProject_InvalidBody(t *testing.T) {
	// Create project key
	projectKey := "TEST"

	// Create mock client
	mockClient := &MockJavaClientForProjects{}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/projects/{key}", handler.UpdateProject).Methods("PUT")

	// Create request with invalid JSON
	req, err := http.NewRequest("PUT", "/api/v1/projects/"+projectKey, bytes.NewBuffer([]byte("invalid json")))
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
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.UpdateProjectCalled {
		t.Error("UpdateProject method was called despite invalid request body")
	}
}

// TestUpdateProject_NotFound tests the UpdateProject handler when the project is not found
func TestUpdateProject_NotFound(t *testing.T) {
	// Create non-existent project key and update data
	projectKey := "NOTFOUND"
	updatedName := "Updated Name"

	// Create mock client with error
	mockClient := &MockJavaClientForProjects{
		UpdateProjectError: fmt.Errorf("project not found: %s", projectKey),
	}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create request body
	requestBody := models.ProjectUpdateRequest{
		Name: &updatedName,
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/projects/{key}", handler.UpdateProject).Methods("PUT")

	// Create request
	req, err := http.NewRequest("PUT", "/api/v1/projects/"+projectKey, bytes.NewBuffer(bodyBytes))
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
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusNotFound)
	}

	// Check that the client method was called
	if !mockClient.UpdateProjectCalled {
		t.Error("UpdateProject method was not called")
	}
}

// TestGetProjectWorkItems tests the GetProjectWorkItems handler - positive case
func TestGetProjectWorkItems(t *testing.T) {
	// Create project key
	projectKey := "TEST"

	// Create mock work items
	mockWorkItems := []models.WorkItem{
		{
			ID:          uuid.New(),
			Title:       "Work Item 1",
			Description: "This is work item 1",
			Type:        models.WorkItemTypeFeature,
			Priority:    models.PriorityMedium,
			Status:      models.WorkflowStateInDev,
			ProjectID:   projectKey,
		},
		{
			ID:          uuid.New(),
			Title:       "Work Item 2",
			Description: "This is work item 2",
			Type:        models.WorkItemTypeBug,
			Priority:    models.PriorityHigh,
			Status:      models.WorkflowStateTesting,
			ProjectID:   projectKey,
		},
	}

	// Create mock client
	mockClient := &MockJavaClientForProjects{
		GetProjectWorkItemsResponse: &models.WorkItemListResponse{
			Items:      mockWorkItems,
			TotalCount: len(mockWorkItems),
			Page:       1,
			PageSize:   10,
		},
	}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/projects/{key}/workitems", handler.GetProjectWorkItems).Methods("GET")

	// Create request
	req, err := http.NewRequest("GET", "/api/v1/projects/"+projectKey+"/workitems?status=IN_DEV", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusOK)
	}

	// Check that the client method was called with correct parameters
	if !mockClient.GetProjectWorkItemsCalled {
		t.Error("GetProjectWorkItems method was not called")
	}
	if mockClient.CapturedProjectKey != projectKey {
		t.Errorf("expected project key %s, got %s", projectKey, mockClient.CapturedProjectKey)
	}
	if mockClient.CapturedStatus != "IN_DEV" {
		t.Errorf("expected status IN_DEV, got %s", mockClient.CapturedStatus)
	}

	// Check the response body
	var response models.WorkItemListResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Verify response content
	if len(response.Items) != len(mockWorkItems) {
		t.Errorf("expected %d work items, got %d", len(mockWorkItems), len(response.Items))
	}
}

// TestGetProjectWorkItems_ProjectNotFound tests the GetProjectWorkItems handler when the project is not found
func TestGetProjectWorkItems_ProjectNotFound(t *testing.T) {
	// Create non-existent project key
	projectKey := "NOTFOUND"

	// Create mock client with error
	mockClient := &MockJavaClientForProjects{
		GetProjectWorkItemsError: fmt.Errorf("project not found: %s", projectKey),
	}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/projects/{key}/workitems", handler.GetProjectWorkItems).Methods("GET")

	// Create request
	req, err := http.NewRequest("GET", "/api/v1/projects/"+projectKey+"/workitems", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusNotFound {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusNotFound)
	}

	// Check that the client method was called
	if !mockClient.GetProjectWorkItemsCalled {
		t.Error("GetProjectWorkItems method was not called")
	}
}

// TestGetProjectWorkItems_InvalidPage tests the GetProjectWorkItems handler with invalid page parameter
func TestGetProjectWorkItems_InvalidPage(t *testing.T) {
	// Create project key
	projectKey := "TEST"

	// Create mock client
	mockClient := &MockJavaClientForProjects{}

	// Create handler
	handler := &testProjectHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/projects/{key}/workitems", handler.GetProjectWorkItems).Methods("GET")

	// Create request with invalid page
	req, err := http.NewRequest("GET", "/api/v1/projects/"+projectKey+"/workitems?page=invalid", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler via the router to set route params
	router.ServeHTTP(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.GetProjectWorkItemsCalled {
		t.Error("GetProjectWorkItems method was called despite invalid page parameter")
	}
}