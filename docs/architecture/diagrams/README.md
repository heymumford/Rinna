# Architecture Diagrams

This directory contains architecture diagrams for the Rinna system.

## Parent Documentation
- [Architecture Documentation](../README.md)
- [Documentation Home](../../README.md)

## Diagram Generation

These diagrams are automatically generated during the build process using the C4 model approach. To manually generate them, run:

```bash
./bin/generate-diagrams.sh
```

## Available Diagrams

The diagrams represent different levels of abstraction according to the C4 model:

1. **Context Diagram**: Shows Rinna and its relationships with users and external systems
2. **Container Diagram**: Shows the high-level technical components in Rinna
3. **Component Diagram**: Shows the internal components of each container
4. **Code Diagram**: Shows how components are implemented as code

These diagrams provide a comprehensive view of the Rinna architecture from high-level context to detailed implementation.