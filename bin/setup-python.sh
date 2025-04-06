#!/usr/bin/env bash
#
# setup-python.sh - Set up Python environment for Rinna
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
#

set -e

# Constants
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
VENV_DIR="$PROJECT_ROOT/.venv"
REQUIREMENTS_FILE="$PROJECT_ROOT/requirements.txt"
MIN_PYTHON_VERSION="3.8"
PYTHON_LIB_DIRS=()  # Will be populated during execution

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Formatting
BOLD='\033[1m'
NORMAL='\033[0m'

# Display functions
print_header() { echo -e "${BOLD}${BLUE}$1${NC}${NORMAL}"; }
print_subheader() { echo -e "${CYAN}$1${NC}"; }
print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_error() { echo -e "${RED}✗ Error: $1${NC}" >&2; }
print_fatal() { echo -e "${RED}✗ Fatal Error: $1${NC}" >&2; exit 1; }

# Parse command line options
FORCE=0
VERBOSE=0
INSTALL_OPTIONAL=0
SKIP_CONFIRMATION=0
SKIP_SYSTEM_PACKAGES=0
SKIP_VERSION_CHECK=0
NO_ENV_CHECK=0

# Display help message
show_help() {
  cat << EOF
${BOLD}${BLUE}setup-python.sh${NC}${NORMAL} - Set up Python environment for Rinna

Usage: ./setup-python.sh [options]

Options:
  -f, --force             Force recreation of virtual environment
  -v, --verbose           Show detailed output
  -y, --yes               Skip confirmations
  -a, --all               Install optional dependencies
  --skip-system           Skip system package installation
  --skip-version-check    Skip Python version check
  --no-env-check          Skip virtual environment check
  -h, --help              Show this help message

Example:
  ./setup-python.sh --force --all    # Force recreation and install all dependencies

EOF
}

# Parse command line arguments
parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -f|--force)
        FORCE=1
        shift
        ;;
      -v|--verbose)
        VERBOSE=1
        shift
        ;;
      -a|--all)
        INSTALL_OPTIONAL=1
        shift
        ;;
      -y|--yes)
        SKIP_CONFIRMATION=1
        shift
        ;;
      --skip-system)
        SKIP_SYSTEM_PACKAGES=1
        shift
        ;;
      --skip-version-check)
        SKIP_VERSION_CHECK=1
        shift
        ;;
      --no-env-check)
        NO_ENV_CHECK=1
        shift
        ;;
      -h|--help)
        show_help
        exit 0
        ;;
      *)
        print_error "Unknown option: $1"
        show_help
        exit 1
        ;;
    esac
  done
}

# Logger function
log() {
  local level="$1"
  local message="$2"
  
  case "$level" in
    INFO)
      if [[ "$VERBOSE" -eq 1 ]]; then
        echo -e "[INFO] $message"
      fi
      ;;
    WARN)
      echo -e "${YELLOW}[WARN] $message${NC}"
      ;;
    ERROR)
      echo -e "${RED}[ERROR] $message${NC}" >&2
      ;;
    DEBUG)
      if [[ "$VERBOSE" -eq 1 ]]; then
        echo -e "${CYAN}[DEBUG] $message${NC}"
      fi
      ;;
    *)
      echo -e "$message"
      ;;
  esac
}

