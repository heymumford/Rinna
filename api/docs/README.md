# Rinna API Documentation

This directory contains the API documentation for the Rinna project management system.

## Documentation

### OpenAPI/Swagger Documentation

Rinna uses the OpenAPI Specification (formerly known as Swagger) to document its API. The main specification file is located at:

```
/api/swagger.yaml
```

### Generated Documentation

Documentation is generated in the following formats:

- **YAML** - The source format (`swagger.yaml`)
- **JSON** - Generated JSON format (`swagger.json`)
- **HTML** - Human-readable HTML documentation using ReDoc (`index.html`)

### Comprehensive Examples

For detailed examples of each API endpoint including request/response examples in multiple formats, see:

- [API Examples Documentation](api-examples.md) - Comprehensive examples for all endpoints

### Viewing the Documentation

When the API server is running, you can access the documentation at:

- `/docs/` - ReDoc HTML documentation
- `/docs/swagger.json` - Raw JSON specification
- `/docs/swagger.yaml` - Raw YAML specification
- `/docs/examples/` - Endpoint examples documentation

### Updating the Documentation

The API documentation is maintained in two ways:

1. **Manual Updates**: The base `swagger.yaml` file can be edited directly.
2. **Annotations**: Go code can include Swagger annotations that are extracted to generate documentation.

## Adding Swagger Annotations

### Handler Example

```go
// swagger:route GET /projects/{key} projects getProjectByKey
// Get project by key
// Returns a project by its unique key
// Parameters:
//   + name: key
//     in: path
//     description: Project key
//     required: true
//     type: string
// Responses:
//   200: Project
//   401: Error
//   404: Error
//   500: Error
func GetProjectByKey(w http.ResponseWriter, r *http.Request) {
    // Handler implementation
}
```

### Model Example

```go
// Project represents a project in the system
// swagger:model Project
type Project struct {
    // Unique identifier
    // example: 550e8400-e29b-41d4-a716-446655440000
    ID string `json:"id"`
    
    // Project key (short code)
    // example: RINNA
    Key string `json:"key"`
    
    // Project name
    // example: Rinna Project Management
    Name string `json:"name"`
    
    // Project description
    // example: Internal project management system
    Description string `json:"description,omitempty"`
    
    // Whether the project is active
    // example: true
    Active bool `json:"active"`
}
```

## Generating Documentation

The documentation is automatically generated during the build process. You can also generate it manually:

```bash
./bin/generate-swagger.sh                  # Generate in YAML format (default)
./bin/generate-swagger.sh --format=json    # Generate in JSON format
./bin/generate-swagger.sh --format=html    # Generate in HTML format
./bin/generate-swagger.sh --validate-only  # Only validate the specification
```

### Dependencies

To use all features of the documentation generator, install:

1. `swagger-cli` - For validation: `npm install -g swagger-cli`
2. `go-swagger` - For Go annotations: `go install github.com/go-swagger/go-swagger/cmd/swagger@latest`
3. `redoc-cli` - For HTML generation: `npm install -g redoc-cli`

## Best Practices

1. **Keep It Updated**: Update the documentation when endpoints change.
2. **Include Examples**: Provide examples for request/response bodies.
3. **Document Error Responses**: Document all possible error responses with their status codes.
4. **Use Tags**: Group endpoints by logical functions (projects, workitems, releases).
5. **Validate**: Always validate the specification before committing changes.

## Reference

- [OpenAPI Specification](https://swagger.io/specification/)
- [go-swagger Documentation](https://goswagger.io/use/spec.html)
- [ReDoc Documentation](https://github.com/Redocly/redoc)