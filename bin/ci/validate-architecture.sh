#!/bin/bash
# validate-architecture.sh - Architecture validation script for Rinna CI
#
# This script validates the codebase's architectural structure and enforces
# architectural standards based on Clean Architecture principles.
#
# Usage:
#   ./bin/ci/validate-architecture.sh [OPTIONS]
#
# Options:
#   --modules LIST     Comma-separated list of modules to validate (default: all)
#   --rules FILE       Path to architecture rules file (default: config/architecture/rules.yml)
#   --ci               Running in CI environment (adjusts output for GitHub Actions)
#   --report-dir DIR   Directory to store validation reports (default: target/architecture-validation)
#   --verbose          Show verbose output
#   --help             Show this help message

set -eo pipefail

# Default values
MODULES="all"
RULES_FILE="config/architecture/rules.yml"
CI_MODE=false
REPORT_DIR="target/architecture-validation"
VERBOSE=false

# ANSI color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
CLEAR='\033[0m'

# Load Rinna logger if available
if [ -f "bin/common/rinna_logger.sh" ]; then
    source bin/common/rinna_logger.sh
else
    # Simple logger functions if rinna_logger.sh is not available
    function log_info() { echo -e "${BLUE}[INFO]${CLEAR} $*"; }
    function log_success() { echo -e "${GREEN}[SUCCESS]${CLEAR} $*"; }
    function log_warning() { echo -e "${YELLOW}[WARNING]${CLEAR} $*"; }
    function log_error() { echo -e "${RED}[ERROR]${CLEAR} $*"; }
    function log_debug() { if $VERBOSE; then echo -e "${CYAN}[DEBUG]${CLEAR} $*"; fi; }
fi

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --modules)
            MODULES="$2"
            shift 2
            ;;
        --rules)
            RULES_FILE="$2"
            shift 2
            ;;
        --ci)
            CI_MODE=true
            shift
            ;;
        --report-dir)
            REPORT_DIR="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            echo "Architecture Validation for Rinna"
            echo ""
            echo "Usage: ./bin/ci/validate-architecture.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --modules LIST     Comma-separated list of modules to validate (default: all)"
            echo "  --rules FILE       Path to architecture rules file (default: config/architecture/rules.yml)"
            echo "  --ci               Running in CI environment (adjusts output for GitHub Actions)"
            echo "  --report-dir DIR   Directory to store validation reports (default: target/architecture-validation)"
            echo "  --verbose          Show verbose output"
            echo "  --help             Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Create report directory if it doesn't exist
mkdir -p "$REPORT_DIR"

# Default architecture rules
DEFAULT_RULES='{
  "clean_architecture": {
    "layers": ["domain", "usecase", "adapter", "infrastructure", "presentation"],
    "allowed_dependencies": {
      "domain": [],
      "usecase": ["domain"],
      "adapter": ["domain", "usecase"],
      "infrastructure": ["domain", "usecase", "adapter"],
      "presentation": ["domain", "usecase", "adapter"]
    }
  },
  "package_structure": {
    "rinna-core": {
      "domain_model_path": "src/main/java/org/rinna/domain/model",
      "required_packages": [
        "org.rinna.domain.model",
        "org.rinna.domain.repository",
        "org.rinna.domain.service",
        "org.rinna.usecase",
        "org.rinna.adapter.repository",
        "org.rinna.adapter.service"
      ]
    },
    "rinna-cli": {
      "required_packages": [
        "org.rinna.cli.command",
        "org.rinna.cli.service",
        "org.rinna.cli.util"
      ]
    },
    "rinna-data-sqlite": {
      "required_packages": [
        "org.rinna.data.repository"
      ]
    }
  },
  "naming_conventions": {
    "service_implementations": "^Default[A-Z][a-zA-Z0-9]*Service$",
    "repositories": "^[A-Z][a-zA-Z0-9]*Repository$",
    "repository_implementations": "^(InMemory|Sqlite)[A-Z][a-zA-Z0-9]*Repository$",
    "domain_models": "^[A-Z][a-zA-Z0-9]*$",
    "usecases": "^[A-Z][a-zA-Z0-9]*Service$"
  }
}'

log_info "Starting architecture validation..."

# Load or initialize rules
if [ -f "$RULES_FILE" ]; then
    log_debug "Using existing rules file: $RULES_FILE"
else
    log_warning "Rules file not found at $RULES_FILE, creating default rules..."
    mkdir -p "$(dirname "$RULES_FILE")"
    echo "$DEFAULT_RULES" > "$RULES_FILE"
