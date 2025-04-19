#!/bin/bash

# Unified build script for Rinna polyglot application
# Usage: ./build.sh [component]
# component can be: all, java, python, go

set -e  # Exit immediately if a command exits with a non-zero status

# Define color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Default build target
BUILD_TARGET=${1:-"all"}
BUILD_MODE=${2:-"dev"}

# Project root directory (location of this script)
PROJECT_ROOT=$(pwd)

# Log file
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
LOG_DIR="${PROJECT_ROOT}/logs"
LOG_FILE="${LOG_DIR}/build-${TIMESTAMP}.log"
mkdir -p ${LOG_DIR}

# Log start of build
log_header() {
    echo -e "${BLUE}======================================================${NC}"
    echo -e "${BLUE}= Rinna Build System                                 =${NC}"
    echo -e "${BLUE}= Build Target: ${GREEN}$BUILD_TARGET${BLUE}                            =${NC}"
    echo -e "${BLUE}= Build Mode: ${GREEN}$BUILD_MODE${BLUE}                               =${NC}"
    echo -e "${BLUE}= Timestamp: ${GREEN}$(date)${BLUE}        =${NC}"
    echo -e "${BLUE}======================================================${NC}"
    echo ""

    # Log to file
    echo "======================================================" > ${LOG_FILE}
    echo "= Rinna Build System                                 =" >> ${LOG_FILE}
    echo "= Build Target: $BUILD_TARGET                            =" >> ${LOG_FILE}
    echo "= Build Mode: $BUILD_MODE                               =" >> ${LOG_FILE}
    echo "= Timestamp: $(date)        =" >> ${LOG_FILE}
    echo "======================================================" >> ${LOG_FILE}
    echo "" >> ${LOG_FILE}
}

log_section() {
    echo -e "${YELLOW}>>> $1${NC}"
    echo ">>> $1" >> ${LOG_FILE}
}

log_success() {
    echo -e "${GREEN}✓ $1${NC}"
    echo "✓ $1" >> ${LOG_FILE}
}

log_error() {
    echo -e "${RED}✗ $1${NC}"
    echo "✗ $1" >> ${LOG_FILE}
    exit 1
}

log_info() {
    echo -e "${PURPLE}ℹ $1${NC}"
    echo "ℹ $1" >> ${LOG_FILE}
}

check_requirements() {
    log_section "Checking build requirements"
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java not found, please install JDK"
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven not found, please install Maven"
    fi
    
    # Check Go
    if [[ "$BUILD_TARGET" == "all" || "$BUILD_TARGET" == "go" ]]; then
        if ! command -v go &> /dev/null; then
            log_error "Go not found, please install Go"
        fi
    fi
    
    # Check Python
    if [[ "$BUILD_TARGET" == "all" || "$BUILD_TARGET" == "python" ]]; then
        if ! command -v python3 &> /dev/null; then
            log_error "Python not found, please install Python 3"
        fi
        
        if ! command -v poetry &> /dev/null; then
            log_error "Poetry not found, please install Poetry"
        fi
    fi
    
    log_success "All required tools found"
}

build_java() {
    log_section "Building Java components"
    
    cd ${PROJECT_ROOT}/java
    
    # Check if we have a pom.xml
    if [ ! -f "pom.xml" ]; then
        log_error "No pom.xml found in java directory"
    fi
    
    # Determine Maven arguments based on build mode
    MVN_ARGS=""
    if [ "$BUILD_MODE" == "prod" ]; then
        MVN_ARGS="-P production"
    elif [ "$BUILD_MODE" == "test" ]; then
        MVN_ARGS="-P test"
    fi
    
    # Run Maven build
    log_info "Running Maven build..."
    mvn clean install $MVN_ARGS -DskipTests=false
    
    if [ $? -eq 0 ]; then
        log_success "Java build completed successfully"
    else
        log_error "Java build failed"
    fi
    
    cd ${PROJECT_ROOT}
}

build_python() {
    log_section "Building Python components"
    
    cd ${PROJECT_ROOT}/python
    
    # Check if we have a pyproject.toml
    if [ ! -f "pyproject.toml" ]; then
        log_error "No pyproject.toml found in python directory"
    fi
    
    # Install dependencies and build
    log_info "Setting up Python environment..."
    
    # Determine Poetry arguments based on build mode
    POETRY_ARGS=""
    if [ "$BUILD_MODE" == "prod" ]; then
        POETRY_ARGS="--extras 'all'"
    elif [ "$BUILD_MODE" == "test" ]; then
        POETRY_ARGS="--with dev"
    fi
    
    # Install dependencies using Poetry
    poetry install $POETRY_ARGS
    
    if [ $? -eq 0 ]; then
        log_success "Python dependencies installed successfully"
    else
        log_error "Python dependency installation failed"
    fi
    
    # Run tests if in test mode
    if [ "$BUILD_MODE" == "test" ]; then
        log_info "Running Python tests..."
        poetry run pytest
        
        if [ $? -eq 0 ]; then
            log_success "Python tests passed"
        else
            log_error "Python tests failed"
        fi
    fi
    
    log_success "Python build completed successfully"
    cd ${PROJECT_ROOT}
}

