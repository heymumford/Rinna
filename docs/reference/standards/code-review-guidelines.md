# Code Review Guidelines

This document was developed with analytical assistance from AI tools including Claude 3.7 Sonnet, Claude Code, and Google Gemini Deep Research, which were used as paid services. All intellectual property rights remain exclusively with the copyright holder Eric C. Mumford (@heymumford). Licensed under the Mozilla Public License 2.0.

## Overview

Code reviews are a critical part of our development process. They help maintain code quality, share knowledge across the team, and catch issues before they reach production. This document establishes a consistent approach to code reviews for the Rinna project.

## Principles

1. **Respectful Communication**: Reviews should be constructive, focusing on the code, not the author
2. **Knowledge Sharing**: Reviews are a learning opportunity for both reviewer and author
3. **Incremental Improvement**: Each review should leave the codebase better than before
4. **Collective Ownership**: The team shares responsibility for all code in the repository
5. **Timely Feedback**: Reviews should be completed promptly to avoid blocking progress

## Process

### Before Submitting a PR

- [ ] Run all tests locally to ensure they pass
- [ ] Ensure code adheres to project style guidelines
- [ ] Run linters and static analysis tools
- [ ] Self-review your code
- [ ] Add appropriate documentation
- [ ] Keep PRs focused and reasonably sized (under 400 lines when possible)

### Submitting a PR

1. Create a PR with a clear title and description
2. Link to any relevant issues or tickets
3. Explain the purpose and approach of your changes
4. Highlight any areas where you specifically want feedback
5. Assign appropriate reviewers (at least 2 recommended)

### Reviewing Code

#### What to Look For

1. **Correctness**: Does the code work as intended?
2. **Test Coverage**: Are there adequate tests?
3. **Security**: Are there potential security issues?
4. **Performance**: Could there be performance problems?
5. **Readability**: Is the code clear and maintainable?
6. **Architecture**: Does the code follow project design patterns?
7. **Error Handling**: Are errors handled properly?
8. **Edge Cases**: Are potential edge cases addressed?

#### Review Checklist

- [ ] Code compiles and all tests pass
- [ ] Code follows project style and naming conventions
- [ ] No obvious bugs or logic errors
- [ ] No security vulnerabilities
- [ ] No potential performance issues
- [ ] Appropriate error handling
- [ ] Documentation is updated
- [ ] No unnecessary code duplication
- [ ] No overly complex methods (consider cyclomatic complexity)
- [ ] Changes are relevant to the PR and focused on its purpose

### Responding to Reviews

1. Respond to all comments, even if just acknowledging them
2. If you disagree with feedback, explain your reasoning
3. Make requested changes or justify why they shouldn't be made
4. Mark comments as resolved when addressed
5. For larger discussions, consider taking them offline then summarizing in the PR

### Approval and Merging

1. At least 2 approvals required before merging
2. All CI checks must pass
3. All required changes must be addressed
4. The PR author is typically responsible for merging after approval
5. Squash commits when appropriate for a clean history

## Best Practices

### For Reviewers

1. **Be Timely**: Review PRs within 1 business day when possible
2. **Be Thorough**: Take your time and pay attention to detail
3. **Be Kind**: Phrase feedback constructively and positively
4. **Be Specific**: Explain the reasoning behind your suggestions
5. **Prioritize Issues**: Focus on important problems over minor style issues
6. **Provide Solutions**: When possible, suggest fixes rather than just pointing out problems
7. **Consider Context**: Understand the purpose of the changes before reviewing

### For Authors

1. **Provide Context**: Explain what the PR is trying to achieve
2. **Be Open**: Accept feedback as a means to improve the code
3. **Break It Down**: Keep PRs focused and reasonably sized
4. **Self-Review**: Review your own code first to catch obvious issues
5. **Respond Promptly**: Address review comments in a timely manner
6. **Learn and Apply**: Use feedback to improve future submissions

## Comment Etiquette

### Constructive Comments

- Use "we" instead of "you" to foster collective ownership
- Ask questions rather than making demands
- Explain the reasoning behind your suggestions
- Provide concrete examples when possible
- Focus on the code, not the author

#### Examples:

✅ "We might want to add a null check here to prevent a potential NPE."
✅ "Could we consider using a StringBuilder here for better performance?"
✅ "I'm wondering if this loop could be simplified with a stream operation?"

### Unconstructive Comments

- Avoid sarcasm or dismissive language
- Don't use absolutist terms like "never" or "always"
- Refrain from questioning the author's competence
- Avoid vague criticisms without suggestions

#### Examples:

❌ "This code is a mess."
❌ "Why would you do it this way?"
❌ "You should know better than to use this pattern."

## Specialized Review Areas

### Security Reviews

- Authentication and authorization checks
- Input validation and sanitization
- Proper handling of sensitive data
- Protection against common vulnerabilities (OWASP Top 10)
- Secure configuration

### Performance Reviews

- Database query efficiency
- Algorithm complexity
- Memory usage and potential leaks
- Resource management
- Caching opportunities

### Accessibility Reviews

- Semantic HTML usage
- ARIA attributes where appropriate
- Keyboard navigation support
- Color contrast and text sizing
- Screen reader compatibility

## Tools and Resources

### Static Analysis

- PMD for Java code
- ESLint for JavaScript
- Checkstyle for formatting
- SpotBugs for potential bugs

### CI Integration

- Automated tests run on all PRs
- Code coverage reports
- Static analysis results
- Performance benchmarks when applicable

## Conclusion

Effective code reviews are essential for maintaining a high-quality codebase. By following these guidelines, we can ensure that our review process is efficient, constructive, and beneficial for the entire team.