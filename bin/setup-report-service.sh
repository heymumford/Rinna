#!/bin/bash
# Setup the report rendering service with necessary dependencies

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
REPO_ROOT="$(dirname "${SCRIPT_DIR}")"

# Default settings
INSTALL_DIR="${REPO_ROOT}/python"
TEMPLATES_DIR="${INSTALL_DIR}/rinna/reports/templates"
CONFIG_DIR="${REPO_ROOT}/.rinna-config"
VERBOSE=false

# Text formatting
BOLD="\033[1m"
GREEN="\033[32m"
YELLOW="\033[33m"
RED="\033[31m"
RESET="\033[0m"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --install-dir)
      INSTALL_DIR="$2"
      shift 2
      ;;
    --verbose)
      VERBOSE=true
      shift
      ;;
    --minimal)
      MINIMAL=true
      shift
      ;;
    --help)
      echo "Usage: $0 [OPTIONS]"
      echo "Setup the report rendering service"
      echo
      echo "Options:"
      echo "  --install-dir DIR     Installation directory (default: ./python)"
      echo "  --verbose             Show more output"
      echo "  --minimal             Install minimal dependencies"
      echo "  --help                Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Run '$0 --help' for usage information"
      exit 1
      ;;
  esac
done

log() {
  echo -e "${GREEN}==>${RESET} $1"
}

log_progress() {
  if [[ "${VERBOSE}" == "true" ]]; then
    echo -e "${YELLOW}-->${RESET} $1"
  fi
}

log_error() {
  echo -e "${RED}Error:${RESET} $1" >&2
}

# Create required directories
log "Creating report service directories"
mkdir -p "${TEMPLATES_DIR}/css"
mkdir -p "${CONFIG_DIR}/reports"

# Check Python environment
log "Checking Python environment"
if ! command -v python3 &> /dev/null; then
  log_error "Python 3 is not installed"
  exit 1
fi

if ! command -v pip3 &> /dev/null; then
  log_error "pip is not installed"
  exit 1
fi

# Install dependencies
log "Installing Python dependencies"
if [[ "${MINIMAL}" == "true" ]]; then
  log_progress "Installing minimal dependencies"
  pip3 install --user weasyprint jinja2 reportlab pillow
else
  log_progress "Installing full dependencies"
  pip3 install --user -r "${REPO_ROOT}/requirements.txt"
fi

# Copy sample templates if they don't exist
if [[ ! -f "${TEMPLATES_DIR}/metrics_default.html" ]]; then
  log "Installing sample templates"
  
  if [[ -f "${REPO_ROOT}/python/rinna/reports/templates/metrics_default.html" ]]; then
    cp "${REPO_ROOT}/python/rinna/reports/templates/metrics_default.html" "${TEMPLATES_DIR}/"
    log_progress "Installed metrics_default.html template"
  else
    log_progress "Creating default template"
    cat > "${TEMPLATES_DIR}/metrics_default.html" << EOL
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>{{ title }}</title>
    <style>
        /* Styles are defined in the CSS files */
    </style>
</head>
<body>
    <!-- Cover page -->
    <div class="cover">
        <h1>{{ title }}</h1>
        <div class="subtitle">{{ subtitle }}</div>
        <div class="date">Generated: {{ timestamp }}</div>
    </div>
    
    <!-- Content pages -->
    {% for section in sections %}
    <section>
        <h1>{{ section.title }}</h1>
        
        {% if section.description %}
        <p>{{ section.description }}</p>
        {% endif %}
        
        {% if section.metrics %}
        <div class="metrics-card">
            <div class="metrics-grid">
                {% for metric in section.metrics %}
                <div class="metric-item">
                    <div class="metric-title">{{ metric.name }}</div>
                    <div class="metric-value">{{ metric.value }}</div>
                    {% if metric.description %}
                    <div class="metric-description">{{ metric.description }}</div>
                    {% endif %}
                </div>
                {% endfor %}
            </div>
        </div>
        
        <table>
            <thead>
                <tr>
                    <th>Metric</th>
                    <th>Value</th>
                    {% if section.metrics[0].description %}
                    <th>Description</th>
                    {% endif %}
                </tr>
            </thead>
            <tbody>
                {% for metric in section.metrics %}
                <tr>
                    <td>{{ metric.name }}</td>
                    <td>{{ metric.value }}</td>
                    {% if metric.description %}
                    <td>{{ metric.description }}</td>
                    {% endif %}
                </tr>
                {% endfor %}
            </tbody>
        </table>
        {% endif %}
    </section>
    {% endfor %}
