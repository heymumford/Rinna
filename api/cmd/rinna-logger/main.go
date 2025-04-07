/*
 * Go logging bridge for the multi-language logging system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package main

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"unicode"

	"github.com/heymumford/rinna/api/pkg/logger"
)

// isValidFieldKey checks if a field key is valid (alphanumeric with underscores)
func isValidFieldKey(key string) bool {
	for _, r := range key {
		if !unicode.IsLetter(r) && !unicode.IsDigit(r) && r != '_' {
			return false
		}
	}
	return true
}

// sanitizeFieldKey converts an invalid field key to a valid one
func sanitizeFieldKey(key string) string {
	var result strings.Builder
	for _, r := range key {
		if unicode.IsLetter(r) || unicode.IsDigit(r) || r == '_' {
			result.WriteRune(r)
		} else {
			result.WriteRune('_')
		}
	}
	return result.String()
}

// ensureLogDirectory ensures the log directory exists
func ensureLogDirectory(dir string) error {
	// Check if directory exists
	info, err := os.Stat(dir)
	if err == nil {
		if info.IsDir() {
			return nil // Directory exists
		}
		return fmt.Errorf("path exists but is not a directory: %s", dir)
	}
	
	// Create directory with parents
	if os.IsNotExist(err) {
		if err := os.MkdirAll(dir, 0755); err != nil {
			return fmt.Errorf("failed to create log directory: %s", err)
		}
		return nil
	}
	
	return err
}

func main() {
	// Define command-line flags
	level := flag.String("level", "INFO", "Log level (TRACE, DEBUG, INFO, WARN, ERROR)")
	name := flag.String("name", "go_bridge", "Logger name")
	message := flag.String("message", "", "Message to log")
	
	// Define fields as a slice flag
	var fields []string
	flag.Func("field", "Context field in the format key=value (can be specified multiple times)", func(s string) error {
		fields = append(fields, s)
		return nil
	})

	flag.Parse()

	// Create logger configuration
	logDir := os.Getenv("RINNA_LOG_DIR")
	if logDir == "" {
		// Default to ~/.rinna/logs
		home, err := os.UserHomeDir()
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error getting user home directory: %v\n", err)
			os.Exit(1)
		}
		logDir = filepath.Join(home, ".rinna", "logs")
	}
	
	// Ensure the log directory exists
	if err := ensureLogDirectory(logDir); err != nil {
		fmt.Fprintf(os.Stderr, "Error creating log directory: %v\n", err)
		// Continue anyway, using stderr only
	}

	logFile := filepath.Join(logDir, "rinna-go.log")

	// Create the logger
	config := logger.DefaultConfig()
	config.LogFile = logFile
	config.ShowCaller = true

	// Set log level
	switch strings.ToUpper(*level) {
	case "TRACE":
		config.Level = logger.TraceLevel
	case "DEBUG":
		config.Level = logger.DebugLevel
	case "INFO":
		config.Level = logger.InfoLevel
	case "WARN", "WARNING":
		config.Level = logger.WarnLevel
	case "ERROR":
		config.Level = logger.ErrorLevel
	case "FATAL":
		config.Level = logger.FatalLevel
	default:
		fmt.Fprintf(os.Stderr, "Unknown log level '%s', defaulting to INFO\n", *level)
		config.Level = logger.InfoLevel
	}

	// Create the logger with prefix (name)
	log := logger.NewWithConfig(config).WithPrefix(*name)

	// Parse fields with validation
	fieldMap := make(map[string]interface{})
	for _, field := range fields {
		parts := strings.SplitN(field, "=", 2)
		if len(parts) == 2 {
			key := strings.TrimSpace(parts[0])
			value := strings.TrimSpace(parts[1])
			
			// Validate key (non-empty and alphanumeric with underscores)
			if key == "" {
				fmt.Fprintf(os.Stderr, "Warning: Empty field key found, skipping\n")
				continue
			}
			
			if !isValidFieldKey(key) {
				fmt.Fprintf(os.Stderr, "Warning: Invalid field key '%s', using sanitized version\n", key)
				key = sanitizeFieldKey(key)
			}
			
			fieldMap[key] = value
		} else {
			fmt.Fprintf(os.Stderr, "Warning: Invalid field format '%s', expected key=value\n", field)
		}
	}

	// Add fields to logger if any
	if len(fieldMap) > 0 {
		log = log.WithFields(fieldMap)
	}

	// Log message with appropriate level
	switch strings.ToUpper(*level) {
	case "TRACE":
		log.Trace(*message)
	case "DEBUG":
		log.Debug(*message)
	case "INFO":
		log.Info(*message)
	case "WARN", "WARNING":
		log.Warn(*message)
	case "ERROR":
		log.Error(*message)
	case "FATAL":
		// Use Error instead of Fatal to avoid program termination
		log.Error(*message)
	default:
		log.Info(*message)
	}
}