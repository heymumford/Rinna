# Rinna Test Sets

This document defines test sets aligned with Rinna's test pyramid approach. These test sets are designed to be imported into Rinna to validate its own testing capabilities.

## Test Pyramid Overview

```
    ▲ Fewer
    │
    │    ┌───────────────┐
    │    │  Performance  │ Slowest, most complex
    │    └───────────────┘
    │    ┌───────────────┐
    │    │  Acceptance   │ End-to-end workflows
    │    └───────────────┘
    │    ┌───────────────┐
    │    │  Integration  │ Tests between modules
    │    └───────────────┘
    │    ┌───────────────┐
    │    │   Component   │ Tests within modules
    │    └───────────────┘
    │    ┌───────────────┐
    │    │     Unit      │ Fastest, most granular
    │    └───────────────┘
    │
    ▼ More
```

## Test Sets

### Unit Tests

```yaml
id: unit-tests
name: "Unit Tests"
description: "Tests of individual classes and methods in isolation"
type: TEST_SET
tags:
  - unit
  - fast
  - automated
tests:
  - id: UNIT-001
    name: "WorkItem Construction Test"
    description: "Tests the creation and validation of work items"
    priority: HIGH
    module: core
    class: "org.rinna.domain.DefaultWorkItemTest"
    method: "shouldConstructWorkItemWithValidData"
    expected_time: 50 # milliseconds
    tags:
      - domain
      - core
      
  - id: UNIT-002
    name: "WorkItem State Transition Test"
    description: "Tests the state transition rules for work items"
    priority: HIGH
    module: core
    class: "org.rinna.domain.DefaultWorkItemTest"
    method: "shouldTransitionToValidNextState"
    expected_time: 50
    tags:
      - domain
      - workflow
      
  - id: UNIT-003
    name: "Project Creation Test"
    description: "Tests project creation with proper validation"
    priority: MEDIUM
    module: core
    class: "org.rinna.domain.DefaultProjectTest"
    method: "shouldCreateProjectWithValidData"
    expected_time: 50
    tags:
      - domain
      - project
      
  - id: UNIT-004
    name: "ItemService Creation Test"
    description: "Tests the item service for creating work items"
    priority: HIGH
    module: core
    class: "org.rinna.usecase.DefaultItemServiceTest"
    method: "shouldCreateWorkItemWithService"
    expected_time: 75
    tags:
      - service
      - core
      
  - id: UNIT-005
    name: "WorkflowService Transition Test"
    description: "Tests the workflow service for transitioning work items"
    priority: HIGH
    module: core
    class: "org.rinna.usecase.DefaultWorkflowServiceTest"
    method: "shouldTransitionWorkItemState"
    expected_time: 75
    tags:
      - service
      - workflow
      
  - id: UNIT-006
    name: "CLI Command Parsing Test"
    description: "Tests the CLI command parser"
    priority: HIGH
    module: cli
    class: "org.rinna.cli.CommandParserTest"
    method: "shouldParseValidCommand"
    expected_time: 50
    tags:
      - cli
      - parser
      
  - id: UNIT-007
    name: "ModelMapper Test"
    description: "Tests the model mapper for CLI to Core mapping"
    priority: HIGH
    module: cli
    class: "org.rinna.cli.util.ModelMapperTest"
    method: "shouldMapCliModelToCoreModel"
    expected_time: 60
    tags:
      - cli
      - util
      
  - id: UNIT-008
    name: "Output Formatter Test"
    description: "Tests the output formatter for CLI results"
    priority: MEDIUM
    module: cli
    class: "org.rinna.cli.output.OutputFormatterTest"
    method: "shouldFormatWorkItemOutput"
    expected_time: 50
    tags:
      - cli
      - output
      
  - id: UNIT-009
    name: "API Controller Validation Test"
    description: "Tests API input validation"
    priority: HIGH
    module: api
    class: "api/pkg/controller.ItemControllerTest"
    method: "TestValidateItemRequest"
    expected_time: 60
    tags:
      - api
      - validation
      
  - id: UNIT-010
    name: "Python Report Generator Test"
    description: "Tests the Python report generator"
    priority: MEDIUM
    module: python
    class: "python/tests/unit/test_report_generator.py"
    method: "test_generate_work_item_report"
    expected_time: 75
    tags:
      - python
      - reports
```

