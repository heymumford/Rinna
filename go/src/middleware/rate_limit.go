/*
 * Rate limiting middleware for the Rinna API server
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package middleware

import (
	"context"
	"fmt"
	"net"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/heymumford/rinna/go/pkg/config"
	"github.com/heymumford/rinna/go/pkg/logger"
)

// RateLimiter implements rate limiting functionality for API requests
type RateLimiter struct {
	config             *config.RateLimitConfig
	requestCounts      map[string]map[time.Time]int
	ipBasedLimits      map[string]int        // IP-specific rate limits
	projectBasedLimits map[string]int        // Project-specific rate limits
	pathBasedLimits    map[string]int        // Path-specific rate limits
	ipWhitelist        map[string]*net.IPNet // Whitelisted IPs/CIDRs
	mu                 sync.RWMutex
	cleanupInterval    time.Duration
	logger             *logger.Logger
}

// NewRateLimiter creates a new rate limiter with the given configuration
func NewRateLimiter(config *config.RateLimitConfig, logger *logger.Logger) *RateLimiter {
	rl := &RateLimiter{
		config:             config,
		requestCounts:      make(map[string]map[time.Time]int),
		ipBasedLimits:      make(map[string]int),
		projectBasedLimits: make(map[string]int),
		pathBasedLimits:    make(map[string]int),
		ipWhitelist:        make(map[string]*net.IPNet),
		cleanupInterval:    5 * time.Minute,
		logger:             logger,
	}

	// Initialize IP whitelist from configuration
	if config != nil && len(config.IPWhitelist) > 0 {
		for _, cidr := range config.IPWhitelist {
			_, ipNet, err := net.ParseCIDR(cidr)
			if err == nil {
				rl.ipWhitelist[cidr] = ipNet
			} else {
				// Try as a single IP
				ip := net.ParseIP(cidr)
				if ip != nil {
					mask := net.CIDRMask(32, 32)
					if ip.To4() == nil {
						// IPv6
						mask = net.CIDRMask(128, 128)
					}
					ipNet = &net.IPNet{
						IP:   ip,
						Mask: mask,
					}
					rl.ipWhitelist[cidr] = ipNet
				} else {
					logger.Warn("Invalid IP or CIDR in whitelist", map[string]interface{}{
						"cidr": cidr,
						"err":  err.Error(),
					})
				}
			}
		}
	}

	// Initialize custom limits
	if config != nil {
		// Custom IP-based limits
		for ip, limit := range config.CustomIPLimits {
			rl.ipBasedLimits[ip] = limit
		}

		// Custom project-based limits
		for project, limit := range config.CustomProjectLimits {
			rl.projectBasedLimits[project] = limit
		}

		// Custom path-based limits
		for path, limit := range config.CustomPathLimits {
			rl.pathBasedLimits[path] = limit
		}
	}

	// Start cleanup goroutine
	go rl.cleanupLoop()

	return rl
}

// cleanupLoop periodically cleans up old request counts
func (rl *RateLimiter) cleanupLoop() {
	ticker := time.NewTicker(rl.cleanupInterval)
	defer ticker.Stop()

	for range ticker.C {
		rl.cleanup()
	}
}

// cleanup removes request counts older than the rate limit window
func (rl *RateLimiter) cleanup() {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	// Get cutoff time (1 hour ago)
	cutoff := time.Now().Add(-1 * time.Hour)

	// Clean up each key's time buckets
	for key, timeBuckets := range rl.requestCounts {
		for t := range timeBuckets {
			if t.Before(cutoff) {
				delete(timeBuckets, t)
			}
		}

		// If all time buckets are gone, remove the key
		if len(timeBuckets) == 0 {
			delete(rl.requestCounts, key)
		}
	}
}

// isIPWhitelisted checks if an IP is in the whitelist
func (rl *RateLimiter) isIPWhitelisted(ip string) bool {
	parsedIP := net.ParseIP(ip)
	if parsedIP == nil {
		return false
	}

	rl.mu.RLock()
	defer rl.mu.RUnlock()

	for _, ipNet := range rl.ipWhitelist {
		if ipNet.Contains(parsedIP) {
			return true
		}
	}

	return false
}

// getClientIP extracts the client IP from the request
func getClientIP(r *http.Request) string {
	// Check X-Forwarded-For header
	xForwardedFor := r.Header.Get("X-Forwarded-For")
	if xForwardedFor != "" {
		// Use the first IP in the list
		ips := strings.Split(xForwardedFor, ",")
		return strings.TrimSpace(ips[0])
	}

	// Check X-Real-IP header
	xRealIP := r.Header.Get("X-Real-IP")
	if xRealIP != "" {
		return xRealIP
	}

	// Fall back to RemoteAddr
	ip, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		// If there's an error, just return the RemoteAddr as is
		return r.RemoteAddr
	}
	return ip
}

// getRateLimit determines the rate limit based on the request details
func (rl *RateLimiter) getRateLimit(r *http.Request) int {
	// Default rate limit
	defaultLimit := 300 // 300 requests per minute
	if rl.config != nil && rl.config.DefaultLimit > 0 {
		defaultLimit = rl.config.DefaultLimit
	}

	// Check for IP-specific limit
	clientIP := getClientIP(r)
	if limit, ok := rl.ipBasedLimits[clientIP]; ok {
		return limit
	}

	// Check for project-specific limit
	projectID := GetProjectID(r.Context())
	if projectID != "" {
		if limit, ok := rl.projectBasedLimits[projectID]; ok {
			return limit
		}
	}

	// Check for path-specific limit
	path := r.URL.Path
	// Check exact path
	if limit, ok := rl.pathBasedLimits[path]; ok {
		return limit
	}

	// Check path prefixes
	for p, limit := range rl.pathBasedLimits {
		if strings.HasSuffix(p, "/*") && strings.HasPrefix(path, p[:len(p)-2]) {
			return limit
		}
	}

	return defaultLimit
}

// generateRateLimitKey generates a key for rate limiting based on the request
func (rl *RateLimiter) generateRateLimitKey(r *http.Request) string {
	// Get client IP
	clientIP := getClientIP(r)

	// Get project ID from context
	projectID := GetProjectID(r.Context())

	// Use the first path component as an endpoint identifier
	pathParts := strings.Split(strings.Trim(r.URL.Path, "/"), "/")
	var endpoint string
	if len(pathParts) > 0 {
		endpoint = pathParts[0]
	} else {
		endpoint = "root"
	}

	// For API paths, include the second component if available
	if len(pathParts) > 2 && pathParts[0] == "api" {
		endpoint = fmt.Sprintf("%s-%s", pathParts[0], pathParts[1])
	}

	// Different key formats based on available information
	if projectID != "" {
		// Include project ID in the key
		return fmt.Sprintf("%s:%s:%s", clientIP, projectID, endpoint)
	}

	// Fallback to IP and endpoint only
	return fmt.Sprintf("%s:%s", clientIP, endpoint)
}

// incrementCounter increments the request counter for the current minute
func (rl *RateLimiter) incrementCounter(key string) int {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	// Get the current minute for bucketing
	now := time.Now().Truncate(time.Minute)

	// Initialize maps if needed
	if _, ok := rl.requestCounts[key]; !ok {
		rl.requestCounts[key] = make(map[time.Time]int)
	}

	// Increment counter for the current minute
	rl.requestCounts[key][now]++

	// Calculate total for the last minute
	total := 0
	for t, count := range rl.requestCounts[key] {
		if t.After(now.Add(-1 * time.Minute)) {
			total += count
		}
	}

	return total
}

// RateLimit middleware applies rate limiting to requests
func RateLimit(rateLimiter *RateLimiter) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// Skip rate limiting for OPTIONS requests (CORS preflight)
			if r.Method == http.MethodOptions {
				next.ServeHTTP(w, r)
				return
			}

			// Get client IP for logging and whitelisting
			clientIP := getClientIP(r)

			// Check if this IP is whitelisted
			if rateLimiter.isIPWhitelisted(clientIP) {
				// Add header to indicate this request was whitelisted
				w.Header().Set("X-RateLimit-Whitelisted", "true")
				next.ServeHTTP(w, r)
				return
			}

			// Generate key for this request
			key := rateLimiter.generateRateLimitKey(r)

			// Get the rate limit for this request
			limit := rateLimiter.getRateLimit(r)

			// Increment counter and get current count
			currentCount := rateLimiter.incrementCounter(key)

			// Add rate limit headers
			w.Header().Set("X-RateLimit-Limit", strconv.Itoa(limit))
			w.Header().Set("X-RateLimit-Remaining", strconv.Itoa(limit-currentCount))
			w.Header().Set("X-RateLimit-Reset", strconv.FormatInt(time.Now().Truncate(time.Minute).Add(time.Minute).Unix(), 10))

			// Check if rate limit is exceeded
			if currentCount > limit {
				// Rate limit exceeded
				w.Header().Set("Retry-After", "60") // Suggest retry after 1 minute
				w.Header().Set("X-RateLimit-Reset", strconv.FormatInt(time.Now().Truncate(time.Minute).Add(time.Minute).Unix(), 10))

				// Log rate limit exceeded
				rateLimiter.logger.Warn("Rate limit exceeded", map[string]interface{}{
					"ip":        clientIP,
					"key":       key,
					"limit":     limit,
					"count":     currentCount,
					"path":      r.URL.Path,
					"method":    r.Method,
					"projectID": GetProjectID(r.Context()),
				})

				http.Error(w, "Rate limit exceeded", http.StatusTooManyRequests)
				return
			}

			// If we got here, the request is allowed
			next.ServeHTTP(w, r)
		})
	}
}

// Add logging context for rate limiting
type rateLimitKey struct{}

// GetRateLimitInfo gets rate limit info from context
func GetRateLimitInfo(ctx context.Context) map[string]interface{} {
	if info, ok := ctx.Value(rateLimitKey{}).(map[string]interface{}); ok {
		return info
	}
	return nil
}

// withRateLimitInfo adds rate limit info to context
func withRateLimitInfo(ctx context.Context, info map[string]interface{}) context.Context {
	return context.WithValue(ctx, rateLimitKey{}, info)
}