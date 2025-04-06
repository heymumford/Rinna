# Rinna Common Utilities

This directory contains shared utility scripts that provide common functionality across all Rinna command-line tools.

## Available Utilities

### rinna_utils.sh

Core utility functions for all Rinna scripts. Provides:

- Terminal color and formatting
- Logging framework with log rotation
- OS and architecture detection
- Environment detection (CI, etc.)
- Version management
- Command and dependency checking
- Backup and restoration
- File and directory utilities
- Cross-platform compatibility helpers

Usage:
```bash
source "$(dirname "$0")/common/rinna_utils.sh"

# Then use utility functions:
print_header "My Script"
log "info" "Processing..."
```

### rinna_logger.sh

Cross-language logging framework that provides consistent logging across bash, Java, Go, and Python components. Ensures:

- Consistent log levels (DEBUG, INFO, WARNING, ERROR, FATAL)
- Standardized log formatting
- Automatic log rotation
- Configurable output destinations
- Color-coded console output

Usage:
```bash
source "$(dirname "$0")/common/rinna_logger.sh"

# Log at different levels:
log_debug "Detailed debug information"
log_info "Standard information message"
log_warning "Warning condition"
log_error "Error condition"
log_fatal "Fatal error that will terminate execution"
```

## Design Principles

1. **DRY (Don't Repeat Yourself)**: Consolidated common functionality to reduce duplication across scripts
2. **Consistency**: Provide consistent interfaces, error handling, and user experience
3. **Robustness**: Handle edge cases and provide sensible defaults
4. **Discoverability**: Well-documented functions with clear names
5. **Compatibility**: Support multiple platforms and environments

## Adding New Utilities

When adding new common utilities:

1. Follow the naming convention: `rinna_<function>.sh`
2. Add comprehensive documentation at the top of the file
3. Ensure the script can be sourced without side effects
4. Use parameter validation for public functions
5. Follow the established logging and error handling patterns
6. Update this README.md with details about the new utility

## Usage Guidelines

- Always `source` these files, don't execute them directly
- Check for required dependencies at the start of your script
- Use the provided logging functions for consistent output
- Handle errors appropriately using the provided error handling
- Set appropriate defaults and provide overrides via environment variables or command line parameters