### Component Tests

```yaml
id: component-tests
name: "Component Tests"
description: "Tests of multiple classes working together within modules"
type: TEST_SET
tags:
  - component
  - fast
  - automated
tests:
  - id: COMPONENT-001
    name: "CLI Add Command Component Test"
    description: "Tests the complete add command with its dependencies"
    priority: HIGH
    module: cli
    class: "org.rinna.cli.component.AddCommandComponentTest"
    method: "shouldExecuteAddCommandEndToEnd"
    expected_time: 150
    tags:
      - cli
      - command
      
  - id: COMPONENT-002
    name: "CLI View Command Component Test"
    description: "Tests the complete view command workflow"
    priority: HIGH
    module: cli
    class: "org.rinna.cli.component.ViewCommandComponentTest"
    method: "shouldExecuteViewCommandWithFormattedOutput"
    expected_time: 150
    tags:
      - cli
      - command
      
  - id: COMPONENT-003
    name: "ItemService Component Test"
    description: "Tests the item service with repository"
    priority: HIGH
    module: core
    class: "org.rinna.component.ItemServiceComponentTest"
    method: "shouldPersistAndRetrieveWorkItem"
    expected_time: 200
    tags:
      - core
      - repository
      
  - id: COMPONENT-004
    name: "WorkflowService Component Test"
    description: "Tests the workflow service with repository and rules engine"
    priority: HIGH
    module: core
    class: "org.rinna.component.WorkflowServiceComponentTest"
    method: "shouldValidateAndExecuteTransition"
    expected_time: 250
    tags:
      - core
      - workflow
      
  - id: COMPONENT-005
    name: "API Item Controller Component Test"
    description: "Tests the API item controller with service layer"
    priority: HIGH
    module: api
    class: "api/test/component/ItemControllerComponentTest"
    method: "TestCreateItemComponent"
    expected_time: 200
    tags:
      - api
      - controller
      
  - id: COMPONENT-006
    name: "API Project Controller Component Test"
    description: "Tests the API project controller with service layer"
    priority: MEDIUM
    module: api
    class: "api/test/component/ProjectControllerComponentTest"
    method: "TestGetProjectsComponent"
    expected_time: 200
    tags:
      - api
      - controller
      
  - id: COMPONENT-007
    name: "Python Report Service Component Test"
    description: "Tests the Python report service with template engine"
    priority: MEDIUM
    module: python
    class: "python/tests/component/test_report_service_component.py"
    method: "test_generate_report_with_templates"
    expected_time: 250
    tags:
      - python
      - reports
```

### Integration Tests

```yaml
id: integration-tests
name: "Integration Tests"
description: "Tests of interactions between modules and systems"
type: TEST_SET
tags:
  - integration
  - automated
tests:
  - id: INTEGRATION-001
    name: "CLI-to-API Integration Test"
    description: "Tests CLI commands interacting with API server"
    priority: HIGH
    module: integration
    class: "org.rinna.integration.CliApiIntegrationTest"
    method: "shouldCreateWorkItemViaCli"
    expected_time: 500
    tags:
      - cli
      - api
      - cross-language
      
  - id: INTEGRATION-002
    name: "API-to-Core Integration Test"
    description: "Tests API server interacting with core services"
    priority: HIGH
    module: integration
    class: "org.rinna.integration.ApiCoreIntegrationTest"
    method: "shouldPersistWorkItemThroughApi"
    expected_time: 500
    tags:
      - api
      - core
      - cross-language
      
  - id: INTEGRATION-003
    name: "Database Integration Test"
    description: "Tests core services with real database"
    priority: HIGH
    module: integration
    class: "org.rinna.integration.DatabaseIntegrationTest"
    method: "shouldPersistAndRetrieveEntities"
    expected_time: 600
    tags:
      - core
      - database
      
  - id: INTEGRATION-004
    name: "File System Integration Test"
    description: "Tests export and import with file system"
    priority: MEDIUM
    module: integration
    class: "org.rinna.integration.FileSystemIntegrationTest"
    method: "shouldExportAndImportWorkItems"
    expected_time: 400
    tags:
      - core
      - filesystem
      
  - id: INTEGRATION-005
    name: "Python-to-API Integration Test"
    description: "Tests Python tools interacting with API"
    priority: MEDIUM
    module: integration
    class: "python/tests/integration/test_api_integration.py"
    method: "test_retrieve_and_process_work_items"
    expected_time: 500
    tags:
      - python
      - api
      - cross-language
```

