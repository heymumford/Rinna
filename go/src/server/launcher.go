/*
 * Server launcher for the Rinna API - automatically manages local server
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package server

import (
	"context"
	"fmt"
	"net"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"sync"
	"time"

	"github.com/heymumford/rinna/api/pkg/config"
	"github.com/heymumford/rinna/api/pkg/logger"
)

// ServerLauncher manages the lifecycle of the local Rinna server
type ServerLauncher struct {
	cfg          *config.RinnaConfig
	mutex        sync.Mutex
	cmd          *exec.Cmd
	isRunning    bool
	serverPort   int
	serverHost   string
	processStarted bool
	processFinished chan struct{}
}

// NewServerLauncher creates a new server launcher
func NewServerLauncher(cfg *config.RinnaConfig) *ServerLauncher {
	return &ServerLauncher{
		cfg:          cfg,
		isRunning:    false,
		serverPort:   cfg.Java.Port,
		serverHost:   cfg.Java.Host,
		processFinished: make(chan struct{}),
	}
}

// IsExternalServerConfigured checks if an external server is configured
func (l *ServerLauncher) IsExternalServerConfigured() bool {
	// Check if RINNA_EXTERNAL_SERVER env var is set
	if os.Getenv("RINNA_EXTERNAL_SERVER") != "" {
		return true
	}

	// Check if server is configured in config file
	if l.cfg.Java.Host != "localhost" && l.cfg.Java.Host != "127.0.0.1" {
		return true
	}

	// Check if JAVA_BACKEND_URL env var is set
	if os.Getenv("JAVA_BACKEND_URL") != "" {
		return true
	}

	return false
}

// IsServerRunning checks if a server is already running on the configured port
func (l *ServerLauncher) IsServerRunning() bool {
	l.mutex.Lock()
	defer l.mutex.Unlock()

	if l.processStarted && l.cmd != nil && l.cmd.Process != nil {
		// Our process is still running
		return true
	}

	// Check if any process is listening on the port
	conn, err := net.DialTimeout(
		"tcp",
		fmt.Sprintf("%s:%d", l.serverHost, l.serverPort), 
		time.Second,
	)
	if err != nil {
		return false
	}
	conn.Close()
	return true
}

// StartLocalServer launches a local Rinna server if one is not already running
func (l *ServerLauncher) StartLocalServer(ctx context.Context) error {
	l.mutex.Lock()
	defer l.mutex.Unlock()

	// Check if our server is already running
	if l.processStarted && l.cmd != nil && l.cmd.Process != nil {
		return nil
	}

	// Check if any server is already running
	conn, err := net.DialTimeout(
		"tcp",
		fmt.Sprintf("%s:%d", l.serverHost, l.serverPort), 
		time.Second,
	)
	if err == nil {
		conn.Close()
		// A server is already running, no need to start one
		logger.Info("External Rinna server already running", 
			logger.Field("host", l.serverHost),
			logger.Field("port", l.serverPort))
		return nil
	}

	// Determine the Java executable path
	javaCmd := "java"
	if javaPath := os.Getenv("JAVA_HOME"); javaPath != "" {
		if runtime.GOOS == "windows" {
			javaCmd = filepath.Join(javaPath, "bin", "java.exe")
		} else {
			javaCmd = filepath.Join(javaPath, "bin", "java")
		}
	}

	// Determine the JAR file path
	jarPath := locateJarFile()
	if jarPath == "" {
		return fmt.Errorf("could not locate Rinna JAR file")
	}

	// Prepare to start the server
	logger.Info("Starting local Rinna server", 
		logger.Field("host", l.serverHost),
		logger.Field("port", l.serverPort),
		logger.Field("jar", jarPath))

	// Build command to start the server
	l.cmd = exec.Command(javaCmd, "-jar", jarPath, 
		"--server.host=" + l.serverHost,
		"--server.port=" + strconv.Itoa(l.serverPort))

	// Set up log redirection
	l.cmd.Stdout = os.Stdout
	l.cmd.Stderr = os.Stderr

	// Start the server
	if err := l.cmd.Start(); err != nil {
		return fmt.Errorf("failed to start local Rinna server: %v", err)
	}

	l.processStarted = true

	// Wait for the server to be available
	startTimeout := time.Now().Add(30 * time.Second)
	for time.Now().Before(startTimeout) {
		if isPortOpen(l.serverHost, l.serverPort) {
			// Verify it's our Rinna server by checking health endpoint
			if isRinnaServer(l.serverHost, l.serverPort) {
				logger.Info("Local Rinna server started successfully", 
					logger.Field("pid", l.cmd.Process.Pid))
				
				// Monitor the process in the background
				go l.monitorProcess()
				return nil
			}
		}
		time.Sleep(500 * time.Millisecond)
	}

	// If we get here, the server didn't start successfully
	if l.cmd.Process != nil {
		l.cmd.Process.Kill() // Clean up the process
	}
	return fmt.Errorf("timed out waiting for local Rinna server to start")
}

// StopLocalServer stops the local Rinna server if it was started by this launcher
func (l *ServerLauncher) StopLocalServer() error {
	l.mutex.Lock()
	defer l.mutex.Unlock()

	if !l.processStarted || l.cmd == nil || l.cmd.Process == nil {
		return nil
	}

	logger.Info("Stopping local Rinna server", logger.Field("pid", l.cmd.Process.Pid))

	// Try graceful shutdown first
	gracefulShutdown(l.serverHost, l.serverPort)

	// Give it a few seconds to shut down gracefully
	select {
	case <-l.processFinished:
		logger.Info("Local Rinna server shut down gracefully")
		return nil
	case <-time.After(5 * time.Second):
		// If it doesn't shut down gracefully, kill it
		if err := l.cmd.Process.Kill(); err != nil {
			return fmt.Errorf("failed to kill local Rinna server: %v", err)
		}
		logger.Info("Local Rinna server killed")
	}

	l.processStarted = false
	return nil
}

// monitorProcess monitors the Java process and updates state when it exits
func (l *ServerLauncher) monitorProcess() {
	// Wait for the process to complete
	if l.cmd != nil && l.cmd.Process != nil {
		err := l.cmd.Wait()
		
		l.mutex.Lock()
		defer l.mutex.Unlock()
		
		exitCode := 0
		if err != nil {
			if exitErr, ok := err.(*exec.ExitError); ok {
				exitCode = exitErr.ExitCode()
			}
		}
		
		logger.Info("Local Rinna server process exited", 
			logger.Field("exitCode", exitCode))
		
		l.processStarted = false
		close(l.processFinished)
		l.processFinished = make(chan struct{})
	}
}

// Helper functions

// Function types for easy mocking in tests
type locateJarFileFn func() string
type isPortOpenFn func(host string, port int) bool
type isRinnaServerFn func(host string, port int) bool

// Package variables that can be replaced in tests
var (
	locateJarFile locateJarFileFn = defaultLocateJarFile
	isPortOpen isPortOpenFn = defaultIsPortOpen
	isRinnaServer isRinnaServerFn = defaultIsRinnaServer
)

// defaultLocateJarFile attempts to find the Rinna JAR file
func defaultLocateJarFile() string {
	// Common locations to check
	locations := []string{
		"./rinna-server.jar",
		"./rinna-server/target/rinna-server.jar",
		"../rinna-server/target/rinna-server.jar",
	}

	// Add the RINNA_JAR environment variable if set
	if jarPath := os.Getenv("RINNA_JAR"); jarPath != "" {
		locations = append([]string{jarPath}, locations...)
	}

	// Check home directory
	if homedir, err := os.UserHomeDir(); err == nil {
		locations = append(locations, 
			filepath.Join(homedir, ".rinna/rinna-server.jar"),
			filepath.Join(homedir, ".rinna/lib/rinna-server.jar"),
		)
	}

	for _, loc := range locations {
		if _, err := os.Stat(loc); err == nil {
			return loc
		}
	}

	return ""
}

// defaultIsPortOpen checks if a port is open on the specified host
func defaultIsPortOpen(host string, port int) bool {
	conn, err := net.DialTimeout(
		"tcp",
		fmt.Sprintf("%s:%d", host, port),
		time.Second,
	)
	if err != nil {
		return false
	}
	conn.Close()
	return true
}

// defaultIsRinnaServer verifies that the server running on the port is a Rinna server
func defaultIsRinnaServer(host string, port int) bool {
	// Create an HTTP client with timeout
	client := &http.Client{
		Timeout: 2 * time.Second,
	}

	// Call the health endpoint
	url := fmt.Sprintf("http://%s:%d/health", host, port)
	resp, err := client.Get(url)
	if err != nil {
		return false
	}
	defer resp.Body.Close()

	// If the server responds with OK, it's likely a Rinna server
	return resp.StatusCode == http.StatusOK
}

// gracefulShutdown attempts to shut down the server gracefully
func gracefulShutdown(host string, port int) {
	// Attempt to call a graceful shutdown endpoint if one exists
	client := &http.Client{
		Timeout: 2 * time.Second,
	}

	// Try the actuator endpoint if it exists
	url := fmt.Sprintf("http://%s:%d/actuator/shutdown", host, port)
	req, err := http.NewRequest(http.MethodPost, url, nil)
	if err == nil {
		client.Do(req) // Ignore errors, best effort
	}

	// Also try a custom shutdown endpoint
	url = fmt.Sprintf("http://%s:%d/admin/shutdown", host, port)
	req, err = http.NewRequest(http.MethodPost, url, nil)
	if err == nil {
		client.Do(req) // Ignore errors, best effort
	}
}