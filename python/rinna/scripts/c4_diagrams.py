#!/usr/bin/env python3
"""
Generate C4 model diagrams for the Rinna project

This script generates C4 model diagrams (Context, Container, Component, and Code)
using either Structurizr DSL or the Python diagrams library, then optionally
uploads them to LucidChart for sharing with the team.

Example usage:
    ./bin/c4_diagrams.py --type context
    ./bin/c4_diagrams.py --type container --output svg
    ./bin/c4_diagrams.py --type all --upload

Dependencies:
    - diagrams (pip install diagrams)
    - lucidchart-py (pip install lucidchart-py)
    - graphviz (system package)
"""

import os
import sys, os
import argparse
import tempfile
from pathlib import Path
from typing import Dict, List, Optional, Tuple

# Add the bin directory to the path so we can import rinna modules
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    from rinna_config import config
    from rinna_logger import logger
except ImportError:
    print("Error: Could not import rinna_config or rinna_logger. Make sure they are in the bin directory.")
    sys.exit(1)

# Determine if we're in virtual environment
in_venv = hasattr(sys, 'real_prefix') or (hasattr(sys, 'base_prefix') and sys.base_prefix != sys.prefix)

# Declare global availability flags
diagrams_available = False
lucidchart_available = False
rich_available = False

# Import common dependencies that should always be available
import textwrap
import subprocess
from typing import List, Dict, Set, Optional, Union, Tuple

