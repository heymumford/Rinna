"""
Rinna API server.

This module provides FastAPI endpoints for the Rinna Python service.
"""

import os
import tempfile
from typing import Any, Dict, List, Optional

# Import uvicorn at the top level
try:
    import uvicorn
except ImportError:
    uvicorn = None

# Import report service at the top level
try:
    from rinna.reports.service import default_service
except ImportError:
    default_service = None

try:
    from fastapi import (
        FastAPI,
        HTTPException,
    )
    from fastapi.responses import FileResponse
    from pydantic import BaseModel, Field
except ImportError:
    # Create placeholder classes if FastAPI is not installed
    class FastAPI:
        def __init__(self, *args, **kwargs):
            pass

        def get(self, path):
            def decorator(func):
                return func

            return decorator

        def post(self, path):
            def decorator(func):
                return func

            return decorator

    class HTTPException(Exception):
        def __init__(self, status_code, detail):
            self.status_code = status_code
            self.detail = detail

    class BaseModel:
        pass

    def Field(default=None, **kwargs):
        return default

    class BackgroundTasks:
        def add_task(self, func, *args, **kwargs):
            pass

    FileResponse = dict

    def File(default=None, **kwargs):
        return default

    class UploadFile:
        pass

    def Form(default=None, **kwargs):
        return default

    def Query(default=None, **kwargs):
        return default

# Create FastAPI app
app = FastAPI(
    title="Rinna Python API",
    description="API for Rinna Python services",
    version="0.1.0",
)


class HealthResponse(BaseModel):
    status: str
    version: str
    python_service: bool = True


@app.get("/health", response_model=HealthResponse)
async def health_check() -> Dict[str, Any]:
    """
    Health check endpoint.

    Returns:
        Dict[str, Any]: Health status information
    """
    return {"status": "ok", "version": "0.1.0", "python_service": True}


class ReportRequest(BaseModel):
    template_id: str = Field(..., description="Template identifier")
    data: Dict[str, Any] = Field(..., description="Report data")
    format: str = Field("pdf", description="Output format (pdf, html, docx, png)")
    engine: Optional[str] = Field(None, description="Rendering engine to use")
    filename: Optional[str] = Field(None, description="Custom filename")


class ReportResponse(BaseModel):
    report_id: str
    status: str
    url: str = Field(..., description="URL to download the report")
    filename: str = Field(..., description="Report filename")
    generation_time_seconds: Optional[float] = Field(
        None, description="Time taken to generate the report"
    )


@app.post("/api/v1/reports", response_model=ReportResponse)
async def generate_report(request: ReportRequest) -> Dict[str, Any]:
    """
    Generate a report from a template.

    Args:
        request (ReportRequest): Report generation request

    Returns:
        Dict[str, Any]: Report generation status and URL
    """
    try:
        # Import report service
        from rinna.reports.service import default_service

        # Generate report
        result = default_service.generate_report(
            template_id=request.template_id,
            data=request.data,
            output_format=request.format,
            engine=request.engine,
            filename=request.filename,
            save=True,
        )

        if result["status"] == "error":
            raise HTTPException(status_code=400, detail=result["error"])

        # Construct file URL
        file_path = result["file_path"]
        filename = os.path.basename(file_path)

        return {
            "report_id": result["report_id"],
            "status": "success",
            "url": f"/api/v1/reports/{result['report_id']}/download",
            "filename": filename,
            "generation_time_seconds": result["generation_time_seconds"],
        }
    except ImportError as e:
        raise HTTPException(
            status_code=501, detail=f"Report generation service not available: {e}"
        ) from e
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Failed to generate report: {str(e)}"
        ) from e


class MetricsReportRequest(BaseModel):
    title: str = Field(..., description="Report title")
    metrics: Dict[str, Any] = Field(..., description="Metrics data")
    template_id: str = Field("metrics_default", description="Template identifier")
    format: str = Field("pdf", description="Output format (pdf, html, docx, png)")
    engine: Optional[str] = Field(None, description="Rendering engine to use")
    filename: Optional[str] = Field(None, description="Custom filename")
    author: str = Field("Rinna System", description="Report author")


