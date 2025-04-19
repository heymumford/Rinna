#!/usr/bin/env python3
"""
Generate API component diagram for the Rinna project

This script creates a C4 component diagram focusing on the API
and the Swagger/OpenAPI documentation integration.
"""

import os
import sys, os
from pathlib import Path

try:
    from diagrams import Diagram, Cluster, Edge
    from diagrams.custom import Custom
    from diagrams.onprem.client import User, Client
    from diagrams.onprem.network import Apache
    from diagrams.onprem.database import PostgreSQL, MySQL
    from diagrams.programming.language import Go, Java, Python
    from diagrams.onprem.queue import Kafka
except ImportError:
    print("Error: Required packages not found.")
    print("Please install: pip install diagrams")
    sys.exit(1)

# Project paths
PROJECT_ROOT = Path(__file__).parent.parent
DOCS_DIR = PROJECT_ROOT / "docs"
ICONS_DIR = DOCS_DIR / "architecture" / "diagrams" / "icons"
OUTPUT_DIR = PROJECT_ROOT / "api" / "docs"

# Ensure output directory exists
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

# Create diagram
graph_attr = {
    "fontsize": "16",
    "bgcolor": "white",
    "rankdir": "TB",
    "splines": "polyline",
}

with Diagram(
    "Rinna API Architecture",
    filename=str(OUTPUT_DIR / "rinna_api_diagram"),
    outformat="svg",
    show=False,
    graph_attr=graph_attr,
    direction="TB"
):
    # External actors
    user = User("End User")
    client_app = Client("Client Application")

    # OpenAPI
    with Cluster("API Documentation"):
        swagger_ui = Custom("Swagger UI", "./docs/architecture/diagrams/icons/swagger.png")
        swagger_docs = Custom("OpenAPI Spec", "./docs/architecture/diagrams/icons/openapi.png")
        swagger_gen = Custom("Doc Generator", "./docs/architecture/diagrams/icons/generator.png")

    # Go API
    with Cluster("API Server (Go)"):
        api_server = Go("API Server")
        
        with Cluster("API Handlers"):
            project_handler = Custom("Project Handler", "./docs/architecture/diagrams/icons/component.svg")
            workitem_handler = Custom("WorkItem Handler", "./docs/architecture/diagrams/icons/component.svg")
            release_handler = Custom("Release Handler", "./docs/architecture/diagrams/icons/component.svg")
            health_handler = Custom("Health Handler", "./docs/architecture/diagrams/icons/component.svg")
            webhook_handler = Custom("Webhook Handler", "./docs/architecture/diagrams/icons/component.svg")
        
        handlers = [project_handler, workitem_handler, release_handler, health_handler, webhook_handler]
        for handler in handlers:
            api_server >> handler

    # Java Services
    with Cluster("Core Services (Java)"):
        java_services = Java("Rinna Core")
        
        with Cluster("Domain Services"):
            project_service = Custom("Project Service", "./docs/architecture/diagrams/icons/component.svg")
            workitem_service = Custom("WorkItem Service", "./docs/architecture/diagrams/icons/component.svg")
            release_service = Custom("Release Service", "./docs/architecture/diagrams/icons/component.svg")
        
        services = [project_service, workitem_service, release_service]
        for service in services:
            java_services >> service

    # Python Utilities
    with Cluster("Utilities (Python)"):
        python_utils = Python("Utilities")
        report_gen = Custom("Report Generator", "./docs/architecture/diagrams/icons/component.svg")
        python_utils >> report_gen

    # Database
    database = PostgreSQL("Database")

    # Build System
    with Cluster("Build System"):
        build_script = Custom("Build Script", "./docs/architecture/diagrams/icons/component.svg")
        swagger_build = Custom("Swagger Generator", "./docs/architecture/diagrams/icons/component.svg")
        build_script >> swagger_build

    # Connect components
    user >> client_app
    client_app >> api_server
    
    # API handlers connect to Java services
    for handler, service in zip(handlers[:3], services):
        handler >> service
    
    # Java services connect to database
    for service in services:
        service >> database

    # Swagger documentation connections
    swagger_build >> swagger_docs
    api_server >> Edge(color="blue", style="dashed") >> swagger_docs
    swagger_docs >> swagger_ui
    client_app >> swagger_ui
    
    # Build system connections
    build_script >> api_server

print(f"API diagram generated: {OUTPUT_DIR}/rinna_api_diagram.svg")