#!/bin/bash
#
# Run CI checks locally with asynchronous OWASP dependency scanning
#

set -e

echo "📋 Running local CI checks with asynchronous OWASP dependency scanning..."
echo "⏳ OWASP dependency check will run in the background and log to dependency-check.log"

# Start OWASP dependency check in the background
mvn org.owasp:dependency-check-maven:check -Ddependency-check.skip=false \
    -Ddependency-check.failBuildOnCVSS=8 -Ddependency-check.nvdApiDelay=1000 \
    -Ddependency-check.format=HTML,JSON > dependency-check.log 2>&1 &

# Capture the OWASP scan PID
OWASP_PID=$!
echo "🔄 OWASP dependency scan started with PID: $OWASP_PID"

# Run the build and tests (without OWASP checks since they're running in background)
echo "🔄 Running build and tests..."
mvn clean verify -Dmaven.test.skip=false -Ddependency-check.skip=true

BUILD_RESULT=$?

echo
if [ $BUILD_RESULT -eq 0 ]; then
    echo "✅ Build and tests completed successfully!"
else
    echo "❌ Build or tests failed with exit code: $BUILD_RESULT"
fi

echo "🔍 OWASP dependency check is running in background with PID: $OWASP_PID"
echo "   Check dependency-check.log for progress."
echo "📊 Results will be available in target/dependency-check/ when complete"
echo
echo "📋 To check OWASP scan status:"
echo "   ps -p $OWASP_PID >/dev/null && echo 'Still running' || echo 'Completed'"