/*
 * Tests for security logging middleware
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
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/go/pkg/logger"
)

// loggerRecorder captures log output for testing
type loggerRecorder struct {
	buf    *bytes.Buffer
	logger *logger.Logger
}

// newLoggerRecorder creates a new logger that records its output
func newLoggerRecorder() *loggerRecorder {
	buf := &bytes.Buffer{}
	cfg := logger.DefaultConfig()
	cfg.Level = logger.DebugLevel
	
	l := &logger.Logger{
		Writer:    buf,
		Fields:    make(map[string]interface{}),
		StdLogger: log.New(buf, "", 0),
		Config:    cfg,
	}
	
	return &loggerRecorder{
		buf:    buf,
		logger: l,
	}
}

// getLogs returns all logged messages
func (lr *loggerRecorder) getLogs() []string {
	return strings.Split(lr.buf.String(), "\n")
}

// countLogsByLevel counts logs with the given level
func (lr *loggerRecorder) countLogsByLevel(level string) int {
	logs := lr.getLogs()
	count := 0
	for _, log := range logs {
		if strings.Contains(log, "["+level+"]") {
			count++
		}
	}
	return count
}

func TestSecurityLoggingMiddleware(t *testing.T) {
	// Create a test router
	router := mux.NewRouter()
	
	// Create a log recorder
	lr := newLoggerRecorder()
	
	// Apply the security logging middleware
	router.Use(func(next http.Handler) http.Handler {
		return SecurityLogging(lr.logger)(next)
	})
	
	// Add some test routes
	router.HandleFunc("/public", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("public"))
	}).Methods("GET")
	
	router.HandleFunc("/error", func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "internal server error", http.StatusInternalServerError)
	}).Methods("GET")
	
	router.HandleFunc("/auth/token", func(w http.ResponseWriter, r *http.Request) {
		sc := GetSecurityContext(r.Context())
		if sc == nil {
			t.Error("Security context should be set")
		}
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("token"))
	}).Methods("GET")
	
	router.HandleFunc("/post-endpoint", func(w http.ResponseWriter, r *http.Request) {
		sc := GetSecurityContext(r.Context())
		if sc == nil {
			t.Error("Security context should be set")
		}
		
		// Read body to ensure it was properly restored
		body, err := io.ReadAll(r.Body)
		if err != nil {
			t.Errorf("Failed to read body: %v", err)
		}
		
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("received " + string(body)))
	}).Methods("POST")
	
	// Test case: Normal request to public endpoint
	t.Run("PublicEndpoint", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/public", nil)
		req.Header.Set("User-Agent", "TestAgent")
		req.RemoteAddr = "192.168.1.100:1234"
		
		// Clear log buffer
		lr.buf.Reset()
		
		// Make request
		recorder := httptest.NewRecorder()
		router.ServeHTTP(recorder, req)
		
		// Check response
		if recorder.Code != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, recorder.Code)
		}
		
		// Check logs
		logs := lr.getLogs()
		debugCount := lr.countLogsByLevel("DEBUG")
		
		// Public endpoints should only generate debug logs for successful requests
		if debugCount != 1 {
			t.Errorf("Expected 1 DEBUG log, got %d", debugCount)
		}
		
		// Check if request ID is in response headers
		if recorder.Header().Get("X-Request-ID") == "" {
			t.Error("Expected X-Request-ID header to be set")
		}
		
		// Check log content for user agent and IP
		logHasUserAgent := false
		logHasIP := false
		for _, log := range logs {
			if strings.Contains(log, "TestAgent") {
				logHasUserAgent = true
			}
			if strings.Contains(log, "192.168.1.100") {
				logHasIP = true
			}
		}
		
		if !logHasUserAgent {
			t.Error("Log should contain User-Agent")
		}
		
		if !logHasIP {
			t.Error("Log should contain IP address")
		}
	})
	
	// Test case: Error response
	t.Run("ErrorResponse", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/error", nil)
		req.RemoteAddr = "192.168.1.100:1234"
		
		// Clear log buffer
		lr.buf.Reset()
		
		// Make request
		recorder := httptest.NewRecorder()
		router.ServeHTTP(recorder, req)
		
		// Check response
		if recorder.Code != http.StatusInternalServerError {
			t.Errorf("Expected status code %d, got %d", http.StatusInternalServerError, recorder.Code)
		}
		
		// Check logs - should have an ERROR level log
		errorCount := lr.countLogsByLevel("ERROR")
		if errorCount != 1 {
			t.Errorf("Expected 1 ERROR log, got %d", errorCount)
		}
		
		// Error logs should include error message
		logs := lr.getLogs()
		errorLogHasMessage := false
		for _, log := range logs {
			if strings.Contains(log, "internal server error") {
				errorLogHasMessage = true
				break
			}
		}
		
		if !errorLogHasMessage {
			t.Error("ERROR log should contain error message")
		}
	})
	
	// Test case: Sensitive endpoint (auth)
	t.Run("SensitiveEndpoint", func(t *testing.T) {
		req := httptest.NewRequest("GET", "/auth/token", nil)
		req.RemoteAddr = "192.168.1.100:1234"
		
		// Add token to context
		ctx := context.WithValue(req.Context(), tokenKey{}, "test-token")
		ctx = context.WithValue(ctx, projectKey{}, "test-project")
		req = req.WithContext(ctx)
		
		// Clear log buffer
		lr.buf.Reset()
		
		// Make request
		recorder := httptest.NewRecorder()
		router.ServeHTTP(recorder, req)
		
		// Check response
		if recorder.Code != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, recorder.Code)
		}
		
		// Check logs - sensitive endpoints generate INFO logs
		infoCount := lr.countLogsByLevel("INFO")
		if infoCount != 2 { // One for request start, one for completion
			t.Errorf("Expected 2 INFO logs, got %d", infoCount)
		}
		
		// Check if logs contain "sensitive" marker
		logs := lr.getLogs()
		hasSensitiveMarker := false
		for _, log := range logs {
			if strings.Contains(log, "sensitive=true") {
				hasSensitiveMarker = true
				break
			}
		}
		
		if !hasSensitiveMarker {
			t.Error("Logs for sensitive endpoint should be marked as sensitive")
		}
	})
	
	// Test case: POST request with body
	t.Run("PostWithBody", func(t *testing.T) {
		// Create JSON body
		body := map[string]interface{}{
			"name":     "test",
			"password": "secret123",
			"data":     "payload",
		}
		
		bodyBytes, _ := json.Marshal(body)
		req := httptest.NewRequest("POST", "/post-endpoint", bytes.NewBuffer(bodyBytes))
		req.Header.Set("Content-Type", "application/json")
		req.RemoteAddr = "192.168.1.100:1234"
		
		// Calculate body hash for verification
		hash := sha256.Sum256(bodyBytes)
		expectedBodyHash := hex.EncodeToString(hash[:])
		
		// Clear log buffer
		lr.buf.Reset()
		
		// Make request
		recorder := httptest.NewRecorder()
		router.ServeHTTP(recorder, req)
		
		// Check response
		if recorder.Code != http.StatusOK {
			t.Errorf("Expected status code %d, got %d", http.StatusOK, recorder.Code)
		}
		
		// Check if body hash is in logs
		logs := lr.getLogs()
		hasBodyHash := false
		
		for _, log := range logs {
			if strings.Contains(log, expectedBodyHash) {
				hasBodyHash = true
				break
			}
		}
		
		if !hasBodyHash {
			t.Error("Logs should contain the body hash")
		}
		
		// Check if response contains the full body
		// This verifies that ExtractBodyMiddleware properly restored the body
		if !strings.Contains(recorder.Body.String(), "received {\"") {
			t.Errorf("Response should contain the body, got: %s", recorder.Body.String())
		}
		
		// Check for redaction of sensitive parameters
		hasRedactedPassword := false
		for _, log := range logs {
			if strings.Contains(log, "\"password\":") && !strings.Contains(log, "secret123") {
				hasRedactedPassword = true
				break
			}
		}
		
		if !hasRedactedPassword {
			t.Error("Password should be redacted in logs")
		}
	})
}