/*
 * OAuth handlers for the Rinna API
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package handlers

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/gorilla/mux"
	"github.com/heymumford/rinna/go/src/auth"
	"github.com/heymumford/rinna/go/src/middleware"
	"github.com/heymumford/rinna/go/pkg/logger"
)

// OAuthHandler handles OAuth-related requests
type OAuthHandler struct {
	oauthManager *auth.OAuthManager
}

// NewOAuthHandler creates a new OAuth handler
func NewOAuthHandler(oauthManager *auth.OAuthManager) *OAuthHandler {
	return &OAuthHandler{
		oauthManager: oauthManager,
	}
}

// RegisterOAuthRoutes registers OAuth-related routes
func RegisterOAuthRoutes(router *mux.Router, oauthManager *auth.OAuthManager, authMiddleware func(http.Handler) http.Handler) {
	handler := NewOAuthHandler(oauthManager)

	// Create a subrouter for OAuth endpoints
	oauthRouter := router.PathPrefix("/api/v1/oauth").Subrouter()

	// Public endpoints (no authentication required)
	oauthRouter.HandleFunc("/callback", handler.HandleCallback).Methods(http.MethodGet)

	// Protected endpoints (authentication required)
	protectedRouter := oauthRouter.NewRoute().Subrouter()
	protectedRouter.Use(authMiddleware)

	protectedRouter.HandleFunc("/authorize/{provider}", handler.HandleAuthorize).Methods(http.MethodGet)
	protectedRouter.HandleFunc("/tokens", handler.ListTokens).Methods(http.MethodGet)
	protectedRouter.HandleFunc("/tokens/{provider}", handler.GetToken).Methods(http.MethodGet)
	protectedRouter.HandleFunc("/tokens/{provider}", handler.RevokeToken).Methods(http.MethodDelete)
	protectedRouter.HandleFunc("/providers", handler.ListProviders).Methods(http.MethodGet)
	protectedRouter.HandleFunc("/providers/{provider}", handler.GetProvider).Methods(http.MethodGet)
}

// HandleAuthorize initiates an OAuth authorization flow
func (h *OAuthHandler) HandleAuthorize(w http.ResponseWriter, r *http.Request) {
	// Get the provider from the URL
	vars := mux.Vars(r)
	providerStr := vars["provider"]

	// Get query parameters
	projectID := r.URL.Query().Get("project")
	redirectURI := r.URL.Query().Get("redirect_uri")

	// Default to logged-in user ID if available
	userID := middleware.GetUserID(r.Context())
	if userIDParam := r.URL.Query().Get("user_id"); userIDParam != "" {
		userID = userIDParam
	}

	// Log the authorization request
	log := logger.WithFields(map[string]interface{}{
		"provider":  providerStr,
		"projectID": projectID,
		"userID":    userID,
	})
	log.Info("OAuth authorization request")

	// Validate required parameters
	if projectID == "" {
		http.Error(w, "Project ID is required", http.StatusBadRequest)
		return
	}
	if userID == "" {
		http.Error(w, "User ID is required", http.StatusBadRequest)
		return
	}

	// Convert provider string to provider type
	provider := auth.OAuthProvider(providerStr)

	// Get extra parameters
	extraParams := make(map[string]string)
	for k, v := range r.URL.Query() {
		if len(v) > 0 && k != "project" && k != "redirect_uri" && k != "user_id" {
			extraParams[k] = v[0]
		}
	}

	// Generate the authorization URL
	authURL, stateToken, err := h.oauthManager.GetAuthorizationURL(provider, projectID, userID, extraParams)
	if err != nil {
		log.WithField("error", err).Error("Failed to generate authorization URL")
		http.Error(w, "Failed to generate authorization URL: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// If a redirect URI was provided, redirect directly
	if redirectURI != "" {
		http.Redirect(w, r, authURL, http.StatusFound)
		return
	}

	// Otherwise, return the authorization URL and state
	response := map[string]string{
		"authorization_url": authURL,
		"state":             stateToken,
		"provider":          string(provider),
		"expires_in":        "1800", // 30 minutes in seconds
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// HandleCallback handles the OAuth callback
func (h *OAuthHandler) HandleCallback(w http.ResponseWriter, r *http.Request) {
	// Get query parameters
	code := r.URL.Query().Get("code")
	stateToken := r.URL.Query().Get("state")
	errorMsg := r.URL.Query().Get("error")
	errorDesc := r.URL.Query().Get("error_description")

	// Create a logger
	log := logger.WithFields(map[string]interface{}{
		"state": stateToken,
	})
	log.Info("OAuth callback request")

	// Check for errors from the provider
	if errorMsg != "" {
		errorResponse := "OAuth provider returned an error: " + errorMsg
		if errorDesc != "" {
			errorResponse += " - " + errorDesc
		}
		log.Error(errorResponse)
		http.Error(w, errorResponse, http.StatusBadRequest)
		return
	}

	// Validate required parameters
	if code == "" {
		http.Error(w, "Authorization code is required", http.StatusBadRequest)
		return
	}
	if stateToken == "" {
		http.Error(w, "State token is required", http.StatusBadRequest)
		return
	}

	// Exchange the code for a token
	token, err := h.oauthManager.HandleCallback(r.Context(), code, stateToken)
	if err != nil {
		log.WithField("error", err).Error("Failed to exchange code for token")
		http.Error(w, "Failed to exchange code for token: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return success with token info
	response := map[string]interface{}{
		"status":      "success",
		"provider":    string(token.Provider),
		"project_id":  token.ProjectID,
		"user_id":     token.UserID,
		"token_type":  token.TokenType,
		"expires_in":  int(token.Expiry.Sub(time.Now()).Seconds()),
		"scopes":      token.Scopes,
		"created_at":  token.CreatedAt,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// ListTokens lists all OAuth tokens for a project
func (h *OAuthHandler) ListTokens(w http.ResponseWriter, r *http.Request) {
	// Get query parameters
	projectID := r.URL.Query().Get("project")
	providerStr := r.URL.Query().Get("provider")

	// Validate required parameters
	if projectID == "" {
		http.Error(w, "Project ID is required", http.StatusBadRequest)
		return
	}

	// Default to all providers if not specified
	var provider auth.OAuthProvider
	if providerStr != "" {
		provider = auth.OAuthProvider(providerStr)
	} else {
		provider = "" // List all providers
	}

	// Create a logger
	log := logger.WithFields(map[string]interface{}{
		"projectID": projectID,
		"provider":  provider,
	})
	log.Info("List OAuth tokens")

	// Get a list of all providers if none specified
	var allProviders []auth.OAuthProvider
	if provider == "" {
		// Get configured providers
		configs := h.oauthManager.GetProviderConfigs()
		for p := range configs {
			allProviders = append(allProviders, p)
		}
	} else {
		allProviders = []auth.OAuthProvider{provider}
	}

	// Collect tokens for all providers
	var allTokens []map[string]interface{}
	for _, p := range allProviders {
		tokens, err := h.oauthManager.ListTokens(p, projectID)
		if err != nil {
			log.WithFields(map[string]interface{}{
				"provider": p,
				"error":    err,
			}).Error("Failed to list tokens")
			continue
		}

		// Convert tokens to safe response format
		for _, token := range tokens {
			tokenInfo := map[string]interface{}{
				"provider":    string(token.Provider),
				"project_id":  token.ProjectID,
				"user_id":     token.UserID,
				"token_type":  token.TokenType,
				"expires_in":  int(token.Expiry.Sub(time.Now()).Seconds()),
				"scopes":      token.Scopes,
				"created_at":  token.CreatedAt,
				"updated_at":  token.UpdatedAt,
				"has_refresh": token.RefreshToken != "",
				"is_expired":  token.Expiry.Before(time.Now()),
			}
			allTokens = append(allTokens, tokenInfo)
		}
	}

	// Return the list of tokens
	response := map[string]interface{}{
		"tokens": allTokens,
		"count":  len(allTokens),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetToken gets an OAuth token for a provider
func (h *OAuthHandler) GetToken(w http.ResponseWriter, r *http.Request) {
	// Get the provider from the URL
	vars := mux.Vars(r)
	providerStr := vars["provider"]

	// Get query parameters
	projectID := r.URL.Query().Get("project")
	userID := r.URL.Query().Get("user_id")

	// Validate required parameters
	if projectID == "" {
		http.Error(w, "Project ID is required", http.StatusBadRequest)
		return
	}
	if userID == "" {
		http.Error(w, "User ID is required", http.StatusBadRequest)
		return
	}

	// Convert provider string to provider type
	provider := auth.OAuthProvider(providerStr)

	// Create a logger
	log := logger.WithFields(map[string]interface{}{
		"provider":  provider,
		"projectID": projectID,
		"userID":    userID,
	})
	log.Info("Get OAuth token")

	// Get the token
	token, err := h.oauthManager.GetToken(provider, projectID, userID)
	if err != nil {
		log.WithField("error", err).Error("Failed to get token")
		http.Error(w, "Failed to get token: "+err.Error(), http.StatusNotFound)
		return
	}

	// Check if the token is expired and needs refreshing
	isExpired := token.Expiry.Before(time.Now())
	hasRefresh := token.RefreshToken != ""

	if isExpired && hasRefresh {
		// Try to refresh the token
		refreshedToken, err := h.oauthManager.RefreshToken(r.Context(), provider, projectID, userID)
		if err != nil {
			log.WithField("error", err).Error("Failed to refresh token")
			http.Error(w, "Failed to refresh token: "+err.Error(), http.StatusInternalServerError)
			return
		}
		token = refreshedToken
		isExpired = false
	}

	// Return token info
	response := map[string]interface{}{
		"provider":    string(token.Provider),
		"project_id":  token.ProjectID,
		"user_id":     token.UserID,
		"token_type":  token.TokenType,
		"expires_in":  int(token.Expiry.Sub(time.Now()).Seconds()),
		"scopes":      token.Scopes,
		"created_at":  token.CreatedAt,
		"updated_at":  token.UpdatedAt,
		"has_refresh": hasRefresh,
		"is_expired":  isExpired,
		"metadata":    token.MetaData,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// RevokeToken revokes an OAuth token
func (h *OAuthHandler) RevokeToken(w http.ResponseWriter, r *http.Request) {
	// Get the provider from the URL
	vars := mux.Vars(r)
	providerStr := vars["provider"]

	// Get query parameters
	projectID := r.URL.Query().Get("project")
	userID := r.URL.Query().Get("user_id")

	// Validate required parameters
	if projectID == "" {
		http.Error(w, "Project ID is required", http.StatusBadRequest)
		return
	}
	if userID == "" {
		http.Error(w, "User ID is required", http.StatusBadRequest)
		return
	}

	// Convert provider string to provider type
	provider := auth.OAuthProvider(providerStr)

	// Create a logger
	log := logger.WithFields(map[string]interface{}{
		"provider":  provider,
		"projectID": projectID,
		"userID":    userID,
	})
	log.Info("Revoke OAuth token")

	// Revoke the token
	err := h.oauthManager.RevokeToken(r.Context(), provider, projectID, userID)
	if err != nil {
		log.WithField("error", err).Error("Failed to revoke token")
		http.Error(w, "Failed to revoke token: "+err.Error(), http.StatusInternalServerError)
		return
	}

	// Return success
	response := map[string]string{
		"status":     "success",
		"message":    "Token revoked successfully",
		"provider":   string(provider),
		"project_id": projectID,
		"user_id":    userID,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// ListProviders lists all registered OAuth providers
func (h *OAuthHandler) ListProviders(w http.ResponseWriter, r *http.Request) {
	log := logger.WithField("operation", "list_providers")
	log.Info("List OAuth providers")

	// Get all provider configurations
	configs := h.oauthManager.GetProviderConfigs()

	// Convert to safe response format
	var providers []map[string]interface{}
	for provider, config := range configs {
		// Create a safe copy without the client secret
		safeConfig := map[string]interface{}{
			"provider":     string(provider),
			"client_id":    config.ClientID,
			"redirect_url": config.RedirectURL,
			"auth_url":     config.AuthURL,
			"token_url":    config.TokenURL,
			"scopes":       config.Scopes,
			"api_base_url": config.APIBaseURL,
		}
		providers = append(providers, safeConfig)
	}

	// Return the list of providers
	response := map[string]interface{}{
		"providers": providers,
		"count":     len(providers),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetProvider gets a specific OAuth provider configuration
func (h *OAuthHandler) GetProvider(w http.ResponseWriter, r *http.Request) {
	// Get the provider from the URL
	vars := mux.Vars(r)
	providerStr := vars["provider"]
	provider := auth.OAuthProvider(providerStr)

	log := logger.WithField("provider", provider)
	log.Info("Get OAuth provider")

	// Get the provider configuration
	config, err := h.oauthManager.GetProviderConfig(provider)
	if err != nil {
		log.WithField("error", err).Error("Failed to get provider")
		http.Error(w, "Failed to get provider: "+err.Error(), http.StatusNotFound)
		return
	}

	// Create a safe copy without the client secret
	safeConfig := map[string]interface{}{
		"provider":     string(provider),
		"client_id":    config.ClientID,
		"redirect_url": config.RedirectURL,
		"auth_url":     config.AuthURL,
		"token_url":    config.TokenURL,
		"scopes":       config.Scopes,
		"api_base_url": config.APIBaseURL,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(safeConfig)
}