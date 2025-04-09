# Test Compatibility Matrix

This document provides a framework for ensuring comprehensive test coverage across language boundaries and component interactions in the Rinna project. The compatibility matrix helps identify gaps in test coverage and ensures that all cross-language interactions are properly tested.

## Table of Contents

1. [Matrix Overview](#matrix-overview)
2. [Component-to-Component Testing](#component-to-component-testing)
3. [Cross-Language API Testing](#cross-language-api-testing)
4. [Data Format Compatibility Testing](#data-format-compatibility-testing)
5. [End-to-End Workflow Testing](#end-to-end-workflow-testing)
6. [Implementing Compatibility Tests](#implementing-compatibility-tests)
7. [Monitoring and Maintaining the Matrix](#monitoring-and-maintaining-the-matrix)

## Matrix Overview

The Rinna architecture consists of multiple components implemented in different languages:

- **Java Core & CLI** - Core business logic and command-line interface
- **Go API Server** - RESTful API server for system integration
- **Python Utilities** - Reporting, data analysis, and diagram generation

Each of these components must work together seamlessly. The compatibility matrix helps visualize and track test coverage across component boundaries.

## Component-to-Component Testing

The following matrix shows primary component interactions that require dedicated integration tests:

| Caller Component   | Called Component   | Test Priority | Current Coverage | Responsibility |
|--------------------|--------------------|--------------:|------------------|----------------|
| Java CLI           | Java Core          | High          | Complete         | CLI Team       |
| Java CLI           | Go API             | High          | Partial          | CLI Team       |
| Go API             | Java Core          | High          | Partial          | API Team       |
| Python Utilities   | Go API             | Medium        | Minimal          | Utilities Team |
| Python Utilities   | Java Core          | Low           | None             | Utilities Team |
| External Clients   | Go API             | High          | Partial          | API Team       |
| Go API             | Database           | High          | Complete         | API Team       |

### Gap Analysis

Based on the matrix above, the following test coverage gaps need to be addressed:

1. **Java CLI ➝ Go API** - Needs more comprehensive tests, especially for error handling and edge cases
2. **Go API ➝ Java Core** - Requires additional tests for newly added endpoints
3. **Python Utilities ➝ Go API** - Needs more formalized integration tests
4. **Python Utilities ➝ Java Core** - No current test coverage, evaluate necessity
5. **External Clients ➝ Go API** - Need more comprehensive API client library tests

## Cross-Language API Testing

This matrix focuses specifically on the REST API interfaces between components:

| API Endpoint          | Java Client Tests | Go Client Tests | Python Client Tests | CLI Integration Tests |
|-----------------------|-------------------|----------------|---------------------|----------------------|
| `/api/health`         | ✅               | ✅             | ✅                 | ✅                   |
| `/api/workitems`      | ✅               | ✅             | ❌                 | ✅                   |
| `/api/workitems/{id}` | ✅               | ✅             | ❌                 | ✅                   |
| `/api/projects`       | ✅               | ✅             | ❌                 | ✅                   |
| `/api/projects/{id}`  | ✅               | ✅             | ❌                 | ✅                   |
| `/api/releases`       | ✅               | ✅             | ❌                 | ❌                   |
| `/api/releases/{id}`  | ✅               | ✅             | ❌                 | ❌                   |
| `/api/webhook`        | ✅               | ✅             | ❌                 | ❌                   |

### API Test Coverage Requirements

For each API endpoint and client language combination, ensure:

1. **Basic CRUD Operations** - Create, read, update, delete
2. **Error Handling** - Invalid inputs, unauthorized access, not found
3. **Edge Cases** - Empty results, large payloads, pagination
4. **Performance** - Response times under load

## Data Format Compatibility Testing

This matrix tracks testing of data formats shared across language boundaries:

| Data Format          | Java Parser Tests | Go Parser Tests | Python Parser Tests | Cross-Language Tests |
|----------------------|-------------------|----------------|---------------------|----------------------|
| JSON Work Item       | ✅               | ✅             | ❌                 | ✅                   |
| JSON Project         | ✅               | ✅             | ❌                 | ✅                   |
| JSON Release         | ✅               | ✅             | ❌                 | ❌                   |
| JSON Webhook Payload | ✅               | ✅             | ❌                 | ❌                   |
| YAML Configuration   | ✅               | ✅             | ✅                 | ✅                   |
| CSV Reports          | ✅               | ❌             | ✅                 | ❌                   |

### Data Format Testing Requirements

For each data format and language combination:

1. **Serialization Tests** - Convert objects to the shared format
2. **Deserialization Tests** - Parse the shared format into objects
3. **Round-Trip Tests** - Serialize and deserialize to verify data integrity
4. **Schema Validation** - Ensure format complies with expected schema
5. **Cross-Language Tests** - Verify that data serialized in one language can be deserialized in another

## End-to-End Workflow Testing

End-to-end tests verify that entire workflows function correctly across language boundaries:

| Workflow                        | Test Implementation | Current Status    | Priority |
|---------------------------------|---------------------|-------------------|----------|
| Create Work Item via CLI        | BDD Feature         | ✅ Implemented    | High     |
| View Work Item via API          | Integration Test    | ✅ Implemented    | High     |
| Transition Workflow via CLI     | BDD Feature         | ✅ Implemented    | High     |
| Generate Report                 | Integration Test    | ❌ Not Implemented | Medium   |
| Project Management Workflow     | BDD Feature         | ✅ Implemented    | High     |
| Release Management Workflow     | BDD Feature         | ✅ Implemented    | High     |
| CI/CD Integration Workflow      | Integration Test    | ❌ Not Implemented | Medium   |
| External System Webhook         | Integration Test    | ❌ Not Implemented | Low      |

### Workflow Test Requirements

For each end-to-end workflow:

1. **Happy Path Test** - Verify workflow succeeds under normal conditions
2. **Error Recovery Test** - Verify system handles failures gracefully
3. **Edge Case Tests** - Verify behavior with unusual inputs or conditions
4. **Performance Tests** - Verify acceptable performance under load

## Implementing Compatibility Tests

### Cross-Language Test Template

Below is a template for implementing a cross-language compatibility test:

```java
/**
 * Tests the compatibility between Java CLI and Go API
 */
@Tag("compatibility")
@Tag("integration")
public class JavaGoCompatibilityTest {

    private static Process apiProcess;
    private static ApiClient apiClient;

    @BeforeAll
    static void startApiServer() throws IOException {
        // Start the Go API server
        ProcessBuilder pb = new ProcessBuilder("go", "run", "./api/cmd/rinnasrv/main.go");
        pb.environment().put("PORT", "8090");
        pb.environment().put("CONFIG_PATH", "./api/configs/test-config.yaml");
        apiProcess = pb.start();
        
        // Wait for server to be ready
        Thread.sleep(2000);
        
        // Initialize API client
        apiClient = new ApiClient("http://localhost:8090");
    }
    
    @AfterAll
    static void stopApiServer() {
        if (apiProcess != null) {
            apiProcess.destroy();
        }
    }
    
    @Test
    void shouldSerializeAndDeserializeWorkItemBetweenLanguages() {
        // Create work item using Java model
        WorkItemCreateRequest request = new WorkItemCreateRequest();
        request.setTitle("Cross-language Test");
        request.setType(WorkItemType.FEATURE);
        request.setPriority(Priority.HIGH);
        
        // Send to Go API
        WorkItem created = apiClient.createWorkItem(request);
        
        // Verify Go API processed and returned the item correctly
        assertNotNull(created.getId());
        assertEquals("Cross-language Test", created.getTitle());
        assertEquals(WorkItemType.FEATURE, created.getType());
        assertEquals(Priority.HIGH, created.getPriority());
        assertEquals(WorkflowState.FOUND, created.getState());
        
        // Retrieve the item back
        WorkItem retrieved = apiClient.getWorkItem(created.getId());
        
        // Verify fields match exactly
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getTitle(), retrieved.getTitle());
        assertEquals(created.getType(), retrieved.getType());
        assertEquals(created.getPriority(), retrieved.getPriority());
        assertEquals(created.getState(), retrieved.getState());
    }
    
    @Test
    void shouldHandleErrorsBetweenLanguages() {
        // Try to access non-existent work item
        try {
            apiClient.getWorkItem(999999L);
            fail("Expected exception was not thrown");
        } catch (ApiException e) {
            // Verify error status and message
            assertEquals(404, e.getStatus());
            assertTrue(e.getMessage().contains("not found"));
        }
    }
}
```

### JSON Round-Trip Compatibility Test

```java
/**
 * Tests JSON compatibility between Java and Go serialization formats
 */
@Tag("compatibility")
@Tag("integration")
public class JsonCompatibilityTest {

    @Test
    void shouldRoundTripWorkItemJsonBetweenLanguages() throws Exception {
        // Create JSON string from Java object
        WorkItem javaItem = new DefaultWorkItem();
        javaItem.setId(123L);
        javaItem.setTitle("Test Item");
        javaItem.setType(WorkItemType.BUG);
        javaItem.setPriority(Priority.HIGH);
        javaItem.setState(WorkflowState.FOUND);
        
        ObjectMapper mapper = new ObjectMapper();
        String javaJson = mapper.writeValueAsString(javaItem);
        
        // Pass JSON to Go service for parsing and re-serialization
        ProcessBuilder pb = new ProcessBuilder(
            "go", "run", "./tools/compatibility/json_roundtrip.go");
        pb.redirectInput(ProcessBuilder.Redirect.PIPE);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        
        Process process = pb.start();
        try (OutputStream os = process.getOutputStream()) {
            os.write(javaJson.getBytes(StandardCharsets.UTF_8));
        }
        
        String goJson;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            goJson = reader.readLine();
        }
        
        // Parse JSON from Go back into Java object
        WorkItem roundTrippedItem = mapper.readValue(goJson, DefaultWorkItem.class);
        
        // Verify fields match exactly
        assertEquals(javaItem.getId(), roundTrippedItem.getId());
        assertEquals(javaItem.getTitle(), roundTrippedItem.getTitle());
        assertEquals(javaItem.getType(), roundTrippedItem.getType());
        assertEquals(javaItem.getPriority(), roundTrippedItem.getPriority());
        assertEquals(javaItem.getState(), roundTrippedItem.getState());
    }
}
```

### CLI to API Compatibility Test

```bash
#!/bin/bash
# Test script for CLI to API compatibility

# Set up test environment
API_URL="http://localhost:8080"
CLI_PATH="./bin/rin"

# Start API server (if not already running)
go run ./api/cmd/rinnasrv/main.go &
API_PID=$!

# Wait for server to start
sleep 2

# Test 1: Create work item via CLI and verify API response
echo "Testing CLI-API compatibility: Create work item"
ITEM_ID=$(${CLI_PATH} add --type=TASK --title="CLI-API Test" --output=json | jq -r '.id')

if [ -z "$ITEM_ID" ]; then
  echo "Test failed: Could not create item via CLI"
  kill $API_PID
  exit 1
fi

# Verify via direct API call
API_RESPONSE=$(curl -s -X GET ${API_URL}/api/workitems/${ITEM_ID})
ITEM_TITLE=$(echo "$API_RESPONSE" | jq -r '.title')

if [ "$ITEM_TITLE" != "CLI-API Test" ]; then
  echo "Test failed: Item created via CLI not retrievable via API"
  echo "Expected: CLI-API Test, Got: $ITEM_TITLE"
  kill $API_PID
  exit 1
fi

# Test 2: Update work item via API and verify CLI view
echo "Testing API-CLI compatibility: Update work item"
curl -s -X PUT ${API_URL}/api/workitems/${ITEM_ID} \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated via API","priority":"HIGH"}'

# Verify via CLI
CLI_RESPONSE=$(${CLI_PATH} view ${ITEM_ID} --output=json)
CLI_TITLE=$(echo "$CLI_RESPONSE" | jq -r '.title')
CLI_PRIORITY=$(echo "$CLI_RESPONSE" | jq -r '.priority')

if [ "$CLI_TITLE" != "Updated via API" ] || [ "$CLI_PRIORITY" != "HIGH" ]; then
  echo "Test failed: Item updated via API not correctly viewed via CLI"
  echo "Expected title: 'Updated via API', Got: '$CLI_TITLE'"
  echo "Expected priority: 'HIGH', Got: '$CLI_PRIORITY'"
  kill $API_PID
  exit 1
fi

# Clean up
echo "Cleaning up test data..."
${CLI_PATH} delete ${ITEM_ID}

# Stop API server
kill $API_PID

echo "All compatibility tests passed!"
exit 0
```

## Monitoring and Maintaining the Matrix

### Adding New Components

When adding a new component to the system:

1. Update the compatibility matrix to include the new component
2. Identify all interaction points with existing components
3. Define test coverage requirements for each interaction
4. Implement the required tests
5. Update documentation with new test patterns

### Continuous Monitoring

To ensure ongoing compatibility:

1. Run compatibility tests as part of CI/CD pipeline
2. Generate compatibility matrix report as part of build process
3. Fail builds if compatibility test coverage falls below threshold
4. Manually review compatibility matrix quarterly to identify coverage gaps

### Compatibility Matrix Script

The following script can be used to generate an up-to-date compatibility matrix:

```python
#!/usr/bin/env python3
"""
Generates a compatibility matrix report based on test coverage analysis.
"""
import os
import re
import json
import argparse
from collections import defaultdict

# Define component mappings
COMPONENTS = {
    "java-cli": {"patterns": ["org.rinna.cli"], "language": "Java"},
    "java-core": {"patterns": ["org.rinna.domain", "org.rinna.usecase"], "language": "Java"},
    "go-api": {"patterns": ["api/internal", "api/pkg"], "language": "Go"},
    "python-utils": {"patterns": ["python/rinna"], "language": "Python"}
}

def find_tests(base_dir):
    """Find all test files in the project."""
    tests = []
    
    # Find Java tests
    for root, dirs, files in os.walk(os.path.join(base_dir, "src/test/java")):
        for file in files:
            if file.endswith("Test.java"):
                tests.append(os.path.join(root, file))
    
    # Find Go tests
    for root, dirs, files in os.walk(os.path.join(base_dir, "api")):
        for file in files:
            if file.endswith("_test.go"):
                tests.append(os.path.join(root, file))
    
    # Find Python tests
    for root, dirs, files in os.walk(os.path.join(base_dir, "python/tests")):
        for file in files:
            if file.startswith("test_") and file.endswith(".py"):
                tests.append(os.path.join(root, file))
                
    return tests

def analyze_test_file(file_path):
    """Analyze a test file to identify components being tested."""
    with open(file_path, 'r') as f:
        content = f.read()
    
    tested_components = []
    for component, info in COMPONENTS.items():
        for pattern in info["patterns"]:
            if pattern in content:
                tested_components.append(component)
    
    # Check for integration test annotations/tags
    is_integration = False
    if file_path.endswith(".java"):
        is_integration = "@Tag(\"integration\")" in content or "IntegrationTest" in file_path
    elif file_path.endswith(".go"):
        is_integration = "Integration" in file_path
    elif file_path.endswith(".py"):
        is_integration = "integration" in file_path
    
    return {
        "file": file_path,
        "tested_components": tested_components,
        "is_integration": is_integration
    }

def generate_matrix(test_analyses):
    """Generate compatibility matrix from test analyses."""
    # Initialize matrix
    matrix = defaultdict(lambda: defaultdict(lambda: {"count": 0, "files": []}))
    
    # Fill matrix
    for analysis in test_analyses:
        if analysis["is_integration"] and len(analysis["tested_components"]) >= 2:
            for comp1 in analysis["tested_components"]:
                for comp2 in analysis["tested_components"]:
                    if comp1 != comp2:
                        matrix[comp1][comp2]["count"] += 1
                        matrix[comp1][comp2]["files"].append(analysis["file"])
    
    return matrix

def generate_report(matrix, output_format="markdown"):
    """Generate a report from the compatibility matrix."""
    if output_format == "json":
        return json.dumps(matrix, indent=2)
    
    # Generate markdown report
    report = ["# Compatibility Test Matrix\n"]
    
    # Generate table header
    report.append("| Caller Component | Called Component | Test Count | Test Files |")
    report.append("|-----------------|-----------------|----------:|------------|")
    
    # Generate table rows
    for comp1 in sorted(matrix.keys()):
        for comp2 in sorted(matrix[comp1].keys()):
            tests = matrix[comp1][comp2]
            test_files = ", ".join([os.path.basename(f) for f in tests["files"]])
            if len(test_files) > 50:
                test_files = test_files[:47] + "..."
            
            report.append(f"| {comp1} | {comp2} | {tests['count']} | {test_files} |")
    
    return "\n".join(report)

def main():
    parser = argparse.ArgumentParser(description="Generate compatibility test matrix")
    parser.add_argument("--base-dir", default=".", help="Project base directory")
    parser.add_argument("--output", default="markdown", choices=["markdown", "json"], 
                      help="Output format")
    parser.add_argument("--report-file", help="Output report to file instead of stdout")
    args = parser.parse_args()
    
    tests = find_tests(args.base_dir)
    analyses = [analyze_test_file(test) for test in tests]
    matrix = generate_matrix(analyses)
    report = generate_report(matrix, args.output)
    
    if args.report_file:
        with open(args.report_file, 'w') as f:
            f.write(report)
    else:
        print(report)

if __name__ == "__main__":
    main()
```

Save this script as `bin/generate-compatibility-matrix.py` and run it to generate an up-to-date compatibility matrix.

### Reporting Standards

For each compatibility test type, establish standard reporting formats:

1. **Test Coverage Matrix** - Visual representation of test coverage
2. **Compatibility Issues** - List of identified compatibility issues
3. **Resolution Timeline** - Schedule for addressing compatibility gaps
4. **Compatibility Metrics** - Key metrics tracking compatibility health

## Additional Resources

- [Test Automation Guide](TEST_AUTOMATION_GUIDE.md) - Complete guide to test automation
- [Test Templates](TEST_TEMPLATES.md) - Ready-to-use test templates
- [Test Troubleshooting Guide](TEST_TROUBLESHOOTING.md) - Solutions for common test issues