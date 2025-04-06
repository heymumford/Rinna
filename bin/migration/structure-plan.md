# Folder Structure & Naming Convention Implementation Plan

This document outlines a balanced approach to simplify the folder structure and standardize naming conventions in the Rinna project without completely flattening the Java package structure.

## 1. Java Package Structure Optimization

### Current Issues:
- Excessive nesting: up to 13 levels deep with rinna-core/src/main/java/org/rinna/domain/entity
- Duplicate code in src/ and rinna-core/
- Inconsistent package naming across similar components

### Implementation Plan:

1. **Phase 1: Targeted Package Simplification**
   - Keep the domain-driven structure but reduce excessive nesting
   - Maintain clean architecture separation with proper Java packages
   - Target structure:
     ```
     org.rinna.domain.model      // Core domain models (formerly entity)
     org.rinna.domain.repository // Repository interfaces
     org.rinna.domain.service    // Service interfaces (formerly usecase)
     org.rinna.adapter.service   // Service implementations
     org.rinna.adapter.repository // Repository implementations (formerly persistence)
     ```

2. **Phase 2: Module Consolidation**
   - Move code from rinna-core/src/ into src/ directly
   - Update Maven pom.xml to reflect new structure
   - Remove redundant rinna-core directory
   - Remove backup directory after verification

## 2. File Naming Convention Standardization

### Current Issues:
- Inconsistent naming patterns across languages
- Mixed test naming conventions
- Unclear prefixes/suffixes

### Standard Naming Patterns:

1. **Java Code:**
   - Interfaces: No prefix/suffix (`Repository.java`, `Service.java`)
   - Implementations: Descriptive prefix (`DefaultService.java`, `InMemoryRepository.java`)
   - Tests: Suffix with `Test` (`ServiceTest.java`)
   - DTOs/Requests: Descriptive purpose (`WorkItemCreateRequest.java`)

2. **Go Code:**
   - File names: snake_case (`work_item.go`)
   - Test files: Suffix with `_test.go` (`work_item_test.go`)
   - Test helpers: Suffix with `_helper.go`
   - Packages: Singular form for models, plural for collections

3. **Shell Scripts:**
   - All scripts: kebab-case (`run-tests.sh`, `start-services.sh`)
   - Group by function: (`test-*.sh`, `build-*.sh`)

## 3. Implementation Approach

The migration will be prudent, preserving package structure where it provides organization benefits:

1. **Module-Level Simplification:**
   - Eliminate the redundant rinna-core module nesting
   - Standardize on Maven's standard directory structure
   - Reduce the path from `/rinna-core/src/main/java/` to just `/src/main/java/`

2. **Package-Level Optimization:**
   - Rename `entity` package to `model` for clarity
   - Rename `usecase` package to `service` for consistency
   - Standardize on `adapter.repository` for implementations
   - Keep domain, adapter separation for clean architecture

3. **File-Level Standardization:**
   - Apply consistent naming patterns across languages
   - Consolidate duplicate files
   - Apply standard prefixes and suffixes

## 4. Implementation Schedule

1. Week 1: Java package optimization
2. Week 2: Module consolidation
3. Week 3: Go code standardization
4. Week 4: Script standardization

## 5. Documentation

1. Update architecture documentation with new structure
2. Document naming conventions in CONVENTIONS.md
3. Update build and deployment scripts to reflect new structure