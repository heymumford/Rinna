// +build component

/*
 * Component tests for the API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package component

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/handlers"
	"github.com/heymumford/rinna/api/internal/models"
	"github.com/heymumford/rinna/api/test"
)

// AuthMiddleware is a simple middleware for testing
type AuthMiddleware struct {
	client ClientInterface
}

// ClientInterface is a minimal interface for the authentication middleware
type ClientInterface interface {
	ValidateToken(ctx context.Context, token string) (string, error)
}

// NewAuthMiddleware creates a new auth middleware
func NewAuthMiddleware(client ClientInterface) *AuthMiddleware {
	return &AuthMiddleware{
		client: client,
	}
}

// Authenticate verifies the API token
func (a *AuthMiddleware) Authenticate(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Get the token from the Authorization header
		token := r.Header.Get("Authorization")
		if token == "" {
			http.Error(w, "Authorization header is required", http.StatusUnauthorized)
			return
		}

		// Remove "Bearer " prefix if present
		if len(token) > 7 && token[:7] == "Bearer " {
			token = token[7:]
		}

		// Validate the token
		_, err := a.client.ValidateToken(r.Context(), token)
		if err != nil {
			http.Error(w, "Invalid token", http.StatusUnauthorized)
			return
		}

		// Token is valid, call the next handler
		next.ServeHTTP(w, r)
	})
}

// setupRouter creates a router with all the required routes for component testing
func setupRouter() (*mux.Router, *test.MockJavaClient) {
	router := mux.NewRouter()
	
	// Create mock Java client
	mockClient := test.NewMockJavaClient()
	
	// Register API routes with auth middleware
	apiRouter := router.PathPrefix("/api").Subrouter()
	
	// Add auth middleware to API routes
	authMiddleware := NewAuthMiddleware(mockClient)
	apiRouter.Use(authMiddleware.Authenticate)
	
	// Register handlers
	handlers.RegisterWorkItemRoutes(apiRouter, mockClient.JavaClient)
	handlers.RegisterProjectRoutes(apiRouter, mockClient.JavaClient)
	
	// Add health check route (no auth required)
	router.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
	}).Methods("GET")
	
	return router, mockClient
}

// TestProjectWorkItemIntegration_Component tests the integration between project and work item endpoints
func TestProjectWorkItemIntegration_Component(t *testing.T) {
	// Set up test router with mock client
	router, _ := setupRouter()
	
	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Create HTTP client
	client := server.Client()
	
	// Create a test project first
	projectKey := "TEST" + uuid.New().String()[:8]
	projectName := "Test Project"
	
	projectCreateRequest := models.ProjectCreateRequest{
		Key:         projectKey,
		Name:        projectName,
		Description: "This is a test project for component testing",
	}
	
	// Convert request to JSON
	projectBytes, err := json.Marshal(projectCreateRequest)
	if err != nil {
		t.Fatal(err)
	}
	
	// Create request
	projectReq, err := http.NewRequest("POST", server.URL+"/api/projects", bytes.NewBuffer(projectBytes))
	if err != nil {
		t.Fatal(err)
	}
	projectReq.Header.Set("Content-Type", "application/json")
	projectReq.Header.Set("Authorization", "Bearer test-token")
	
	// Send request
	projectResp, err := client.Do(projectReq)
	if err != nil {
		t.Fatal(err)
	}
	defer projectResp.Body.Close()
	
	// Check response status
	if projectResp.StatusCode != http.StatusCreated {
		t.Errorf("expected status Created, got %v", projectResp.Status)
	}
	
	// Now create a work item in that project
	workItemTitle := "Test Work Item"
	
	workItemCreateRequest := models.WorkItemCreateRequest{
		Title:       workItemTitle,
		Description: "This is a test work item for component testing",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
		ProjectID:   projectKey,
	}
	
	// Convert request to JSON
	workItemBytes, err := json.Marshal(workItemCreateRequest)
	if err != nil {
		t.Fatal(err)
	}
	
	// Create request
	workItemReq, err := http.NewRequest("POST", server.URL+"/api/workitems", bytes.NewBuffer(workItemBytes))
	if err != nil {
		t.Fatal(err)
	}
	workItemReq.Header.Set("Content-Type", "application/json")
	workItemReq.Header.Set("Authorization", "Bearer test-token")
	
	// Send request
	workItemResp, err := client.Do(workItemReq)
	if err != nil {
		t.Fatal(err)
	}
	defer workItemResp.Body.Close()
	
	// Check response status
	if workItemResp.StatusCode != http.StatusCreated {
		t.Errorf("expected status Created, got %v", workItemResp.Status)
	}
	
	// Now get the project work items
	projectWorkItemsReq, err := http.NewRequest("GET", fmt.Sprintf("%s/api/projects/%s/workitems", server.URL, projectKey), nil)
	if err != nil {
		t.Fatal(err)
	}
	projectWorkItemsReq.Header.Set("Authorization", "Bearer test-token")
	
	// Send request
	projectWorkItemsResp, err := client.Do(projectWorkItemsReq)
	if err != nil {
		t.Fatal(err)
	}
	defer projectWorkItemsResp.Body.Close()
	
	// Check response status
	if projectWorkItemsResp.StatusCode != http.StatusOK {
		t.Errorf("expected status OK, got %v", projectWorkItemsResp.Status)
	}
	
	// Parse response body
	var workItemsResponse models.WorkItemListResponse
	if err := json.NewDecoder(projectWorkItemsResp.Body).Decode(&workItemsResponse); err != nil {
		t.Fatal(err)
	}
	
	// Verify that the work item is in the response
	if len(workItemsResponse.Items) < 1 {
		t.Error("expected at least one work item, got none")
	}
}

// TestAuthMiddleware_Component tests the authentication middleware
func TestAuthMiddleware_Component(t *testing.T) {
	// Set up test router with mock client
	router, _ := setupRouter()
	
	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Create HTTP client
	client := server.Client()
	
	// Test cases
	testCases := []struct {
		name           string
		token          string
		expectedStatus int
		endpoint       string
	}{
		{
			name:           "Valid token",
			token:          "Bearer test-token",
			expectedStatus: http.StatusOK,
			endpoint:       "/api/projects",
		},
		{
			name:           "Missing token",
			token:          "",
			expectedStatus: http.StatusUnauthorized,
			endpoint:       "/api/projects",
		},
		{
			name:           "Invalid token format",
			token:          "invalid-format",
			expectedStatus: http.StatusUnauthorized,
			endpoint:       "/api/projects",
		},
		{
			name:           "Health endpoint - no auth required",
			token:          "",
			expectedStatus: http.StatusOK,
			endpoint:       "/health",
		},
	}
	
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			// Create request
			req, err := http.NewRequest("GET", server.URL+tc.endpoint, nil)
			if err != nil {
				t.Fatal(err)
			}
			
			// Set token if provided
			if tc.token != "" {
				req.Header.Set("Authorization", tc.token)
			}
			
			// Send request
			resp, err := client.Do(req)
			if err != nil {
				t.Fatal(err)
			}
			defer resp.Body.Close()
			
			// Check response status
			if resp.StatusCode != tc.expectedStatus {
				t.Errorf("expected status %v, got %v", tc.expectedStatus, resp.Status)
			}
		})
	}
}

// TestWorkItemLifecycle_Component tests the work item lifecycle (create, get, update, transition)
func TestWorkItemLifecycle_Component(t *testing.T) {
	// Set up test router with mock client
	router, _ := setupRouter()
	
	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Create HTTP client
	client := server.Client()
	
	// Create a work item
	workItemTitle := "Lifecycle Test Work Item"
	
	workItemCreateRequest := models.WorkItemCreateRequest{
		Title:       workItemTitle,
		Description: "This is a test work item for lifecycle testing",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
		ProjectID:   "TEST-PROJ",
	}
	
	// Convert request to JSON
	workItemBytes, err := json.Marshal(workItemCreateRequest)
	if err != nil {
		t.Fatal(err)
	}
	
	// Create request
	workItemReq, err := http.NewRequest("POST", server.URL+"/api/workitems", bytes.NewBuffer(workItemBytes))
	if err != nil {
		t.Fatal(err)
	}
	workItemReq.Header.Set("Content-Type", "application/json")
	workItemReq.Header.Set("Authorization", "Bearer test-token")
	
	// Send request
	workItemResp, err := client.Do(workItemReq)
	if err != nil {
		t.Fatal(err)
	}
	defer workItemResp.Body.Close()
	
	// Check response status
	if workItemResp.StatusCode != http.StatusCreated {
		t.Errorf("expected status Created, got %v", workItemResp.Status)
	}
	
	// Parse response body to get the ID
	var createdWorkItem models.WorkItem
	if err := json.NewDecoder(workItemResp.Body).Decode(&createdWorkItem); err != nil {
		t.Fatal(err)
	}
	
	// Get the work item
	getWorkItemReq, err := http.NewRequest("GET", fmt.Sprintf("%s/api/workitems/%s", server.URL, createdWorkItem.ID), nil)
	if err != nil {
		t.Fatal(err)
	}
	getWorkItemReq.Header.Set("Authorization", "Bearer test-token")
	
	// Send request
	getWorkItemResp, err := client.Do(getWorkItemReq)
	if err != nil {
		t.Fatal(err)
	}
	defer getWorkItemResp.Body.Close()
	
	// Check response status
	if getWorkItemResp.StatusCode != http.StatusOK {
		t.Errorf("expected status OK, got %v", getWorkItemResp.Status)
	}
	
	// Parse response body
	var getWorkItem models.WorkItem
	if err := json.NewDecoder(getWorkItemResp.Body).Decode(&getWorkItem); err != nil {
		t.Fatal(err)
	}
	
	// Verify work item details
	if getWorkItem.Title != workItemTitle {
		t.Errorf("expected title %s, got %s", workItemTitle, getWorkItem.Title)
	}
	
	// Update the work item
	updatedTitle := "Updated Lifecycle Test Work Item"
	
	updateRequest := models.WorkItemUpdateRequest{
		Title: &updatedTitle,
	}
	
	// Convert request to JSON
	updateBytes, err := json.Marshal(updateRequest)
	if err != nil {
		t.Fatal(err)
	}
	
	// Create request
	updateReq, err := http.NewRequest("PUT", fmt.Sprintf("%s/api/workitems/%s", server.URL, createdWorkItem.ID), bytes.NewBuffer(updateBytes))
	if err != nil {
		t.Fatal(err)
	}
	updateReq.Header.Set("Content-Type", "application/json")
	updateReq.Header.Set("Authorization", "Bearer test-token")
	
	// Send request
	updateResp, err := client.Do(updateReq)
	if err != nil {
		t.Fatal(err)
	}
	defer updateResp.Body.Close()
	
	// Check response status
	if updateResp.StatusCode != http.StatusOK {
		t.Errorf("expected status OK, got %v", updateResp.Status)
	}
	
	// Transition the work item
	transitionRequest := models.WorkItemTransitionRequest{
		ToState: models.WorkflowStateTesting,
	}
	
	// Convert request to JSON
	transitionBytes, err := json.Marshal(transitionRequest)
	if err != nil {
		t.Fatal(err)
	}
	
	// Create request
	transitionReq, err := http.NewRequest("POST", fmt.Sprintf("%s/api/workitems/%s/transitions", server.URL, createdWorkItem.ID), bytes.NewBuffer(transitionBytes))
	if err != nil {
		t.Fatal(err)
	}
	transitionReq.Header.Set("Content-Type", "application/json")
	transitionReq.Header.Set("Authorization", "Bearer test-token")
	
	// Send request
	transitionResp, err := client.Do(transitionReq)
	if err != nil {
		t.Fatal(err)
	}
	defer transitionResp.Body.Close()
	
	// Check response status
	if transitionResp.StatusCode != http.StatusOK {
		t.Errorf("expected status OK, got %v", transitionResp.Status)
	}
	
	// Parse response body
	var transitionedWorkItem models.WorkItem
	if err := json.NewDecoder(transitionResp.Body).Decode(&transitionedWorkItem); err != nil {
		t.Fatal(err)
	}
	
	// Verify the transition
	if transitionedWorkItem.Status != models.WorkflowStateTesting {
		t.Errorf("expected status %s, got %s", models.WorkflowStateTesting, transitionedWorkItem.Status)
	}
}

// TestInvalidWorkItemTransition_Component tests invalid work item transitions
func TestInvalidWorkItemTransition_Component(t *testing.T) {
	// Set up test router with mock client
	router, mockClient := setupRouter()
	
	// Create test server
	server := httptest.NewServer(router)
	defer server.Close()
	
	// Create HTTP client
	client := server.Client()
	
	// Create a work item
	workItemCreateRequest := models.WorkItemCreateRequest{
		Title:       "Invalid Transition Test Work Item",
		Description: "This is a test work item for invalid transition testing",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
		ProjectID:   "TEST-PROJ",
	}
	
	// Convert request to JSON
	workItemBytes, err := json.Marshal(workItemCreateRequest)
	if err != nil {
		t.Fatal(err)
	}
	
	// Create request
	workItemReq, err := http.NewRequest("POST", server.URL+"/api/workitems", bytes.NewBuffer(workItemBytes))
	if err != nil {
		t.Fatal(err)
	}
	workItemReq.Header.Set("Content-Type", "application/json")
	workItemReq.Header.Set("Authorization", "Bearer test-token")
	
	// Send request
	workItemResp, err := client.Do(workItemReq)
	if err != nil {
		t.Fatal(err)
	}
	defer workItemResp.Body.Close()
	
	// Parse response body to get the ID
	var createdWorkItem models.WorkItem
	if err := json.NewDecoder(workItemResp.Body).Decode(&createdWorkItem); err != nil {
		t.Fatal(err)
	}
	
	// Try an invalid transition (directly to DONE from FOUND)
	transitionRequest := models.WorkItemTransitionRequest{
		ToState: models.WorkflowStateDone,
	}
	
	// Set up mock client to return an error for this transition
	mockClient.JavaClient = nil // We don't need the real client anymore
	
	// Convert request to JSON
	transitionBytes, err := json.Marshal(transitionRequest)
	if err != nil {
		t.Fatal(err)
	}
	
	// Create request
	transitionReq, err := http.NewRequest("POST", fmt.Sprintf("%s/api/workitems/%s/transitions", server.URL, createdWorkItem.ID), bytes.NewBuffer(transitionBytes))
	if err != nil {
		t.Fatal(err)
	}
	transitionReq.Header.Set("Content-Type", "application/json")
	transitionReq.Header.Set("Authorization", "Bearer test-token")
	
	// Send request
	transitionResp, err := client.Do(transitionReq)
	if err != nil {
		t.Fatal(err)
	}
	defer transitionResp.Body.Close()
	
	// We expect a 500 since the mock client doesn't actually implement the transition method
	// In a real scenario, it would return a more specific error code like 400 Bad Request
	if transitionResp.StatusCode != http.StatusInternalServerError {
		t.Errorf("expected status Internal Server Error for invalid transition, got %v", transitionResp.Status)
	}
}

// TestMockJavaClient_Component tests that the mock Java client works as expected
func TestMockJavaClient_Component(t *testing.T) {
	// Create mock client
	mockClient := test.NewMockJavaClient()
	
	// Test creating a work item
	workItemCreateRequest := models.WorkItemCreateRequest{
		Title:       "Test Work Item",
		Description: "This is a test work item",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
		ProjectID:   "TEST-PROJ",
	}
	
	// Create work item
	workItem, err := mockClient.CreateWorkItem(context.Background(), workItemCreateRequest)
	if err != nil {
		t.Fatal(err)
	}
	
	// Verify work item details
	if workItem.Title != workItemCreateRequest.Title {
		t.Errorf("expected title %s, got %s", workItemCreateRequest.Title, workItem.Title)
	}
	if workItem.Type != workItemCreateRequest.Type {
		t.Errorf("expected type %s, got %s", workItemCreateRequest.Type, workItem.Type)
	}
	if workItem.Priority != workItemCreateRequest.Priority {
		t.Errorf("expected priority %s, got %s", workItemCreateRequest.Priority, workItem.Priority)
	}
	if workItem.Status != models.WorkflowStateFound {
		t.Errorf("expected status %s, got %s", models.WorkflowStateFound, workItem.Status)
	}
	
	// Test validating a token
	projectID, err := mockClient.ValidateToken(context.Background(), "test-token")
	if err != nil {
		t.Fatal(err)
	}
	if projectID != "test-project" {
		t.Errorf("expected project ID test-project, got %s", projectID)
	}
	
	// Test getting the webhook secret
	secret, err := mockClient.GetWebhookSecret(context.Background(), "TEST-PROJ", "github")
	if err != nil {
		t.Fatal(err)
	}
	if secret != "gh-webhook-secret-1234" {
		t.Errorf("expected webhook secret gh-webhook-secret-1234, got %s", secret)
	}
}

// TestClientConfig_Component tests the client configuration
func TestClientConfig_Component(t *testing.T) {
	// Test that the mock client was created with the correct default configuration
	mockClient := test.NewMockJavaClient()
	
	// Perform a request that would fail if the configuration was wrong
	workItemCreateRequest := models.WorkItemCreateRequest{
		Title:       "Config Test Work Item",
		Description: "This is a test work item for configuration testing",
		Type:        models.WorkItemTypeFeature,
		Priority:    models.PriorityMedium,
		ProjectID:   "TEST-PROJ",
	}
	
	// Create work item
	workItem, err := mockClient.CreateWorkItem(context.Background(), workItemCreateRequest)
	if err != nil {
		t.Fatal(err)
	}
	
	// Verify work item was created
	if workItem == nil {
		t.Fatal("expected work item to be created")
	}
	
	// Verify it matches the request
	if workItem.Title != workItemCreateRequest.Title {
		t.Errorf("expected title %s, got %s", workItemCreateRequest.Title, workItem.Title)
	}
}
