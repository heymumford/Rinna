# Rinna Configuration Files

This directory contains configuration files for various build quality tools used in the Rinna project.

## Checkstyle

Located in `config/checkstyle/`:

- `checkstyle.xml` - Main Checkstyle configuration with coding style rules
- `checkstyle-suppressions.xml` - Rules for suppressing Checkstyle checks in specific contexts (e.g., test files)

## SpotBugs

Located in `config/spotbugs/`:

- `spotbugs-exclude.xml` - Configuration for excluding certain bug patterns or specific classes from SpotBugs analysis

## SonarQube

Located in `config/sonar/`:

- `sonar-project.properties` - Configuration for SonarQube code quality analysis, including source paths and exclusion patterns

## Maven Integration

These configuration files are referenced in the root `pom.xml` file and are used during the build process:

- Checkstyle runs during the `validate` phase
- SpotBugs runs during the `verify` phase

## Custom Rules

If you need to modify the rules:

1. For code style changes, edit `config/checkstyle/checkstyle.xml`
2. To exclude files from style checks, edit `config/checkstyle/checkstyle-suppressions.xml`
3. To suppress specific bug warnings, edit `config/spotbugs/spotbugs-exclude.xml`

After making changes, run `mvn validate` to verify your changes are correctly formatted and detected.