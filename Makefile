# Makefile for Rinna project
# -----------------------------

.PHONY: all build clean test lint run-api test-pyramid help

# Default target
all: build test

# Build targets
build:
	@echo "Building all components..."
	./bin/build.sh

build-java:
	@echo "Building Java components..."
	./bin/build.sh --components=java

build-go:
	@echo "Building Go API server..."
	./bin/build.sh --components=go

# Test targets
test:
	@echo "Running all tests..."
	./bin/build.sh --phase=test

test-java:
	@echo "Running Java tests..."
	./bin/build.sh --components=java --phase=test

test-go:
	@echo "Running Go tests..."
	./bin/build.sh --components=go --phase=test

test-pyramid:
	@echo "Generating test pyramid coverage report..."
	./bin/test-pyramid-coverage.sh

# Clean targets
clean:
	@echo "Cleaning all build artifacts..."
	./bin/build.sh --phase=initialize
	mvn clean

clean-java:
	@echo "Cleaning Java build artifacts..."
	mvn clean

clean-go:
	@echo "Cleaning Go build artifacts..."
	rm -f bin/rinnasrv

# Lint targets
lint:
	@echo "Running all linters..."
	./bin/build.sh --phase=validate

lint-java:
	@echo "Running Java linters..."
	./bin/build.sh --components=java --phase=validate

lint-go:
	@echo "Running Go linters..."
	./bin/build.sh --components=go --phase=validate

# Run targets
run-api:
	@echo "Starting API server..."
	./bin/rinnasrv

# Quick build (minimal checks)
quick:
	@echo "Running quick build (skipping tests and quality checks)..."
	./bin/build.sh --quick

# Help
help:
	@echo "Rinna Project Makefile"
	@echo "----------------------"
	@echo "Available targets:"
	@echo "  all         : Build and test everything (default)"
	@echo "  build       : Build all components"
	@echo "  build-java  : Build Java components only"
	@echo "  build-go    : Build Go API server only"
	@echo "  test        : Run all tests"
	@echo "  test-java   : Run Java tests only"
	@echo "  test-go     : Run Go tests only"
	@echo "  test-pyramid: Generate test pyramid coverage report"
	@echo "  clean       : Clean all build artifacts"
	@echo "  clean-java  : Clean Java build artifacts only"
	@echo "  clean-go    : Clean Go build artifacts only"
	@echo "  lint        : Run all linters"
	@echo "  lint-java   : Run Java linters only"
	@echo "  lint-go     : Run Go linters only"
	@echo "  run-api     : Start the API server"
	@echo "  quick       : Run quick build (skip tests and quality)"
	@echo "  help        : Show this help message"
	@echo ""
	@echo "For more advanced options, run: ./bin/build.sh --help"