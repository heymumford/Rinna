"""
Report rendering functionality using open source libraries.

This module provides classes and functions for generating PDF reports
with high-quality templates using open source tools like WeasyPrint,
ReportLab, or xhtml2pdf.
"""

import json
import logging
import os
from abc import ABC, abstractmethod
from enum import Enum
from pathlib import Path
from typing import Any, BinaryIO, Dict, List, Optional, Union

# Type stubs for external dependencies that might be missing
# This is handled through pyproject.toml configuration for mypy

# Default templates directory relative to this file
DEFAULT_TEMPLATES_DIR = Path(__file__).parent / "templates"

# Configure logging
logger = logging.getLogger(__name__)


class ReportFormat(str, Enum):
    """Supported report formats."""
    PDF = "pdf"
    HTML = "html"
    DOCX = "docx"
    PNG = "png"


class RenderingEngine(str, Enum):
    """Available rendering engines."""
    WEASYPRINT = "weasyprint"
    REPORTLAB = "reportlab"
    XHTML2PDF = "xhtml2pdf"
    DOCMOSIS = "docmosis"  # Optional proprietary engine


class ReportTemplate:
    """Represents a report template."""
    
    def __init__(
        self,
        template_id: str,
        path: Union[str, Path],
        title: str,
        description: str,
        engine: RenderingEngine,
        metadata: Optional[Dict[str, Any]] = None
    ):
        """
        Initialize a report template.
        
        Args:
            template_id: Unique identifier for the template
            path: Path to the template file
            title: Human-readable title
            description: Template description
            engine: Rendering engine to use
            metadata: Additional template metadata
        """
        self.template_id = template_id
        self.path = Path(path)
        self.title = title
        self.description = description
        self.engine = engine
        self.metadata = metadata or {}
        
    def __repr__(self) -> str:
        return f"ReportTemplate(id='{self.template_id}', engine={self.engine})"
    
    @property
    def exists(self) -> bool:
        """Check if the template file exists."""
        return self.path.exists()


class TemplateManager:
    """Manages report templates."""
    
    def __init__(self, templates_dir: Optional[Union[str, Path]] = None):
        """
        Initialize the template manager.
        
        Args:
            templates_dir: Directory containing templates. If None, uses default.
        """
        self.templates_dir = Path(templates_dir) if templates_dir else DEFAULT_TEMPLATES_DIR
        self.templates: Dict[str, ReportTemplate] = {}
        
        # Create templates directory if it doesn't exist
        os.makedirs(self.templates_dir, exist_ok=True)
        
        # Load templates catalog if it exists
        self._load_catalog()
        
    def _load_catalog(self) -> None:
        """Load the templates catalog from templates directory."""
        catalog_path = self.templates_dir / "catalog.json"
        
        if not catalog_path.exists():
            logger.info(f"Templates catalog not found at {catalog_path}")
            return
        
        try:
            with open(catalog_path, "r") as f:
                catalog = json.load(f)
                
            for template_data in catalog.get("templates", []):
                template_id = template_data.get("id")
                if not template_id:
                    logger.warning("Skipping template without ID")
                    continue
                    
                path = self.templates_dir / template_data.get("path", "")
                engine = template_data.get("engine", RenderingEngine.WEASYPRINT)
                
                self.templates[template_id] = ReportTemplate(
                    template_id=template_id,
                    path=path,
                    title=template_data.get("title", template_id),
                    description=template_data.get("description", ""),
                    engine=engine,
                    metadata=template_data.get("metadata", {})
                )
                
            logger.info(f"Loaded {len(self.templates)} templates from catalog")
            
        except (json.JSONDecodeError, IOError) as e:
            logger.error(f"Error loading templates catalog: {e}")
    
    def get_template(self, template_id: str) -> Optional[ReportTemplate]:
        """
        Get a template by ID.
        
        Args:
            template_id: The template identifier
            
        Returns:
            The template if found, None otherwise
        """
        return self.templates.get(template_id)
    
    def list_templates(self) -> List[ReportTemplate]:
        """
        List all available templates.
        
        Returns:
            List of templates
        """
        return list(self.templates.values())
    
    def add_template(self, template: ReportTemplate) -> None:
        """
        Add a template to the catalog.
        
        Args:
            template: The template to add
        """
        self.templates[template.template_id] = template


