# Scripts Directory

This directory contains activation and utility scripts for the Rinna project. Most scripts are maintained as symbolic links to their implementation in the `utils/` directory for backward compatibility.

## Available Scripts

### Activation Scripts
- `activate-api.sh` - Activate API development environment
- `activate-java.sh` - Activate Java development environment
- `activate-python.sh` - Activate Python development environment
- `activate-rinna.sh` - Activate main Rinna development environment
- `activate-system-python.sh` - Activate system Python environment

### Build Scripts
- `build.sh` - Main build script
- `build-orchestrator.sh` - Build orchestration script

### Utility Scripts
- `cleanup-root.sh` - Clean up root directory
- `remove-originals.sh` - Remove original files after migration
- `custom-maven.sh` - Custom Maven configuration script

## Script Naming Convention

The project is transitioning to a new script naming convention:
```
rin-[module]-[submodule]-[language]-[action].sh
```

See `docs/development/script-naming-convention.md` for more details.