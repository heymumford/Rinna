"""
WeasyPrint-based PDF report renderer.

This module implements PDF report generation using WeasyPrint, a powerful
open source tool for converting HTML/CSS to PDF.
"""

import io
import os
import logging
from pathlib import Path
from typing import Any, Dict, Optional, Union

from jinja2 import Environment, FileSystemLoader

from .renderer import ReportFormat, ReportRenderer, TemplateManager

logger = logging.getLogger(__name__)


class WeasyPrintRenderer(ReportRenderer):
    """WeasyPrint-based report renderer."""
    
    def __init__(self, template_manager: Optional[TemplateManager] = None):
        """
        Initialize the WeasyPrint renderer.
        
        Args:
            template_manager: Template manager instance
        """
        super().__init__(template_manager)
        
        # Set up Jinja2 template environment
        self.jinja_env = Environment(
            loader=FileSystemLoader(str(self.template_manager.templates_dir)),
            autoescape=True
        )
        
        # Load CSS
        self.css_dir = self.template_manager.templates_dir / "css"
        os.makedirs(self.css_dir, exist_ok=True)
        
        # Default CSS file
        self.default_css_path = self.css_dir / "default.css"
        if not self.default_css_path.exists():
            self._create_default_css()
    
    def _create_default_css(self) -> None:
        """Create a default CSS file if none exists."""
        default_css = """
        /* Default CSS for WeasyPrint PDF reports */
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
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
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
            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
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
        
        logger.info(f"Created default CSS file at {self.default_css_path}")
    
    def render(
        self,
        template_id: str,
        data: Dict[str, Any],
        output_format: ReportFormat = ReportFormat.PDF
    ) -> bytes:
        """
        Render a report using WeasyPrint.
        
        Args:
            template_id: Template identifier
            data: Data to render in the template
            output_format: Output format
            
        Returns:
            The rendered report as bytes
            
        Raises:
            ValueError: If template not found or rendering fails
        """
        # Lazy import WeasyPrint to avoid dependency if not used
        try:
            from weasyprint import HTML, CSS
        except ImportError:
            raise ValueError(
                "WeasyPrint is not installed. Install with 'pip install weasyprint'"
            )
        
        # Get template
        template = self.template_manager.get_template(template_id)
        if not template:
            raise ValueError(f"Template not found: {template_id}")
        
        if not template.exists:
            raise ValueError(f"Template file does not exist: {template.path}")
        
        # Generate HTML from template
        try:
            jinja_template = self.jinja_env.get_template(template.path.name)
            html_content = jinja_template.render(**data)
        except Exception as e:
            logger.error(f"Error rendering template {template_id}: {e}")
            raise ValueError(f"Failed to render template {template_id}: {e}")
        
        # Apply CSS
        css_files = [self.default_css_path]
        custom_css = self.css_dir / f"{template_id}.css"
        if custom_css.exists():
            css_files.append(custom_css)
        
        # Load CSS
        css_list = [CSS(filename=str(css_file)) for css_file in css_files]
        
        # Generate output based on format
        output = io.BytesIO()
        
        if output_format == ReportFormat.PDF:
            # Generate PDF
            HTML(string=html_content).write_pdf(
                output,
                stylesheets=css_list
            )
        elif output_format == ReportFormat.HTML:
            # Return HTML directly
            output.write(html_content.encode("utf-8"))
        elif output_format == ReportFormat.PNG:
            # For PNG, we need to render to a PDF then convert the first page
            from PIL import Image
            from pdf2image import convert_from_bytes
            
            temp_pdf = io.BytesIO()
            HTML(string=html_content).write_pdf(temp_pdf, stylesheets=css_list)
            temp_pdf.seek(0)
            
            images = convert_from_bytes(temp_pdf.read(), dpi=300)
            if images:
                images[0].save(output, format="PNG")
            else:
                raise ValueError("Failed to convert PDF to PNG")
        else:
            raise ValueError(f"Unsupported output format for WeasyPrint: {output_format}")
        
        return output.getvalue()