# Check for requirements and handle missing dependencies elegantly
def check_install_requirements():
    global diagrams_available, lucidchart_available, rich_available
    
    # Dictionary to track required packages and their status
    packages_status = {
        "diagrams": {
            "required": True,
            "available": False,
            "system_deps": ["graphviz"],
            "description": "Architecture diagram generation library",
            "import_test": "from diagrams import Diagram"
        },
        "lucidchart-py": {
            "required": False,
            "available": False,
            "system_deps": [],
            "description": "LucidChart API integration",
            "import_test": "import lucidchart"
        },
        "rich": {
            "required": False,
            "available": False,
            "system_deps": [],
            "description": "Rich terminal output formatting",
            "import_test": "from rich.console import Console"
        }
    }
    
    # Get the project root directory
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    
    # Function to safely execute an import test
    def test_import(import_test):
        try:
            exec(import_test)
            return True
        except (ImportError, ModuleNotFoundError):
            return False
    
    # Check each package
    missing_required = []
    missing_optional = []
    
    for package_name, package_info in packages_status.items():
        package_info['available'] = test_import(package_info['import_test'])
        
        if package_info['available']:
            # Package is available, set global flag if applicable
            if package_name == "diagrams":
                diagrams_available = True
            elif package_name == "lucidchart-py":
                lucidchart_available = True
            elif package_name == "rich":
                rich_available = True
        else:
            # Package is missing, categorize as required or optional
            if package_info['required']:
                missing_required.append(package_name)
            else:
                missing_optional.append(package_name)
    
    # If rich is available, use it for pretty output
    if rich_available:
        try:
            from rich.console import Console
            from rich.panel import Panel
            from rich.text import Text
            console = Console()
            
            def print_package_status():
                console.print(Panel.fit(
                    "Package Status:",
                    title="Rinna C4 Diagram Generator",
                    subtitle="Dependency Check"
                ))
                
                for package, info in packages_status.items():
                    status = "[green]✓[/green]" if info['available'] else "[red]✗[/red]"
                    required = "[yellow]required[/yellow]" if info['required'] else "optional"
                    console.print(f"  {status} {package:<15} - {info['description']} ({required})")
            
            print_package_status()
        except Exception:
            # Fall back to regular print if rich fails somehow
            rich_available = False
    
    # Handle missing dependencies
    if missing_required or missing_optional:
        if not rich_available:
            # Basic output if rich isn't available
            print("\n=== Dependency Check ===")
            for package, info in packages_status.items():
                status = "✓" if info['available'] else "✗"
                required = "required" if info['required'] else "optional"
                print(f"  {status} {package:<15} - {info['description']} ({required})")
        
        # Prepare installation commands based on environment
        venv_python = os.path.join(project_root, ".venv", "bin", "python")
        venv_exists = os.path.exists(venv_python)
        venv_active = in_venv
        
        if venv_active:
            # In an active virtual environment - offer automatic installation
            if missing_required:
                print(f"\nMissing required packages: {', '.join(missing_required)}")
                if len(missing_optional) > 0:
                    print(f"Missing optional packages: {', '.join(missing_optional)}")
                
                # Combine all missing packages for installation
                all_missing = missing_required + missing_optional
                
                # Since we're in a venv, we can offer to install
                install = input("Would you like to install all missing packages now? (y/n): ")
                
                if install.lower() == 'y':
                    try:
                        for package in all_missing:
                            print(f"Installing {package}...")
                            subprocess.check_call([sys.executable, "-m", "pip", "install", package])
                            print(f"{package} installed successfully!")
                        
                        # Reload the modules to make them available
                        import importlib
                        importlib.invalidate_caches()
                        
                        # Check if diagrams is now available
                        if "diagrams" in all_missing:
                            try:
                                from diagrams import Diagram, Cluster
                                from diagrams.programming.language import Java, Go, Python
                                from diagrams.onprem.database import PostgreSQL, SQLite3
                                from diagrams.onprem.client import Users, User
                                from diagrams.onprem.compute import Server
                                from diagrams.onprem.workflow import Airflow
                                from diagrams.onprem.queue import Kafka
                                from diagrams.onprem.vcs import Git
                                from diagrams.onprem.network import Nginx
                                from diagrams.generic.storage import Storage
                                from diagrams.generic.database import SQL
                                from diagrams.programming.framework import Spring
                                diagrams_available = True
                                print("Successfully imported diagrams package.")
                            except ImportError as e:
                                print(f"Warning: Failed to import diagrams package after installation: {e}")
                                
                                # Check for system dependencies like graphviz
                                if "graphviz" in str(e).lower() or not os.system("which dot >/dev/null 2>&1") == 0:
                                    print("\nGraphviz is required for the diagrams package to work correctly.")
                                    print("Please install graphviz using your system package manager:")
                                    print("  Ubuntu/Debian: sudo apt install graphviz")
                                    print("  Fedora/RHEL: sudo dnf install graphviz")
                                    print("  macOS: brew install graphviz")
                                
                        # Check if lucidchart-py is now available
                        if "lucidchart-py" in all_missing:
                            try:
                                import lucidchart
                                lucidchart_available = True
                                print("Successfully imported lucidchart-py package.")
                            except ImportError as e:
                                print(f"Warning: Failed to import lucidchart-py package after installation: {e}")
                    except Exception as e:
                        print(f"Error installing packages: {e}")
                        print("Please try installing them manually:")
                        print(f"  {sys.executable} -m pip install {' '.join(all_missing)}")
                else:
                    if missing_required:
                        print("\nSome features will not be available without required packages.")
                        print(f"To install manually: pip install {' '.join(missing_required)}")
                    if missing_optional:
                        print(f"Optional packages: pip install {' '.join(missing_optional)}")
            else:
                # Only optional packages are missing
                if len(missing_optional) > 0:
                    print(f"\nMissing optional packages: {', '.join(missing_optional)}")
                    print("These packages are not required but provide additional functionality.")
                    print(f"To install: pip install {' '.join(missing_optional)}")
        
        elif venv_exists:
            # Virtual environment exists but not activated
            print("\nA Python virtual environment was found but is not activated.")
            print(f"Run 'source {project_root}/activate-python.sh' to activate it.")
            
            if missing_required:
                print("\nAfter activation, install required packages with:")
                print(f"  pip install {' '.join(missing_required)}")
            if missing_optional:
                print("Optional packages can be installed with:")
                print(f"  pip install {' '.join(missing_optional)}")
            
            print(f"\nOr run '{project_root}/bin/setup-python.sh --all' to set up all dependencies.")
            
        else:
            # No virtual environment
            print("\nNo Python virtual environment found.")
            print(f"Run '{project_root}/bin/setup-python.sh' to set up the environment with all required dependencies.")
            print("This will create a virtual environment and install all needed packages.")
    
    # Explicitly import diagrams if available
    if diagrams_available:
        try:
            global Diagram, Cluster, Java, Go, Python, PostgreSQL, SQLite3
            global Users, User, Server, Airflow, Kafka, Git, Nginx, Storage, SQL, Spring
            
            from diagrams import Diagram, Cluster
            from diagrams.programming.language import Java, Go, Python
            # Handle different versions of diagrams package which may have different database classes
            try:
                from diagrams.onprem.database import PostgreSQL, SQLite3
            except ImportError:
                try:
                    from diagrams.onprem.database import PostgreSQL, SQLite
                    SQLite3 = SQLite  # Create alias for compatibility
                except ImportError:
                    from diagrams.onprem.database import PostgreSQL
                    # Create a placeholder for SQLite3
                    from diagrams.generic.database import SQL as SQLite3
                    print("Warning: SQLite3 not found in diagrams package, using generic SQL instead")
                    
            from diagrams.onprem.client import Users, User
            from diagrams.onprem.compute import Server
            from diagrams.onprem.workflow import Airflow
            from diagrams.onprem.queue import Kafka
            from diagrams.onprem.vcs import Git
            from diagrams.onprem.network import Nginx
            from diagrams.generic.storage import Storage
            from diagrams.generic.database import SQL
            from diagrams.programming.framework import Spring
        except Exception as e:
            diagrams_available = False
            print(f"Error importing diagrams modules: {e}")
    
    # Return status for required packages
    return not missing_required

