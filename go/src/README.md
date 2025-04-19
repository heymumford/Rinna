# API Internal Components

## Purpose
Contains implementation details for the Rinna API that are not meant to be imported by other projects.

## Contents
- HTTP handlers
- API middleware
- Internal clients
- API-specific business logic

## Naming Conventions
- Handler files: `projects.go`, `workitems.go` (plural resource name)
- Middleware files: `auth.go`, `logging.go` (function name)
- Client files: `client.go`, `httpClient.go` (descriptive name + Client)

## When to Add Files Here
Add files here for any API implementation details that should not be exposed outside the API module.

## When to Create Subdirectories
Three standard subdirectories:
- `handlers/` - HTTP request handlers
- `middleware/` - HTTP middleware components
- `client/` - API client implementations

Additional subdirectories should only be created for distinct functional areas with multiple related files.