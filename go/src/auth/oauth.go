/*
 * OAuth 2.0 implementation for third-party API connections
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package auth

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"

	"golang.org/x/oauth2"
)

// OAuthProvider represents a supported OAuth provider
type OAuthProvider string

const (
	// OAuthProviderGitHub is the GitHub OAuth provider
	OAuthProviderGitHub OAuthProvider = "github"
	
	// OAuthProviderJira is the Jira OAuth provider
	OAuthProviderJira OAuthProvider = "jira"
	
	// OAuthProviderAzureDevOps is the Azure DevOps OAuth provider
	OAuthProviderAzureDevOps OAuthProvider = "azure-devops"
	
	// OAuthProviderGitLab is the GitLab OAuth provider
	OAuthProviderGitLab OAuthProvider = "gitlab"
	
	// OAuthProviderBitbucket is the Bitbucket OAuth provider
	OAuthProviderBitbucket OAuthProvider = "bitbucket"
	
	// OAuthProviderGeneric is a generic OAuth provider
	OAuthProviderGeneric OAuthProvider = "generic"
)

// OAuthConfig represents the configuration for an OAuth provider
type OAuthConfig struct {
	Provider      OAuthProvider `json:"provider"`
	ClientID      string        `json:"client_id"`
	ClientSecret  string        `json:"client_secret,omitempty"` // Omitted in responses
	RedirectURL   string        `json:"redirect_url"`
	AuthURL       string        `json:"auth_url"`
	TokenURL      string        `json:"token_url"`
	Scopes        []string      `json:"scopes"`
	APIBaseURL    string        `json:"api_base_url"`
	ExtraParams   map[string]string `json:"extra_params,omitempty"`
}

// OAuthToken represents an OAuth token with metadata
type OAuthToken struct {
	AccessToken  string    `json:"access_token"`
	TokenType    string    `json:"token_type"`
	RefreshToken string    `json:"refresh_token,omitempty"`
	Expiry       time.Time `json:"expiry,omitempty"`
	Scopes       []string  `json:"scopes,omitempty"`
	Provider     OAuthProvider `json:"provider"`
	UserID       string    `json:"user_id,omitempty"`
	ProjectID    string    `json:"project_id,omitempty"`
	CreatedAt    time.Time `json:"created_at"`
	UpdatedAt    time.Time `json:"updated_at"`
	MetaData     map[string]string `json:"metadata,omitempty"`
}

// OAuthState represents the state for an OAuth authorization flow
type OAuthState struct {
	StateToken  string    `json:"state_token"`
	Provider    OAuthProvider `json:"provider"`
	RedirectURL string    `json:"redirect_url"`
	ProjectID   string    `json:"project_id,omitempty"`
	UserID      string    `json:"user_id,omitempty"`
	CreatedAt   time.Time `json:"created_at"`
	Expiry      time.Time `json:"expiry"`
	ExtraParams map[string]string `json:"extra_params,omitempty"`
}

// OAuthManager handles OAuth authentication flows
type OAuthManager struct {
	configs       map[OAuthProvider]*OAuthConfig
	tokens        map[string]*OAuthToken        // Keyed by provider+projectID+userID
	states        map[string]*OAuthState        // Keyed by state token
	stateExpiry   time.Duration                 // How long a state is valid
	tokenStorage  OAuthTokenStorage             // Interface for persistent token storage
	stateLock     sync.RWMutex                  // Lock for concurrent access to states
	tokenLock     sync.RWMutex                  // Lock for concurrent access to tokens
	configLock    sync.RWMutex                  // Lock for concurrent access to configs
}

// OAuthTokenStorage defines the interface for OAuth token persistence
type OAuthTokenStorage interface {
	SaveToken(token *OAuthToken) error
	LoadToken(provider OAuthProvider, projectID, userID string) (*OAuthToken, error)
	DeleteToken(provider OAuthProvider, projectID, userID string) error
	ListTokens(provider OAuthProvider, projectID string) ([]*OAuthToken, error)
}

// NewOAuthManager creates a new OAuth manager with the given configuration
func NewOAuthManager(tokenStorage OAuthTokenStorage) *OAuthManager {
	if tokenStorage == nil {
		// Use in-memory storage by default
		tokenStorage = newInMemoryTokenStorage()
	}
	
	return &OAuthManager{
		configs:      make(map[OAuthProvider]*OAuthConfig),
		tokens:       make(map[string]*OAuthToken),
		states:       make(map[string]*OAuthState),
		stateExpiry:  30 * time.Minute, // Default: 30 minutes
		tokenStorage: tokenStorage,
	}
}

// RegisterProvider registers an OAuth provider configuration
func (m *OAuthManager) RegisterProvider(config *OAuthConfig) error {
	if config.Provider == "" {
		return errors.New("provider is required")
	}
	if config.ClientID == "" {
		return errors.New("client ID is required")
	}
	if config.ClientSecret == "" {
		return errors.New("client secret is required")
	}
	if config.RedirectURL == "" {
		return errors.New("redirect URL is required")
	}
	if config.AuthURL == "" {
		return errors.New("authorization URL is required")
	}
	if config.TokenURL == "" {
		return errors.New("token URL is required")
	}
	
	m.configLock.Lock()
	defer m.configLock.Unlock()
	
	m.configs[config.Provider] = config
	return nil
}

// GetProviderConfig returns the configuration for a provider
func (m *OAuthManager) GetProviderConfig(provider OAuthProvider) (*OAuthConfig, error) {
	m.configLock.RLock()
	defer m.configLock.RUnlock()
	
	config, exists := m.configs[provider]
	if !exists {
		return nil, fmt.Errorf("provider %s not registered", provider)
	}
	
	return config, nil
}

// GetAuthorizationURL generates an authorization URL for the given provider
func (m *OAuthManager) GetAuthorizationURL(provider OAuthProvider, projectID, userID string, extraParams map[string]string) (string, string, error) {
	// Get provider config
	config, err := m.GetProviderConfig(provider)
	if err != nil {
		return "", "", err
	}
	
	// Create OAuth2 config
	oauth2Config := &oauth2.Config{
		ClientID:     config.ClientID,
		ClientSecret: config.ClientSecret,
		RedirectURL:  config.RedirectURL,
		Scopes:       config.Scopes,
		Endpoint: oauth2.Endpoint{
			AuthURL:  config.AuthURL,
			TokenURL: config.TokenURL,
		},
	}
	
	// Generate a secure random state
	stateToken, err := generateSecureRandomString(32)
	if err != nil {
		return "", "", fmt.Errorf("failed to generate state token: %w", err)
	}
	
	// Store the state
	state := &OAuthState{
		StateToken:  stateToken,
		Provider:    provider,
		RedirectURL: config.RedirectURL,
		ProjectID:   projectID,
		UserID:      userID,
		CreatedAt:   time.Now(),
		Expiry:      time.Now().Add(m.stateExpiry),
		ExtraParams: extraParams,
	}
	
	m.stateLock.Lock()
	m.states[stateToken] = state
	m.stateLock.Unlock()
	
	// Clean up expired states occasionally
	go m.cleanupExpiredStates()
	
	// Add any extra provider-specific parameters
	authOpts := make([]oauth2.AuthCodeOption, 0)
	for k, v := range config.ExtraParams {
		authOpts = append(authOpts, oauth2.SetAuthURLParam(k, v))
	}
	for k, v := range extraParams {
		authOpts = append(authOpts, oauth2.SetAuthURLParam(k, v))
	}
	
	// Generate the authorization URL
	authURL := oauth2Config.AuthCodeURL(stateToken, authOpts...)
	
	return authURL, stateToken, nil
}

// HandleCallback processes the OAuth callback and exchanges the code for a token
func (m *OAuthManager) HandleCallback(ctx context.Context, code, stateToken string) (*OAuthToken, error) {
	// Validate the state token
	m.stateLock.RLock()
	state, exists := m.states[stateToken]
	m.stateLock.RUnlock()
	
	if !exists {
		return nil, errors.New("invalid or expired state token")
	}
	
	if time.Now().After(state.Expiry) {
		m.stateLock.Lock()
		delete(m.states, stateToken)
		m.stateLock.Unlock()
		return nil, errors.New("state token has expired")
	}
	
	// Get provider config
	config, err := m.GetProviderConfig(state.Provider)
	if err != nil {
		return nil, err
	}
	
	// Create OAuth2 config
	oauth2Config := &oauth2.Config{
		ClientID:     config.ClientID,
		ClientSecret: config.ClientSecret,
		RedirectURL:  config.RedirectURL,
		Scopes:       config.Scopes,
		Endpoint: oauth2.Endpoint{
			AuthURL:  config.AuthURL,
			TokenURL: config.TokenURL,
		},
	}
	
	// Exchange the authorization code for a token
	oauth2Token, err := oauth2Config.Exchange(ctx, code)
	if err != nil {
		return nil, fmt.Errorf("failed to exchange code for token: %w", err)
	}
	
	// Create our token object
	token := &OAuthToken{
		AccessToken:  oauth2Token.AccessToken,
		TokenType:    oauth2Token.TokenType,
		RefreshToken: oauth2Token.RefreshToken,
		Expiry:       oauth2Token.Expiry,
		Scopes:       config.Scopes,
		Provider:     state.Provider,
		UserID:       state.UserID,
		ProjectID:    state.ProjectID,
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
		MetaData:     make(map[string]string),
	}
	
	// Attempt to get user information if the provider supports it
	userInfo, err := m.getUserInfo(ctx, token)
	if err == nil && userInfo != nil {
		for k, v := range userInfo {
			token.MetaData[k] = v
		}
	}
	
	// Store the token
	err = m.storeToken(token)
	if err != nil {
		return nil, fmt.Errorf("failed to store token: %w", err)
	}
	
	// Clean up the used state
	m.stateLock.Lock()
	delete(m.states, stateToken)
	m.stateLock.Unlock()
	
	return token, nil
}

// RefreshToken refreshes an OAuth token if it supports refresh tokens
func (m *OAuthManager) RefreshToken(ctx context.Context, provider OAuthProvider, projectID, userID string) (*OAuthToken, error) {
	// Get the current token
	token, err := m.GetToken(provider, projectID, userID)
	if err != nil {
		return nil, err
	}
	
	// Check if the token supports refresh
	if token.RefreshToken == "" {
		return nil, errors.New("token does not support refresh")
	}
	
	// Get provider config
	config, err := m.GetProviderConfig(provider)
	if err != nil {
		return nil, err
	}
	
	// Create OAuth2 config
	oauth2Config := &oauth2.Config{
		ClientID:     config.ClientID,
		ClientSecret: config.ClientSecret,
		RedirectURL:  config.RedirectURL,
		Endpoint: oauth2.Endpoint{
			AuthURL:  config.AuthURL,
			TokenURL: config.TokenURL,
		},
	}
	
	// Create an OAuth2 token
	oauth2Token := &oauth2.Token{
		AccessToken:  token.AccessToken,
		TokenType:    token.TokenType,
		RefreshToken: token.RefreshToken,
		Expiry:       token.Expiry,
	}
	
	// Refresh the token
	newOAuth2Token, err := oauth2Config.TokenSource(ctx, oauth2Token).Token()
	if err != nil {
		return nil, fmt.Errorf("failed to refresh token: %w", err)
	}
	
	// Update our token object
	token.AccessToken = newOAuth2Token.AccessToken
	token.TokenType = newOAuth2Token.TokenType
	token.RefreshToken = newOAuth2Token.RefreshToken
	token.Expiry = newOAuth2Token.Expiry
	token.UpdatedAt = time.Now()
	
	// Store the updated token
	err = m.storeToken(token)
	if err != nil {
		return nil, fmt.Errorf("failed to store refreshed token: %w", err)
	}
	
	return token, nil
}

// GetToken returns a stored OAuth token
func (m *OAuthManager) GetToken(provider OAuthProvider, projectID, userID string) (*OAuthToken, error) {
	tokenKey := fmt.Sprintf("%s:%s:%s", provider, projectID, userID)
	
	// Try memory cache first
	m.tokenLock.RLock()
	token, exists := m.tokens[tokenKey]
	m.tokenLock.RUnlock()
	
	if exists {
		return token, nil
	}
	
	// Try persistent storage
	token, err := m.tokenStorage.LoadToken(provider, projectID, userID)
	if err != nil {
		return nil, err
	}
	
	// Cache the token
	m.tokenLock.Lock()
	m.tokens[tokenKey] = token
	m.tokenLock.Unlock()
	
	return token, nil
}

// RevokeToken revokes and removes an OAuth token
func (m *OAuthManager) RevokeToken(ctx context.Context, provider OAuthProvider, projectID, userID string) error {
	tokenKey := fmt.Sprintf("%s:%s:%s", provider, projectID, userID)
	
	// Try to get the token
	token, err := m.GetToken(provider, projectID, userID)
	if err != nil {
		return err
	}
	
	// Attempt to revoke with the provider if the provider supports it
	// This could be provider-specific, but many don't have a revocation endpoint
	config, err := m.GetProviderConfig(provider)
	if err == nil {
		// Only attempt to revoke if the provider has a revocation endpoint specified
		if revocationURL, ok := config.ExtraParams["revocation_url"]; ok {
			client := &http.Client{Timeout: 10 * time.Second}
			
			// Build the revocation request
			data := url.Values{}
			data.Set("token", token.AccessToken)
			data.Set("client_id", config.ClientID)
			data.Set("client_secret", config.ClientSecret)
			
			// Send the revocation request
			req, err := http.NewRequestWithContext(ctx, "POST", revocationURL, strings.NewReader(data.Encode()))
			if err == nil {
				req.Header.Add("Content-Type", "application/x-www-form-urlencoded")
				_, _ = client.Do(req) // We don't check the result, as we'll delete the token locally anyway
			}
		}
	}
	
	// Remove from memory cache
	m.tokenLock.Lock()
	delete(m.tokens, tokenKey)
	m.tokenLock.Unlock()
	
	// Remove from persistent storage
	return m.tokenStorage.DeleteToken(provider, projectID, userID)
}

// ListTokens lists all tokens for a provider and project
func (m *OAuthManager) ListTokens(provider OAuthProvider, projectID string) ([]*OAuthToken, error) {
	// We go directly to persistent storage here to ensure we get all tokens
	return m.tokenStorage.ListTokens(provider, projectID)
}

// storeToken stores a token in both memory cache and persistent storage
func (m *OAuthManager) storeToken(token *OAuthToken) error {
	// Store in persistent storage first
	err := m.tokenStorage.SaveToken(token)
	if err != nil {
		return err
	}
	
	// Then cache in memory
	tokenKey := fmt.Sprintf("%s:%s:%s", token.Provider, token.ProjectID, token.UserID)
	m.tokenLock.Lock()
	m.tokens[tokenKey] = token
	m.tokenLock.Unlock()
	
	return nil
}

// getUserInfo attempts to get user information from the provider
func (m *OAuthManager) getUserInfo(ctx context.Context, token *OAuthToken) (map[string]string, error) {
	// Get provider config
	config, err := m.GetProviderConfig(token.Provider)
	if err != nil {
		return nil, err
	}
	
	// Check if provider has a user info URL
	userInfoURL, ok := config.ExtraParams["userinfo_url"]
	if !ok || userInfoURL == "" {
		return nil, errors.New("provider does not support user info")
	}
	
	// Create client with token
	client := oauth2.NewClient(ctx, oauth2.StaticTokenSource(&oauth2.Token{
		AccessToken: token.AccessToken,
		TokenType:   token.TokenType,
		Expiry:      token.Expiry,
	}))
	
	// Get user info
	resp, err := client.Get(userInfoURL)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	
	// Parse response
	var data map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&data); err != nil {
		return nil, err
	}
	
	// Convert to string map
	result := make(map[string]string)
	for k, v := range data {
		if str, ok := v.(string); ok {
			result[k] = str
		} else {
			// Convert non-string values to JSON string
			if jsonBytes, err := json.Marshal(v); err == nil {
				result[k] = string(jsonBytes)
			}
		}
	}
	
	return result, nil
}

// cleanupExpiredStates removes expired state tokens
func (m *OAuthManager) cleanupExpiredStates() {
	m.stateLock.Lock()
	defer m.stateLock.Unlock()
	
	now := time.Now()
	for stateToken, state := range m.states {
		if now.After(state.Expiry) {
			delete(m.states, stateToken)
		}
	}
}

// GetOAuth2Client returns an HTTP client configured with OAuth authentication
func (m *OAuthManager) GetOAuth2Client(ctx context.Context, provider OAuthProvider, projectID, userID string) (*http.Client, error) {
	// Get the token
	token, err := m.GetToken(provider, projectID, userID)
	if err != nil {
		return nil, err
	}
	
	// Check if the token is expired and needs to be refreshed
	if token.Expiry.Before(time.Now()) {
		// Try to refresh the token
		refreshedToken, err := m.RefreshToken(ctx, provider, projectID, userID)
		if err != nil {
			return nil, fmt.Errorf("token expired and refresh failed: %w", err)
		}
		token = refreshedToken
	}
	
	// Create OAuth2 token
	oauth2Token := &oauth2.Token{
		AccessToken:  token.AccessToken,
		TokenType:    token.TokenType,
		RefreshToken: token.RefreshToken,
		Expiry:       token.Expiry,
	}
	
	// Create OAuth2 client
	client := oauth2.NewClient(ctx, oauth2.StaticTokenSource(oauth2Token))
	
	return client, nil
}

// generateSecureRandomString generates a secure random string of the specified length
func generateSecureRandomString(length int) (string, error) {
	// Generate random bytes
	bytes := make([]byte, length)
	_, err := rand.Read(bytes)
	if err != nil {
		return "", err
	}
	
	// Convert to base64
	return base64.URLEncoding.EncodeToString(bytes)[:length], nil
}

// inMemoryTokenStorage is a simple in-memory implementation of OAuthTokenStorage
type inMemoryTokenStorage struct {
	tokens map[string]*OAuthToken
	mu     sync.RWMutex
}

// newInMemoryTokenStorage creates a new in-memory token storage
func newInMemoryTokenStorage() *inMemoryTokenStorage {
	return &inMemoryTokenStorage{
		tokens: make(map[string]*OAuthToken),
	}
}

// SaveToken implements OAuthTokenStorage
func (s *inMemoryTokenStorage) SaveToken(token *OAuthToken) error {
	key := fmt.Sprintf("%s:%s:%s", token.Provider, token.ProjectID, token.UserID)
	s.mu.Lock()
	defer s.mu.Unlock()
	s.tokens[key] = token
	return nil
}

// LoadToken implements OAuthTokenStorage
func (s *inMemoryTokenStorage) LoadToken(provider OAuthProvider, projectID, userID string) (*OAuthToken, error) {
	key := fmt.Sprintf("%s:%s:%s", provider, projectID, userID)
	s.mu.RLock()
	defer s.mu.RUnlock()
	token, exists := s.tokens[key]
	if !exists {
		return nil, errors.New("token not found")
	}
	return token, nil
}

// DeleteToken implements OAuthTokenStorage
func (s *inMemoryTokenStorage) DeleteToken(provider OAuthProvider, projectID, userID string) error {
	key := fmt.Sprintf("%s:%s:%s", provider, projectID, userID)
	s.mu.Lock()
	defer s.mu.Unlock()
	delete(s.tokens, key)
	return nil
}

// ListTokens implements OAuthTokenStorage
func (s *inMemoryTokenStorage) ListTokens(provider OAuthProvider, projectID string) ([]*OAuthToken, error) {
	prefix := fmt.Sprintf("%s:%s:", provider, projectID)
	s.mu.RLock()
	defer s.mu.RUnlock()
	var result []*OAuthToken
	for key, token := range s.tokens {
		if strings.HasPrefix(key, prefix) {
			result = append(result, token)
		}
	}
	return result, nil
}