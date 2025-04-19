/*
 * Tests for authentication middleware
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package middleware

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/heymumford/rinna/go/pkg/config"
)

// MockJavaClient is a mock of the Java client for testing
type MockJavaClient struct {
	ValidateTokenFunc      func(ctx context.Context, token string) (string, error)
	GetWebhookSecretFunc   func(ctx context.Context, projectKey, source string) (string, error)
}

// ValidateToken mocks the JavaClient's ValidateToken method
func (m *MockJavaClient) ValidateToken(ctx context.Context, token string) (string, error) {
	if m.ValidateTokenFunc != nil {
		return m.ValidateTokenFunc(ctx, token)
	}
	return "", fmt.Errorf("not implemented")
}

// GetWebhookSecret mocks the JavaClient's GetWebhookSecret method
func (m *MockJavaClient) GetWebhookSecret(ctx context.Context, projectKey, source string) (string, error) {
	if m.GetWebhookSecretFunc != nil {
		return m.GetWebhookSecretFunc(ctx, projectKey, source)
	}
	return "", fmt.Errorf("not implemented")
}

// TestValidateToken tests the ValidateToken function
func TestValidateToken(t *testing.T) {
	tests := []struct {
		name              string
		token             string
		mockJavaClient    *MockJavaClient
		expectedProjectID string
		expectError       bool
	}{
		{
			name:  "Invalid token format - no prefix",
			token: "invalid-token",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "", fmt.Errorf("invalid token")
				},
			},
			expectError: true,
		},
		{
			name:  "Invalid token format - too few parts",
			token: "ri-token",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "", fmt.Errorf("invalid token")
				},
			},
			expectError: true,
		},
		{
			name:  "Invalid token type",
			token: "ri-invalid-12345",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "", fmt.Errorf("invalid token type")
				},
			},
			expectError: true,
		},
		{
			name:  "Valid dev token",
			token: "ri-dev-12345",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "dev-project", nil
				},
			},
			expectedProjectID: "dev-project",
			expectError:       false,
		},
		{
			name:  "Valid prod token",
			token: "ri-prod-12345",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "prod-project-12345", nil
				},
			},
			expectedProjectID: "prod-project-12345",
			expectError:       false,
		},
		{
			name:  "Java client validation fails",
			token: "ri-prod-12345",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "", fmt.Errorf("token validation failed")
				},
			},
			expectError: true,
		},
		{
			name:  "No Java client - fallback",
			token: "ri-test-12345",
			mockJavaClient: nil,
			expectedProjectID: "test-project-12345",
			expectError:       false,
		},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			// Create auth service
			authConfig := &config.AuthConfig{
				TokenExpiry:  60,
				SecretExpiry: 60,
				DevMode:      true,
			}
			
			var authService *AuthService
			if tc.mockJavaClient != nil {
				authService = NewAuthService(tc.mockJavaClient, authConfig)
			} else {
				authService = NewAuthService(nil, authConfig)
			}

			// Call the function
			projectID, err := authService.ValidateToken(context.Background(), tc.token)

			// Check results
			if tc.expectError && err == nil {
				t.Errorf("expected error but got none")
			}
			if !tc.expectError && err != nil {
				t.Errorf("did not expect error but got: %v", err)
			}
			if !tc.expectError && projectID != tc.expectedProjectID {
				t.Errorf("expected project ID %s, got %s", tc.expectedProjectID, projectID)
			}
		})
	}
}

// TestValidateWebhookSignature tests the ValidateWebhookSignature function
func TestValidateWebhookSignature(t *testing.T) {
	// Create a test payload
	payload := []byte("test payload")
	
	// Create a valid signature
	validSecret := "gh-webhook-secret-1234"
	mac := hmac.New(sha256.New, []byte(validSecret))
	mac.Write(payload)
	validSignature := hex.EncodeToString(mac.Sum(nil))
	
	// Create some test cases
	tests := []struct {
		name           string
		projectKey     string
		source         string
		signature      string
		payload        []byte
		mockJavaClient *MockJavaClient
		expectError    bool
	}{
		{
			name:       "Valid signature",
			projectKey: "TEST",
			source:     "github",
			signature:  validSignature,
			payload:    payload,
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectError: false,
		},
		{
			name:       "Invalid signature",
			projectKey: "TEST",
			source:     "github",
			signature:  "invalid",
			payload:    payload,
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectError: true,
		},
		{
			name:       "Java client error - dev mode",
			projectKey: "TEST",
			source:     "github",
			signature:  validSignature,
			payload:    payload,
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return "", fmt.Errorf("failed to get secret")
				},
			},
			expectError: false, // Should succeed due to dev mode fallback
		},
		{
			name:       "No Java client - dev mode",
			projectKey: "TEST",
			source:     "github",
			signature:  validSignature,
			payload:    payload,
			mockJavaClient: nil,
			expectError: false, // Should succeed due to dev mode fallback
		},
	}
	
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			// Create auth service with dev mode enabled
			authConfig := &config.AuthConfig{
				TokenExpiry:  60,
				SecretExpiry: 60,
				DevMode:      true,
			}
			
			var authService *AuthService
			if tc.mockJavaClient != nil {
				authService = NewAuthService(tc.mockJavaClient, authConfig)
			} else {
				authService = NewAuthService(nil, authConfig)
			}
			
			// Call the function
			err := authService.ValidateWebhookSignature(context.Background(), tc.projectKey, tc.source, tc.signature, tc.payload)
			
			// Check results
			if tc.expectError && err == nil {
				t.Errorf("expected error but got none")
			}
			if !tc.expectError && err != nil {
				t.Errorf("did not expect error but got: %v", err)
			}
		})
	}
	
	// Test non-dev mode
	t.Run("Java client error - non-dev mode", func(t *testing.T) {
		// Create auth service with dev mode disabled
		authConfig := &config.AuthConfig{
			TokenExpiry:  60,
			SecretExpiry: 60,
			DevMode:      false,
		}
		
		mockJavaClient := &MockJavaClient{
			GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
				return "", fmt.Errorf("failed to get secret")
			},
		}
		
		authService := NewAuthService(mockJavaClient, authConfig)
		
		// Call the function
		err := authService.ValidateWebhookSignature(context.Background(), "TEST", "github", validSignature, payload)
		
		// Check results - should error in non-dev mode
		if err == nil {
			t.Errorf("expected error but got none")
		}
	})
}

// TestTokenCache tests that the token cache works correctly
func TestTokenCache(t *testing.T) {
	// Create a counter to track JavaClient calls
	calls := 0
	
	// Create a mock JavaClient
	mockJavaClient := &MockJavaClient{
		ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
			calls++
			return "test-project", nil
		},
	}
	
	// Create auth service with a short cache expiry
	authConfig := &config.AuthConfig{
		TokenExpiry:  1, // 1 minute
		SecretExpiry: 60,
		DevMode:      true,
	}
	
	authService := NewAuthService(mockJavaClient, authConfig)
	
	// First call should call the Java client
	projectID, err := authService.ValidateToken(context.Background(), "ri-test-12345")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if projectID != "test-project" {
		t.Errorf("expected project ID test-project, got %s", projectID)
	}
	if calls != 1 {
		t.Errorf("expected 1 call to JavaClient, got %d", calls)
	}
	
	// Second call should use the cache
	projectID, err = authService.ValidateToken(context.Background(), "ri-test-12345")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if projectID != "test-project" {
		t.Errorf("expected project ID test-project, got %s", projectID)
	}
	if calls != 1 {
		t.Errorf("expected 1 call to JavaClient, got %d", calls)
	}
	
	// Wait for the cache to expire
	time.Sleep(70 * time.Second) // Warning: this test takes 70 seconds to run!
	
	// Third call should call the Java client again
	projectID, err = authService.ValidateToken(context.Background(), "ri-test-12345")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if projectID != "test-project" {
		t.Errorf("expected project ID test-project, got %s", projectID)
	}
	if calls != 2 {
		t.Errorf("expected 2 calls to JavaClient, got %d", calls)
	}
}

// TestTokenAuthentication tests the TokenAuthentication middleware
func TestTokenAuthentication(t *testing.T) {
	tests := []struct {
		name           string
		authHeader     string
		mockJavaClient *MockJavaClient
		expectedStatus int
	}{
		{
			name:       "No authorization header",
			authHeader: "",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "test-project", nil
				},
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name:       "Invalid authorization header format",
			authHeader: "InvalidFormat token123",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "test-project", nil
				},
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name:       "Invalid token",
			authHeader: "Bearer invalid-token",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "", fmt.Errorf("invalid token")
				},
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name:       "Valid token",
			authHeader: "Bearer ri-test-12345",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "test-project", nil
				},
			},
			expectedStatus: http.StatusOK,
		},
		{
			name:       "OPTIONS request - skips auth",
			authHeader: "",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "test-project", nil
				},
			},
			expectedStatus: http.StatusOK,
		},
		{
			name:       "Webhook request - skips auth",
			authHeader: "",
			mockJavaClient: &MockJavaClient{
				ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
					return "test-project", nil
				},
			},
			expectedStatus: http.StatusOK,
		},
	}
	
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			// Create auth service
			authConfig := &config.AuthConfig{
				TokenExpiry:  60,
				SecretExpiry: 60,
				DevMode:      true,
			}
			
			authService := NewAuthService(tc.mockJavaClient, authConfig)
			
			// Create a request
			var r *http.Request
			var err error
			
			if tc.name == "OPTIONS request - skips auth" {
				r, err = http.NewRequest(http.MethodOptions, "/api/resources", nil)
			} else if tc.name == "Webhook request - skips auth" {
				r, err = http.NewRequest(http.MethodPost, "/api/v1/webhooks/github", nil)
			} else {
				r, err = http.NewRequest(http.MethodGet, "/api/resources", nil)
			}
			
			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
			
			// Add auth header if provided
			if tc.authHeader != "" {
				r.Header.Set("Authorization", tc.authHeader)
			}
			
			// Create a test handler that always succeeds
			testHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				// Check if token and project ID were added to context
				if tc.expectedStatus == http.StatusOK {
					token := GetToken(r.Context())
					if token == "" && tc.name != "OPTIONS request - skips auth" && tc.name != "Webhook request - skips auth" {
						t.Error("expected token in context but got none")
					}
					
					projectID := GetProjectID(r.Context())
					if projectID == "" && tc.name != "OPTIONS request - skips auth" && tc.name != "Webhook request - skips auth" {
						t.Error("expected project ID in context but got none")
					}
				}
				
				w.WriteHeader(http.StatusOK)
			})
			
			// Create the middleware
			middleware := TokenAuthentication(authService)
			
			// Create a response recorder
			rr := httptest.NewRecorder()
			
			// Apply the middleware to the test handler
			middleware(testHandler).ServeHTTP(rr, r)
			
			// Check the response
			if rr.Code != tc.expectedStatus {
				t.Errorf("expected status %d, got %d", tc.expectedStatus, rr.Code)
			}
		})
	}
}

// TestWebhookAuthentication tests the WebhookAuthentication middleware
func TestWebhookAuthentication(t *testing.T) {
	// Create a test payload
	payload := []byte("test payload")
	
	// Create a valid signature
	validSecret := "gh-webhook-secret-1234"
	mac := hmac.New(sha256.New, []byte(validSecret))
	mac.Write(payload)
	validSignature := hex.EncodeToString(mac.Sum(nil))
	
	tests := []struct {
		name           string
		path           string
		headers        map[string]string
		queryParams    map[string]string
		mockJavaClient *MockJavaClient
		expectedStatus int
	}{
		{
			name: "Non-webhook request - skips auth",
			path: "/api/resources",
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusOK,
		},
		{
			name: "GitHub webhook - missing project key",
			path: "/api/v1/webhooks/github",
			headers: map[string]string{
				"X-Hub-Signature-256": "sha256=" + validSignature,
				"X-GitHub-Event":      "push",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusBadRequest,
		},
		{
			name: "GitHub webhook - missing event type",
			path: "/api/v1/webhooks/github",
			headers: map[string]string{
				"X-Hub-Signature-256": "sha256=" + validSignature,
			},
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusBadRequest,
		},
		{
			name: "GitHub webhook - missing signature",
			path: "/api/v1/webhooks/github",
			headers: map[string]string{
				"X-GitHub-Event": "push",
			},
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name: "GitHub webhook - invalid signature",
			path: "/api/v1/webhooks/github",
			headers: map[string]string{
				"X-Hub-Signature-256": "sha256=invalid",
				"X-GitHub-Event":      "push",
			},
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name: "GitHub webhook - valid",
			path: "/api/v1/webhooks/github",
			headers: map[string]string{
				"X-Hub-Signature-256": "sha256=" + validSignature,
				"X-GitHub-Event":      "push",
			},
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusOK,
		},
		{
			name: "GitLab webhook - missing token",
			path: "/api/v1/webhooks/gitlab",
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name: "GitLab webhook - valid",
			path: "/api/v1/webhooks/gitlab",
			headers: map[string]string{
				"X-Gitlab-Token": validSecret,
			},
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusOK,
		},
		{
			name: "Bitbucket webhook - missing signature",
			path: "/api/v1/webhooks/bitbucket",
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name: "Bitbucket webhook - valid",
			path: "/api/v1/webhooks/bitbucket",
			headers: map[string]string{
				"X-Hub-Signature": validSignature,
			},
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusOK,
		},
		{
			name: "Custom webhook - missing signature",
			path: "/api/v1/webhooks/custom/mywebhook",
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusUnauthorized,
		},
		{
			name: "Custom webhook - valid",
			path: "/api/v1/webhooks/custom/mywebhook",
			headers: map[string]string{
				"X-Webhook-Signature": validSignature,
			},
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusOK,
		},
		{
			name: "Unsupported webhook source",
			path: "/api/v1/webhooks/unknown",
			queryParams: map[string]string{
				"project": "TEST",
			},
			mockJavaClient: &MockJavaClient{
				GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
					return validSecret, nil
				},
			},
			expectedStatus: http.StatusBadRequest,
		},
	}
	
	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			// Create auth service
			authConfig := &config.AuthConfig{
				TokenExpiry:  60,
				SecretExpiry: 60,
				DevMode:      true,
			}
			
			authService := NewAuthService(tc.mockJavaClient, authConfig)
			
			// Create a request
			r, err := http.NewRequest(http.MethodPost, tc.path, strings.NewReader("test payload"))
			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
			
			// Add query parameters
			q := r.URL.Query()
			for k, v := range tc.queryParams {
				q.Add(k, v)
			}
			r.URL.RawQuery = q.Encode()
			
			// Add headers
			for k, v := range tc.headers {
				r.Header.Set(k, v)
			}
			
			// Create a test handler that always succeeds
			testHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				// Check if source was added to context
				if tc.expectedStatus == http.StatusOK && strings.Contains(tc.path, "/webhooks/") {
					source := GetWebhookSource(r.Context())
					if source == "" {
						t.Error("expected webhook source in context but got none")
					}
				}
				
				w.WriteHeader(http.StatusOK)
			})
			
			// Create the middleware
			middleware := WebhookAuthentication(authService)
			
			// Create a response recorder
			rr := httptest.NewRecorder()
			
			// Apply the middleware to the test handler
			middleware(testHandler).ServeHTTP(rr, r)
			
			// Check the response
			if rr.Code != tc.expectedStatus {
				t.Errorf("expected status %d, got %d", tc.expectedStatus, rr.Code)
			}
		})
	}
}

// TestContextFunctions tests the context retrieval functions
func TestContextFunctions(t *testing.T) {
	// Test GetToken
	t.Run("GetToken", func(t *testing.T) {
		// With token in context
		token := "ri-test-12345"
		ctx := context.WithValue(context.Background(), tokenKey{}, token)
		gotToken := GetToken(ctx)
		if gotToken != token {
			t.Errorf("GetToken() = %v, want %v", gotToken, token)
		}
		
		// Without token in context
		emptyToken := GetToken(context.Background())
		if emptyToken != "" {
			t.Errorf("GetToken() with empty context = %v, want empty string", emptyToken)
		}
	})
	
	// Test GetProjectID
	t.Run("GetProjectID", func(t *testing.T) {
		// With project ID in context
		projectID := "test-project"
		ctx := context.WithValue(context.Background(), projectKey{}, projectID)
		gotProjectID := GetProjectID(ctx)
		if gotProjectID != projectID {
			t.Errorf("GetProjectID() = %v, want %v", gotProjectID, projectID)
		}
		
		// Without project ID in context
		emptyProjectID := GetProjectID(context.Background())
		if emptyProjectID != "" {
			t.Errorf("GetProjectID() with empty context = %v, want empty string", emptyProjectID)
		}
	})
	
	// Test GetWebhookSource
	t.Run("GetWebhookSource", func(t *testing.T) {
		// With source in context
		source := "github"
		ctx := context.WithValue(context.Background(), webhookSourceKey{}, source)
		gotSource := GetWebhookSource(ctx)
		if gotSource != source {
			t.Errorf("GetWebhookSource() = %v, want %v", gotSource, source)
		}
		
		// Without source in context
		emptySource := GetWebhookSource(context.Background())
		if emptySource != "" {
			t.Errorf("GetWebhookSource() with empty context = %v, want empty string", emptySource)
		}
	})
}

// TestSecretCache tests that the secret cache works correctly
func TestSecretCache(t *testing.T) {
	// Create a counter to track JavaClient calls
	calls := 0
	
	// Create a mock JavaClient
	mockJavaClient := &MockJavaClient{
		GetWebhookSecretFunc: func(ctx context.Context, projectKey, source string) (string, error) {
			calls++
			return "test-secret", nil
		},
	}
	
	// Create auth service with a short cache expiry
	authConfig := &config.AuthConfig{
		TokenExpiry:  60,
		SecretExpiry: 1, // 1 minute
		DevMode:      true,
	}
	
	authService := NewAuthService(mockJavaClient, authConfig)
	projectKey := "TEST"
	source := "github"
	
	// Create a valid payload and signature
	payload := []byte("test payload")
	mac := hmac.New(sha256.New, []byte("test-secret"))
	mac.Write(payload)
	signature := hex.EncodeToString(mac.Sum(nil))
	
	// First call should call the Java client
	err := authService.ValidateWebhookSignature(context.Background(), projectKey, source, signature, payload)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if calls != 1 {
		t.Errorf("expected 1 call to JavaClient, got %d", calls)
	}
	
	// Second call should use the cache
	err = authService.ValidateWebhookSignature(context.Background(), projectKey, source, signature, payload)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if calls != 1 {
		t.Errorf("expected still 1 call to JavaClient, got %d", calls)
	}
	
	// Manually expire the cache by setting the entry's expiration time to the past
	cacheKey := fmt.Sprintf("%s:%s", projectKey, source)
	entry := authService.secretCache[cacheKey]
	entry.expiration = time.Now().Add(-2 * time.Minute)
	authService.secretCache[cacheKey] = entry
	
	// Third call should call the Java client again
	err = authService.ValidateWebhookSignature(context.Background(), projectKey, source, signature, payload)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if calls != 2 {
		t.Errorf("expected 2 calls to JavaClient, got %d", calls)
	}
}