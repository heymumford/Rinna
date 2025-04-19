#!/usr/bin/env bash
#
# install-linters.sh - Install all linters and plugins for Rinna
#
# PURPOSE: Install all linters and plugins for Java, Go, and Python
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# 
# This source code is licensed under the MIT License
# found in the LICENSE file in the root directory of this source tree.
#

set -eo pipefail

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

# Status emojis
SUCCESS="✅"
FAILURE="❌"
PENDING="🔄"
SKIPPED="⏭️" 
WARNING="⚠️"

echo -e "${BLUE}${BOLD}== Installing Linters and Plugins ==${NC}\n"

# Function to check if a command exists
command_exists() {
  command -v "$1" &> /dev/null
}

# Function to install a package with pip
install_pip_package() {
  local package="$1"
  local version="${2:-latest}"
  
  echo -e "${PENDING} Installing ${package}..."
  
  if [ "$version" = "latest" ]; then
    pip install "$package"
  else
    pip install "${package}==${version}"
  fi
  
  echo -e "${SUCCESS} Installed ${package}"
}

# Install Go linters
install_go_linters() {
  echo -e "\n${BLUE}${BOLD}Installing Go Linters${NC}\n"
  
  # Install golangci-lint
  if ! command_exists golangci-lint; then
    echo -e "${PENDING} Installing golangci-lint..."
    
    # Use the official installation script
    curl -sSfL https://raw.githubusercontent.com/golangci/golangci-lint/master/install.sh | sh -s -- -b "$(go env GOPATH)/bin"
    
    echo -e "${SUCCESS} Installed golangci-lint"
  else
    echo -e "${SKIPPED} golangci-lint is already installed"
  fi
}

# Install Python linters
install_python_linters() {
  echo -e "\n${BLUE}${BOLD}Installing Python Linters${NC}\n"
  
  # Install mypy
  if ! command_exists mypy; then
    install_pip_package "mypy"
  else
    echo -e "${SKIPPED} mypy is already installed"
  fi
  
  # Install ruff
  if ! command_exists ruff; then
    install_pip_package "ruff"
  else
    echo -e "${SKIPPED} ruff is already installed"
  fi
  
  # Install black
  if ! command_exists black; then
    install_pip_package "black"
  else
    echo -e "${SKIPPED} black is already installed"
  fi
  
  # Install isort
  if ! command_exists isort; then
    install_pip_package "isort"
  else
    echo -e "${SKIPPED} isort is already installed"
  fi
}

# Install Java linters
install_java_linters() {
  echo -e "\n${BLUE}${BOLD}Installing Java Linters${NC}\n"
  
  # Java linters are installed via Maven, so we just need to make sure Maven is installed
  if ! command_exists mvn; then
    echo -e "${FAILURE} Maven is not installed. Please install Maven to use Java linters."
    return 1
  else
    echo -e "${SUCCESS} Maven is installed, Java linters will be installed via Maven"
  fi
}

# Main function
main() {
  # Install linters for each language
  install_java_linters
  install_go_linters
  install_python_linters
  
  echo -e "\n${GREEN}${BOLD}All linters and plugins installed successfully!${NC}\n"
}

# Run the main function
main