fi

# Get all modules to validate
if [ "$MODULES" = "all" ]; then
    MODULES_TO_CHECK=("rinna-core" "rinna-cli" "rinna-data-sqlite")
else
    IFS=',' read -ra MODULES_TO_CHECK <<< "$MODULES"
fi

log_info "Modules to validate: ${MODULES_TO_CHECK[*]}"

# Function to check clean architecture dependencies
check_clean_architecture() {
    local module="$1"
    local report="$2"
    local violations=0
    
    log_info "Checking Clean Architecture dependencies for $module..."
    
    # Add to report
    {
        echo "## Clean Architecture Validation for $module"
        echo ""
        echo "### Layer Dependencies"
        echo ""
    } >> "$report"
    
    # Check each Java file for imports that violate clean architecture
    local java_files
    if [ -d "$module/src/main/java" ]; then
        java_files=$(find "$module/src/main/java" -name "*.java")
    else
        # Handle case where the module path might be different
        java_files=$(find "src/main/java" -name "*.java" 2>/dev/null || echo "")
    fi
    
    if [ -z "$java_files" ]; then
        log_warning "No Java files found in $module"
        return 0
    fi
    
    # Extract allowed dependencies from rules
    local domain_allowed=()
    local usecase_allowed=()
    local adapter_allowed=()
    local infra_allowed=()
    local presentation_allowed=()
    
    # Use grep and sed to extract configuration
    if [ -f "$RULES_FILE" ]; then
        # This is simplified for shell script - a real implementation would use a
        # proper JSON/YAML parser depending on the rules file format
        domain_allowed=()
        usecase_allowed=($(grep -A 10 '"usecase":' "$RULES_FILE" | grep -o '"domain"' | tr -d '"' || echo ""))
        adapter_allowed=($(grep -A 10 '"adapter":' "$RULES_FILE" | grep -o '"domain\|usecase"' | tr -d '"' || echo ""))
        infra_allowed=($(grep -A 10 '"infrastructure":' "$RULES_FILE" | grep -o '"domain\|usecase\|adapter"' | tr -d '"' || echo ""))
        presentation_allowed=($(grep -A 10 '"presentation":' "$RULES_FILE" | grep -o '"domain\|usecase\|adapter"' | tr -d '"' || echo ""))
    fi
    
    # Check each Java file
    for file in $java_files; do
        local package=$(grep -o "package [a-zA-Z0-9_.]*;" "$file" | cut -d' ' -f2 | tr -d ';')
        local layer=""
        
        # Determine which layer this file belongs to
        if [[ "$package" == *".domain."* ]]; then
            layer="domain"
        elif [[ "$package" == *".usecase."* ]]; then
            layer="usecase"
        elif [[ "$package" == *".adapter."* ]]; then
            layer="adapter"
        elif [[ "$package" == *".infrastructure."* ]] || [[ "$package" == *".config."* ]]; then
            layer="infrastructure"
        elif [[ "$package" == *".cli."* ]] || [[ "$package" == *".ui."* ]] || [[ "$package" == *".pui."* ]]; then
            layer="presentation"
        else
            log_debug "Skipping file with unknown layer: $file"
            continue
        fi
        
        log_debug "Checking $file in layer $layer"
        
        # Get imports for this file
        local imports=$(grep -o "import [a-zA-Z0-9_.]*;" "$file" | cut -d' ' -f2 | tr -d ';')
        
        # Check for clean architecture violations based on layer
        for import in $imports; do
            local import_layer=""
            
            # Skip standard Java imports and third-party imports
            if [[ "$import" != "org.rinna."* ]]; then
                continue
            fi
            
            # Determine which layer the import belongs to
            if [[ "$import" == *".domain."* ]]; then
                import_layer="domain"
            elif [[ "$import" == *".usecase."* ]]; then
                import_layer="usecase"
            elif [[ "$import" == *".adapter."* ]]; then
                import_layer="adapter"
            elif [[ "$import" == *".infrastructure."* ]] || [[ "$import" == *".config."* ]]; then
                import_layer="infrastructure"
            elif [[ "$import" == *".cli."* ]] || [[ "$import" == *".ui."* ]] || [[ "$import" == *".pui."* ]]; then
                import_layer="presentation"
            else
                log_debug "Skipping import with unknown layer: $import"
                continue
            fi
            
            # Check if this import is allowed for the current layer
            local is_allowed=false
            
            case "$layer" in
                "domain")
                    if [[ " ${domain_allowed[@]} " =~ " ${import_layer} " ]]; then
                        is_allowed=true
                    fi
                    ;;
                "usecase")
                    if [[ " ${usecase_allowed[@]} " =~ " ${import_layer} " ]]; then
                        is_allowed=true
                    fi
                    ;;
                "adapter")
                    if [[ " ${adapter_allowed[@]} " =~ " ${import_layer} " ]]; then
                        is_allowed=true
                    fi
                    ;;
                "infrastructure")
                    if [[ " ${infra_allowed[@]} " =~ " ${import_layer} " ]]; then
                        is_allowed=true
                    fi
                    ;;
                "presentation")
                    if [[ " ${presentation_allowed[@]} " =~ " ${import_layer} " ]]; then
                        is_allowed=true
                    fi
                    ;;
            esac
            
            # If current layer is the same as import layer, it's allowed
            if [ "$layer" = "$import_layer" ]; then
                is_allowed=true
            fi
            
            if ! $is_allowed; then
                violations=$((violations + 1))
                local violation_message="$file: Layer '$layer' should not depend on '$import_layer' ($import)"
                log_error "$violation_message"
                echo "- ❌ $violation_message" >> "$report"
                
                if $CI_MODE; then
                    echo "::error file=$file::Clean Architecture violation: Layer '$layer' should not depend on '$import_layer'"
                fi
            fi
        done
    done
    
    # Add summary to report
    {
        echo ""
        echo "### Summary"
        echo ""
        if [ "$violations" -eq 0 ]; then
            echo "✅ **No Clean Architecture violations found**"
        else
            echo "❌ **$violations Clean Architecture violations found**"
        fi
        echo ""
    } >> "$report"
    
    return $violations
}

