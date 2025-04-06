/*
 * Authentication middleware for the Rinna API server
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
	"fmt"
	"io"
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

	// Validate token format (ri-<type>-<uuid>)
	if !strings.HasPrefix(token, "ri-") {
		return "", fmt.Errorf("invalid token format: must start with 'ri-'")
	}

	parts := strings.Split(token, "-")
	if len(parts) < 3 {
		return "", fmt.Errorf("invalid token format: must be ri-<type>-<id>")
	}

	tokenType := parts[1]
	// Validate token type
	switch tokenType {
	case "dev", "test", "prod":
		// Valid token types
	default:
		return "", fmt.Errorf("invalid token type: %s", tokenType)
	}

	// For development tokens, accept any properly formatted token
	if tokenType == "dev" {
		projectID := "dev-project"
		s.tokenCache[token] = tokenCacheEntry{
			projectID:  projectID,
			expiration: time.Now().Add(time.Duration(s.config.TokenExpiry) * time.Minute),
		}
		return projectID, nil
	}

	// For production tokens, call the Java service for validation
	// This would normally use the Java client to call the token validation service
	if s.javaClient != nil {
		projectID, err := s.javaClient.ValidateToken(ctx, token)
		if err != nil {
			return "", fmt.Errorf("token validation failed: %w", err)
		}

		// Cache the result
		s.tokenCache[token] = tokenCacheEntry{
			projectID:  projectID,
			expiration: time.Now().Add(time.Duration(s.config.TokenExpiry) * time.Minute),
		}

		return projectID, nil
	}

	// Fallback for when Java client is not available (test environment)
	projectID := fmt.Sprintf("%s-project-%s", tokenType, parts[2])

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
		var err error
		
		// Retrieve the webhook secret from the Java service
		if s.javaClient != nil {
			secret, err = s.javaClient.GetWebhookSecret(ctx, projectKey, source)
			if err != nil {
				// If the Java service is unavailable or returns an error, 
				// fallback to development mode if configured
				if s.config.DevMode {
					secret = "gh-webhook-secret-1234" // Default dev secret
				} else {
					return fmt.Errorf("failed to retrieve webhook secret: %w", err)
				}
			}
		} else if s.config.DevMode {
			// If Java client is not available and we're in dev mode, use default secret
			secret = "gh-webhook-secret-1234"
		} else {
			return fmt.Errorf("cannot validate webhook: Java client not available")
		}

		// Cache the result
		s.secretCache[cacheKey] = secretCacheEntry{
			secret:     secret,
			expiration: time.Now().Add(time.Duration(s.config.SecretExpiry) * time.Minute),
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

// webhookSourceKey is the context key for the webhook source
type webhookSourceKey struct{}

// GetWebhookSource gets the webhook source from the context
func GetWebhookSource(ctx context.Context) string {
	if source, ok := ctx.Value(webhookSourceKey{}).(string); ok {
		return source
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

			// Read the request body
			payload, err := io.ReadAll(r.Body)
			if err != nil {
				http.Error(w, "Failed to read request body", http.StatusInternalServerError)
				return
			}
			
			// Replace the request body (since we consumed it)
			r.Body = io.NopCloser(bytes.NewBuffer(payload))

			var source string
			var signature string

			// Identify the source and extract the signature
			switch {
			case strings.HasSuffix(r.URL.Path, "/github"):
				source = "github"
				
				// Get the signature
				fullSig := r.Header.Get("X-Hub-Signature-256")
				if fullSig == "" || !strings.HasPrefix(fullSig, "sha256=") {
					http.Error(w, "Invalid or missing signature", http.StatusUnauthorized)
					return
				}
				signature = strings.TrimPrefix(fullSig, "sha256=")

				// Get the event type
				if r.Header.Get("X-GitHub-Event") == "" {
					http.Error(w, "Missing event type", http.StatusBadRequest)
					return
				}
				
			case strings.HasSuffix(r.URL.Path, "/gitlab"):
				source = "gitlab"
				
				// Get the signature
				signature = r.Header.Get("X-Gitlab-Token")
				if signature == "" {
					http.Error(w, "Invalid or missing token", http.StatusUnauthorized)
					return
				}
				
				// Special validation for GitLab which uses a plain token match
				// Instead of passing to ValidateWebhookSignature, we'll do a direct comparison
				err := authService.ValidateWebhookSignature(r.Context(), projectKey, source, signature, []byte(""))
				if err != nil {
					http.Error(w, "Invalid webhook signature: "+err.Error(), http.StatusUnauthorized)
					return
				}
				
				// Skip further validation since we already validated
				next.ServeHTTP(w, r)
				return
				
			case strings.HasSuffix(r.URL.Path, "/bitbucket"):
				source = "bitbucket"
				
				// Extract the UUID from the URL path
				// Format: /api/v1/webhooks/bitbucket/:uuid
				parts := strings.Split(r.URL.Path, "/")
				if len(parts) < 5 {
					http.Error(w, "Invalid webhook URL", http.StatusBadRequest)
					return
				}
				
				// Get the signature
				signature = r.Header.Get("X-Hub-Signature")
				if signature == "" {
					http.Error(w, "Invalid or missing signature", http.StatusUnauthorized)
					return
				}
				
			default:
				// Check if it's a custom webhook
				if strings.HasPrefix(r.URL.Path, "/api/v1/webhooks/custom/") {
					source = "custom"
					
					// Get the signature
					signature = r.Header.Get("X-Webhook-Signature")
					if signature == "" {
						http.Error(w, "Invalid or missing signature", http.StatusUnauthorized)
						return
					}
				} else {
					http.Error(w, "Unsupported webhook source", http.StatusBadRequest)
					return
				}
			}

			// Validate the signature
			if source != "gitlab" { // GitLab already validated above
				err := authService.ValidateWebhookSignature(r.Context(), projectKey, source, signature, payload)
				if err != nil {
					http.Error(w, "Invalid webhook signature: "+err.Error(), http.StatusUnauthorized)
					return
				}
			}

			// Add source to context for handlers
			ctx := context.WithValue(r.Context(), webhookSourceKey{}, source)

			// Call the next handler
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}