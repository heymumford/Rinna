/*
 * Security logging middleware for the Rinna API server
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package middleware

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/heymumford/rinna/go/pkg/logger"
)

// SecurityContext holds security-related information for a request
type SecurityContext struct {
	RequestID     string                 `json:"request_id"`
	IP            string                 `json:"ip"`
	UserAgent     string                 `json:"user_agent"`
	Method        string                 `json:"method"`
	Path          string                 `json:"path"`
	UserID        string                 `json:"user_id,omitempty"`
	ProjectID     string                 `json:"project_id,omitempty"`
	TokenType     string                 `json:"token_type,omitempty"`
	TokenScope    string                 `json:"token_scope,omitempty"`
	WebhookSource string                 `json:"webhook_source,omitempty"`
	StartTime     time.Time              `json:"start_time"`
	Duration      time.Duration          `json:"duration,omitempty"`
	StatusCode    int                    `json:"status_code,omitempty"`
	ErrorMessage  string                 `json:"error_message,omitempty"`
	Parameters    map[string]interface{} `json:"parameters,omitempty"`
	Sensitive     bool                   `json:"sensitive"`
}

// securityContextKey is the context key for security context
type securityContextKey struct{}

// responseWriter wraps http.ResponseWriter to capture the status code
type responseWriter struct {
	http.ResponseWriter
	statusCode int
	body       *bytes.Buffer
}

// newResponseWriter creates a new responseWriter
func newResponseWriter(w http.ResponseWriter) *responseWriter {
	return &responseWriter{
		ResponseWriter: w,
		statusCode:     http.StatusOK,
		body:           &bytes.Buffer{},
	}
}

// WriteHeader captures the status code
func (rw *responseWriter) WriteHeader(code int) {
	rw.statusCode = code
	rw.ResponseWriter.WriteHeader(code)
}

// Write captures response body
func (rw *responseWriter) Write(b []byte) (int, error) {
	// Only capture the first 1024 bytes of the body for security logs
	if rw.body.Len() < 1024 {
		if remaining := 1024 - rw.body.Len(); remaining > 0 {
			if len(b) <= remaining {
				rw.body.Write(b)
			} else {
				rw.body.Write(b[:remaining])
			}
		}
	}
	return rw.ResponseWriter.Write(b)
}

// GetSecurityContext gets the security context from the context
func GetSecurityContext(ctx context.Context) *SecurityContext {
	if sc, ok := ctx.Value(securityContextKey{}).(*SecurityContext); ok {
		return sc
	}
	return nil
}

// generateRequestID generates a unique request ID
func generateRequestID() string {
	return uuid.New().String()
}

// shouldRedactPath checks if a path contains sensitive information that should be redacted
func shouldRedactPath(path string) bool {
	// Redact paths that might contain sensitive information in URL segments
	sensitivePatterns := []string{
		"/auth/",
		"/oauth/",
		"/token/",
		"/secret/",
		"/password/",
		"/credential/",
	}

	for _, pattern := range sensitivePatterns {
		if strings.Contains(path, pattern) {
			return true
		}
	}

	return false
}

// redactSensitiveData redacts sensitive data in query parameters and headers
func redactSensitiveData(r *http.Request) map[string]interface{} {
	// Copy and redact query parameters
	redactedParams := make(map[string]interface{})

	// Sensitive parameter names to redact
	sensitiveParams := []string{
		"token", "password", "secret", "api_key", "key", "auth", "credential",
		"access_token", "refresh_token", "id_token", "session",
	}

	// Process query parameters
	query := r.URL.Query()
	for key, values := range query {
		// Check if this is a sensitive parameter
		isSensitive := false
		for _, sensitive := range sensitiveParams {
			if strings.Contains(strings.ToLower(key), sensitive) {
				isSensitive = true
				break
			}
		}

		if isSensitive {
			// Redact sensitive parameter values
			redactedValues := make([]string, len(values))
			for i, val := range values {
				if len(val) > 8 {
					// Hash the value to maintain traceability while protecting sensitive data
					hash := sha256.Sum256([]byte(val))
					hashStr := hex.EncodeToString(hash[:])[:8]
					redactedValues[i] = fmt.Sprintf("%s...%s", val[:2], hashStr)
				} else {
					redactedValues[i] = "***"
				}
			}
			redactedParams[key] = redactedValues
		} else {
			// Keep non-sensitive parameters as is
			if len(values) == 1 {
				redactedParams[key] = values[0]
			} else {
				redactedParams[key] = values
			}
		}
	}

	// Redacted headers
	redactedHeaders := make(map[string]string)
	sensitiveHeaders := []string{
		"authorization", "cookie", "x-api-key", "proxy-authorization",
		"x-forwarded-authorization", "x-token", "token",
	}

	for _, header := range sensitiveHeaders {
		if value := r.Header.Get(header); value != "" {
			if len(value) > 8 {
				redactedHeaders[header] = fmt.Sprintf("%s...%s", value[:8], "[REDACTED]")
			} else {
				redactedHeaders[header] = "[REDACTED]"
			}
		}
	}

	// Add safe headers
	safeHeaders := []string{
		"user-agent", "content-type", "accept", "origin", "referer",
		"x-requested-with", "x-correlation-id", "x-request-id",
	}

	for _, header := range safeHeaders {
		if value := r.Header.Get(header); value != "" {
			redactedHeaders[header] = value
		}
	}

	if len(redactedHeaders) > 0 {
		redactedParams["headers"] = redactedHeaders
	}

	// Check for content type to handle form data
	contentType := r.Header.Get("Content-Type")
	if strings.Contains(contentType, "application/x-www-form-urlencoded") ||
		strings.Contains(contentType, "multipart/form-data") {
		// Parse form data
		if err := r.ParseForm(); err == nil {
			formData := make(map[string]interface{})
			for key, values := range r.Form {
				// Check if sensitive
				isSensitive := false
				for _, sensitive := range sensitiveParams {
					if strings.Contains(strings.ToLower(key), sensitive) {
						isSensitive = true
						break
					}
				}

				if isSensitive {
					// Redact sensitive form values
					if len(values) == 1 {
						formData[key] = "[REDACTED]"
					} else {
						redactedValues := make([]string, len(values))
						for i := range values {
							redactedValues[i] = "[REDACTED]"
						}
						formData[key] = redactedValues
					}
				} else {
					// Keep non-sensitive form values
					if len(values) == 1 {
						formData[key] = values[0]
					} else {
						formData[key] = values
					}
				}
			}
			if len(formData) > 0 {
				redactedParams["form"] = formData
			}
		}
	}

	return redactedParams
}

// SecurityLogging middleware logs security-related information for requests
func SecurityLogging(logger *logger.Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			startTime := time.Now()
			requestID := generateRequestID()

			// Create security context
			sc := &SecurityContext{
				RequestID: requestID,
				IP:        getClientIP(r),
				UserAgent: r.UserAgent(),
				Method:    r.Method,
				Path:      r.URL.Path,
				StartTime: startTime,
				Sensitive: shouldRedactPath(r.URL.Path),
			}

			// Add request ID header
			w.Header().Set("X-Request-ID", requestID)

			// Extract token claims from context if available
			if claims := GetTokenClaims(r.Context()); claims != nil {
				sc.UserID = claims.UserID
				sc.ProjectID = claims.ProjectID
				sc.TokenType = claims.TokenType
				sc.TokenScope = claims.Scope
			} else {
				// Try to get project ID directly
				sc.ProjectID = GetProjectID(r.Context())
			}

			// Get webhook source if applicable
			sc.WebhookSource = GetWebhookSource(r.Context())

			// Add security context to request context
			ctx := context.WithValue(r.Context(), securityContextKey{}, sc)

			// Process and redact request parameters
			sc.Parameters = redactSensitiveData(r)

			// Create response wrapper to capture status code
			rw := newResponseWriter(w)

			// Log request start for sensitive operations
			if sc.Sensitive || strings.Contains(r.URL.Path, "/auth/") || 
			   strings.Contains(r.URL.Path, "/oauth/") || 
			   strings.Contains(r.URL.Path, "/token/") {
				logger.Info("Security sensitive request started", map[string]interface{}{
					"request_id":     sc.RequestID,
					"ip":             sc.IP,
					"method":         sc.Method,
					"path":           sc.Path,
					"project_id":     sc.ProjectID,
					"token_type":     sc.TokenType,
					"webhook_source": sc.WebhookSource,
					"sensitive":      sc.Sensitive,
				})
			}

			// Handle the request
			next.ServeHTTP(rw, r.WithContext(ctx))

			// Update security context with response information
			sc.Duration = time.Since(startTime)
			sc.StatusCode = rw.statusCode

			// Determine log level based on status code
			logLevel := "info"
			if rw.statusCode >= 400 && rw.statusCode < 500 {
				logLevel = "warn"
				// Capture error message for client errors
				if rw.body.Len() > 0 {
					sc.ErrorMessage = rw.body.String()
				}
			} else if rw.statusCode >= 500 {
				logLevel = "error"
				// Capture error message for server errors
				if rw.body.Len() > 0 {
					sc.ErrorMessage = rw.body.String()
				}
			}

			// Prepare log details
			logDetails := map[string]interface{}{
				"request_id":     sc.RequestID,
				"ip":             sc.IP,
				"user_agent":     sc.UserAgent,
				"method":         sc.Method,
				"path":           sc.Path,
				"status_code":    sc.StatusCode,
				"duration_ms":    sc.Duration.Milliseconds(),
				"project_id":     sc.ProjectID,
				"user_id":        sc.UserID,
				"token_type":     sc.TokenType,
				"token_scope":    sc.TokenScope,
				"webhook_source": sc.WebhookSource,
				"sensitive":      sc.Sensitive,
			}

			// Add error message if present
			if sc.ErrorMessage != "" {
				if len(sc.ErrorMessage) > 500 {
					logDetails["error"] = sc.ErrorMessage[:500] + "..."
				} else {
					logDetails["error"] = sc.ErrorMessage
				}
			}

			// Add parameters for elevated status codes or sensitive operations
			if rw.statusCode >= 400 || sc.Sensitive {
				logDetails["parameters"] = sc.Parameters
			}

			// Log the request completion
			switch logLevel {
			case "warn":
				logger.Warn("API request completed with warning", logDetails)
			case "error":
				logger.Error("API request failed", logDetails)
			default:
				// For successful requests, only log details for sensitive operations
				if sc.Sensitive || strings.Contains(r.URL.Path, "/auth/") || 
				   strings.Contains(r.URL.Path, "/oauth/") || 
				   strings.Contains(r.URL.Path, "/token/") {
					logger.Info("Security sensitive request completed", logDetails)
				} else {
					// For normal successful requests, log minimal information
					minimalDetails := map[string]interface{}{
						"request_id":  sc.RequestID,
						"method":      sc.Method,
						"path":        sc.Path,
						"status_code": sc.StatusCode,
						"duration_ms": sc.Duration.Milliseconds(),
						"project_id":  sc.ProjectID,
					}
					logger.Debug("API request completed", minimalDetails)
				}
			}
		})
	}
}

// ExtractBodyMiddleware extracts and makes the request body available for multiple reads
func ExtractBodyMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Skip for GET, HEAD, DELETE requests which typically don't have a body
		if r.Method == http.MethodGet || r.Method == http.MethodHead || r.Method == http.MethodDelete {
			next.ServeHTTP(w, r)
			return
		}

		// Read the body
		bodyBytes, err := io.ReadAll(r.Body)
		if err != nil {
			http.Error(w, "Failed to read request body", http.StatusBadRequest)
			return
		}

		// Close the original body
		r.Body.Close()

		// Create a new ReadCloser from the bytes
		r.Body = io.NopCloser(bytes.NewBuffer(bodyBytes))

		// Create a copy for the security context
		bodyCopy := make([]byte, len(bodyBytes))
		copy(bodyCopy, bodyBytes)

		// Get security context
		if sc := GetSecurityContext(r.Context()); sc != nil {
			// Add body hash to context for traceability
			hash := sha256.Sum256(bodyCopy)
			if sc.Parameters == nil {
				sc.Parameters = make(map[string]interface{})
			}
			sc.Parameters["body_hash"] = hex.EncodeToString(hash[:])

			// For small bodies of non-sensitive operations, include truncated content
			if len(bodyCopy) <= 1024 && !sc.Sensitive {
				contentType := r.Header.Get("Content-Type")
				if strings.Contains(contentType, "application/json") {
					sc.Parameters["body_preview"] = string(bodyCopy)
				}
			}
		}

		// Call the next handler
		next.ServeHTTP(w, r)
	})
}