# Function to check package structure
check_package_structure() {
    local module="$1"
    local report="$2"
    local violations=0
    
    log_info "Checking package structure for $module..."
    
    # Add to report
    {
        echo "## Package Structure Validation for $module"
        echo ""
    } >> "$report"
    
    # Get required packages for this module from rules
    local required_packages=()
    if [ -f "$RULES_FILE" ]; then
        # Extract required packages for this module - simplified for shell script
        required_packages=($(grep -A 20 "\"$module\":" "$RULES_FILE" | grep -A 10 "required_packages" | grep -o '"org[^"]*"' | tr -d '"' || echo ""))
    fi
    
    if [ ${#required_packages[@]} -eq 0 ]; then
        log_warning "No required packages defined for $module in rules file"
        return 0
    fi
    
    {
        echo "### Required Packages"
        echo ""
        echo "The following packages are required for $module:"
        echo ""
        for pkg in "${required_packages[@]}"; do
            echo "- \`$pkg\`"
        done
        echo ""
        echo "### Validation Results"
        echo ""
    } >> "$report"
    
    # Check for each required package
    for package in "${required_packages[@]}"; do
        local package_path=$(echo "$package" | tr '.' '/')
        local found=false
        
        # Check if the package exists in the module
        if [ -d "$module/src/main/java/$package_path" ]; then
            found=true
        elif [ -d "src/main/java/$package_path" ]; then
            # For root module or alternative structure
            found=true
        fi
        
        if $found; then
            log_debug "✓ Required package $package exists"
            echo "- ✅ Required package \`$package\` exists" >> "$report"
        else
            violations=$((violations + 1))
            log_error "✗ Required package $package is missing"
            echo "- ❌ Required package \`$package\` is missing" >> "$report"
            
            if $CI_MODE; then
                echo "::error::Package Structure violation: Required package '$package' is missing in module '$module'"
            fi
        fi
    done
    
    # Add summary to report
    {
        echo ""
        echo "### Summary"
        echo ""
        if [ "$violations" -eq 0 ]; then
            echo "✅ **All required packages are present**"
        else
            echo "❌ **$violations required packages are missing**"
        fi
        echo ""
    } >> "$report"
    
    return $violations
}

# Function to check naming conventions
check_naming_conventions() {
    local module="$1"
    local report="$2"
    local violations=0
    
    log_info "Checking naming conventions for $module..."
    
    # Add to report
    {
        echo "## Naming Conventions Validation for $module"
        echo ""
    } >> "$report"
    
    # Get naming conventions from rules
    local service_pattern=""
    local repo_pattern=""
    local repo_impl_pattern=""
    local domain_model_pattern=""
    local usecase_pattern=""
    
    if [ -f "$RULES_FILE" ]; then
        # Extract patterns - simplified for shell script
        service_pattern=$(grep -A 2 '"service_implementations":' "$RULES_FILE" | grep -o '"[^"]*"$' | tr -d '"' || echo "")
        repo_pattern=$(grep -A 2 '"repositories":' "$RULES_FILE" | grep -o '"[^"]*"$' | tr -d '"' || echo "")
        repo_impl_pattern=$(grep -A 2 '"repository_implementations":' "$RULES_FILE" | grep -o '"[^"]*"$' | tr -d '"' || echo "")
        domain_model_pattern=$(grep -A 2 '"domain_models":' "$RULES_FILE" | grep -o '"[^"]*"$' | tr -d '"' || echo "")
        usecase_pattern=$(grep -A 2 '"usecases":' "$RULES_FILE" | grep -o '"[^"]*"$' | tr -d '"' || echo "")
    fi
    
    {
        echo "### Naming Convention Rules"
        echo ""
        echo "- Service Implementations: \`$service_pattern\`"
        echo "- Repositories: \`$repo_pattern\`"
        echo "- Repository Implementations: \`$repo_impl_pattern\`"
        echo "- Domain Models: \`$domain_model_pattern\`"
        echo "- Usecases: \`$usecase_pattern\`"
        echo ""
        echo "### Validation Results"
        echo ""
    } >> "$report"
    
    # Get all Java files
    local java_files
    if [ -d "$module/src/main/java" ]; then
        java_files=$(find "$module/src/main/java" -name "*.java")
    else
        # Handle case where the module path might be different
        java_files=$(find "src/main/java" -name "*.java" 2>/dev/null || echo "")
    fi
    
    if [ -z "$java_files" ]; then
        log_warning "No Java files found in $module"
        return 0
    fi
    
    # Check naming conventions for each file
    for file in $java_files; do
        local class_name=$(basename "$file" .java)
        local package=$(grep -o "package [a-zA-Z0-9_.]*;" "$file" | cut -d' ' -f2 | tr -d ';')
        
        # Check service implementations
        if [[ "$package" == *".adapter.service"* || "$package" == *".service.impl"* ]] && [[ "$class_name" == *"Service" ]]; then
            if [ -n "$service_pattern" ] && ! [[ "$class_name" =~ $service_pattern ]]; then
                violations=$((violations + 1))
                log_error "✗ Service implementation '$class_name' doesn't match pattern"
                echo "- ❌ Service implementation \`$class_name\` doesn't match pattern \`$service_pattern\`" >> "$report"
                
                if $CI_MODE; then
                    echo "::error file=$file::Naming Convention violation: Service implementation '$class_name' doesn't match pattern"
                fi
            fi
        fi
        
        # Check repositories
        if [[ "$package" == *".domain.repository"* ]] && [[ "$class_name" == *"Repository" ]]; then
            if [ -n "$repo_pattern" ] && ! [[ "$class_name" =~ $repo_pattern ]]; then
                violations=$((violations + 1))
                log_error "✗ Repository interface '$class_name' doesn't match pattern"
                echo "- ❌ Repository interface \`$class_name\` doesn't match pattern \`$repo_pattern\`" >> "$report"
                
                if $CI_MODE; then
                    echo "::error file=$file::Naming Convention violation: Repository interface '$class_name' doesn't match pattern"
                fi
            fi
        fi
        
        # Check repository implementations
        if [[ "$package" == *".adapter.repository"* ]] && [[ "$class_name" == *"Repository" ]]; then
            if [ -n "$repo_impl_pattern" ] && ! [[ "$class_name" =~ $repo_impl_pattern ]]; then
                violations=$((violations + 1))
                log_error "✗ Repository implementation '$class_name' doesn't match pattern"
                echo "- ❌ Repository implementation \`$class_name\` doesn't match pattern \`$repo_impl_pattern\`" >> "$report"
                
                if $CI_MODE; then
                    echo "::error file=$file::Naming Convention violation: Repository implementation '$class_name' doesn't match pattern"
                fi
            fi
        fi
        
        # Check domain models
        if [[ "$package" == *".domain.model"* ]]; then
            if [ -n "$domain_model_pattern" ] && ! [[ "$class_name" =~ $domain_model_pattern ]]; then
                violations=$((violations + 1))
                log_error "✗ Domain model '$class_name' doesn't match pattern"
                echo "- ❌ Domain model \`$class_name\` doesn't match pattern \`$domain_model_pattern\`" >> "$report"
                
                if $CI_MODE; then
                    echo "::error file=$file::Naming Convention violation: Domain model '$class_name' doesn't match pattern"
                fi
            fi
        fi
        
        # Check usecases
        if [[ "$package" == *".usecase"* ]] && [[ "$class_name" == *"Service" ]]; then
            if [ -n "$usecase_pattern" ] && ! [[ "$class_name" =~ $usecase_pattern ]]; then
                violations=$((violations + 1))
                log_error "✗ Usecase service '$class_name' doesn't match pattern"
                echo "- ❌ Usecase service \`$class_name\` doesn't match pattern \`$usecase_pattern\`" >> "$report"
                
                if $CI_MODE; then
                    echo "::error file=$file::Naming Convention violation: Usecase service '$class_name' doesn't match pattern"
                fi
            fi
        fi
    done
    
    # Add summary to report
    {
        echo ""
        echo "### Summary"
        echo ""
        if [ "$violations" -eq 0 ]; then
            echo "✅ **All classes follow naming conventions**"
        else
            echo "❌ **$violations naming convention violations found**"
        fi
        echo ""
    } >> "$report"
    
    return $violations
}

# Process each module
TOTAL_VIOLATIONS=0

for module in "${MODULES_TO_CHECK[@]}"; do
    log_info "Validating architecture for module: $module"
    
    # Create module report file
    module_report="$REPORT_DIR/$module-report.md"
    
    # Initialize report file
    cat > "$module_report" << EOF
# Architecture Validation Report for $module

Date: $(date "+%Y-%m-%d %H:%M:%S")
Module: $module

EOF
    
    # Run all validation checks
    module_violations=0
    
    # Check clean architecture dependencies
    check_clean_architecture "$module" "$module_report"
    clean_arch_violations=$?
    module_violations=$((module_violations + clean_arch_violations))
    
    # Check package structure
    check_package_structure "$module" "$module_report"
    package_violations=$?
    module_violations=$((module_violations + package_violations))
    
    # Check naming conventions
    check_naming_conventions "$module" "$module_report"
    naming_violations=$?
    module_violations=$((module_violations + naming_violations))
    
    # Add module violations to total
    TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + module_violations))
    
    # Add module summary to report
    cat >> "$module_report" << EOF
