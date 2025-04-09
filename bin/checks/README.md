# Architecture Validation Scripts

This directory contains scripts for validating the Rinna project's architecture and enforcing code quality standards, with a focus on preventing circular dependencies and ensuring adherence to Clean Architecture principles.

## Available Scripts

### 1. `run-checks.sh`

Master script that executes all validation checks in this directory. This is typically the script you'll run directly.

```bash
# Run all validation checks
./bin/run-checks.sh
```

### 2. `dependency-validator.sh`

Checks for circular dependencies between modules in the project by examining POM files and ensuring that modules follow the correct dependency hierarchy.

```bash
# Check for circular dependencies
./bin/checks/dependency-validator.sh
```

### 3. `test-structure-validator.sh`

Validates that tests are properly structured, with correct test categories and appropriate tags.

```bash
# Verify test structure
./bin/checks/test-structure-validator.sh
```

### 4. `check-clean-architecture.sh`

Validates adherence to Clean Architecture principles, ensuring that inner layers don't depend on outer layers.

```bash
# Check Clean Architecture compliance
./bin/checks/check-clean-architecture.sh
```

## Integration Points

These scripts are integrated into the development workflow in several ways:

1. **Git Pre-Commit Hook**: Runs all validation checks before allowing commits
2. **Maven Build**: The `validate-architecture` profile integrates checks into the build process
3. **CI/CD Workflow**: GitHub Actions workflow runs these checks on pull requests and pushes
4. **Manual Execution**: Can be run manually during development

## Adding New Validation Checks

To add a new validation check:

1. Create a new script in this directory following the naming pattern: `check-[name].sh`
2. Implement your validation logic, making sure to:
   - Use appropriate exit codes (0 for success, non-zero for failure)
   - Provide clear error messages
   - Include proper error output formatting (using emoji prefixes for consistency)
3. Make the script executable: `chmod +x bin/checks/check-[name].sh`

The `run-checks.sh` script will automatically discover and run your new validation check.

## Expected Output

Successful validation will look like:

```
======================================================================
🔍 Running Rinna Project Validation Checks
======================================================================
----------------------------------------------------------------------
▶️ Running check: dependency-validator.sh
----------------------------------------------------------------------
✅ Check dependency-validator.sh passed
----------------------------------------------------------------------
▶️ Running check: test-structure-validator.sh
----------------------------------------------------------------------
✅ Check test-structure-validator.sh passed
----------------------------------------------------------------------
▶️ Running check: check-clean-architecture.sh
----------------------------------------------------------------------
✅ Check check-clean-architecture.sh passed
======================================================================
✅ All checks passed successfully!
======================================================================
```

Failed validation will show detailed error messages for the specific validation that failed.