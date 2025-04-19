#!/bin/bash

# Exit on any error
set -e

echo "Creating backup of current project..."
mkdir -p ../rinna-backup-$(date +%Y%m%d)
cp -R . ../rinna-backup-$(date +%Y%m%d)/

echo "Creating new directory structure..."
mkdir -p java/rinna-core java/rinna-cli 
mkdir -p python/scripts/api
mkdir -p api/specs
mkdir -p config/java config/python config/shared
mkdir -p scripts build

echo "Moving Java components..."
# Move Java core and CLI projects
mv rinna-core/* java/rinna-core/
mv rinna-cli/* java/rinna-cli/
mv pom.xml java/

echo "Moving Python utility scripts..."
find bin -name "*.py" -type f -exec mv {} python/scripts/ \;
find api/bin -name "*.py" -type f -exec mv {} python/scripts/api/ \;

echo "Moving API definitions..."
find api -name "*.json" -o -name "*.yaml" -o -name "*.yml" | grep -v bin | while read file; do
    target_dir="api/specs/$(dirname "$file" | sed 's|^api/||')"
    mkdir -p "$target_dir"
    mv "$file" "$target_dir/"
done

echo "Moving build scripts..."
mv build.sh scripts/
chmod +x scripts/build.sh

echo "Updating configuration structure..."
if [ -d config ]; then
    find config -type f | while read file; do
        if [[ "$file" == *java* ]]; then
            target_dir="config/java/$(dirname "$file" | sed 's|^config/||')"
            mkdir -p "$target_dir"
            mv "$file" "$target_dir/"
        elif [[ "$file" == *python* ]]; then
            target_dir="config/python/$(dirname "$file" | sed 's|^config/||')"
            mkdir -p "$target_dir"
            mv "$file" "$target_dir/"
        else
            target_dir="config/shared/$(dirname "$file" | sed 's|^config/||')"
            mkdir -p "$target_dir"
            mv "$file" "$target_dir/"
        fi
    done
fi

echo "Creating .gitignore to exclude build artifacts..."
cat > .gitignore << EOF
# Build artifacts
build/

# Java
java/**/target/
*.class
*.jar

# Python
__pycache__/
*.py[cod]
*$py.class
*.so
.Python
.env
.venv
env/
venv/
.coverage
htmlcov/

# IDE files
.idea/
.vscode/
*.iml

# Logs
logs/
*.log
EOF

echo "Updating README.md with new structure information..."
cat > README.md << EOF
# Rinna Project

A polyglot workflow management system.

## Project Structure

This project follows a polyglot architecture pattern:

\`\`\`
/
├── java/                     # Java components
│   ├── rinna-core/           # Core Java library
│   ├── rinna-cli/            # Command-line interface
│   └── pom.xml               # Maven configuration for Java
│
├── python/                   # Python components
│   ├── rinna/                # Main Python package
│   ├── lucidchart_py/        # Secondary Python package
│   ├── tests/                # Python tests
│   ├── scripts/              # Python utility scripts
│   │   └── api/              # API-related scripts
│   └── pyproject.toml        # Poetry configuration
│
├── api/                      # API definition files (Swagger, OpenAPI)
│   └── specs/                # API specifications
│
├── config/                   # Configuration files
│   ├── java/                 # Java-specific configs
│   ├── python/               # Python-specific configs
│   └── shared/               # Shared configuration
│
├── docs/                     # Documentation
│
├── build/                    # Build output directory
│
└── scripts/                  # Build and utility scripts
    └── build.sh              # Main build script
\`\`\`

## Building the Project

\`\`\`bash
# Build all components
./scripts/build.sh

# Build specific components
./scripts/build.sh java
./scripts/build.sh python
\`\`\`

## Development

### Java Components

\`\`\`bash
cd java
mvn clean install
\`\`\`

### Python Components

\`\`\`bash
cd python
poetry install
poetry env use python3.13
poetry env activate
# Then run the source command from the output
\`\`\`
EOF

echo "Reorganization complete. Please check the new structure and adjust as needed."
echo "A backup of your original project has been created at ../rinna-backup-$(date +%Y%m%d)/"

