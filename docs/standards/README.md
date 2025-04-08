# Project Standards

This directory contains documentation on standards to be followed throughout the Rinna project.

## Contents

- [XML Tools Standard](./XML_TOOLS_STANDARD.md) - Standards for XML file manipulation
- [File Organization](./FILE_ORGANIZATION.md) - Standards for file organization and placement
- [Code Implementation](./CODE_IMPLEMENTATION.md) - Standards for code implementation

## General Standards

1. **Code Organization**
   - Files should always be placed in the appropriate folders according to their function
   - Files should never be placed in the project root unless they are project-level configuration files
   - Documentation should be organized in the docs directory with appropriate subdirectories
   - Never create symlinks for backward compatibility

2. **Implementation Standards**
   - No placeholder code - all implementations should be fully functional
   - No "in a real implementation" comments - all code should be production-ready
   - No TODO comments unless they are tracked in the issue system
   - Proper error handling in all code paths

3. **File Location Standards**
   - If file relocations break functionality, tests should fail
   - Fix broken file references properly rather than working around them
   - Update all documentation when file locations change
   - All modules should have well-defined boundaries and dependencies

4. **Testing Standards**
   - All code should have appropriate tests at the right level of the testing pyramid
   - Tests should run without side effects
   - Tests should not rely on specific file locations
   - Test fixtures should be isolated and self-contained
   
5. **XML Manipulation**
   - Always use XMLStarlet for XML manipulation (see [XML Tools Standard](./XML_TOOLS_STANDARD.md))
   - Never use grep, sed, or other text tools for XML parsing or manipulation