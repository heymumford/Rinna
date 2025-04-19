# Karate Test Syntax Overview

Karate is a BDD-style API testing framework that combines API test automation, mocks, performance testing, and UI automation. This document provides a concise overview of Karate syntax and best practices for the Rinna project.

## Basic Structure

```gherkin
Feature: Brief description of feature being tested

Background:
  * def baseUrl = 'http://localhost:8080/api'
  * header Accept = 'application/json'

Scenario: Descriptive name of test scenario
  Given url baseUrl + '/workitems'
  And request { title: 'Test Item', type: 'TASK' }
  When method post
  Then status 201
  And match response.id == '#present'
```

## Syntax Elements

### Asterisk (`*`) Meaning and Commands

The asterisk (`*`) is a generic step prefix in Karate that can be used to:

1. **Define variables**: `* def variableName = value`
2. **Execute JavaScript**: `* eval someFunction()`
3. **Configure headers/params**: `* header Content-Type = 'application/json'`
4. **Print debug info**: `* print 'Response:', response`
5. **Read files**: `* def payload = read('data.json')`

Common commands after asterisks:

| Command | Description | Example |
|---------|-------------|---------|
| `def` | Define a variable | `* def id = 42` |
| `path` | Append to URL path | `* path 'workitems', id` |
| `param` | Add URL parameter | `* param type = 'TASK'` |
| `header` | Set request header | `* header Authorization = token` |
| `request` | Set request body | `* request { name: 'test' }` |
| `method` | Execute HTTP method | `* method get` |
| `status` | Assert response code | `* status 200` |
| `match` | Assert JSON/XML | `* match response.id == '#number'` |
| `configure` | Set config options | `* configure ssl = true` |

## Setting Up Preconditions

### 1. Background Section

The `Background` section runs before each scenario and is ideal for setting up common preconditions:

```gherkin
Background:
  # Base configuration
  * url 'http://localhost:8080/api'
  * header Content-Type = 'application/json'
  
  # Authentication setup
  * def authResponse = call read('auth.feature')
  * def token = authResponse.token
  * header Authorization = 'Bearer ' + token
  
  # Common test data
  * def testData = read('test-data.json')
```

### 2. Calling Other Features

Reuse authentication or setup logic from other feature files:

```gherkin
Scenario: Create a new work item
  # Setup precondition by calling another feature
  * def result = call read('create-project.feature')
  * def projectId = result.response.id
  
  # Use the results in this test
  Given path 'projects', projectId, 'workitems'
  And request { title: 'Test Item' }
  When method post
  Then status 201
```

### 3. JavaScript Functions for Complex Setup

```gherkin
Background:
  * def createTestWorkItems = 
    """
    function(count) {
      var items = [];
      for (var i = 0; i < count; i++) {
        var item = { 
          title: 'Test Item ' + i, 
          type: 'TASK',
          priority: 'MEDIUM' 
        };
        items.push(item);
      }
      return items;
    }
    """
  
  * def workItems = createTestWorkItems(3)
```

## Best Practices for Tagging

### Recommended Tag Structure

```gherkin
@api @workitems @regression
Feature: Work Item API Tests

@smoke
Scenario: Get work item by ID - basic check
  # Quick smoke test

@crud @create
Scenario: Create a new work item
  # Test CRUD operations

@security @negative
Scenario: Attempt to access unauthorized resource
  # Security test

@performance
Scenario: Retrieve multiple work items with pagination
  # Performance-related test
```

### Tagging Best Practices

1. **Layer tags hierarchically**:
   - Module-level: `@workitems`, `@projects`, `@releases`
   - Operation-type: `@crud`, `@query`, `@validation`
   - Test-type: `@smoke`, `@regression`, `@security`

2. **Use consistent naming conventions**:
   - Lowercase with hyphens for multi-word tags
   - Avoid spaces and special characters

3. **Tag for selective execution**:
   - `@smoke` for critical path tests run frequently
   - `@regression` for comprehensive tests
   - `@ignore` to temporarily disable tests

