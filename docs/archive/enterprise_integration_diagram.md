# Rinna Enterprise Integration

This diagram shows how Rinna integrates with external systems and the data flow between components.

## System Integration Diagram

```mermaid
flowchart TB
    subgraph "Rinna System"
        CLI["Rinna CLI\n(Command-line Interface)"]
        Core["Rinna Core\n(Domain & Business Logic)"]
        API["Rinna API\n(RESTful Service)"]
        DB[(Rinna Database)]
        Core --> DB
    end
    
    subgraph "External Issue Trackers"
        JIRA["Jira"]
        GitHub["GitHub Issues"]
        Azure["Azure DevOps"]
    end
    
    subgraph "Version Control"
        Git["Git Repositories"]
    end
    
    subgraph "Document Systems"
        Confluence["Confluence"]
        SharePoint["SharePoint"]
    end
    
    subgraph "CI/CD Systems"
        Jenkins["Jenkins"]
        GitHubActions["GitHub Actions"]
        AzureDevOps["Azure DevOps Pipelines"]
    end
    
    CLI <--> Core
    Core <--> API
    
    API <-- "REST API\n(CRUD operations)" --> JIRA
    API <-- "REST API\n(Issue management)" --> GitHub
    API <-- "REST API\n(Work items)" --> Azure
    
    CLI <-- "Git hooks\n(Commit validation)" --> Git
    
    API <-- "REST API\n(Document generation)" --> Confluence
    API <-- "REST API\n(Document storage)" --> SharePoint
    
    API <-- "Webhooks\n(Build events)" --> Jenkins
    API <-- "Webhooks\n(CI events)" --> GitHubActions
    API <-- "REST API\n(Pipeline events)" --> AzureDevOps
    
    User((User)) <--> CLI
    User <--> API
    
    classDef core fill:#f9f,stroke:#333,stroke-width:2px
    classDef external fill:#bbf,stroke:#333,stroke-width:1px
    classDef data fill:#ffa,stroke:#333,stroke-width:1px
    
    class Core,DB core
    class JIRA,GitHub,Azure,Git,Confluence,SharePoint,Jenkins,GitHubActions,AzureDevOps external
    class User data
```

## Data Flow Diagram

```mermaid
flowchart LR
    subgraph "Rinna System"
        CLI["Rinna CLI"]
        Core["Rinna Core"]
        API["Rinna API"]
        DB[(Database)]
        
        CLI -- "1. Command input" --> Core
        Core -- "2. Business logic" --> Core
        Core -- "3. Data access" --> DB
        DB -- "4. Query results" --> Core
        Core -- "5. Processed data" --> CLI
        Core -- "6. Domain events" --> API
    end
    
    subgraph "External Systems"
        JIRA["Jira"]
        GH["GitHub"]
        DOCS["Documentation\nSystems"]
        CI["CI/CD\nSystems"]
    end
    
    API -- "7. Work item updates" --> JIRA
    JIRA -- "8. Status changes" --> API
    API -- "9. Issue updates" --> GH
    GH -- "10. Webhooks" --> API
    API -- "11. Document generation" --> DOCS
    API -- "12. Build triggers" --> CI
    CI -- "13. Build status" --> API
    
    User((User)) -- "Commands" --> CLI
    User -- "HTTP Requests" --> API
    API -- "Responses" --> User
    
    classDef rinna fill:#f9f,stroke:#333,stroke-width:2px
    classDef external fill:#bbf,stroke:#333,stroke-width:1px
    classDef user fill:#ffa,stroke:#333,stroke-width:1px
    
    class CLI,Core,API,DB rinna
    class JIRA,GH,DOCS,CI external
    class User user
```

## Integration Adapters

```mermaid
classDiagram
    class RinnaCore {
        +processWorkItem()
        +updateStatus()
        +generateDocument()
    }
    
    class IntegrationAdapter {
        <<interface>>
        +sendUpdate()
        +receiveUpdate()
        +validateCredentials()
    }
    
    class JiraAdapter {
        -jiraClient
        +sendUpdate()
        +receiveUpdate()
        +validateCredentials()
        -mapRinnaToJiraStatus()
        -mapJiraToRinnaStatus()
    }
    
    class GitHubAdapter {
        -githubClient
        +sendUpdate()
        +receiveUpdate()
        +validateCredentials()
        -mapRinnaToGitHubState()
        -mapGitHubToRinnaState()
    }
    
    class AzureDevOpsAdapter {
        -azureClient
        +sendUpdate()
        +receiveUpdate()
        +validateCredentials()
        -mapRinnaToAzureStatus()
        -mapAzureToRinnaStatus()
    }
    
    class DocumentAdapter {
        <<interface>>
        +generateDocument()
        +storeDocument()
        +retrieveDocument()
    }
    
    class ConfluenceAdapter {
        -confluenceClient
        +generateDocument()
        +storeDocument()
        +retrieveDocument()
    }
    
    class SharePointAdapter {
        -sharePointClient
        +generateDocument()
        +storeDocument()
        +retrieveDocument()
    }
    
    RinnaCore --> IntegrationAdapter
    RinnaCore --> DocumentAdapter
    IntegrationAdapter <|.. JiraAdapter
    IntegrationAdapter <|.. GitHubAdapter
    IntegrationAdapter <|.. AzureDevOpsAdapter
    DocumentAdapter <|.. ConfluenceAdapter
    DocumentAdapter <|.. SharePointAdapter
```

