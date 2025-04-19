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
	"crypto/aes"
	"crypto/cipher"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/heymumford/rinna/api/pkg/config"
)

const (
	// TokenPrefixLength is the length of the token prefix "ri-<type>-"
	TokenPrefixLength = 8

	// TokenFormatVersion indicates the token format version
	TokenFormatVersion = "v1"

	// TokenExpirationBufferHours is a buffer period before actual expiration
	// to ensure tokens are rotated before expiring
	TokenExpirationBufferHours = 24
)

// tokenKey is the context key for the token
type tokenKey struct{}

// projectKey is the context key for the project
type projectKey struct{}

// tokenMetadataKey is the context key for token metadata
type tokenMetadataKey struct{}

// TokenClaims contains the metadata for a token
type TokenClaims struct {
	TokenID      string    `json:"tid"` // Unique token identifier
	ProjectID    string    `json:"pid"` // Project the token is associated with
	TokenType    string    `json:"typ"` // Token type (dev, test, prod)
	IssuedAt     time.Time `json:"iat"` // When the token was issued
	ExpiresAt    time.Time `json:"exp"` // When the token expires
	FormatVer    string    `json:"ver"` // Token format version
	ClientIPHash string    `json:"cip"` // Hash of the client IP (optional)
	Scope        string    `json:"scp"` // Token scope/permissions
	UserID       string    `json:"uid"` // Associated user ID (optional)
}

// JavaClientInterface defines the methods required from the Java client
type JavaClientInterface interface {
	ValidateToken(ctx context.Context, token string) (string, error)
	GetWebhookSecret(ctx context.Context, projectKey, source string) (string, error)
}

// AuthService handles authentication-related operations
type AuthService struct {
	javaClient  JavaClientInterface
	config      *config.AuthConfig
	tokenCache  map[string]tokenCacheEntry
	secretCache map[string]secretCacheEntry
	encKey      []byte // Encryption key for tokens
}

type tokenCacheEntry struct {
	claims      TokenClaims
	projectID   string
	expiration  time.Time
}

type secretCacheEntry struct {
	secret     string
	expiration time.Time
}

// NewAuthService creates a new authentication service
func NewAuthService(javaClient JavaClientInterface, config *config.AuthConfig) *AuthService {
	var encKey []byte
	if config != nil && config.TokenEncryptionKey != "" {
		// Use configured encryption key, padded or truncated to 32 bytes
		encKeyStr := config.TokenEncryptionKey
		if len(encKeyStr) > 32 {
			encKeyStr = encKeyStr[:32]
		} else {
			// Pad with zeros if needed
			for len(encKeyStr) < 32 {
				encKeyStr += "0"
			}
		}
		encKey = []byte(encKeyStr)
	} else {
		// Generate a random key for this instance (will be lost on restart)
		encKey = make([]byte, 32)
		if _, err := rand.Read(encKey); err != nil {
			// Fallback to a fixed key if random generation fails
			encKey = []byte("rinna-default-encryption-key-12345")[:32]
		}
	}

	return &AuthService{
		javaClient:  javaClient,
		config:      config,
		tokenCache:  make(map[string]tokenCacheEntry),
		secretCache: make(map[string]secretCacheEntry),
		encKey:      encKey,
	}
}

// GenerateSecureToken creates a new secure token with the specified parameters
func (s *AuthService) GenerateSecureToken(ctx context.Context, projectID, tokenType, scope, userID string, expirationDays int) (string, error) {
	// Generate a unique token ID
	tokenID := uuid.New().String()

	// Create token claims with metadata
	issuedAt := time.Now().UTC()
	expiresAt := issuedAt.AddDate(0, 0, expirationDays)

	claims := TokenClaims{
		TokenID:     tokenID,
		ProjectID:   projectID,
		TokenType:   tokenType,
		IssuedAt:    issuedAt,
		ExpiresAt:   expiresAt,
		FormatVer:   TokenFormatVersion,
		Scope:       scope,
		UserID:      userID,
	}

	// Encode claims to a token
	token, err := s.encodeToken(claims)
	if err != nil {
		return "", fmt.Errorf("failed to encode token: %w", err)
	}

	// Add token to cache
	s.tokenCache[token] = tokenCacheEntry{
		claims:     claims,
		projectID:  projectID,
		expiration: time.Now().Add(time.Duration(s.config.TokenExpiry) * time.Minute),
	}

	return token, nil
}