## Module Summary

- Clean Architecture Violations: $clean_arch_violations
- Package Structure Violations: $package_violations
- Naming Convention Violations: $naming_violations
- **Total Violations: $module_violations**

EOF
    
    if [ "$module_violations" -eq 0 ]; then
        log_success "Module $module passed architecture validation ✓"
    else
        log_warning "Module $module has $module_violations architecture violations ✗"
    fi
done

# Create summary report
summary_report="$REPORT_DIR/summary.md"

cat > "$summary_report" << EOF
# Architecture Validation Summary

Date: $(date "+%Y-%m-%d %H:%M:%S")
Modules checked: ${MODULES_TO_CHECK[*]}

## Summary

EOF

# Add results for each module
for module in "${MODULES_TO_CHECK[@]}"; do
    module_report="$REPORT_DIR/$module-report.md"
    if [ -f "$module_report" ]; then
        module_violations=$(grep -o "Total Violations: [0-9]*" "$module_report" | awk '{print $3}')
        
        if [ "$module_violations" -eq 0 ]; then
            echo "- $module: ✅ **No violations**" >> "$summary_report"
        else
            echo "- $module: ❌ **$module_violations violations**" >> "$summary_report"
        fi
    fi
done

cat >> "$summary_report" << EOF

## Overall Result

