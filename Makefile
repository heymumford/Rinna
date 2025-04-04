.PHONY: clean compile package test verify run coverage lint check-style spotbugs pmd

# Default target
all: clean package

# Clean the project
clean:
	@echo "Cleaning the project..."
	@mvn clean

# Compile the project
compile:
	@echo "Compiling the project..."
	@mvn compile

# Package the project
package:
	@echo "Packaging the project..."
	@mvn package

# Run tests
test:
	@echo "Running tests..."
	@mvn test

# Run verification (including tests, checkstyle, spotbugs, pmd)
verify:
	@echo "Running verification..."
	@mvn verify

# Run the application
run:
	@echo "Running the application..."
	@java -jar rinna-core/target/rinna-cli.jar

# Generate test coverage report
coverage:
	@echo "Generating coverage report..."
	@mvn test jacoco:report
	@echo "Coverage report generated in target/site/jacoco/"

# Run checkstyle
check-style:
	@echo "Running checkstyle..."
	@mvn checkstyle:check

# Run spotbugs
spotbugs:
	@echo "Running spotbugs..."
	@mvn spotbugs:check

# Run pmd
pmd:
	@echo "Running PMD..."
	@mvn pmd:check

# Run all quality checks
quality-checks: check-style spotbugs pmd

# Help
help:
	@echo "Available targets:"
	@echo "  all           : Clean and package the project (default)"
	@echo "  clean         : Clean the project"
	@echo "  compile       : Compile the project"
	@echo "  package       : Package the project"
	@echo "  test          : Run tests"
	@echo "  verify        : Run verification (tests + quality checks)"
	@echo "  run           : Run the application"
	@echo "  coverage      : Generate test coverage report"
	@echo "  check-style   : Run checkstyle"
	@echo "  spotbugs      : Run spotbugs"
	@echo "  pmd           : Run PMD"
	@echo "  quality-checks: Run all quality checks (checkstyle, spotbugs, pmd)"