@app.post("/api/v1/metrics/reports", response_model=ReportResponse)
async def generate_metrics_report(request: MetricsReportRequest) -> Dict[str, Any]:
    """
    Generate a metrics report.

    Args:
        request (MetricsReportRequest): Metrics report generation request

    Returns:
        Dict[str, Any]: Report generation status and URL
    """
    try:
        # Import report service
        from rinna.reports.service import default_service

        # Generate metrics report
        result = default_service.generate_metrics_report(
            title=request.title,
            metrics_data=request.metrics,
            template_id=request.template_id,
            output_format=request.format,
            engine=request.engine,
            filename=request.filename,
            author=request.author,
        )

        if result["status"] == "error":
            raise HTTPException(status_code=400, detail=result["error"])

        # Construct file URL
        file_path = result["file_path"]
        filename = os.path.basename(file_path)

        return {
            "report_id": result["report_id"],
            "status": "success",
            "url": f"/api/v1/reports/{result['report_id']}/download",
            "filename": filename,
            "generation_time_seconds": result["generation_time_seconds"],
        }
    except ImportError as e:
        raise HTTPException(
            status_code=501, detail=f"Report generation service not available: {e}"
        ) from e
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Failed to generate metrics report: {str(e)}"
        ) from e


@app.get("/api/v1/reports/{report_id}/download")
async def download_report(report_id: str):
    """
    Download a generated report.

    Args:
        report_id (str): Report identifier

    Returns:
        FileResponse: The report file
    """
    try:
        # Import report service
        from rinna.reports.service import default_service

        # This is a placeholder implementation
        # In a real implementation, you would look up the report in a database
        # For now, we'll assume the file is in the reports directory
        reports_dir = default_service.output_dir

        if not reports_dir:
            reports_dir = tempfile.gettempdir() / "rinna_reports"

        # List files in the reports directory
        files = os.listdir(reports_dir)

        # Find the file with the matching report ID
        # This is a simplistic approach - in production, you would use a database
        matching_files = [f for f in files if report_id in f]

        if not matching_files:
            raise HTTPException(status_code=404, detail="Report not found")

        # Return the first matching file
        file_path = os.path.join(reports_dir, matching_files[0])

        return FileResponse(
            path=file_path,
            filename=os.path.basename(file_path),
            media_type="application/octet-stream",
        )
    except ImportError as e:
        raise HTTPException(
            status_code=501, detail=f"Report service not available: {e}"
        ) from e
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Failed to download report: {str(e)}"
        ) from e


class TemplateInfo(BaseModel):
    id: str
    title: str
    description: str
    engine: str
    exists: bool


@app.get("/api/v1/reports/templates", response_model=List[TemplateInfo])
async def list_templates() -> List[Dict[str, Any]]:
    """
    List available report templates.

    Returns:
        List[Dict[str, Any]]: List of templates
    """
    try:
        # Import report service
        from rinna.reports.service import default_service

        # List templates
        templates = default_service.list_templates()

        return templates
    except ImportError as e:
        raise HTTPException(
            status_code=501, detail=f"Report service not available: {e}"
        ) from e
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Failed to list templates: {str(e)}"
        ) from e


@app.get("/api/v1/metrics/sample", response_model=Dict[str, Any])
async def get_sample_metrics() -> Dict[str, Any]:
    """
    Get sample metrics data for testing.

    Returns:
        Dict[str, Any]: Sample metrics data
    """
    try:
        # Import report service
        from rinna.reports.service import default_service

        # Get sample metrics
        sample_data = default_service.get_sample_metrics_data()

        return sample_data
    except ImportError as e:
        raise HTTPException(
            status_code=501, detail=f"Report service not available: {e}"
        ) from e
    except Exception as e:
        raise HTTPException(
            status_code=500, detail=f"Failed to get sample metrics: {str(e)}"
        ) from e


if __name__ == "__main__":
    if uvicorn:
        uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", 5000)))
    else:
        print("Error: uvicorn is not installed. Cannot run the API server.")
