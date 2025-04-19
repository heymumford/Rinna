#!/bin/zsh
# build.sh - Unified polyglot build system for the Rinna project
# Usage: ./build.sh [component] [mode]

# --- STRICT MODE ---
emulate -L zsh
setopt ERR_EXIT PIPE_FAIL NO_UNSET NO_CLOBBER

# --- CONSTANTS ---
readonly SCRIPT_DIR=${0:a:h}
readonly PROJECT_ROOT=${SCRIPT_DIR}
readonly BUILD_TIMESTAMP=$(date +%Y%m%d-%H%M%S)
readonly LOG_DIR="${PROJECT_ROOT}/logs"
readonly LOG_FILE="${LOG_DIR}/build-${BUILD_TIMESTAMP}.log"
readonly LOG_LOCK="${LOG_DIR}/.log.lock"
readonly SUMMARY_FILE="${LOG_DIR}/build-summary-latest.log"
readonly ARTIFACTS_DIR="${PROJECT_ROOT}/build/artifacts"
readonly BUILD_OUTPUT_ARCHIVE="${ARTIFACTS_DIR}/build-output-${BUILD_TIMESTAMP}.tar.gz"

# --- COLOR CONSTANTS ---
readonly COLOR_RED=$'\e[0;31m'
readonly COLOR_GREEN=$'\e[0;32m'
readonly COLOR_YELLOW=$'\e[0;33m'
readonly COLOR_BLUE=$'\e[0;34m'
readonly COLOR_PURPLE=$'\e[0;35m'
readonly COLOR_CYAN=$'\e[0;36m'
readonly COLOR_NONE=$'\e[0m'

# --- BUILD CONFIGURATION ---
readonly MAX_PARALLEL_JOBS=12

# --- PARAMETER PARSING ---
BUILD_TARGET=${1:-"all"}
BUILD_MODE=${2:-"dev"}
BUILD_START_TIME=$(date +%s)

# --- ENVIRONMENT SETUP ---
mkdir -p ${LOG_DIR}
mkdir -p ${ARTIFACTS_DIR}
touch ${LOG_FILE}

# --- LOGGING FUNCTIONS ---
function logThreadSafe() {
    local message="$1"
    {
        zsystem flock ${LOG_LOCK}
        echo "$message" >> ${LOG_FILE}
    }
}

