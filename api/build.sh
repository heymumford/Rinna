#\!/bin/bash
set -euo pipefail

# Move to the project root directory
cd "$(dirname "$0")"

# Get version from project
VERSION=$(git describe --tags --always --dirty 2>/dev/null || echo "dev")
COMMIT_SHA=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
BUILD_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Build flags to set the version information
BUILD_FLAGS=(
  "-X" "github.com/heymumford/rinna/api/pkg/health.Version=${VERSION}"
  "-X" "github.com/heymumford/rinna/api/pkg/health.CommitSHA=${COMMIT_SHA}"
  "-X" "github.com/heymumford/rinna/api/pkg/health.BuildTime=${BUILD_TIME}"
)

echo "Building Rinna API server version ${VERSION} (${COMMIT_SHA}) at ${BUILD_TIME}"

# Build the API server
go build -ldflags="${BUILD_FLAGS[*]}" -o rinnasrv ./cmd/rinnasrv

echo "Build completed successfully"
