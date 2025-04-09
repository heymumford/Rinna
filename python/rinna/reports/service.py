"""
Report generation service for the Rinna project.

This module provides a high-level service for generating reports using
the various rendering backends. It serves as the main entry point for
report generation functionality.
"""

import logging
import os
import tempfile
import uuid
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, MutableSequence, Optional, Union

from .renderer import (
    RenderingEngine,
    ReportFormat,
    ReportRenderer,
    TemplateManager,
    create_renderer,
)

logger = logging.getLogger(__name__)


class ReportService:
    """
    Report generation service for the Rinna project.
    
    This class provides a high-level API for generating reports
    with various renderers and templates.
    """
    
    def __init__(
        self,
        templates_dir: Optional[Union[str, Path]] = None,
        output_dir: Optional[Union[str, Path]] = None,
        default_engine: Union[str, RenderingEngine] = RenderingEngine.WEASYPRINT
    ):
        """
        Initialize the report service.
        
        Args:
            templates_dir: Directory for templates. If None, uses default.
            output_dir: Directory for storing generated reports. If None, uses temp.
            default_engine: Default rendering engine to use.
        """
        # Initialize template manager
        self.template_manager = TemplateManager(templates_dir)
        
        # Set up output directory
        if output_dir:
            self.output_dir = Path(output_dir)
            os.makedirs(self.output_dir, exist_ok=True)
        else:
            # For type checking, use a default path that will be created when needed
            temp_dir = Path(tempfile.gettempdir()) / "rinna_reports"
            self.output_dir = temp_dir
        
        # Default engine
        if isinstance(default_engine, RenderingEngine):
            self.default_engine = default_engine
        else:
            self.default_engine = RenderingEngine(default_engine)
        
        # Store renderers for reuse
        self._renderers: Dict[RenderingEngine, ReportRenderer] = {}
    
    def get_renderer(self, engine: Union[str, RenderingEngine]) -> ReportRenderer:
        """
        Get a renderer for the specified engine.
        
        Args:
            engine: The rendering engine to use
            
        Returns:
            A report renderer instance
        """
        if isinstance(engine, str):
            engine = RenderingEngine(engine)
        
        if engine not in self._renderers:
            try:
                self._renderers[engine] = create_renderer(engine, self.template_manager)
            except ValueError as e:
                logger.warning(f"Failed to create renderer for {engine}: {e}")
                # Fall back to default engine if available
                if engine != self.default_engine:
                    if self.default_engine not in self._renderers:
                        # Create default renderer
                        self._renderers[engine] = create_renderer(
                            self.default_engine, self.template_manager
                        )
                    else:
                        # Use existing default renderer
                        self._renderers[engine] = self._renderers[self.default_engine]
                else:
                    # Can't fall back if default engine failed
                    raise
        
        return self._renderers[engine]
    
    def list_templates(self) -> List[Dict[str, Any]]:
        """
        List all available templates.
        
        Returns:
            List of template information dictionaries
        """
        templates = []
        
        for template in self.template_manager.list_templates():
            templates.append({
                "id": template.template_id,
                "title": template.title,
                "description": template.description,
                "engine": template.engine,
                "exists": template.exists
            })
        
        return templates
    
    def generate_report(
        self,
        template_id: str,
        data: Dict[str, Any],
        output_format: Union[str, ReportFormat] = ReportFormat.PDF,
        engine: Optional[Union[str, RenderingEngine]] = None,
        filename: Optional[str] = None,
        save: bool = True
    ) -> Dict[str, Any]:
        """
        Generate a report.
        
        Args:
            template_id: Template identifier
            data: Data to render in the template
            output_format: Output format
            engine: Rendering engine to use, or None for default
            filename: Custom filename, or None for auto-generated
            save: Whether to save the report to disk
            
        Returns:
            Report information dictionary
            
        Raises:
            ValueError: If template not found or rendering fails
        """
        # Validate template
        template = self.template_manager.get_template(template_id)
        if not template:
            raise ValueError(f"Template not found: {template_id}")
        
        # Determine output format
        if isinstance(output_format, str):
            output_format = ReportFormat(output_format)
        
        # Use template engine if not specified
        if engine is None:
            engine = template.engine
        
        # Generate a report ID
        report_id = str(uuid.uuid4())
        
        # Generate default filename if not provided
        if not filename:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"{template_id}_{timestamp}.{output_format.value}"
        
        # Get renderer and generate report
        renderer = self.get_renderer(engine)
        
        try:
            # Start timing
            start_time = datetime.now()
            
            # Generate report content
            content = renderer.render(template_id, data, output_format)
            
            # End timing
            end_time = datetime.now()
            generation_time = (end_time - start_time).total_seconds()
            
            # Save the report if requested
            file_path = None
            if save:
                if self.output_dir:
                    file_path = self.output_dir / filename
                else:
                    # Use temp directory if no output dir is specified
                    temp_dir = Path(tempfile.gettempdir()) / "rinna_reports"
                    os.makedirs(temp_dir, exist_ok=True)
                    file_path = temp_dir / filename
                
                with open(file_path, "wb") as f:
                    f.write(content)
                
                logger.info(f"Report saved to {file_path}")
            
            # Return report information
            return {
                "report_id": report_id,
                "template_id": template_id,
                "format": output_format.value,
                "engine": engine if isinstance(engine, str) else engine.value,
                "file_path": str(file_path) if file_path else None,
                "size_bytes": len(content),
                "generation_time_seconds": generation_time,
                "timestamp": end_time.isoformat(),
                "status": "success"
            }
            
        except Exception as e:
            logger.error(f"Error generating report with template {template_id}: {e}")
            return {
                "report_id": report_id,
                "template_id": template_id,
                "format": output_format.value,
                "engine": engine if isinstance(engine, str) else engine.value,
                "error": str(e),
                "timestamp": datetime.now().isoformat(),
                "status": "error"
            }
    
    def generate_metrics_report(
        self,
        title: str,
        metrics_data: Dict[str, Any],
        template_id: str = "metrics_default",
        output_format: Union[str, ReportFormat] = ReportFormat.PDF,
        engine: Optional[Union[str, RenderingEngine]] = None,
        filename: Optional[str] = None,
        author: str = "Rinna System"
    ) -> Dict[str, Any]:
        """
        Generate a metrics report with standardized formatting.
        
        Args:
            title: Report title
            metrics_data: Metrics data to include in the report
            template_id: Template identifier
            output_format: Output format
            engine: Rendering engine to use, or None for default
            filename: Custom filename, or None for auto-generated
            author: Report author
            
        Returns:
            Report information dictionary
        """
        # Prepare data for the report template
        now = datetime.now()
        
        data = {
            "title": title,
            "subtitle": f"Generated on {now.strftime('%Y-%m-%d at %H:%M:%S')}",
            "author": author,
            "timestamp": now.isoformat(),
            "sections": []
        }
        
        # Process metrics data
        sections: MutableSequence[Dict[str, Any]] = []
        if "summary" in metrics_data:
            sections.append({
                "title": "Summary",
                "description": "Key metrics overview",
                "metrics": [
                    {"name": key, "value": value, "description": ""}
                    for key, value in metrics_data["summary"].items()
                ]
            })
        data["sections"] = sections
        
        # Process detailed sections
        for section_name, section_data in metrics_data.items():
            if section_name == "summary":
                continue
                
            section = {
                "title": section_name.replace("_", " ").title(),
                "metrics": []
            }
            
            metrics: MutableSequence[Dict[str, Any]] = []
            if isinstance(section_data, dict):
                for metric_name, metric_value in section_data.items():
                    metric = {
                        "name": metric_name.replace("_", " ").title(),
                        "value": metric_value,
                        "description": ""
                    }
                    metrics.append(metric)
            elif isinstance(section_data, list):
                for item in section_data:
                    if isinstance(item, dict) and "name" in item and "value" in item:
                        metrics.append(item)
            
            section["metrics"] = metrics
            sections.append(section)
        
        # Generate the report
        return self.generate_report(
            template_id=template_id,
            data=data,
            output_format=output_format,
            engine=engine,
            filename=filename
        )
    
    def get_sample_metrics_data(self) -> Dict[str, Any]:
        """
        Get sample metrics data for testing.
        
        Returns:
            Sample metrics data dictionary
        """
        return {
            "summary": {
                "Total Work Items": 127,
                "Completed Items": 89,
                "In Progress": 32,
                "Blocked": 6,
                "Average Completion Time": "3.2 days"
            },
            "workflow_metrics": {
                "workflow_efficiency": 0.87,
                "average_transition_time": 1.2,
                "blocked_percentage": 0.047,
                "first_time_completion_rate": 0.92
            },
            "priority_distribution": {
                "high": 18,
                "medium": 65,
                "low": 44
            },
            "team_performance": [
                {
                    "name": "Team Alpha",
                    "value": 94,
                    "description": "Completion percentage"
                },
                {
                    "name": "Team Beta",
                    "value": 87,
                    "description": "Completion percentage"
                },
                {
                    "name": "Team Gamma",
                    "value": 91,
                    "description": "Completion percentage"
                }
            ],
            "monthly_trend": {
                "january": 42,
                "february": 38,
                "march": 47
            }
        }


# Create a default report service instance for easy importing
default_service = ReportService()