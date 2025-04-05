<!-- Copyright (c) 2025 [Eric C. Mumford](https://github.com/heymumford) [@heymumford] -->

# Contribution Guidelines

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/rinna.git`
3. Set up the development environment: `mvn clean install`

## Development Process

1. Create a feature branch: `git checkout -b feature/your-feature-name`
2. Implement changes with appropriate tests
3. Ensure all tests pass: `mvn test`
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

## Release Process

- Semantic versioning (major.minor.patch)
- Release notes generation
- API compatibility validation
