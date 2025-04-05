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
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
	
	"github.com/heymumford/rinna/api/internal/models"

	"github.com/gorilla/mux"
)

func TestHandleGitHubWebhook(t *testing.T) {
	// Create a new webhook handler
	handler := &WebhookHandler{}

	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", handler.HandleGitHubWebhook).Methods(http.MethodPost)

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
		{
			name:           "Successful ping event",
			projectKey:     "test-project",
			eventType:      "ping",
			signature:      "", // Will be computed during the test
			payload:        `{"zen": "Test ping"}`,
			expectedStatus: http.StatusOK,
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

			// Compute signature if needed
			if tc.signature == "" && tc.expectedStatus == http.StatusOK {
				// Compute the HMAC
				secret := "gh-webhook-secret-1234"
				mac := hmac.New(sha256.New, []byte(secret))
				mac.Write([]byte(tc.payload))
				signature := hex.EncodeToString(mac.Sum(nil))
				req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
			} else {
				req.Header.Set("X-Hub-Signature-256", tc.signature)
			}

			// Create response recorder
			rr := httptest.NewRecorder()

			// Serve the request
			router.ServeHTTP(rr, req)

			// Check status code
			if rr.Code != tc.expectedStatus {
				t.Errorf("Expected status code %d, got %d", tc.expectedStatus, rr.Code)
			}

			// If success, check response
			if tc.expectedStatus == http.StatusOK {
				var response map[string]interface{}
				if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
					t.Fatalf("Failed to parse response: %v", err)
				}

				// Check status in response
				if status, ok := response["status"].(string); !ok || status != "ok" {
					t.Errorf("Expected status 'ok', got %v", status)
				}
			}
		})
	}
}

func TestHandlePullRequestEvent(t *testing.T) {
	// Create a new webhook handler
	handler := &WebhookHandler{}

	// Test pull request event handling
	payload := []byte(`{
		"action": "opened",
		"pull_request": {
			"title": "Add feature X",
			"body": "This PR adds feature X",
			"user": {
				"login": "testuser"
			},
			"number": 42,
			"html_url": "https://github.com/test/repo/pull/42"
		},
		"repository": {
			"full_name": "test/repo"
		}
	}`)

	// Call the handler function
	result, err := handler.handlePullRequestEvent("test-project", payload)
	if err != nil {
		t.Fatalf("Failed to handle pull request event: %v", err)
	}

	// Check the result
	resultMap, ok := result.(map[string]interface{})
	if !ok {
		t.Fatalf("Expected map result, got %T", result)
	}

	// Check status
	if status, ok := resultMap["status"].(string); !ok || status != "created" {
		t.Errorf("Expected status 'created', got %v", status)
	}

	// Check work item exists
	workItemValue, exists := resultMap["workItem"]
	if !exists {
		t.Fatalf("Expected workItem in result, got %v", resultMap)
	}
	
	// We can use the map directly for testing since we don't need to manipulate the object
	var title, itemType string
	
	// Try to access as struct or map
	workItemObj, objOk := workItemValue.(*models.WorkItem)
	if objOk {
		title = workItemObj.Title
		itemType = string(workItemObj.Type)
	} else {
		// Try as map
		workItemMap, mapOk := workItemValue.(map[string]interface{})
		if !mapOk {
			t.Fatalf("Expected workItem to be a *models.WorkItem or map, got %T", workItemValue)
			return
		}
		
		// Access map values
		title = workItemMap["title"].(string)
		itemType = workItemMap["type"].(string)
	}
	
	// Check work item title
	if title != "PR: Add feature X" {
		t.Errorf("Expected title 'PR: Add feature X', got %v", title)
	}

	// Check work item type
	if itemType != "FEATURE" {
		t.Errorf("Expected type 'FEATURE', got %v", itemType)
	}
}

func TestHandleWorkflowRunEvent(t *testing.T) {
	// Create a new webhook handler
	handler := &WebhookHandler{}

	// Test workflow run event handling
	payload := []byte(`{
		"action": "completed",
		"workflow_run": {
			"name": "CI Pipeline",
			"head_branch": "feature/branch",
			"conclusion": "failure",
			"html_url": "https://github.com/test/repo/actions/runs/123456"
		},
		"repository": {
			"full_name": "test/repo"
		}
	}`)

	// Call the handler function
	result, err := handler.handleWorkflowRunEvent("test-project", payload)
	if err != nil {
		t.Fatalf("Failed to handle workflow run event: %v", err)
	}

	// Check the result
	resultMap, ok := result.(map[string]interface{})
	if !ok {
		t.Fatalf("Expected map result, got %T", result)
	}

	// Check status
	if status, ok := resultMap["status"].(string); !ok || status != "created" {
		t.Errorf("Expected status 'created', got %v", status)
	}

	// Check work item exists
	workItemValue, exists := resultMap["workItem"]
	if !exists {
		t.Fatalf("Expected workItem in result, got %v", resultMap)
	}
	
	// We can use the map directly for testing since we don't need to manipulate the object
	var title, itemType, priority string
	
	// Try to access as struct or map
	workItemObj, objOk := workItemValue.(*models.WorkItem)
	if objOk {
		title = workItemObj.Title
		itemType = string(workItemObj.Type)
		priority = string(workItemObj.Priority)
	} else {
		// Try as map
		workItemMap, mapOk := workItemValue.(map[string]interface{})
		if !mapOk {
			t.Fatalf("Expected workItem to be a *models.WorkItem or map, got %T", workItemValue)
			return
		}
		
		// Access map values
		title = workItemMap["title"].(string)
		itemType = workItemMap["type"].(string)
		priority = workItemMap["priority"].(string)
	}
	
	// Check work item title
	expectedTitle := "CI Failure: CI Pipeline on feature/branch"
	if title != expectedTitle {
		t.Errorf("Expected title '%s', got %v", expectedTitle, title)
	}

	// Check work item type
	if itemType != "BUG" {
		t.Errorf("Expected type 'BUG', got %v", itemType)
	}

	// Check work item priority
	if priority != "HIGH" {
		t.Errorf("Expected priority 'HIGH', got %v", priority)
	}
}