### Acceptance Tests

```yaml
id: acceptance-tests
name: "Acceptance Tests"
description: "End-to-end tests of user-facing functionality"
type: TEST_SET
tags:
  - acceptance
  - bdd
  - automated
tests:
  - id: ACCEPTANCE-001
    name: "Add Work Item Acceptance Test"
    description: "User creates a new work item"
    priority: HIGH
    module: acceptance
    class: "features/work_item_management.feature"
    method: "Scenario: Create a new work item with CLI"
    expected_time: 1000
    tags:
      - cli
      - workflow
      - user-story
      
  - id: ACCEPTANCE-002
    name: "Transition Work Item Acceptance Test"
    description: "User transitions a work item through workflow states"
    priority: HIGH
    module: acceptance
    class: "features/workflow_management.feature"
    method: "Scenario: Transition work item through states"
    expected_time: 1200
    tags:
      - cli
      - workflow
      - user-story
      
  - id: ACCEPTANCE-003
    name: "Project Management Acceptance Test"
    description: "User manages projects and their settings"
    priority: MEDIUM
    module: acceptance
    class: "features/project_management.feature"
    method: "Scenario: Create and configure a new project"
    expected_time: 1000
    tags:
      - cli
      - project
      - user-story
      
  - id: ACCEPTANCE-004
    name: "Reporting Acceptance Test"
    description: "User generates and views reports"
    priority: MEDIUM
    module: acceptance
    class: "features/reporting.feature"
    method: "Scenario: Generate work item status report"
    expected_time: 1500
    tags:
      - cli
      - reports
      - user-story
      
  - id: ACCEPTANCE-005
    name: "API Integration Acceptance Test"
    description: "External system integrates with Rinna API"
    priority: HIGH
    module: acceptance
    class: "features/api_integration.feature"
    method: "Scenario: Third-party system manages work items via API"
    expected_time: 1200
    tags:
      - api
      - integration
      - user-story
```

### Performance Tests

```yaml
id: performance-tests
name: "Performance Tests"
description: "Tests of system performance characteristics"
type: TEST_SET
tags:
  - performance
  - automated
tests:
  - id: PERFORMANCE-001
    name: "CLI Command Performance Test"
    description: "Tests CLI command execution time"
    priority: MEDIUM
    module: performance
    class: "org.rinna.performance.CliCommandPerformanceTest"
    method: "shouldExecuteAddCommandUnderThreshold"
    expected_time: 2000
    threshold: 100 # ms
    tags:
      - cli
      - benchmark
      
  - id: PERFORMANCE-002
    name: "API Throughput Test"
    description: "Tests API request throughput under load"
    priority: HIGH
    module: performance
    class: "org.rinna.performance.ApiThroughputTest"
    method: "shouldHandleMultipleConcurrentRequests"
    expected_time: 5000
    threshold: 500 # requests per second
    tags:
      - api
      - throughput
      
  - id: PERFORMANCE-003
    name: "Database Performance Test"
    description: "Tests database query performance"
    priority: HIGH
    module: performance
    class: "org.rinna.performance.DatabasePerformanceTest"
    method: "shouldQueryLargeDatasetWithinThreshold"
    expected_time: 3000
    threshold: 200 # ms
    tags:
      - database
      - query
      
  - id: PERFORMANCE-004
    name: "Report Generation Performance Test"
    description: "Tests report generation time for large datasets"
    priority: MEDIUM
    module: performance
    class: "org.rinna.performance.ReportGenerationTest"
    method: "shouldGenerateLargeReportWithinThreshold"
    expected_time: 4000
    threshold: 1000 # ms
    tags:
      - reports
      - benchmark
      
  - id: PERFORMANCE-005
    name: "Memory Usage Test"
    description: "Tests memory usage under normal operation"
    priority: MEDIUM
    module: performance
    class: "org.rinna.performance.MemoryUsageTest"
    method: "shouldMaintainMemoryUsageUnderThreshold"
    expected_time: 5000
    threshold: 256 # MB
    tags:
      - memory
      - resource
```

