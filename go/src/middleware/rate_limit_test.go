/*
 * Tests for rate limiting middleware
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package middleware

import (
	"net/http"
	"net/http/httptest"
	"strconv"
	"testing"
	"time"

	"github.com/heymumford/rinna/go/pkg/config"
	"github.com/heymumford/rinna/go/pkg/logger"
)

func TestRateLimiter(t *testing.T) {
	// Create test logger
	log := logger.New()

	// Create test rate limiter with low limits for testing
	cfg := &config.RateLimitConfig{
		Enabled:      true,
		DefaultLimit: 5, // 5 requests per minute for testing
		IPWhitelist:  []string{"192.168.1.100", "10.0.0.0/24"},
	}
	rateLimiter := NewRateLimiter(cfg, log)

	// Create a test handler that always succeeds
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	// Create middleware handler
	middlewareHandler := RateLimit(rateLimiter)(nextHandler)

	// Test case: Normal request should succeed
	t.Run("NormalRequest", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/test", nil)
		req.RemoteAddr = "192.168.1.101:1234" // Not in whitelist
		
		// Make a few requests (below the limit)
		for i := 0; i < 3; i++ {
			recorder := httptest.NewRecorder()
			middlewareHandler.ServeHTTP(recorder, req)
			
			// Check response
			if recorder.Code != http.StatusOK {
				t.Errorf("Expected status code %d, got %d", http.StatusOK, recorder.Code)
			}
			
			// Check rate limit headers
			limit := recorder.Header().Get("X-RateLimit-Limit")
			remaining := recorder.Header().Get("X-RateLimit-Remaining")
			
			if limit != "5" {
				t.Errorf("Expected X-RateLimit-Limit to be 5, got %s", limit)
			}
			
			remainingVal, _ := strconv.Atoi(remaining)
			expectedRemaining := 5 - (i + 1)
			if remainingVal != expectedRemaining {
				t.Errorf("Expected X-RateLimit-Remaining to be %d, got %d", expectedRemaining, remainingVal)
			}
		}
	})

	// Test case: Exceeding rate limit should get 429 Too Many Requests
	t.Run("ExceedRateLimit", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/test", nil)
		req.RemoteAddr = "192.168.1.102:1234" // Not in whitelist
		
		// Make requests up to the limit
		for i := 0; i < 5; i++ {
			recorder := httptest.NewRecorder()
			middlewareHandler.ServeHTTP(recorder, req)
			
			// All should succeed
			if recorder.Code != http.StatusOK {
				t.Errorf("Expected status code %d, got %d", http.StatusOK, recorder.Code)
			}
		}
		
		// One more request should fail with 429
		recorder := httptest.NewRecorder()
		middlewareHandler.ServeHTTP(recorder, req)
		
		if recorder.Code != http.StatusTooManyRequests {
			t.Errorf("Expected status code %d, got %d", http.StatusTooManyRequests, recorder.Code)
		}
		
		// Check Retry-After header
		retryAfter := recorder.Header().Get("Retry-After")
		if retryAfter != "60" {
			t.Errorf("Expected Retry-After to be 60, got %s", retryAfter)
		}
	})

	// Test case: Whitelisted IPs should not be rate limited
	t.Run("WhitelistedIP", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/test", nil)
		req.RemoteAddr = "192.168.1.100:1234" // In whitelist
		
		// Make many requests (above the limit)
		for i := 0; i < 10; i++ {
			recorder := httptest.NewRecorder()
			middlewareHandler.ServeHTTP(recorder, req)
			
			// All should succeed
			if recorder.Code != http.StatusOK {
				t.Errorf("Expected status code %d, got %d", http.StatusOK, recorder.Code)
			}
			
			// Check whitelisted header
			whitelist := recorder.Header().Get("X-RateLimit-Whitelisted")
			if whitelist != "true" {
				t.Errorf("Expected X-RateLimit-Whitelisted to be true, got %s", whitelist)
			}
		}
	})

	// Test case: CIDR whitelisting
	t.Run("CIDRWhitelisting", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/api/test", nil)
		req.RemoteAddr = "10.0.0.5:1234" // In CIDR whitelist
		
		// Make many requests (above the limit)
		for i := 0; i < 10; i++ {
			recorder := httptest.NewRecorder()
			middlewareHandler.ServeHTTP(recorder, req)
			
			// All should succeed
			if recorder.Code != http.StatusOK {
				t.Errorf("Expected status code %d, got %d", http.StatusOK, recorder.Code)
			}
			
			// Check whitelisted header
			whitelist := recorder.Header().Get("X-RateLimit-Whitelisted")
			if whitelist != "true" {
				t.Errorf("Expected X-RateLimit-Whitelisted to be true, got %s", whitelist)
			}
		}
	})

	// Test case: Different endpoints should have separate limits
	t.Run("DifferentEndpoints", func(t *testing.T) {
		// First endpoint
		req1 := httptest.NewRequest("GET", "/api/endpoint1", nil)
		req1.RemoteAddr = "192.168.1.103:1234"
		
		// Second endpoint
		req2 := httptest.NewRequest("GET", "/api/endpoint2", nil)
		req2.RemoteAddr = "192.168.1.103:1234" // Same IP
		
		// Make requests up to the limit for first endpoint
		for i := 0; i < 5; i++ {
			recorder := httptest.NewRecorder()
			middlewareHandler.ServeHTTP(recorder, req1)
			
			// All should succeed
			if recorder.Code != http.StatusOK {
				t.Errorf("Expected status code %d, got %d for endpoint1", http.StatusOK, recorder.Code)
			}
		}
		
		// One more request to first endpoint should fail
		recorder := httptest.NewRecorder()
		middlewareHandler.ServeHTTP(recorder, req1)
		
		if recorder.Code != http.StatusTooManyRequests {
			t.Errorf("Expected status code %d, got %d for endpoint1 over limit", 
				http.StatusTooManyRequests, recorder.Code)
		}
		
		// Requests to second endpoint should still succeed
		for i := 0; i < 5; i++ {
			recorder := httptest.NewRecorder()
			middlewareHandler.ServeHTTP(recorder, req2)
			
			// All should succeed
			if recorder.Code != http.StatusOK {
				t.Errorf("Expected status code %d, got %d for endpoint2", http.StatusOK, recorder.Code)
			}
		}
		
		// One more request to second endpoint should fail
		recorder = httptest.NewRecorder()
		middlewareHandler.ServeHTTP(recorder, req2)
		
		if recorder.Code != http.StatusTooManyRequests {
			t.Errorf("Expected status code %d, got %d for endpoint2 over limit", 
				http.StatusTooManyRequests, recorder.Code)
		}
	})

	// Test case: Cleanup removes old entries
	t.Run("Cleanup", func(t *testing.T) {
		// Set a very short cleanup interval for testing
		originalCleanupInterval := rateLimiter.cleanupInterval
		rateLimiter.cleanupInterval = 200 * time.Millisecond
		defer func() {
			rateLimiter.cleanupInterval = originalCleanupInterval
		}()
		
		req := httptest.NewRequest("GET", "/api/cleanup-test", nil)
		req.RemoteAddr = "192.168.1.104:1234"
		
		// Make some requests
		for i := 0; i < 5; i++ {
			recorder := httptest.NewRecorder()
			middlewareHandler.ServeHTTP(recorder, req)
		}
		
		// Verify we've hit the limit
		recorder := httptest.NewRecorder()
		middlewareHandler.ServeHTTP(recorder, req)
		
		if recorder.Code != http.StatusTooManyRequests {
			t.Errorf("Expected status code %d, got %d", http.StatusTooManyRequests, recorder.Code)
		}
		
		// Manually force cleanup
		rateLimiter.cleanup()
		
		// After cleanup, request should succeed again
		recorder = httptest.NewRecorder()
		middlewareHandler.ServeHTTP(recorder, req)
		
		if recorder.Code != http.StatusOK {
			t.Errorf("Expected status code %d after cleanup, got %d", http.StatusOK, recorder.Code)
		}
	})
}