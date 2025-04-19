"""
Unit tests for the report generation module.
"""

import os
import tempfile
from pathlib import Path
from typing import Any, Dict, Generator, List
from unittest import mock

import pytest

from rinna.reports.renderer import (
    RenderingEngine,
    ReportFormat,
    ReportTemplate,
    TemplateManager,
)
from rinna.reports.service import ReportService


class TestReportGeneration:
    """Test cases for the report generation module."""

    @pytest.fixture
    def temp_templates_dir(self) -> Generator[Path, None, None]:
        """Create a temporary templates directory."""
        with tempfile.TemporaryDirectory() as temp_dir:
            templates_dir = Path(temp_dir) / "templates"
            os.makedirs(templates_dir, exist_ok=True)
            yield templates_dir

    @pytest.fixture
    def sample_template(self, temp_templates_dir: Path) -> Path:
        """Create a sample HTML template."""
        template_path = temp_templates_dir / "sample.html"
        with open(template_path, "w") as f:
            f.write(
                """<!DOCTYPE html>
            <html>
            <head>
                <title>{{ title }}</title>
            </head>
            <body>
                <h1>{{ title }}</h1>
                <p>{{ content }}</p>
            </body>
            </html>"""
            )
        return template_path

    @pytest.fixture
    def template_manager(
        self, temp_templates_dir: Path, sample_template: Path
    ) -> TemplateManager:
        """Create a template manager with a sample template."""
        manager = TemplateManager(temp_templates_dir)

        # Add sample template
        template = ReportTemplate(
            template_id="sample",
            path=sample_template,
            title="Sample Template",
            description="A sample template for testing",
            engine=RenderingEngine.WEASYPRINT,
        )
        manager.add_template(template)

        return manager

    @pytest.fixture
    def report_service(self, template_manager: TemplateManager) -> ReportService:
        """Create a report service with the template manager."""
        # First create a service with the template directory
        service = ReportService(
            templates_dir=template_manager.templates_dir,
            default_engine=RenderingEngine.WEASYPRINT,
        )
        # Then replace its template manager with our test one that already has templates
        service.template_manager = template_manager
        return service

    def test_template_manager_initialization(self, temp_templates_dir: Path) -> None:
        """Test that template manager initializes correctly."""
        manager = TemplateManager(temp_templates_dir)
        assert manager.templates_dir == temp_templates_dir
        assert isinstance(manager.templates, dict)

    def test_template_manager_add_template(
        self, template_manager: TemplateManager
    ) -> None:
        """Test adding a template to the manager."""
        # Template is added in the fixture
        assert "sample" in template_manager.templates
        template = template_manager.get_template("sample")
        assert template is not None
        assert template.template_id == "sample"
        assert template.title == "Sample Template"

    def test_template_manager_list_templates(
        self, template_manager: TemplateManager
    ) -> None:
        """Test listing templates."""
        templates = template_manager.list_templates()
        assert len(templates) == 1
        assert templates[0].template_id == "sample"

    @mock.patch("rinna.reports.weasyprint_renderer.WeasyPrintRenderer.render")
    def test_report_service_generate_report(
        self, mock_render: mock.MagicMock, report_service: ReportService
    ) -> None:
        """Test generating a report."""
        # Mock the renderer to avoid actual rendering
        mock_render.return_value = b"PDF content"

        # Generate a report
        result = report_service.generate_report(
            template_id="sample",
            data={"title": "Test Report", "content": "This is a test report."},
            output_format=ReportFormat.PDF,
            save=False,  # Don't save to disk for testing
        )

        # Check result
        assert result["status"] == "success"
        assert result["template_id"] == "sample"
        assert result["format"] == "pdf"
        assert "report_id" in result
        assert "generation_time_seconds" in result

        # Verify the renderer was called with correct arguments
        mock_render.assert_called_once()
        args, kwargs = mock_render.call_args
        assert args[0] == "sample"
        assert args[1]["title"] == "Test Report"
        assert args[1]["content"] == "This is a test report."
        assert args[2] == ReportFormat.PDF

    @mock.patch("rinna.reports.weasyprint_renderer.WeasyPrintRenderer.render")
    def test_report_service_generate_metrics_report(
        self, mock_render: mock.MagicMock, report_service: ReportService
    ) -> None:
        """Test generating a metrics report."""
        # Mock the renderer to avoid actual rendering
        mock_render.return_value = b"PDF content"

        # Generate a metrics report
        metrics_data = {
            "summary": {"Total Items": 100, "Completed": 75},
            "details": {"high_priority": 20, "medium_priority": 50, "low_priority": 30},
        }

        result = report_service.generate_metrics_report(
            title="Test Metrics Report",
            metrics_data=metrics_data,
            template_id="sample",  # Use our sample template instead of metrics_default
            output_format=ReportFormat.PDF,
            # Note: save parameter doesn't exist on generate_metrics_report
        )

        # Check result
        assert result["status"] == "success"
        assert result["template_id"] == "sample"
        assert result["format"] == "pdf"
        assert "report_id" in result

        # Verify the renderer was called with correct arguments
        mock_render.assert_called_once()
        args, kwargs = mock_render.call_args
        assert args[0] == "sample"
        assert "title" in args[1]
        assert "sections" in args[1]
        assert len(args[1]["sections"]) == 2  # summary and details
        assert args[2] == ReportFormat.PDF
