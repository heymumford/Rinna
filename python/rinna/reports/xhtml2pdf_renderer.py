"""
XHTML2PDF-based PDF report renderer.

This module implements PDF report generation using XHTML2PDF, an open source
HTML to PDF converter that is simpler to install than WeasyPrint on some systems.
"""

import io
import logging
import os
from pathlib import Path
from typing import Any, Dict, Optional, Union

from jinja2 import Environment, FileSystemLoader

from .renderer import ReportFormat, ReportRenderer, TemplateManager

logger = logging.getLogger(__name__)


class XHTML2PDFRenderer(ReportRenderer):
    """XHTML2PDF-based report renderer."""
    
    def __init__(self, template_manager: Optional[TemplateManager] = None):
        """
        Initialize the XHTML2PDF renderer.
        
        Args:
            template_manager: Template manager instance
        """
        super().__init__(template_manager)
        
        # Set up Jinja2 template environment
        self.jinja_env = Environment(
            loader=FileSystemLoader(str(self.template_manager.templates_dir)),
            autoescape=True
        )
        
        # Create CSS directory
        self.css_dir = self.template_manager.templates_dir / "css"
        os.makedirs(self.css_dir, exist_ok=True)
        
        # Default CSS file for XHTML2PDF
        self.default_css_path = self.css_dir / "xhtml2pdf_default.css"
        if not self.default_css_path.exists():
            self._create_default_css()
    
    def _create_default_css(self) -> None:
        """Create a default CSS file for XHTML2PDF if none exists."""
        default_css = """
        /* Default CSS for XHTML2PDF */
        @page {
            size: A4;
            margin: 1cm;
            @frame header {
                -pdf-frame-content: headerContent;
                top: 0.5cm;
                margin-left: 1cm;
                margin-right: 1cm;
                height: 1cm;
            }
            @frame footer {
                -pdf-frame-content: footerContent;
                bottom: 0.5cm;
                margin-left: 1cm;
                margin-right: 1cm;
                height: 1cm;
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
        
        h3 {
            color: #366092;
            font-size: 13pt;
            margin-top: 0.6cm;
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
        
        tr:nth-child(even) {
            background-color: #f9f9f9;
        }
        
        .chart {
            page-break-inside: avoid;
            margin: 0.5cm 0;
        }
        
        .metrics-card {
            border: 1px solid #ddd;
            border-radius: 5px;
            padding: 15px;
            margin: 0.5cm 0;
            page-break-inside: avoid;
        }
        
        .metrics-card h3 {
            margin-top: 0;
            color: #224870;
        }
        
        .metrics-grid {
            display: flex;
            flex-wrap: wrap;
            gap: 15px;
        }
        
        .metric-item {
            flex: 1 0 calc(33% - 15px);
            min-width: 200px;
            background-color: #f8f9fa;
            border-radius: 5px;
            padding: 10px;
        }
        
        .metric-title {
            font-size: 12pt;
            color: #666;
            margin-bottom: 5px;
        }
        
        .metric-value {
            font-size: 18pt;
            font-weight: bold;
            color: #224870;
        }
        
        .metric-description {
            font-size: 9pt;
            color: #888;
            margin-top: 5px;
        }
        
        /* Cover page styling */
        .cover {
            text-align: center;
            padding-top: 5cm;
            height: 25cm;
        }
        
        .cover h1 {
            font-size: 28pt;
            color: #224870;
            margin-bottom: 1cm;
        }
        
        .cover .subtitle {
            font-size: 16pt;
            color: #666;
            margin-bottom: 2cm;
        }
        
        .cover .date {
            font-size: 14pt;
            color: #333;
        }
        
        /* Status indicators */
        .status {
            display: inline-block;
            padding: 3px 8px;
            border-radius: 3px;
            font-weight: bold;
        }
        
        .status-completed {
            background-color: #dff0d8;
            color: #3c763d;
        }
        
        .status-in-progress {
            background-color: #fcf8e3;
            color: #8a6d3b;
        }
        
        .status-blocked {
            background-color: #f2dede;
            color: #a94442;
        }
        
        .status-not-started {
            background-color: #f5f5f5;
            color: #777;
        }
        
        /* Priority indicators */
        .priority {
            display: inline-block;
            width: 20px;
            height: 20px;
            border-radius: 50%;
            margin-right: 5px;
        }
        
        .priority-high {
            background-color: #d9534f;
        }
        
        .priority-medium {
            background-color: #f0ad4e;
        }
        
        .priority-low {
            background-color: #5bc0de;
        }
        """
        
        with open(self.default_css_path, "w") as f:
            f.write(default_css)
        
        logger.info(f"Created default XHTML2PDF CSS file at {self.default_css_path}")
    
    def render(
        self,
        template_id: str,
        data: Dict[str, Any],
        output_format: ReportFormat = ReportFormat.PDF
    ) -> bytes:
        """
        Render a report using XHTML2PDF.
        
        Args:
            template_id: Template identifier
            data: Data to render in the template
            output_format: Output format
            
        Returns:
            The rendered report as bytes
            
        Raises:
            ValueError: If template not found or rendering fails
        """
        # Get template
        template = self.template_manager.get_template(template_id)
        if not template:
            raise ValueError(f"Template not found: {template_id}")
        
        if not template.exists:
            raise ValueError(f"Template file does not exist: {template.path}")
        
        # Generate HTML from template
        try:
            jinja_template = self.jinja_env.get_template(template.path.name)
            
            # Add title and header/footer for PDF generation
            html_data = dict(data)
            html_data["report_title"] = data.get("title", "Report")
            
            html_content = jinja_template.render(**html_data)
            
            # Add XHTML2PDF-specific headers and footers
            if output_format == ReportFormat.PDF:
                # Add header and footer divs if not present
                if "<div id=\"headerContent\">" not in html_content:
                    header = f"""
                    <div id="headerContent" style="text-align: right; font-size: 9pt; color: #666;">
                        {html_data.get("report_title", "Report")}
                    </div>
                    """
                    footer = """
                    <div id="footerContent" style="text-align: right; font-size: 9pt; color: #666;">
                        Page <pdf:pagenumber> of <pdf:pagecount>
                    </div>
                    """
                    # Insert after <body>
                    if "<body>" in html_content:
                        html_content = html_content.replace("<body>", f"<body>{header}{footer}")
            
            # Add stylesheet reference
            if output_format == ReportFormat.PDF:
                css_path_str = f'<link rel="stylesheet" href="{self.default_css_path}">'
                
                # Check for custom CSS
                custom_css = self.css_dir / f"{template_id}_xhtml2pdf.css"
                if custom_css.exists():
                    css_path_str += f'\n<link rel="stylesheet" href="{custom_css}">'
                
                # Add CSS to head if not already present
                if "<head>" in html_content and css_path_str not in html_content:
                    html_content = html_content.replace("<head>", f"<head>\n{css_path_str}")
            
        except Exception as e:
            logger.error(f"Error rendering template {template_id}: {e}")
            raise ValueError(f"Failed to render template {template_id}: {e}")
        
        # Generate output based on format
        output = io.BytesIO()
        
        if output_format == ReportFormat.PDF:
            # Lazy import to avoid dependency if not used
            try:
                import xhtml2pdf.pisa as pisa
            except ImportError:
                raise ValueError(
                    "XHTML2PDF is not installed. Install with 'pip install xhtml2pdf'"
                )
            
            # Convert HTML to PDF
            pisa_status = pisa.CreatePDF(
                html_content,
                dest=output,
                path=str(self.template_manager.templates_dir)
            )
            
            if pisa_status.err:
                raise ValueError(f"XHTML2PDF error: {pisa_status.err}")
            
        elif output_format == ReportFormat.HTML:
            # Return HTML directly
            output.write(html_content.encode("utf-8"))
            
        else:
            raise ValueError(f"Unsupported output format for XHTML2PDF: {output_format}")
        
        return output.getvalue()