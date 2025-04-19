/*
 * Tests for the token manager
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package client

import (
	"context"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/heymumford/rinna/go/pkg/config"
)

// MockJavaClient is a mock implementation of the JavaClient
type MockJavaClient struct {
	ValidateTokenFunc   func(ctx context.Context, token string) (string, error)
	GetWebhookSecretFunc func(ctx context.Context, projectKey, source string) (string, error)
	RequestFunc         func(ctx context.Context, method, path string, payload interface{}, response interface{}) error
}

func (m *MockJavaClient) ValidateToken(ctx context.Context, token string) (string, error) {
	if m.ValidateTokenFunc != nil {
		return m.ValidateTokenFunc(ctx, token)
	}
	return "", nil
}

func (m *MockJavaClient) GetWebhookSecret(ctx context.Context, projectKey, source string) (string, error) {
	if m.GetWebhookSecretFunc != nil {
		return m.GetWebhookSecretFunc(ctx, projectKey, source)
	}
	return "", nil
}

func (m *MockJavaClient) Request(ctx context.Context, method, path string, payload interface{}, response interface{}) error {
	if m.RequestFunc != nil {
		return m.RequestFunc(ctx, method, path, payload, response)
	}
	return nil
}

func TestTokenManagerBasics(t *testing.T) {
	// Create a temporary directory for token storage
	tempDir, err := os.MkdirTemp("", "token-manager-test")
	if err != nil {
		t.Fatalf("Failed to create temporary directory: %v", err)
	}
	defer os.RemoveAll(tempDir)

	// Set the token storage directory
	os.Setenv("RINNA_TOKENS_DIR", tempDir)
	defer os.Unsetenv("RINNA_TOKENS_DIR")

	// Create a mock client
	mockClient := &MockJavaClient{}

	// Create a token manager
	cfg := &config.RinnaConfig{}
	tm, err := NewTokenManager(cfg, mockClient)
	if err != nil {
		t.Fatalf("Failed to create token manager: %v", err)
	}

	// Test token storage functionality
	token := "ri-test-abc123"
	projectID := "test-project"
	
	// Add a test token
	tm.mu.Lock()
	tm.tokens[token] = TokenInfo{
		Token:       token,
		ProjectID:   projectID,
		TokenType:   "test",
		Scope:       "api",
		IssuedAt:    time.Now(),
		ExpiresAt:   time.Now().Add(24 * time.Hour),
		LastChecked: time.Now(),
		Valid:       true,
	}
	tm.mu.Unlock()

	// Save tokens
	if err := tm.saveTokens(); err != nil {
		t.Fatalf("Failed to save tokens: %v", err)
	}

	// Verify token file was created
	if _, err := os.Stat(filepath.Join(tempDir, "tokens.json")); os.IsNotExist(err) {
		t.Errorf("Token file was not created")
	}

	// Create a new token manager that should load the saved tokens
	tm2, err := NewTokenManager(cfg, mockClient)
	if err != nil {
		t.Fatalf("Failed to create second token manager: %v", err)
	}

	// Verify token was loaded
	tm2.mu.RLock()
	tokenInfo, ok := tm2.tokens[token]
	tm2.mu.RUnlock()
	
	if !ok {
		t.Errorf("Token was not loaded by second token manager")
	} else if tokenInfo.ProjectID != projectID {
		t.Errorf("Expected project ID %s, got %s", projectID, tokenInfo.ProjectID)
	}
}

func TestTokenRequest(t *testing.T) {
	// Create a temporary directory for token storage
	tempDir, err := os.MkdirTemp("", "token-manager-test")
	if err != nil {
		t.Fatalf("Failed to create temporary directory: %v", err)
	}
	defer os.RemoveAll(tempDir)

	// Set the token storage directory
	os.Setenv("RINNA_TOKENS_DIR", tempDir)
	defer os.Unsetenv("RINNA_TOKENS_DIR")

	// Create a mock server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		
		if r.URL.Path == "/api/auth/token/generate" {
			w.Write([]byte(`{"token":"ri-test-generated123","expiresAt":"2025-05-09T00:00:00Z"}`))
		}
	}))
	defer server.Close()

	// Create a mock client that forwards to the test server
	mockClient := &MockJavaClient{
		RequestFunc: func(ctx context.Context, method, path string, payload interface{}, response interface{}) error {
			client := &http.Client{}
			req, _ := http.NewRequest(method, server.URL+path, nil)
			resp, _ := client.Do(req)
			defer resp.Body.Close()
			
			// Simulate token generation response
			if path == "/api/auth/token/generate" && response != nil {
				respStruct := response.(*struct{
					Token     string    `json:"token"`
					ExpiresAt time.Time `json:"expiresAt"`
				})
				
				respStruct.Token = "ri-test-generated123"
				respStruct.ExpiresAt = time.Now().Add(24 * time.Hour)
			}
			
			return nil
		},
	}

	// Create a token manager
	cfg := &config.RinnaConfig{
		Java: config.JavaServiceConfig{
			Endpoints: map[string]string{
				"token_generate": "/api/auth/token/generate",
			},
		},
	}
	tm, err := NewTokenManager(cfg, mockClient)
	if err != nil {
		t.Fatalf("Failed to create token manager: %v", err)
	}

	// Request a new token
	ctx := context.Background()
	projectID := "test-project"
	token, err := tm.requestNewToken(ctx, projectID)
	
	if err != nil {
		t.Fatalf("Failed to request new token: %v", err)
	}
	
	if token != "ri-test-generated123" {
		t.Errorf("Expected token ri-test-generated123, got %s", token)
	}
	
	// Verify token was stored
	tm.mu.RLock()
	tokenInfo, ok := tm.tokens[token]
	tm.mu.RUnlock()
	
	if !ok {
		t.Errorf("Token was not stored")
	} else if tokenInfo.ProjectID != projectID {
		t.Errorf("Expected project ID %s, got %s", projectID, tokenInfo.ProjectID)
	}
}

func TestTokenValidation(t *testing.T) {
	// Create a temporary directory for token storage
	tempDir, err := os.MkdirTemp("", "token-manager-test")
	if err != nil {
		t.Fatalf("Failed to create temporary directory: %v", err)
	}
	defer os.RemoveAll(tempDir)

	// Set the token storage directory
	os.Setenv("RINNA_TOKENS_DIR", tempDir)
	defer os.Unsetenv("RINNA_TOKENS_DIR")

	// Create a mock client
	mockClient := &MockJavaClient{
		ValidateTokenFunc: func(ctx context.Context, token string) (string, error) {
			if token == "ri-test-valid123" {
				return "test-project", nil
			}
			return "", nil
		},
	}

	// Create a token manager
	cfg := &config.RinnaConfig{}
	tm, err := NewTokenManager(cfg, mockClient)
	if err != nil {
		t.Fatalf("Failed to create token manager: %v", err)
	}

	// Add test tokens
	tm.mu.Lock()
	// Valid token
	tm.tokens["ri-test-valid123"] = TokenInfo{
		Token:       "ri-test-valid123",
		ProjectID:   "test-project",
		TokenType:   "test",
		Scope:       "api",
		IssuedAt:    time.Now(),
		ExpiresAt:   time.Now().Add(24 * time.Hour),
		LastChecked: time.Now().Add(-25 * time.Hour), // Force validation
		Valid:       true,
	}
	
	// Expired token
	tm.tokens["ri-test-expired123"] = TokenInfo{
		Token:       "ri-test-expired123",
		ProjectID:   "test-project",
		TokenType:   "test",
		Scope:       "api",
		IssuedAt:    time.Now().Add(-48 * time.Hour),
		ExpiresAt:   time.Now().Add(-24 * time.Hour),
		LastChecked: time.Now(),
		Valid:       true,
	}
	tm.mu.Unlock()

	// Test valid token
	ctx := context.Background()
	isValid, err := tm.ValidateToken(ctx, "ri-test-valid123")
	if err != nil {
		t.Fatalf("Failed to validate token: %v", err)
	}
	if !isValid {
		t.Errorf("Expected valid token to be valid")
	}

	// Test expired token
	isValid, err = tm.ValidateToken(ctx, "ri-test-expired123")
	if err == nil || isValid {
		t.Errorf("Expected expired token to be invalid")
	}
}

func ExampleTokenManager() {
	// This example demonstrates how to use the token manager
	// in a complete application flow.
	
	// Initialize configuration and client
	cfg, _ := config.GetConfig()
	javaClient := NewJavaClient(&cfg.Java).WithFullConfig(cfg)
	
	// Initialize token manager
	tokenManager, _ := NewTokenManager(cfg, javaClient)
	
	// Get a token for a project
	ctx := context.Background()
	projectID := "my-project"
	token, _ := tokenManager.GetToken(ctx, projectID)
	
	// Use the token for API requests
	fmt.Printf("Using token %s for project %s\n", truncateToken(token), projectID)
	
	// Validate a token
	isValid, _ := tokenManager.ValidateToken(ctx, token)
	fmt.Printf("Token is valid: %v\n", isValid)
	
	// Revoke a token when done
	tokenManager.RevokeToken(ctx, token)
	
	// Clean up expired tokens
	tokenManager.CleanExpiredTokens()
}