# Check if Python 3 is installed and meets minimum version
check_python() {
  print_header "Checking Python installation"

  # Find the Python executable
  local python_cmd=""
  if command -v python3 &> /dev/null; then
    python_cmd="python3"
  elif command -v python &> /dev/null; then
    if python --version 2>&1 | grep -q "Python 3"; then
      python_cmd="python"
    fi
  fi

  if [[ -z "$python_cmd" ]]; then
    print_error "Python 3 is not installed"
    print_subheader "Please install Python $MIN_PYTHON_VERSION or higher"
    
    # Suggest installation method based on OS
    if [[ "$(uname)" == "Linux" ]]; then
      # Check the distribution
      if command -v apt &> /dev/null; then
        echo "  sudo apt update && sudo apt install python3 python3-venv python3-pip"
      elif command -v dnf &> /dev/null; then
        echo "  sudo dnf install python3 python3-devel"
      elif command -v yum &> /dev/null; then
        echo "  sudo yum install python3 python3-devel"
      fi
    elif [[ "$(uname)" == "Darwin" ]]; then
      echo "  brew install python"
    fi
    
    exit 1
  fi

  # Check Python version
  if [[ "$SKIP_VERSION_CHECK" -eq 0 ]]; then
    local py_version=$($python_cmd --version 2>&1 | sed 's/Python //')
    local py_major=$(echo "$py_version" | cut -d. -f1)
    local py_minor=$(echo "$py_version" | cut -d. -f2)
    
    local min_major=$(echo "$MIN_PYTHON_VERSION" | cut -d. -f1)
    local min_minor=$(echo "$MIN_PYTHON_VERSION" | cut -d. -f2)
    
    if [[ "$py_major" -lt "$min_major" || ("$py_major" -eq "$min_major" && "$py_minor" -lt "$min_minor") ]]; then
      print_error "Python $MIN_PYTHON_VERSION or higher is required (found $py_version)"
      exit 1
    fi
    
    print_success "Found Python $py_version ($($python_cmd -c 'import sys; print(sys.executable)'))"
    PYTHON="$python_cmd"
  else
    print_warning "Skipping Python version check"
    PYTHON="$python_cmd"
  fi

  # Check for venv module
  if ! $PYTHON -c "import venv" &> /dev/null; then
    print_error "Python venv module is not available"
    
    if [[ "$SKIP_SYSTEM_PACKAGES" -eq 0 ]]; then
      if command -v apt &> /dev/null; then
        print_subheader "Installing python3-venv..."
        sudo apt update && sudo apt install -y python3-venv
      elif command -v dnf &> /dev/null; then
        print_subheader "Installing python3-venv..."
        sudo dnf install -y python3-devel
      elif [[ "$(uname)" == "Darwin" ]]; then
        print_warning "On macOS, venv should be included with Python"
        print_subheader "If you're having issues, try reinstalling Python with Homebrew"
        echo "  brew reinstall python"
      else
        print_warning "Please install the Python venv module manually"
        exit 1
      fi
      
      # Verify installation
      if ! $PYTHON -c "import venv" &> /dev/null; then
        print_error "Failed to install Python venv module"
        exit 1
      fi
      print_success "Successfully installed Python venv module"
    else
      print_error "Python venv module is required but system package installation is disabled"
      exit 1
    fi
  else
    print_success "Python venv module is available"
  fi
  
  # Find Python library directories (for optional system dependencies)
  PYTHON_LIB_DIRS+=("$($PYTHON -c 'import site; print(site.getsitepackages()[0])')")
  PYTHON_LIB_DIRS+=("$($PYTHON -c 'import site; print(site.getusersitepackages())')")
  
  log "DEBUG" "Python library directories: ${PYTHON_LIB_DIRS[*]}"
}

# Check if running within virtual environment
check_in_venv() {
  if [[ "$NO_ENV_CHECK" -eq 1 ]]; then
    log "INFO" "Skipping virtual environment check"
    return 0
  fi
  
  if [[ -n "$VIRTUAL_ENV" ]]; then
    print_warning "Already running in a virtual environment: $VIRTUAL_ENV"
    
    if [[ "$SKIP_CONFIRMATION" -eq 0 ]]; then
      read -p "Continue anyway? [y/N] " -n 1 -r
      echo
      if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_warning "Aborting..."
        exit 0
      fi
    else
      log "INFO" "Continuing anyway (--yes flag provided)"
    fi
  fi
}

# Create or recreate the virtual environment
create_venv() {
  # Check if venv exists and force flag is set
  if [[ -d "$VENV_DIR" ]]; then
    if [[ "$FORCE" -eq 1 ]]; then
      print_header "Removing existing virtual environment"
      rm -rf "$VENV_DIR"
      print_success "Removed existing virtual environment"
    else
      print_success "Virtual environment already exists at $VENV_DIR"
      return 0
    fi
  fi

  print_header "Creating virtual environment at $VENV_DIR"
  if [[ "$VERBOSE" -eq 1 ]]; then
    $PYTHON -m venv "$VENV_DIR"
  else
    $PYTHON -m venv "$VENV_DIR" &> /dev/null
  fi
  
  if [[ ! -f "$VENV_DIR/bin/activate" ]]; then
    print_error "Failed to create virtual environment"
    exit 1
  fi
  
  print_success "Virtual environment created successfully"
}

# Activate the virtual environment
activate_venv() {
  if [[ -f "$VENV_DIR/bin/activate" ]]; then
    log "INFO" "Activating virtual environment"
    # We can't directly source in this script, so we'll use the pip from the venv directly
    PIP="$VENV_DIR/bin/pip"
    log "DEBUG" "Using pip: $PIP"
  else
    print_error "Virtual environment not found at $VENV_DIR"
    exit 1
  fi
}

