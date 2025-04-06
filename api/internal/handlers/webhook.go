/*
 * Webhook handlers for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/client"
	"github.com/heymumford/rinna/api/internal/middleware"
	"github.com/heymumford/rinna/api/internal/models"
)

// WebhookHandler handles webhook-related requests
type WebhookHandler struct {
	javaClient  *client.JavaClient
	authService *middleware.AuthService
}

// NewWebhookHandler creates a new webhook handler
func NewWebhookHandler(javaClient *client.JavaClient, authService *middleware.AuthService) *WebhookHandler {
	return &WebhookHandler{
		javaClient:  javaClient,
		authService: authService,
	}
}

// RegisterWebhookRoutes registers webhook-related routes
func RegisterWebhookRoutes(router *mux.Router, javaClient *client.JavaClient, authService *middleware.AuthService) {
	// Create handler with dependencies
	handler := NewWebhookHandler(javaClient, authService)

	// Register routes
	router.HandleFunc("/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost) // For backward compatibility
	router.HandleFunc("/api/v1/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost) // New API path
}

// HandleGitHubWebhook handles GitHub webhook requests
func (h *WebhookHandler) HandleGitHubWebhook(w http.ResponseWriter, r *http.Request) {
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

	// Validate the signature with the auth service
	if err := h.authService.ValidateWebhookSignature(r.Context(), projectKey, "github", signature, body); err != nil {
		http.Error(w, "Invalid webhook signature: "+err.Error(), http.StatusUnauthorized)
		return
	}

	// Parse the payload based on the event type
	var response interface{}
	switch eventType {
	case "pull_request":
		response, err = h.handlePullRequestEvent(projectKey, body)
	case "workflow_run":
		response, err = h.handleWorkflowRunEvent(projectKey, body)
	case "push":
		response, err = h.handlePushEvent(projectKey, body)
	case "issues":
		response, err = h.handleIssuesEvent(projectKey, body)
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

	if err != nil {
		http.Error(w, "Failed to handle webhook: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return the response
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// handlePullRequestEvent handles GitHub pull request events
func (h *WebhookHandler) handlePullRequestEvent(projectKey string, payload []byte) (interface{}, error) {
	// Parse the payload
	var pr models.GitHubPullRequestEvent
	if err := json.Unmarshal(payload, &pr); err != nil {
		return nil, fmt.Errorf("failed to parse pull request payload: %w", err)
	}

	// Only handle opened or reopened pull requests
	if pr.Action != "opened" && pr.Action != "reopened" {
		return map[string]string{
			"status": "skipped",
			"message": fmt.Sprintf("Ignoring pull request action: %s", pr.Action),
		}, nil
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
	workItem, err := h.javaClient.CreateWorkItem(context.Background(), workItemReq)
	if err != nil {
		return nil, fmt.Errorf("failed to create work item: %w", err)
	}

	return map[string]interface{}{
		"status": "created",
		"workItem": workItem,
	}, nil
}

// handleWorkflowRunEvent handles GitHub workflow run events
func (h *WebhookHandler) handleWorkflowRunEvent(projectKey string, payload []byte) (interface{}, error) {
	// Parse the payload
	var wf models.GitHubWorkflowRunEvent
	if err := json.Unmarshal(payload, &wf); err != nil {
		return nil, fmt.Errorf("failed to parse workflow run payload: %w", err)
	}

	// Only handle completed workflow runs with failure conclusion
	if wf.Action != "completed" || wf.WorkflowRun.Conclusion != "failure" {
		return map[string]string{
			"status": "skipped",
			"message": "Ignoring non-failed workflow run",
		}, nil
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
	workItem, err := h.javaClient.CreateWorkItem(context.Background(), workItemReq)
	if err != nil {
		return nil, fmt.Errorf("failed to create work item: %w", err)
	}

	return map[string]interface{}{
		"status": "created",
		"workItem": workItem,
	}, nil
}

// handlePushEvent handles GitHub push events
func (h *WebhookHandler) handlePushEvent(projectKey string, payload []byte) (interface{}, error) {
	// Parse the payload
	var push models.GitHubPushEvent
	if err := json.Unmarshal(payload, &push); err != nil {
		return nil, fmt.Errorf("failed to parse push payload: %w", err)
	}

	// Skip if it's a delete operation or has no commits
	if push.Deleted || len(push.Commits) == 0 {
		return map[string]string{
			"status": "skipped",
			"message": "Skipping delete operation or push with no commits",
		}, nil
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
	workItem, err := h.javaClient.CreateWorkItem(context.Background(), workItemReq)
	if err != nil {
		return nil, fmt.Errorf("failed to create work item: %w", err)
	}

	return map[string]interface{}{
		"status": "created",
		"workItem": workItem,
	}, nil
}

// handleIssuesEvent handles GitHub issues events
func (h *WebhookHandler) handleIssuesEvent(projectKey string, payload []byte) (interface{}, error) {
	// Parse the payload
	var issues models.GitHubIssuesEvent
	if err := json.Unmarshal(payload, &issues); err != nil {
		return nil, fmt.Errorf("failed to parse issues payload: %w", err)
	}

	// Only handle opened or reopened issues
	if issues.Action != "opened" && issues.Action != "reopened" {
		return map[string]string{
			"status": "skipped",
			"message": fmt.Sprintf("Ignoring issue action: %s", issues.Action),
		}, nil
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
	workItem, err := h.javaClient.CreateWorkItem(context.Background(), workItemReq)
	if err != nil {
		return nil, fmt.Errorf("failed to create work item: %w", err)
	}

	return map[string]interface{}{
		"status": "created",
		"workItem": workItem,
	}, nil
}