## Cross-Language Test Matrix

```yaml
id: cross-language-matrix
name: "Cross-Language Test Matrix"
description: "Matrix of cross-language integration tests to verify polyglot functionality"
type: TEST_MATRIX
matrix:
  rows:
    - name: "Java CLI"
      id: java-cli
    - name: "Java Core"
      id: java-core
    - name: "Go API"
      id: go-api
    - name: "Python Scripts"
      id: python-scripts
  columns:
    - name: "Java CLI"
      id: java-cli
    - name: "Java Core"
      id: java-core
    - name: "Go API"
      id: go-api
    - name: "Python Scripts"
      id: python-scripts
  cells:
    - row: java-cli
      column: java-core
      tests:
        - id: INTEGRATION-006
          name: "CLI to Core Integration"
          class: "org.rinna.integration.CliCoreIntegrationTest"
          priority: HIGH
          
    - row: java-cli
      column: go-api
      tests:
        - id: INTEGRATION-001
          name: "CLI to API Integration"
          class: "org.rinna.integration.CliApiIntegrationTest"
          priority: HIGH
          
    - row: java-cli
      column: python-scripts
      tests:
        - id: INTEGRATION-007
          name: "CLI to Python Integration"
          class: "org.rinna.integration.CliPythonIntegrationTest"
          priority: MEDIUM
          
    - row: java-core
      column: go-api
      tests:
        - id: INTEGRATION-002
          name: "Core to API Integration"
          class: "org.rinna.integration.CoreApiIntegrationTest"
          priority: HIGH
          
    - row: java-core
      column: python-scripts
      tests:
        - id: INTEGRATION-008
          name: "Core to Python Integration"
          class: "org.rinna.integration.CorePythonIntegrationTest"
          priority: MEDIUM
          
    - row: go-api
      column: python-scripts
      tests:
        - id: INTEGRATION-005
          name: "API to Python Integration"
          class: "python/tests/integration/test_api_integration.py"
          priority: MEDIUM
```

## Smoke Test Set

```yaml
id: smoke-tests
name: "Smoke Tests"
description: "Critical tests that run quickly to verify basic functionality"
type: TEST_SET
tags:
  - smoke
  - fast
  - critical
  - automated
tests:
  - id: SMOKE-001
    name: "Basic CLI Commands Smoke Test"
    description: "Verifies core CLI commands function correctly"
    priority: CRITICAL
    module: cli
    class: "org.rinna.smoke.CliSmokeTest"
    method: "shouldExecuteBasicCommands"
    expected_time: 500
    tags:
      - cli
      
  - id: SMOKE-002
    name: "API Health Check Smoke Test"
    description: "Verifies API server is running and healthy"
    priority: CRITICAL
    module: api
    class: "api/test/smoke/ApiSmokeTest"
    method: "TestApiHealth"
    expected_time: 200
    tags:
      - api
      
  - id: SMOKE-003
    name: "Database Connection Smoke Test"
    description: "Verifies database connection is established"
    priority: CRITICAL
    module: core
    class: "org.rinna.smoke.DatabaseSmokeTest"
    method: "shouldConnectToDatabase"
    expected_time: 300
    tags:
      - database
      
  - id: SMOKE-004
    name: "Basic Workflow Smoke Test"
    description: "Verifies basic workflow transitions work"
    priority: CRITICAL
    module: core
    class: "org.rinna.smoke.WorkflowSmokeTest"
    method: "shouldPerformBasicTransitions"
    expected_time: 400
    tags:
      - workflow
      
  - id: SMOKE-005
    name: "Python Integration Smoke Test"
    description: "Verifies Python scripts can be executed"
    priority: HIGH
    module: python
    class: "python/tests/smoke/test_smoke.py"
    method: "test_basic_functionality"
    expected_time: 300
    tags:
      - python
```

## CYNEFIN Domain Distribution

