/*
 * Functional tests for webhook handlers in the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package test

import (
	"bytes"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/handlers"
	"github.com/heymumford/rinna/api/internal/middleware"
	"github.com/heymumford/rinna/api/pkg/config"
)


// TestWebhookHandlerFunctional is a functional test that tests the integration
// of the webhook handler with the auth service
func TestWebhookHandlerFunctional(t *testing.T) {
	// Create auth service with dev mode config
	authConfig := &config.AuthConfig{
		DevMode:      true,
		TokenExpiry:  60,
		SecretExpiry: 60,
	}
	authService := middleware.NewAuthService(nil, authConfig)

	// Create a mock JavaClient with overridden methods
	mockJavaClient := NewMockJavaClient()

	// Create webhook handler using the constructor
	// Using the underlying JavaClient field from the mock
	webhookHandler := handlers.NewWebhookHandler(mockJavaClient.JavaClient, authService)

	// Set up router
	router := mux.NewRouter()
	router.HandleFunc("/webhooks/github", webhookHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	router.HandleFunc("/api/v1/webhooks/github", webhookHandler.HandleGitHubWebhook).Methods(http.MethodPost)
	
	// Also register the webhook middleware to test the full path
	apiRouter := router.PathPrefix("/api/v1").Subrouter()
	apiRouter.Use(middleware.WebhookAuthentication(authService))

	// Test cases
	testCases := []struct {
		name           string
		path           string
		projectKey     string
		eventType      string
		signature      string // Will be calculated for valid tests
		payload        string
		expectedStatus int
		invalidSig     bool // If true, an invalid signature will be used
	}{
		{
			name:           "Valid ping on legacy path",
			path:           "/webhooks/github",
			projectKey:     "test-project",
			eventType:      "ping",
			payload:        `{"zen": "Test ping", "hook_id": 123456}`,
			expectedStatus: http.StatusOK,
		},
		{
			name:           "Valid ping on API path",
			path:           "/api/v1/webhooks/github",
			projectKey:     "test-project",
			eventType:      "ping",
			payload:        `{"zen": "Test ping", "hook_id": 123456}`,
			expectedStatus: http.StatusOK,
		},
		{
			name:           "Missing project key on legacy path",
			path:           "/webhooks/github",
			projectKey:     "",
			eventType:      "ping",
			payload:        `{"zen": "Test ping", "hook_id": 123456}`,
			expectedStatus: http.StatusBadRequest,
		},
		{
			name:           "Missing project key on API path",
			path:           "/api/v1/webhooks/github",
			projectKey:     "",
			eventType:      "ping",
			payload:        `{"zen": "Test ping", "hook_id": 123456}`,
			expectedStatus: http.StatusBadRequest,
		},
		{
			name:           "Missing event type on legacy path",
			path:           "/webhooks/github",
			projectKey:     "test-project",
			eventType:      "",
			payload:        `{"zen": "Test ping", "hook_id": 123456}`,
			expectedStatus: http.StatusBadRequest,
		},
		{
			name:           "Invalid signature on legacy path",
			path:           "/webhooks/github",
			projectKey:     "test-project",
			eventType:      "ping",
			payload:        `{"zen": "Test ping", "hook_id": 123456}`,
			expectedStatus: http.StatusUnauthorized,
			invalidSig:     true,
		},
		{
			name:           "Invalid signature on API path",
			path:           "/api/v1/webhooks/github",
			projectKey:     "test-project",
			eventType:      "ping",
			payload:        `{"zen": "Test ping", "hook_id": 123456}`,
			expectedStatus: http.StatusUnauthorized,
			invalidSig:     true,
		},
	}

	// Run test cases
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			// Create request
			url := tc.path
			if tc.projectKey != "" {
				url += "?project=" + tc.projectKey
			}
			
			req, err := http.NewRequest(http.MethodPost, url, bytes.NewBufferString(tc.payload))
			if err != nil {
				t.Fatalf("Failed to create request: %v", err)
			}

			// Add headers
			if tc.eventType != "" {
				req.Header.Set("X-GitHub-Event", tc.eventType)
			}

			// Add signature
			if tc.invalidSig {
				req.Header.Set("X-Hub-Signature-256", "sha256=invalid-signature")
			} else {
				// Compute the signature
				secret := "gh-webhook-secret-1234" // Default dev mode secret
				mac := hmac.New(sha256.New, []byte(secret))
				mac.Write([]byte(tc.payload))
				signature := hex.EncodeToString(mac.Sum(nil))
				req.Header.Set("X-Hub-Signature-256", "sha256="+signature)
			}

			// Set content type
			req.Header.Set("Content-Type", "application/json")

			// Create response recorder
			rr := httptest.NewRecorder()

			// Serve the request
			router.ServeHTTP(rr, req)

			// Check status code
			if rr.Code != tc.expectedStatus {
				t.Errorf("Expected status code %d, got %d", tc.expectedStatus, rr.Code)
				
				// Provide more debugging info
				t.Logf("Response body: %s", rr.Body.String())
			}

			// If expecting success, check the response
			if tc.expectedStatus == http.StatusOK {
				var response map[string]interface{}
				if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
					t.Errorf("Failed to parse response: %v", err)
					return
				}

				// For ping events, check the status
				if tc.eventType == "ping" {
					status, ok := response["status"].(string)
					if !ok || status != "ok" {
						t.Errorf("Expected status 'ok', got %v", status)
					}
				}

				// For pull_request and workflow_run events, check that a work item was "created"
				// (we're not actually creating it since we don't have a real Java client)
				if tc.eventType == "pull_request" || tc.eventType == "workflow_run" {
					status, ok := response["status"].(string)
					if !ok {
						t.Errorf("Response missing 'status' field")
					} else if status != "created" && status != "skipped" {
						t.Errorf("Expected status 'created' or 'skipped', got %v", status)
					}
				}
			}
		})
	}
}