/*
 * Tests for work item handlers
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

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/go/src/models"
)

// Define a client interface for the handlers
type WorkItemClientInterface interface {
	// Work Item Operations
	ListWorkItems(ctx context.Context, status string, page, pageSize int) (*models.WorkItemListResponse, error)
	GetWorkItem(ctx context.Context, id string) (*models.WorkItem, error)
	CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error)
	UpdateWorkItem(ctx context.Context, id string, request models.WorkItemUpdateRequest) (*models.WorkItem, error)
	TransitionWorkItem(ctx context.Context, id string, request models.WorkItemTransitionRequest) (*models.WorkItem, error)
	
	// The JavaClient also implements these methods for other resources
	// ListProjects(ctx context.Context, page, pageSize int, activeOnly bool) (*models.ProjectListResponse, error)
	// GetProject(ctx context.Context, key string) (*models.Project, error)
	// CreateProject(ctx context.Context, request models.ProjectCreateRequest) (*models.Project, error)
	// UpdateProject(ctx context.Context, key string, request models.ProjectUpdateRequest) (*models.Project, error)
	// GetProjectWorkItems(ctx context.Context, key string, status string, page, pageSize int) (*models.WorkItemListResponse, error)
	// ListReleases(ctx context.Context, page, pageSize int, status string) (*models.ReleaseListResponse, error)
	// GetRelease(ctx context.Context, id string) (*models.Release, error)
	// CreateRelease(ctx context.Context, request models.ReleaseCreateRequest) (*models.Release, error)
	// UpdateRelease(ctx context.Context, id string, request models.ReleaseUpdateRequest) (*models.Release, error)
	// GetReleaseWorkItems(ctx context.Context, id string, status string, page, pageSize int) (*models.WorkItemListResponse, error)
}

// MockJavaClient is a mock implementation of the Java client for testing
type MockJavaClient struct {
	// Mock responses
	ListWorkItemsResponse  *models.WorkItemListResponse
	GetWorkItemResponse    *models.WorkItem
	CreateWorkItemResponse *models.WorkItem
	UpdateWorkItemResponse *models.WorkItem
	TransitionResponse     *models.WorkItem
	
	// Record of calls
	ListWorkItemsCalled    bool
	GetWorkItemCalled      bool
	CreateWorkItemCalled   bool
	UpdateWorkItemCalled   bool
	TransitionCalled       bool
	
	// Error responses
	ListWorkItemsError  error
	GetWorkItemError    error
	CreateWorkItemError error
	UpdateWorkItemError error
	TransitionError     error
}

// ListWorkItems mocks the client's ListWorkItems method
func (m *MockJavaClient) ListWorkItems(ctx context.Context, status string, page, pageSize int) (*models.WorkItemListResponse, error) {
	m.ListWorkItemsCalled = true
	if m.ListWorkItemsError != nil {
		return nil, m.ListWorkItemsError
	}
	return m.ListWorkItemsResponse, nil
}

// GetWorkItem mocks the client's GetWorkItem method
func (m *MockJavaClient) GetWorkItem(ctx context.Context, id string) (*models.WorkItem, error) {
	m.GetWorkItemCalled = true
	if m.GetWorkItemError != nil {
		return nil, m.GetWorkItemError
	}
	return m.GetWorkItemResponse, nil
}

// CreateWorkItem mocks the client's CreateWorkItem method
func (m *MockJavaClient) CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error) {
	m.CreateWorkItemCalled = true
	if m.CreateWorkItemError != nil {
		return nil, m.CreateWorkItemError
	}
	return m.CreateWorkItemResponse, nil
}

// UpdateWorkItem mocks the client's UpdateWorkItem method
func (m *MockJavaClient) UpdateWorkItem(ctx context.Context, id string, request models.WorkItemUpdateRequest) (*models.WorkItem, error) {
	m.UpdateWorkItemCalled = true
	if m.UpdateWorkItemError != nil {
		return nil, m.UpdateWorkItemError
	}
	return m.UpdateWorkItemResponse, nil
}

// TransitionWorkItem mocks the client's TransitionWorkItem method
func (m *MockJavaClient) TransitionWorkItem(ctx context.Context, id string, request models.WorkItemTransitionRequest) (*models.WorkItem, error) {
	m.TransitionCalled = true
	if m.TransitionError != nil {
		return nil, m.TransitionError
	}
	return m.TransitionResponse, nil
}

// Create a test work item handler that uses our interface
type testWorkItemHandler struct {
	javaClient WorkItemClientInterface
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

// TestListWorkItems tests the ListWorkItems handler
func TestListWorkItems(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClient{
		ListWorkItemsResponse: &models.WorkItemListResponse{
			Items: []models.WorkItem{
				{
					ID:          uuid.New(),
					Title:       "Test Work Item 1",
					Description: "This is a test work item",
					Type:        models.WorkItemTypeFeature,
					Priority:    models.PriorityMedium,
					Status:      models.WorkflowStateInDev,
				},
				{
					ID:          uuid.New(),
					Title:       "Test Work Item 2",
					Description: "This is another test work item",
					Type:        models.WorkItemTypeBug,
					Priority:    models.PriorityHigh,
					Status:      models.WorkflowStateTesting,
				},
			},
			TotalCount: 2,
			Page:       1,
			PageSize:   10,
		},
	}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create request
	req, err := http.NewRequest("GET", "/api/v1/workitems?status=IN_DEV&page=1&pageSize=10", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListWorkItems(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusOK)
	}

	// Check that the client method was called
	if !mockClient.ListWorkItemsCalled {
		t.Error("ListWorkItems method was not called")
	}

	// Check the response body
	var response models.WorkItemListResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Basic validation of the response
	if len(response.Items) != 2 {
		t.Errorf("handler returned wrong number of items: got %d want %d",
			len(response.Items), 2)
	}
}

// TestListWorkItems_Error tests the ListWorkItems handler when an error occurs
func TestListWorkItems_Error(t *testing.T) {
	// Create mock client with error
	mockClient := &MockJavaClient{
		ListWorkItemsError: fmt.Errorf("failed to connect to Java service"),
	}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create request
	req, err := http.NewRequest("GET", "/api/v1/workitems", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListWorkItems(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusInternalServerError {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusInternalServerError)
	}
}

// TestListWorkItems_InvalidPageParam tests the ListWorkItems handler with invalid page parameter
func TestListWorkItems_InvalidPageParam(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClient{}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create request with invalid page parameter
	req, err := http.NewRequest("GET", "/api/v1/workitems?page=invalid", nil)
	if err != nil {
		t.Fatal(err)
	}

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.ListWorkItems(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.ListWorkItemsCalled {
		t.Error("ListWorkItems method was called despite invalid parameters")
	}
}

// TestGetWorkItem tests the GetWorkItem handler
func TestGetWorkItem(t *testing.T) {
	// Create mock work item
	mockWorkItem := &models.WorkItem{
		ID:          uuid.New(),
		Title:       "Test Work Item",
		Description: "This is a test work item",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
		Status:      models.WorkflowStateInDev,
	}

	// Create mock client
	mockClient := &MockJavaClient{
		GetWorkItemResponse: mockWorkItem,
	}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/workitems/{id}", handler.GetWorkItem).Methods("GET")

	// Create request
	req, err := http.NewRequest("GET", fmt.Sprintf("/api/v1/workitems/%s", mockWorkItem.ID), nil)
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

	// Check that the client method was called
	if !mockClient.GetWorkItemCalled {
		t.Error("GetWorkItem method was not called")
	}

	// Check the response body
	var response models.WorkItem
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Basic validation of the response
	if response.ID != mockWorkItem.ID {
		t.Errorf("handler returned wrong work item ID: got %s want %s",
			response.ID, mockWorkItem.ID)
	}
}

// TestGetWorkItem_NotFound tests the GetWorkItem handler when the item is not found
func TestGetWorkItem_NotFound(t *testing.T) {
	// Create ID for test
	id := uuid.New().String()

	// Create mock client with "not found" error
	mockClient := &MockJavaClient{
		GetWorkItemError: fmt.Errorf("work item not found: " + id),
	}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/workitems/{id}", handler.GetWorkItem).Methods("GET")

	// Create request
	req, err := http.NewRequest("GET", fmt.Sprintf("/api/v1/workitems/%s", id), nil)
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
}

// TestCreateWorkItem tests the CreateWorkItem handler
func TestCreateWorkItem(t *testing.T) {
	// Create mock work item
	mockWorkItem := &models.WorkItem{
		ID:          uuid.New(),
		Title:       "New Work Item",
		Description: "This is a new work item",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
		Status:      models.WorkflowStateFound,
	}

	// Create mock client
	mockClient := &MockJavaClient{
		CreateWorkItemResponse: mockWorkItem,
	}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create request body
	requestBody := models.WorkItemCreateRequest{
		Title:       "New Work Item",
		Description: "This is a new work item",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create request
	req, err := http.NewRequest("POST", "/api/v1/workitems", bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateWorkItem(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusCreated {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusCreated)
	}

	// Check that the client method was called
	if !mockClient.CreateWorkItemCalled {
		t.Error("CreateWorkItem method was not called")
	}

	// Check the response body
	var response models.WorkItem
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Basic validation of the response
	if response.Title != requestBody.Title {
		t.Errorf("handler returned wrong work item title: got %s want %s",
			response.Title, requestBody.Title)
	}
}

// TestCreateWorkItem_InvalidBody tests the CreateWorkItem handler with an invalid request body
func TestCreateWorkItem_InvalidBody(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClient{}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create request with invalid JSON
	req, err := http.NewRequest("POST", "/api/v1/workitems", bytes.NewBuffer([]byte("invalid json")))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateWorkItem(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.CreateWorkItemCalled {
		t.Error("CreateWorkItem method was called despite invalid request body")
	}
}

// TestCreateWorkItem_MissingTitle tests the CreateWorkItem handler with a missing title
func TestCreateWorkItem_MissingTitle(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClient{}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create request body without a title
	requestBody := models.WorkItemCreateRequest{
		Description: "This is a new work item",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create request
	req, err := http.NewRequest("POST", "/api/v1/workitems", bytes.NewBuffer(bodyBytes))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Content-Type", "application/json")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Call the handler
	handler.CreateWorkItem(rr, req)

	// Check the status code
	if status := rr.Code; status != http.StatusBadRequest {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusBadRequest)
	}

	// Check that the client method was not called
	if mockClient.CreateWorkItemCalled {
		t.Error("CreateWorkItem method was called despite missing title")
	}
}

// TestUpdateWorkItem tests the UpdateWorkItem handler
func TestUpdateWorkItem(t *testing.T) {
	// Create mock work item
	mockID := uuid.New()
	mockWorkItem := &models.WorkItem{
		ID:          mockID,
		Title:       "Updated Work Item",
		Description: "This is an updated work item",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityHigh,
		Status:      models.WorkflowStateInDev,
	}

	// Create mock client
	mockClient := &MockJavaClient{
		UpdateWorkItemResponse: mockWorkItem,
	}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create a title for the update
	updatedTitle := "Updated Work Item"

	// Create request body
	requestBody := models.WorkItemUpdateRequest{
		Title: &updatedTitle,
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/workitems/{id}", handler.UpdateWorkItem).Methods("PUT")

	// Create request
	req, err := http.NewRequest("PUT", fmt.Sprintf("/api/v1/workitems/%s", mockID), bytes.NewBuffer(bodyBytes))
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

	// Check that the client method was called
	if !mockClient.UpdateWorkItemCalled {
		t.Error("UpdateWorkItem method was not called")
	}

	// Check the response body
	var response models.WorkItem
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Basic validation of the response
	if response.Title != updatedTitle {
		t.Errorf("handler returned wrong work item title: got %s want %s",
			response.Title, updatedTitle)
	}
}

// TestTransitionWorkItem tests the TransitionWorkItem handler
func TestTransitionWorkItem(t *testing.T) {
	// Create mock work item
	mockID := uuid.New()
	mockWorkItem := &models.WorkItem{
		ID:          mockID,
		Title:       "Work Item",
		Description: "This is a work item",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
		Status:      models.WorkflowStateTesting, // Transitioned state
	}

	// Create mock client
	mockClient := &MockJavaClient{
		TransitionResponse: mockWorkItem,
	}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create request body
	requestBody := models.WorkItemTransitionRequest{
		ToState: models.WorkflowStateTesting,
		Comment: "Moving to testing",
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/workitems/{id}/transitions", handler.TransitionWorkItem).Methods("POST")

	// Create request
	req, err := http.NewRequest("POST", fmt.Sprintf("/api/v1/workitems/%s/transitions", mockID), bytes.NewBuffer(bodyBytes))
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

	// Check that the client method was called
	if !mockClient.TransitionCalled {
		t.Error("TransitionWorkItem method was not called")
	}

	// Check the response body
	var response models.WorkItem
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Errorf("failed to parse response body: %v", err)
	}

	// Basic validation of the response
	if response.Status != models.WorkflowStateTesting {
		t.Errorf("handler returned wrong work item status: got %s want %s",
			response.Status, models.WorkflowStateTesting)
	}
}

// TestTransitionWorkItem_MissingToState tests the TransitionWorkItem handler with a missing ToState
func TestTransitionWorkItem_MissingToState(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClient{}

	// Create handler
	handler := &testWorkItemHandler{javaClient: mockClient}

	// Create request body without ToState
	requestBody := models.WorkItemTransitionRequest{
		Comment: "Missing ToState",
	}

	// Convert request body to JSON
	bodyBytes, err := json.Marshal(requestBody)
	if err != nil {
		t.Fatal(err)
	}

	// Create router with route parameters
	router := mux.NewRouter()
	router.HandleFunc("/api/v1/workitems/{id}/transitions", handler.TransitionWorkItem).Methods("POST")

	// Create request
	req, err := http.NewRequest("POST", "/api/v1/workitems/123/transitions", bytes.NewBuffer(bodyBytes))
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
	if mockClient.TransitionCalled {
		t.Error("TransitionWorkItem method was called despite missing ToState")
	}
}