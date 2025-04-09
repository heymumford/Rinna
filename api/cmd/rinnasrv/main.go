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
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/api/internal/client"
	"github.com/heymumford/rinna/api/internal/handlers"
	"github.com/heymumford/rinna/api/internal/middleware"
	"github.com/heymumford/rinna/api/internal/server"
	"github.com/heymumford/rinna/api/pkg/config"
	"github.com/heymumford/rinna/api/pkg/health"
	"github.com/heymumford/rinna/api/pkg/logger"
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
	logger.Info("Checking Java service health...")
	err := c.client.Ping(ctx)
	if err != nil {
		logger.Error("Java service health check failed", logger.Field("error", err))
		return health.ServiceStatus{
			Status:    "error",
			Message:   err.Error(),
			Timestamp: time.Now().Format(time.RFC3339),
		}
	}
	
	logger.Info("Java service health check succeeded")
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
	skipAutoStart := flag.Bool("no-autostart", false, "Disable automatic Java server startup")
	flag.Parse()

	// Configure the logger
	logLevel := logger.InfoLevel
	if os.Getenv("RINNA_LOG_LEVEL") == "DEBUG" {
		logLevel = logger.DebugLevel
	}
	
	// Setup logging to file and stdout
	logDir := os.Getenv("RINNA_LOG_DIR")
	if logDir == "" {
		homeDir, _ := os.UserHomeDir()
		logDir = homeDir + "/.rinna/logs"
	}
	
	// Ensure log directory exists
	os.MkdirAll(logDir, 0755)
	
	logger.Configure(logger.Config{
		Level:      logLevel,
		TimeFormat: time.RFC3339,
		LogFile:    logDir + "/rinna-api.log",
		ShowCaller: true,
	})
	
	// Load configuration
	logger.Debug("Loading configuration", logger.Field("path", *configPath))
	cfg, err := config.LoadConfig()
	if err != nil {
		logger.Fatal("Failed to load configuration", logger.Field("error", err))
	}

	// Override config with command line flags if provided
	if *serverPort != 0 {
		cfg.Server.Port = *serverPort
	}
	if *serverHost != "" {
		cfg.Server.Host = *serverHost
	}

	// Set up server launcher to manage the Java backend
	launcher := server.NewServerLauncher(cfg)
	
	// Handle server auto-starting
	if !*skipAutoStart && !launcher.IsExternalServerConfigured() {
		// Start local server if no external server is configured
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		
		err := launcher.StartLocalServer(ctx)
		if err != nil {
			logger.Error("Failed to start local Rinna server", logger.Field("error", err))
			// Continue anyway, the client will handle the connection failure
		}
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
	handlers.RegisterReleaseRoutes(api, javaClient)
	handlers.RegisterProjectRoutes(api, javaClient)
	handlers.RegisterWebhookRoutes(api, javaClient, authService)
	
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
		logger.WithFields(map[string]interface{}{
			"address": serverAddr,
			"version": health.Version,
		}).Info("Starting Rinna API server")
		
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Fatal("Failed to start server", logger.Field("error", err))
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
	logger.Info("Shutting down API server...")
	if err := server.Shutdown(ctx); err != nil {
		logger.Fatal("API server shutdown failed", logger.Field("error", err))
	}
	logger.Info("API server stopped gracefully")
	
	// If we started the Java server, stop it too
	if !*skipAutoStart && !launcher.IsExternalServerConfigured() {
		err := launcher.StopLocalServer()
		if err != nil {
			logger.Error("Error stopping local Rinna server", logger.Field("error", err))
		}
	}
	
	// Close the logger to flush any buffered logs
	logger.Close()
}