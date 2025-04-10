#!/bin/bash
#
# Script to fix Maven repository caching issues for local dependencies
# This script:
# 1. Cleans the Maven repository cache for Rinna modules
# 2. Reinstalls modules in the correct order
# 3. Verifies dependency convergence

set -e

# ANSI color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Get current directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Check if M2_HOME is defined, use default if not
if [ -z "$M2_HOME" ]; then
    M2_HOME=~/.m2
    log_info "M2_HOME not set, using default: $M2_HOME"
fi

# Path to local repository
REPO_PATH="$M2_HOME/repository/org/rinna"

# Clean local Maven repository cache
clean_maven_repo() {
    log_info "Cleaning Maven repository cache for Rinna modules..."
    
    if [ -d "$REPO_PATH" ]; then
        rm -rf "$REPO_PATH"
        log_success "Removed cached Rinna artifacts from $REPO_PATH"
    else
        log_info "No cached Rinna artifacts found at $REPO_PATH"
    fi
}

# Install a module with proper caching behavior
install_module() {
    MODULE_PATH=$1
    MODULE_NAME=$2
    
    log_info "Installing $MODULE_NAME from $MODULE_PATH..."
    
    # Change to module directory
    cd "$PROJECT_ROOT/$MODULE_PATH"
    
    # Use -U flag to force update and skip tests
    mvn clean install -U -DskipTests -Dmaven.exec.skip=true
    
    if [ $? -eq 0 ]; then
        log_success "$MODULE_NAME installed successfully"
    else
        log_error "Failed to install $MODULE_NAME"
        exit 1
    fi
    
    # Return to project root
    cd "$PROJECT_ROOT"
}

# Check dependency convergence
check_convergence() {
    log_info "Checking dependency convergence..."
    
    # Run Maven enforcer plugin with dependencyConvergence rule
    mvn org.apache.maven.plugins:maven-enforcer-plugin:3.4.1:enforce \
        -Drules=dependencyConvergence \
        -DskipTests \
        -Dmaven.exec.skip=true \
        -q
    
    if [ $? -eq 0 ]; then
        log_success "Dependency convergence check passed"
    else
        log_warning "Dependency convergence check failed"
        log_info "You may need to resolve version conflicts manually"
    fi
}

# Verify installation
verify_installation() {
    log_info "Verifying module installation..."
    
    # Compile without running tests to verify dependencies resolve correctly
    mvn clean compile -Dmaven.test.skip=true -Dmaven.exec.skip=true
    
    if [ $? -eq 0 ]; then
        log_success "Module installation verification passed"
    else
        log_error "Module installation verification failed"
        log_info "There may still be dependency issues to resolve"
        exit 1
    fi
}

# Main execution flow
main() {
    log_info "Starting Maven repository caching fix for Rinna..."
    
    # Clean repository
    clean_maven_repo
    
    # Install modules in dependency order
    install_module "rinna-core" "Rinna Core"
    install_module "rinna-cli" "Rinna CLI"
    install_module "rinna-data-sqlite" "Rinna SQLite Persistence"
    
    # Check dependency convergence
    check_convergence
    
    # Verify installation
    verify_installation
    
    log_success "Maven repository caching fix completed"
    log_info "You can now build your project with: mvn clean install"
}

# Execute main
main