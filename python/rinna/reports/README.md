# Rinna Report Generation Module

This module provides PDF report generation functionality for the Rinna project using open source libraries.

## Features

- Multiple rendering engines: WeasyPrint, ReportLab, and XHTML2PDF
- Template-based report generation using Jinja2
- High-quality professional templates for metrics reports
- RESTful API endpoints for report generation
- Container-based deployment

## Quick Start

### Setup

Run the setup script to install dependencies and create sample templates:

```bash
./bin/setup-report-service.sh
```

For a minimal installation:

```bash
./bin/setup-report-service.sh --minimal
```

### Running the Service

Start the report generation service with:

```bash
./bin/run-report-service.sh
```

Options:
- `--port PORT`: Port to run the service on (default: 5001)
- `--log-level LEVEL`: Log level (default: info)
- `--reports-dir DIR`: Directory to store reports (default: ./reports)
- `--dev`: Run in development mode

### Using with Docker Compose

```bash
docker-compose -f python/docker-compose.yml up -d report-service
```

## API Endpoints

### Health Check

```
GET /health
```

### Generate a Metrics Report

```
POST /api/v1/metrics/reports
```

Request body:
```json
{
  "title": "Monthly Metrics Report",
  "metrics": {
    "summary": {
      "Total Work Items": 127,
      "Completed Items": 89,
      "In Progress": 32
    },
    "workflow_metrics": {
      "workflow_efficiency": 0.87,
      "average_transition_time": 1.2
    }
  },
  "format": "pdf",
  "engine": "weasyprint"
}
```

### List Available Templates

```
GET /api/v1/reports/templates
```

### Get Sample Metrics Data

```
GET /api/v1/metrics/sample
```

## Rendering Engines

### WeasyPrint (Default)

[WeasyPrint](https://weasyprint.org/) is a visual rendering engine for HTML and CSS that can export to PDF. It's based on Pango, Cairo, and CFfi.

Advantages:
- High-quality PDF output
- Excellent CSS support
- Good typography and layout capabilities
- Active development

### ReportLab

[ReportLab](https://www.reportlab.com/opensource/) is the primary open source PDF library for Python. It's a robust and proven solution for programmatically creating PDFs.

Advantages:
- Very mature and stable
- Precise control over PDF output
- Excellent for complex layouts
- High performance

### XHTML2PDF

[XHTML2PDF](https://github.com/xhtml2pdf/xhtml2pdf) is a converter that converts HTML and CSS to PDF documents using ReportLab.

Advantages:
- Simpler installation than WeasyPrint on some platforms
- Good for simpler reports
- Faster rendering for basic documents

## Optional Docmosis Integration

This module includes a stub for [Docmosis](https://www.docmosis.com/) integration, which can be enabled by:

1. Create a `~/.docmosis/docmosis.properties` file with your Docmosis credentials:
   ```
   docmosis.key=your-license-key
   docmosis.site=https://your-docmosis-site
   ```

2. Set the `docmosis_enabled` flag to true in the configuration file.

## Templates

Templates are stored in the `templates` directory and use Jinja2 syntax with HTML and CSS. 

The default template catalog is in `templates/catalog.json` and defines available templates.

## Customization

### Adding a New Template

1. Create an HTML template file in the `templates` directory
2. Create a CSS file in the `templates/css` directory (optional)
3. Add the template to the catalog in `templates/catalog.json`

### Adding a New Rendering Engine

1. Create a new renderer class that extends `ReportRenderer`
2. Implement the `render` method
3. Register the renderer in `renderer.py`

## Development

### Running Tests

```bash
docker-compose -f python/docker-compose.yml run report-tests
```

### Building the Container

```bash
docker-compose -f python/docker-compose.yml build report-service
```