# Dependency Management

This document describes how dependencies are managed in the Rinna project, with a focus on preventing circular dependencies and ensuring proper architecture validation.

## Core Principles

1. **Clean Architecture Compliance**: The project follows Clean Architecture principles, with dependencies pointing inward:
   - Domain layer (entities) has no dependencies on outer layers
   - Use case layer depends only on the domain layer
   - Adapter layer depends on use case and domain layers
   - Framework/infrastructure depends on all inner layers

2. **Module Structure**:
   - `rinna-core`: Core domain model and use cases (innermost layers)
   - `rinna-cli`: Command-line interface, depends on rinna-core
   - `api`: REST API implementation, depends on rinna-core 
   - Parent project: Coordinates builds but does not contain business logic

3. **Dependency Flow**: Dependencies must flow in one direction to prevent circular dependencies:
   ```
   api → rinna-cli → rinna-core
   ```

## Validation Mechanisms

### 1. Automated Validation Scripts

The project includes several validation scripts to ensure architectural integrity:

- **dependency-validator.sh**: Checks for circular dependencies in module POM files
- **check-clean-architecture.sh**: Validates adherence to Clean Architecture principles
- **test-structure-validator.sh**: Ensures tests are properly organized and tagged
- **run-checks.sh**: Master script that runs all validation checks

### 2. Git Hooks

A pre-commit hook runs all architecture validation checks before allowing commits:

```bash
# Install the pre-commit hook
cp .git/hooks/pre-commit.sample .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

### 3. Maven Profile

The `validate-architecture` Maven profile runs architecture checks during the validate phase:

```bash
# Run Maven build with architecture validation
mvn clean verify -P validate-architecture
```

### 4. CI/CD Integration

Architecture validation is integrated into the CI/CD workflow, running checks on:
- Pull requests to main/develop branches
- Direct pushes to main/develop branches
- Manual workflow dispatch

## Common Issues and Solutions

### Circular Dependencies

**Problem**: Module A depends on Module B, and Module B depends on Module A.

**Solution**:
1. Identify the circular dependency using `./bin/checks/dependency-validator.sh`
2. Extract common code into a shared module or move it to a more appropriate layer
3. Apply dependency inversion principle to break the cycle

### Layer Violations

**Problem**: Code in an inner layer imports from an outer layer (e.g., domain using adapter).

**Solution**:
1. Use `./bin/checks/check-clean-architecture.sh` to identify the violation
2. Apply dependency inversion to maintain the correct dependency flow
3. Move shared code to the appropriate layer

## Best Practices

1. **New Modules**: When creating a new module, ensure you:
   - Define clear layer boundaries
   - Add necessary checks to validation scripts
   - Update CI/CD workflow

2. **Test Dependencies**: All test dependencies should:
   - Be marked with `<scope>test</scope>`
   - Not create circular dependencies

3. **Regular Validation**: Run validation checks regularly:
   ```bash
   ./bin/run-checks.sh
   ```

4. **Pre-release Check**: Before releasing, verify all dependencies:
   ```bash
   mvn dependency:tree
   mvn dependency:analyze
   ```

## References

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Maven Dependency Management](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)