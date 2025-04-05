/*
 * API server for the Rinna workflow management system
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
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/client"
	"github.com/heymumford/rinna/api/internal/handlers"
	"github.com/heymumford/rinna/api/internal/middleware"
	"github.com/heymumford/rinna/api/pkg/config"
	"github.com/heymumford/rinna/api/pkg/health"
)

// JavaHealthChecker implements the health.DependencyChecker interface for Java service
type JavaHealthChecker struct {
	client *client.JavaClient
}

// CheckHealth checks the health of the Java service
func (c *JavaHealthChecker) CheckHealth() health.ServiceStatus {
	// Create a context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	
	// Ping the Java service
	err := c.client.Ping(ctx)
	if err != nil {
		return health.ServiceStatus{
			Status:    "error",
			Message:   err.Error(),
			Timestamp: time.Now().Format(time.RFC3339),
		}
	}
	
	return health.ServiceStatus{
		Status:    "ok",
		Timestamp: time.Now().Format(time.RFC3339),
	}
}

func main() {
	// Parse command line flags
	configPath := flag.String("config", "", "Path to configuration file")
	serverPort := flag.Int("port", 0, "Server port (overrides config)")
	serverHost := flag.String("host", "", "Server host (overrides config)")
	flag.Parse()

	// Load configuration
	cfg, err := config.Load(*configPath)
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}

	// Override config with command line flags if provided
	if *serverPort != 0 {
		cfg.Server.Port = *serverPort
	}
	if *serverHost != "" {
		cfg.Server.Host = *serverHost
	}

	// Create Java client
	javaClient := client.NewJavaClient(&cfg.Java)

	// Create auth service
	authService := middleware.NewAuthService(javaClient, &cfg.Auth)

	// Create router
	r := mux.NewRouter()
	r.Use(middleware.Logging)
	r.Use(middleware.RequestID)
	r.Use(middleware.CORS(cfg.Auth.AllowedOrigins))

	// API version middleware
	api := r.PathPrefix("/api/v1").Subrouter()
	
	// Apply authentication middleware
	// api.Use(middleware.TokenAuthentication(authService))
	
	// For backward compatibility, use the simplified authentication for now
	api.Use(middleware.Authentication)
	
	// Apply webhook authentication middleware for webhook endpoints
	api.Use(middleware.WebhookAuthentication(authService))

	// Register routes
	handlers.RegisterWorkItemRoutes(api, javaClient)
	handlers.RegisterReleaseRoutes(api)
	handlers.RegisterProjectRoutes(api)
	handlers.RegisterWebhookRoutes(api, javaClient)
	
	// Create health handler with Java client checker
	javaChecker := &JavaHealthChecker{client: javaClient}
	healthHandler := health.NewHandler(javaChecker)
	healthHandler.RegisterRoutes(r)

	// Build the server address string
	serverAddr := fmt.Sprintf("%s:%d", cfg.Server.Host, cfg.Server.Port)

	// Configure server
	server := &http.Server{
		Addr:         serverAddr,
		Handler:      r,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Start server in the background
	go func() {
		log.Printf("Starting Rinna API server on %s", serverAddr)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Failed to start server: %v", err)
		}
	}()

	// Wait for interrupt signal
	signalCh := make(chan os.Signal, 1)
	signal.Notify(signalCh, os.Interrupt, syscall.SIGTERM)
	<-signalCh

	// Create a deadline for graceful shutdown
	shutdownTimeout := time.Duration(cfg.Server.ShutdownTimeout) * time.Second
	ctx, cancel := context.WithTimeout(context.Background(), shutdownTimeout)
	defer cancel()

	// Shut down server gracefully
	log.Println("Shutting down server...")
	if err := server.Shutdown(ctx); err != nil {
		log.Fatalf("Server shutdown failed: %v", err)
	}
	log.Println("Server stopped gracefully")
}