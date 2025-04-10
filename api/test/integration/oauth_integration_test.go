/*
 * OAuth integration tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package integration

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/auth"
	"github.com/heymumford/rinna/api/internal/handlers"
	"github.com/heymumford/rinna/api/internal/middleware"
	"github.com/heymumford/rinna/api/pkg/config"
	"github.com/heymumford/rinna/api/pkg/logger"
)

func TestOAuthIntegration(t *testing.T) {
	// Set up logger for tests
	logger.Configure(logger.Config{
		Level:      logger.DebugLevel,
		TimeFormat: time.RFC3339,
		ShowCaller: false,
	})

	// Create a temporary storage directory for OAuth tokens
	tempDir, err := os.MkdirTemp("", "oauth-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	// Create token storage
	tokenStorage, err := auth.NewFileTokenStorage(tempDir, "test-encryption-key")
	if err != nil {
		t.Fatalf("Failed to create token storage: %v", err)
	}

	// Create OAuth manager
	oauthManager := auth.NewOAuthManager(tokenStorage)

	// Register a test provider
	err = oauthManager.RegisterProvider(&auth.OAuthConfig{
		Provider:     auth.OAuthProviderGitHub,
		ClientID:     "test-client-id",
		ClientSecret: "test-client-secret",
		RedirectURL:  "http://localhost:8080/api/v1/oauth/callback",
		AuthURL:      "https://github.com/login/oauth/authorize",
		TokenURL:     "https://github.com/login/oauth/access_token",
		Scopes:       []string{"repo", "user"},
		APIBaseURL:   "https://api.github.com",
		ExtraParams: map[string]string{
			"userinfo_url": "https://api.github.com/user",
		},
	})
	if err != nil {
		t.Fatalf("Failed to register provider: %v", err)
	}

	// Create auth service
	cfg := &config.RinnaConfig{
		Auth: config.AuthConfig{
			AllowedOrigins: []string{"*"},
			TokenSecret:    "test-token-secret",
		},
	}
	authService := middleware.NewAuthService(nil, &cfg.Auth)

	// Create router with auth middleware
	r := mux.NewRouter()
	r.Use(middleware.Logging)
	r.Use(middleware.RequestID)

	// Register OAuth routes
	handlers.RegisterOAuthRoutes(r, oauthManager, authService.TokenAuthMiddleware)

	// Test listing providers
	t.Run("ListProviders", func(t *testing.T) {
		req, _ := http.NewRequest("GET", "/api/v1/oauth/providers", nil)
		req.Header.Set("Authorization", "Bearer test-token")

		// Create a response recorder
		rr := httptest.NewRecorder()

		// Serve the request
		r.ServeHTTP(rr, req)

		// Check the status code
		if status := rr.Code; status != http.StatusOK {
			t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
		}

		// Parse the response
		var response map[string]interface{}
		if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Check response structure
		providers, ok := response["providers"].([]interface{})
		if !ok {
			t.Fatalf("Invalid response format: %v", response)
		}

		// Should have one provider
		if len(providers) != 1 {
			t.Errorf("Expected 1 provider, got %d", len(providers))
		}
	})

	// Test getting a provider
	t.Run("GetProvider", func(t *testing.T) {
		req, _ := http.NewRequest("GET", "/api/v1/oauth/providers/github", nil)
		req.Header.Set("Authorization", "Bearer test-token")

		// Create a response recorder
		rr := httptest.NewRecorder()

		// Serve the request
		r.ServeHTTP(rr, req)

		// Check the status code
		if status := rr.Code; status != http.StatusOK {
			t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
		}

		// Parse the response
		var response map[string]interface{}
		if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Check provider details
		if provider, ok := response["provider"].(string); !ok || provider != "github" {
			t.Errorf("Expected provider 'github', got %v", provider)
		}
	})

	// Test authorization URL generation
	t.Run("GenerateAuthURL", func(t *testing.T) {
		req, _ := http.NewRequest("GET", "/api/v1/oauth/authorize/github?project=test-project&user_id=test-user", nil)
		req.Header.Set("Authorization", "Bearer test-token")

		// Create a response recorder
		rr := httptest.NewRecorder()

		// Serve the request
		r.ServeHTTP(rr, req)

		// Check the status code
		if status := rr.Code; status != http.StatusOK {
			t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
		}

		// Parse the response
		var response map[string]interface{}
		if err := json.Unmarshal(rr.Body.Bytes(), &response); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		// Check response structure
		authURL, ok := response["authorization_url"].(string)
		if !ok || authURL == "" {
			t.Errorf("Invalid or missing authorization_url: %v", response)
		}

		state, ok := response["state"].(string)
		if !ok || state == "" {
			t.Errorf("Invalid or missing state: %v", response)
		}
	})

	// Test token operations (most we can do without a real OAuth server)
	t.Run("TokenOperations", func(t *testing.T) {
		// Manually create a token and store it
		token := &auth.OAuthToken{
			AccessToken:  "test-access-token",
			TokenType:    "Bearer",
			RefreshToken: "test-refresh-token",
			Expiry:       time.Now().Add(1 * time.Hour),
			Scopes:       []string{"repo", "user"},
			Provider:     auth.OAuthProviderGitHub,
			UserID:       "test-user",
			ProjectID:    "test-project",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
			MetaData: map[string]string{
				"login": "testuser",
				"name":  "Test User",
			},
		}

		// Store the token
		err := tokenStorage.SaveToken(token)
		if err != nil {
			t.Fatalf("Failed to save token: %v", err)
		}

		// Test listing tokens
		req, _ := http.NewRequest("GET", "/api/v1/oauth/tokens?project=test-project", nil)
		req.Header.Set("Authorization", "Bearer test-token")

		rr := httptest.NewRecorder()
		r.ServeHTTP(rr, req)

		if status := rr.Code; status != http.StatusOK {
			t.Errorf("List tokens handler returned wrong status code: got %v want %v", status, http.StatusOK)
		}

		var listResponse map[string]interface{}
		if err := json.Unmarshal(rr.Body.Bytes(), &listResponse); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		tokens, ok := listResponse["tokens"].([]interface{})
		if !ok || len(tokens) != 1 {
			t.Errorf("Expected 1 token, got %v", listResponse)
		}

		// Test getting a specific token
		req, _ = http.NewRequest("GET", "/api/v1/oauth/tokens/github?project=test-project&user_id=test-user", nil)
		req.Header.Set("Authorization", "Bearer test-token")

		rr = httptest.NewRecorder()
		r.ServeHTTP(rr, req)

		if status := rr.Code; status != http.StatusOK {
			t.Errorf("Get token handler returned wrong status code: got %v want %v", status, http.StatusOK)
		}

		var getResponse map[string]interface{}
		if err := json.Unmarshal(rr.Body.Bytes(), &getResponse); err != nil {
			t.Fatalf("Failed to parse response: %v", err)
		}

		if provider, ok := getResponse["provider"].(string); !ok || provider != "github" {
			t.Errorf("Expected provider 'github', got %v", provider)
		}

		// Test revoking a token
		req, _ = http.NewRequest("DELETE", "/api/v1/oauth/tokens/github?project=test-project&user_id=test-user", nil)
		req.Header.Set("Authorization", "Bearer test-token")

		rr = httptest.NewRecorder()
		r.ServeHTTP(rr, req)

		if status := rr.Code; status != http.StatusOK {
			t.Errorf("Revoke token handler returned wrong status code: got %v want %v", status, http.StatusOK)
		}

		// Verify token was deleted by trying to get it again
		_, err = tokenStorage.LoadToken(auth.OAuthProviderGitHub, "test-project", "test-user")
		if err == nil {
			t.Errorf("Token should have been deleted")
		}
	})

	// Test OAuth client functionality
	t.Run("OAuthClient", func(t *testing.T) {
		// Manually create a token with a fake expiry
		token := &auth.OAuthToken{
			AccessToken:  "test-access-token",
			TokenType:    "Bearer",
			RefreshToken: "test-refresh-token",
			Expiry:       time.Now().Add(1 * time.Hour),
			Scopes:       []string{"repo", "user"},
			Provider:     auth.OAuthProviderGitHub,
			UserID:       "test-user",
			ProjectID:    "test-project",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}

		// Store the token
		err := tokenStorage.SaveToken(token)
		if err != nil {
			t.Fatalf("Failed to save token: %v", err)
		}

		// Get an OAuth client
		client, err := oauthManager.GetOAuth2Client(
			context.Background(),
			auth.OAuthProviderGitHub,
			"test-project",
			"test-user",
		)

		if err != nil {
			t.Fatalf("Failed to get OAuth client: %v", err)
		}

		if client == nil {
			t.Fatal("OAuth client is nil")
		}
	})
}