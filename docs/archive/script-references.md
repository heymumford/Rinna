# Script Reference Migration Guide

This document lists all files containing references to scripts that have been renamed.
Use this to help update references to the new script naming convention.

## build.sh → rin-build-main-all.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:          chmod +x api/build.sh 2>/dev/null || true
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/rin-ci.yml:          chmod +x api/build.sh || true
/home/emumford/NativeLinuxProjects/Rinna/backup/version-sync-20250409-161456/README.md:bin/increment-build.sh
/home/emumford/NativeLinuxProjects/Rinna/backup/version-sync-20250409-161456/README.md:bin/increment-build.sh set 500
/home/emumford/NativeLinuxProjects/Rinna/bin/README-VERSION-MANAGER.md:- `bin/increment-build.sh`: Increments the build number
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:- Runs automatically after every build via integration with build.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/build.sh:  # Set quick build shortcuts
/home/emumford/NativeLinuxProjects/Rinna/bin/build.sh:# build.sh - Unified Build Orchestrator for Rinna
/home/emumford/NativeLinuxProjects/Rinna/bin/cleanup-legacy-files.sh:  echo -e "  ${CYAN}./bin/build.sh --skip-quality${NC}"
/home/emumford/NativeLinuxProjects/Rinna/bin/increment-build.sh:      echo "Usage: increment-build.sh [set NUMBER] [--no-commit]"
/home/emumford/NativeLinuxProjects/Rinna/bin/increment-build.sh:# increment-build.sh - Increment the build number in version.properties
/home/emumford/NativeLinuxProjects/Rinna/bin/install-git-hooks.sh:echo "      Option 2: For automated CI environments, you can call bin/increment-build.sh directly."
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-build-main-all.sh:  # Set quick build shortcuts
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-build-main-all.sh:# build.sh - Unified Build Orchestrator for Rinna
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-test-run-all.sh:  ./build.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-test-run-all.sh:if [ -f "./build.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-util-version-increment.sh:      echo "Usage: increment-build.sh [set NUMBER] [--no-commit]"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-util-version-increment.sh:# increment-build.sh - Increment the build number in version.properties
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-tests.sh:  ./build.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-tests.sh:if [ -f "./build.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/bin/test/performance/test_perf_build_pipeline.sh:    # The incremental build should be significantly faster
/home/emumford/NativeLinuxProjects/Rinna/docs/development/quality-tools.md:1. Run the full build once: `./bin/build.sh`
/home/emumford/NativeLinuxProjects/Rinna/docs/development/version-management.md:- `bin/increment-build.sh`: Increments the build number
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/BUILD_NUMBER_MANAGEMENT.md:2. A dedicated script (`bin/increment-build.sh`) for manual build number management
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/BUILD_NUMBER_MANAGEMENT.md:bin/increment-build.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/BUILD_NUMBER_MANAGEMENT.md:bin/increment-build.sh set <number>
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/SWAGGER_IMPLEMENTATION_SUMMARY.md:- Integrated with the build system in `bin/build.sh`
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/SWAGGER_IMPLEMENTATION_SUMMARY.md:./bin/build.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/VERSION_MANAGEMENT_REFACTORING.md:2. **increment-build.sh**: Updated to use version-manager.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/project-docs/CLAUDE.md:- Build: `./bin/build.sh` or `mvn clean install`
/home/emumford/NativeLinuxProjects/Rinna/docs/project-docs/CLEANUP.md:  - build.sh → utils/build.sh (with symlink)
/home/emumford/NativeLinuxProjects/Rinna/docs/project-docs/CLEANUP.md:build.sh -> utils/build.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## build.sh → rin-build-main-all.sh
/home/emumford/NativeLinuxProjects/Rinna/src/test/java/org/rinna/bdd/AdminMavenIntegrationSteps.java:    @Then("the build should complete successfully")
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-tests.sh:  ./build.sh
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-tests.sh:if [ -f "./build.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/utils/build.sh:# build.sh - Main entry point for Rinna build system
```

## rinna-container.sh → rin-infra-container-all.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/build-all-in-one.sh:echo -e "${CYAN}  ./bin/rinna-container.sh --zero-install start${NC}"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:  ./bin/rinna-container.sh --docker --wsl start   # Start with Docker on WSL
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:  ./bin/rinna-container.sh --type=api start       # Start only API server
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:  ./bin/rinna-container.sh --type=python logs     # View Python container logs
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:  ./bin/rinna-container.sh --zero-install start   # Use prebuilt image
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:  ./bin/rinna-container.sh logs                   # View all container logs
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:  ./bin/rinna-container.sh shell                  # Open shell in the development container
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:  ./bin/rinna-container.sh start                  # Start all containers
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:# Generated by rinna-container.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:# rinna-container.sh - Universal container management script for Rinna
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:${BLUE}rinna-container.sh${NC} - Universal container management for Rinna
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-infra-container-all.sh:Usage: rinna-container.sh [options] ACTION
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:  ./bin/rinna-container.sh --docker --wsl start   # Start with Docker on WSL
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:  ./bin/rinna-container.sh --type=api start       # Start only API server
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:  ./bin/rinna-container.sh --type=python logs     # View Python container logs
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:  ./bin/rinna-container.sh --zero-install start   # Use prebuilt image
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:  ./bin/rinna-container.sh logs                   # View all container logs
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:  ./bin/rinna-container.sh shell                  # Open shell in the development container
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:  ./bin/rinna-container.sh start                  # Start all containers
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:# Generated by rinna-container.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:# rinna-container.sh - Universal container management script for Rinna
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:${BLUE}rinna-container.sh${NC} - Universal container management for Rinna
/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-container.sh:Usage: rinna-container.sh [options] ACTION
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --docker start      # Use Docker explicitly
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --linux start       # Configure for native Linux
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --podman start      # Use Podman explicitly
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --type=api start    # Start API server only
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --type=dev start    # Start development environment
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --type=java start   # Start Java service only 
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --type=python build
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --type=python start # Start Python service only
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --type=test start
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --windows start     # Configure for Windows Git Bash
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --wsl start         # Configure for WSL
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh --zero-install start # Use prebuilt all-in-one image
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh build
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh clean               # Remove containers and optionally data
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh health
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh health              # Check container health
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh logs
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh logs                # View logs from all containers
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh restart             # Restart all containers
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh shell
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh shell
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh start
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh start
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh start
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh start
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh status
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh status              # Check container status
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:./bin/rinna-container.sh stop                # Stop all containers
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:The `rinna-container.sh` script provides a consistent interface across all platforms:
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:chmod +x bin/rinna-container.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:chmod +x bin/rinna-container.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:chmod +x bin/rinna-container.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/cross-platform-container-setup.md:chmod +x bin/rinna-container.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/zero-install-container.md:./bin/rinna-container.sh --zero-install health
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/zero-install-container.md:./bin/rinna-container.sh --zero-install shell
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/zero-install-container.md:./bin/rinna-container.sh --zero-install start
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/zero-install-container.md:./bin/rinna-container.sh --zero-install status
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/zero-install-container.md:./bin/rinna-container.sh --zero-install stop
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/zero-install-container.md:If you've cloned the repository, you can use the included `rinna-container.sh` script:
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## rinna-container.sh → rin-infra-container-all.sh
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:  ./bin/rinna-container.sh --docker --wsl start   # Start with Docker on WSL
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:  ./bin/rinna-container.sh --type=api start       # Start only API server
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:  ./bin/rinna-container.sh --type=python logs     # View Python container logs
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:  ./bin/rinna-container.sh --zero-install start   # Use prebuilt image
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:  ./bin/rinna-container.sh logs                   # View all container logs
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:  ./bin/rinna-container.sh shell                  # Open shell in the development container
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:  ./bin/rinna-container.sh start                  # Start all containers
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:# Generated by rinna-container.sh
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:# rinna-container.sh - Universal container management script for Rinna
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:${BLUE}rinna-container.sh${NC} - Universal container management for Rinna
/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-container.sh:Usage: rinna-container.sh [options] ACTION
```

## test-rate-limiting.sh → rin-api-test-rate-limiting.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## test-rate-limiting.sh → rin-api-test-rate-limiting.sh
```

## test-oauth-integration.sh → rin-api-test-oauth.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## test-oauth-integration.sh → rin-api-test-oauth.sh
```

## test-failure-notify.sh → rin-ci-notify-test-failures.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:          ./bin/test-failure-notify.sh --reports-dir target/surefire-reports --notify-method slack --slack-webhook ${{ secrets.SLACK_WEBHOOK_URL }} --ci
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:          ./bin/test-failure-notify.sh --reports-dir target/surefire-reports --notify-method slack --slack-webhook ${{ secrets.SLACK_WEBHOOK_URL }} --ci --summary-only
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:          ./bin/test-failure-notify.sh --reports-dir target/surefire-reports --notify-method slack --slack-webhook ${{ secrets.SLACK_WEBHOOK_URL }} --ci --summary-only
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:          ./bin/test-failure-notify.sh --reports-dir target/surefire-reports --notify-method slack --slack-webhook ${{ secrets.SLACK_WEBHOOK_URL }} --ci --threshold 1
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:          chmod +x bin/test-failure-notify.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:        run: chmod +x bin/test-failure-notify.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:        run: chmod +x bin/test-failure-notify.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:        run: chmod +x bin/test-failure-notify.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:        run: chmod +x bin/test-failure-notify.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/test-failure-report.yml:          ./bin/test-failure-notify.sh \
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/test-failure-report.yml:          ./bin/test-failure-notify.sh \
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/test-failure-report.yml:        run: chmod +x bin/test-failure-notify.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/test-failure-report.yml:        run: chmod +x bin/test-failure-notify.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/ci/enhance-ci-pipeline.sh:    ensure_executable "bin/test-failure-notify.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/ci/enhance-ci-pipeline.sh:echo "   - Notification script: bin/test-failure-notify.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-ci-notify-test-failures.sh:            echo "Usage: ./test-failure-notify.sh [OPTIONS]"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-ci-notify-test-failures.sh:#   ./test-failure-notify.sh [OPTIONS]
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-ci-notify-test-failures.sh:# test-failure-notify.sh - Test failure notification system for Rinna
/home/emumford/NativeLinuxProjects/Rinna/bin/test-failure-notify.sh:            echo "Usage: ./test-failure-notify.sh [OPTIONS]"
/home/emumford/NativeLinuxProjects/Rinna/bin/test-failure-notify.sh:#   ./test-failure-notify.sh [OPTIONS]
/home/emumford/NativeLinuxProjects/Rinna/bin/test-failure-notify.sh:# test-failure-notify.sh - Test failure notification system for Rinna
/home/emumford/NativeLinuxProjects/Rinna/docs/testing/TEST_FAILURE_NOTIFICATION.md:./bin/test-failure-notify.sh [OPTIONS]
/home/emumford/NativeLinuxProjects/Rinna/docs/testing/TEST_FAILURE_NOTIFICATION.md:The core component is the `bin/test-failure-notify.sh` script, which:
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## test-failure-notify.sh → rin-ci-notify-test-failures.sh
```

## quality-thresholds.sh → rin-quality-config-thresholds.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/cli-module-build-verification.yml:          ./bin/quality-thresholds.sh --mode=$QUALITY_MODE --coverage=$COVERAGE_THRESHOLD --check
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/cli-module-build-verification.yml:          ./bin/quality-thresholds.sh --report
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/cli-module-build-verification.yml:          chmod +x bin/quality-thresholds.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/cli-module-build-verification.yml:          chmod +x bin/quality-thresholds.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-config-thresholds.sh:  ./bin/quality-thresholds.sh --check       # Check against current thresholds
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-config-thresholds.sh:  ./bin/quality-thresholds.sh --coverage=80 # Set coverage threshold to 80%
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-config-thresholds.sh:  ./bin/quality-thresholds.sh --mode=ci     # Use CI quality thresholds
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-config-thresholds.sh:  ./bin/quality-thresholds.sh --report      # Generate quality metrics report
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-config-thresholds.sh:# Generated by quality-thresholds.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-config-thresholds.sh:# quality-thresholds.sh - Establish and enforce code quality thresholds
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-config-thresholds.sh:${BLUE}quality-thresholds.sh${NC} - Establish and enforce code quality thresholds
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-config-thresholds.sh:Usage: quality-thresholds.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-thresholds.sh:  ./bin/quality-thresholds.sh --check       # Check against current thresholds
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-thresholds.sh:  ./bin/quality-thresholds.sh --coverage=80 # Set coverage threshold to 80%
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-thresholds.sh:  ./bin/quality-thresholds.sh --mode=ci     # Use CI quality thresholds
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-thresholds.sh:  ./bin/quality-thresholds.sh --report      # Generate quality metrics report
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-thresholds.sh:# Generated by quality-thresholds.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-thresholds.sh:# quality-thresholds.sh - Establish and enforce code quality thresholds
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-thresholds.sh:${BLUE}quality-thresholds.sh${NC} - Establish and enforce code quality thresholds
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-thresholds.sh:Usage: quality-thresholds.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## quality-thresholds.sh → rin-quality-config-thresholds.sh
```

## increment-build.sh → rin-util-version-increment.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/backup/version-sync-20250409-161456/README.md:bin/increment-build.sh
/home/emumford/NativeLinuxProjects/Rinna/backup/version-sync-20250409-161456/README.md:bin/increment-build.sh set 500
/home/emumford/NativeLinuxProjects/Rinna/bin/README-VERSION-MANAGER.md:- `bin/increment-build.sh`: Increments the build number
/home/emumford/NativeLinuxProjects/Rinna/bin/increment-build.sh:      echo "Usage: increment-build.sh [set NUMBER] [--no-commit]"
/home/emumford/NativeLinuxProjects/Rinna/bin/increment-build.sh:# increment-build.sh - Increment the build number in version.properties
/home/emumford/NativeLinuxProjects/Rinna/bin/install-git-hooks.sh:echo "      Option 2: For automated CI environments, you can call bin/increment-build.sh directly."
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-util-version-increment.sh:      echo "Usage: increment-build.sh [set NUMBER] [--no-commit]"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-util-version-increment.sh:# increment-build.sh - Increment the build number in version.properties
/home/emumford/NativeLinuxProjects/Rinna/docs/development/version-management.md:- `bin/increment-build.sh`: Increments the build number
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/BUILD_NUMBER_MANAGEMENT.md:2. A dedicated script (`bin/increment-build.sh`) for manual build number management
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/BUILD_NUMBER_MANAGEMENT.md:bin/increment-build.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/BUILD_NUMBER_MANAGEMENT.md:bin/increment-build.sh set <number>
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/VERSION_MANAGEMENT_REFACTORING.md:2. **increment-build.sh**: Updated to use version-manager.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## increment-build.sh → rin-util-version-increment.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/backup/version-sync-20250409-161456/README.md:bin/increment-build.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/backup/version-sync-20250409-161456/README.md:bin/increment-build.sh set 500
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/README-VERSION-MANAGER.md:- `bin/increment-build.sh`: Increments the build number
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/increment-build.sh:      echo "Usage: increment-build.sh [set NUMBER] [--no-commit]"
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/increment-build.sh:# increment-build.sh - Increment the build number in version.properties
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/install-git-hooks.sh:echo "      Option 2: For automated CI environments, you can call bin/increment-build.sh directly."
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-util-version-increment.sh:      echo "Usage: increment-build.sh [set NUMBER] [--no-commit]"
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-util-version-increment.sh:# increment-build.sh - Increment the build number in version.properties
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/docs/development/version-management.md:- `bin/increment-build.sh`: Increments the build number
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/BUILD_NUMBER_MANAGEMENT.md:2. A dedicated script (`bin/increment-build.sh`) for manual build number management
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/BUILD_NUMBER_MANAGEMENT.md:bin/increment-build.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/BUILD_NUMBER_MANAGEMENT.md:bin/increment-build.sh set <number>
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/VERSION_MANAGEMENT_REFACTORING.md:2. **increment-build.sh**: Updated to use version-manager.sh
```

## update-versions.sh → rin-util-version-update.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/README-VERSION-MANAGER.md:- `bin/update-versions.sh`: Updates all version references 
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-util-version-update.sh:# update-versions.sh - Backward compatibility wrapper for version-manager.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/update-versions.sh:# update-versions.sh - Backward compatibility wrapper for version-manager.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/development/version-management.md:- `bin/update-versions.sh`: Updates all version references
/home/emumford/NativeLinuxProjects/Rinna/docs/implementation/VERSION_MANAGEMENT_REFACTORING.md:1. **update-versions.sh**: Updated to use version-manager.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## update-versions.sh → rin-util-version-update.sh
```

## setup-hooks.sh → rin-git-setup-hooks.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## setup-hooks.sh → rin-git-setup-hooks.sh
```

## run-security-tests.sh → rin-security-test-all.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## run-security-tests.sh → rin-security-test-all.sh
```

## run-checks.sh → rin-quality-check-all.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/architecture-validation.yml:          ./bin/run-checks.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/architecture-validation.yml:        chmod +x ./bin/run-checks.sh || true
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/architecture-validation.yml:        if [ -f "./bin/run-checks.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/backup/pom-backups-20250408123241/pom.xml:                  <executable>${session.executionRootDirectory}/bin/run-checks.sh</executable>
/home/emumford/NativeLinuxProjects/Rinna/backup/version-20250409-102330/pom.xml:                  <executable>${session.executionRootDirectory}/bin/run-checks.sh</executable>
/home/emumford/NativeLinuxProjects/Rinna/backup/version-sync-20250409-161456/pom.xml:                  <executable>${session.executionRootDirectory}/bin/run-checks.sh</executable>
/home/emumford/NativeLinuxProjects/Rinna/bin/build.sh:    run_formatted "$PROJECT_ROOT/bin/run-checks.sh" "Validating architecture"
/home/emumford/NativeLinuxProjects/Rinna/bin/build.sh:  if [[ -f "$PROJECT_ROOT/bin/run-checks.sh" ]]; then
/home/emumford/NativeLinuxProjects/Rinna/bin/checks/README.md:### 1. `run-checks.sh`
/home/emumford/NativeLinuxProjects/Rinna/bin/checks/README.md:./bin/run-checks.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/checks/README.md:The `run-checks.sh` script will automatically discover and run your new validation check.
/home/emumford/NativeLinuxProjects/Rinna/bin/ci/enhance-ci-pipeline.sh:    ensure_executable "bin/run-checks.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/maven-validate-architecture.sh:"${PROJECT_ROOT}/bin/run-checks.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-build-main-all.sh:    run_formatted "$PROJECT_ROOT/bin/run-checks.sh" "Validating architecture"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-build-main-all.sh:  if [[ -f "$PROJECT_ROOT/bin/run-checks.sh" ]]; then
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-git-setup-hooks.sh:${PROJECT_ROOT}/bin/run-checks.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/setup-hooks.sh:${PROJECT_ROOT}/bin/run-checks.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/FOLDERS.md:./bin/run-checks.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/development/dependency-management.md:   ./bin/run-checks.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/development/dependency-management.md:- **run-checks.sh**: Master script that runs all validation checks
/home/emumford/NativeLinuxProjects/Rinna/docs/project-docs/CLAUDE.md:- CI verification: `./bin/run-checks.sh` (validates architecture and dependencies)
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/rin-quick-reference.md:| `bin/run-checks.sh` | Architecture validation | `./bin/run-checks.sh` |
/home/emumford/NativeLinuxProjects/Rinna/docs/user-guide/rin-quick-reference.md:| `run-checks.sh` | All validation checks | `./bin/run-checks.sh` |
/home/emumford/NativeLinuxProjects/Rinna/pom.xml:                  <executable>${session.executionRootDirectory}/bin/run-checks.sh</executable>
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## run-checks.sh → rin-quality-check-all.sh
```

## rinna-tests.sh → rin-test-run-all.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/ci.yml:          chmod +x bin/rinna-tests.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/rin-ci.yml:          ./bin/rinna-tests.sh
/home/emumford/NativeLinuxProjects/Rinna/.github/workflows/rin-ci.yml:          chmod +x bin/rinna-tests.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/development/testing.md:3. The `rinna-tests.sh` script for cross-language testing
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## rinna-tests.sh → rin-test-run-all.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-tests.sh:  ./build.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/rinna-tests.sh:if [ -f "./build.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-tests.sh:  ./build.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/target/distribution/bin/rinna-tests.sh:if [ -f "./build.sh" ]; then
```

## checkstyle.sh → rin-quality-check-java-style.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:      run_check "checkstyle.sh" "checkstyle" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-style.sh:  ./bin/quality-tools/checkstyle.sh                      # Run on all code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-style.sh:  ./bin/quality-tools/checkstyle.sh --file=src/main/java/org/rinna/Rinna.java
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-style.sh:  ./bin/quality-tools/checkstyle.sh --module=rinna-cli   # Run on CLI module only
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-style.sh:# checkstyle.sh - Run Checkstyle on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-style.sh:${BLUE}checkstyle.sh${NC} - Run Checkstyle on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-style.sh:Usage: checkstyle.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/checkstyle.sh:  ./bin/quality-tools/checkstyle.sh                      # Run on all code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/checkstyle.sh:  ./bin/quality-tools/checkstyle.sh --file=src/main/java/org/rinna/Rinna.java
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/checkstyle.sh:  ./bin/quality-tools/checkstyle.sh --module=rinna-cli   # Run on CLI module only
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/checkstyle.sh:# checkstyle.sh - Run Checkstyle on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/checkstyle.sh:${BLUE}checkstyle.sh${NC} - Run Checkstyle on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/checkstyle.sh:Usage: checkstyle.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:      run_check "checkstyle.sh" "checkstyle" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/logs/temp/quality-tools-summary.md:   - `checkstyle.sh` - For code style checks
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## checkstyle.sh → rin-quality-check-java-style.sh
```

## count-warnings.sh → rin-quality-analyze-warnings.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-analyze-warnings.sh:# count-warnings.sh - Count checkstyle warnings
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/count-warnings.sh:# count-warnings.sh - Count checkstyle warnings
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## count-warnings.sh → rin-quality-analyze-warnings.sh
```

## enforcer.sh → rin-quality-check-maven-rules.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:      run_check "enforcer.sh" "enforcer" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-maven-rules.sh:  ./bin/quality-tools/enforcer.sh                                  # Run all rules on all modules
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-maven-rules.sh:  ./bin/quality-tools/enforcer.sh --module=rinna-cli               # Run all rules on CLI module
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-maven-rules.sh:  ./bin/quality-tools/enforcer.sh --rule=dependencyConvergence     # Check dependency versions
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-maven-rules.sh:# enforcer.sh - Run Maven Enforcer Rules
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-maven-rules.sh:${BLUE}enforcer.sh${NC} - Run Maven Enforcer Rules
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-maven-rules.sh:Usage: enforcer.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/enforcer.sh:  ./bin/quality-tools/enforcer.sh                                  # Run all rules on all modules
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/enforcer.sh:  ./bin/quality-tools/enforcer.sh --module=rinna-cli               # Run all rules on CLI module
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/enforcer.sh:  ./bin/quality-tools/enforcer.sh --rule=dependencyConvergence     # Check dependency versions
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/enforcer.sh:# enforcer.sh - Run Maven Enforcer Rules
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/enforcer.sh:${BLUE}enforcer.sh${NC} - Run Maven Enforcer Rules
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/enforcer.sh:Usage: enforcer.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:      run_check "enforcer.sh" "enforcer" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/logs/temp/quality-tools-summary.md:   - `enforcer.sh` - For dependency rule enforcement
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## enforcer.sh → rin-quality-check-maven-rules.sh
```

## fix-imports.sh → rin-quality-fix-java-imports.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/migration/fix-imports.sh:# fix-imports.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-fix-java-imports.sh:  ./bin/quality-tools/fix-imports.sh                # Just run Spotless on all modules
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-fix-java-imports.sh:  ./bin/quality-tools/fix-imports.sh --add-plugin   # Add Spotless plugin and fix all imports
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-fix-java-imports.sh:  ./bin/quality-tools/fix-imports.sh --module=rinna-cli  # Run only on CLI module
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-fix-java-imports.sh:# fix-imports.sh - Automatically fix import ordering in Java files
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-fix-java-imports.sh:${BLUE}fix-imports.sh${NC} - Automatically fix import ordering in Java files
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-fix-java-imports.sh:Usage: fix-imports.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/fix-imports.sh:  ./bin/quality-tools/fix-imports.sh                # Just run Spotless on all modules
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/fix-imports.sh:  ./bin/quality-tools/fix-imports.sh --add-plugin   # Add Spotless plugin and fix all imports
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/fix-imports.sh:  ./bin/quality-tools/fix-imports.sh --module=rinna-cli  # Run only on CLI module
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/fix-imports.sh:# fix-imports.sh - Automatically fix import ordering in Java files
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/fix-imports.sh:${BLUE}fix-imports.sh${NC} - Automatically fix import ordering in Java files
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/fix-imports.sh:Usage: fix-imports.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/docs/FOLDERS.md:./bin/fix-imports.sh
/home/emumford/NativeLinuxProjects/Rinna/docs/architecture/decisions/0004-refactor-package-structure-to-align-with-clean-architecture.md:- `bin/migration/fix-imports.sh`: Updates import statements
/home/emumford/NativeLinuxProjects/Rinna/docs/development/codebase-organization.md:3. `bin/fix-imports.sh`: Fix imports after moving classes
/home/emumford/NativeLinuxProjects/Rinna/docs/development/package-structure.md:- `bin/migration/fix-imports.sh`: Updates import statements
/home/emumford/NativeLinuxProjects/Rinna/logs/temp/quality-tools-summary.md:   - `fix-imports.sh` - For automatic import ordering fix
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## fix-imports.sh → rin-quality-fix-java-imports.sh
```

## owasp.sh → rin-security-check-dependencies.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:      run_check "owasp.sh" "owasp" "$MODULE_ARG --quick"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-security-check-dependencies.sh:  ./bin/quality-tools/owasp.sh                 # Run on all code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-security-check-dependencies.sh:  ./bin/quality-tools/owasp.sh --module=rinna-cli     # Run on CLI module only
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-security-check-dependencies.sh:  ./bin/quality-tools/owasp.sh --quick         # Quick scan for faster results
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-security-check-dependencies.sh:  ./bin/quality-tools/owasp.sh --update-only   # Just update vulnerability database
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-security-check-dependencies.sh:# owasp.sh - Run OWASP Dependency Check
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-security-check-dependencies.sh:${BLUE}owasp.sh${NC} - Run OWASP Dependency Check for security vulnerability scanning
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-security-check-dependencies.sh:Usage: owasp.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/owasp.sh:  ./bin/quality-tools/owasp.sh                 # Run on all code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/owasp.sh:  ./bin/quality-tools/owasp.sh --module=rinna-cli     # Run on CLI module only
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/owasp.sh:  ./bin/quality-tools/owasp.sh --quick         # Quick scan for faster results
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/owasp.sh:  ./bin/quality-tools/owasp.sh --update-only   # Just update vulnerability database
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/owasp.sh:# owasp.sh - Run OWASP Dependency Check
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/owasp.sh:${BLUE}owasp.sh${NC} - Run OWASP Dependency Check for security vulnerability scanning
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/owasp.sh:Usage: owasp.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:      run_check "owasp.sh" "owasp" "$MODULE_ARG --quick"
/home/emumford/NativeLinuxProjects/Rinna/logs/temp/quality-tools-summary.md:   - `owasp.sh` - For security vulnerability scanning
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## owasp.sh → rin-security-check-dependencies.sh
```

## pmd.sh → rin-quality-check-java-pmd.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:      run_check "pmd.sh" "pmd" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-pmd.sh:  ./bin/quality-tools/pmd.sh                       # Run on all code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-pmd.sh:  ./bin/quality-tools/pmd.sh --category=security   # Run only security rules
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-pmd.sh:  ./bin/quality-tools/pmd.sh --module=rinna-cli    # Run on CLI module only
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-pmd.sh:# pmd.sh - Run PMD on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-pmd.sh:${BLUE}pmd.sh${NC} - Run PMD on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-pmd.sh:Usage: pmd.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/pmd.sh:  ./bin/quality-tools/pmd.sh                       # Run on all code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/pmd.sh:  ./bin/quality-tools/pmd.sh --category=security   # Run only security rules
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/pmd.sh:  ./bin/quality-tools/pmd.sh --module=rinna-cli    # Run on CLI module only
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/pmd.sh:# pmd.sh - Run PMD on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/pmd.sh:${BLUE}pmd.sh${NC} - Run PMD on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/pmd.sh:Usage: pmd.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:      run_check "pmd.sh" "pmd" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/logs/temp/quality-tools-summary.md:   - `pmd.sh` - For static code analysis
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## pmd.sh → rin-quality-check-java-pmd.sh
```

## run-all.sh → rin-quality-check-all.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:  ./bin/quality-tools/run-all.sh                               # Run all checks
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:  ./bin/quality-tools/run-all.sh --module=rinna-cli            # Run checks on CLI module
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:  ./bin/quality-tools/run-all.sh --skip=owasp,spotbugs         # Skip OWASP and SpotBugs
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:# run-all.sh - Run all quality checks sequentially
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:${BLUE}run-all.sh${NC} - Run all quality checks sequentially
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:Usage: run-all.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:  ./bin/quality-tools/run-all.sh                               # Run all checks
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:  ./bin/quality-tools/run-all.sh --module=rinna-cli            # Run checks on CLI module
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:  ./bin/quality-tools/run-all.sh --skip=owasp,spotbugs         # Skip OWASP and SpotBugs
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:# run-all.sh - Run all quality checks sequentially
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:${BLUE}run-all.sh${NC} - Run all quality checks sequentially
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:Usage: run-all.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## rinna-tests.sh → rin-test-run-all.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## run-all.sh → rin-quality-check-all.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-test-run-all.sh:  ./build.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-test-run-all.sh:if [ -f "./build.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:      run_check "checkstyle.sh" "checkstyle" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:      run_check "enforcer.sh" "enforcer" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:      run_check "owasp.sh" "owasp" "$MODULE_ARG --quick"
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:      run_check "pmd.sh" "pmd" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## rinna-tests.sh → rin-test-run-all.sh
```

## spotbugs.sh → rin-quality-check-java-bugs.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-all.sh:      run_check "spotbugs.sh" "spotbugs" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-bugs.sh:  ./bin/quality-tools/spotbugs.sh                      # Run on all code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-bugs.sh:  ./bin/quality-tools/spotbugs.sh --effort=max --threshold=low   # Most thorough scan
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-bugs.sh:  ./bin/quality-tools/spotbugs.sh --module=rinna-cli   # Run on CLI module only
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-bugs.sh:# spotbugs.sh - Run SpotBugs on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-bugs.sh:${BLUE}spotbugs.sh${NC} - Run SpotBugs on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-quality-check-java-bugs.sh:Usage: spotbugs.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/run-all.sh:      run_check "spotbugs.sh" "spotbugs" "$MODULE_ARG"
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/spotbugs.sh:  ./bin/quality-tools/spotbugs.sh                      # Run on all code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/spotbugs.sh:  ./bin/quality-tools/spotbugs.sh --effort=max --threshold=low   # Most thorough scan
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/spotbugs.sh:  ./bin/quality-tools/spotbugs.sh --module=rinna-cli   # Run on CLI module only
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/spotbugs.sh:# spotbugs.sh - Run SpotBugs on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/spotbugs.sh:${BLUE}spotbugs.sh${NC} - Run SpotBugs on Java code
/home/emumford/NativeLinuxProjects/Rinna/bin/quality-tools/spotbugs.sh:Usage: spotbugs.sh [options]
/home/emumford/NativeLinuxProjects/Rinna/logs/temp/quality-tools-summary.md:   - `spotbugs.sh` - For bug detection
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## spotbugs.sh → rin-quality-check-java-bugs.sh
```

## pom-n-tag-fixer.sh → rin-xml-fix-pom-tags.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:### pom-n-tag-fixer.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-xml-schedule-cleanup.sh:        "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-xml-schedule-cleanup.sh:        "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-xml-schedule-cleanup.sh:    if [ -f "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-xml-schedule-cleanup.sh:    if [ -f "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:        "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:        "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:    if [ -f "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:    if [ -f "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## pom-n-tag-fixer.sh → rin-xml-fix-pom-tags.sh
```

## xml-cleanup-scheduler.sh → rin-xml-schedule-cleanup.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:### xml-cleanup-scheduler.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:bin/xml-tools/xml-cleanup-scheduler.sh --force
/home/emumford/NativeLinuxProjects/Rinna/bin/build.sh:    "$PROJECT_ROOT/bin/xml-tools/xml-cleanup-scheduler.sh" || true
/home/emumford/NativeLinuxProjects/Rinna/bin/build.sh:  if [[ -f "$PROJECT_ROOT/bin/xml-tools/xml-cleanup-scheduler.sh" ]]; then
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-build-main-all.sh:    "$PROJECT_ROOT/bin/xml-tools/xml-cleanup-scheduler.sh" || true
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-build-main-all.sh:  if [[ -f "$PROJECT_ROOT/bin/xml-tools/xml-cleanup-scheduler.sh" ]]; then
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-xml-schedule-cleanup.sh:# Usage: ./bin/xml-tools/xml-cleanup-scheduler.sh [--force]
/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:# Usage: ./bin/xml-tools/xml-cleanup-scheduler.sh [--force]
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## xml-cleanup-scheduler.sh → rin-xml-schedule-cleanup.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:        "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh"
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:        "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh"
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:    if [ -f "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh" ]; then
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:    if [ -f "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh" ]; then
```

## xml-cleanup.sh → rin-xml-format-all.sh

References:
```
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:### xml-cleanup.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:- Runs automatically before xml-cleanup.sh in the scheduled process
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:bin/xml-tools/xml-cleanup.sh
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:bin/xml-tools/xml-cleanup.sh --deps-only
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:bin/xml-tools/xml-cleanup.sh --validate-only
/home/emumford/NativeLinuxProjects/Rinna/bin/README-XML-TOOLS.md:bin/xml-tools/xml-cleanup.sh --verbose
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-xml-format-all.sh:# Usage: ./bin/xml-tools/xml-cleanup.sh [--validate-only] [--format-only]
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-xml-schedule-cleanup.sh:    "$PROJECT_ROOT/bin/xml-tools/xml-cleanup.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/new-structure/rin-xml-schedule-cleanup.sh:    "$PROJECT_ROOT/bin/xml-tools/xml-cleanup.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:    "$PROJECT_ROOT/bin/xml-tools/xml-cleanup.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup-scheduler.sh:    "$PROJECT_ROOT/bin/xml-tools/xml-cleanup.sh"
/home/emumford/NativeLinuxProjects/Rinna/bin/xml-tools/xml-cleanup.sh:# Usage: ./bin/xml-tools/xml-cleanup.sh [--validate-only] [--format-only]
/home/emumford/NativeLinuxProjects/Rinna/docs/project-docs/CLAUDE.md:./bin/xml-tools/xml-cleanup.sh
/home/emumford/NativeLinuxProjects/Rinna/script-references.md:## xml-cleanup.sh → rin-xml-format-all.sh
```

