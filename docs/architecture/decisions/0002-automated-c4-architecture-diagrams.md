# 2. Automated C4 Architecture Diagrams

Date: 2025-04-06

## Status

Accepted

## Context

Architecture documentation is critical for understanding the system structure, but it commonly falls out of sync with the actual implementation. Developers often prioritize working code over documentation, leading to outdated diagrams that no longer represent the current architecture.

Common issues with architecture diagrams include:
1. Manual creation and maintenance is time-consuming
2. Diagrams become outdated as the codebase evolves
3. Diagrams may use inconsistent notations and styles
4. Documentation lives separately from the codebase
5. Teams may not have a shared understanding of the architecture

For the Rinna project, we need a sustainable approach to architecture documentation that:
- Stays up-to-date with the codebase
- Follows a consistent modeling approach
- Is easily accessible to all team members
- Can be generated automatically as part of the build
- Represents multiple levels of architectural abstraction

## Decision

We will adopt the C4 model for architecture documentation with an automated generation approach that integrates with our build process.

The C4 model (Context, Containers, Components, Code) provides four levels of increasing detail:
- Context diagrams show the system and its relationships with users and external systems
- Container diagrams show high-level technical components (applications, data stores, etc.)
- Component diagrams show how containers are composed of components
- Code diagrams show how components are implemented as code

Implementation details:
1. We will use the Python diagrams library to generate C4 model diagrams
2. Diagrams will be automatically generated during the Maven build process
3. Diagram generation will run asynchronously by default to avoid slowing down builds
4. Diagrams will be stored in the `docs/diagrams` directory as SVG files
5. A dedicated documentation page will be generated with the diagrams
6. A Maven profile `diagrams` will allow explicit diagram regeneration
7. Diagram generation will be integrated with cross-language tests

## Consequences

Positive consequences:
1. Architecture documentation will stay in sync with the codebase
2. Developers don't need to manually create and update diagrams
3. All team members have access to consistent, up-to-date architecture documentation
4. Multiple levels of abstraction help different stakeholders understand the system
5. SVG format ensures high-quality diagrams can be viewed directly in browsers and documentation
6. Diagrams become a natural part of the development process

Challenges and mitigations:
1. Automated diagrams may not capture all nuances of the architecture
   - Solution: The diagram generation script can be extended with more detailed representations
2. Diagram generation could potentially slow down the build process
   - Solution: We run diagram generation asynchronously by default
3. The Python dependencies add complexity to the build environment
   - Solution: Graceful degradation if dependencies are missing, with clear installation instructions
4. CI/CD systems need to handle diagram generation
   - Solution: The script detects CI environments and adjusts accordingly

## Implementation

Key implementation details:
- `bin/generate-diagrams.sh` script handles diagram generation with options for synchronous/async modes
- A Maven profile `diagrams` is available for explicit diagram generation
- Cross-language testing now includes diagram generation and testing
- Default builds include asynchronous diagram generation
- Diagrams are documented in the architecture documentation with links
- ADRs capture significant architectural decisions

The diagrams will be automatically updated when the architecture changes, ensuring that the documentation accurately reflects the current system structure.