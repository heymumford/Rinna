/*
 * Secure token management for API clients
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package client

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"sync"
	"time"

	"github.com/heymumford/rinna/api/pkg/config"
)

const (
	// TokenExpirationBufferHours is the buffer period before actual expiration
	// to ensure tokens are rotated before they expire
	TokenExpirationBufferHours = 24

	// DefaultTokenStorageDir is the default location for token storage
	DefaultTokenStorageDir = "${HOME}/.rinna/tokens"

	// TokenFilePermissions defines the permissions for token files
	TokenFilePermissions = 0600
)

// TokenInfo contains information about an API token
type TokenInfo struct {
	Token       string    `json:"token"`
	ProjectID   string    `json:"project_id"`
	TokenType   string    `json:"token_type"`
	Scope       string    `json:"scope"`
	IssuedAt    time.Time `json:"issued_at"`
	ExpiresAt   time.Time `json:"expires_at"`
	LastChecked time.Time `json:"last_checked"`
	Valid       bool      `json:"valid"`
}

// TokenManager handles secure token management for API clients
type TokenManager struct {
	config        *config.RinnaConfig
	javaClient    *JavaClient
	tokens        map[string]TokenInfo
	tokensDir     string
	tokenFilePath string
	mu            sync.RWMutex
}

// NewTokenManager creates a new token manager
func NewTokenManager(cfg *config.RinnaConfig, javaClient *JavaClient) (*TokenManager, error) {
	// Set up token storage directory
	tokensDir := os.ExpandEnv(DefaultTokenStorageDir)
	if envTokensDir := os.Getenv("RINNA_TOKENS_DIR"); envTokensDir != "" {
		tokensDir = envTokensDir
	}

	// Ensure tokens directory exists
	if err := os.MkdirAll(tokensDir, 0700); err != nil {
		return nil, fmt.Errorf("failed to create tokens directory: %w", err)
	}

	tokenFilePath := filepath.Join(tokensDir, "tokens.json")

	tm := &TokenManager{
		config:        cfg,
		javaClient:    javaClient,
		tokens:        make(map[string]TokenInfo),
		tokensDir:     tokensDir,
		tokenFilePath: tokenFilePath,
	}

	// Load existing tokens
	if err := tm.loadTokens(); err != nil {
		// If the file doesn't exist, that's fine, we'll create it later
		if !os.IsNotExist(err) {
			return nil, fmt.Errorf("failed to load tokens: %w", err)
		}
	}

	return tm, nil
}

// loadTokens loads tokens from the tokens file
func (tm *TokenManager) loadTokens() error {
	tm.mu.Lock()
	defer tm.mu.Unlock()

	// Read the tokens file
	data, err := os.ReadFile(tm.tokenFilePath)
	if err != nil {
		return err
	}

	// Parse the tokens
	return json.Unmarshal(data, &tm.tokens)
}

// saveTokens saves tokens to the tokens file
func (tm *TokenManager) saveTokens() error {
	tm.mu.RLock()
	defer tm.mu.RUnlock()

	// Create the parent directory if it doesn't exist
	if err := os.MkdirAll(filepath.Dir(tm.tokenFilePath), 0700); err != nil {
		return err
	}

	// Marshal the tokens
	data, err := json.MarshalIndent(tm.tokens, "", "  ")
	if err != nil {
		return err
	}

	// Write the tokens file with secure permissions
	return os.WriteFile(tm.tokenFilePath, data, TokenFilePermissions)
}

// GetToken gets a token for a specific project
func (tm *TokenManager) GetToken(ctx context.Context, projectID string) (string, error) {
	tm.mu.RLock()
	// First check if we have a valid token in memory
	for tokenID, tokenInfo := range tm.tokens {
		if tokenInfo.ProjectID == projectID && tokenInfo.Valid && time.Now().Before(tokenInfo.ExpiresAt) {
			// If the token is about to expire, log a warning
			if time.Now().Add(TokenExpirationBufferHours * time.Hour).After(tokenInfo.ExpiresAt) {
				fmt.Printf("Warning: Token for project %s will expire in %v\n", 
					projectID, tokenInfo.ExpiresAt.Sub(time.Now()))
			}
			tm.mu.RUnlock()
			return tokenID, nil
		}
	}
	tm.mu.RUnlock()

	// If no valid token found, request a new one
	return tm.requestNewToken(ctx, projectID)
}

// requestNewToken requests a new token from the server
func (tm *TokenManager) requestNewToken(ctx context.Context, projectID string) (string, error) {
	// This would normally call the Java service to get a new token
	// Here we'll use a simplified implementation

	if tm.javaClient == nil {
		return "", errors.New("Java client not available")
	}

	// Request the new token
	url := "/api/auth/token/generate"
	if tm.config != nil && tm.config.Java.Endpoints != nil && tm.config.Java.Endpoints["token_generate"] != "" {
		url = tm.config.Java.Endpoints["token_generate"]
	}

	request := struct {
		ProjectID string `json:"projectId"`
		Scope     string `json:"scope"`
		Duration  int    `json:"durationDays"`
	}{
		ProjectID: projectID,
		Scope:     "api",
		Duration:  90, // 90 days
	}

	var response struct {
		Token     string    `json:"token"`
		ExpiresAt time.Time `json:"expiresAt"`
	}

	err := tm.javaClient.Request(ctx, http.MethodPost, url, request, &response)
	if err != nil {
		return "", fmt.Errorf("failed to request new token: %w", err)
	}

	// Store the new token
	tm.mu.Lock()
	tm.tokens[response.Token] = TokenInfo{
		Token:       response.Token,
		ProjectID:   projectID,
		TokenType:   "prod", // Assume production token
		Scope:       "api",
		IssuedAt:    time.Now(),
		ExpiresAt:   response.ExpiresAt,
		LastChecked: time.Now(),
		Valid:       true,
	}
	tm.mu.Unlock()

	// Save tokens to file
	if err := tm.saveTokens(); err != nil {
		fmt.Printf("Warning: Failed to save tokens: %v\n", err)
	}

	return response.Token, nil
}

// ValidateToken validates a token and updates its status
func (tm *TokenManager) ValidateToken(ctx context.Context, token string) (bool, error) {
	tm.mu.RLock()
	tokenInfo, ok := tm.tokens[token]
	tm.mu.RUnlock()

	if !ok {
		return false, errors.New("token not found")
	}

	// Check if token is expired
	if time.Now().After(tokenInfo.ExpiresAt) {
		tm.mu.Lock()
		tokenInfo.Valid = false
		tm.tokens[token] = tokenInfo
		tm.mu.Unlock()
		
		if err := tm.saveTokens(); err != nil {
			fmt.Printf("Warning: Failed to save tokens: %v\n", err)
		}
		
		return false, errors.New("token expired")
	}

	// Only validate with the server if it's been more than a day since last check
	if time.Since(tokenInfo.LastChecked) < 24*time.Hour {
		return tokenInfo.Valid, nil
	}

	// Validate with the server
	if tm.javaClient == nil {
		return tokenInfo.Valid, nil // Just use local validation if no client
	}

	// Call the Java service to validate the token
	_, err := tm.javaClient.ValidateToken(ctx, token)
	isValid := err == nil

	// Update token status
	tm.mu.Lock()
	tokenInfo.Valid = isValid
	tokenInfo.LastChecked = time.Now()
	tm.tokens[token] = tokenInfo
	tm.mu.Unlock()

	// Save tokens to file
	if err := tm.saveTokens(); err != nil {
		fmt.Printf("Warning: Failed to save tokens: %v\n", err)
	}

	return isValid, err
}

// RevokeToken revokes a token
func (tm *TokenManager) RevokeToken(ctx context.Context, token string) error {
	tm.mu.RLock()
	_, ok := tm.tokens[token]
	tm.mu.RUnlock()

	if !ok {
		return errors.New("token not found")
	}

	// This would normally call the Java service to revoke the token
	if tm.javaClient != nil {
		url := "/api/auth/token/revoke"
		if tm.config != nil && tm.config.Java.Endpoints != nil && tm.config.Java.Endpoints["token_revoke"] != "" {
			url = tm.config.Java.Endpoints["token_revoke"]
		}

		request := struct {
			Token string `json:"token"`
		}{
			Token: token,
		}

		err := tm.javaClient.Request(ctx, http.MethodPost, url, request, nil)
		if err != nil {
			return fmt.Errorf("failed to revoke token: %w", err)
		}
	}

	// Remove the token
	tm.mu.Lock()
	delete(tm.tokens, token)
	tm.mu.Unlock()

	// Save tokens to file
	if err := tm.saveTokens(); err != nil {
		fmt.Printf("Warning: Failed to save tokens: %v\n", err)
	}

	return nil
}

// RevokeAllTokens revokes all tokens
func (tm *TokenManager) RevokeAllTokens(ctx context.Context) error {
	tm.mu.RLock()
	tokens := make([]string, 0, len(tm.tokens))
	for token := range tm.tokens {
		tokens = append(tokens, token)
	}
	tm.mu.RUnlock()

	for _, token := range tokens {
		if err := tm.RevokeToken(ctx, token); err != nil {
			return err
		}
	}

	return nil
}

// ListTokens returns a list of all tokens
func (tm *TokenManager) ListTokens() []TokenInfo {
	tm.mu.RLock()
	defer tm.mu.RUnlock()

	tokens := make([]TokenInfo, 0, len(tm.tokens))
	for _, tokenInfo := range tm.tokens {
		tokens = append(tokens, tokenInfo)
	}

	return tokens
}

// GetTokenInfo returns information about a token
func (tm *TokenManager) GetTokenInfo(token string) (TokenInfo, error) {
	tm.mu.RLock()
	defer tm.mu.RUnlock()

	tokenInfo, ok := tm.tokens[token]
	if !ok {
		return TokenInfo{}, errors.New("token not found")
	}

	return tokenInfo, nil
}

// CleanExpiredTokens removes all expired tokens
func (tm *TokenManager) CleanExpiredTokens() {
	tm.mu.Lock()
	defer tm.mu.Unlock()

	now := time.Now()
	for token, tokenInfo := range tm.tokens {
		if now.After(tokenInfo.ExpiresAt) {
			delete(tm.tokens, token)
		}
	}

	// Save tokens to file
	if err := tm.saveTokens(); err != nil {
		fmt.Printf("Warning: Failed to save tokens: %v\n", err)
	}
}