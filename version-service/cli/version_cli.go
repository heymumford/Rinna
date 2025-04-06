/*
 * Version CLI tool
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

	"github.com/heymumford/Rinna/version-service/adapters/bash"
	"github.com/heymumford/Rinna/version-service/adapters/go"
	"github.com/heymumford/Rinna/version-service/adapters/java"
	"github.com/heymumford/Rinna/version-service/adapters/python"
	"github.com/heymumford/Rinna/version-service/core"
)

// Command-line flags
var (
	cmdCurrent     bool
	cmdMajor       bool
	cmdMinor       bool
	cmdPatch       bool
	cmdSet         string
	cmdVerify      bool
	cmdUpdate      bool
	cmdRelease     bool
	cmdTag         bool
	msgFlag        string
	forceGithubFlag bool
)

func main() {
	// Define command flags
	flag.BoolVar(&cmdCurrent, "current", false, "Show current version information")
	flag.BoolVar(&cmdMajor, "major", false, "Bump major version (x.0.0)")
	flag.BoolVar(&cmdMinor, "minor", false, "Bump minor version (0.x.0)")
	flag.BoolVar(&cmdPatch, "patch", false, "Bump patch version (0.0.x)")
	flag.StringVar(&cmdSet, "set", "", "Set to specific version (e.g., 1.2.3)")
	flag.BoolVar(&cmdVerify, "verify", false, "Verify version consistency across files")
	flag.BoolVar(&cmdUpdate, "update", false, "Update all files to match version.properties")
	flag.BoolVar(&cmdRelease, "release", false, "Create a release from current version")
	flag.BoolVar(&cmdTag, "tag", false, "Create a git tag for current version")
	
	// Define option flags
	flag.StringVar(&msgFlag, "message", "", "Custom release/commit message")
	flag.StringVar(&msgFlag, "m", "", "Custom release/commit message (shorthand)")
	flag.BoolVar(&forceGithubFlag, "github", false, "Force GitHub release creation")
	flag.BoolVar(&forceGithubFlag, "g", false, "Force GitHub release creation (shorthand)")
	
	// Parse flags
	flag.Parse()
	
	// Determine project root
	exePath, err := os.Executable()
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}
	
	exeDir := filepath.Dir(exePath)
	
	// If we're running from the bin directory in the version service, adjust the path
	if filepath.Base(exeDir) == "bin" && filepath.Base(filepath.Dir(exeDir)) == "version-service" {
		exeDir = filepath.Dir(filepath.Dir(exeDir))
	} else if filepath.Base(exeDir) == "version-service" {
		exeDir = filepath.Dir(exeDir)
	}
	
	projectRoot := exeDir
	propertiesPath := filepath.Join(projectRoot, "version.properties")
	
	// Create version registry
	registry, err := core.NewPropertiesRegistry(propertiesPath, projectRoot)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}
	
	// Register handlers for different languages
	registerHandlers(registry, projectRoot)
	
	// Execute the requested command
	err = executeCommand(registry)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}
}

func registerHandlers(registry *core.PropertiesRegistry, projectRoot string) {
	// Verify key directories exist before registering handlers
	apiDir := filepath.Join(projectRoot, "api")
	apiPkgDir := filepath.Join(apiDir, "pkg", "health")
	apiInternalDir := filepath.Join(apiDir, "internal", "version")
	
	// Register Go handler if the directories exist
	if dirExists(apiPkgDir) || dirExists(apiInternalDir) {
		goHandler := go_adapter.NewGoFileHandler([]string{
			filepath.Join(apiPkgDir, "version.go"),
			filepath.Join(apiInternalDir, "version.go"),
		})
		registry.RegisterHandler(goHandler)
	}
	
	// Register Maven handler if pom.xml exists
	if fileExists(filepath.Join(projectRoot, "pom.xml")) {
		mavenHandler := java_adapter.NewMavenFileHandler(projectRoot)
		registry.RegisterHandler(mavenHandler)
	}
	
	// Register Python handler if Python files exist
	if fileExists(filepath.Join(projectRoot, "bin", "rinna_config.py")) {
		pythonHandler := python_adapter.NewPythonFileHandler(projectRoot)
		registry.RegisterHandler(pythonHandler)
	}
	
	// Register README handler if README.md exists
	if fileExists(filepath.Join(projectRoot, "README.md")) {
		readmeHandler := bash_adapter.NewReadmeFileHandler(projectRoot)
		registry.RegisterHandler(readmeHandler)
	}
}

// Helper function to check if a directory exists
func dirExists(path string) bool {
	info, err := os.Stat(path)
	if err != nil {
		return false
	}
	return info.IsDir()
}

// Helper function to check if a file exists
func fileExists(path string) bool {
	info, err := os.Stat(path)
	if err != nil {
		return false
	}
	return !info.IsDir()
}

func executeCommand(registry *core.PropertiesRegistry) error {
	switch {
	case cmdCurrent:
		return showCurrentVersion(registry)
	case cmdMajor:
		return bumpMajorVersion(registry)
	case cmdMinor:
		return bumpMinorVersion(registry)
	case cmdPatch:
		return bumpPatchVersion(registry)
	case cmdSet != "":
		return setSpecificVersion(registry, cmdSet)
	case cmdVerify:
		return verifyVersionConsistency(registry)
	case cmdUpdate:
		return updateAllFiles(registry)
	case cmdRelease:
		return createRelease(registry)
	case cmdTag:
		return createGitTag(registry)
	default:
		// If no command specified, show help
		flag.Usage()
		return nil
	}
}

func showCurrentVersion(registry *core.PropertiesRegistry) error {
	version, err := registry.GetVersion()
	if err != nil {
		return err
	}
	
	fmt.Printf("Current version: %s\n", version.FullVersion)
	fmt.Printf("  Major: %d, Minor: %d, Patch: %d\n", version.Major, version.Minor, version.Patch)
	fmt.Printf("  Last Updated: %s\n", version.LastUpdated)
	fmt.Printf("  Release Type: %s\n", version.ReleaseType)
	fmt.Printf("  Build Number: %d\n", version.BuildNumber)
	
	// Verify consistency
	isConsistent, _, err := registry.VerifyConsistency()
	if err != nil {
		return err
	}
	
	if isConsistent {
		fmt.Println("All versions are consistent!")
	} else {
		fmt.Println("Warning: Version inconsistencies found. Run with --verify for details.")
	}
	
	return nil
}

func bumpMajorVersion(registry *core.PropertiesRegistry) error {
	version, err := registry.BumpMajor()
	if err != nil {
		return err
	}
	
	fmt.Printf("Bumped version to %s\n", version.FullVersion)
	
	if msgFlag != "" {
		err = registry.CreateGitTag(msgFlag)
		if err != nil {
			return err
		}
		fmt.Printf("Created git tag: v%s\n", version.FullVersion)
	}
	
	return nil
}

func bumpMinorVersion(registry *core.PropertiesRegistry) error {
	version, err := registry.BumpMinor()
	if err != nil {
		return err
	}
	
	fmt.Printf("Bumped version to %s\n", version.FullVersion)
	
	if msgFlag != "" {
		err = registry.CreateGitTag(msgFlag)
		if err != nil {
			return err
		}
		fmt.Printf("Created git tag: v%s\n", version.FullVersion)
	}
	
	return nil
}

func bumpPatchVersion(registry *core.PropertiesRegistry) error {
	version, err := registry.BumpPatch()
	if err != nil {
		return err
	}
	
	fmt.Printf("Bumped version to %s\n", version.FullVersion)
	
	if msgFlag != "" {
		err = registry.CreateGitTag(msgFlag)
		if err != nil {
			return err
		}
		fmt.Printf("Created git tag: v%s\n", version.FullVersion)
	}
	
	return nil
}

func setSpecificVersion(registry *core.PropertiesRegistry, versionStr string) error {
	version, err := core.FromString(versionStr)
	if err != nil {
		return err
	}
	
	err = registry.SetVersion(version)
	if err != nil {
		return err
	}
	
	fmt.Printf("Set version to %s\n", version.FullVersion)
	
	if msgFlag != "" {
		err = registry.CreateGitTag(msgFlag)
		if err != nil {
			return err
		}
		fmt.Printf("Created git tag: v%s\n", version.FullVersion)
	}
	
	return nil
}

func verifyVersionConsistency(registry *core.PropertiesRegistry) error {
	isConsistent, inconsistencies, err := registry.VerifyConsistency()
	if err != nil {
		return err
	}
	
	version, err := registry.GetVersion()
	if err != nil {
		return err
	}
	
	fmt.Printf("Verifying version consistency for version %s\n", version.FullVersion)
	
	if !isConsistent {
		fmt.Println("Inconsistencies found:")
		for _, inconsistency := range inconsistencies {
			fmt.Printf("  - %s\n", inconsistency)
		}
		return fmt.Errorf("%d inconsistencies found", len(inconsistencies))
	}
	
	fmt.Println("All versions are consistent!")
	return nil
}

func updateAllFiles(registry *core.PropertiesRegistry) error {
	err := registry.UpdateAllFiles()
	if err != nil {
		return err
	}
	
	version, err := registry.GetVersion()
	if err != nil {
		return err
	}
	
	fmt.Printf("Updated all files to version %s\n", version.FullVersion)
	return nil
}

func createRelease(registry *core.PropertiesRegistry) error {
	err := registry.CreateRelease(msgFlag, forceGithubFlag)
	if err != nil {
		return err
	}
	
	version, err := registry.GetVersion()
	if err != nil {
		return err
	}
	
	fmt.Printf("Created release for version %s\n", version.FullVersion)
	return nil
}

func createGitTag(registry *core.PropertiesRegistry) error {
	err := registry.CreateGitTag(msgFlag)
	if err != nil {
		return err
	}
	
	version, err := registry.GetVersion()
	if err != nil {
		return err
	}
	
	fmt.Printf("Created git tag for version %s\n", version.FullVersion)
	return nil
}