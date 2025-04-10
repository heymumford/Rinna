/*
 * Unified configuration for the Rinna API server
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package config

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"

	"github.com/spf13/viper"
)

// RinnaConfig represents the full configuration for the Rinna API server.
type RinnaConfig struct {
	Project    ProjectConfig     `mapstructure:"project"`
	Security   SecurityConfig    `mapstructure:"security"`
	Auth       AuthConfig        `mapstructure:"auth"`
	OAuth      OAuthConfig       `mapstructure:"oauth"`
	Go         GoConfig          `mapstructure:"go"`
	Server     ServerConfig      `mapstructure:"server"`
	Java       JavaServiceConfig `mapstructure:"java"`
	RateLimit  RateLimitConfig   `mapstructure:"rate_limit"`
	Logging    LoggingConfig     `mapstructure:"logging"`
}

// AuthConfig contains authentication-related configuration.
type AuthConfig struct {
	TokenExpiry         int      `mapstructure:"token_expiry_minutes"`
	SecretExpiry        int      `mapstructure:"secret_expiry_minutes"`
	WebhookSecretExpiry int      `mapstructure:"webhook_secret_expiry_minutes"`
	AllowedSources      []string `mapstructure:"allowed_sources"`
	AllowedOrigins      []string `mapstructure:"allowed_origins"`
	DevMode             bool     `mapstructure:"dev_mode"`
	TokenEncryptionKey  string   `mapstructure:"token_encryption_key"`
	TokenSecret         string   `mapstructure:"token_secret"`
}

// OAuthConfig contains OAuth provider configurations
type OAuthConfig struct {
	TokenEncryptionKey string              `mapstructure:"token_encryption_key"`
	GitHub             OAuthProviderConfig `mapstructure:"github"`
	GitLab             OAuthProviderConfig `mapstructure:"gitlab"`
	Jira               OAuthProviderConfig `mapstructure:"jira"`
	Azure              OAuthProviderConfig `mapstructure:"azure"`
	BitBucket          OAuthProviderConfig `mapstructure:"bitbucket"`
	Generic            OAuthProviderConfig `mapstructure:"generic"`
}

// OAuthProviderConfig contains configuration for a specific OAuth provider
type OAuthProviderConfig struct {
	Enabled      bool     `mapstructure:"enabled"`
	ClientID     string   `mapstructure:"client_id"`
	ClientSecret string   `mapstructure:"client_secret"`
	RedirectURL  string   `mapstructure:"redirect_url"`
	ServerURL    string   `mapstructure:"server_url"`
	Scopes       []string `mapstructure:"scopes"`
}

// ServerConfig contains the API server configuration.
type ServerConfig struct {
	Host           string `mapstructure:"host"`
	Port           int    `mapstructure:"port"`
	ReadTimeout    int    `mapstructure:"read_timeout"`
	WriteTimeout   int    `mapstructure:"write_timeout"`
	ShutdownTimeout int   `mapstructure:"shutdown_timeout"`
}

// ProjectConfig contains general project configuration.
type ProjectConfig struct {
	Name        string `mapstructure:"name"`
	Version     string `mapstructure:"version"`
	Environment string `mapstructure:"environment"`
	DataDir     string `mapstructure:"data_dir"`
	TempDir     string `mapstructure:"temp_dir"`
	ConfigDir   string `mapstructure:"config_dir"`
}

// SecurityConfig contains security-related configuration.
type SecurityConfig struct {
	APITokenExpirationDays   int      `mapstructure:"api_token_expiration_days"`
	WebhookTokenExpirationDays int    `mapstructure:"webhook_token_expiration_days"`
	TokenEncryptionKey       string   `mapstructure:"token_encryption_key"`
	EnableCORS               bool     `mapstructure:"enable_cors"`
	AllowedOrigins           []string `mapstructure:"allowed_origins"`
}

// GoConfig contains Go-specific configuration.
type GoConfig struct {
	API     APIConfig     `mapstructure:"api"`
	Health  HealthConfig  `mapstructure:"health"`
	Metrics MetricsConfig `mapstructure:"metrics"`
	Backend BackendConfig `mapstructure:"backend"`
}

// APIConfig contains API server configuration.
type APIConfig struct {
	Port                  int    `mapstructure:"port"`
	ReadTimeoutSeconds    int    `mapstructure:"read_timeout_seconds"`
	WriteTimeoutSeconds   int    `mapstructure:"write_timeout_seconds"`
	ShutdownTimeoutSeconds int   `mapstructure:"shutdown_timeout_seconds"`
	EnableCORS            bool   `mapstructure:"enable_cors"`
	LogLevel              string `mapstructure:"log_level"`
}

// HealthConfig contains health check configuration.
type HealthConfig struct {
	Enabled bool   `mapstructure:"enabled"`
	Path    string `mapstructure:"path"`
}

// MetricsConfig contains metrics configuration.
type MetricsConfig struct {
	Enabled bool   `mapstructure:"enabled"`
	Path    string `mapstructure:"path"`
}

// BackendConfig contains backend service configuration.
type BackendConfig struct {
	JavaURL             string `mapstructure:"java_url"`
	ConnectionTimeoutMS int    `mapstructure:"connection_timeout_ms"`
	RequestTimeoutMS    int    `mapstructure:"request_timeout_ms"`
}

// JavaServiceConfig contains Java service connection configuration
type JavaServiceConfig struct {
	Host           string            `json:"host"`
	Port           int               `json:"port"`
	ConnectTimeout int               `json:"connect_timeout"`
	RequestTimeout int               `json:"request_timeout"`
	Endpoints      map[string]string `json:"endpoints"`
}

// RateLimitConfig contains rate limiting configuration
type RateLimitConfig struct {
	Enabled             bool                `mapstructure:"enabled"`
	DefaultLimit        int                 `mapstructure:"default_limit"`        // Requests per minute
	BurstLimit          int                 `mapstructure:"burst_limit"`          // Allowed burst size
	IPWhitelist         []string            `mapstructure:"ip_whitelist"`         // Whitelisted IPs (no rate limit)
	CustomIPLimits      map[string]int      `mapstructure:"custom_ip_limits"`     // IP-specific limits
	CustomProjectLimits map[string]int      `mapstructure:"custom_project_limits"` // Project-specific limits
	CustomPathLimits    map[string]int      `mapstructure:"custom_path_limits"`   // Path-specific limits
	DefaultPenaltyTime  int                 `mapstructure:"default_penalty_time"` // Seconds of penalty when rate limit is exceeded
}

// LoggingConfig contains logging configuration
type LoggingConfig struct {
	Level           string            `mapstructure:"level"`            // debug, info, warn, error
	Format          string            `mapstructure:"format"`           // json or text
	FileEnabled     bool              `mapstructure:"file_enabled"`     // Enable file logging
	FilePath        string            `mapstructure:"file_path"`        // Log file path
	Rotation        bool              `mapstructure:"rotation"`         // Enable log rotation
	MaxSize         int               `mapstructure:"max_size"`         // Max size in MB before rotation
	MaxAge          int               `mapstructure:"max_age"`          // Max age in days before deletion
	MaxBackups      int               `mapstructure:"max_backups"`      // Max number of rotated files to keep
	SecurityLogging bool              `mapstructure:"security_logging"` // Enable detailed security logging
	RedactPaths     []string          `mapstructure:"redact_paths"`     // Paths that need redaction of sensitive data
	CustomFields    map[string]string `mapstructure:"custom_fields"`    // Custom fields to include in all logs
}

const (
	// Environment variable prefix for Rinna configuration
	envPrefix = "RINNA"
	
	// Default configuration directory
	defaultConfigDir = "${HOME}/.rinna/config"
	
	// Go-specific configuration path
	goConfigPath = "/go/config.yaml"
)

var (
	config     *RinnaConfig
	configOnce sync.Once
)

// LoadConfig loads the configuration from all sources.
func LoadConfig() (*RinnaConfig, error) {
	var err error
	configOnce.Do(func() {
		config = &RinnaConfig{}
		err = loadConfigFromSources(config)
	})
	
	if err != nil {
		return nil, err
	}
	
	return config, nil
}

// GetConfig returns the cached configuration or loads it if not loaded yet.
func GetConfig() (*RinnaConfig, error) {
	if config != nil {
		return config, nil
	}
	return LoadConfig()
}

// ReloadConfig forces a reload of the configuration from all sources.
func ReloadConfig() (*RinnaConfig, error) {
	config = nil
	configOnce = sync.Once{}
	return LoadConfig()
}

// loadConfigFromSources loads configuration from all available sources.
func loadConfigFromSources(cfg *RinnaConfig) error {
	v := viper.New()
	
	// Set up Viper to handle environment variables
	v.SetEnvPrefix(envPrefix)
	v.AutomaticEnv()
	v.SetEnvKeyReplacer(strings.NewReplacer(".", "_"))
	
	// Get config directory from environment variable or use default
	configDir := os.ExpandEnv(defaultConfigDir)
	if envConfigDir := os.Getenv(envPrefix + "_PROJECT_CONFIG_DIR"); envConfigDir != "" {
		configDir = envConfigDir
	}
	
	// Look for config file
	configFile := filepath.Join(configDir, goConfigPath)
	
	// Set up Viper to read the config file
	v.SetConfigFile(configFile)
	
	// Try to read the config file
	if err := v.ReadInConfig(); err != nil {
		// It's okay if the config file doesn't exist, we'll use environment variables
		// and default values, but log a warning
		if _, ok := err.(viper.ConfigFileNotFoundError); !ok {
			return fmt.Errorf("failed to read config file: %w", err)
		}
		
		fmt.Printf("Warning: Config file not found: %s\n", configFile)
		fmt.Println("Run 'rin config generate' to create it.")
	}
	
	// Unmarshal the configuration into the struct
	if err := v.Unmarshal(cfg); err != nil {
		return fmt.Errorf("failed to unmarshal config: %w", err)
	}
	
	// Fill in default values for any missing fields
	setDefaultValues(cfg)
	
	return nil
}

// setDefaultValues sets default values for configuration fields that are not set.
func setDefaultValues(cfg *RinnaConfig) {
	// Project defaults
	if cfg.Project.Name == "" {
		cfg.Project.Name = "Rinna"
	}
	
	if cfg.Project.Version == "" {
		cfg.Project.Version = "1.0.0"
	}
	
	if cfg.Project.Environment == "" {
		cfg.Project.Environment = "development"
	}
	
	// Security defaults
	if cfg.Security.APITokenExpirationDays == 0 {
		cfg.Security.APITokenExpirationDays = 90
	}
	
	if cfg.Security.WebhookTokenExpirationDays == 0 {
		cfg.Security.WebhookTokenExpirationDays = 365
	}
	
	// Auth defaults
	if cfg.Auth.TokenExpiry == 0 {
		cfg.Auth.TokenExpiry = 60 // 60 minutes
	}
	
	if cfg.Auth.SecretExpiry == 0 {
		cfg.Auth.SecretExpiry = 60 // 60 minutes
	}
	
	if cfg.Auth.WebhookSecretExpiry == 0 {
		cfg.Auth.WebhookSecretExpiry = 1440 // 24 hours
	}
	
	if len(cfg.Auth.AllowedSources) == 0 {
		cfg.Auth.AllowedSources = []string{"github", "gitlab", "bitbucket"}
	}
	
	if len(cfg.Auth.AllowedOrigins) == 0 {
		cfg.Auth.AllowedOrigins = []string{"*"}
	}
	
	// Set DevMode based on environment
	if cfg.Project.Environment == "development" {
		cfg.Auth.DevMode = true
	}
	
	// Set a default token secret if not provided (should be overridden in production)
	if cfg.Auth.TokenSecret == "" {
		cfg.Auth.TokenSecret = "default-token-secret-for-development-only"
	}
	
	// OAuth defaults
	if cfg.OAuth.TokenEncryptionKey == "" {
		// Use the same encryption key as auth if not specified
		cfg.OAuth.TokenEncryptionKey = cfg.Auth.TokenSecret
	}
	
	// Set GitHub OAuth defaults
	if len(cfg.OAuth.GitHub.Scopes) == 0 {
		cfg.OAuth.GitHub.Scopes = []string{"repo", "user:email"}
	}
	
	if cfg.OAuth.GitHub.ServerURL == "" {
		cfg.OAuth.GitHub.ServerURL = "https://github.com"
	}
	
	// Set GitLab OAuth defaults
	if len(cfg.OAuth.GitLab.Scopes) == 0 {
		cfg.OAuth.GitLab.Scopes = []string{"api", "read_user"}
	}
	
	if cfg.OAuth.GitLab.ServerURL == "" {
		cfg.OAuth.GitLab.ServerURL = "https://gitlab.com"
	}
	
	// Set Jira OAuth defaults
	if len(cfg.OAuth.Jira.Scopes) == 0 {
		cfg.OAuth.Jira.Scopes = []string{"read:jira-work", "read:jira-user"}
	}
	
	// Set Azure DevOps OAuth defaults
	if len(cfg.OAuth.Azure.Scopes) == 0 {
		cfg.OAuth.Azure.Scopes = []string{"vso.code", "vso.project", "vso.work"}
	}
	
	// Server defaults
	if cfg.Server.Host == "" {
		cfg.Server.Host = "0.0.0.0"
	}
	
	if cfg.Server.Port == 0 {
		cfg.Server.Port = 8080
	}
	
	if cfg.Server.ReadTimeout == 0 {
		cfg.Server.ReadTimeout = 30
	}
	
	if cfg.Server.WriteTimeout == 0 {
		cfg.Server.WriteTimeout = 30
	}
	
	if cfg.Server.ShutdownTimeout == 0 {
		cfg.Server.ShutdownTimeout = 10
	}
	
	// API defaults
	if cfg.Go.API.Port == 0 {
		cfg.Go.API.Port = 8080
	}
	
	if cfg.Go.API.ReadTimeoutSeconds == 0 {
		cfg.Go.API.ReadTimeoutSeconds = 30
	}
	
	if cfg.Go.API.WriteTimeoutSeconds == 0 {
		cfg.Go.API.WriteTimeoutSeconds = 30
	}
	
	if cfg.Go.API.ShutdownTimeoutSeconds == 0 {
		cfg.Go.API.ShutdownTimeoutSeconds = 10
	}
	
	if cfg.Go.API.LogLevel == "" {
		cfg.Go.API.LogLevel = "info"
	}
	
	// Health defaults
	if !cfg.Go.Health.Enabled {
		cfg.Go.Health.Enabled = true
	}
	
	if cfg.Go.Health.Path == "" {
		cfg.Go.Health.Path = "/health"
	}
	
	// Metrics defaults
	if !cfg.Go.Metrics.Enabled {
		cfg.Go.Metrics.Enabled = true
	}
	
	if cfg.Go.Metrics.Path == "" {
		cfg.Go.Metrics.Path = "/metrics"
	}
	
	// Backend defaults
	if cfg.Go.Backend.JavaURL == "" {
		cfg.Go.Backend.JavaURL = "http://localhost:8090/api/v1"
	}
	
	if cfg.Go.Backend.ConnectionTimeoutMS == 0 {
		cfg.Go.Backend.ConnectionTimeoutMS = 5000
	}
	
	if cfg.Go.Backend.RequestTimeoutMS == 0 {
		cfg.Go.Backend.RequestTimeoutMS = 10000
	}
	
	// Rate limiting defaults
	if !cfg.RateLimit.Enabled {
		cfg.RateLimit.Enabled = true // Enable rate limiting by default
	}
	
	if cfg.RateLimit.DefaultLimit == 0 {
		cfg.RateLimit.DefaultLimit = 300 // 300 requests per minute
	}
	
	if cfg.RateLimit.BurstLimit == 0 {
		cfg.RateLimit.BurstLimit = 50 // 50 burst requests
	}
	
	if cfg.RateLimit.DefaultPenaltyTime == 0 {
		cfg.RateLimit.DefaultPenaltyTime = 60 // 60 seconds penalty
	}
	
	// Initialize maps if nil
	if cfg.RateLimit.CustomIPLimits == nil {
		cfg.RateLimit.CustomIPLimits = make(map[string]int)
	}
	
	if cfg.RateLimit.CustomProjectLimits == nil {
		cfg.RateLimit.CustomProjectLimits = make(map[string]int)
	}
	
	if cfg.RateLimit.CustomPathLimits == nil {
		cfg.RateLimit.CustomPathLimits = make(map[string]int)
		
		// Set some sensible defaults for specific endpoints
		cfg.RateLimit.CustomPathLimits["/api/v1/auth/*"] = 60    // 60 requests per minute for auth endpoints
		cfg.RateLimit.CustomPathLimits["/api/v1/oauth/*"] = 60   // 60 requests per minute for oauth endpoints
		cfg.RateLimit.CustomPathLimits["/api/v1/webhooks/*"] = 600 // 600 requests per minute for webhooks
		cfg.RateLimit.CustomPathLimits["/health"] = 1200      // 1200 requests per minute for health checks
		cfg.RateLimit.CustomPathLimits["/metrics"] = 600      // 600 requests per minute for metrics
	}
	
	// Logging defaults
	if cfg.Logging.Level == "" {
		cfg.Logging.Level = "info"
	}
	
	if cfg.Logging.Format == "" {
		cfg.Logging.Format = "json" // Use JSON format by default
	}
	
	if cfg.Logging.FilePath == "" {
		cfg.Logging.FilePath = "/var/log/rinna/api.log" // Default log path
	}
	
	if cfg.Logging.MaxSize == 0 {
		cfg.Logging.MaxSize = 100 // 100 MB
	}
	
	if cfg.Logging.MaxAge == 0 {
		cfg.Logging.MaxAge = 30 // 30 days
	}
	
	if cfg.Logging.MaxBackups == 0 {
		cfg.Logging.MaxBackups = 10 // Keep 10 backups
	}
	
	// Default sensitive paths that need redaction
	if len(cfg.Logging.RedactPaths) == 0 {
		cfg.Logging.RedactPaths = []string{
			"/api/v1/auth/",
			"/api/v1/oauth/",
			"/api/v1/token/",
		}
	}
	
	// Initialize custom fields map if nil
	if cfg.Logging.CustomFields == nil {
		cfg.Logging.CustomFields = make(map[string]string)
		cfg.Logging.CustomFields["service"] = "rinna-api"
		cfg.Logging.CustomFields["environment"] = cfg.Project.Environment
	}
}