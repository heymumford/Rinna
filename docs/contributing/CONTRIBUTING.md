# Contributing to Rinna

Thank you for your interest in contributing to Rinna! This document provides guidelines and instructions for contributing to the project.

## 🌟 Developer Guide

**For comprehensive developer documentation, start with the [Developer Guide](DEVELOPER.md)**, which includes:

- Setting up your development environment
- Understanding the architecture
- Development workflow
- Build system usage
- Testing guidelines
- Version management
- Reference documentation

## 🚀 Quick Start for Contributors

1. **Fork and Clone**
   ```bash
   git clone https://github.com/your-username/Rinna.git
   cd Rinna
   ```

2. **Set Up Environment**
   ```bash
   bin/rin-setup-unified --developer
   ```

3. **Create Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

4. **Implement Changes**
   - Follow our [Clean Architecture](docs/architecture/decisions/0003-adopt-clean-architecture-for-system-design.md) principles
   - Add appropriate tests following our [Testing Strategy](docs/testing/TESTING_STRATEGY.md)
   - Use our [Coding Standards](docs/reference/standards/code-review-guidelines.md)

5. **Test Your Changes**
   ```bash
   bin/rin test
   ```

6. **Submit Pull Request**
   - Include a clear description of the changes
   - Reference any related issues
   - Ensure all tests pass
   - Follow our [Documentation Requirements](docs/reference/standards/documentation-requirements.md)

## ⚖️ Intellectual Property

By contributing to this project, you confirm that your contributions are your 
own original work. If you use AI assistance tools in creating your contribution, 
please disclose this in your pull request description. Use of such tools does not 
affect your ownership of your contribution, but transparency helps maintain clear 
provenance of the project's intellectual property.

## 📜 Code of Conduct

We expect all contributors to follow our [Code of Conduct](CODE_OF_CONDUCT.md) to ensure a respectful and inclusive environment for everyone.

## 🔍 Review Process

1. **Automated Checks**
   - CI/CD pipeline will verify your changes
   - Code style checks
   - Test coverage requirements

2. **Peer Review**
   - At least one maintainer will review your code
   - Address any feedback or comments

3. **Acceptance and Merge**
   - Once approved, a maintainer will merge your changes
   - Your contribution will be included in the next release

## 📄 License

By contributing to Rinna, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).