"""
ReportLab-based PDF report renderer.

This module implements PDF report generation using ReportLab, a powerful
open source tool for creating PDF documents programmatically.
"""

import io
import json
import logging
import os
from typing import Any, Dict, Optional, Tuple

from .renderer import ReportFormat, ReportRenderer, TemplateManager

logger = logging.getLogger(__name__)


class ReportLabRenderer(ReportRenderer):
    """ReportLab-based report renderer."""

    def __init__(self, template_manager: Optional[TemplateManager] = None):
        """
        Initialize the ReportLab renderer.

        Args:
            template_manager: Template manager instance
        """
        super().__init__(template_manager)

        # Create styles directory if it doesn't exist
        self.styles_dir = self.template_manager.templates_dir / "reportlab_styles"
        os.makedirs(self.styles_dir, exist_ok=True)

        # Default style file
        self.default_style_path = self.styles_dir / "default_style.json"
        if not self.default_style_path.exists():
            self._create_default_style()

    def _create_default_style(self) -> None:
        """Create a default style file if none exists."""
        default_style = {
            "pageSize": "A4",
            "pageMargins": [72, 72, 72, 72],  # 1 inch margins
            "defaultFont": "Helvetica",
            "defaultFontSize": 11,
            "titleFont": "Helvetica-Bold",
            "titleFontSize": 24,
            "headingFont": "Helvetica-Bold",
            "heading1FontSize": 20,
            "heading2FontSize": 16,
            "heading3FontSize": 14,
            "colors": {
                "primary": "#224870",
                "secondary": "#366092",
                "background": "#ffffff",
                "text": "#333333",
                "lighterText": "#666666",
                "tableHeader": "#f2f2f2",
                "tableBorder": "#dddddd",
                "tableEvenRow": "#f9f9f9",
            },
            "tables": {
                "headerBackground": "#f2f2f2",
                "headerFont": "Helvetica-Bold",
                "headerFontSize": 11,
                "rowEvenBackground": "#f9f9f9",
                "rowOddBackground": "#ffffff",
                "cellPadding": 8,
                "borderWidth": 0.5,
                "borderColor": "#dddddd",
            },
            "statusColors": {
                "completed": "#dff0d8",
                "in-progress": "#fcf8e3",
                "blocked": "#f2dede",
                "not-started": "#f5f5f5",
            },
            "priorityColors": {
                "high": "#d9534f",
                "medium": "#f0ad4e",
                "low": "#5bc0de",
            },
        }

        with open(self.default_style_path, "w") as f:
            json.dump(default_style, f, indent=2)

        logger.info(
            f"Created default ReportLab style file at {self.default_style_path}"
        )

    def _load_style(self, template_id: str) -> Dict[str, Any]:
        """
        Load style configuration for a template.

        Args:
            template_id: Template identifier

        Returns:
            Style configuration dictionary
        """
        # First load default style
        with open(self.default_style_path, "r") as f:
            style = json.load(f)

        # Check for template-specific style
        custom_style_path = self.styles_dir / f"{template_id}_style.json"
        if custom_style_path.exists():
            try:
                with open(custom_style_path, "r") as f:
                    custom_style = json.load(f)

                # Update style with custom settings
                style.update(custom_style)
                logger.debug(f"Loaded custom style for template {template_id}")
            except (json.JSONDecodeError, IOError) as e:
                logger.warning(f"Error loading custom style for {template_id}: {e}")

        return style

    def _hex_to_rgb(self, hex_color: str) -> Tuple[int, int, int]:
        """
        Convert hex color to RGB tuple.

        Args:
            hex_color: Hex color string (e.g., "#224870")

        Returns:
            RGB tuple (0-255)
        """
        hex_color = hex_color.lstrip("#")
        rgb = tuple(int(hex_color[i : i + 2], 16) for i in (0, 2, 4))
        return (rgb[0], rgb[1], rgb[2])  # Explicitly return a 3-tuple

    def render(
        self,
        template_id: str,
        data: Dict[str, Any],
        output_format: ReportFormat = ReportFormat.PDF,
    ) -> bytes:
        """
        Render a report using ReportLab.

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

        # Load style configuration
        style = self._load_style(template_id)

        # Lazy import ReportLab to avoid dependency if not used
        try:
            from reportlab.lib import colors
            from reportlab.lib.enums import TA_CENTER
            from reportlab.lib.pagesizes import A4, letter
            from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
            from reportlab.lib.units import inch
            from reportlab.platypus import (
                Paragraph,
                SimpleDocTemplate,
                Spacer,
                Table,
                TableStyle,
            )
        except ImportError as import_err:
            raise ValueError(
                "ReportLab is not installed. Install with 'pip install reportlab'"
            ) from import_err

        # Determine page size
        page_size_str = style.get("pageSize", "A4").upper()
        page_size = A4 if page_size_str == "A4" else letter

        # Set up the document
        buffer = io.BytesIO()
        doc = SimpleDocTemplate(
            buffer,
            pagesize=page_size,
            leftMargin=style["pageMargins"][0],
            rightMargin=style["pageMargins"][1],
            topMargin=style["pageMargins"][2],
            bottomMargin=style["pageMargins"][3],
            title=data.get("title", "Report"),
            author=data.get("author", "Rinna"),
            subject=data.get("subject", "Metrics Report"),
            creator="Rinna Reports",
        )

        # Create styles
        styles = getSampleStyleSheet()

        # Add custom styles
        title_style = ParagraphStyle(
            name="CustomTitle",
            parent=styles["Title"],
            fontName=style["titleFont"],
            fontSize=style["titleFontSize"],
            textColor=colors.HexColor(style["colors"]["primary"]),
            alignment=TA_CENTER,
            spaceAfter=30,
        )

        heading1_style = ParagraphStyle(
            name="CustomHeading1",
            parent=styles["Heading1"],
            fontName=style["headingFont"],
            fontSize=style["heading1FontSize"],
            textColor=colors.HexColor(style["colors"]["primary"]),
            spaceAfter=10,
        )

        heading2_style = ParagraphStyle(
            name="CustomHeading2",
            parent=styles["Heading2"],
            fontName=style["headingFont"],
            fontSize=style["heading2FontSize"],
            textColor=colors.HexColor(style["colors"]["secondary"]),
            spaceAfter=8,
        )

        heading3_style = ParagraphStyle(
            name="CustomHeading3",
            parent=styles["Heading3"],
            fontName=style["headingFont"],
            fontSize=style["heading3FontSize"],
            textColor=colors.HexColor(style["colors"]["secondary"]),
            spaceAfter=6,
        )

        normal_style = ParagraphStyle(
            name="CustomNormal",
            parent=styles["Normal"],
            fontName=style["defaultFont"],
            fontSize=style["defaultFontSize"],
            textColor=colors.HexColor(style["colors"]["text"]),
        )

        # Build document content
        content = []

        # Add title
        title_text = data.get("title", "Metrics Report")
        content.append(Paragraph(title_text, title_style))
        content.append(Spacer(1, 0.25 * inch))

        # Add subtitle if present
        if "subtitle" in data:
            subtitle_style = ParagraphStyle(
                name="Subtitle",
                parent=normal_style,
                fontSize=14,
                textColor=colors.HexColor(style["colors"]["lighterText"]),
                alignment=TA_CENTER,
                spaceAfter=20,
            )
            content.append(Paragraph(data["subtitle"], subtitle_style))
            content.append(Spacer(1, 0.25 * inch))

        # Process template-specific content
        # This would normally come from the template file, but for now,
        # we'll implement a generic metrics report structure

        # Add sections for metrics
        if "sections" in data:
            for section in data["sections"]:
                section_title = section.get("title", "")
                content.append(Paragraph(section_title, heading1_style))

                # Add section description if present
                if "description" in section:
                    content.append(Paragraph(section["description"], normal_style))
                    content.append(Spacer(1, 0.15 * inch))

                # Add metrics table if present
                if "metrics" in section:
                    metrics = section["metrics"]
                    table_data = [["Metric", "Value", "Description"]]

                    for metric in metrics:
                        table_data.append(
                            [
                                Paragraph(metric.get("name", ""), heading3_style),
                                Paragraph(str(metric.get("value", "")), normal_style),
                                Paragraph(metric.get("description", ""), normal_style),
                            ]
                        )

                    # Create table
                    metrics_table = Table(
                        table_data, colWidths=[2 * inch, 1 * inch, 3 * inch]
                    )

                    # Style the table
                    table_style = TableStyle(
                        [
                            (
                                "BACKGROUND",
                                (0, 0),
                                (-1, 0),
                                colors.HexColor(style["tables"]["headerBackground"]),
                            ),
                            (
                                "TEXTCOLOR",
                                (0, 0),
                                (-1, 0),
                                colors.HexColor(style["colors"]["primary"]),
                            ),
                            ("ALIGN", (0, 0), (-1, 0), "CENTER"),
                            (
                                "FONTNAME",
                                (0, 0),
                                (-1, 0),
                                style["tables"]["headerFont"],
                            ),
                            (
                                "FONTSIZE",
                                (0, 0),
                                (-1, 0),
                                style["tables"]["headerFontSize"],
                            ),
                            ("BOTTOMPADDING", (0, 0), (-1, 0), 12),
                            (
                                "GRID",
                                (0, 0),
                                (-1, -1),
                                style["tables"]["borderWidth"],
                                colors.HexColor(style["tables"]["borderColor"]),
                            ),
                            ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                            (
                                "PADDING",
                                (0, 0),
                                (-1, -1),
                                style["tables"]["cellPadding"],
                            ),
                        ]
                    )

                    # Add alternating row colors
                    for i in range(1, len(table_data)):
                        if i % 2 == 0:
                            bg_color = style["tables"]["rowEvenBackground"]
                        else:
                            bg_color = style["tables"]["rowOddBackground"]
                        table_style.add(
                            "BACKGROUND", (0, i), (-1, i), colors.HexColor(bg_color)
                        )

                    metrics_table.setStyle(table_style)
                    content.append(metrics_table)
                    content.append(Spacer(1, 0.2 * inch))

                # Add subsections if present
                if "subsections" in section:
                    for subsection in section["subsections"]:
                        subsection_title = subsection.get("title", "")
                        content.append(Paragraph(subsection_title, heading2_style))

                        if "description" in subsection:
                            content.append(
                                Paragraph(subsection["description"], normal_style)
                            )
                            content.append(Spacer(1, 0.1 * inch))

                        # Process subsection content here...
                        if "content" in subsection and isinstance(
                            subsection["content"], str
                        ):
                            content.append(
                                Paragraph(subsection["content"], normal_style)
                            )
                            content.append(Spacer(1, 0.15 * inch))

                content.append(Spacer(1, 0.3 * inch))

        # Build the PDF
        doc.build(content)

        # Get the PDF data
        pdf_data = buffer.getvalue()
        buffer.close()

        if output_format == ReportFormat.PDF:
            return pdf_data
        elif output_format == ReportFormat.PNG:
            # Convert PDF to PNG (first page only)
            try:
                from pdf2image import convert_from_bytes

                output = io.BytesIO()
                images = convert_from_bytes(pdf_data, dpi=300)
                if images:
                    images[0].save(output, format="PNG")
                    return output.getvalue()
                else:
                    raise ValueError("Failed to convert PDF to PNG")
            except ImportError as import_err:
                raise ValueError(
                    "pdf2image is not installed. Install with 'pip install pdf2image'"
                ) from import_err
        else:
            output_type = (
                output_format.value
                if hasattr(output_format, "value")
                else output_format
            )
            raise ValueError(f"Unsupported ReportLab output: {output_type}")