# Update pip to latest version
update_pip() {
  print_header "Updating pip to latest version"
  
  if [[ "$VERBOSE" -eq 1 ]]; then
    "$PIP" install --upgrade pip
  else
    "$PIP" install --upgrade pip &> /dev/null
  fi
  
  # Get the new pip version
  local pip_version=$("$PIP" --version | cut -d' ' -f2)
  print_success "Pip updated to version $pip_version"
}

# Parse requirements.txt to separate regular and optional dependencies
parse_requirements() {
  if [[ ! -f "$REQUIREMENTS_FILE" ]]; then
    print_error "Requirements file not found at $REQUIREMENTS_FILE"
    exit 1
  fi
  
  log "INFO" "Parsing requirements file: $REQUIREMENTS_FILE"
  
  # Create temporary file for filtered requirements
  TEMP_REQ=$(mktemp)
  
  # Extract non-optional requirements
  grep -v "(optional)" "$REQUIREMENTS_FILE" | grep -v "^#" | grep -v "^$" > "$TEMP_REQ"
  
  log "DEBUG" "Created filtered requirements file at $TEMP_REQ"
  
  if [[ "$INSTALL_OPTIONAL" -eq 1 ]]; then
    # If installing optional deps, use the original file
    ACTIVE_REQ="$REQUIREMENTS_FILE"
    log "INFO" "Will install ALL dependencies including optional ones"
  else
    # Otherwise use the filtered file
    ACTIVE_REQ="$TEMP_REQ"
    log "INFO" "Will install only required dependencies (use --all for optional deps)"
  fi
}

