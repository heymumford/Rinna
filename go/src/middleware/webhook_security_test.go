/*
 * Tests for the webhook security middleware
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package middleware

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/heymumford/rinna/api/pkg/config"
)

// MockJavaClient implements the JavaClientInterface for testing
type MockJavaClient struct {
	GetWebhookSecretFunc func(ctx context.Context, projectKey, source string) (string, error)
	ValidateTokenFunc    func(ctx context.Context, token string) (string, error)
}

func (m *MockJavaClient) GetWebhookSecret(ctx context.Context, projectKey, source string) (string, error) {
	if m.GetWebhookSecretFunc != nil {
		return m.GetWebhookSecretFunc(ctx, projectKey, source)
	}
	return "test-secret", nil
}

func (m *MockJavaClient) ValidateToken(ctx context.Context, token string) (string, error) {
	if m.ValidateTokenFunc != nil {
		return m.ValidateTokenFunc(ctx, token)
	}
	return "test-project", nil
}

func TestWebhookSecurityMiddleware(t *testing.T) {
	// Create a test configuration
	cfg := &config.AuthConfig{
		DevMode:             false,
		SecretExpiry:        60,
		WebhookSecretExpiry: 1440,
		AllowedSources:      []string{"github", "gitlab", "bitbucket", "custom"},
	}

	// Create a mock Java client
	mockClient := &MockJavaClient{}

	// Create a webhook security service
	securityService := NewWebhookSecurityService(mockClient, cfg)

	// Create a test handler that always succeeds
	testHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("success"))
	})

	// Apply the webhook security middleware
	handler := WebhookSecurityMiddleware(securityService)(testHandler)

	// Test cases
	tests := []struct {
		name           string
		path           string
		projectKey     string
		source         string
		payload        string
		headers        map[string]string
		secretFunc     func(ctx context.Context, projectKey, source string) (string, error)
		expectedStatus int
	}{
		{
			name:           "Valid GitHub webhook",
			path:           "/api/v1/webhooks/github",
			projectKey:     "test-project",
			source:         "github",
			payload:        `{"test":"payload"}`,
			headers: map[string]string{
				"X-GitHub-Event":       "push",
				"X-GitHub-Delivery":    "test-delivery-id",
				"X-Hub-Signature-256":  "", // To be computed
			},
			secretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
				return "test-secret", nil
			},
			expectedStatus: http.StatusOK,
		},
		{
			name:           "Invalid GitHub signature",
			path:           "/api/v1/webhooks/github",
			projectKey:     "test-project",
			source:         "github",
			payload:        `{"test":"payload"}`,
			headers: map[string]string{
				"X-GitHub-Event":       "push",
				"X-GitHub-Delivery":    "test-delivery-id",
				"X-Hub-Signature-256":  "sha256=invalid",
			},
			secretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
				return "test-secret", nil
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name:           "Missing project key",
			path:           "/api/v1/webhooks/github",
			projectKey:     "",
			source:         "github",
			payload:        `{"test":"payload"}`,
			headers: map[string]string{
				"X-GitHub-Event":       "push",
				"X-GitHub-Delivery":    "test-delivery-id",
				"X-Hub-Signature-256":  "", // To be computed
			},
			secretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
				return "test-secret", nil
			},
			expectedStatus: http.StatusBadRequest,
		},
		{
			name:           "Valid GitLab webhook",
			path:           "/api/v1/webhooks/gitlab",
			projectKey:     "test-project",
			source:         "gitlab",
			payload:        `{"test":"payload"}`,
			headers: map[string]string{
				"X-Gitlab-Event":      "Push Hook",
				"X-Gitlab-Event-UUID": "test-event-uuid",
				"X-Gitlab-Token":      "test-secret",
			},
			secretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
				return "test-secret", nil
			},
			expectedStatus: http.StatusOK,
		},
		{
			name:           "Invalid GitLab token",
			path:           "/api/v1/webhooks/gitlab",
			projectKey:     "test-project",
			source:         "gitlab",
			payload:        `{"test":"payload"}`,
			headers: map[string]string{
				"X-Gitlab-Event":      "Push Hook",
				"X-Gitlab-Event-UUID": "test-event-uuid",
				"X-Gitlab-Token":      "invalid-token",
			},
			secretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
				return "test-secret", nil
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name:           "Valid Custom webhook",
			path:           "/api/v1/webhooks/custom/test-id",
			projectKey:     "test-project",
			source:         "custom",
			payload:        `{"test":"payload"}`,
			headers: map[string]string{
				"X-Webhook-Event":     "custom-event",
				"X-Webhook-Nonce":     "test-nonce",
				"X-Webhook-Algorithm": "sha256",
				"X-Webhook-Signature": "", // To be computed
			},
			secretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
				return "test-secret", nil
			},
			expectedStatus: http.StatusOK,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Set up the mock client
			mockClient.GetWebhookSecretFunc = tt.secretFunc

			// Create a request
			req, err := http.NewRequest("POST", tt.path, bytes.NewBufferString(tt.payload))
			if err != nil {
				t.Fatalf("Failed to create request: %v", err)
			}

			// Add query parameters
			q := req.URL.Query()
			if tt.projectKey != "" {
				q.Add("project", tt.projectKey)
			}
			req.URL.RawQuery = q.Encode()

			// Add headers
			for k, v := range tt.headers {
				if k == "X-Hub-Signature-256" && v == "" {
					// Compute the signature
					mac := hmac.New(sha256.New, []byte("test-secret"))
					mac.Write([]byte(tt.payload))
					signature := "sha256=" + hex.EncodeToString(mac.Sum(nil))
					req.Header.Set(k, signature)
				} else if k == "X-Webhook-Signature" && v == "" {
					// Compute the signature
					mac := hmac.New(sha256.New, []byte("test-secret"))
					mac.Write([]byte(tt.payload))
					signature := hex.EncodeToString(mac.Sum(nil))
					req.Header.Set(k, signature)
				} else {
					req.Header.Set(k, v)
				}
			}

			// Create a response recorder
			rr := httptest.NewRecorder()

			// Serve the request
			handler.ServeHTTP(rr, req)

			// Check the status code
			if rr.Code != tt.expectedStatus {
				t.Errorf("Expected status %d, got %d", tt.expectedStatus, rr.Code)
			}
		})
	}
}

func TestWebhookSecurityValidation(t *testing.T) {
	// Create a test configuration
	cfg := &config.AuthConfig{
		DevMode:             false,
		SecretExpiry:        60,
		WebhookSecretExpiry: 1440,
		AllowedSources:      []string{"github", "gitlab", "bitbucket", "custom"},
	}

	// Create a mock Java client
	mockClient := &MockJavaClient{
		GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
			return "test-secret", nil
		},
	}

	// Create a webhook security service
	securityService := NewWebhookSecurityService(mockClient, cfg)

	// Test cases
	tests := []struct {
		name          string
		projectKey    string
		source        string
		signature     string
		headers       http.Header
		payload       []byte
		expectedError bool
	}{
		{
			name:       "Valid SHA256 signature",
			projectKey: "test-project",
			source:     "github",
			payload:    []byte(`{"test":"payload"}`),
			headers: func() http.Header {
				h := http.Header{}
				h.Set("X-GitHub-Event", "push")
				h.Set("X-GitHub-Delivery", "test-delivery-id")
				return h
			}(),
			expectedError: false,
		},
		{
			name:       "Invalid signature",
			projectKey: "test-project",
			source:     "github",
			signature:  "invalid",
			payload:    []byte(`{"test":"payload"}`),
			headers: func() http.Header {
				h := http.Header{}
				h.Set("X-GitHub-Event", "push")
				h.Set("X-GitHub-Delivery", "test-delivery-id")
				return h
			}(),
			expectedError: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Compute the signature if not provided
			signature := tt.signature
			if signature == "" && tt.source == "github" {
				mac := hmac.New(sha256.New, []byte("test-secret"))
				mac.Write(tt.payload)
				signature = hex.EncodeToString(mac.Sum(nil))
			}

			// Call the validation function
			err := securityService.ValidateWebhookSignature(context.Background(), tt.projectKey, tt.source, signature, tt.headers, tt.payload)

			// Check the result
			if (err != nil) != tt.expectedError {
				t.Errorf("Expected error: %v, got: %v", tt.expectedError, err)
			}
		})
	}
}

func TestRateLimiting(t *testing.T) {
	// Create a test configuration
	cfg := &config.AuthConfig{
		DevMode:             false,
		SecretExpiry:        60,
		WebhookSecretExpiry: 1440,
		AllowedSources:      []string{"github", "gitlab", "bitbucket", "custom"},
	}

	// Create a webhook security service
	securityService := NewWebhookSecurityService(nil, cfg)

	// Override rate limit for testing
	securityService.securityConfig.RateLimitPerMinute = 2

	// Test cases
	clientIP := "127.0.0.1"
	source := "github"
	projectKey := "test-project"

	// First request should be allowed
	allowed, err := securityService.CheckRateLimit(clientIP, source, projectKey)
	if !allowed || err != nil {
		t.Errorf("First request should be allowed, got allowed=%v, err=%v", allowed, err)
	}

	// Second request should be allowed
	allowed, err = securityService.CheckRateLimit(clientIP, source, projectKey)
	if !allowed || err != nil {
		t.Errorf("Second request should be allowed, got allowed=%v, err=%v", allowed, err)
	}

	// Third request should be rejected
	allowed, err = securityService.CheckRateLimit(clientIP, source, projectKey)
	if allowed || err == nil {
		t.Errorf("Third request should be rejected, got allowed=%v, err=%v", allowed, err)
	}

	// Different project or source should be allowed
	allowed, err = securityService.CheckRateLimit(clientIP, "gitlab", projectKey)
	if !allowed || err != nil {
		t.Errorf("Request with different source should be allowed, got allowed=%v, err=%v", allowed, err)
	}

	// Whitelisted IP should always be allowed
	securityService.securityConfig.IPWhitelist = []string{"127.0.0.1"}
	allowed, err = securityService.CheckRateLimit(clientIP, source, projectKey)
	if !allowed || err != nil {
		t.Errorf("Whitelisted IP should always be allowed, got allowed=%v, err=%v", allowed, err)
	}
}