# Run the requirements check
check_install_requirements()

class C4DiagramGenerator:
    """Generate C4 model diagrams for the Rinna project."""
    
    def __init__(self, output_dir: Optional[str] = None, output_format: str = "png"):
        """Initialize the diagram generator.
        
        Args:
            output_dir: Directory to save output files (default: ~/.rinna/data/diagrams)
            output_format: Output format (png, svg, pdf)
        """
        self.output_format = output_format.lower()
        if self.output_format not in ("png", "svg", "pdf"):
            logger.warning(f"Unsupported output format: {output_format}, defaulting to png")
            self.output_format = "png"
        
        # Get output directory
        if output_dir:
            self.output_dir = Path(output_dir)
        else:
            self.output_dir = Path(config.get_path("python.diagrams.output_dir"))
        
        # Create output directory if it doesn't exist
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # Check for Lucidchart credentials
        self.lucidchart_api_key = config.get("python.diagrams.lucidchart.api_key")
        self.lucidchart_token = config.get("python.diagrams.lucidchart.token")
        
        # Initialize lucidchart client if credentials are available
        self.lucidchart_client = None
        if lucidchart_available and self.lucidchart_api_key:
            try:
                self.lucidchart_client = lucidchart.LucidchartClient(
                    api_key=self.lucidchart_api_key,
                    token=self.lucidchart_token
                )
            except Exception as e:
                logger.error(f"Failed to initialize Lucidchart client: {e}")
    
    def generate_context_diagram(self) -> str:
        """Generate a C4 context diagram.
        
        Returns:
            Path to the generated diagram file
        """
        if not diagrams_available:
            logger.error("Diagrams library not available. Cannot generate context diagram.")
            return ""
        
        output_file = self.output_dir / f"rinna_context_diagram.{self.output_format}"
        
        try:
            with Diagram(
                "Rinna System Context",
                filename=str(output_file.with_suffix("")),
                outformat=self.output_format,
                show=False,
            ):
                users = Users("Development Teams")
                
                with Cluster("Rinna Workflow Management"):
                    core = Java("Rinna Core")
                    api = Go("API Server")
                    cli = Python("CLI Tool")
                    db = SQLite3("Database")
                
                with Cluster("External Systems"):
                    vcs = Git("Git Server")
                    ci = Server("CI/CD System")
                    docs = Storage("Document Repository")
                
                users >> cli >> api >> core >> db
                users >> api
                core >> vcs
                core >> docs
                api >> ci
            
            logger.info(f"Generated context diagram: {output_file}")
            return str(output_file)
        
        except Exception as e:
            logger.error(f"Failed to generate context diagram: {e}")
            return ""
    
    def generate_container_diagram(self) -> str:
        """Generate a C4 container diagram.
        
        Returns:
            Path to the generated diagram file
        """
        if not diagrams_available:
            logger.error("Diagrams library not available. Cannot generate container diagram.")
            return ""
        
        output_file = self.output_dir / f"rinna_container_diagram.{self.output_format}"
        
        try:
            with Diagram(
                "Rinna Container Diagram",
                filename=str(output_file.with_suffix("")),
                outformat=self.output_format,
                show=False,
            ):
                dev = User("Developer")
                pm = User("Project Manager")
                
                with Cluster("Rinna System"):
                    with Cluster("Frontend"):
                        cli = Python("CLI Tool")
                        web = Spring("Web Interface")
                    
                    with Cluster("Backend Services"):
                        api = Go("API Server")
                        core = Java("Core Domain")
                        docs = Java("Document Service")
                    
                    with Cluster("Data Storage"):
                        db = SQLite3("Local Database")
                        docs_store = Storage("Document Store")
                
                with Cluster("External Systems"):
                    git = Git("GitHub")
                    ci_cd = Server("CI/CD Pipeline")
                
                dev >> cli >> api >> core >> db
                pm >> web >> api
                api >> docs >> docs_store
                core >> git
            
            logger.info(f"Generated container diagram: {output_file}")
            return str(output_file)
        
        except Exception as e:
            logger.error(f"Failed to generate container diagram: {e}")
            return ""
    
    def generate_component_diagram(self) -> str:
        """Generate a C4 component diagram.
        
        Returns:
            Path to the generated diagram file
        """
        if not diagrams_available:
            logger.error("Diagrams library not available. Cannot generate component diagram.")
            return ""
        
        output_file = self.output_dir / f"rinna_component_diagram.{self.output_format}"
        
        try:
            with Diagram(
                "Rinna Component Diagram",
                filename=str(output_file.with_suffix("")),
                outformat=self.output_format,
                show=False,
            ):
                with Cluster("Core Domain"):
                    model = Java("Domain Model")
                    serv = Java("Domain Services")
                    repo = Java("Repositories")
                
                with Cluster("Service Layer"):
                    workflow = Java("Workflow Service")
                    items = Java("Item Service")
                    releases = Java("Release Service")
                    queues = Java("Queue Service")
                    docs = Java("Document Service")
                
                with Cluster("Persistence"):
                    db = SQLite3("SQLite Database")
                    mem = Storage("In-Memory Store")
                
                with Cluster("API"):
                    rest = Go("REST API")
                    webhook = Go("Webhook Handler")
                    health = Go("Health Check")
                
                # Connect components
                model >> serv
                serv >> repo
                
                workflow >> model
                items >> model
                releases >> model
                queues >> model
                
                repo >> db
                repo >> mem
                
                rest >> workflow
                rest >> items
                rest >> releases
                rest >> queues
                rest >> docs
                
                webhook >> items
            
            logger.info(f"Generated component diagram: {output_file}")
            return str(output_file)
        
        except Exception as e:
            logger.error(f"Failed to generate component diagram: {e}")
            return ""
    
    def generate_code_diagram(self) -> str:
        """Generate a C4 code diagram.
        
        Returns:
            Path to the generated diagram file
        """
        if not diagrams_available:
            logger.error("Diagrams library not available. Cannot generate code diagram.")
            return ""
        
        output_file = self.output_dir / f"rinna_code_diagram.{self.output_format}"
        
        try:
            with Diagram(
                "Rinna Code Diagram",
                filename=str(output_file.with_suffix("")),
                outformat=self.output_format,
                show=False,
                direction="TB",
            ):
                with Cluster("Domain Model"):
                    project = Java("Project")
                    work_item = Java("WorkItem")
                    release = Java("Release")
                    work_queue = Java("WorkQueue")
                    state = Java("WorkflowState")
                
                with Cluster("Domain Services"):
                    project_svc = Java("ProjectService")
                    item_svc = Java("ItemService")
                    release_svc = Java("ReleaseService")
                    queue_svc = Java("QueueService")
                    workflow_svc = Java("WorkflowService")
                
                with Cluster("Repositories"):
                    project_repo = Java("ProjectRepository")
                    item_repo = Java("ItemRepository")
                    release_repo = Java("ReleaseRepository")
                    queue_repo = Java("QueueRepository")
                
                # Connect classes
                project_svc >> project >> project_repo
                item_svc >> work_item >> item_repo
                release_svc >> release >> release_repo
                queue_svc >> work_queue >> queue_repo
                workflow_svc >> state
                workflow_svc >> work_item
            
            logger.info(f"Generated code diagram: {output_file}")
            return str(output_file)
        
        except Exception as e:
            logger.error(f"Failed to generate code diagram: {e}")
            return ""
    
    def upload_to_lucidchart(self, file_path: str, title: Optional[str] = None) -> bool:
        """Upload a diagram to Lucidchart.
        
        Args:
            file_path: Path to the diagram file
            title: Title for the diagram in Lucidchart
        
        Returns:
            True if upload successful, False otherwise
        """
        if not lucidchart_available or not self.lucidchart_client:
            logger.error("Lucidchart integration not available. Cannot upload diagram.")
            return False
        
        if not os.path.exists(file_path):
            logger.error(f"File not found: {file_path}")
            return False
        
        try:
            # Use filename as title if not provided
            if not title:
                title = os.path.basename(file_path)
            
            # Upload file to Lucidchart
            result = self.lucidchart_client.upload_diagram(
                file_path=file_path,
                title=title,
                folder_id=None  # Use default folder
            )
            
            if result and "id" in result:
                logger.info(f"Successfully uploaded diagram to Lucidchart: {result['id']}")
                logger.info(f"Access URL: {result.get('editUrl', 'Not available')}")
                return True
            else:
                logger.error(f"Failed to upload diagram: {result}")
                return False
        
        except Exception as e:
            logger.error(f"Error uploading to Lucidchart: {e}")
            return False
    
    def generate_clean_architecture_diagram(self) -> str:
        """Generate a diagram showing the Clean Architecture layers.
        
        Returns:
            Path to the generated diagram file
        """
        if not diagrams_available:
            logger.error("Diagrams library not available. Cannot generate clean architecture diagram.")
            return ""
        
        output_file = self.output_dir / f"rinna_clean_architecture_diagram.{self.output_format}"
        
        try:
            from diagrams.onprem.monitoring import Grafana
            from diagrams.custom import Custom
            
            with Diagram(
                "Rinna Clean Architecture",
                filename=str(output_file.with_suffix("")),
                outformat=self.output_format,
                show=False,
                direction="TB",
                curvestyle="ortho",
            ):
                with Cluster("Core Domain"):
                    entity = Java("Entities")
                
                with Cluster("Use Cases"):
                    usecases = Java("Use Cases")
                    
                with Cluster("Interface Adapters"):
                    with Cluster("Input Adapters"):
                        controllers = Java("Controllers")
                        presenters = Java("Presenters")
                    
                    with Cluster("Output Adapters"):
                        gateways = Java("Gateways")
                        repositories = Java("Repositories")
                
                with Cluster("Frameworks & Drivers"):
                    with Cluster("UI"):
                        web = Spring("Web UI")
                        cli = Python("CLI")
                    
                    with Cluster("External Interfaces"):
                        db = SQLite3("Database")
                        external_api = Go("External APIs")
                    
                # Dependency Rule: Arrows point inward
                web >> controllers
                cli >> controllers
                controllers >> usecases
                presenters >> usecases
                usecases >> entity
                repositories >> db
                gateways >> external_api
                usecases >> repositories
                usecases >> gateways
                presenters >> web
                
            logger.info(f"Generated clean architecture diagram: {output_file}")
            return str(output_file)
        
        except Exception as e:
            logger.error(f"Failed to generate clean architecture diagram: {e}")
            return ""

    def generate_all(self, upload: bool = False) -> List[str]:
        """Generate all C4 diagrams.
        
        Args:
            upload: Whether to upload diagrams to Lucidchart
        
        Returns:
            List of paths to generated diagrams
        """
        files = []
        
        # Context diagram
        context_file = self.generate_context_diagram()
        if context_file:
            files.append(context_file)
            if upload and context_file:
                self.upload_to_lucidchart(context_file, "Rinna Context Diagram")
        
        # Container diagram
        container_file = self.generate_container_diagram()
        if container_file:
            files.append(container_file)
            if upload and container_file:
                self.upload_to_lucidchart(container_file, "Rinna Container Diagram")
        
        # Component diagram
        component_file = self.generate_component_diagram()
        if component_file:
            files.append(component_file)
            if upload and component_file:
                self.upload_to_lucidchart(component_file, "Rinna Component Diagram")
        
        # Code diagram
        code_file = self.generate_code_diagram()
        if code_file:
            files.append(code_file)
            if upload and code_file:
                self.upload_to_lucidchart(code_file, "Rinna Code Diagram")
        
        # Clean architecture diagram
        clean_arch_file = self.generate_clean_architecture_diagram()
        if clean_arch_file:
            files.append(clean_arch_file)
            if upload and clean_arch_file:
                self.upload_to_lucidchart(clean_arch_file, "Rinna Clean Architecture Diagram")
        
        return files


