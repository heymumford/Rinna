#!/bin/bash
# Run the report generation service in a container

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
REPO_ROOT="$(dirname "${SCRIPT_DIR}")"

# Default settings
REPORT_PORT=5001
LOG_LEVEL="info"
REPORTS_DIR="${REPO_ROOT}/reports"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --port)
      REPORT_PORT="$2"
      shift 2
      ;;
    --log-level)
      LOG_LEVEL="$2"
      shift 2
      ;;
    --reports-dir)
      REPORTS_DIR="$2"
      shift 2
      ;;
    --dev)
      DEV_MODE="true"
      shift
      ;;
    --help)
      echo "Usage: $0 [OPTIONS]"
      echo "Run the report generation service in a container"
      echo
      echo "Options:"
      echo "  --port PORT          Port to run the service on (default: 5001)"
      echo "  --log-level LEVEL    Log level (default: info)"
      echo "  --reports-dir DIR    Directory to store reports (default: ./reports)"
      echo "  --dev                Run in development mode"
      echo "  --help               Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Run '$0 --help' for usage information"
      exit 1
      ;;
  esac
done

# Create reports directory if it doesn't exist
mkdir -p "${REPORTS_DIR}"

# Set environment variables
export REPORT_PORT="${REPORT_PORT}"
export LOG_LEVEL="${LOG_LEVEL}"
export REPORTS_DIR="${REPORTS_DIR}"

# Get current user ID and group ID
USER_ID=$(id -u)
GROUP_ID=$(id -g)
export USER_ID
export GROUP_ID

echo "Starting report generation service..."
echo "Port: ${REPORT_PORT}"
echo "Reports directory: ${REPORTS_DIR}"
echo "Log level: ${LOG_LEVEL}"

cd "${REPO_ROOT}"

if [[ "${DEV_MODE}" == "true" ]]; then
  echo "Running in development mode"
  docker-compose -f python/docker-compose.yml run --service-ports \
    -e REPORTS_DIR=/app/reports \
    -e PORT="${REPORT_PORT}" \
    --profile reports \
    report-service
else
  echo "Running in production mode"
  docker-compose -f python/docker-compose.yml up -d \
    --force-recreate \
    --build \
    --profile reports \
    report-service
  
  # Display service logs
  docker-compose -f python/docker-compose.yml logs -f report-service
fi