// encodeToken encodes token claims into a secure token string
func (s *AuthService) encodeToken(claims TokenClaims) (string, error) {
	// Create the token format structure:
	// ri-<type>-<base64(encrypted(claims))>
	
	// Convert claims to a string representation (simplified for example)
	claimsStr := fmt.Sprintf(
		"%s|%s|%s|%d|%d|%s|%s|%s",
		claims.TokenID,
		claims.ProjectID,
		claims.TokenType,
		claims.IssuedAt.Unix(),
		claims.ExpiresAt.Unix(),
		claims.FormatVer,
		claims.Scope,
		claims.UserID,
	)

	// Encrypt the claims
	encrypted, err := encrypt(s.encKey, []byte(claimsStr))
	if err != nil {
		return "", err
	}

	// Encode the encrypted claims
	encodedClaims := base64.RawURLEncoding.EncodeToString(encrypted)

	// Format the token
	return fmt.Sprintf("ri-%s-%s", claims.TokenType, encodedClaims), nil
}

// decodeToken decodes a token string into token claims
func (s *AuthService) decodeToken(token string) (TokenClaims, error) {
	var claims TokenClaims

	// Validate token format (ri-<type>-<base64(encrypted(claims))>)
	if !strings.HasPrefix(token, "ri-") {
		return claims, fmt.Errorf("invalid token format: must start with 'ri-'")
	}

	parts := strings.SplitN(token, "-", 3)
	if len(parts) < 3 {
		return claims, fmt.Errorf("invalid token format: must be ri-<type>-<data>")
	}

	// Extract token type
	tokenType := parts[1]

	// For development tokens with the old format, return a skeleton claim
	if tokenType == "dev" && len(parts[2]) < 20 {
		return TokenClaims{
			TokenID:   parts[2],
			ProjectID: "dev-project",
			TokenType: "dev",
			IssuedAt:  time.Now().AddDate(0, 0, -1),
			ExpiresAt: time.Now().AddDate(0, 0, 90),
			FormatVer: "legacy",
			Scope:     "admin",
		}, nil
	}

	// Decode the encoded claims
	encryptedClaims, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil {
		return claims, fmt.Errorf("invalid token encoding: %w", err)
	}

	// Decrypt the claims
	claimsBytes, err := decrypt(s.encKey, encryptedClaims)
	if err != nil {
		return claims, fmt.Errorf("invalid token encryption: %w", err)
	}

	// Parse the claims (simplified for example)
	claimsParts := strings.Split(string(claimsBytes), "|")
	if len(claimsParts) < 6 {
		return claims, fmt.Errorf("invalid token claims format")
	}

	// Parse timestamps
	issuedAtUnix, err := parseInt64(claimsParts[3])
	if err != nil {
		return claims, fmt.Errorf("invalid issued timestamp: %w", err)
	}

	expiresAtUnix, err := parseInt64(claimsParts[4])
	if err != nil {
		return claims, fmt.Errorf("invalid expiration timestamp: %w", err)
	}

	// Create claims object
	claims = TokenClaims{
		TokenID:   claimsParts[0],
		ProjectID: claimsParts[1],
		TokenType: claimsParts[2],
		IssuedAt:  time.Unix(issuedAtUnix, 0),
		ExpiresAt: time.Unix(expiresAtUnix, 0),
		FormatVer: claimsParts[5],
		Scope:     safeGetPart(claimsParts, 6),
		UserID:    safeGetPart(claimsParts, 7),
	}

	return claims, nil
}

// parseInt64 safely parses a string to int64
func parseInt64(s string) (int64, error) {
	var i int64
	_, err := fmt.Sscanf(s, "%d", &i)
	return i, err
}