build_go() {
    log_section "Building Go components"
    
    cd ${PROJECT_ROOT}/go
    
    # Check if we have a go.mod file
    if [ ! -f "go.mod" ]; then
        log_error "No go.mod found in go directory"
    fi
    
    # Download dependencies
    log_info "Downloading Go dependencies..."
    go mod download
    
    if [ $? -ne 0 ]; then
        log_error "Failed to download Go dependencies"
    fi
    
    # Build Go components
    log_info "Building Go binaries..."
    
    # Determine build flags based on mode
    GO_BUILD_FLAGS=""
    if [ "$BUILD_MODE" == "prod" ]; then
        GO_BUILD_FLAGS="-ldflags=-s -ldflags=-w"
    fi
    
    # Create output directory
    mkdir -p bin
    
    # Find all main packages and build them
    go_packages=$(find ./src -name "main.go" -exec dirname {} \;)
    
    for pkg in $go_packages; do
        binary_name=$(basename $pkg)
        log_info "Building $binary_name..."
        go build $GO_BUILD_FLAGS -o bin/$binary_name $pkg
        
        if [ $? -eq 0 ]; then
            log_success "Built $binary_name successfully"
        else
            log_error "Failed to build $binary_name"
        fi
    done
    
    # Run tests if in test mode
    if [ "$BUILD_MODE" == "test" ]; then
        log_info "Running Go tests..."
        go test ./...
        
        if [ $? -eq 0 ]; then
            log_success "Go tests passed"
        else
            log_error "Go tests failed"
        fi
    fi
    
    log_success "Go build completed successfully"
    cd ${PROJECT_ROOT}
}

build_api_specs() {
    log_section "Processing API specifications"
    
    cd ${PROJECT_ROOT}/api-specs
    
    # Check if we have any API specs
    if [ ! -d "swagger" ] && [ ! -d "openapi" ]; then
        log_info "No API specifications found, skipping"
        cd ${PROJECT_ROOT}
        return 0
    fi
    
    # Process OpenAPI specs (if any tools are installed)
    if command -v swagger &> /dev/null; then
        log_info "Validating Swagger specs..."
        for spec in $(find ./swagger -name "*.yaml" -o -name "*.json"); do
            swagger validate $spec
            if [ $? -eq 0 ]; then
                log_success "Validated $spec successfully"
            else
                log_error "Failed to validate $spec"
            fi
        done
    else
        log_info "Swagger tools not installed, skipping validation"
    fi
    
    log_success "API specifications processed successfully"
    cd ${PROJECT_ROOT}
}

create_build_summary() {
    log_section "Creating build summary"
    
    SUMMARY_FILE="${LOG_DIR}/build-summary-latest.log"
    
    # Extract key information from log
    echo "Build Summary for ${TIMESTAMP}" > ${SUMMARY_FILE}
    echo "=========================" >> ${SUMMARY_FILE}
    echo "Build target: ${BUILD_TARGET}" >> ${SUMMARY_FILE}
    echo "Build mode: ${BUILD_MODE}" >> ${SUMMARY_FILE}
    echo "=========================" >> ${SUMMARY_FILE}
    echo "" >> ${SUMMARY_FILE}
    
    # Extract success messages
    grep "✓" ${LOG_FILE} >> ${SUMMARY_FILE}
    
    # Extract error messages if any
    if grep -q "✗" ${LOG_FILE}; then
        echo "" >> ${SUMMARY_FILE}
        echo "ERRORS:" >> ${SUMMARY_FILE}
        grep "✗" ${LOG_FILE} >> ${SUMMARY_FILE}
    fi
    
    log_success "Build summary created at ${SUMMARY_FILE}"
}

# Main build logic
main() {
    log_header
    check_requirements
    
    case ${BUILD_TARGET} in
        all)
            build_java
            build_python
            build_go
            build_api_specs
            ;;
        java)
            build_java
            ;;
        python)
            build_python
            ;;
        go)
            build_go
            ;;
        api-specs)
            build_api_specs
            ;;
        *)
            log_error "Unknown build target: ${BUILD_TARGET}. Valid targets are: all, java, python, go, api-specs"
            ;;
    esac
    
    create_build_summary
    log_success "Build completed successfully"
}

# Execute main function
main
