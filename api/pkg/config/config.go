/*
 * Configuration for the Rinna API server
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package config

import (
	"path/filepath"
	"runtime"
	"strings"

	"github.com/spf13/viper"
)

// Config represents the application configuration
type Config struct {
	Server     ServerConfig     `mapstructure:"server"`
	Java       JavaServiceConfig `mapstructure:"java"`
	Logging    LoggingConfig    `mapstructure:"logging"`
	Auth       AuthConfig       `mapstructure:"auth"`
}

// ServerConfig holds server-specific configuration
type ServerConfig struct {
	Port            int    `mapstructure:"port"`
	Host            string `mapstructure:"host"`
	ShutdownTimeout int    `mapstructure:"shutdownTimeout"`
}

// JavaServiceConfig holds configuration for Java service communication
type JavaServiceConfig struct {
	Command     string `mapstructure:"command"`
	Host        string `mapstructure:"host"`
	Port        int    `mapstructure:"port"`
	ConnectTimeout int  `mapstructure:"connectTimeout"`
	RequestTimeout int  `mapstructure:"requestTimeout"`
}

// LoggingConfig holds logging configuration
type LoggingConfig struct {
	Level  string `mapstructure:"level"`
	Format string `mapstructure:"format"`
	File   string `mapstructure:"file"`
}

// AuthConfig holds authentication configuration
type AuthConfig struct {
	TokenSecret       string `mapstructure:"tokenSecret"`
	TokenExpiry       int    `mapstructure:"tokenExpiry"` // in minutes
	AllowedOrigins    []string `mapstructure:"allowedOrigins"`
}

// DefaultConfig returns a default configuration
func DefaultConfig() *Config {
	return &Config{
		Server: ServerConfig{
			Port:            8080,
			Host:            "localhost",
			ShutdownTimeout: 15,
		},
		Java: JavaServiceConfig{
			Command:        "java",
			Host:           "localhost",
			Port:           8081,
			ConnectTimeout: 5000,
			RequestTimeout: 30000,
		},
		Logging: LoggingConfig{
			Level:  "info",
			Format: "json",
			File:   "",
		},
		Auth: AuthConfig{
			TokenSecret:     "rinna-development-secret-key",
			TokenExpiry:     60 * 24, // 1 day in minutes
			AllowedOrigins:  []string{"http://localhost:3000"},
		},
	}
}

// Load loads configuration from config files and environment variables
func Load(configPath string) (*Config, error) {
	// Set defaults
	config := DefaultConfig()

	// Setup viper
	v := viper.New()
	v.SetConfigName("config")
	v.SetConfigType("yaml")
	
	if configPath != "" {
		// Use specified config file
		v.SetConfigFile(configPath)
	} else {
		// Add default config paths
		_, b, _, _ := runtime.Caller(0)
		basepath := filepath.Dir(filepath.Dir(filepath.Dir(b)))
		v.AddConfigPath(filepath.Join(basepath, "configs"))
		v.AddConfigPath(".")
		v.AddConfigPath("./configs")
		v.AddConfigPath("/etc/rinna")
	}
	
	// Environment variables
	v.SetEnvPrefix("RINNA")
	v.SetEnvKeyReplacer(strings.NewReplacer(".", "_"))
	v.AutomaticEnv()
	
	// Read config
	if err := v.ReadInConfig(); err != nil {
		// Config file not found is not fatal
		if _, ok := err.(viper.ConfigFileNotFoundError); !ok {
			return nil, err
		}
	}
	
	// Unmarshal config
	if err := v.Unmarshal(config); err != nil {
		return nil, err
	}
	
	return config, nil
}