// safeGetPart safely gets a part from parts slice
func safeGetPart(parts []string, index int) string {
	if index < len(parts) {
		return parts[index]
	}
	return ""
}

// encrypt encrypts data using AES-GCM
func encrypt(key, data []byte) ([]byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	// Never use more than 2^32 random nonces with a given key
	nonce := make([]byte, 12)
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return nil, err
	}

	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	ciphertext := aesgcm.Seal(nil, nonce, data, nil)
	
	// Prepend nonce to ciphertext
	result := make([]byte, len(nonce)+len(ciphertext))
	copy(result, nonce)
	copy(result[len(nonce):], ciphertext)
	
	return result, nil
}

// decrypt decrypts data using AES-GCM
func decrypt(key, data []byte) ([]byte, error) {
	if len(data) < 13 { // Nonce + at least 1 byte of ciphertext
		return nil, errors.New("invalid ciphertext length")
	}

	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	// Extract nonce from the front of data
	nonce := data[:12]
	ciphertext := data[12:]

	return aesgcm.Open(nil, nonce, ciphertext, nil)
}

// ValidateToken validates an API token and returns the project ID
func (s *AuthService) ValidateToken(ctx context.Context, token string) (string, error) {
	// Check the cache first
	if entry, ok := s.tokenCache[token]; ok && entry.expiration.After(time.Now()) {
		return entry.projectID, nil
	}

	// Attempt to decode as a secure token
	claims, err := s.decodeToken(token)
	if err == nil {
		// Token was successfully decoded, validate it
		now := time.Now()
		
		// Check if token is expired
		if now.After(claims.ExpiresAt) {
			return "", fmt.Errorf("token has expired")
		}
		
		// If token is about to expire (within buffer period), log a warning
		if claims.ExpiresAt.Sub(now) < time.Duration(TokenExpirationBufferHours)*time.Hour {
			fmt.Printf("Warning: Token %s will expire in less than %d hours\n", 
				truncateToken(token), TokenExpirationBufferHours)
		}

		// For production tokens, verify with Java service
		if claims.TokenType != "dev" && s.javaClient != nil {
			projectID, err := s.javaClient.ValidateToken(ctx, token)
			if err != nil {
				return "", fmt.Errorf("token validation failed: %w", err)
			}

			// Cache the result with claims
			s.tokenCache[token] = tokenCacheEntry{
				claims:     claims,
				projectID:  projectID,
				expiration: time.Now().Add(time.Duration(s.config.TokenExpiry) * time.Minute),
			}

			return projectID, nil
		}

		// For dev tokens or when Java client is not available
		s.tokenCache[token] = tokenCacheEntry{
			claims:     claims,
			projectID:  claims.ProjectID,
			expiration: time.Now().Add(time.Duration(s.config.TokenExpiry) * time.Minute),
		}

		return claims.ProjectID, nil
	}

	// If decoding as a secure token failed, try legacy token format
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
			claims: TokenClaims{
				TokenID:   parts[2],
				ProjectID: projectID,
				TokenType: "dev",
				IssuedAt:  time.Now().AddDate(0, 0, -1),
				ExpiresAt: time.Now().AddDate(0, 0, 90),
				FormatVer: "legacy",
			},
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
			claims: TokenClaims{
				TokenID:   parts[2],
				ProjectID: projectID,
				TokenType: tokenType,
				IssuedAt:  time.Now().AddDate(0, 0, -1),
				ExpiresAt: time.Now().AddDate(0, 0, 90),
				FormatVer: "legacy",
			},
			projectID:  projectID,
			expiration: time.Now().Add(time.Duration(s.config.TokenExpiry) * time.Minute),
		}

		return projectID, nil
	}

	// Fallback for when Java client is not available (test environment)
	projectID := fmt.Sprintf("%s-project-%s", tokenType, parts[2])

	// Cache the result
	s.tokenCache[token] = tokenCacheEntry{
		claims: TokenClaims{
			TokenID:   parts[2],
			ProjectID: projectID,
			TokenType: tokenType,
			IssuedAt:  time.Now().AddDate(0, 0, -1),
			ExpiresAt: time.Now().AddDate(0, 0, 90),
			FormatVer: "legacy",
		},
		projectID:  projectID,
		expiration: time.Now().Add(time.Duration(s.config.TokenExpiry) * time.Minute),
	}

	return projectID, nil
}

