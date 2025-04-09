/*
 * HTTP middleware for the Rinna API server
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package middleware

import (
	"context"
	"net/http"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/heymumford/rinna/api/pkg/logger"
)

// RequestIDKey is the context key for the request ID
type requestIDKey struct{}

// Logging middleware logs HTTP requests
func Logging(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		
		// Call the next handler
		next.ServeHTTP(w, r)
		
		// Log the request with structured fields
		duration := time.Since(start)
		requestID := GetRequestID(r.Context())
		
		log := logger.WithFields(map[string]interface{}{
			"method":      r.Method,
			"uri":         r.RequestURI,
			"remote_addr": r.RemoteAddr,
			"duration":    duration.String(),
			"request_id":  requestID,
		})
		
		log.Info("HTTP Request")
	})
}

// RequestID middleware adds a unique request ID to each request
func RequestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Generate a UUID
		id := uuid.New().String()
		
		// Add the request ID to the response headers
		w.Header().Set("X-Request-ID", id)
		
		// Store the request ID in the context
		ctx := context.WithValue(r.Context(), requestIDKey{}, id)
		
		// Call the next handler with the updated context
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// Authentication middleware authenticates requests
// This is a simplified version that delegates to TokenAuthentication
// It's kept for backward compatibility
func Authentication(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
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
		
		// Validate the token (simplified for now)
		if !strings.HasPrefix(token, "ri-") {
			http.Error(w, "Invalid token format", http.StatusUnauthorized)
			return
		}
		
		// Add the token to the context
		ctx := context.WithValue(r.Context(), "token", token)
		
		// Call the next handler
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// GetRequestID returns the request ID from the context
func GetRequestID(ctx context.Context) string {
	if id, ok := ctx.Value(requestIDKey{}).(string); ok {
		return id
	}
	return ""
}

// CORS middleware handles Cross-Origin Resource Sharing
func CORS(allowedOrigins []string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			origin := r.Header.Get("Origin")
			
			// Check if the origin is allowed
			allowed := false
			for _, allowedOrigin := range allowedOrigins {
				if allowedOrigin == "*" || allowedOrigin == origin {
					allowed = true
					break
				}
			}
			
			if allowed {
				w.Header().Set("Access-Control-Allow-Origin", origin)
				w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
				w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
				w.Header().Set("Access-Control-Allow-Credentials", "true")
				w.Header().Set("Access-Control-Max-Age", "3600")
			}
			
			// Handle preflight requests
			if r.Method == "OPTIONS" {
				w.WriteHeader(http.StatusOK)
				return
			}
			
			// Call the next handler
			next.ServeHTTP(w, r)
		})
	}
}