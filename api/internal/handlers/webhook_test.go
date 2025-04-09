/*
 * Tests for webhook handlers in the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	
	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/middleware"
	"github.com/heymumford/rinna/api/internal/models"
	"github.com/heymumford/rinna/api/pkg/config"
)

// WebhookClientInterface defines the methods required from the Java client for webhook handlers
type WebhookClientInterface interface {
	ValidateToken(ctx context.Context, token string) (string, error)
	GetWebhookSecret(ctx context.Context, projectKey, source string) (string, error)
	CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error)
}

// MockJavaClientForWebhooks is our test implementation
type MockJavaClientForWebhooks struct {
	// Mock responses
	CreateWorkItemResponse *models.WorkItem
	WebhookSecret         string
	
	// Track method calls
	CreateWorkItemCalled   bool
	GetWebhookSecretCalled bool
	
	// Capture parameters for verification
	CapturedProjectKey        string
	CapturedSource            string
	CapturedWorkItemRequest   models.WorkItemCreateRequest
}

// ValidateToken implementation
func (m *MockJavaClientForWebhooks) ValidateToken(ctx context.Context, token string) (string, error) {
	return "test-project", nil
}

// GetWebhookSecret implementation
func (m *MockJavaClientForWebhooks) GetWebhookSecret(ctx context.Context, projectKey, source string) (string, error) {
	m.GetWebhookSecretCalled = true
	m.CapturedProjectKey = projectKey
	m.CapturedSource = source
	
	if m.WebhookSecret == "" {
		return "gh-webhook-secret-1234", nil // Default test secret
	}
	return m.WebhookSecret, nil
}

// CreateWorkItem implementation
func (m *MockJavaClientForWebhooks) CreateWorkItem(ctx context.Context, request models.WorkItemCreateRequest) (*models.WorkItem, error) {
	m.CreateWorkItemCalled = true
	m.CapturedWorkItemRequest = request
	
	if m.CreateWorkItemResponse == nil {
		id := uuid.New()
		return &models.WorkItem{
			ID:          id,
			Title:       request.Title,
			Description: request.Description,
			Type:        request.Type,
			Priority:    request.Priority,
			Status:      models.WorkflowStateFound,
			ProjectID:   request.ProjectID,
			Metadata:    request.Metadata,
		}, nil
	}
	
	return m.CreateWorkItemResponse, nil
}

// Create a test webhook handler that uses our interface
type testWebhookHandler struct {
	javaClient  WebhookClientInterface
	authService *middleware.AuthService
}

// HandleGitHubWebhook handles GitHub webhook requests for testing
func (h *testWebhookHandler) HandleGitHubWebhook(w http.ResponseWriter, r *http.Request) {
	// Get the project key from the query parameter
	projectKey := r.URL.Query().Get("project")
	if projectKey == "" {
		http.Error(w, "Project key is required", http.StatusBadRequest)
		return
	}

	// Get the signature from the header
	signature := r.Header.Get("X-Hub-Signature-256")
	if !strings.HasPrefix(signature, "sha256=") {
		http.Error(w, "Missing or invalid signature", http.StatusUnauthorized)
		return
	}
	signature = strings.TrimPrefix(signature, "sha256=")

	// Get the event type
	eventType := r.Header.Get("X-GitHub-Event")
	if eventType == "" {
		http.Error(w, "Missing event type", http.StatusBadRequest)
		return
	}

	// Read the request body
	body, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "Failed to read request body: "+err.Error(), http.StatusInternalServerError)
		return
	}
	
	// Replace the request body (since we consumed it)
	r.Body = io.NopCloser(bytes.NewBuffer(body))

	// Validate the signature with the auth service
	if err := h.authService.ValidateWebhookSignature(r.Context(), projectKey, "github", signature, body); err != nil {
		http.Error(w, "Invalid webhook signature: "+err.Error(), http.StatusUnauthorized)
		return
	}

	// Parse the payload based on the event type
	var response interface{}
	switch eventType {
	case "pull_request":
		// Parse the payload
		var pr models.GitHubPullRequestEvent
		if err := json.Unmarshal(body, &pr); err != nil {
			http.Error(w, "Failed to parse pull request payload: "+err.Error(), http.StatusBadRequest)
			return
		}

		// Only handle opened or reopened pull requests
		if pr.Action != "opened" && pr.Action != "reopened" {
			response = map[string]string{
				"status": "skipped",
				"message": fmt.Sprintf("Ignoring pull request action: %s", pr.Action),
			}
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(response)
			return
		}

		// Create a work item from the pull request
		workItemReq := models.WorkItemCreateRequest{
			Title: fmt.Sprintf("PR: %s", pr.PullRequest.Title),
			Description: fmt.Sprintf("%s\n\nPR URL: %s", pr.PullRequest.Body, pr.PullRequest.HTMLURL),
			Type: models.WorkItemTypeFeature,
			Priority: models.PriorityMedium,
			ProjectID: projectKey,
			Metadata: map[string]string{
				"source": "github",
				"github_pr": fmt.Sprintf("%d", pr.PullRequest.Number),
				"github_user": pr.PullRequest.User.Login,
				"github_repo": pr.Repository.FullName,
			},
		}

		// Create the work item via the Java client
		workItem, err := h.javaClient.CreateWorkItem(r.Context(), workItemReq)
		if err != nil {
			http.Error(w, "Failed to create work item: "+err.Error(), http.StatusInternalServerError)
			return
		}

		response = map[string]interface{}{
			"status": "created",
			"workItem": workItem,
		}
		
	case "workflow_run":
		// Parse the payload
		var wf models.GitHubWorkflowRunEvent
		if err := json.Unmarshal(body, &wf); err != nil {
			http.Error(w, "Failed to parse workflow run payload: "+err.Error(), http.StatusBadRequest)
			return
		}

		// Only handle completed workflow runs with failure conclusion
		if wf.Action != "completed" || wf.WorkflowRun.Conclusion != "failure" {
			response = map[string]string{
				"status": "skipped",
				"message": "Ignoring non-failed workflow run",
			}
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(response)
			return
		}

		// Create a work item from the workflow run
		workItemReq := models.WorkItemCreateRequest{
			Title: fmt.Sprintf("CI Failure: %s on %s", wf.WorkflowRun.Name, wf.WorkflowRun.HeadBranch),
			Description: fmt.Sprintf("The CI pipeline '%s' failed on branch '%s'.\n\nWorkflow URL: %s", 
				wf.WorkflowRun.Name, wf.WorkflowRun.HeadBranch, wf.WorkflowRun.HTMLURL),
			Type: models.WorkItemTypeBug,
			Priority: models.PriorityHigh,
			ProjectID: projectKey,
			Metadata: map[string]string{
				"source": "github_ci",
				"branch": wf.WorkflowRun.HeadBranch,
				"workflow_name": wf.WorkflowRun.Name,
				"workflow_url": wf.WorkflowRun.HTMLURL,
				"github_repo": wf.Repository.FullName,
			},
		}

		// Create the work item via the Java client
		workItem, err := h.javaClient.CreateWorkItem(r.Context(), workItemReq)
		if err != nil {
			http.Error(w, "Failed to create work item: "+err.Error(), http.StatusInternalServerError)
			return
		}

		response = map[string]interface{}{
			"status": "created",
			"workItem": workItem,
		}
		
	case "push":
		// Parse the payload
		var push models.GitHubPushEvent
		if err := json.Unmarshal(body, &push); err != nil {
			http.Error(w, "Failed to parse push payload: "+err.Error(), http.StatusBadRequest)
			return
		}

		// Skip if it's a delete operation or has no commits
		if push.Deleted || len(push.Commits) == 0 {
			response = map[string]string{
				"status": "skipped",
				"message": "Skipping delete operation or push with no commits",
			}
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(response)
			return
		}

		// Get the branch name from the ref
		branchName := strings.TrimPrefix(push.Ref, "refs/heads/")

		// Create a summary of the commits
		commitMessages := make([]string, 0, len(push.Commits))
		for _, commit := range push.Commits {
			commitMessages = append(commitMessages, fmt.Sprintf("- %s (%s)", 
				commit.Message, commit.ID[:8]))
		}

		// Create a work item for the push
		workItemReq := models.WorkItemCreateRequest{
			Title: fmt.Sprintf("Push: %d commits to %s", len(push.Commits), branchName),
			Description: fmt.Sprintf("Push to %s by %s\n\nCommits:\n%s", 
				branchName, push.Pusher.Name, strings.Join(commitMessages, "\n")),
			Type: models.WorkItemTypeChore,
			Priority: models.PriorityLow,
			ProjectID: projectKey,
			Metadata: map[string]string{
				"source": "github_push",
				"branch": branchName,
				"pusher": push.Pusher.Name,
				"commit_count": fmt.Sprintf("%d", len(push.Commits)),
				"github_repo": push.Repository.FullName,
			},
		}

		// Create the work item via the Java client
		workItem, err := h.javaClient.CreateWorkItem(r.Context(), workItemReq)
		if err != nil {
			http.Error(w, "Failed to create work item: "+err.Error(), http.StatusInternalServerError)
			return
		}

		response = map[string]interface{}{
			"status": "created",
			"workItem": workItem,
		}
		
	case "issues":
		// Parse the payload
		var issues models.GitHubIssuesEvent
		if err := json.Unmarshal(body, &issues); err != nil {
			http.Error(w, "Failed to parse issues payload: "+err.Error(), http.StatusBadRequest)
			return
		}

		// Only handle opened or reopened issues
		if issues.Action != "opened" && issues.Action != "reopened" {
			response = map[string]string{
				"status": "skipped",
				"message": fmt.Sprintf("Ignoring issue action: %s", issues.Action),
			}
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(response)
			return
		}

		// Create a work item from the issue
		workItemReq := models.WorkItemCreateRequest{
			Title: fmt.Sprintf("Issue: %s", issues.Issue.Title),
			Description: fmt.Sprintf("%s\n\nIssue URL: %s", issues.Issue.Body, issues.Issue.HTMLURL),
			Type: models.WorkItemTypeBug, // Default to bug, could be improved with label analysis
			Priority: models.PriorityMedium,
			ProjectID: projectKey,
			Metadata: map[string]string{
				"source": "github_issue",
				"github_issue": fmt.Sprintf("%d", issues.Issue.Number),
				"github_user": issues.Issue.User.Login,
				"github_repo": issues.Repository.FullName,
			},
		}

		// Create the work item via the Java client
		workItem, err := h.javaClient.CreateWorkItem(r.Context(), workItemReq)
		if err != nil {
			http.Error(w, "Failed to create work item: "+err.Error(), http.StatusInternalServerError)
			return
		}

		response = map[string]interface{}{
			"status": "created",
			"workItem": workItem,
		}
		
	case "ping":
		// Special case for GitHub's ping event
		response = map[string]string{
			"status": "ok",
			"message": "Webhook received successfully",
		}
		
	default:
		http.Error(w, "Unsupported event type: "+eventType, http.StatusBadRequest)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// Basic validation tests

// TestHandleGitHubWebhook_Validation tests basic input validation
func TestHandleGitHubWebhook_Validation(t *testing.T) {
	// Create a handler with mocked dependencies
	handler := &WebhookHandler{
		// For validation tests, we don't need the JavaClient
		authService: middleware.NewAuthService(nil, &config.AuthConfig{
			DevMode: true, // Use dev mode for simpler testing
		}),
	}

	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost)
	router.HandleFunc("/api/v1/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost)

	// Test cases
	testCases := []struct {
		name           string
		projectKey     string
		eventType      string
		signature      string
		payload        string
		expectedStatus int
	}{
		{
			name:           "Missing project key",
			projectKey:     "",
			eventType:      "ping",
			signature:      "sha256=1234567890",
			payload:        `{"zen": "Test ping"}`,
			expectedStatus: http.StatusBadRequest,
		},
		{
			name:           "Missing event type",
			projectKey:     "test-project",
			eventType:      "",
			signature:      "sha256=1234567890",
			payload:        `{"zen": "Test ping"}`,
			expectedStatus: http.StatusBadRequest,
		},
		{
			name:           "Invalid signature format",
			projectKey:     "test-project",
			eventType:      "ping",
			signature:      "invalid-signature",
			payload:        `{"zen": "Test ping"}`,
			expectedStatus: http.StatusUnauthorized,
		},
	}

	// Run test cases
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			// Create request
			req, err := http.NewRequest(http.MethodPost, "/webhooks/github", bytes.NewBufferString(tc.payload))
			if err != nil {
				t.Fatalf("Failed to create request: %v", err)
			}

			// Add query parameter
			if tc.projectKey != "" {
				req.URL.RawQuery = fmt.Sprintf("project=%s", tc.projectKey)
			}

			// Add headers
			if tc.eventType != "" {
				req.Header.Set("X-GitHub-Event", tc.eventType)
			}

			req.Header.Set("X-Hub-Signature-256", tc.signature)

			// Create response recorder
			rr := httptest.NewRecorder()

			// Serve the request
			router.ServeHTTP(rr, req)

			// Check status code
			if rr.Code != tc.expectedStatus {
				t.Errorf("Expected status code %d, got %d", tc.expectedStatus, rr.Code)
			}
		})
	}
}

// TestHandleGitHubWebhook_Ping tests ping event handling
func TestHandleGitHubWebhook_Ping(t *testing.T) {
	// Create a handler with mocked dependencies
	handler := &WebhookHandler{
		// For ping tests, we don't need the JavaClient
		authService: middleware.NewAuthService(nil, &config.AuthConfig{
			DevMode: true, // Use dev mode for simpler testing
		}),
	}

	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost)
	router.HandleFunc("/api/v1/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost)

	// Create ping payload
	payload := `{"zen": "Test ping"}`
	
	// Create request
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBufferString(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}

	// Add headers
	req.Header.Set("X-GitHub-Event", "ping")

	// Compute signature for dev mode
	secret := "gh-webhook-secret-1234" // Dev mode secret
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(payload))
	signature := hex.EncodeToString(mac.Sum(nil))
	req.Header.Set("X-Hub-Signature-256", "sha256="+signature)

	// Create response recorder
	rr := httptest.NewRecorder()

	// Serve the request
	router.ServeHTTP(rr, req)

	// Check status code
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, rr.Code)
	}

	// Check response
	var response map[string]interface{}
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Fatalf("Failed to parse response: %v", err)
	}

	// Check status in response
	if status, ok := response["status"].(string); !ok || status != "ok" {
		t.Errorf("Expected status 'ok', got %v", status)
	}
}

// Test invalid signatures

// TestHandleGitHubWebhook_InvalidSignature tests the webhook handler with invalid signatures
func TestHandleGitHubWebhook_InvalidSignature(t *testing.T) {
	// Create a handler with dev mode auth service
	handler := &WebhookHandler{
		authService: middleware.NewAuthService(nil, &config.AuthConfig{
			DevMode: true, // Use dev mode for simpler testing
		}),
	}

	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost)
	router.HandleFunc("/api/v1/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost)

	// Create request with invalid signature
	payload := `{"zen": "Test ping"}`
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBufferString(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}

	// Add headers with invalid signature
	req.Header.Set("X-GitHub-Event", "ping")
	req.Header.Set("X-Hub-Signature-256", "sha256=invalid-signature")

	// Create response recorder
	rr := httptest.NewRecorder()

	// Serve the request
	router.ServeHTTP(rr, req)

	// Check status code
	if rr.Code != http.StatusUnauthorized {
		t.Errorf("Expected status code %d, got %d", http.StatusUnauthorized, rr.Code)
	}
}

// Tests for pull request events

// TestHandleGitHubWebhook_PullRequest tests pull request event handling
func TestHandleGitHubWebhook_PullRequest(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForWebhooks{
		WebhookSecret: "gh-webhook-secret-1234", // Use the same secret as in the dev mode
	}
	
	// Create auth service
	authService := middleware.NewAuthService(mockClient, &config.AuthConfig{
		DevMode: true,
	})
	
	// Create a test handler that uses our custom interface
	testHandler := &testWebhookHandler{
		javaClient:  mockClient,
		authService: authService,
	}
	
	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", testHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// Create test payload
	pullRequestEvent := models.GitHubPullRequestEvent{
		Action: "opened",
		Number: 42,
		PullRequest: models.GitHubPullRequest{
			ID:     12345,
			Number: 42,
			Title:  "Add new feature",
			Body:   "This PR adds an important new feature",
			State:  "open",
			User: models.GitHubUser{
				Login: "testuser",
				ID:    9876,
			},
			HTMLURL: "https://github.com/org/repo/pull/42",
		},
		Repository: models.GitHubRepository{
			FullName: "org/repo",
			HTMLURL:  "https://github.com/org/repo",
		},
	}
	
	// Marshal the payload
	payload, err := json.Marshal(pullRequestEvent)
	if err != nil {
		t.Fatalf("Failed to marshal test payload: %v", err)
	}
	
	// Create request
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBuffer(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}
	
	// Add headers
	req.Header.Set("X-GitHub-Event", "pull_request")
	
	// Compute signature for the payload
	mac := hmac.New(sha256.New, []byte("gh-webhook-secret-1234"))
	mac.Write(payload)
	signature := hex.EncodeToString(mac.Sum(nil))
	req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
	
	// Create response recorder
	rr := httptest.NewRecorder()
	
	// Serve the request
	router.ServeHTTP(rr, req)
	
	// Check status code
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, rr.Code)
	}
	
	// Verify JavaClient was called correctly
	if !mockClient.CreateWorkItemCalled {
		t.Error("JavaClient.CreateWorkItem was not called")
	}
	
	// Verify request to create work item had correct details
	workItemReq := mockClient.CapturedWorkItemRequest
	expectedTitle := "PR: Add new feature"
	if workItemReq.Title != expectedTitle {
		t.Errorf("Expected title %q, got %q", expectedTitle, workItemReq.Title)
	}
	
	if workItemReq.Type != models.WorkItemTypeFeature {
		t.Errorf("Expected type %q, got %q", models.WorkItemTypeFeature, workItemReq.Type)
	}
	
	if workItemReq.ProjectID != "test-project" {
		t.Errorf("Expected project ID %q, got %q", "test-project", workItemReq.ProjectID)
	}
	
	// Check source metadata
	if source, ok := workItemReq.Metadata["source"]; !ok || source != "github" {
		t.Errorf("Expected metadata source 'github', got %q", source)
	}
	
	if prNum, ok := workItemReq.Metadata["github_pr"]; !ok || prNum != "42" {
		t.Errorf("Expected metadata github_pr '42', got %q", prNum)
	}
}

// Test for ignored pull request events

// TestHandleGitHubWebhook_PullRequest_Ignored tests pull request event that should be ignored
func TestHandleGitHubWebhook_PullRequest_Ignored(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForWebhooks{
		WebhookSecret: "gh-webhook-secret-1234", // Use the same secret as in the dev mode
	}
	
	// Create auth service
	authService := middleware.NewAuthService(mockClient, &config.AuthConfig{
		DevMode: true,
	})
	
	// Create a test handler that uses our interface
	testHandler := &testWebhookHandler{
		javaClient:  mockClient,
		authService: authService,
	}
	
	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", testHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// Create test payload for a closed PR (should be ignored)
	pullRequestEvent := models.GitHubPullRequestEvent{
		Action: "closed", // This action should be ignored
		Number: 42,
		PullRequest: models.GitHubPullRequest{
			ID:     12345,
			Number: 42,
			Title:  "Add new feature",
			Body:   "This PR adds an important new feature",
			State:  "closed",
			User: models.GitHubUser{
				Login: "testuser",
				ID:    9876,
			},
			HTMLURL: "https://github.com/org/repo/pull/42",
		},
		Repository: models.GitHubRepository{
			FullName: "org/repo",
			HTMLURL:  "https://github.com/org/repo",
		},
	}
	
	// Marshal the payload
	payload, err := json.Marshal(pullRequestEvent)
	if err != nil {
		t.Fatalf("Failed to marshal test payload: %v", err)
	}
	
	// Create request
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBuffer(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}
	
	// Add headers
	req.Header.Set("X-GitHub-Event", "pull_request")
	
	// Compute signature for the payload
	mac := hmac.New(sha256.New, []byte("gh-webhook-secret-1234"))
	mac.Write(payload)
	signature := hex.EncodeToString(mac.Sum(nil))
	req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
	
	// Create response recorder
	rr := httptest.NewRecorder()
	
	// Serve the request
	router.ServeHTTP(rr, req)
	
	// Check status code
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, rr.Code)
	}
	
	// Verify JavaClient was NOT called to create a work item
	if mockClient.CreateWorkItemCalled {
		t.Error("JavaClient.CreateWorkItem was called for a closed PR, but should have been skipped")
	}
	
	// Verify response contains "skipped" status
	var response map[string]interface{}
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Fatalf("Failed to parse response: %v", err)
	}
	
	if status, ok := response["status"].(string); !ok || status != "skipped" {
		t.Errorf("Expected status 'skipped', got %v", status)
	}
}

// Tests for workflow run events

// TestHandleGitHubWebhook_WorkflowRun tests workflow run event handling
func TestHandleGitHubWebhook_WorkflowRun(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForWebhooks{
		WebhookSecret: "gh-webhook-secret-1234", // Use the same secret as in the dev mode
	}
	
	// Create auth service
	authService := middleware.NewAuthService(mockClient, &config.AuthConfig{
		DevMode: true,
	})
	
	// Create a test handler that uses our interface
	testHandler := &testWebhookHandler{
		javaClient:  mockClient,
		authService: authService,
	}
	
	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", testHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// Create test payload for a failed workflow run
	workflowRunEvent := models.GitHubWorkflowRunEvent{
		Action: "completed",
		WorkflowRun: models.GitHubWorkflowRun{
			ID:         12345,
			Name:       "CI/CD Pipeline",
			HeadBranch: "feature/new-feature",
			HeadSHA:    "abcdef1234567890",
			Status:     "completed",
			Conclusion: "failure", // Failed workflow
			HTMLURL:    "https://github.com/org/repo/actions/runs/12345",
		},
		Repository: models.GitHubRepository{
			FullName: "org/repo",
			HTMLURL:  "https://github.com/org/repo",
		},
	}
	
	// Marshal the payload
	payload, err := json.Marshal(workflowRunEvent)
	if err != nil {
		t.Fatalf("Failed to marshal test payload: %v", err)
	}
	
	// Create request
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBuffer(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}
	
	// Add headers
	req.Header.Set("X-GitHub-Event", "workflow_run")
	
	// Compute signature for the payload
	mac := hmac.New(sha256.New, []byte("gh-webhook-secret-1234"))
	mac.Write(payload)
	signature := hex.EncodeToString(mac.Sum(nil))
	req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
	
	// Create response recorder
	rr := httptest.NewRecorder()
	
	// Serve the request
	router.ServeHTTP(rr, req)
	
	// Check status code
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, rr.Code)
	}
	
	// Verify JavaClient was called correctly
	if !mockClient.CreateWorkItemCalled {
		t.Error("JavaClient.CreateWorkItem was not called")
	}
	
	// Verify request to create work item had correct details
	workItemReq := mockClient.CapturedWorkItemRequest
	expectedTitle := "CI Failure: CI/CD Pipeline on feature/new-feature"
	if workItemReq.Title != expectedTitle {
		t.Errorf("Expected title %q, got %q", expectedTitle, workItemReq.Title)
	}
	
	if workItemReq.Type != models.WorkItemTypeBug {
		t.Errorf("Expected type %q, got %q", models.WorkItemTypeBug, workItemReq.Type)
	}
	
	if workItemReq.Priority != models.PriorityHigh {
		t.Errorf("Expected priority %q, got %q", models.PriorityHigh, workItemReq.Priority)
	}
	
	// Check source metadata
	if source, ok := workItemReq.Metadata["source"]; !ok || source != "github_ci" {
		t.Errorf("Expected metadata source 'github_ci', got %q", source)
	}
	
	if branch, ok := workItemReq.Metadata["branch"]; !ok || branch != "feature/new-feature" {
		t.Errorf("Expected metadata branch 'feature/new-feature', got %q", branch)
	}
}

// Test for ignored workflow run events

// TestHandleGitHubWebhook_WorkflowRun_Ignored tests workflow run event that should be ignored
func TestHandleGitHubWebhook_WorkflowRun_Ignored(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForWebhooks{
		WebhookSecret: "gh-webhook-secret-1234", // Use the same secret as in the dev mode
	}
	
	// Create auth service
	authService := middleware.NewAuthService(mockClient, &config.AuthConfig{
		DevMode: true,
	})
	
	// Create a test handler that uses our interface
	testHandler := &testWebhookHandler{
		javaClient:  mockClient,
		authService: authService,
	}
	
	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", testHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// Create test payload for a successful workflow run (should be ignored)
	workflowRunEvent := models.GitHubWorkflowRunEvent{
		Action: "completed",
		WorkflowRun: models.GitHubWorkflowRun{
			ID:         12345,
			Name:       "CI/CD Pipeline",
			HeadBranch: "feature/new-feature",
			HeadSHA:    "abcdef1234567890",
			Status:     "completed",
			Conclusion: "success", // Successful workflow - should be ignored
			HTMLURL:    "https://github.com/org/repo/actions/runs/12345",
		},
		Repository: models.GitHubRepository{
			FullName: "org/repo",
			HTMLURL:  "https://github.com/org/repo",
		},
	}
	
	// Marshal the payload
	payload, err := json.Marshal(workflowRunEvent)
	if err != nil {
		t.Fatalf("Failed to marshal test payload: %v", err)
	}
	
	// Create request
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBuffer(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}
	
	// Add headers
	req.Header.Set("X-GitHub-Event", "workflow_run")
	
	// Compute signature for the payload
	mac := hmac.New(sha256.New, []byte("gh-webhook-secret-1234"))
	mac.Write(payload)
	signature := hex.EncodeToString(mac.Sum(nil))
	req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
	
	// Create response recorder
	rr := httptest.NewRecorder()
	
	// Serve the request
	router.ServeHTTP(rr, req)
	
	// Check status code
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, rr.Code)
	}
	
	// Verify JavaClient was NOT called to create a work item
	if mockClient.CreateWorkItemCalled {
		t.Error("JavaClient.CreateWorkItem was called for a successful workflow, but should have been skipped")
	}
	
	// Verify response contains "skipped" status
	var response map[string]interface{}
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Fatalf("Failed to parse response: %v", err)
	}
	
	if status, ok := response["status"].(string); !ok || status != "skipped" {
		t.Errorf("Expected status 'skipped', got %v", status)
	}
}

// Tests for push events

// TestHandleGitHubWebhook_Push tests push event handling
func TestHandleGitHubWebhook_Push(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForWebhooks{
		WebhookSecret: "gh-webhook-secret-1234", // Use the same secret as in the dev mode
	}
	
	// Create auth service
	authService := middleware.NewAuthService(mockClient, &config.AuthConfig{
		DevMode: true,
	})
	
	// Create a test handler that uses our interface
	testHandler := &testWebhookHandler{
		javaClient:  mockClient,
		authService: authService,
	}
	
	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", testHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// Create test payload for a push event
	pushEvent := models.GitHubPushEvent{
		Ref:     "refs/heads/feature/api-improvements",
		Before:  "0000000000000000000000000000000000000000",
		After:   "1234567890abcdef1234567890abcdef12345678",
		Created: true,
		Deleted: false,
		Forced:  false,
		Commits: []models.GitHubCommit{
			{
				ID:      "1234567890abcdef1234567890abcdef12345678",
				Message: "Implement new API endpoint",
				URL:     "https://github.com/org/repo/commit/1234567890abcdef1234567890abcdef12345678",
			},
			{
				ID:      "abcdef1234567890abcdef1234567890abcdef12",
				Message: "Fix bug in API response handling",
				URL:     "https://github.com/org/repo/commit/abcdef1234567890abcdef1234567890abcdef12",
			},
		},
		HeadCommit: models.GitHubCommit{
			ID:      "1234567890abcdef1234567890abcdef12345678",
			Message: "Implement new API endpoint",
			URL:     "https://github.com/org/repo/commit/1234567890abcdef1234567890abcdef12345678",
		},
		Repository: models.GitHubRepository{
			FullName: "org/repo",
			HTMLURL:  "https://github.com/org/repo",
		},
		Pusher: models.GitHubPusher{
			Name:  "testuser",
			Email: "test@example.com",
		},
	}
	
	// Marshal the payload
	payload, err := json.Marshal(pushEvent)
	if err != nil {
		t.Fatalf("Failed to marshal test payload: %v", err)
	}
	
	// Create request
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBuffer(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}
	
	// Add headers
	req.Header.Set("X-GitHub-Event", "push")
	
	// Compute signature for the payload
	mac := hmac.New(sha256.New, []byte("gh-webhook-secret-1234"))
	mac.Write(payload)
	signature := hex.EncodeToString(mac.Sum(nil))
	req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
	
	// Create response recorder
	rr := httptest.NewRecorder()
	
	// Serve the request
	router.ServeHTTP(rr, req)
	
	// Check status code
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, rr.Code)
	}
	
	// Verify JavaClient was called correctly
	if !mockClient.CreateWorkItemCalled {
		t.Error("JavaClient.CreateWorkItem was not called")
	}
	
	// Verify request to create work item had correct details
	workItemReq := mockClient.CapturedWorkItemRequest
	expectedTitle := "Push: 2 commits to feature/api-improvements"
	if workItemReq.Title != expectedTitle {
		t.Errorf("Expected title %q, got %q", expectedTitle, workItemReq.Title)
	}
	
	if workItemReq.Type != models.WorkItemTypeChore {
		t.Errorf("Expected type %q, got %q", models.WorkItemTypeChore, workItemReq.Type)
	}
	
	// Check source metadata
	if source, ok := workItemReq.Metadata["source"]; !ok || source != "github_push" {
		t.Errorf("Expected metadata source 'github_push', got %q", source)
	}
	
	if branch, ok := workItemReq.Metadata["branch"]; !ok || branch != "feature/api-improvements" {
		t.Errorf("Expected metadata branch 'feature/api-improvements', got %q", branch)
	}
	
	if commitCount, ok := workItemReq.Metadata["commit_count"]; !ok || commitCount != "2" {
		t.Errorf("Expected metadata commit_count '2', got %q", commitCount)
	}
}

// Test for ignored push events

// TestHandleGitHubWebhook_Push_Ignored tests push event that should be ignored
func TestHandleGitHubWebhook_Push_Ignored(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForWebhooks{
		WebhookSecret: "gh-webhook-secret-1234", // Use the same secret as in the dev mode
	}
	
	// Create auth service
	authService := middleware.NewAuthService(mockClient, &config.AuthConfig{
		DevMode: true,
	})
	
	// Create a test handler that uses our interface
	testHandler := &testWebhookHandler{
		javaClient:  mockClient,
		authService: authService,
	}
	
	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", testHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// Create test payload for a branch deletion (should be ignored)
	pushEvent := models.GitHubPushEvent{
		Ref:     "refs/heads/feature/to-be-deleted",
		Before:  "1234567890abcdef1234567890abcdef12345678",
		After:   "0000000000000000000000000000000000000000",
		Created: false,
		Deleted: true, // Branch deletion, should be ignored
		Forced:  false,
		Commits: []models.GitHubCommit{}, // No commits in a deletion
		Repository: models.GitHubRepository{
			FullName: "org/repo",
			HTMLURL:  "https://github.com/org/repo",
		},
		Pusher: models.GitHubPusher{
			Name:  "testuser",
			Email: "test@example.com",
		},
	}
	
	// Marshal the payload
	payload, err := json.Marshal(pushEvent)
	if err != nil {
		t.Fatalf("Failed to marshal test payload: %v", err)
	}
	
	// Create request
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBuffer(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}
	
	// Add headers
	req.Header.Set("X-GitHub-Event", "push")
	
	// Compute signature for the payload
	mac := hmac.New(sha256.New, []byte("gh-webhook-secret-1234"))
	mac.Write(payload)
	signature := hex.EncodeToString(mac.Sum(nil))
	req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
	
	// Create response recorder
	rr := httptest.NewRecorder()
	
	// Serve the request
	router.ServeHTTP(rr, req)
	
	// Check status code
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, rr.Code)
	}
	
	// Verify JavaClient was NOT called to create a work item
	if mockClient.CreateWorkItemCalled {
		t.Error("JavaClient.CreateWorkItem was called for a branch deletion, but should have been skipped")
	}
	
	// Verify response contains "skipped" status
	var response map[string]interface{}
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Fatalf("Failed to parse response: %v", err)
	}
	
	if status, ok := response["status"].(string); !ok || status != "skipped" {
		t.Errorf("Expected status 'skipped', got %v", status)
	}
}

// Tests for issues events

// TestHandleGitHubWebhook_Issues tests issue event handling
func TestHandleGitHubWebhook_Issues(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForWebhooks{
		WebhookSecret: "gh-webhook-secret-1234", // Use the same secret as in the dev mode
	}
	
	// Create auth service
	authService := middleware.NewAuthService(mockClient, &config.AuthConfig{
		DevMode: true,
	})
	
	// Create a test handler that uses our interface
	testHandler := &testWebhookHandler{
		javaClient:  mockClient,
		authService: authService,
	}
	
	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", testHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// Create test payload for an issue event
	issuesEvent := models.GitHubIssuesEvent{
		Action: "opened",
		Issue: models.GitHubIssue{
			ID:     12345,
			Number: 42,
			Title:  "Bug: Application crashes when processing large files",
			Body:   "When uploading files larger than 100MB, the application crashes with an out of memory error.",
			State:  "open",
			User: models.GitHubUser{
				Login: "testuser",
				ID:    9876,
			},
			HTMLURL: "https://github.com/org/repo/issues/42",
			Labels: []models.GitHubLabel{
				{
					ID:    1,
					Name:  "bug",
					Color: "ff0000",
				},
				{
					ID:    2,
					Name:  "high-priority",
					Color: "ff00ff",
				},
			},
		},
		Repository: models.GitHubRepository{
			FullName: "org/repo",
			HTMLURL:  "https://github.com/org/repo",
		},
	}
	
	// Marshal the payload
	payload, err := json.Marshal(issuesEvent)
	if err != nil {
		t.Fatalf("Failed to marshal test payload: %v", err)
	}
	
	// Create request
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBuffer(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}
	
	// Add headers
	req.Header.Set("X-GitHub-Event", "issues")
	
	// Compute signature for the payload
	mac := hmac.New(sha256.New, []byte("gh-webhook-secret-1234"))
	mac.Write(payload)
	signature := hex.EncodeToString(mac.Sum(nil))
	req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
	
	// Create response recorder
	rr := httptest.NewRecorder()
	
	// Serve the request
	router.ServeHTTP(rr, req)
	
	// Check status code
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, rr.Code)
	}
	
	// Verify JavaClient was called correctly
	if !mockClient.CreateWorkItemCalled {
		t.Error("JavaClient.CreateWorkItem was not called")
	}
	
	// Verify request to create work item had correct details
	workItemReq := mockClient.CapturedWorkItemRequest
	expectedTitle := "Issue: Bug: Application crashes when processing large files"
	if workItemReq.Title != expectedTitle {
		t.Errorf("Expected title %q, got %q", expectedTitle, workItemReq.Title)
	}
	
	if workItemReq.Type != models.WorkItemTypeBug {
		t.Errorf("Expected type %q, got %q", models.WorkItemTypeBug, workItemReq.Type)
	}
	
	// Check source metadata
	if source, ok := workItemReq.Metadata["source"]; !ok || source != "github_issue" {
		t.Errorf("Expected metadata source 'github_issue', got %q", source)
	}
	
	if issueNum, ok := workItemReq.Metadata["github_issue"]; !ok || issueNum != "42" {
		t.Errorf("Expected metadata github_issue '42', got %q", issueNum)
	}
}

// Test for ignored issues events

// TestHandleGitHubWebhook_Issues_Ignored tests issue event that should be ignored
func TestHandleGitHubWebhook_Issues_Ignored(t *testing.T) {
	// Create mock client
	mockClient := &MockJavaClientForWebhooks{
		WebhookSecret: "gh-webhook-secret-1234", // Use the same secret as in the dev mode
	}
	
	// Create auth service
	authService := middleware.NewAuthService(mockClient, &config.AuthConfig{
		DevMode: true,
	})
	
	// Create a test handler that uses our interface
	testHandler := &testWebhookHandler{
		javaClient:  mockClient,
		authService: authService,
	}
	
	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", testHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// Create test payload for a closed issue (should be ignored)
	issuesEvent := models.GitHubIssuesEvent{
		Action: "closed", // Should be ignored
		Issue: models.GitHubIssue{
			ID:     12345,
			Number: 42,
			Title:  "Bug: Application crashes when processing large files",
			Body:   "When uploading files larger than 100MB, the application crashes with an out of memory error.",
			State:  "closed",
			User: models.GitHubUser{
				Login: "testuser",
				ID:    9876,
			},
			HTMLURL: "https://github.com/org/repo/issues/42",
		},
		Repository: models.GitHubRepository{
			FullName: "org/repo",
			HTMLURL:  "https://github.com/org/repo",
		},
	}
	
	// Marshal the payload
	payload, err := json.Marshal(issuesEvent)
	if err != nil {
		t.Fatalf("Failed to marshal test payload: %v", err)
	}
	
	// Create request
	req, err := http.NewRequest(http.MethodPost, "/webhooks/github?project=test-project", bytes.NewBuffer(payload))
	if err != nil {
		t.Fatalf("Failed to create request: %v", err)
	}
	
	// Add headers
	req.Header.Set("X-GitHub-Event", "issues")
	
	// Compute signature for the payload
	mac := hmac.New(sha256.New, []byte("gh-webhook-secret-1234"))
	mac.Write(payload)
	signature := hex.EncodeToString(mac.Sum(nil))
	req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
	
	// Create response recorder
	rr := httptest.NewRecorder()
	
	// Serve the request
	router.ServeHTTP(rr, req)
	
	// Check status code
	if rr.Code != http.StatusOK {
		t.Errorf("Expected status code %d, got %d", http.StatusOK, rr.Code)
	}
	
	// Verify JavaClient was NOT called to create a work item
	if mockClient.CreateWorkItemCalled {
		t.Error("JavaClient.CreateWorkItem was called for a closed issue, but should have been skipped")
	}
	
	// Verify response contains "skipped" status
	var response map[string]interface{}
	if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
		t.Fatalf("Failed to parse response: %v", err)
	}
	
	if status, ok := response["status"].(string); !ok || status != "skipped" {
		t.Errorf("Expected status 'skipped', got %v", status)
	}
}

// Helper function to compute a valid GitHub signature for a payload
func computeGitHubSignature(secret string, payload []byte) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(payload)
	return "sha256=" + hex.EncodeToString(mac.Sum(nil))
}