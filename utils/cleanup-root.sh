#!/bin/bash
# Root directory cleanup script
# This script organizes the root directory of the Rinna project

set -e
echo "Starting root directory cleanup..."

# Base directory
BASE_DIR="/home/emumford/NativeLinuxProjects/Rinna"
cd $BASE_DIR

# Create necessary directories
mkdir -p docs/project-docs
mkdir -p config/python
mkdir -p config/docker
mkdir -p config/maven
mkdir -p config/version
mkdir -p config/hooks
mkdir -p logs/coverage
mkdir -p logs/dependency-check
mkdir -p logs/cache
mkdir -p logs/temp
mkdir -p logs/test-bin
mkdir -p logs/test-output
mkdir -p logs/test-data

# Move markdown files
echo "Moving documentation files..."
[ -f CHANGELOG.md ] && cp -f CHANGELOG.md docs/project-docs/ && ln -sf docs/project-docs/CHANGELOG.md CHANGELOG.md
[ -f CLAUDE.md ] && cp -f CLAUDE.md docs/project-docs/ && ln -sf docs/project-docs/CLAUDE.md CLAUDE.md

# Move script files
echo "Moving script files..."
for script in *.sh; do
  if [ -f "$script" ] && [ ! -L "$script" ]; then
    cp -f "$script" utils/
    ln -sf utils/"$script" "$script"
  fi
done

# Move configuration files
echo "Moving configuration files..."
[ -f pyproject.toml ] && cp -f pyproject.toml config/python/ && ln -sf config/python/pyproject.toml pyproject.toml
[ -f requirements.txt ] && cp -f requirements.txt config/python/ && ln -sf config/python/requirements.txt requirements.txt
[ -f requirements-core.txt ] && cp -f requirements-core.txt config/python/ && ln -sf config/python/requirements-core.txt requirements-core.txt
[ -f podman-compose.yml ] && cp -f podman-compose.yml config/docker/ && ln -sf config/docker/podman-compose.yml podman-compose.yml
[ -f pom-test-config.xml ] && cp -f pom-test-config.xml config/maven/ && ln -sf config/maven/pom-test-config.xml pom-test-config.xml
[ -f test-profiles.xml ] && cp -f test-profiles.xml config/maven/ && ln -sf config/maven/test-profiles.xml test-profiles.xml
[ -f version.properties ] && cp -f version.properties config/version/ && ln -sf config/version/version.properties version.properties
[ -f .env ] && cp -f .env config/ && ln -sf config/.env .env
[ -f .rinna.yaml ] && cp -f .rinna.yaml config/ && ln -sf config/.rinna.yaml .rinna.yaml
[ -f .pre-commit-config.yaml ] && cp -f .pre-commit-config.yaml config/hooks/ && ln -sf config/hooks/.pre-commit-config.yaml .pre-commit-config.yaml

# Move test and coverage files
echo "Moving test and coverage files..."
[ -f .coverage ] && cp -f .coverage logs/coverage/
[ -f .coveragerc ] && cp -f .coveragerc logs/coverage/
[ -f dependency-check.log ] && cp -f dependency-check.log logs/dependency-check/

# Consolidate utility directories
echo "Consolidating utility directories..."
if [ -d util ] && [ -d utils ]; then
  cp -rf util/* utils/ 2>/dev/null
fi

echo "Root directory cleanup completed successfully!"