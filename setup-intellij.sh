#!/bin/zsh
# setup-intellij.sh - Script to configure IntelliJ IDEA settings for Rinna project
# This script creates or overwrites IntelliJ IDEA configuration files

# Strict mode
emulate -L zsh
setopt ERR_EXIT PIPE_FAIL NO_UNSET

# Constants
readonly PROJECT_ROOT=$(pwd)
readonly IDEA_DIR="${PROJECT_ROOT}/.idea"
readonly RUN_DIR="${PROJECT_ROOT}/.run"

# Color constants for output
readonly COLOR_GREEN=$'\e[0;32m'
readonly COLOR_YELLOW=$'\e[0;33m'
readonly COLOR_RED=$'\e[0;31m'
readonly COLOR_BLUE=$'\e[0;34m'
readonly COLOR_NONE=$'\e[0m'

# Output functions
function logInfo() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_NONE} $1"
}

function logSuccess() {
    echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_NONE} $1"
}

function logWarning() {
    echo -e "${COLOR_YELLOW}[WARNING]${COLOR_NONE} $1"
}

function logError() {
    echo -e "${COLOR_RED}[ERROR]${COLOR_NONE} $1"
    return 1
}

# Create directories
function createDirectories() {
    logInfo "Creating required directories..."
    
    if [[ -d "${IDEA_DIR}" ]]; then
        logWarning "Directory ${IDEA_DIR} already exists"
    else
        mkdir -p "${IDEA_DIR}"
        logSuccess "Created ${IDEA_DIR}"
    fi
    
    if [[ -d "${RUN_DIR}" ]]; then
        logWarning "Directory ${RUN_DIR} already exists"
    else
        mkdir -p "${RUN_DIR}"
        logSuccess "Created ${RUN_DIR}"
    fi
}

# Create the misc.xml file
function createMiscXml() {
    local filePath="${IDEA_DIR}/misc.xml"
    
    logInfo "Creating ${filePath}..."
    
    cat > "${filePath}" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="ExternalStorageConfigurationManager" enabled="true" />
  <component name="MavenProjectsManager">
    <option name="originalFiles">
      <list>
        <option value="$PROJECT_DIR$/java/pom.xml" />
        <option value="$PROJECT_DIR$/java/rinna-core/pom.xml" />
        <option value="$PROJECT_DIR$/java/rinna-cli/pom.xml" />
        <option value="$PROJECT_DIR$/java/rinna-data/pom.xml" />
      </list>
    </option>
  </component>
  <component name="ProjectRootManager" version="2" languageLevel="JDK_23" default="true" project-jdk-name="23" project-jdk-type="JavaSDK">
    <output url="file://$PROJECT_DIR$/build/idea" />
  </component>
  <component name="ProjectType">
    <option name="id" value="jpab" />
  </component>
</project>
EOF

    logSuccess "Created ${filePath}"
}

# Create the encodings.xml file
function createEncodingsXml() {
    local filePath="${IDEA_DIR}/encodings.xml"
    
    logInfo "Creating ${filePath}..."
    
    cat > "${filePath}" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="Encoding">
    <file url="file://$PROJECT_DIR$/java/rinna-cli/src/main/java" charset="UTF-8" />
    <file url="file://$PROJECT_DIR$/java/rinna-cli/src/main/resources" charset="UTF-8" />
    <file url="file://$PROJECT_DIR$/java/rinna-core/src/main/java" charset="UTF-8" />
    <file url="file://$PROJECT_DIR$/java/rinna-core/src/main/resources" charset="UTF-8" />
    <file url="file://$PROJECT_DIR$/java/rinna-data/src/main/java" charset="UTF-8" />
    <file url="file://$PROJECT_DIR$/java/rinna-data/src/main/resources" charset="UTF-8" />
    <file url="file://$PROJECT_DIR$/java/src/main/java" charset="UTF-8" />
    <file url="file://$PROJECT_DIR$/java/src/main/resources" charset="UTF-8" />
    <file url="PROJECT" charset="UTF-8" />
  </component>
</project>
EOF

    logSuccess "Created ${filePath}"
}