class ReportRenderer(ABC):
    """Base abstract class for report renderers."""
    
    def __init__(self, template_manager: Optional[TemplateManager] = None):
        """
        Initialize the renderer.
        
        Args:
            template_manager: Template manager instance. If None, creates a new one.
        """
        self.template_manager = template_manager or TemplateManager()
    
    @abstractmethod
    def render(
        self,
        template_id: str,
        data: Dict[str, Any],
        output_format: ReportFormat = ReportFormat.PDF
    ) -> bytes:
        """
        Render a report using the specified template and data.
        
        Args:
            template_id: Template identifier
            data: Data to render in the template
            output_format: Output format
            
        Returns:
            The rendered report as bytes
            
        Raises:
            ValueError: If template not found or rendering fails
        """
        pass
    
    def render_to_file(
        self,
        template_id: str,
        data: Dict[str, Any],
        output_path: Union[str, Path],
        output_format: Optional[ReportFormat] = None
    ) -> Path:
        """
        Render a report to a file.
        
        Args:
            template_id: Template identifier
            data: Data to render
            output_path: Path to write the output file
            output_format: Output format, if None, inferred from output_path
            
        Returns:
            Path to the output file
            
        Raises:
            ValueError: If template not found or rendering fails
        """
        # Determine format from file extension if not specified
        if output_format is None:
            ext = os.path.splitext(output_path)[1].lower()
            if ext == '.pdf':
                output_format = ReportFormat.PDF
            elif ext == '.html':
                output_format = ReportFormat.HTML
            elif ext == '.docx':
                output_format = ReportFormat.DOCX
            elif ext == '.png':
                output_format = ReportFormat.PNG
            else:
                raise ValueError(f"Cannot determine format from extension: {ext}")
        
        # Generate the report
        content = self.render(template_id, data, output_format)
        
        # Write to file
        output_path = Path(output_path)
        os.makedirs(output_path.parent, exist_ok=True)
        
        with open(output_path, "wb") as f:
            f.write(content)
        
        return output_path
    
    def render_to_stream(
        self,
        template_id: str,
        data: Dict[str, Any],
        output_stream: BinaryIO,
        output_format: ReportFormat = ReportFormat.PDF
    ) -> None:
        """
        Render a report to a stream.
        
        Args:
            template_id: Template identifier
            data: Data to render in the template
            output_stream: Stream to write the output to
            output_format: Output format
            
        Raises:
            ValueError: If template not found or rendering fails
        """
        content = self.render(template_id, data, output_format)
        output_stream.write(content)


# Import specific renderers if available
try:
    from .weasyprint_renderer import WeasyPrintRenderer
    WEASYPRINT_AVAILABLE = True
except ImportError:
    WEASYPRINT_AVAILABLE = False

try:
    from .reportlab_renderer import ReportLabRenderer
    REPORTLAB_AVAILABLE = True
except ImportError:
    REPORTLAB_AVAILABLE = False

try:
    from .xhtml2pdf_renderer import XHTML2PDFRenderer
    XHTML2PDF_AVAILABLE = True
except ImportError:
    XHTML2PDF_AVAILABLE = False

try:
    from .docmosis_renderer import DocmosisRenderer
    DOCMOSIS_AVAILABLE = True
except ImportError:
    DOCMOSIS_AVAILABLE = False


def create_renderer(
    engine: Union[str, RenderingEngine] = RenderingEngine.WEASYPRINT,
    template_manager: Optional[TemplateManager] = None
) -> ReportRenderer:
    """
    Factory function to create a renderer.
    
    Args:
        engine: The rendering engine to use
        template_manager: Template manager instance
        
    Returns:
        A report renderer instance
        
    Raises:
        ValueError: If the specified engine is not available
    """
    if isinstance(engine, str):
        engine = RenderingEngine(engine)
    
    if engine == RenderingEngine.WEASYPRINT:
        if not WEASYPRINT_AVAILABLE:
            raise ValueError("WeasyPrint is not available. Install with 'pip install weasyprint'")
        return WeasyPrintRenderer(template_manager)
    
    elif engine == RenderingEngine.REPORTLAB:
        if not REPORTLAB_AVAILABLE:
            raise ValueError("ReportLab is not available. Install with 'pip install reportlab'")
        return ReportLabRenderer(template_manager)
    
    elif engine == RenderingEngine.XHTML2PDF:
        if not XHTML2PDF_AVAILABLE:
            raise ValueError("XHTML2PDF is not available. Install with 'pip install xhtml2pdf'")
        return XHTML2PDFRenderer(template_manager)
    
    elif engine == RenderingEngine.DOCMOSIS:
        if not DOCMOSIS_AVAILABLE:
            raise ValueError("Docmosis renderer is not available.")
        return DocmosisRenderer(template_manager)
    
    else:
        raise ValueError(f"Unknown rendering engine: {engine}")