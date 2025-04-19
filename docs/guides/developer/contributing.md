# Contributing to Rinna

This guide explains how to contribute to the Rinna project.

## Getting Started

1. Fork the repository: `git clone https://github.com/your-username/rinna.git`
2. Set up the development environment: `bin/rin-setup-unified --developer`
3. Build the project: `bin/rin build test`

## Development Process

1. Create a feature branch: `git checkout -b feature/your-feature-name`
2. Implement changes with appropriate tests
3. Ensure all tests pass: `bin/rin test`
4. Submit a pull request to the main repository

## Code Style

- Follow Oracle Java style guide
- Use CamelCase for classes, lowerCamelCase for methods/variables
- Group and order imports: java.*, javax.*, com.*, org.*
- Use 4-space indentation, 100 character line limit
- Write explicit exceptions with meaningful messages
- Prefer immutable objects, use interfaces for declarations

## Documentation Requirements

- All public classes and methods must have JavaDoc
- Update relevant documentation for new features
- Add BDD tests for high-level features
- Add unit tests for classes and methods

## Review Process

1. Automated checks (CI/CD, tests, code style)
2. Peer review by at least one maintainer
3. Acceptance and merge

## Intellectual Property

By contributing to this project, you confirm that your contributions are your 
own original work. If you use AI assistance tools in creating your contribution, 
please disclose this in your pull request description. Use of such tools does not 
affect your ownership of your contribution, but transparency helps maintain clear 
provenance of the project's intellectual property.

## Release Process

- Semantic versioning (major.minor.patch)
- Release notes generation
- API compatibility validation

## Testing

Ensure your contributions include appropriate tests:

1. Unit tests for individual components
2. Component tests for modules
3. Integration tests for cross-component functionality
4. BDD tests for user-facing features

Run tests using the following commands:

```bash
# Run all tests
bin/rin test all

# Run specific test categories
bin/rin test unit
bin/rin test component
bin/rin test integration
bin/rin test bdd
```

## Pull Request Guidelines

1. **Description**: Provide a clear description of your changes
2. **Issue Link**: Reference any related issues
3. **Test Coverage**: Ensure adequate test coverage
4. **Documentation**: Update relevant documentation
5. **Breaking Changes**: Clearly mark any breaking changes
6. **Clean Commits**: Keep commits focused and clean

## Commit Message Format

Follow this format for commit messages:

```
type(scope): Subject line (50 chars max)

Body of the explanation (72 chars per line)
Explain what and why, not how

Fixes #123
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`

## Getting Help

If you need help with your contribution:

1. Check existing documentation and issues
2. Ask in the development channel on Discord
3. Reach out to maintainers via email