function logHeader() {
    local targetPadding=$((10 - ${#BUILD_TARGET}))
    local modePadding=$((11 - ${#BUILD_MODE}))
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    
    echo -e "${COLOR_BLUE}========================================================${COLOR_NONE}"
    echo -e "${COLOR_BLUE}= Rinna Build System                                   =${COLOR_NONE}"
    echo -e "${COLOR_BLUE}= Build Target: ${COLOR_GREEN}$BUILD_TARGET${COLOR_BLUE}$(printf '%*s' $targetPadding '')=${COLOR_NONE}"
    echo -e "${COLOR_BLUE}= Build Mode:   ${COLOR_GREEN}$BUILD_MODE${COLOR_BLUE}$(printf '%*s' $modePadding '')=${COLOR_NONE}"
    echo -e "${COLOR_BLUE}= Parallel Jobs: ${COLOR_GREEN}$MAX_PARALLEL_JOBS${COLOR_BLUE}                           =${COLOR_NONE}"
    echo -e "${COLOR_BLUE}= Timestamp:    ${COLOR_GREEN}$timestamp${COLOR_BLUE}  =${COLOR_NONE}"
    echo -e "${COLOR_BLUE}========================================================${COLOR_NONE}"
    echo ""

    {
        echo "========================================================"
        echo "= Rinna Build System                                   ="
        echo "= Build Target: $BUILD_TARGET$(printf '%*s' $targetPadding '')="
        echo "= Build Mode:   $BUILD_MODE$(printf '%*s' $modePadding '')="
        echo "= Parallel Jobs: $MAX_PARALLEL_JOBS                           ="
        echo "= Timestamp:    $timestamp  ="
        echo "========================================================"
        echo ""
    } > ${LOG_FILE}
}

function logSection() {
    echo -e "${COLOR_YELLOW}>>> $1${COLOR_NONE}"
    logThreadSafe ">>> $1"
}

function logSuccess() {
    echo -e "${COLOR_GREEN}✓ $1${COLOR_NONE}"
    logThreadSafe "✓ $1"
}

function logError() {
    echo -e "${COLOR_RED}✗ $1${COLOR_NONE}"
    logThreadSafe "✗ $1"
}

function logInfo() {
    echo -e "${COLOR_PURPLE}ℹ $1${COLOR_NONE}"
    logThreadSafe "ℹ $1"
}

function logWarning() {
    echo -e "${COLOR_CYAN}⚠ $1${COLOR_NONE}"
    logThreadSafe "⚠ $1"
}

function logCommandOutput() {
    local componentName="$1"
    local commandOutput="$2"
    
    logThreadSafe "--- $componentName Output ---"
    logThreadSafe "$commandOutput"
    logThreadSafe "--- End $componentName Output ---"
    
    # Save detailed output to component-specific log
    local componentLog="${LOG_DIR}/${componentName}-${BUILD_TIMESTAMP}.log"
    echo "$commandOutput" > ${componentLog}
    logInfo "Detailed $componentName output saved to ${componentLog}"
}

function handleCommandFailure() {
    local componentName="$1"
    local errorMessage="$2"
    local status=${3:-1}
    
    logError "$componentName build failed: $errorMessage"
    createBuildSummary "failed"
    exit $status
}

# --- BUILD VERIFICATION FUNCTIONS ---
function checkRequirements() {
    logSection "Checking build requirements"
    
    # Check for parallel utility
    if ! command -v parallel &> /dev/null; then
        logWarning "GNU parallel not found, some parallel operations won't be available"
        logInfo "Install with: brew install parallel"
    fi
    
    # Check Java
    if ! command -v java &> /dev/null; then
        handleCommandFailure "Environment" "Java not found, please install JDK"
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        handleCommandFailure "Environment" "Maven not found, please install Maven"
    fi
    
    # Check Go if needed
    if [[ "$BUILD_TARGET" == "all" || "$BUILD_TARGET" == "go" ]]; then
        if ! command -v go &> /dev/null; then
            handleCommandFailure "Environment" "Go not found, please install Go"
        fi
    fi
    
    # Check Python if needed
    if [[ "$BUILD_TARGET" == "all" || "$BUILD_TARGET" == "python" ]]; then
        if ! command -v python3 &> /dev/null; then
            handleCommandFailure "Environment" "Python not found, please install Python 3"
        fi
        
        if ! command -v poetry &> /dev/null; then
            handleCommandFailure "Environment" "Poetry not found, please install Poetry"
        fi
    fi
    
    logSuccess "All required tools found"
}

# --- BUILD COMPONENT FUNCTIONS ---
function buildJavaComponent() {
    logSection "Building Java components"
    
    cd ${PROJECT_ROOT}/java
    
    # Check if we have a pom.xml
    if [ ! -f "pom.xml" ]; then
        handleCommandFailure "Java" "No pom.xml found in java directory"
    fi
    
    # Determine Maven arguments based on build mode
    local mvnArgs="-T ${MAX_PARALLEL_JOBS}"
    if [ "$BUILD_MODE" == "prod" ]; then
        mvnArgs+=" -P production"
    elif [ "$BUILD_MODE" == "test" ]; then
        mvnArgs+=" -P test"
    fi
    
    # Run Maven build
    logInfo "Running Maven build with ${MAX_PARALLEL_JOBS} threads..."
    
    # Capture build output and status
    local buildOutput
    buildOutput=$(mvn clean install ${mvnArgs} -DskipTests=false 2>&1)
    local buildStatus=$?
    
    # Log the output
    logCommandOutput "java" "$buildOutput"
    
    if [ $buildStatus -eq 0 ]; then
        logSuccess "Java build completed successfully"
    else
        handleCommandFailure "Java" "Maven build failed with status $buildStatus"
    fi
    
    cd ${PROJECT_ROOT}
}

function buildPythonComponent() {
    logSection "Building Python components"
    
    cd ${PROJECT_ROOT}/python
    
    # Check if we have a pyproject.toml
    if [ ! -f "pyproject.toml" ]; then
        handleCommandFailure "Python" "No pyproject.toml found in python directory"
    fi
    
    # Determine Poetry arguments based on build mode
    local poetryArgs=""
    if [ "$BUILD_MODE" == "prod" ]; then
        poetryArgs="--extras 'all'"
    elif [ "$BUILD_MODE" == "test" ]; then
        poetryArgs="--with dev"
    fi
    
    # Install dependencies using Poetry
    logInfo "Setting up Python environment..."
    local poetryOutput
    poetryOutput=$(poetry install $poetryArgs 2>&1)
    local poetryStatus=$?
    
    # Log the output
    logCommandOutput "python-deps" "$poetryOutput"
    
    if [ $poetryStatus -eq 0 ]; then
        logSuccess "Python dependencies installed successfully"
    else
        handleCommandFailure "Python" "Poetry dependency installation failed with status $poetryStatus"
    fi
    
    # Run tests if in test mode
    if [ "$BUILD_MODE" == "test" ]; then
        runPythonTests
    fi
    
    logSuccess "Python build completed successfully"
    cd ${PROJECT_ROOT}
}

function runPythonTests() {
    logInfo "Running Python tests..."
    
    # Check for pytest-xdist for parallel testing
    if poetry run pip list | grep -q pytest-xdist; then
        logInfo "Running tests in parallel with ${MAX_PARALLEL_JOBS} workers..."
        local testOutput
        testOutput=$(poetry run pytest -n ${MAX_PARALLEL_JOBS} 2>&1)
        local testStatus=$?
    else
        logWarning "pytest-xdist not found, running tests sequentially"
        logInfo "Install with: poetry add --group dev pytest-xdist"
        local testOutput
        testOutput=$(poetry run pytest 2>&1)
        local testStatus=$?
    fi
    
    # Log the output
    logCommandOutput "python-tests" "$testOutput"
    
    if [ $testStatus -eq 0 ]; then
        logSuccess "Python tests passed"
    else
        handleCommandFailure "Python" "Tests failed with status $testStatus"
    fi
}

function buildGoComponent() {
    logSection "Building Go components"
    
    cd ${PROJECT_ROOT}/go
    
    # Check if we have a go.mod file
    if [ ! -f "go.mod" ]; then
        handleCommandFailure "Go" "No go.mod found in go directory"
    fi
    
    # Set GOMAXPROCS to use multiple cores
    export GOMAXPROCS=${MAX_PARALLEL_JOBS}
    
    # Download dependencies
    downloadGoDependencies
    
    # Build Go binaries
    buildGoBinaries
    
    # Run tests if in test mode
    if [ "$BUILD_MODE" == "test" ]; then
        runGoTests
    fi
    
    logSuccess "Go build completed successfully"
    cd ${PROJECT_ROOT}
}

function downloadGoDependencies() {
    logInfo "Downloading Go dependencies..."
    
    local downloadOutput
    downloadOutput=$(go mod download 2>&1)
    local downloadStatus=$?
    
    # Log the output
    logCommandOutput "go-deps" "$downloadOutput"
    
    if [ $downloadStatus -ne 0 ]; then
        handleCommandFailure "Go" "Failed to download dependencies with status $downloadStatus"
    fi
}

function buildGoBinaries() {
    logInfo "Building Go binaries with ${MAX_PARALLEL_JOBS} threads..."
    
    # Determine build flags based on mode
    local goBuildFlags="-p ${MAX_PARALLEL_JOBS}"
    if [ "$BUILD_MODE" == "prod" ]; then
        goBuildFlags+=" -ldflags=-s -ldflags=-w"
    fi
    
    # Create output directory
    mkdir -p bin
    
    # Find all main packages and build them
    local goPackages
    goPackages=($(find ./src -name "main.go" -exec dirname {} \;))
    
    # Check if any main packages were found
    if [ ${#goPackages[@]} -eq 0 ]; then
        logWarning "No Go main packages found to build"
        return 0
    fi
    
    # Build packages in parallel if GNU parallel is available
    if command -v parallel &> /dev/null; then
        buildGoBinariesParallel "$goBuildFlags" "${goPackages[@]}"
    else
        buildGoBinariesSequential "$goBuildFlags" "${goPackages[@]}"
    fi
}

function buildGoBinariesParallel() {
    local goBuildFlags="$1"
    shift
    local goPackages=("$@")
    
    logInfo "Building ${#goPackages[@]} Go packages in parallel..."
    
    # Create a temporary build script
    local tempBuildScript="${LOG_DIR}/go-build-script-${BUILD_TIMESTAMP}.sh"
    cat > ${tempBuildScript} << 'EOF'
#!/bin/zsh
pkg=$1
binary_name=$(basename $pkg)
output=$(go build $2 -o bin/$binary_name $pkg 2>&1)
status=$?
echo "status:$status:$binary_name:$output"
EOF
    chmod +x ${tempBuildScript}
    
    # Run builds in parallel
    export goBuildFlags
    local buildResults
    buildResults=$(parallel --will-cite -j ${MAX_PARALLEL_JOBS} ${tempBuildScript} {} "${goBuildFlags}" ::: ${goPackages[@]})
    
    # Process results
    local allOutput=""
    local buildFailed=false
    
    echo "$buildResults" | while IFS= read -r line; do
        if [[ $line =~ status:([0-9]+):([^:]+):(.*) ]]; then
            local status=${match[1]}
            local binary=${match[2]}
            local output=${match[3]}
            
            # Collect all output
            allOutput+="Output for $binary:\n$output\n\n"
            
            if [ $status -eq 0 ]; then
                logSuccess "Built $binary successfully"
            else
                logError "Failed to build $binary"
                buildFailed=true
            fi
        fi
    done
    
    # Log combined output
    logCommandOutput "go-build" "$allOutput"
    
    # Clean up
    rm ${tempBuildScript}
    
    # Check if any builds failed
    if $buildFailed; then
        handleCommandFailure "Go" "Some binary builds failed"
    fi
}

function buildGoBinariesSequential() {
    local goBuildFlags="$1"
    shift
    local goPackages=("$@")
    
    logWarning "GNU parallel not found, building Go packages sequentially"
    local allOutput=""
    local buildFailed=false
    
    for pkg in ${goPackages[@]}; do
        local binaryName=$(basename $pkg)
        logInfo "Building $binaryName..."
        
        local buildOutput
        buildOutput=$(go build ${goBuildFlags} -o bin/$binaryName $pkg 2>&1)
        local buildStatus=$?
        
        # Collect output
        allOutput+="Output for $binaryName:\n$buildOutput\n\n"
        
        if [ $buildStatus -eq 0 ]; then
            logSuccess "Built $binaryName successfully"
        else
            logError "Failed to build $binaryName"
            buildFailed=true
        fi
    done
    
    # Log combined output
    logCommandOutput "go-build" "$allOutput"
    
    # Check if any builds failed
    if $buildFailed; then
        handleCommandFailure "Go" "Some binary builds failed"
    fi
}

function runGoTests() {
    logInfo "Running Go tests with ${MAX_PARALLEL_JOBS} threads..."
    
    local testOutput
    testOutput=$(go test -p ${MAX_PARALLEL_JOBS} ./... 2>&1)
    local testStatus=$?
    
    # Log the output
    logCommandOutput "go-tests" "$testOutput"
    
    if [ $testStatus -eq 0 ]; then
        logSuccess "Go tests passed"
    else
        handleCommandFailure "Go" "Tests failed with status $testStatus"
    fi
}

function processApiSpecs() {
    logSection "Processing API specifications"
    
    # Ensure the directory exists
    mkdir -p ${PROJECT_ROOT}/api-specs
    cd ${PROJECT_ROOT}/api-specs
    
    # Check if we have any API specs
    if [ ! -d "swagger" ] && [ ! -d "openapi" ]; then
        logInfo "No API specifications found, creating directories"
        mkdir -p swagger openapi
        cd ${PROJECT_ROOT}
        return 0
    fi
    
    # Process OpenAPI specs
    validateApiSpecs
    
    logSuccess "API specifications processed successfully"
    cd ${PROJECT_ROOT}
}

function validateApiSpecs() {
    # Check if Swagger tools are installed
    if ! command -v swagger &> /dev/null; then
        logWarning "Swagger tools not installed, skipping validation"
        logInfo "Install with: brew install go-swagger"
        return 0
    fi
    
    logInfo "Validating API specifications..."
    
    # Find all spec files
    local specs
    specs=($(find . -name "*.yaml" -o -name "*.json"))
    
    if [ ${#specs[@]} -eq 0 ]; then
        logWarning "No API specification files found"
        return 0
    fi
    
    # Validate specs (parallel or sequential)
    if command -v parallel &> /dev/null; then
        validateApiSpecsParallel "${specs[@]}"
    else
        validateApiSpecsSequential "${specs[@]}"
    fi
}

function validateApiSpecsParallel() {
    local specs=("$@")
    
    logInfo "Validating ${#specs[@]} specs in parallel..."
    
    # Create a temporary validation script
    local tempValidationScript="${LOG_DIR}/swagger-validate-script-${BUILD_TIMESTAMP}.sh"
    cat > ${tempValidationScript} << 'EOF'
#!/bin/zsh
spec=$1
output=$(swagger validate $spec 2>&1)
status=$?
echo "status:$status:$spec:$output"
EOF
    chmod +x ${tempValidationScript}
    
    # Run validations in parallel
    local validationResults
    validationResults=$(parallel --will-cite -j ${MAX_PARALLEL_JOBS} ${tempValidationScript} {} ::: ${specs[@]})
    
    # Process results
    local allOutput=""
    local validationFailed=false
    
    echo "$validationResults" | while IFS= read -r line; do
        if [[ $line =~ status:([0-9]+):([^:]+):(.*) ]]; then
            local status=${match[1]}
            local spec=${match[2]}
            local output=${match[3]}
            
            # Collect all output
            allOutput+="Validation for $spec:\n$output\n\n"
            
            if [ $status -eq 0 ]; then
                logSuccess "Validated $spec successfully"
            else
                logError "Failed to validate $spec"
                validationFailed=true
            fi
        fi
    done
    
    # Log combined output
    logCommandOutput "api-specs" "$allOutput"
    
    # Clean up
    rm ${tempValidationScript}
    
    # Check if any validations failed
    if $validationFailed; then
        handleCommandFailure "API Specs" "Some specification validations failed"
    fi
}

function validateApiSpecsSequential() {
    local specs=("$@")
    
    logWarning "GNU parallel not found, validating specs sequentially"
    local allOutput=""
    local validationFailed=false
    
    for spec in ${specs[@]}; do
        logInfo "Validating $spec..."
        
        local validationOutput
        validationOutput=$(swagger validate $spec 2>&1)
        local validationStatus=$?
        
        # Collect output
        allOutput+="Validation for $spec:\n$validationOutput\n\n"
        
        if [ $validationStatus -eq 0 ]; then
            logSuccess "Validated $spec successfully"
        else
            logError "Failed to validate $spec"
            validationFailed=true
        fi
    done
    
    # Log combined output
    logCommandOutput "api-specs" "$allOutput"
    
    # Check if any validations failed
    if $validationFailed; then
        handleCommandFailure "API Specs" "Some specification validations failed"
    fi
}

# --- REPORTING FUNCTIONS ---
function createBuildSummary() {
    local buildStatus=${1:-"success"}
    local buildEndTime=$(date +%s)
    local buildDuration=$((buildEndTime - BUILD_START_TIME))
    
    logSection "Creating build summary"
    
    {
        echo "Build Summary for ${BUILD_TIMESTAMP}"
        echo "================================"
        echo "Status: ${buildStatus}"
        echo "Build target: ${BUILD_TARGET}"
        echo "Build mode: ${BUILD_MODE}"
        echo "Parallel jobs: ${MAX_PARALLEL_JOBS}"
        echo "================================"
        echo ""
        
        # Extract success messages
        grep "^✓" ${LOG_FILE} || true
        
        # Extract warning messages if any
        if grep -q "^⚠" ${LOG_FILE}; then
            echo ""
            echo "WARNINGS:"
            grep "^⚠" ${LOG_FILE}
        fi
        
        # Extract error messages if any
        if grep -q "^✗" ${LOG_FILE}; then
            echo ""
            echo "ERRORS:"
            grep "^✗" ${LOG_FILE}
        fi
        
        # Add build time information
        echo ""
        echo "Build duration: $(printf '%dm:%02ds' $((buildDuration/60)) $((buildDuration%60)))"
        echo "Build started at: $(date -r $BUILD_START_TIME)"
        echo "Build completed at: $(date -r $buildEndTime)"
        echo ""
    } > ${SUMMARY_FILE}
    
    logSuccess "Build summary created at ${SUMMARY_FILE}"
}

function createBuildArtifact() {
    logSection "Creating build artifact"
    
    # Create a tar.gz of all log files
    tar -czf ${BUILD_OUTPUT_ARCHIVE} ${LOG_DIR}/*.log
    
    # Also include relevant output files based on what was built
    if [[ "$BUILD_TARGET" == "all" || "$BUILD_TARGET" == "java" ]]; then
        tar -rf ${BUILD_OUTPUT_ARCHIVE} ${PROJECT_ROOT}/java/*/target/*.jar 2>/dev/null || true
    fi
    
    if [[ "$BUILD_TARGET" == "all" || "$BUILD_TARGET" == "go" ]]; then
        tar -rf ${BUILD_OUTPUT_ARCHIVE} ${PROJECT_ROOT}/go/bin/* 2>/dev/null || true
    fi
    
    logSuccess "Build artifact created at ${BUILD_OUTPUT_ARCHIVE}"
}

function displayBuildCompletion() {
    local buildEndTime=$(date +%s)
    local buildDuration=$((buildEndTime - BUILD_START_TIME))
    
    echo ""
    echo -e "${COLOR_GREEN}========================================================${COLOR_NONE}"
    echo -e "${COLOR_GREEN}= Build Completed Successfully                         =${COLOR_NONE}"
    echo -e "${COLOR_GREEN}= Time: $(printf '%dm:%02ds' $((buildDuration/60)) $((buildDuration%60)))                                      =${COLOR_NONE}"
    echo -e "${COLOR_GREEN}= Build Artifact: ${BUILD_OUTPUT_ARCHIVE}${COLOR_NONE}"
    echo -e "${COLOR_GREEN}= Build Summary: ${SUMMARY_FILE}${COLOR_NONE}"
    echo -e "${COLOR_GREEN}========================================================${COLOR_NONE}"
}

function displayHelp() {
    echo -e "${COLOR_BLUE}Rinna Polyglot Build System${COLOR_NONE}"
    echo ""
    echo -e "Usage: ${COLOR_YELLOW}./build.sh [component] [mode]${COLOR_NONE}"
    echo ""
    echo -e "Components:"
    echo -e "  ${COLOR_GREEN}all${COLOR_NONE}        Build all components (default)"
    echo -e "  ${COLOR_GREEN}java${COLOR_NONE}       Build only Java components"
    echo -e "  ${COLOR_GREEN}python${COLOR_NONE}     Build only Python components"
    echo -e "  ${COLOR_GREEN}go${COLOR_NONE}         Build only Go components"
    echo -e "  ${COLOR_GREEN}api-specs${COLOR_NONE}  Process API specifications"
    echo ""
    echo -e "Modes:"
    echo -e "  ${COLOR_GREEN}dev${COLOR_NONE}        Development build (default)"
    echo -e "  ${COLOR_GREEN}test${COLOR_NONE}       Development build with tests"
    echo -e "  ${COLOR_GREEN}prod${COLOR_NONE}       Production build with optimizations"
    echo ""
    echo -e "Parallel Processing:"
    echo -e "  Using ${COLOR_YELLOW}${MAX_PARALLEL_JOBS}${COLOR_NONE} parallel jobs"
    echo ""
    echo -e "Examples:"
    echo -e "  ${COLOR_YELLOW}./build.sh${COLOR_NONE}                  Build all components in dev mode"
    echo -e "  ${COLOR_YELLOW}./build.sh java${COLOR_NONE}             Build only Java components in dev mode"
    echo -e "  ${COLOR_YELLOW}./build.sh python test${COLOR_NONE}      Build Python components with tests"
    echo -e "  ${COLOR_YELLOW}./build.sh all prod${COLOR_NONE}         Build all components for production"
    echo -e "  ${COLOR_YELLOW}./build.sh api-specs${COLOR_NONE}        Process API specifications only"
    echo -e "  ${COLOR_YELLOW}./build.sh help${COLOR_NONE}             Display this help message"
    echo ""
    exit 0
}

# --- MAIN EXECUTION ---
function main() {
    # Check for help argument
    if [[ "$BUILD_TARGET" == "help" || "$BUILD_TARGET" == "--help" || "$BUILD_TARGET" == "-h" ]]; then
        displayHelp
    fi
    
    # Begin build process
    logHeader
    checkRequirements
    
    # Run appropriate build targets
    case ${BUILD_TARGET} in
        all)
            buildJavaComponent
            buildPythonComponent
            buildGoComponent
            processApiSpecs
            ;;
        java)
            buildJavaComponent
            ;;
        python)
            buildPythonComponent
            ;;
        go)
            buildGoComponent
            ;;
        api-specs)
            processApiSpecs
            ;;
        *)
            handleCommandFailure "Build" "Unknown build target: ${BUILD_TARGET}. Valid targets are: all, java, python, go, api-specs" 2
            ;;
    esac
    
    # Create build summary and artifact
    createBuildSummary
    createBuildArtifact
    displayBuildCompletion
}

# Execute main function
main
