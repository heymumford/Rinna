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
	"path/filepath"
	"syscall"
	"time"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/go/src/auth"
	"github.com/heymumford/rinna/go/src/client"
	"github.com/heymumford/rinna/go/src/handlers"
	"github.com/heymumford/rinna/go/src/middleware"
	"github.com/heymumford/rinna/go/src/server"
	"github.com/heymumford/rinna/go/pkg/config"
	"github.com/heymumford/rinna/go/pkg/health"
	"github.com/heymumford/rinna/go/pkg/logger"
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
	
	// Create security-focused logger
	securityLogger := logger.GetLogger().WithPrefix("security")
	
	// Create rate limiter
	rateLimiter := middleware.NewRateLimiter(&cfg.RateLimit, securityLogger)
	
	// Apply middleware in correct order
	r.Use(middleware.RequestID)                          // First add request ID for tracing
	r.Use(middleware.SecurityLogging(securityLogger))    // Then add security logging for all requests
	r.Use(middleware.ExtractBodyMiddleware)              // Extract body for multiple reads
	r.Use(middleware.RateLimit(rateLimiter))             // Apply rate limiting before other processing
	r.Use(middleware.Logging)                            // General request logging
	r.Use(middleware.CORS(cfg.Auth.AllowedOrigins))      // CORS headers
	
	// API version middleware
	api := r.PathPrefix("/api/v1").Subrouter()
	
	// Apply authentication middleware
	// Uncomment the following line to use the more robust token authentication
	// api.Use(middleware.TokenAuthentication(authService))
	
	// For backward compatibility, use the simplified authentication for now
	api.Use(middleware.Authentication)
	
	// Apply webhook authentication middleware for webhook endpoints
	api.Use(middleware.WebhookAuthentication(authService))

	// Setup OAuth
	// Get OAuth token storage directory
	oauthDir := os.Getenv("RINNA_OAUTH_DIR")
	if oauthDir == "" {
		homeDir, _ := os.UserHomeDir()
		oauthDir = filepath.Join(homeDir, ".rinna/oauth")
	}
	
	// Ensure OAuth directory exists
	os.MkdirAll(oauthDir, 0700)
	
	// Create token storage
	tokenStorage, err := auth.NewFileTokenStorage(oauthDir, cfg.Auth.TokenSecret)
	if err != nil {
		logger.Fatal("Failed to create OAuth token storage", logger.Field("error", err))
	}
	
	// Create OAuth manager
	oauthManager := auth.NewOAuthManager(tokenStorage)
	
	// Register common OAuth providers
	if cfg.OAuth.GitHub.Enabled {
		err := oauthManager.RegisterProvider(&auth.OAuthConfig{
			Provider:     auth.OAuthProviderGitHub,
			ClientID:     cfg.OAuth.GitHub.ClientID,
			ClientSecret: cfg.OAuth.GitHub.ClientSecret,
			RedirectURL:  cfg.OAuth.GitHub.RedirectURL,
			AuthURL:      "https://github.com/login/oauth/authorize",
			TokenURL:     "https://github.com/login/oauth/access_token",
			Scopes:       cfg.OAuth.GitHub.Scopes,
			APIBaseURL:   "https://api.github.com",
			ExtraParams: map[string]string{
				"userinfo_url": "https://api.github.com/user",
			},
		})
		if err != nil {
			logger.Error("Failed to register GitHub OAuth provider", logger.Field("error", err))
		} else {
			logger.Info("GitHub OAuth provider registered")
		}
	}
	
	if cfg.OAuth.GitLab.Enabled {
		err := oauthManager.RegisterProvider(&auth.OAuthConfig{
			Provider:     auth.OAuthProviderGitLab,
			ClientID:     cfg.OAuth.GitLab.ClientID,
			ClientSecret: cfg.OAuth.GitLab.ClientSecret,
			RedirectURL:  cfg.OAuth.GitLab.RedirectURL,
			AuthURL:      cfg.OAuth.GitLab.ServerURL + "/oauth/authorize",
			TokenURL:     cfg.OAuth.GitLab.ServerURL + "/oauth/token",
			Scopes:       cfg.OAuth.GitLab.Scopes,
			APIBaseURL:   cfg.OAuth.GitLab.ServerURL + "/api/v4",
			ExtraParams: map[string]string{
				"userinfo_url": cfg.OAuth.GitLab.ServerURL + "/api/v4/user",
			},
		})
		if err != nil {
			logger.Error("Failed to register GitLab OAuth provider", logger.Field("error", err))
		} else {
			logger.Info("GitLab OAuth provider registered")
		}
	}
	
	if cfg.OAuth.Azure.Enabled {
		// Azure DevOps has a different OAuth flow
		err := oauthManager.RegisterProvider(&auth.OAuthConfig{
			Provider:     auth.OAuthProviderAzureDevOps,
			ClientID:     cfg.OAuth.Azure.ClientID,
			ClientSecret: cfg.OAuth.Azure.ClientSecret,
			RedirectURL:  cfg.OAuth.Azure.RedirectURL,
			AuthURL:      "https://app.vssps.visualstudio.com/oauth2/authorize",
			TokenURL:     "https://app.vssps.visualstudio.com/oauth2/token",
			Scopes:       cfg.OAuth.Azure.Scopes,
			APIBaseURL:   "https://app.vssps.visualstudio.com",
			ExtraParams: map[string]string{
				"userinfo_url": "https://app.vssps.visualstudio.com/_apis/profile/profiles/me",
			},
		})
		if err != nil {
			logger.Error("Failed to register Azure DevOps OAuth provider", logger.Field("error", err))
		} else {
			logger.Info("Azure DevOps OAuth provider registered")
		}
	}
	
	if cfg.OAuth.Jira.Enabled {
		err := oauthManager.RegisterProvider(&auth.OAuthConfig{
			Provider:     auth.OAuthProviderJira,
			ClientID:     cfg.OAuth.Jira.ClientID,
			ClientSecret: cfg.OAuth.Jira.ClientSecret,
			RedirectURL:  cfg.OAuth.Jira.RedirectURL,
			AuthURL:      cfg.OAuth.Jira.ServerURL + "/plugins/servlet/oauth/authorize",
			TokenURL:     cfg.OAuth.Jira.ServerURL + "/plugins/servlet/oauth/access-token",
			Scopes:       cfg.OAuth.Jira.Scopes,
			APIBaseURL:   cfg.OAuth.Jira.ServerURL,
			ExtraParams: map[string]string{
				"userinfo_url": cfg.OAuth.Jira.ServerURL + "/rest/api/2/myself",
			},
		})
		if err != nil {
			logger.Error("Failed to register Jira OAuth provider", logger.Field("error", err))
		} else {
			logger.Info("Jira OAuth provider registered")
		}
	}
	
	// Register routes
	handlers.RegisterWorkItemRoutes(api, javaClient)
	handlers.RegisterReleaseRoutes(api, javaClient)
	handlers.RegisterProjectRoutes(api, javaClient)
	handlers.RegisterWebhookRoutes(api, javaClient, authService)
	handlers.RegisterOAuthRoutes(r, oauthManager, authService.TokenAuthMiddleware)
	
	// Get the API directory for documentation
	apiDir := os.Getenv("RINNA_API_DIR")
	if apiDir == "" {
		// Use the current working directory as fallback
		var err error
		apiDir, err = os.Getwd()
		if err != nil {
			logger.Error("Failed to get working directory", logger.Field("error", err))
			apiDir = "."
		}
	}
	
	// Register documentation routes (without authentication)
	handlers.RegisterDocumentationRoutes(r, apiDir)
	
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