EOF

if [ "$TOTAL_VIOLATIONS" -eq 0 ]; then
    echo "✅ **Architecture validation passed with no violations**" >> "$summary_report"
    log_success "Architecture validation passed with no violations"
else
    echo "❌ **Architecture validation failed with $TOTAL_VIOLATIONS violations**" >> "$summary_report"
    log_warning "Architecture validation failed with $TOTAL_VIOLATIONS violations"
    
    if $CI_MODE; then
        echo "::error::Architecture validation failed with $TOTAL_VIOLATIONS violations. See reports for details."
    fi
fi

cat >> "$summary_report" << EOF

## Detailed Reports

EOF

for module in "${MODULES_TO_CHECK[@]}"; do
    echo "- [${module} Report](${module}-report.md)" >> "$summary_report"
done

# Generate HTML report
cat > "$REPORT_DIR/index.html" << EOF
<!DOCTYPE html>
<html>
<head>
  <title>Rinna Architecture Validation Report</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }
    h1, h2, h3 { color: #333; }
    .summary { background-color: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
    .module { background-color: #fff; padding: 15px; border-radius: 5px; margin-bottom: 10px; border: 1px solid #ddd; }
    .pass { color: green; }
    .fail { color: red; }
    table { border-collapse: collapse; width: 100%; }
    th, td { text-align: left; padding: 8px; border-bottom: 1px solid #ddd; }
    th { background-color: #f2f2f2; }
    .timestamp { color: #666; font-size: 0.9em; }
  </style>
</head>
<body>
  <h1>Rinna Architecture Validation Report</h1>
  <div class="timestamp">Generated on $(date '+%Y-%m-%d %H:%M:%S')</div>
  
  <h2>Summary</h2>
  <div class="summary">
    <table>
      <tr>
        <th>Module</th>
        <th>Clean Architecture</th>
        <th>Package Structure</th>
        <th>Naming Conventions</th>
        <th>Total</th>
        <th>Status</th>
      </tr>
EOF

# Add module results to HTML report
for module in "${MODULES_TO_CHECK[@]}"; do
    module_report="$REPORT_DIR/$module-report.md"
    if [ -f "$module_report" ]; then
        # Extract violations
        clean_arch_violations=$(grep -o "Clean Architecture Violations: [0-9]*" "$module_report" | awk '{print $4}')
        package_violations=$(grep -o "Package Structure Violations: [0-9]*" "$module_report" | awk '{print $4}')
        naming_violations=$(grep -o "Naming Convention Violations: [0-9]*" "$module_report" | awk '{print $4}')
        module_violations=$(grep -o "Total Violations: [0-9]*" "$module_report" | awk '{print $3}')
        
        # Add to HTML table
        cat >> "$REPORT_DIR/index.html" << EOF
      <tr>
        <td>$module</td>
        <td>$clean_arch_violations</td>
        <td>$package_violations</td>
        <td>$naming_violations</td>
        <td>$module_violations</td>
        <td class="$([ "$module_violations" -eq 0 ] && echo 'pass' || echo 'fail')">
          $([ "$module_violations" -eq 0 ] && echo '✓ Pass' || echo '✗ Fail')
        </td>
      </tr>
EOF
    fi
done

# Finish HTML report
cat >> "$REPORT_DIR/index.html" << EOF
      <tr>
        <td><strong>Overall</strong></td>
        <td colspan="3"></td>
        <td><strong>$TOTAL_VIOLATIONS</strong></td>
        <td class="$([ "$TOTAL_VIOLATIONS" -eq 0 ] && echo 'pass' || echo 'fail')">
          $([ "$TOTAL_VIOLATIONS" -eq 0 ] && echo '✓ Pass' || echo '✗ Fail')
        </td>
      </tr>
    </table>
  </div>
  
  <h2>Detailed Results</h2>
EOF

# Add links to detailed module reports
for module in "${MODULES_TO_CHECK[@]}"; do
    cat >> "$REPORT_DIR/index.html" << EOF
  <div class="module">
    <h3>$module</h3>
    <p>
      <a href="${module}-report.html">View detailed report for $module</a>
    </p>
  </div>
EOF

    # Convert module markdown to HTML (simplified)
    if [ -f "$REPORT_DIR/$module-report.md" ]; then
        if command -v pandoc &>/dev/null; then
            pandoc "$REPORT_DIR/$module-report.md" -o "$REPORT_DIR/$module-report.html" || true
        else
            # Simple markdown to HTML conversion
            awk '{gsub(/^# /, "<h1>"); gsub(/^## /, "<h2>"); gsub(/^### /, "<h3>"); gsub(/^- /, "<li>"); print}' \
                "$REPORT_DIR/$module-report.md" | \
                sed 's/^<li>/\n<li>/g' | \
                sed 's/^<h1>/<h1>/g' | \
                sed 's/^<h2>/<h2>/g' | \
                sed 's/^<h3>/<h3>/g' | \
                sed 's/✅/<span style="color:green">✅<\/span>/g' | \
                sed 's/❌/<span style="color:red">❌<\/span>/g' > "$REPORT_DIR/$module-report.html"
        fi
    fi
done

log_info "Architecture validation completed"
log_info "Reports saved to: $REPORT_DIR"
log_info "HTML report available at: $REPORT_DIR/index.html"

# Return appropriate exit code
if [ "$TOTAL_VIOLATIONS" -eq 0 ]; then
    exit 0
else
    exit 1
fi