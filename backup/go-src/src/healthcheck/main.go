/*
 * Standalone health check API server for testing
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package main

import (
	"context"
	"flag"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/pkg/health"
	"github.com/heymumford/rinna/api/pkg/logger"
)

func main() {
	// Parse command line flags
	host := flag.String("host", "localhost", "Server host")
	port := flag.Int("port", 8080, "Server port")
	logLevel := flag.String("log-level", "INFO", "Log level (TRACE, DEBUG, INFO, WARN, ERROR)")
	flag.Parse()
	
	// Configure logger
	level := logger.InfoLevel
	switch *logLevel {
	case "TRACE":
		level = logger.TraceLevel
	case "DEBUG":
		level = logger.DebugLevel
	case "WARN":
		level = logger.WarnLevel
	case "ERROR":
		level = logger.ErrorLevel
	}
	
	// Setup logging
	logDir := os.Getenv("RINNA_LOG_DIR")
	if logDir == "" {
		homeDir, _ := os.UserHomeDir()
		logDir = homeDir + "/.rinna/logs"
	}
	
	// Ensure log directory exists
	os.MkdirAll(logDir, 0755)
	
	logger.Configure(logger.Config{
		Level:      level,
		TimeFormat: time.RFC3339,
		LogFile:    logDir + "/rinna-healthcheck.log",
		ShowCaller: true,
	})

	// Create router
	router := mux.NewRouter()

	// Create health checker
	javaChecker := &health.JavaServiceChecker{}

	// Create health handler
	healthHandler := health.NewHandler(javaChecker)

	// Register health routes
	healthHandler.RegisterRoutes(router)

	// Add basic middleware
	router.Use(loggingMiddleware)

	// Configure server
	addr := fmt.Sprintf("%s:%d", *host, *port)
	server := &http.Server{
		Addr:         addr,
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Start server
	go func() {
		logger.Info("Starting health check server", logger.Field("address", addr))
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("Failed to start server", logger.Field("error", err))
			os.Exit(1)
		}
	}()

	// Wait for interrupt signal
	stop := make(chan os.Signal, 1)
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)
	<-stop

	logger.Info("Shutting down server...")

	// Create deadline for shutdown
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()

	// Shutdown gracefully
	if err := server.Shutdown(ctx); err != nil {
		logger.Error("Server shutdown failed", logger.Field("error", err))
		os.Exit(1)
	}

	logger.Info("Server stopped")
}

// loggingMiddleware logs HTTP requests
func loggingMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		duration := time.Since(start)
		
		logger.Info("HTTP Request",
			logger.Field("method", r.Method),
			logger.Field("uri", r.RequestURI),
			logger.Field("remote", r.RemoteAddr),
			logger.Field("duration", duration.String()),
		)
	})
}