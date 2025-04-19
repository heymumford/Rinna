# Rinna Document Generation

Rinna includes a powerful document generation system that can create beautiful reports, exports, and other documents from your workflow data. This document explains how to use this feature.

## Document Generation Features

Rinna's document generation system:

1. Supports multiple output formats (PDF, DOCX, HTML)
2. Uses customizable templates 
3. Offers a flexible API for programmatic document generation
4. Includes Docmosis integration for professional document generation (optional)
5. Provides fallback generators when Docmosis is not available

## Using Docmosis for Enhanced Documents

For high-quality document generation, Rinna supports [Docmosis](https://www.docmosis.com/), a powerful template-based document generation engine. Docmosis allows you to:

- Create beautiful documents with complex formatting
- Use Word templates with special tagging for dynamic content
- Generate documents with tables, charts, and images
- Control page breaks and document sections

**Note**: Docmosis is optional and requires a license. If you don't have a Docmosis license, Rinna will use its built-in document generators.

## Setting Up Docmosis

To use Docmosis with Rinna, follow these steps:

1. Purchase a Docmosis license from [https://www.docmosis.com/](https://www.docmosis.com/)
2. Set your license key in Rinna:

```bash
./bin/rin doc license YOUR_LICENSE_KEY_HERE
```

3. Create Word templates with Docmosis tags
4. Place templates in the `rinna-core/src/main/resources/templates` directory

## Document Command Reference

```bash
# View document configuration
./bin/rin doc config

# Set Docmosis license key
./bin/rin doc license YOUR_LICENSE_KEY_HERE

# View masked license key
./bin/rin doc license-show

# List available templates
./bin/rin doc templates
```

## Creating Templates for Docmosis

Docmosis uses a straightforward template system based on Microsoft Word documents. To create a template:

1. Create a Word document (.docx) with your desired formatting
2. Add template fields using special tags:
   - `<<fieldName>>` - Simple field
   - `<<fieldName:expression>>` - Field with expression
   - `<<rs_tableName>>` and `<<re_tableName>>` - Table row start/end
   - `<<cs_condition>>` and `<<ce_condition>>` - Conditional section start/end

Example template tags for work items:

```
Work Item: <<itemId>> - <<title>>
Type: <<type>>
Status: <<status>>
Priority: <<priority>>
Assigned to: <<assignee>>
Created: <<createdAt>>
Updated: <<updatedAt>>

Description:
<<description>>
```

For more details on Docmosis templates, see the [Docmosis Template Guide](https://www.docmosis.com/resources/docmosis-templates.html).

## Using the Built-in Document Generator

When Docmosis is not available, Rinna uses a built-in document generator based on Apache POI (for DOCX), PDFBox (for PDF), and direct HTML generation. While it lacks the advanced formatting capabilities of Docmosis, it provides a functional alternative for basic document generation needs.

## Programmatic Document Generation

You can also generate documents programmatically using the DocumentService API:

```java
import org.rinna.config.RinnaConfig;
import org.rinna.domain.usecase.DocumentService;
import org.rinna.domain.entity.WorkItem;

// Get the document service
RinnaConfig config = new RinnaConfig();
DocumentService docService = config.getDocumentService();

// Generate a document
try (FileOutputStream output = new FileOutputStream("work_item_report.pdf")) {
    docService.generateWorkItemDocument(
        workItem,
        DocumentService.Format.PDF,
        DocumentService.TemplateType.WORKITEM_DETAILS,
        output
    );
}
```

The service will automatically use Docmosis if available, or fall back to the built-in generators if not.