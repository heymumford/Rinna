/*
 * Configuration utilities for auto-starting the Rinna server
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

	"github.com/heymumford/rinna/api/pkg/logger"
)

const (
	// DefaultConfigDir is the default directory for configuration files
	DefaultConfigDir = "${HOME}/.rinna/config"
	
	// DefaultConfigFile is the default configuration file name
	DefaultConfigFile = "config.yaml"
	
	// AutoStartKey is the key for the auto-start setting in the configuration
	AutoStartKey = "server.auto_start"
	
	// ExternalServerEnvVar is the environment variable for external server config
	ExternalServerEnvVar = "RINNA_EXTERNAL_SERVER"
)

// AutoStartConfig represents the auto-start configuration
type AutoStartConfig struct {
	Enabled            bool   `yaml:"enabled"`
	JavaServerPath     string `yaml:"java_server_path"`
	JavaServerJarName  string `yaml:"java_server_jar_name"`
	ServerHost         string `yaml:"host"`
	ServerPort         int    `yaml:"port"`
	ConnectTimeoutSecs int    `yaml:"connect_timeout_secs"`
}

// GetAutoStartConfigFile returns the path to the auto-start configuration file
func GetAutoStartConfigFile() string {
	// Check for the explicit environment variable
	if configPath := os.Getenv("RINNA_CONFIG_PATH"); configPath != "" {
		return configPath
	}
	
	// Check for the config directory
	configDir := os.ExpandEnv(DefaultConfigDir)
	if configDirEnv := os.Getenv("RINNA_CONFIG_DIR"); configDirEnv != "" {
		configDir = configDirEnv
	}
	
	// Build the config file path
	return filepath.Join(configDir, DefaultConfigFile)
}

// SaveAutoStartSettings saves the auto-start settings to the configuration file
func SaveAutoStartSettings(enabled bool, javaPath string, host string, port int) error {
	// Get the configuration file path
	configFile := GetAutoStartConfigFile()
	
	// Ensure the directory exists
	configDir := filepath.Dir(configFile)
	if err := os.MkdirAll(configDir, 0755); err != nil {
		return fmt.Errorf("failed to create config directory: %w", err)
	}
	
	// Build config content
	configContent := fmt.Sprintf(`# Rinna API Server Configuration
server:
  auto_start: %t
  java_server_path: "%s"
  host: "%s"
  port: %d
`, enabled, javaPath, host, port)
	
	// Write the file
	if err := os.WriteFile(configFile, []byte(configContent), 0644); err != nil {
		return fmt.Errorf("failed to write config file: %w", err)
	}
	
	logger.Info("Auto-start settings saved", 
		logger.Field("configFile", configFile),
		logger.Field("enabled", enabled))
	
	return nil
}

// IsAutoStartEnabled checks if server auto-start is enabled in the configuration
func IsAutoStartEnabled() bool {
	// Check for explicit environment variable
	if val := os.Getenv("RINNA_AUTO_START"); val != "" {
		return val == "true" || val == "1" || val == "yes"
	}
	
	// If external server is configured, auto-start is disabled
	if val := os.Getenv(ExternalServerEnvVar); val != "" {
		return false
	}
	
	// TODO: Check the configuration file if expanded
	// For now, default to enabled
	return true
}