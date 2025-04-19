# Rinna Documentation Guide

This guide explains how to work with, build, and view the Rinna documentation.

## Documentation System

Rinna uses [Antora](https://antora.org/) to manage its documentation system. Antora is a multi-repository documentation site generator designed for creating a unified documentation site from multiple components.

## Documentation Structure

The documentation is organized into component-specific sections:

```
/
├── docs/                     # Main documentation
│   ├── antora/               # Antora documentation content
│       ├── antora.yml        # Component config
│       └── modules/          # Documentation modules
├── java/docs/                # Java component documentation
│   ├── antora/               # Antora documentation content
├── python/docs/              # Python component documentation
│   ├── antora/               # Antora documentation content
├── go/docs/                  # Go component documentation
│   ├── antora/               # Antora documentation content
└── antora-playbook.yml       # Main site configuration
```

## Building and Viewing the Documentation

To build and view the Rinna documentation:

1. Ensure you have Node.js installed (version 16 or later recommended)

2. From the project root directory, install dependencies:
   ```bash
   npm install
   ```

3. Build the documentation:
   ```bash
   npm run docs
   ```

4. Start the local documentation server:
   ```bash
   npm run docs:serve
   ```

5. Open your browser to [http://localhost:3000](http://localhost:3000)

## Writing Documentation

When contributing documentation:

1. Find the appropriate component:
   - Core project documentation goes in `/docs/antora/`
   - Java-specific documentation goes in `/java/docs/antora/`
   - Python-specific documentation goes in `/python/docs/antora/`
   - Go-specific documentation goes in `/go/docs/antora/`

2. Create or edit AsciiDoc files with the `.adoc` extension in the `modules/ROOT/pages/` directory

3. Update the navigation by editing the `modules/ROOT/nav.adoc` file

## AsciiDoc Basics

Antora uses AsciiDoc format for documentation. Here are some basics:

```asciidoc
= Page Title
:description: Brief description for SEO

== Section Heading

This is a paragraph.

=== Subsection

* List item 1
* List item 2

[source,java]
----
// Code block
public class Example {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
----

TIP: This is a tip admonition.

See the xref:another-page.adoc[Another Page] for more information.
```

## Cross-References

To link between pages:

- Within the same component:
  ```asciidoc
  xref:page.adoc[Link Text]
  ```

- To another component:
  ```asciidoc
  xref:component:page.adoc[Link Text]
  ```

## Troubleshooting

If you encounter issues with the documentation:

- **Missing dependencies**: Run `npm install` from the project root
- **Build errors**: Check your AsciiDoc syntax for errors
- **Pages not found**: Verify the path and filename in your cross-references

For more information, refer to the [Antora Documentation](https://docs.antora.org/).