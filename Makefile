# Makefile for Rinna project
# -----------------------------

.PHONY: all build clean test lint run-api

# Default target
all: build test

# Build targets
build: build-java build-go

build-java:
	@echo "Building Java components..."
	mvn compile

build-go:
	@echo "Building Go API server..."
	cd api && go build -o ../bin/rinnasrv ./cmd/rinnasrv

# Test targets
test: test-java test-go

test-java:
	@echo "Running Java tests..."
	mvn test

test-go:
	@echo "Running Go tests..."
	cd api && go test ./...

# Clean targets
clean: clean-java clean-go

clean-java:
	@echo "Cleaning Java build artifacts..."
	mvn clean

clean-go:
	@echo "Cleaning Go build artifacts..."
	rm -f bin/rinnasrv

# Lint targets
lint: lint-java lint-go

lint-java:
	@echo "Running Java linters..."
	mvn checkstyle:check pmd:check spotbugs:check

lint-go:
	@echo "Running Go linters..."
	cd api && go vet ./...

# Run targets
run-api:
	@echo "Starting API server..."
	./bin/rinnasrv

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
	@echo "  clean       : Clean all build artifacts"
	@echo "  clean-java  : Clean Java build artifacts only"
	@echo "  clean-go    : Clean Go build artifacts only"
	@echo "  lint        : Run all linters"
	@echo "  lint-java   : Run Java linters only"
	@echo "  lint-go     : Run Go linters only"
	@echo "  run-api     : Start the API server"
	@echo "  help        : Show this help message"
