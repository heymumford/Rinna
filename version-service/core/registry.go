/*
 * Version registry interface
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package core

// VersionRegistry defines the interface for interacting with the version management system
type VersionRegistry interface {
	// GetVersion returns the current version information
	GetVersion() (*VersionInfo, error)
	
	// SetVersion sets a specific version
	SetVersion(version *VersionInfo) error
	
	// BumpMajor increments the major version
	BumpMajor() (*VersionInfo, error)
	
	// BumpMinor increments the minor version
	BumpMinor() (*VersionInfo, error)
	
	// BumpPatch increments the patch version
	BumpPatch() (*VersionInfo, error)
	
	// VerifyConsistency checks if all version files are consistent
	VerifyConsistency() (bool, []string, error)
	
	// UpdateAllFiles updates all files to match the current version
	UpdateAllFiles() error
	
	// CreateRelease creates a release for the current version
	CreateRelease(message string, forceGithub bool) error
	
	// CreateGitTag creates a git tag for the current version
	CreateGitTag(message string) error
}

// FileHandler defines the interface for language-specific file operations
type FileHandler interface {
	// ReadVersion reads the version from a language-specific file
	ReadVersion() (*VersionInfo, error)
	
	// WriteVersion writes the version to a language-specific file
	WriteVersion(version *VersionInfo) error
	
	// VerifyVersion checks if the language-specific file has the correct version
	VerifyVersion(expectedVersion *VersionInfo) (bool, string, error)
}

// OperationResult contains the result of a version operation
type OperationResult struct {
	Success bool
	Message string
	Error   error
	Version *VersionInfo
}

// NewOperationResult creates a new operation result
func NewOperationResult(success bool, message string, err error, version *VersionInfo) *OperationResult {
	return &OperationResult{
		Success: success,
		Message: message,
		Error:   err,
		Version: version,
	}
}