## Security and Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant CLI as Rinna CLI
    participant API as Rinna API
    participant Auth as Auth Service
    participant OAuth as OAuth Manager
    participant Storage as Token Storage
    participant Ext as External System
    
    User->>CLI: Issue command with external integration
    CLI->>API: Forward request
    API->>Auth: Check credentials
    Auth-->>API: Credentials valid
    
    alt OAuth 2.0 Flow
        API->>OAuth: Get token for provider
        OAuth->>Storage: Retrieve token
        Storage-->>OAuth: Return token
        
        alt Token needs refresh
            OAuth->>Ext: Request token refresh
            Ext-->>OAuth: New token
            OAuth->>Storage: Store refreshed token
        end
        
        OAuth-->>API: Provide valid token
        API->>Ext: Request with OAuth token
        Ext-->>API: Success response
    else API Key Flow
        API->>Ext: Request with API key
        Ext-->>API: Success response
    else Basic Auth Flow
        API->>Ext: Request with basic auth
        Ext-->>API: Success response
    end
    
    API-->>CLI: Integration result
    CLI-->>User: Display result
```

## OAuth Authorization Flow

```mermaid
sequenceDiagram
    participant User
    participant API as Rinna API
    participant OAuth as OAuth Manager
    participant Storage as Token Storage
    participant Provider as OAuth Provider
    
    User->>API: Initiate OAuth authorization
    API->>OAuth: Generate authorization URL
    OAuth->>OAuth: Generate state token
    OAuth->>Storage: Store state token
    OAuth-->>API: Return authorization URL with state
    API-->>User: Redirect to provider
    
    User->>Provider: Authenticate & authorize
    Provider-->>User: Redirect to callback URL with code
    
    User->>API: Callback with code & state
    API->>OAuth: Exchange code for token
    OAuth->>Storage: Verify state token
    Storage-->>OAuth: State token valid
    
    OAuth->>Provider: Exchange code for token
    Provider-->>OAuth: Access & refresh tokens
    
    OAuth->>OAuth: Process token response
    OAuth->>Storage: Store tokens securely
    OAuth-->>API: Authorization successful
    API-->>User: Display success message
```

## Integration Configuration

The integration between Rinna and external systems relies on a flexible adapter pattern with configuration-driven connections. Each integration point requires specific configuration in the `config/integrations` directory:

1. **Authentication Configuration**: Credentials, tokens, and connection settings
   - **OAuth Configuration**: Client IDs, secrets, redirect URLs, and scopes for OAuth providers
   - **API Key Configuration**: API keys and secrets for services using key-based authentication
   - **Basic Auth Configuration**: Username and password credentials for basic authentication

2. **OAuth Provider Configuration**: Settings for supported OAuth providers:
   - GitHub: OAuth application credentials and API endpoints
   - GitLab: OAuth application credentials and API endpoints
   - Jira: OAuth application credentials and API endpoints
   - Azure DevOps: OAuth application credentials and API endpoints
   - Bitbucket: OAuth application credentials and API endpoints

3. **Field Mapping Configuration**: Mapping between Rinna fields and external system fields
4. **Workflow State Mapping**: Translation between Rinna workflow states and external system states
5. **Webhook Configuration**: Endpoints and event triggers for bidirectional updates
6. **Document Templates**: Templates for generating documents in external systems

### OAuth Configuration Example

```yaml
oauth:
  # Encryption key for token storage (in production, use a secure random key)
  token_encryption_key: "your-secure-encryption-key"
  
  # GitHub OAuth configuration
  github:
    enabled: true
    client_id: "github-client-id"
    client_secret: "github-client-secret"
    redirect_url: "http://localhost:8080/api/v1/oauth/callback"
    scopes:
      - "repo"
      - "user:email"
  
  # GitLab OAuth configuration
  gitlab:
    enabled: true
    client_id: "gitlab-client-id"
    client_secret: "gitlab-client-secret"
    redirect_url: "http://localhost:8080/api/v1/oauth/callback"
    server_url: "https://gitlab.com"  # Or custom GitLab instance
    scopes:
      - "api"
      - "read_user"
```

## Implementation Strategy

The integration system follows these key principles:

1. **Loose Coupling**: Rinna core functionality works independently of external integrations
2. **Adapter Pattern**: Each external system has a dedicated adapter implementing common interfaces
3. **Idempotent Operations**: Integration operations can be safely retried without side effects
4. **Fallback Mechanisms**: System continues functioning when external systems are unavailable
5. **Audit Trail**: All integration actions are logged with comprehensive tracking
6. **Secure Authentication**: OAuth 2.0 for third-party API access with token encryption and refresh capabilities
7. **Provider Flexibility**: Unified API across different OAuth providers to simplify integration
8. **Stateless Design**: Authorization state managed securely with CSRF protection

### OAuth Implementation Features

- **Centralized Token Management**: All OAuth tokens are managed by the OAuth Manager component
- **Secure Token Storage**: Tokens are encrypted at rest using AES-GCM
- **Token Refresh**: Automatic refresh of expired tokens when possible
- **Provider Abstraction**: Common interface for all OAuth providers
- **State Parameter Protection**: Prevention of CSRF attacks during authorization
- **Scoped Access**: Tokens are scoped to specific projects and users
- **Revocation Support**: Ability to revoke tokens when access is no longer needed