4. **Tag for reporting and organization**:
   - `@version-1.2.3` for version-specific tests
   - `@bug-fix-123` for tests validating specific bug fixes

## Build Script Integration

### Maven Configuration

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.intuit.karate</groupId>
    <artifactId>karate-junit5</artifactId>
    <version>1.4.0</version>
    <scope>test</scope>
</dependency>

<!-- Optional UI support -->
<dependency>
    <groupId>com.intuit.karate</groupId>
    <artifactId>karate-robot</artifactId>
    <version>1.4.0</version>
    <scope>test</scope>
</dependency>
```

### Runner Class

Create a JUnit runner for Karate tests:

```java
package org.rinna.api.test;

import com.intuit.karate.junit5.Karate;

class KarateTests {
    
    @Karate.Test
    Karate workItemTests() {
        return Karate.run("workitems").relativeTo(getClass());
    }
    
    @Karate.Test
    Karate allTests() {
        return Karate.run().relativeTo(getClass());
    }
    
    @Karate.Test
    Karate smokeTests() {
        return Karate.run().tags("@smoke").relativeTo(getClass());
    }
}
```

### Parallel Execution

Run tests in parallel for faster execution:

```gherkin
* configure parallel = { "threads": 5 }
```

### Environment-Specific Configuration

Using `karate-config.js`:

```javascript
function() {
  var env = karate.env || 'dev';
  var config = {
    baseUrl: 'http://localhost:8080/api'
  };
  
  if (env === 'dev') {
    // dev-specific config
  } else if (env === 'staging') {
    config.baseUrl = 'https://staging.rinna.org/api';
  }
  
  // Authentication helper
  config.getAuthToken = function() {
    var authResult = karate.call('classpath:auth.feature');
    return authResult.token;
  };
  
  return config;
}
```

### Command Line Execution

```bash
# Run all tests
mvn test -Dtest=KarateTests

# Run only smoke tests
mvn test -Dtest=KarateTests#smokeTests

# Run with a specific environment
mvn test -Dkarate.env=staging

# Run with tags
mvn test -Dkarate.options="--tags @smoke,@regression"
```

## Common Patterns for Rinna-Specific API Testing

### Testing WorkItem API

```gherkin
Feature: Work Item API

Scenario: Create and retrieve a work item
  # Create item
  Given url baseUrl + '/workitems'
  And request { title: 'New Feature', type: 'FEATURE', priority: 'HIGH' }
  When method post
  Then status 201
  And match response.id == '#present'
  And match response.title == 'New Feature'
  
  # Store ID for future use
  * def itemId = response.id
  
  # Retrieve the created item
  Given url baseUrl + '/workitems/' + itemId
  When method get
  Then status 200
  And match response.title == 'New Feature'
  And match response.type == 'FEATURE'
  And match response.priority == 'HIGH'
```

### Workflow State Transitions

```gherkin
Scenario: Transition a work item through states
  # Create item in initial state
  * def itemId = call read('create-workitem.feature').response.id
  
  # Transition to TRIAGED
  Given url baseUrl + '/workitems/' + itemId + '/transition'
  And request { state: 'TRIAGED' }
  When method put
  Then status 200
  And match response.state == 'TRIAGED'
  
  # Verify invalid transition fails
  Given url baseUrl + '/workitems/' + itemId + '/transition'
  And request { state: 'DONE' }
  When method put
  Then status 400
```

### Data Validation

```gherkin
Scenario: Validate work item data constraints
  # Test required fields
  Given url baseUrl + '/workitems'
  And request { }  # Empty body
  When method post
  Then status 400
  And match response.errors[*].field contains 'title'
  
  # Test field validation
  Given url baseUrl + '/workitems'
  And request { title: 'Test', priority: 'INVALID_PRIORITY' }
  When method post
  Then status 400
  And match response.errors[*].field contains 'priority'
```

## Reference

For full Karate documentation, refer to:
- [Karate GitHub](https://github.com/karatelabs/karate)
- [Karate Documentation](https://karatelabs.github.io/karate/)