```yaml
id: cynefin-domains
name: "CYNEFIN Domain Distribution"
description: "Distribution of tests across CYNEFIN complexity domains"
type: TEST_DISTRIBUTION
domains:
  - name: "Clear"
    description: "Well-understood problems with established solutions"
    tests:
      - UNIT-001
      - UNIT-003
      - UNIT-006
      - UNIT-008
      - COMPONENT-001
      - COMPONENT-002
      - SMOKE-001
      - SMOKE-002
  
  - name: "Complicated"
    description: "Problems requiring expertise but with knowable solutions"
    tests:
      - UNIT-002
      - UNIT-004
      - UNIT-005
      - UNIT-007
      - UNIT-009
      - COMPONENT-003
      - COMPONENT-004
      - COMPONENT-005
      - COMPONENT-006
      - INTEGRATION-003
      - INTEGRATION-004
      - SMOKE-003
      - SMOKE-004
      - PERFORMANCE-001
      - PERFORMANCE-003
  
  - name: "Complex"
    description: "Problems requiring experimentation and adaptation"
    tests:
      - UNIT-010
      - COMPONENT-007
      - INTEGRATION-001
      - INTEGRATION-002
      - INTEGRATION-005
      - ACCEPTANCE-001
      - ACCEPTANCE-002
      - ACCEPTANCE-003
      - ACCEPTANCE-004
      - PERFORMANCE-002
      - PERFORMANCE-004
      - SMOKE-005
  
  - name: "Chaotic"
    description: "Novel problems requiring innovative approaches"
    tests:
      - ACCEPTANCE-005
      - PERFORMANCE-005
```

## Cognitive Load Assessment

```yaml
id: cognitive-load
name: "Test Cognitive Load Assessment"
description: "Assessment of cognitive load for test implementation and maintenance"
type: ASSESSMENT
items:
  - id: "unit-tests"
    name: "Unit Tests"
    cognitive_load: 3
    description: "Low cognitive load due to isolation and clear scoping"
    
  - id: "component-tests"
    name: "Component Tests" 
    cognitive_load: 5
    description: "Medium cognitive load due to interaction between components"
    
  - id: "integration-tests"
    name: "Integration Tests"
    cognitive_load: 7
    description: "Higher cognitive load due to cross-module and cross-language concerns"
    
  - id: "acceptance-tests"
    name: "Acceptance Tests"
    cognitive_load: 6
    description: "Medium-high cognitive load due to end-to-end flows but supported by BDD"
    
  - id: "performance-tests"
    name: "Performance Tests"
    cognitive_load: 8
    description: "High cognitive load due to complex performance dependencies and analysis"
    
  - id: "cross-language-tests"
    name: "Cross-Language Tests"
    cognitive_load: 9
    description: "Very high cognitive load due to polyglot complexities and integration points"
```

## Test Implementation Schedule

```yaml
id: test-implementation-schedule
name: "Test Implementation Schedule"
description: "Timeline for implementing test pyramid coverage"
type: SCHEDULE
milestones:
  - name: "Initial Unit Tests"
    date: "2025-04-22"
    completion: 100
    tests:
      - UNIT-001
      - UNIT-002
      - UNIT-004
      - UNIT-006
      - UNIT-007
      - UNIT-009
    
  - name: "Component Test Coverage"
    date: "2025-04-29"
    completion: 90
    tests:
      - COMPONENT-001
      - COMPONENT-002
      - COMPONENT-003
      - COMPONENT-004
      - COMPONENT-005
    
  - name: "Integration Test Framework"
    date: "2025-05-06"
    completion: 75
    tests:
      - INTEGRATION-001
      - INTEGRATION-002
      - INTEGRATION-003
    
  - name: "Acceptance Test Foundation"
    date: "2025-05-13"
    completion: 50
    tests:
      - ACCEPTANCE-001
      - ACCEPTANCE-002
    
  - name: "Performance Test Baseline"
    date: "2025-05-20"
    completion: 30
    tests:
      - PERFORMANCE-001
      - PERFORMANCE-002
    
  - name: "Cross-Language Test Matrix"
    date: "2025-05-27"
    completion: 40
    description: "Implementing cross-language test matrix"
    
  - name: "Complete Test Pyramid"
    date: "2025-06-10"
    completion: 0
    description: "All test types implemented and integrated"
```
