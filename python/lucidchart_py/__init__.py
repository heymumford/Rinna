"""
Mock Lucidchart API Python client.

This is a placeholder module that provides mock functionality
for the lucidchart-py package.
"""

__version__ = "0.1.0"


class LucidchartClient:
    """Mock client for Lucidchart API."""
    
    def __init__(self, api_token=None):
        """Initialize the client with an API token."""
        self.api_token = api_token
        self.authenticated = api_token is not None
        
    def authenticate(self, api_token):
        """Authenticate with the Lucidchart API."""
        self.api_token = api_token
        self.authenticated = True
        return True
        
    def upload_diagram(self, diagram_path, name=None, folder_id=None):
        """Mock uploading a diagram to Lucidchart."""
        if not self.authenticated:
            raise Exception("Not authenticated")
            
        # Just pretend it worked
        diagram_id = "mock-diagram-12345"
        
        return {
            "id": diagram_id,
            "name": name or "Uploaded Diagram",
            "url": f"https://lucid.app/documents/{diagram_id}"
        }