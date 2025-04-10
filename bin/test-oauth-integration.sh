#!/bin/bash
# Script to test OAuth integration
# Copyright (c) 2025 Eric C. Mumford (@heymumford)

set -e

# Source common functions
if [[ -f ./bin/common/rinna_logger.sh ]]; then
    source ./bin/common/rinna_logger.sh
else
    echo "Error: Unable to find common logger script"
    exit 1
fi

# Configuration
API_PORT=${1:-8080}
API_HOST=${2:-localhost}
BASE_URL="http://${API_HOST}:${API_PORT}/api/v1"
AUTH_TOKEN="test-token"  # For testing only
PROJECT_ID="RINNA"
USER_ID="test-user"

# Log header
log_info "==================================================="
log_info "Rinna OAuth Integration Test"
log_info "==================================================="
log_info "API URL: ${BASE_URL}"
log_info "Project: ${PROJECT_ID}"
log_info "User: ${USER_ID}"
log_info "==================================================="

# Check if the API server is running
function check_server() {
    log_info "Checking if API server is running..."
    if ! curl -s "${BASE_URL}/health" > /dev/null; then
        log_error "API server not running. Please start the server first."
        exit 1
    fi
    log_success "API server is running"
}

# List OAuth providers
function list_providers() {
    log_info "Listing OAuth providers..."
    curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" \
         "${BASE_URL}/oauth/providers" | jq .
}

# Get a specific provider
function get_provider() {
    local provider=$1
    log_info "Getting details for provider: ${provider}"
    curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" \
         "${BASE_URL}/oauth/providers/${provider}" | jq .
}

# Get authorization URL for a provider
function get_auth_url() {
    local provider=$1
    log_info "Getting authorization URL for provider: ${provider}"
    curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" \
         "${BASE_URL}/oauth/authorize/${provider}?project=${PROJECT_ID}&user_id=${USER_ID}" | jq .
}

# List tokens for a project
function list_tokens() {
    log_info "Listing tokens for project: ${PROJECT_ID}"
    curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" \
         "${BASE_URL}/oauth/tokens?project=${PROJECT_ID}" | jq .
}

# Get a specific token
function get_token() {
    local provider=$1
    log_info "Getting token for provider: ${provider}, project: ${PROJECT_ID}, user: ${USER_ID}"
    curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" \
         "${BASE_URL}/oauth/tokens/${provider}?project=${PROJECT_ID}&user_id=${USER_ID}" | jq .
}

# Revoke a token
function revoke_token() {
    local provider=$1
    log_info "Revoking token for provider: ${provider}, project: ${PROJECT_ID}, user: ${USER_ID}"
    curl -s -X DELETE -H "Authorization: Bearer ${AUTH_TOKEN}" \
         "${BASE_URL}/oauth/tokens/${provider}?project=${PROJECT_ID}&user_id=${USER_ID}" | jq .
}

# Run the tests
check_server
list_providers

# Test with GitHub provider
if [[ -n $(curl -s -H "Authorization: Bearer ${AUTH_TOKEN}" "${BASE_URL}/oauth/providers" | grep -o "github") ]]; then
    get_provider "github"
    get_auth_url "github"
    
    # Note: We can't fully test the OAuth flow without a GitHub app
    # and user interaction, but we can test the API endpoints
    
    log_info "Note: Full OAuth flow testing requires a registered OAuth app"
    log_info "      and user interaction to authorize the application."
fi

# List tokens (likely empty in a fresh environment)
list_tokens

log_info "==================================================="
log_info "OAuth Integration Test Completed"
log_info "==================================================="