# Check for system dependencies that might be needed
check_system_dependencies() {
  if [[ "$SKIP_SYSTEM_PACKAGES" -eq 1 ]]; then
    log "INFO" "Skipping system dependency check"
    return 0
  fi
  
  print_header "Checking for system dependencies"
  
  local system_deps=()
  local os_type="$(uname)"
  
  # Check for graphviz (needed for diagrams library)
  if grep -q "diagrams" "$ACTIVE_REQ"; then
    if ! command -v dot &> /dev/null; then
      system_deps+=("graphviz")
    else
      log "INFO" "Graphviz is already installed: $(dot -V 2>&1 | head -n1)"
    fi
  fi
  
  # Check for other potential dependencies here
  # ...
  
  # Install system dependencies if needed
  if [[ ${#system_deps[@]} -gt 0 ]]; then
    print_subheader "The following system packages may be required: ${system_deps[*]}"
    
    if [[ "$SKIP_CONFIRMATION" -eq 0 ]]; then
      read -p "Do you want to install these system packages? [Y/n] " -n 1 -r
      echo
      if [[ ! $REPLY =~ ^[Nn]$ ]]; then
        install_system_packages "${system_deps[@]}"
      else
        print_warning "Skipping system package installation. Some Python packages may fail to install."
      fi
    else
      install_system_packages "${system_deps[@]}"
    fi
  else
    print_success "All required system dependencies are already installed"
  fi
}

# Install system packages based on the OS
install_system_packages() {
  local packages=("$@")
  local os_type="$(uname)"
  
  if [[ "$os_type" == "Linux" ]]; then
    # Detect package manager
    if command -v apt &> /dev/null; then
      print_subheader "Installing packages with apt: ${packages[*]}"
      sudo apt update && sudo apt install -y "${packages[@]}"
    elif command -v dnf &> /dev/null; then
      print_subheader "Installing packages with dnf: ${packages[*]}"
      sudo dnf install -y "${packages[@]}"
    elif command -v yum &> /dev/null; then
      print_subheader "Installing packages with yum: ${packages[*]}"
      sudo yum install -y "${packages[@]}"
    else
      print_warning "Unsupported package manager. Please install these packages manually: ${packages[*]}"
      return 1
    fi
  elif [[ "$os_type" == "Darwin" ]]; then
    # macOS with Homebrew
    if command -v brew &> /dev/null; then
      print_subheader "Installing packages with brew: ${packages[*]}"
      brew install "${packages[@]}"
    else
      print_warning "Homebrew not found. Please install these packages manually: ${packages[*]}"
      return 1
    fi
  else
    print_warning "Unsupported operating system: $os_type"
    print_warning "Please install these packages manually: ${packages[*]}"
    return 1
  fi
  
  print_success "System packages installed successfully"
}

# Install dependencies from requirements.txt
install_dependencies() {
  print_header "Installing Python dependencies"
  
  # Install wheel first to help with binary packages
  print_subheader "Installing wheel package"
  if [[ "$VERBOSE" -eq 1 ]]; then
    "$PIP" install wheel
  else
    "$PIP" install wheel &> /dev/null
  fi
  
  # Install dependencies from requirements file
  print_subheader "Installing from: $(basename "$ACTIVE_REQ")"
  
  if [[ "$VERBOSE" -eq 1 ]]; then
    "$PIP" install -r "$ACTIVE_REQ" --upgrade
  else
    # Show a progress message since this might take a while
    echo -n "Installing packages..."
    "$PIP" install -r "$ACTIVE_REQ" --upgrade &> /dev/null || {
      echo " failed!"
      print_error "Failed to install dependencies. Run with --verbose to see the errors."
      exit 1
    }
    echo " done!"
  fi
  
  print_success "Python dependencies installed successfully"
  
  # Show installed packages if verbose
  if [[ "$VERBOSE" -eq 1 ]]; then
    print_subheader "Installed packages:"
    "$PIP" list
  fi
  
  # Cleanup temporary file
  rm -f "$TEMP_REQ"
}

# Add version file to virtual environment
update_venv_version() {
  print_header "Setting virtual environment version"
  
  # Get project version from version.properties
  VERSION_PROPS="$PROJECT_ROOT/version.properties"
  if [[ -f "$VERSION_PROPS" ]]; then
    VERSION=$(grep -m 1 "^version=" "$VERSION_PROPS" | cut -d'=' -f2)
    if [[ -n "$VERSION" ]]; then
      # Create version file in virtual environment
      echo "$VERSION" > "$VENV_DIR/version"
      print_success "Set virtual environment version to $VERSION"
      
      # Also create a Python module with version info
      PY_SITEPACKAGES=$($VENV_DIR/bin/python -c "import site; print(site.getsitepackages()[0])")
      VENV_INFO_DIR="$PY_SITEPACKAGES/rinna_venv_info"
      mkdir -p "$VENV_INFO_DIR"
      
      # Create __init__.py
      cat > "$VENV_INFO_DIR/__init__.py" << EOF
"""Rinna virtual environment information."""

VERSION = "$VERSION"
CREATION_DATE = "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
PYTHON_VERSION = "$(${PYTHON} --version 2>&1 | cut -d' ' -f2)"

def get_version():
    """Return the Rinna project version."""
    return VERSION

def get_info():
    """Return dictionary with virtual environment information."""
    return {
        "version": VERSION,
        "creation_date": CREATION_DATE,
        "python_version": PYTHON_VERSION,
    }

def print_info():
    """Print virtual environment information."""
    print(f"Rinna Virtual Environment")
    print(f"Version: {VERSION}")
    print(f"Created: {CREATION_DATE}")
    print(f"Python:  {PYTHON_VERSION}")
EOF

      # Create __main__.py for direct module execution
      cat > "$VENV_INFO_DIR/__main__.py" << EOF
"""Main entry point for rinna_venv_info package."""

from rinna_venv_info import print_info

# Print environment info when module is executed directly
print_info()
EOF
      
      print_success "Created Python version module"
    else
      print_warning "Could not extract version from $VERSION_PROPS"
      echo "1.0.0" > "$VENV_DIR/version"
    fi
  else
    print_warning "Version file not found at $VERSION_PROPS"
    echo "1.0.0" > "$VENV_DIR/version"
  fi
}

# Create activation script for virtual environment
create_activation_script() {
  ACTIVATE_SCRIPT="$PROJECT_ROOT/activate-python.sh"
  
  print_header "Creating activation script"
  
  cat > "$ACTIVATE_SCRIPT" << EOL
#!/usr/bin/env bash
#
# activate-python.sh - Activate Python virtual environment for Rinna
#

# Activate the virtual environment
if [[ -f "$VENV_DIR/bin/activate" ]]; then
    source "$VENV_DIR/bin/activate"
    
    # Set project environment variables
    export RINNA_ROOT="$PROJECT_ROOT"
    export RINNA_VERSION=\$(cat "$VENV_DIR/version" 2>/dev/null || echo "unknown")
    
    # Show version info
    if python -c "import rinna_venv_info" &>/dev/null; then
        python -m rinna_venv_info
    else
        echo "Rinna Python environment activated (version \$RINNA_VERSION)"
    fi
    
    echo "Run 'deactivate' to exit the virtual environment"
else
    echo "Error: Virtual environment not found at $VENV_DIR"
    echo "Run 'bin/setup-python.sh' to create it"
    return 1
fi
EOL
  
  chmod +x "$ACTIVATE_SCRIPT"
  print_success "Created activation script at $ACTIVATE_SCRIPT"
}

# Verify the environment is working correctly
verify_environment() {
  print_header "Verifying Python environment"
  
  # Check if important packages are actually importable
  print_subheader "Testing imports..."
  
  # List of key packages to verify
  local packages=("yaml" "pytest")
  if grep -q "diagrams" "$REQUIREMENTS_FILE"; then
    packages+=("diagrams")
  fi
  
  local failed=0
  for pkg in "${packages[@]}"; do
    echo -n "  Checking $pkg... "
    if "$VENV_DIR/bin/python" -c "import $pkg" &> /dev/null; then
      echo -e "${GREEN}OK${NC}"
    else
      echo -e "${RED}FAILED${NC}"
      failed=1
    fi
  done
  
  if [[ "$failed" -eq 1 ]]; then
    print_warning "Some packages failed to import correctly"
    print_subheader "This might be due to missing system dependencies"
    print_subheader "Try running with: ${YELLOW}./setup-python.sh --force --verbose${NC}"
    return 1
  else
    print_success "All imports verified successfully"
  fi
  
  return 0
}

# Create a .env file for VS Code and other tools
create_env_file() {
  print_header "Creating .env file for development tools"
  
  local env_file="$PROJECT_ROOT/.env"
  
  cat > "$env_file" << EOL
# Python environment variables for Rinna
# Generated on $(date)
PYTHONPATH=${PROJECT_ROOT}:${PROJECT_ROOT}/src
VIRTUAL_ENV=${VENV_DIR}
EOL
  
  # For VS Code, also create settings if they don't exist
  local vscode_dir="$PROJECT_ROOT/.vscode"
  local settings_file="$vscode_dir/settings.json"
  
  if [[ ! -d "$vscode_dir" ]]; then
    mkdir -p "$vscode_dir"
  fi
  
  if [[ ! -f "$settings_file" ]]; then
    cat > "$settings_file" << EOL
{
    "python.defaultInterpreterPath": "${VENV_DIR}/bin/python",
    "python.analysis.extraPaths": [
        "${PROJECT_ROOT}",
        "${PROJECT_ROOT}/src"
    ],
    "python.testing.pytestEnabled": true,
    "python.linting.enabled": true,
    "python.linting.pylintEnabled": true,
    "python.formatting.provider": "black"
}
EOL
    print_success "Created VS Code settings at $settings_file"
  else
    print_warning "VS Code settings already exist at $settings_file"
    print_subheader "You may need to update them manually to use the virtual environment"
  fi
  
  print_success "Created .env file at $env_file"
}

# Clean up and display final message
display_final_message() {
  print_header "Python environment setup complete!"
  
  echo ""
  echo -e "To activate the virtual environment, run: ${GREEN}source ./activate-python.sh${NC}"
  echo -e "To deactivate, run: ${GREEN}deactivate${NC}"
  echo ""
  
  if [[ "$VERBOSE" -eq 1 ]]; then
    echo "Environment details:"
    echo "  - Python: $($VENV_DIR/bin/python --version 2>&1)"
    echo "  - Pip: $($VENV_DIR/bin/pip --version | cut -d' ' -f1-2)"
    echo "  - Location: $VENV_DIR"
    
    local version="$(cat $VENV_DIR/version 2>/dev/null || echo "unknown")"
    echo "  - Project version: $version"
    echo ""
  fi
}

# Main execution function
main() {
  parse_args "$@"
  
  print_header "Setting up Python environment for Rinna"
  log "INFO" "Started at $(date)"
  
  # Environment checks
  check_in_venv
  check_python
  
  # Create and activate virtual environment
  create_venv
  activate_venv
  update_pip
  
  # Parse requirements and install dependencies
  parse_requirements
  check_system_dependencies
  install_dependencies
  
  # Finalize setup
  update_venv_version
  create_activation_script
  create_env_file
  verify_environment
  
  # Show final message
  display_final_message
  
  log "INFO" "Completed at $(date)"
}

# Run the main function with all arguments
main "$@"