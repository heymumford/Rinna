/*
 * Authentication middleware for the Rinna API server
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
	"strings"
	"time"

	"github.com/heymumford/rinna/api/internal/client"
	"github.com/heymumford/rinna/api/pkg/config"
)

// tokenKey is the context key for the token
type tokenKey struct{}

// projectKey is the context key for the project
type projectKey struct{}

// AuthService handles authentication-related operations
type AuthService struct {
	javaClient  *client.JavaClient
	config      *config.AuthConfig
	tokenCache  map[string]tokenCacheEntry
	secretCache map[string]secretCacheEntry
}

type tokenCacheEntry struct {
	projectID  string
	expiration time.Time
}

type secretCacheEntry struct {
	secret     string
	expiration time.Time
}

// NewAuthService creates a new authentication service
func NewAuthService(javaClient *client.JavaClient, config *config.AuthConfig) *AuthService {
	return &AuthService{
		javaClient:  javaClient,
		config:      config,
		tokenCache:  make(map[string]tokenCacheEntry),
		secretCache: make(map[string]secretCacheEntry),
	}
}

// ValidateToken validates an API token and returns the project ID
func (s *AuthService) ValidateToken(ctx context.Context, token string) (string, error) {
	// Check the cache first
	if entry, ok := s.tokenCache[token]; ok && entry.expiration.After(time.Now()) {
		return entry.projectID, nil
	}

	// TODO: Implement token validation with the Java service
	// For now, we'll use a simple validation for tokens starting with "ri-"
	if !strings.HasPrefix(token, "ri-") {
		return "", fmt.Errorf("invalid token format")
	}

	// Simulate a project ID
	projectID := "simulated-project-id"

	// Cache the result
	s.tokenCache[token] = tokenCacheEntry{
		projectID:  projectID,
		expiration: time.Now().Add(time.Duration(s.config.TokenExpiry) * time.Minute),
	}

	return projectID, nil
}

// ValidateWebhookSignature validates a webhook signature
func (s *AuthService) ValidateWebhookSignature(ctx context.Context, projectKey, source, signature string, payload []byte) error {
	// Generate a cache key
	cacheKey := fmt.Sprintf("%s:%s", projectKey, source)

	// Check the cache first
	var secret string
	if entry, ok := s.secretCache[cacheKey]; ok && entry.expiration.After(time.Now()) {
		secret = entry.secret
	} else {
		// TODO: Implement webhook secret retrieval from the Java service
		// For now, we'll use a fixed secret for testing
		secret = "gh-webhook-secret-1234"

		// Cache the result
		s.secretCache[cacheKey] = secretCacheEntry{
			secret:     secret,
			expiration: time.Now().Add(time.Hour),
		}
	}

	// Validate the signature
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(payload)
	expectedSignature := "sha256=" + hex.EncodeToString(mac.Sum(nil))

	if !hmac.Equal([]byte(expectedSignature), []byte("sha256="+signature)) {
		return fmt.Errorf("invalid webhook signature")
	}

	return nil
}

// GetToken gets the token from the context
func GetToken(ctx context.Context) string {
	if token, ok := ctx.Value(tokenKey{}).(string); ok {
		return token
	}
	return ""
}

// GetProjectID gets the project ID from the context
func GetProjectID(ctx context.Context) string {
	if projectID, ok := ctx.Value(projectKey{}).(string); ok {
		return projectID
	}
	return ""
}

// TokenAuthentication middleware authenticates requests using API tokens
func TokenAuthentication(authService *AuthService) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// Skip authentication for OPTIONS requests (CORS preflight)
			if r.Method == http.MethodOptions {
				next.ServeHTTP(w, r)
				return
			}

			// Skip authentication for webhook endpoints
			if strings.HasPrefix(r.URL.Path, "/api/v1/webhooks/") {
				next.ServeHTTP(w, r)
				return
			}

			// Get the Authorization header
			authHeader := r.Header.Get("Authorization")

			// Check if the header is present
			if authHeader == "" {
				http.Error(w, "Authorization header required", http.StatusUnauthorized)
				return
			}

			// Check if it's a Bearer token
			if !strings.HasPrefix(authHeader, "Bearer ") {
				http.Error(w, "Authorization header must be Bearer token", http.StatusUnauthorized)
				return
			}

			// Extract the token
			token := strings.TrimPrefix(authHeader, "Bearer ")

			// Validate the token
			projectID, err := authService.ValidateToken(r.Context(), token)
			if err != nil {
				http.Error(w, "Invalid token: "+err.Error(), http.StatusUnauthorized)
				return
			}

			// Add the token and project ID to the context
			ctx := context.WithValue(r.Context(), tokenKey{}, token)
			ctx = context.WithValue(ctx, projectKey{}, projectID)

			// Call the next handler
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

// WebhookAuthentication middleware authenticates webhook requests
func WebhookAuthentication(authService *AuthService) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// Only apply to webhook endpoints
			if !strings.HasPrefix(r.URL.Path, "/api/v1/webhooks/") {
				next.ServeHTTP(w, r)
				return
			}

			// Get the project key
			projectKey := r.URL.Query().Get("project")
			if projectKey == "" {
				http.Error(w, "Project key is required", http.StatusBadRequest)
				return
			}

			// For GitHub webhooks
			if strings.HasSuffix(r.URL.Path, "/github") {
				// Get the signature
				signature := r.Header.Get("X-Hub-Signature-256")
				if signature == "" || !strings.HasPrefix(signature, "sha256=") {
					http.Error(w, "Invalid or missing signature", http.StatusUnauthorized)
					return
				}
				signature = strings.TrimPrefix(signature, "sha256=")

				// Get the event type
				if r.Header.Get("X-GitHub-Event") == "" {
					http.Error(w, "Missing event type", http.StatusBadRequest)
					return
				}

				// Pass through for now, actual validation will be done in the handler
				// In a production implementation, we would validate the signature here
			}

			// Call the next handler
			next.ServeHTTP(w, r)
		})
	}
}