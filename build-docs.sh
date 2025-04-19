#!/bin/bash

# Script to build and serve Rinna documentation
# This script should be run from the root directory of the Rinna project

set -e

# Ensure we're in the Rinna project directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "Building Rinna documentation from: $(pwd)"

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "ERROR: npm is not installed. Please install Node.js and npm first."
    exit 1
fi

# Check if package.json exists
if [ ! -f "package.json" ]; then
    echo "ERROR: package.json not found. Please run this script from the Rinna project root directory."
    exit 1
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Build documentation
echo "Building documentation..."
npm run docs

# Check if build succeeded
if [ ! -d "build/site" ]; then
    echo "ERROR: Documentation build failed. Please check the error messages above."
    exit 1
fi

# Start server
echo "Starting documentation server..."
echo "Documentation will be available at: http://localhost:3000"
echo "Press Ctrl+C to stop the server"

npm run docs:serve