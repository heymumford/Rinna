#!/bin/bash
#
# Run CI checks locally with asynchronous OWASP dependency scanning
#

set -e

echo "📋 Running local CI checks with asynchronous OWASP dependency scanning..."
echo "⏳ OWASP dependency check will run in the background and log to dependency-check.log"

# Start the test and verification process with CI local profile
mvn clean verify -Dmaven.test.skip=false -P ci-local

echo -e "\n✅ Build and tests completed successfully!"
echo "🔍 OWASP dependency check is running in background. Check dependency-check.log for progress."
echo "📊 Results will be available in target/dependency-check/ when complete"