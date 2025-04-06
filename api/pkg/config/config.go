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
	Project  ProjectConfig     `mapstructure:"project"`
	Security SecurityConfig    `mapstructure:"security"`
	Auth     AuthConfig        `mapstructure:"auth"`
	Go       GoConfig          `mapstructure:"go"`
	Server   ServerConfig      `mapstructure:"server"`
	Java     JavaServiceConfig `mapstructure:"java"`
}

// AuthConfig contains authentication-related configuration.
type AuthConfig struct {
	TokenExpiry         int      `mapstructure:"token_expiry_minutes"`
	SecretExpiry        int      `mapstructure:"secret_expiry_minutes"`
	WebhookSecretExpiry int      `mapstructure:"webhook_secret_expiry_minutes"`
	AllowedSources      []string `mapstructure:"allowed_sources"`
	AllowedOrigins      []string `mapstructure:"allowed_origins"`
	DevMode             bool     `mapstructure:"dev_mode"`
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
}