# Create the .idea/.gitignore file
function createIdeaGitignore() {
    local filePath="${IDEA_DIR}/.gitignore"
    
    logInfo "Creating ${filePath}..."
    
    cat > "${filePath}" << 'EOF'
# Default ignored files
/shelf/
/workspace.xml
# Editor-based HTTP Client requests
/httpRequests/
# Datasource local storage ignored files
/dataSources/
/dataSources.local.xml
# User-specific files
/tasks.xml
/usage.statistics.xml
/dictionaries
/shelf
/*.iws
# Sensitive or high-churn files
/dataSources/
/dataSources.local.xml
/uiDesigner.xml
/dynamic.xml
/jarRepositories.xml
/libraries-with-intellij-classes.xml
EOF

    logSuccess "Created ${filePath}"
}

# Create the README.md file in .idea directory
function createIdeaReadme() {
    local filePath="${IDEA_DIR}/README.md"
    
    logInfo "Creating ${filePath}..."
    
    cat > "${filePath}" << 'EOF'
# IntelliJ IDEA Configuration

This directory contains shared IntelliJ IDEA configuration for the Rinna project.

## Configuration Files

- `misc.xml`: Configures JDK and Maven settings
- `encodings.xml`: Sets file encodings
- `.gitignore`: Controls which IDE settings are shared vs. personal
- Various run configurations in the `.run` directory at the project root

## Setup Instructions

1. Open the project in IntelliJ IDEA Ultimate
2. Ensure you have JDK 23 installed and configured
3. Make sure Maven, Python, and Go plugins are installed
4. Import the Maven projects from `/java`
5. Configure Python SDK for the `/python` directory
6. Configure Go SDK for the `/go` directory

## Python Configuration

For the Python components:

1. Go to File > Project Structure > Modules
2. Add a new Python module for the `/python` directory
3. Configure the Python interpreter to use Poetry:
   - Go to Settings > Python Interpreter
   - Add a new Poetry Environment
   - Point it to `/python/pyproject.toml`

## Go Configuration

For the Go components:

1. Go to Settings > Languages & Frameworks > Go
2. Set GOROOT to your Go installation
3. Add `/go` as a content root
4. Make sure Go modules integration is enabled

## Run Configurations

Pre-configured run configurations:

- `Build All`: Runs the full build script 
- `Build Java`: Builds only Java components
- `Build Python`: Builds only Python components
- `Build Go`: Builds only Go components

Additional run configurations can be created as needed.
EOF

    logSuccess "Created ${filePath}"
}

# Create run configuration file for Build All
function createRunConfigBuildAll() {
    local filePath="${RUN_DIR}/Build All.run.xml"
    
    logInfo "Creating ${filePath}..."
    
    cat > "${filePath}" << 'EOF'
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Build All" type="ShConfigurationType">
    <option name="SCRIPT_TEXT" value="" />
    <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
    <option name="SCRIPT_PATH" value="$PROJECT_DIR$/build.sh" />
    <option name="SCRIPT_OPTIONS" value="all" />
    <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
    <option name="SCRIPT_WORKING_DIRECTORY" value="$PROJECT_DIR$" />
    <option name="INDEPENDENT_INTERPRETER_PATH" value="true" />
    <option name="INTERPRETER_PATH" value="/bin/zsh" />
    <option name="INTERPRETER_OPTIONS" value="" />
    <option name="EXECUTE_IN_TERMINAL" value="true" />
    <option name="EXECUTE_SCRIPT_FILE" value="true" />
    <envs />
    <method v="2" />
  </configuration>
</component>
EOF

    logSuccess "Created ${filePath}"
}

# Create run configuration file for Build Java
function createRunConfigBuildJava() {
    local filePath="${RUN_DIR}/Build Java.run.xml"
    
    logInfo "Creating ${filePath}..."
    
    cat > "${filePath}" << 'EOF'
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Build Java" type="ShConfigurationType">
    <option name="SCRIPT_TEXT" value="" />
    <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
    <option name="SCRIPT_PATH" value="$PROJECT_DIR$/build.sh" />
    <option name="SCRIPT_OPTIONS" value="java" />
    <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
    <option name="SCRIPT_WORKING_DIRECTORY" value="$PROJECT_DIR$" />
    <option name="INDEPENDENT_INTERPRETER_PATH" value="true" />
    <option name="INTERPRETER_PATH" value="/bin/zsh" />
    <option name="INTERPRETER_OPTIONS" value="" />
    <option name="EXECUTE_IN_TERMINAL" value="true" />
    <option name="EXECUTE_SCRIPT_FILE" value="true" />
    <envs />
    <method v="2" />
  </configuration>
</component>
EOF

    logSuccess "Created ${filePath}"
}

# Create run configuration file for Build Python
function createRunConfigBuildPython() {
    local filePath="${RUN_DIR}/Build Python.run.xml"
    
    logInfo "Creating ${filePath}..."
    
    cat > "${filePath}" << 'EOF'
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Build Python" type="ShConfigurationType">
    <option name="SCRIPT_TEXT" value="" />
    <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
    <option name="SCRIPT_PATH" value="$PROJECT_DIR$/build.sh" />
    <option name="SCRIPT_OPTIONS" value="python" />
    <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
    <option name="SCRIPT_WORKING_DIRECTORY" value="$PROJECT_DIR$" />
    <option name="INDEPENDENT_INTERPRETER_PATH" value="true" />
    <option name="INTERPRETER_PATH" value="/bin/zsh" />
    <option name="INTERPRETER_OPTIONS" value="" />
    <option name="EXECUTE_IN_TERMINAL" value="true" />
    <option name="EXECUTE_SCRIPT_FILE" value="true" />
    <envs />
    <method v="2" />
  </configuration>
</component>
EOF

    logSuccess "Created ${filePath}"
}

# Create run configuration file for Build Go
function createRunConfigBuildGo() {
    local filePath="${RUN_DIR}/Build Go.run.xml"
    
    logInfo "Creating ${filePath}..."
    
    cat > "${filePath}" << 'EOF'
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="Build Go" type="ShConfigurationType">
    <option name="SCRIPT_TEXT" value="" />
    <option name="INDEPENDENT_SCRIPT_PATH" value="true" />
    <option name="SCRIPT_PATH" value="$PROJECT_DIR$/build.sh" />
    <option name="SCRIPT_OPTIONS" value="go" />
    <option name="INDEPENDENT_SCRIPT_WORKING_DIRECTORY" value="true" />
    <option name="SCRIPT_WORKING_DIRECTORY" value="$PROJECT_DIR$" />
    <option name="INDEPENDENT_INTERPRETER_PATH" value="true" />
    <option name="INTERPRETER_PATH" value="/bin/zsh" />
    <option name="INTERPRETER_OPTIONS" value="" />
    <option name="EXECUTE_IN_TERMINAL" value="true" />
    <option name="EXECUTE_SCRIPT_FILE" value="true" />
    <envs />
    <method v="2" />
  </configuration>
</component>
EOF

    logSuccess "Created ${filePath}"
}

# Run setup
function main() {
    logInfo "Starting IntelliJ IDEA configuration setup..."
    
    # Create necessary directories
    createDirectories
    
    # Create .idea configuration files
    createMiscXml
    createEncodingsXml
    createIdeaGitignore
    createIdeaReadme
    
    # Create run configurations
    createRunConfigBuildAll
    createRunConfigBuildJava
    createRunConfigBuildPython
    createRunConfigBuildGo
    
    logSuccess "IntelliJ IDEA configuration setup complete!"
    logInfo "You can now import/reopen the project in IntelliJ IDEA Ultimate."
}

# Execute main function
main
