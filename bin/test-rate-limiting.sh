#!/bin/bash
# Script to test rate limiting functionality
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
API_PORT=${1:-9080}
API_HOST=${2:-localhost}
BASE_URL="http://${API_HOST}:${API_PORT}/api/v1"
AUTH_TOKEN="ri-test-1234"  # For testing only

# Test parameters
NUM_REQUESTS=10
DELAY=0.1  # seconds between requests

# Log function
log_info() {
    echo "[INFO] $1"
}

log_error() {
    echo "[ERROR] $1"
}

log_success() {
    echo "[SUCCESS] $1"
}

# Make a test request with timing
make_request() {
    local endpoint=$1
    local request_num=$2
    local extra_param=$3
    
    # Add a unique parameter to prevent caching
    if [ -z "$extra_param" ]; then
        extra_param="ts=$(date +%s%N)"
    fi
    
    local url="${BASE_URL}${endpoint}?${extra_param}"
    
    start_time=$(date +%s.%N)
    
    # Make the request and capture status code and headers
    response=$(curl -s -o /dev/null -w "%{http_code}|%{time_total}|%{response_header[X-RateLimit-Limit]}|%{response_header[X-RateLimit-Remaining]}|%{response_header[X-RateLimit-Reset]}" \
               -H "Authorization: Bearer ${AUTH_TOKEN}" \
               "$url")
    
    end_time=$(date +%s.%N)
    duration=$(echo "$end_time - $start_time" | bc)
    
    # Parse response
    status_code=$(echo "$response" | cut -d'|' -f1)
    time_total=$(echo "$response" | cut -d'|' -f2)
    rate_limit=$(echo "$response" | cut -d'|' -f3)
    rate_remaining=$(echo "$response" | cut -d'|' -f4)
    rate_reset=$(echo "$response" | cut -d'|' -f5)
    
    # Print formatted output
    if [ "$status_code" -eq 200 ]; then
        log_info "Request $request_num to $endpoint: $status_code OK (${time_total}s) | Limit: $rate_limit | Remaining: $rate_remaining"
    elif [ "$status_code" -eq 429 ]; then
        log_error "Request $request_num to $endpoint: $status_code Rate limited (${time_total}s) | Limit: $rate_limit | Remaining: 0"
    else
        log_error "Request $request_num to $endpoint: $status_code Error (${time_total}s)"
    fi
    
    # Return status code for checking
    echo "$status_code"
}

# Test rate limiting on a specific endpoint
test_endpoint() {
    local endpoint=$1
    local expected_limit=$2
    local description=$3
    
    log_info "-------------------------------------------------"
    log_info "Testing rate limiting on $description ($endpoint)"
    log_info "Expected limit: $expected_limit requests"
    log_info "-------------------------------------------------"
    
    local rate_limited=false
    
    # Make multiple requests to trigger rate limiting
    for i in $(seq 1 $NUM_REQUESTS); do
        status=$(make_request "$endpoint" "$i")
        
        # Check if rate limited
        if [ "$status" -eq 429 ]; then
            rate_limited=true
            log_info "Rate limiting triggered after $i requests"
            break
        fi
        
        # Add small delay between requests
        sleep $DELAY
    done
    
    # Verify rate limiting worked as expected
    if [ "$rate_limited" = true ]; then
        log_success "Rate limiting is working correctly for $description"
    else
        log_error "Rate limiting was not triggered for $description after $NUM_REQUESTS requests"
    fi
    
    # Wait before next test to allow rate limit to reset
    sleep 2
}

# Main test function
run_tests() {
    log_info "Starting rate limiting tests on $BASE_URL"
    
    # Test different endpoints with different expected limits
    test_endpoint "/health" 1200 "Health check endpoint"
    test_endpoint "/projects" 300 "Projects endpoint"
    test_endpoint "/auth/token" 60 "Authentication endpoint"
    
    # Test whitelisted IP (this should not be rate limited)
    log_info "-------------------------------------------------"
    log_info "Testing whitelisted IP (should not be rate limited)"
    log_info "-------------------------------------------------"
    
    # Simulate whitelisted IP by adding special header (this is just for testing)
    for i in $(seq 1 20); do
        curl -s -o /dev/null -w "%{http_code}|%{time_total}|%{response_header[X-RateLimit-Whitelisted]}\n" \
            -H "Authorization: Bearer ${AUTH_TOKEN}" \
            -H "X-Forwarded-For: 10.0.0.5" \
            "${BASE_URL}/projects?ts=$(date +%s%N)"
    done
    
    log_success "Rate limiting tests completed"
}

# Run the tests
run_tests