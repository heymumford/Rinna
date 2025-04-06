# Rinna Architecture Documentation

This section contains documentation about the Rinna project architecture.

## C4 Model Architecture Diagrams

The architecture is documented using the [C4 model](https://c4model.com/) approach, which provides multiple levels of abstraction:

1. **Context Diagram**: Shows the system and its relationships with users and other systems
2. **Container Diagram**: Shows the high-level technical components (applications, data stores, etc.)
3. **Component Diagram**: Shows how containers are composed of components
4. **Code Diagram**: Shows how components are implemented as code

These diagrams are automatically generated during the build process and stored in the [diagrams](../diagrams) directory.

### Viewing the Latest Diagrams

The latest generated diagrams can be found in the [diagrams directory](../diagrams/README.md).

### Generating Diagrams

The C4 model diagrams are automatically generated during the Maven build process. To manually generate them, run:

```bash
./bin/generate-diagrams.sh
```

For asynchronous generation (useful during development):

```bash
./bin/generate-diagrams.sh --async
```

For more options:

```bash
./bin/generate-diagrams.sh --help
```

## Architecture Decision Records

Important architectural decisions are documented as Architecture Decision Records (ADRs) in the [decisions](./decisions) directory.

## Design Principles

The Rinna project follows these key design principles:

1. **Clean Architecture**: Separation of concerns with domain logic at the center
2. **Domain-Driven Design**: Focus on the core domain and its business logic
3. **Test-Driven Development**: Tests drive the design of the system
4. **Polyglot Implementation**: Using the right language for each component
5. **Cross-language Interoperability**: Consistent APIs and models across languages
6. **Developer Experience**: Command-line tools and APIs designed for developer productivity

## Implementation Details

For more detailed implementation information, see:

- [Development Approach](../development/design-approach.md)
- [Package Structure](../development/package-structure.md)
- [API Design](../development/api-design.md)