// IsTokenExpired checks if a token is expired or about to expire
func (s *AuthService) IsTokenExpired(ctx context.Context, token string) (bool, time.Duration, error) {
	// Try to get claims from cache
	if entry, ok := s.tokenCache[token]; ok && entry.expiration.After(time.Now()) {
		now := time.Now()
		timeLeft := entry.claims.ExpiresAt.Sub(now)
		return timeLeft <= 0, timeLeft, nil
	}

	// Attempt to decode token
	claims, err := s.decodeToken(token)
	if err != nil {
		return false, 0, fmt.Errorf("failed to decode token: %w", err)
	}

	now := time.Now()
	timeLeft := claims.ExpiresAt.Sub(now)
	return timeLeft <= 0, timeLeft, nil
}

// GetTokenClaims returns the claims for a token
func (s *AuthService) GetTokenClaims(ctx context.Context, token string) (TokenClaims, error) {
	// Check the cache first
	if entry, ok := s.tokenCache[token]; ok && entry.expiration.After(time.Now()) {
		return entry.claims, nil
	}

	// Attempt to decode token
	claims, err := s.decodeToken(token)
	if err != nil {
		return TokenClaims{}, fmt.Errorf("failed to decode token: %w", err)
	}

	return claims, nil
}

// truncateToken returns a truncated token for logging (security)
func truncateToken(token string) string {
	if len(token) <= 12 {
		return "********"
	}
	return token[:8] + "..." + token[len(token)-4:]
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

// GetTokenClaims gets the token claims from the context
func GetTokenClaims(ctx context.Context) *TokenClaims {
	if claims, ok := ctx.Value(tokenMetadataKey{}).(*TokenClaims); ok {
		return claims
	}
	return nil
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

			// Get token claims
			claims, err := authService.GetTokenClaims(r.Context(), token)
			if err != nil {
				// This shouldn't happen if validation succeeded, but just in case
				http.Error(w, "Failed to get token claims: "+err.Error(), http.StatusInternalServerError)
				return
			}

			// Add the token, project ID, and claims to the context
			ctx := context.WithValue(r.Context(), tokenKey{}, token)
			ctx = context.WithValue(ctx, projectKey{}, projectID)
			ctx = context.WithValue(ctx, tokenMetadataKey{}, &claims)

			// Check token expiration warning
			if time.Now().Add(TokenExpirationBufferHours * time.Hour).After(claims.ExpiresAt) {
				// Add warning header
				w.Header().Add("X-Token-Expiring-Soon", "true")
				w.Header().Add("X-Token-Expires-In", fmt.Sprintf("%d", int(claims.ExpiresAt.Sub(time.Now()).Hours())))
			}

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
				// Instead of passing to ValidateWebhookSignature, we'll get the secret directly
				var secret string
				var err error
				
				if authService.javaClient != nil {
					secret, err = authService.javaClient.GetWebhookSecret(r.Context(), projectKey, source)
					if err != nil {
						if authService.config.DevMode {
							secret = "gh-webhook-secret-1234" // Default dev secret
						} else {
							http.Error(w, "Failed to retrieve webhook secret: "+err.Error(), http.StatusUnauthorized)
							return
						}
					}
				} else if authService.config.DevMode {
					secret = "gh-webhook-secret-1234" // Default dev secret
				} else {
					http.Error(w, "Cannot validate webhook: Java client not available", http.StatusUnauthorized)
					return
				}
				
				// For GitLab we do a direct token comparison
				if signature != secret {
					http.Error(w, "Invalid webhook token", http.StatusUnauthorized)
					return
				}
				
				// Add source to context for handlers
				ctx := context.WithValue(r.Context(), webhookSourceKey{}, source)
					
				// Skip further validation since we already validated
				next.ServeHTTP(w, r.WithContext(ctx))
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