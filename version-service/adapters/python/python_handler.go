/*
 * Python version handler
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package python_adapter

import (
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strings"
	"time"

	"github.com/heymumford/Rinna/version-service/core"
)

// PythonFileHandler handles reading and writing version information to Python files
type PythonFileHandler struct {
	ProjectRoot  string
	ConfigPath   string
	PyProjectPath string
	VenvPath     string
}

// NewPythonFileHandler creates a new Python file handler
func NewPythonFileHandler(projectRoot string) *PythonFileHandler {
	return &PythonFileHandler{
		ProjectRoot:  projectRoot,
		ConfigPath:   filepath.Join(projectRoot, "bin", "rinna_config.py"),
		PyProjectPath: filepath.Join(projectRoot, "pyproject.toml"),
		VenvPath:     filepath.Join(projectRoot, ".venv", "version"),
	}
}

// ReadVersion reads the version from Python configuration
func (h *PythonFileHandler) ReadVersion() (*core.VersionInfo, error) {
	// First try to get the version from running the config module
	if _, err := os.Stat(h.ConfigPath); err == nil {
		cmd := exec.Command("python3", h.ConfigPath, "project.version")
		cmd.Dir = h.ProjectRoot
		output, err := cmd.Output()
		if err == nil && len(output) > 0 {
			versionStr := strings.TrimSpace(string(output))
			version, err := core.FromString(versionStr)
			if err == nil {
				return version, nil
			}
		}
	}
	
	// Next, try pyproject.toml if it exists
	if _, err := os.Stat(h.PyProjectPath); err == nil {
		content, err := os.ReadFile(h.PyProjectPath)
		if err == nil {
			re := regexp.MustCompile(`version\s*=\s*"([^"]*)"`)
			matches := re.FindSubmatch(content)
			if len(matches) >= 2 {
				versionStr := string(matches[1])
				version, err := core.FromString(versionStr)
				if err == nil {
					return version, nil
				}
			}
		}
	}
	
	// Finally, try the virtual env version file
	if _, err := os.Stat(h.VenvPath); err == nil {
		content, err := os.ReadFile(h.VenvPath)
		if err == nil {
			versionStr := strings.TrimSpace(string(content))
			version, err := core.FromString(versionStr)
			if err == nil {
				return version, nil
			}
		}
	}
	
	// If all methods fail, return an error
	return nil, errors.New("no valid Python version file found")
}

// WriteVersion updates Python version files
func (h *PythonFileHandler) WriteVersion(version *core.VersionInfo) error {
	// Python config reads directly from version.properties, so we don't need to modify it
	// Just touch the file to force reload
	if _, err := os.Stat(h.ConfigPath); err == nil {
		now := time.Now()
		err := os.Chtimes(h.ConfigPath, now, now)
		if err != nil {
			return fmt.Errorf("failed to touch Python config file: %w", err)
		}
	}
	
	// Update pyproject.toml if it exists
	if _, err := os.Stat(h.PyProjectPath); err == nil {
		content, err := os.ReadFile(h.PyProjectPath)
		if err != nil {
			return fmt.Errorf("failed to read pyproject.toml: %w", err)
		}
		
		contentStr := string(content)
		re := regexp.MustCompile(`(version\s*=\s*)"[^"]*"`)
		contentStr = re.ReplaceAllString(contentStr, fmt.Sprintf("${1}\"%s\"", version.FullVersion))
		
		err = os.WriteFile(h.PyProjectPath, []byte(contentStr), 0644)
		if err != nil {
			return fmt.Errorf("failed to write pyproject.toml: %w", err)
		}
	}
	
	// Update virtual env version file if it exists
	if _, err := os.Stat(filepath.Dir(h.VenvPath)); err == nil {
		err := os.WriteFile(h.VenvPath, []byte(version.FullVersion), 0644)
		if err != nil {
			return fmt.Errorf("failed to write virtual env version file: %w", err)
		}
	}
	
	return nil
}

// VerifyVersion checks if Python files have the correct version
func (h *PythonFileHandler) VerifyVersion(expectedVersion *core.VersionInfo) (bool, string, error) {
	// Create a temporary Python script to read version directly from the properties file
	// This ensures we validate what will actually be used at runtime
	tempScriptPath := filepath.Join(os.TempDir(), fmt.Sprintf("rinna_version_test_%d.py", time.Now().UnixNano()))
	defer os.Remove(tempScriptPath)
	
	scriptContent := `
import os, sys
script_dir = os.path.dirname(sys.argv[1])
project_root = os.path.abspath(os.path.join(script_dir, ".."))
version_file = os.path.join(project_root, "version.properties")
with open(version_file, 'r') as f:
    for line in f:
        line = line.strip()
        if line.startswith("version="):
            print(line.split("=", 1)[1].strip())
            break
`
	
	err := os.WriteFile(tempScriptPath, []byte(scriptContent), 0644)
	if err != nil {
		return false, "", fmt.Errorf("failed to create version test script: %w", err)
	}
	
	cmd := exec.Command("python3", tempScriptPath, h.ConfigPath)
	output, err := cmd.Output()
	if err != nil {
		return false, fmt.Sprintf("Failed to run Python test script: %s", err), nil
	}
	
	versionStr := strings.TrimSpace(string(output))
	if versionStr != expectedVersion.FullVersion {
		return false, fmt.Sprintf("Python version mismatch: %s (should be %s)", versionStr, expectedVersion.FullVersion), nil
	}
	
	// Check pyproject.toml if it exists
	if _, err := os.Stat(h.PyProjectPath); err == nil {
		content, err := os.ReadFile(h.PyProjectPath)
		if err == nil {
			re := regexp.MustCompile(`version\s*=\s*"([^"]*)"`)
			matches := re.FindSubmatch(content)
			if len(matches) >= 2 {
				pyProjectVersion := string(matches[1])
				if pyProjectVersion != expectedVersion.FullVersion {
					return false, fmt.Sprintf("pyproject.toml version mismatch: %s (should be %s)", pyProjectVersion, expectedVersion.FullVersion), nil
				}
			}
		}
	}
	
	// Check virtual env version file if it exists
	if _, err := os.Stat(h.VenvPath); err == nil {
		content, err := os.ReadFile(h.VenvPath)
		if err == nil {
			venvVersion := strings.TrimSpace(string(content))
			if venvVersion != expectedVersion.FullVersion {
				return false, fmt.Sprintf("Virtual env version mismatch: %s (should be %s)", venvVersion, expectedVersion.FullVersion), nil
			}
		}
	}
	
	return true, "", nil
}

