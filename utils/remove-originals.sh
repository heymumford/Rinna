#!/bin/bash
# Script to remove original files after cleanup

set -e
echo "Removing original files that have been moved..."

# Base directory
BASE_DIR="/home/emumford/NativeLinuxProjects/Rinna"
cd $BASE_DIR

# Remove test and coverage files that have been copied
echo "Removing test and coverage files..."
[ -f .coverage ] && rm -f .coverage
[ -f .coveragerc ] && rm -f .coveragerc
[ -f dependency-check.log ] && rm -f dependency-check.log

# Remove temporary and cache directories
echo "Removing temporary directories..."
[ -d tmp ] && rm -rf tmp
[ -d temp-cleanup ] && rm -rf temp-cleanup
[ -d docker-cache ] && rm -rf docker-cache
[ -d test-output ] && rm -rf test-output

# Clean up utility directories
echo "Cleaning up utility directories..."
if [ -d util ] && [ -d utils ]; then
  rm -rf util
fi

echo "Removal of original files completed successfully!"