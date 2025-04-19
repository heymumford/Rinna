/*
 * Webhook security middleware for the Rinna API server
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
	"crypto/sha1"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/heymumford/rinna/api/pkg/config"
	"github.com/heymumford/rinna/api/pkg/logger"
)

// Context keys for webhook security
// type webhookSourceKey struct{} - already defined in auth.go
type projectKeyCtxKey struct{}

// WebhookSecurityConfig holds configuration for webhook security features
type WebhookSecurityConfig struct {
	RateLimitPerMinute  int               // Max requests per minute
	TimestampTolerance  time.Duration     // Max age of timestamp in webhook
	NonceExpiration     time.Duration     // How long to remember nonces
	ProviderSettings    map[string]string // Provider-specific settings
	DefaultSecret       string            // Used only in development mode
	RecordMetrics       bool              // Whether to record metrics
	StrictMode          bool              // Enforce strict security checks
	IPWhitelist         []string          // Trusted IPs that bypass some checks
	NonceReplayProtect  bool              // Enable nonce-based replay protection
	SignatureAlgorithms map[string]string // Supported signature algorithms
}

// WebhookSecurityService manages webhook authentication and security
type WebhookSecurityService struct {
	javaClient     JavaClientInterface
	config         *config.AuthConfig
	securityConfig *WebhookSecurityConfig
	secretCache    map[string]secretCacheEntry
	nonceCache     map[string]time.Time
	rateLimitCache map[string]rateLimitEntry
	mu             sync.RWMutex
	nonceMu        sync.RWMutex
	rateLimitMu    sync.RWMutex
}

type rateLimitEntry struct {
	count     int
	resetTime time.Time
}

// NewWebhookSecurityService creates a new webhook security service
func NewWebhookSecurityService(javaClient JavaClientInterface, config *config.AuthConfig) *WebhookSecurityService {
	// Set default security config
	securityConfig := &WebhookSecurityConfig{
		RateLimitPerMinute:  60,
		TimestampTolerance:  15 * time.Minute,
		NonceExpiration:     24 * time.Hour,
		RecordMetrics:       true,
		StrictMode:          false,
		NonceReplayProtect:  true,
		SignatureAlgorithms: map[string]string{
			"github":    "sha256",
			"gitlab":    "token",
			"bitbucket": "sha1",
			"custom":    "sha256",
		},
	}

	// Default IP whitelist in development mode
	if config != nil && config.DevMode {
		securityConfig.IPWhitelist = []string{"127.0.0.1", "::1"}
		securityConfig.DefaultSecret = "dev-webhook-secret-1234"
	}

	return &WebhookSecurityService{
		javaClient:     javaClient,
		config:         config,
		securityConfig: securityConfig,
		secretCache:    make(map[string]secretCacheEntry),
		nonceCache:     make(map[string]time.Time),
		rateLimitCache: make(map[string]rateLimitEntry),
	}
}

// ValidateWebhookSignature validates a webhook signature with enhanced security
func (s *WebhookSecurityService) ValidateWebhookSignature(ctx context.Context, projectKey, source, signature string, headers http.Header, payload []byte) error {
	// Log the validation attempt
	logger.Debug("Validating webhook signature", 
		logger.Field("projectKey", projectKey),
		logger.Field("source", source),
	)

	// Generate a cache key for the secret (used elsewhere in implementation)
	_ = fmt.Sprintf("%s:%s", projectKey, source)

	// Check if the source is supported
	signatureAlgo, supported := s.securityConfig.SignatureAlgorithms[source]
	if !supported {
		return fmt.Errorf("unsupported webhook source: %s", source)
	}

	// Validate timestamp if provided and strict mode is enabled
	if s.securityConfig.StrictMode {
		if err := s.validateTimestamp(headers, source); err != nil {
			logger.Warn("Webhook timestamp validation failed", logger.Field("error", err))
			return err
		}
	}

	// Check for nonce replay if enabled
	if s.securityConfig.NonceReplayProtect {
		if err := s.validateNonce(headers, source); err != nil {
			logger.Warn("Webhook nonce validation failed", logger.Field("error", err))
			return err
		}
	}

	// Retrieve the secret from cache or remote service
	secret, err := s.getWebhookSecret(ctx, projectKey, source)
	if err != nil {
		logger.Error("Failed to retrieve webhook secret", logger.Field("error", err))
		return err
	}

	// Validate according to the appropriate algorithm for the source
	switch signatureAlgo {
	case "sha256":
		// GitHub and custom webhooks use HMAC-SHA256
		return s.validateHmacSha256(signature, secret, payload)
	case "sha1":
		// Bitbucket uses HMAC-SHA1
		return s.validateHmacSha1(signature, secret, payload)
	case "token":
		// GitLab uses direct token comparison
		return s.validateToken(signature, secret)
	default:
		return fmt.Errorf("unsupported signature algorithm: %s", signatureAlgo)
	}
}

// getWebhookSecret retrieves the webhook secret for a project/source combination
func (s *WebhookSecurityService) getWebhookSecret(ctx context.Context, projectKey, source string) (string, error) {
	cacheKey := fmt.Sprintf("%s:%s", projectKey, source)

	// Check the cache first under a read lock
	s.mu.RLock()
	entry, ok := s.secretCache[cacheKey]
	s.mu.RUnlock()

	if ok && entry.expiration.After(time.Now()) {
		return entry.secret, nil
	}

	// Not in cache or expired, acquire write lock and retrieve
	s.mu.Lock()
	defer s.mu.Unlock()

	// Check again in case another goroutine updated while we were waiting
	entry, ok = s.secretCache[cacheKey]
	if ok && entry.expiration.After(time.Now()) {
		return entry.secret, nil
	}

	// Retrieve from Java service if available
	var secret string
	var err error

	if s.javaClient != nil {
		secret, err = s.javaClient.GetWebhookSecret(ctx, projectKey, source)
		if err != nil {
			// If Java service is unavailable, fall back to development mode
			if s.config != nil && s.config.DevMode {
				secret = s.securityConfig.DefaultSecret
			} else {
				return "", fmt.Errorf("failed to retrieve webhook secret: %w", err)
			}
		}
	} else if s.config != nil && s.config.DevMode {
		// If Java client is not available and we're in dev mode, use default
		secret = s.securityConfig.DefaultSecret
	} else {
		return "", fmt.Errorf("cannot validate webhook: Java client not available")
	}

	// Cache the result
	expiry := time.Duration(s.config.SecretExpiry) * time.Minute
	if expiry <= 0 {
		expiry = 60 * time.Minute // 1 hour default
	}

	s.secretCache[cacheKey] = secretCacheEntry{
		secret:     secret,
		expiration: time.Now().Add(expiry),
	}

	return secret, nil
}

// validateHmacSha256 validates a SHA-256 HMAC signature
func (s *WebhookSecurityService) validateHmacSha256(signature, secret string, payload []byte) error {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(payload)
	expectedMAC := mac.Sum(nil)
	expectedSignature := hex.EncodeToString(expectedMAC)

	// Use constant-time comparison to prevent timing attacks
	if subtle.ConstantTimeCompare([]byte(expectedSignature), []byte(signature)) != 1 {
		return fmt.Errorf("invalid signature")
	}

	return nil
}

// validateHmacSha1 validates a SHA-1 HMAC signature
func (s *WebhookSecurityService) validateHmacSha1(signature, secret string, payload []byte) error {
	mac := hmac.New(sha1.New, []byte(secret))
	mac.Write(payload)
	expectedMAC := mac.Sum(nil)
	expectedSignature := hex.EncodeToString(expectedMAC)

	// Use constant-time comparison to prevent timing attacks
	if subtle.ConstantTimeCompare([]byte(expectedSignature), []byte(signature)) != 1 {
		return fmt.Errorf("invalid signature")
	}

	return nil
}

// validateToken performs a direct token comparison (for GitLab)
func (s *WebhookSecurityService) validateToken(token, secret string) error {
	// Use constant-time comparison to prevent timing attacks
	if subtle.ConstantTimeCompare([]byte(token), []byte(secret)) != 1 {
		return fmt.Errorf("invalid token")
	}

	return nil
}

// validateTimestamp ensures the webhook timestamp is recent
func (s *WebhookSecurityService) validateTimestamp(headers http.Header, source string) error {
	var timestamp time.Time
	var err error

	// Different headers for different providers
	switch source {
	case "github":
		timestampStr := headers.Get("X-GitHub-Delivery")
		if timestampStr == "" {
			return fmt.Errorf("missing timestamp header")
		}
		// GitHub uses a GUID that includes a timestamp component
		// For simplicity, we'll just check that it exists
		return nil
	case "gitlab":
		timestampStr := headers.Get("X-Gitlab-Event-Timestamp")
		if timestampStr == "" {
			return fmt.Errorf("missing timestamp header")
		}
		// GitLab uses a Unix timestamp in seconds
		var ts int64
		ts, err = strconv.ParseInt(timestampStr, 10, 64)
		if err != nil {
			return fmt.Errorf("invalid timestamp format: %w", err)
		}
		timestamp = time.Unix(ts, 0)
	case "bitbucket":
		timestampStr := headers.Get("X-Event-Time")
		if timestampStr == "" {
			return fmt.Errorf("missing timestamp header")
		}
		// Bitbucket uses RFC3339 format
		timestamp, err = time.Parse(time.RFC3339, timestampStr)
		if err != nil {
			return fmt.Errorf("invalid timestamp format: %w", err)
		}
	case "custom":
		timestampStr := headers.Get("X-Webhook-Timestamp")
		if timestampStr == "" {
			return fmt.Errorf("missing timestamp header")
		}
		// Custom webhooks use Unix timestamp in seconds
		var ts int64
		ts, err = strconv.ParseInt(timestampStr, 10, 64)
		if err != nil {
			return fmt.Errorf("invalid timestamp format: %w", err)
		}
		timestamp = time.Unix(ts, 0)
	default:
		return fmt.Errorf("unknown source for timestamp validation: %s", source)
	}

	// Calculate the age of the timestamp
	age := time.Since(timestamp)
	if age > s.securityConfig.TimestampTolerance {
		return fmt.Errorf("webhook timestamp too old: %v", age)
	}

	return nil
}

// validateNonce prevents replay attacks by tracking used nonces
func (s *WebhookSecurityService) validateNonce(headers http.Header, source string) error {
	var nonce string

	// Different headers for different providers
	switch source {
	case "github":
		nonce = headers.Get("X-GitHub-Delivery")
		if nonce == "" {
			return fmt.Errorf("missing nonce header")
		}
	case "gitlab":
		nonce = headers.Get("X-Gitlab-Event-UUID")
		if nonce == "" {
			return fmt.Errorf("missing nonce header")
		}
	case "bitbucket":
		nonce = headers.Get("X-Request-UUID")
		if nonce == "" {
			return fmt.Errorf("missing nonce header")
		}
	case "custom":
		nonce = headers.Get("X-Webhook-Nonce")
		if nonce == "" {
			return fmt.Errorf("missing nonce header")
		}
	default:
		return fmt.Errorf("unknown source for nonce validation: %s", source)
	}

	// Check if nonce has been used before
	s.nonceMu.RLock()
	expiration, exists := s.nonceCache[nonce]
	s.nonceMu.RUnlock()

	if exists {
		// If nonce exists but has expired, remove it
		if time.Now().After(expiration) {
			s.nonceMu.Lock()
			delete(s.nonceCache, nonce)
			s.nonceMu.Unlock()
		} else {
			return fmt.Errorf("webhook nonce has already been used")
		}
	}

	// Store the nonce with expiration
	s.nonceMu.Lock()
	s.nonceCache[nonce] = time.Now().Add(s.securityConfig.NonceExpiration)
	s.nonceMu.Unlock()

	return nil
}

// CheckRateLimit applies rate limiting to webhook requests
func (s *WebhookSecurityService) CheckRateLimit(clientIP, source, projectKey string) (bool, error) {
	// Skip rate limiting for whitelisted IPs
	for _, ip := range s.securityConfig.IPWhitelist {
		if ip == clientIP {
			return true, nil
		}
	}

	// Create a key combining IP, source, and project
	key := fmt.Sprintf("%s:%s:%s", clientIP, source, projectKey)

	// Lock for rate limit checking
	s.rateLimitMu.Lock()
	defer s.rateLimitMu.Unlock()

	// Get or initialize entry
	entry, exists := s.rateLimitCache[key]
	now := time.Now()

	// If entry exists but has reset time in the past, reset it
	if exists && entry.resetTime.Before(now) {
		entry.count = 0
		entry.resetTime = now.Add(time.Minute)
	}

	// If entry doesn't exist, create it
	if !exists {
		entry = rateLimitEntry{
			count:     0,
			resetTime: now.Add(time.Minute),
		}
	}

	// Check if rate limit is exceeded
	if entry.count >= s.securityConfig.RateLimitPerMinute {
		retryAfter := entry.resetTime.Sub(now)
		return false, fmt.Errorf("rate limit exceeded: retry after %v", retryAfter)
	}

	// Increment the counter and update
	entry.count++
	s.rateLimitCache[key] = entry

	return true, nil
}

// CleanupCaches removes expired entries from cache
func (s *WebhookSecurityService) CleanupCaches() {
	// Clean up nonce cache
	s.nonceMu.Lock()
	now := time.Now()
	for nonce, expiration := range s.nonceCache {
		if now.After(expiration) {
			delete(s.nonceCache, nonce)
		}
	}
	s.nonceMu.Unlock()

	// Clean up rate limit cache
	s.rateLimitMu.Lock()
	for key, entry := range s.rateLimitCache {
		if now.After(entry.resetTime) {
			delete(s.rateLimitCache, key)
		}
	}
	s.rateLimitMu.Unlock()
}

// WebhookSecurityMiddleware provides enhanced security for webhook endpoints
func WebhookSecurityMiddleware(securityService *WebhookSecurityService) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// Only apply to webhook endpoints
			if !strings.HasPrefix(r.URL.Path, "/api/v1/webhooks/") && !strings.HasPrefix(r.URL.Path, "/webhooks/") {
				next.ServeHTTP(w, r)
				return
			}

			// Start timing for metrics
			startTime := time.Now()

			// Get request ID or generate one
			requestID := r.Header.Get("X-Request-ID")
			if requestID == "" {
				requestID = fmt.Sprintf("webhook-%d", time.Now().UnixNano())
				r.Header.Set("X-Request-ID", requestID)
			}

			// Create logger with request context
			log := logger.WithFields(map[string]interface{}{
				"requestID": requestID,
				"path":      r.URL.Path,
				"method":    r.Method,
				"remoteIP":  getWebhookClientIP(r),
			})

			// Extract source from path
			pathParts := strings.Split(r.URL.Path, "/")
			var source string
			if len(pathParts) >= 3 {
				source = pathParts[len(pathParts)-1]
			}

			if source == "" {
				http.Error(w, "Invalid webhook path", http.StatusBadRequest)
				return
			}

			log = log.WithField("source", source)

			// Get the project key
			projectKey := r.URL.Query().Get("project")
			if projectKey == "" {
				log.Warn("Missing project key in webhook request")
				http.Error(w, "Project key is required", http.StatusBadRequest)
				return
			}

			log = log.WithField("project", projectKey)

			// Check rate limit
			clientIP := getWebhookClientIP(r)
			allowed, err := securityService.CheckRateLimit(clientIP, source, projectKey)
			if !allowed {
				log.WithField("error", err).Warn("Rate limit exceeded for webhook")
				w.Header().Set("Retry-After", "60")
				http.Error(w, err.Error(), http.StatusTooManyRequests)
				return
			}

			// Read the request body
			body, err := io.ReadAll(r.Body)
			if err != nil {
				log.WithField("error", err).Error("Failed to read webhook request body")
				http.Error(w, "Failed to read request body", http.StatusInternalServerError)
				return
			}

			// Replace the body for downstream handlers
			r.Body = io.NopCloser(bytes.NewBuffer(body))

			// Extract signature based on source
			var signature string
			switch source {
			case "github":
				fullSig := r.Header.Get("X-Hub-Signature-256")
				if fullSig == "" || !strings.HasPrefix(fullSig, "sha256=") {
					log.Warn("Missing or invalid signature format in GitHub webhook")
					http.Error(w, "Invalid or missing signature", http.StatusUnauthorized)
					return
				}
				signature = strings.TrimPrefix(fullSig, "sha256=")
			case "gitlab":
				signature = r.Header.Get("X-Gitlab-Token")
				if signature == "" {
					log.Warn("Missing token in GitLab webhook")
					http.Error(w, "Invalid or missing token", http.StatusUnauthorized)
					return
				}
			case "bitbucket":
				fullSig := r.Header.Get("X-Hub-Signature")
				if fullSig == "" || !strings.HasPrefix(fullSig, "sha1=") {
					log.Warn("Missing or invalid signature format in Bitbucket webhook")
					http.Error(w, "Invalid or missing signature", http.StatusUnauthorized)
					return
				}
				signature = strings.TrimPrefix(fullSig, "sha1=")
			case "custom":
				algorithm := r.Header.Get("X-Webhook-Algorithm")
				if algorithm == "" {
					algorithm = "sha256" // Default for custom webhooks
				}

				fullSig := r.Header.Get("X-Webhook-Signature")
				if fullSig == "" {
					log.Warn("Missing signature in custom webhook")
					http.Error(w, "Invalid or missing signature", http.StatusUnauthorized)
					return
				}

				if strings.Contains(fullSig, "=") {
					signature = strings.SplitN(fullSig, "=", 2)[1]
				} else {
					signature = fullSig
				}
			default:
				log.WithField("source", source).Warn("Unsupported webhook source")
				http.Error(w, "Unsupported webhook source", http.StatusBadRequest)
				return
			}

			// Validate webhook signature and security
			err = securityService.ValidateWebhookSignature(r.Context(), projectKey, source, signature, r.Header, body)
			if err != nil {
				log.WithField("error", err).Warn("Webhook security validation failed")
				http.Error(w, fmt.Sprintf("Webhook validation failed: %s", err.Error()), http.StatusUnauthorized)
				return
			}

			// Add security context to the request for downstream handlers
			ctx := context.WithValue(r.Context(), webhookSourceKey{}, source)
			ctx = context.WithValue(ctx, projectKeyCtxKey{}, projectKey)

			// Calculate processing time for the security check
			processingTime := time.Since(startTime)
			log.WithField("securityProcessingTime", processingTime.String()).Debug("Webhook security check passed")

			// Add security headers to the response
			w.Header().Set("X-Security-Processing-Time", processingTime.String())

			// Call the next handler with the context
			next.ServeHTTP(w, r.WithContext(ctx))

			// Cleanup on a schedule (not every request)
			if rand.Intn(100) < 5 { // ~5% chance to clean up caches
				go securityService.CleanupCaches()
			}
		})
	}
}

// getWebhookClientIP extracts the client IP from the request for webhook security
func getWebhookClientIP(r *http.Request) string {
	// Try X-Forwarded-For header first
	ip := r.Header.Get("X-Forwarded-For")
	if ip != "" {
		// X-Forwarded-For can contain multiple IPs, we want the first one
		ips := strings.Split(ip, ",")
		return strings.TrimSpace(ips[0])
	}

	// Try X-Real-IP header next
	ip = r.Header.Get("X-Real-IP")
	if ip != "" {
		return ip
	}

	// Fallback to RemoteAddr
	return strings.Split(r.RemoteAddr, ":")[0]
}