def main():
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(description="Generate C4 model diagrams for Rinna")
    parser.add_argument(
        "--type", 
        choices=["context", "container", "component", "code", "clean", "all"],
        default="all",
        help="Type of C4 diagram to generate (default: all)"
    )
    parser.add_argument(
        "--output", 
        choices=["png", "svg", "pdf"],
        default="png",
        help="Output format (default: png)"
    )
    parser.add_argument(
        "--dir",
        help="Output directory (default: from config)"
    )
    parser.add_argument(
        "--upload",
        action="store_true",
        help="Upload diagrams to Lucidchart"
    )
    
    args = parser.parse_args()
    
    # Initialize generator
    generator = C4DiagramGenerator(
        output_dir=args.dir,
        output_format=args.output
    )
    
    # Generate diagrams based on type
    if args.type == "context":
        file = generator.generate_context_diagram()
        if args.upload and file:
            generator.upload_to_lucidchart(file, "Rinna Context Diagram")
    
    elif args.type == "container":
        file = generator.generate_container_diagram()
        if args.upload and file:
            generator.upload_to_lucidchart(file, "Rinna Container Diagram")
    
    elif args.type == "component":
        file = generator.generate_component_diagram()
        if args.upload and file:
            generator.upload_to_lucidchart(file, "Rinna Component Diagram")
    
    elif args.type == "code":
        file = generator.generate_code_diagram()
        if args.upload and file:
            generator.upload_to_lucidchart(file, "Rinna Code Diagram")
    
    elif args.type == "clean":
        file = generator.generate_clean_architecture_diagram()
        if args.upload and file:
            generator.upload_to_lucidchart(file, "Rinna Clean Architecture Diagram")
    
    else:  # "all"
        generator.generate_all(upload=args.upload)


if __name__ == "__main__":
    main()