</body>
</html>
EOL
  fi
  
  # Create default CSS
  if [[ ! -f "${TEMPLATES_DIR}/css/default.css" ]]; then
    log_progress "Creating default CSS"
    mkdir -p "${TEMPLATES_DIR}/css"
    
    cat > "${TEMPLATES_DIR}/css/default.css" << EOL
/* Default CSS for PDF reports */
@page {
    margin: 1cm;
    @top-center {
        content: string(title);
        font-size: 9pt;
        color: #666;
    }
    @bottom-right {
        content: "Page " counter(page) " of " counter(pages);
        font-size: 9pt;
        color: #666;
    }
}

body {
    font-family: Helvetica, Arial, sans-serif;
    font-size: 11pt;
    line-height: 1.4;
    color: #333;
}

h1 {
    color: #224870;
    font-size: 20pt;
    margin-top: 1cm;
    margin-bottom: 0.5cm;
    string-set: title content();
    page-break-before: always;
}

h1:first-of-type {
    page-break-before: avoid;
}

h2 {
    color: #224870;
    font-size: 16pt;
    margin-top: 0.8cm;
    margin-bottom: 0.3cm;
}

table {
    width: 100%;
    border-collapse: collapse;
    margin: 0.5cm 0;
}

th, td {
    border: 1px solid #ddd;
    padding: 8px;
    text-align: left;
}

th {
    background-color: #f2f2f2;
    color: #224870;
}

.cover {
    height: 100vh;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    text-align: center;
}

.cover h1 {
    font-size: 28pt;
    color: #224870;
    margin-bottom: 1cm;
}
EOL
  fi
fi

# Create configuration
log "Creating configuration"
cat > "${CONFIG_DIR}/reports/config.json" << EOL
{
  "templates_dir": "${TEMPLATES_DIR}",
  "output_dir": "${CONFIG_DIR}/reports/output",
  "default_engine": "weasyprint",
  "available_engines": ["weasyprint", "reportlab", "xhtml2pdf"],
  "docmosis_enabled": false
}
EOL

mkdir -p "${CONFIG_DIR}/reports/output"

# Create catalog file for templates
if [[ ! -f "${TEMPLATES_DIR}/catalog.json" ]]; then
  log "Creating template catalog"
  
  cat > "${TEMPLATES_DIR}/catalog.json" << EOL
{
  "templates": [
    {
      "id": "metrics_default",
      "path": "metrics_default.html",
      "title": "Metrics Report",
      "description": "Default metrics report template",
      "engine": "weasyprint"
    }
  ]
}
EOL
fi

log "Report service setup complete!"
echo
echo "To run the report service:"
echo "  ${SCRIPT_DIR}/run-report-service.sh"
echo
echo "To test the report service:"
echo "  curl http://localhost:5001/api/v1/metrics/sample | curl -X POST -H 'Content-Type: application/json' -d @- http://localhost:5001/api/v1/metrics/reports -o report.pdf"
echo 
echo "Default template installed at: ${TEMPLATES_DIR}/metrics_default.html"
echo "Default CSS installed at: ${TEMPLATES_DIR}/css/default.css"
echo "Configuration created at: ${CONFIG_DIR}/reports/config.json"