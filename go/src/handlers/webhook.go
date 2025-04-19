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
	"time"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/go/src/client"
	"github.com/heymumford/rinna/go/src/middleware"
	"github.com/heymumford/rinna/go/src/models"
	"github.com/heymumford/rinna/go/pkg/config"
	"github.com/heymumford/rinna/go/pkg/logger"
)

// WebhookHandler handles webhook-related requests
type WebhookHandler struct {
	javaClient        *client.JavaClient
	securityService   *middleware.WebhookSecurityService
	config            *config.RinnaConfig
	// Track received webhooks for idempotency
	processedEvents   map[string]time.Time
}

// NewWebhookHandler creates a new webhook handler
func NewWebhookHandler(javaClient *client.JavaClient, securityService *middleware.WebhookSecurityService, config *config.RinnaConfig) *WebhookHandler {
	return &WebhookHandler{
		javaClient:      javaClient,
		securityService: securityService,
		config:          config,
		processedEvents: make(map[string]time.Time),
	}
}

// RegisterWebhookRoutes registers webhook-related routes
func RegisterWebhookRoutes(router *mux.Router, javaClient *client.JavaClient, securityService *middleware.WebhookSecurityService, config *config.RinnaConfig) {
	// Create handler with dependencies
	handler := NewWebhookHandler(javaClient, securityService, config)

	// Webhook endpoints - use the same handler with different URL patterns
	webhooksRouter := router.PathPrefix("/api/v1/webhooks").Subrouter()
	
	// GitHub webhooks
	webhooksRouter.HandleFunc("/github", handler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// GitLab webhooks
	webhooksRouter.HandleFunc("/gitlab", handler.HandleGitLabWebhook).Methods(http.MethodPost)
	
	// Bitbucket webhooks
	webhooksRouter.HandleFunc("/bitbucket", handler.HandleBitbucketWebhook).Methods(http.MethodPost)
	
	// Custom webhooks with ID parameter
	webhooksRouter.HandleFunc("/custom/{id}", handler.HandleCustomWebhook).Methods(http.MethodPost)
	
	// Legacy routes for backward compatibility
	router.HandleFunc("/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost)
}

// HandleGitHubWebhook handles GitHub webhook requests
func (h *WebhookHandler) HandleGitHubWebhook(w http.ResponseWriter, r *http.Request) {
	start := time.Now()

	// Get request ID from security middleware
	requestID := r.Header.Get("X-Request-ID")
	if requestID == "" {
		requestID = fmt.Sprintf("webhook-%d", time.Now().UnixNano())
	}

	// Create a logger with context fields
	log := logger.WithFields(map[string]interface{}{
		"requestID": requestID,
		"source":    "github",
		"path":      r.URL.Path,
	})

	log.Info("Processing GitHub webhook")

	// Get the project key from the context or query parameter
	projectKey := middleware.GetProjectID(r.Context())
	if projectKey == "" {
		projectKey = r.URL.Query().Get("project")
	}
	
	log = log.WithField("project", projectKey)
	
	if projectKey == "" {
		log.Warn("Missing project key in webhook request")
		http.Error(w, "Project key is required", http.StatusBadRequest)
		return
	}

	// Get the event type
	eventType := r.Header.Get("X-GitHub-Event")
	log = log.WithField("eventType", eventType)
	
	if eventType == "" {
		log.Warn("Missing event type in webhook request")
		http.Error(w, "Missing event type", http.StatusBadRequest)
		return
	}

	// Check for duplicate events using delivery ID
	eventID := r.Header.Get("X-GitHub-Delivery")
	if eventID != "" {
		// Check if we've already processed this event
		if _, exists := h.processedEvents[eventID]; exists {
			log.WithField("eventID", eventID).Info("Ignoring duplicate webhook event")
			w.WriteHeader(http.StatusOK)
			json.NewEncoder(w).Encode(map[string]string{
				"status":  "ignored",
				"message": "Duplicate event",
			})
			return
		}
		
		// Mark this event as processed
		h.processedEvents[eventID] = time.Now()
		
		// Clean up old events (keep map size reasonable)
		cleanupTime := time.Now().Add(-24 * time.Hour)
		for id, t := range h.processedEvents {
			if t.Before(cleanupTime) {
				delete(h.processedEvents, id)
			}
		}
	}

	// Read the request body
	body, err := io.ReadAll(r.Body)
	if err != nil {
		log.WithField("error", err).Error("Failed to read webhook request body")
		http.Error(w, "Failed to read request body", http.StatusInternalServerError)
		return
	}

	// Log payload size for debugging
	log = log.WithField("payloadSize", len(body))

	// Process specific event
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
			"status":  "ok",
			"message": "Webhook received successfully",
		}
	default:
		log.WithField("eventType", eventType).Info("Unsupported event type")
		http.Error(w, "Unsupported event type: "+eventType, http.StatusBadRequest)
		return
	}

	if err != nil {
		log.WithField("error", err).Error("Failed to handle webhook")
		http.Error(w, "Failed to handle webhook: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Calculate processing time
	duration := time.Since(start)
	
	// Log the result
	log.WithFields(map[string]interface{}{
		"duration": duration.String(),
		"status":   "success",
	}).Info("Webhook processed successfully")
	
	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("X-Request-ID", requestID)
	w.Header().Set("X-Processing-Time", duration.String())
	
	// Add cache control headers to prevent caching
	w.Header().Set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
	w.Header().Set("Pragma", "no-cache")
	w.Header().Set("Expires", "0")
	
	json.NewEncoder(w).Encode(response)
}

// HandleGitLabWebhook handles GitLab webhook requests
func (h *WebhookHandler) HandleGitLabWebhook(w http.ResponseWriter, r *http.Request) {
	start := time.Now()

	// Get request ID from security middleware
	requestID := r.Header.Get("X-Request-ID")
	if requestID == "" {
		requestID = fmt.Sprintf("webhook-%d", time.Now().UnixNano())
	}

	// Create a logger with context fields
	log := logger.WithFields(map[string]interface{}{
		"requestID": requestID,
		"source":    "gitlab",
		"path":      r.URL.Path,
	})

	log.Info("Processing GitLab webhook")

	// Get the project key from the context or query parameter
	projectKey := middleware.GetProjectID(r.Context())
	if projectKey == "" {
		projectKey = r.URL.Query().Get("project")
	}
	
	log = log.WithField("project", projectKey)
	
	if projectKey == "" {
		log.Warn("Missing project key in webhook request")
		http.Error(w, "Project key is required", http.StatusBadRequest)
		return
	}

	// Get the event type
	eventType := r.Header.Get("X-Gitlab-Event")
	log = log.WithField("eventType", eventType)
	
	if eventType == "" {
		log.Warn("Missing event type in webhook request")
		http.Error(w, "Missing event type", http.StatusBadRequest)
		return
	}

	// Check for duplicate events using event UUID
	eventID := r.Header.Get("X-Gitlab-Event-UUID")
	if eventID != "" {
		// Check if we've already processed this event
		if _, exists := h.processedEvents[eventID]; exists {
			log.WithField("eventID", eventID).Info("Ignoring duplicate webhook event")
			w.WriteHeader(http.StatusOK)
			json.NewEncoder(w).Encode(map[string]string{
				"status":  "ignored",
				"message": "Duplicate event",
			})
			return
		}
		
		// Mark this event as processed
		h.processedEvents[eventID] = time.Now()
	}

	// Read the request body
	body, err := io.ReadAll(r.Body)
	if err != nil {
		log.WithField("error", err).Error("Failed to read webhook request body")
		http.Error(w, "Failed to read request body", http.StatusInternalServerError)
		return
	}

	// Return simple success response for now
	// TODO: Add GitLab webhook event parsing and handling
	response := map[string]string{
		"status":  "received",
		"message": "GitLab webhook received successfully - implementation pending",
	}

	// Calculate processing time
	duration := time.Since(start)
	
	// Log the result
	log.WithFields(map[string]interface{}{
		"duration": duration.String(),
		"status":   "success",
	}).Info("Webhook received")
	
	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("X-Request-ID", requestID)
	w.Header().Set("X-Processing-Time", duration.String())
	json.NewEncoder(w).Encode(response)
}

// HandleBitbucketWebhook handles Bitbucket webhook requests
func (h *WebhookHandler) HandleBitbucketWebhook(w http.ResponseWriter, r *http.Request) {
	start := time.Now()

	// Get request ID from security middleware
	requestID := r.Header.Get("X-Request-ID")
	if requestID == "" {
		requestID = fmt.Sprintf("webhook-%d", time.Now().UnixNano())
	}

	// Create a logger with context fields
	log := logger.WithFields(map[string]interface{}{
		"requestID": requestID,
		"source":    "bitbucket",
		"path":      r.URL.Path,
	})

	log.Info("Processing Bitbucket webhook")

	// Get the project key from the context or query parameter
	projectKey := middleware.GetProjectID(r.Context())
	if projectKey == "" {
		projectKey = r.URL.Query().Get("project")
	}
	
	log = log.WithField("project", projectKey)
	
	if projectKey == "" {
		log.Warn("Missing project key in webhook request")
		http.Error(w, "Project key is required", http.StatusBadRequest)
		return
	}

	// Get the event type
	eventType := r.Header.Get("X-Event-Key")
	log = log.WithField("eventType", eventType)
	
	if eventType == "" {
		log.Warn("Missing event type in webhook request")
		http.Error(w, "Missing event type", http.StatusBadRequest)
		return
	}

	// Check for duplicate events using request UUID
	eventID := r.Header.Get("X-Request-UUID")
	if eventID != "" {
		// Check if we've already processed this event
		if _, exists := h.processedEvents[eventID]; exists {
			log.WithField("eventID", eventID).Info("Ignoring duplicate webhook event")
			w.WriteHeader(http.StatusOK)
			json.NewEncoder(w).Encode(map[string]string{
				"status":  "ignored",
				"message": "Duplicate event",
			})
			return
		}
		
		// Mark this event as processed
		h.processedEvents[eventID] = time.Now()
	}

	// Read the request body
	body, err := io.ReadAll(r.Body)
	if err != nil {
		log.WithField("error", err).Error("Failed to read webhook request body")
		http.Error(w, "Failed to read request body", http.StatusInternalServerError)
		return
	}

	// Return simple success response for now
	// TODO: Add Bitbucket webhook event parsing and handling
	response := map[string]string{
		"status":  "received",
		"message": "Bitbucket webhook received successfully - implementation pending",
	}

	// Calculate processing time
	duration := time.Since(start)
	
	// Log the result
	log.WithFields(map[string]interface{}{
		"duration": duration.String(),
		"status":   "success",
	}).Info("Webhook received")
	
	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("X-Request-ID", requestID)
	w.Header().Set("X-Processing-Time", duration.String())
	json.NewEncoder(w).Encode(response)
}

// HandleCustomWebhook handles custom webhook requests
func (h *WebhookHandler) HandleCustomWebhook(w http.ResponseWriter, r *http.Request) {
	start := time.Now()

	// Get the custom webhook ID from the URL
	vars := mux.Vars(r)
	webhookID := vars["id"]

	// Get request ID from security middleware
	requestID := r.Header.Get("X-Request-ID")
	if requestID == "" {
		requestID = fmt.Sprintf("webhook-%d", time.Now().UnixNano())
	}

	// Create a logger with context fields
	log := logger.WithFields(map[string]interface{}{
		"requestID": requestID,
		"source":    "custom",
		"webhookID": webhookID,
		"path":      r.URL.Path,
	})

	log.Info("Processing custom webhook")

	// Get the project key from the context or query parameter
	projectKey := middleware.GetProjectID(r.Context())
	if projectKey == "" {
		projectKey = r.URL.Query().Get("project")
	}
	
	log = log.WithField("project", projectKey)
	
	if projectKey == "" {
		log.Warn("Missing project key in webhook request")
		http.Error(w, "Project key is required", http.StatusBadRequest)
		return
	}

	// Get the event type
	eventType := r.Header.Get("X-Webhook-Event")
	if eventType == "" {
		eventType = "default" // Default event type for custom webhooks
	}
	log = log.WithField("eventType", eventType)

	// Check for duplicate events using event ID
	eventID := r.Header.Get("X-Webhook-Nonce")
	if eventID != "" {
		// Check if we've already processed this event
		if _, exists := h.processedEvents[eventID]; exists {
			log.WithField("eventID", eventID).Info("Ignoring duplicate webhook event")
			w.WriteHeader(http.StatusOK)
			json.NewEncoder(w).Encode(map[string]string{
				"status":  "ignored",
				"message": "Duplicate event",
			})
			return
		}
		
		// Mark this event as processed
		h.processedEvents[eventID] = time.Now()
	}

	// Read the request body
	body, err := io.ReadAll(r.Body)
	if err != nil {
		log.WithField("error", err).Error("Failed to read webhook request body")
		http.Error(w, "Failed to read request body", http.StatusInternalServerError)
		return
	}

	// Return simple success response for now
	response := map[string]string{
		"status":     "received",
		"message":    "Custom webhook received successfully",
		"webhook_id": webhookID,
		"event_type": eventType,
	}

	// Calculate processing time
	duration := time.Since(start)
	
	// Log the result
	log.WithFields(map[string]interface{}{
		"duration": duration.String(),
		"status":   "success",
	}).Info("Webhook received")
	
	// Return the response
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("X-Request-ID", requestID)
	w.Header().Set("X-Processing-Time", duration.String())
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