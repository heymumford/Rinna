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
	
	"github.com/heymumford/rinna/api/internal/middleware"
	"github.com/heymumford/rinna/api/pkg/config"

	"github.com/gorilla/mux"
)

// Simple test to verify the webhook handler correctly checks project keys and signatures
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

// Simple test for the ping event which doesn't require the JavaClient
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

// Test that webhook handler correctly handles invalid signatures
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