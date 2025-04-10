@api @workitems
Feature: Work Item API Tests
  As a Rinna API user
  I want to manage work items through REST API
  So that I can create, read, update, and manage tasks programmatically

Background:
  * url baseUrl
  * path 'api/v1/workitems'
  * header Content-Type = 'application/json'
  * header Accept = 'application/json'
  # Authentication setup - uses config function to get token
  * def token = getAuthToken()
  * header Authorization = 'Bearer ' + token

@smoke @get
Scenario: Get all work items
  # Simple GET request to list work items
  When method get
  Then status 200
  And match response == '#array'
  And match each response contains { id: '#string', title: '#string' }
  And assert response.length >= 0

@crud @create
Scenario: Create a new work item
  # Define the request payload
  * def workItem = 
    """
    {
      "title": "Implement OAuth2 authentication",
      "description": "Add support for OAuth2 authentication flow",
      "type": "TASK",
      "priority": "HIGH"
    }
    """
  
  # Send POST request
  Given request workItem
  When method post
  Then status 201
  And match response.title == workItem.title
  And match response.type == workItem.type
  And match response.priority == workItem.priority
  And match response.id == '#string'
  And match response.state == 'FOUND'

  # Store the created ID for future use
  * def workItemId = response.id

@crud @read
Scenario: Get a specific work item
  # First create a work item to retrieve
  * def createResult = call read('classpath:workitems.feature@create')
  * def workItemId = createResult.workItemId
  
  # GET the created work item
  Given path workItemId
  When method get
  Then status 200
  And match response.id == workItemId
  And match response.title == 'Implement OAuth2 authentication'

@crud @update
Scenario: Update a work item
  # First create a work item to update
  * def createResult = call read('classpath:workitems.feature@create')
  * def workItemId = createResult.workItemId
  
  # Define update payload
  * def updatedWorkItem = 
    """
    {
      "title": "Updated: Implement OAuth2 authentication",
      "description": "Updated description with more details",
      "priority": "MEDIUM"
    }
    """
  
  # Send PUT request
  Given path workItemId
  And request updatedWorkItem
  When method put
  Then status 200
  And match response.id == workItemId
  And match response.title == updatedWorkItem.title
  And match response.description == updatedWorkItem.description
  And match response.priority == updatedWorkItem.priority

@workflow @transition
Scenario: Transition a work item state
  # First create a work item to transition
  * def createResult = call read('classpath:workitems.feature@create')
  * def workItemId = createResult.workItemId
  
  # Transition to TRIAGED state
  Given path workItemId, 'transitions'
  And request { state: 'TRIAGED' }
  When method post
  Then status 200
  And match response.state == 'TRIAGED'
  
  # Get the work item to verify state change
  Given path workItemId
  When method get
  Then status 200
  And match response.state == 'TRIAGED'

@negative @validation
Scenario Outline: Validate work item constraints
  # Test various validation scenarios
  * def invalidWorkItem = 
    """
    {
      "title": "<title>",
      "type": "<type>",
      "priority": "<priority>"
    }
    """
  
  Given request invalidWorkItem
  When method post
  Then status 400
  And match response.errors[*].field contains "<errorField>"

  Examples:
    | title             | type     | priority | errorField |
    |                   | TASK     | HIGH     | title      |
    | API Test          | INVALID  | HIGH     | type       |
    | API Test          | TASK     | INVALID  | priority   |

@security @negative
Scenario: Access without authentication
  # Remove the authentication header
  * header Authorization = null
  
  When method get
  Then status 401
  And match response.error == '#string'
  And match response.error contains 'Authentication'

@performance
Scenario: Retrieve work items with pagination
  # Test pagination performance
  * param page = 0
  * param size = 50
  
  When method get
  Then status 200
  And assert responseTime < 200
  
  # Get second page
  * param page = 1
  * param size = 50
  
  When method get
  Then status 200
  And assert responseTime < 200