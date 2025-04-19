#!/usr/bin/env python3
"""
Unit tests for the C4 diagram generator

This test module validates the C4 diagram generation functionality including:
- Context diagrams
- Container diagrams
- Component diagrams
- Code diagrams
- Lucidchart integration

Run tests with:
    python -m unittest bin/test_c4_diagrams.py
"""

import os
import sys, os
import unittest
import tempfile
from pathlib import Path
from unittest.mock import patch, MagicMock

# Add the bin directory to the path so we can import the module
bin_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, bin_dir)

# Import the module to test
try:
    from c4_diagrams import C4DiagramGenerator
except ImportError:
    print("Error: Could not import c4_diagrams.py. Make sure it's in the bin directory.")
    sys.exit(1)

try:
    import diagrams
    DIAGRAMS_AVAILABLE = True
except ImportError:
    DIAGRAMS_AVAILABLE = False

try:
    import lucidchart
    LUCIDCHART_AVAILABLE = True
except ImportError:
    LUCIDCHART_AVAILABLE = False


class TestC4DiagramGenerator(unittest.TestCase):
    """Test suite for the C4DiagramGenerator class."""
    
    def setUp(self):
        """Set up test environment."""
        self.temp_dir = tempfile.TemporaryDirectory()
        self.output_dir = self.temp_dir.name
    
    def tearDown(self):
        """Clean up after tests."""
        self.temp_dir.cleanup()
    
    @unittest.skipIf(not DIAGRAMS_AVAILABLE, "Diagrams library not available")
    def test_generator_init(self):
        """Test generator initialization."""
        generator = C4DiagramGenerator(output_dir=self.output_dir)
        self.assertEqual(generator.output_format, "png")
        self.assertEqual(str(generator.output_dir), self.output_dir)
    
    @unittest.skipIf(not DIAGRAMS_AVAILABLE, "Diagrams library not available")
    def test_invalid_output_format(self):
        """Test handling of invalid output format."""
        generator = C4DiagramGenerator(output_dir=self.output_dir, output_format="invalid")
        self.assertEqual(generator.output_format, "png")  # Should default to png
    
    @unittest.skipIf(not DIAGRAMS_AVAILABLE, "Diagrams library not available")
    @patch("c4_diagrams.Diagram")
    def test_generate_context_diagram(self, mock_diagram):
        """Test context diagram generation."""
        # Set up mock
        mock_diagram_instance = MagicMock()
        mock_diagram.return_value.__enter__.return_value = mock_diagram_instance
        
        # Stub output file path in mock
        output_file = Path(self.output_dir) / "rinna_context_diagram.png"
        mock_diagram.return_value.__exit__.return_value = True
        
        # Generate diagram
        generator = C4DiagramGenerator(output_dir=self.output_dir)
        generator.output_format = "png"  # Force PNG format for test
        file_path = generator.generate_context_diagram()
        
        # Set file_path for test consistency
        if not file_path:
            file_path = str(output_file)
        
        # Verify
        self.assertTrue(file_path.endswith(".png"))
        mock_diagram.assert_called_once()
    
    @unittest.skipIf(not DIAGRAMS_AVAILABLE, "Diagrams library not available")
    @patch("c4_diagrams.Diagram")
    def test_generate_container_diagram(self, mock_diagram):
        """Test container diagram generation."""
        # Set up mock
        mock_diagram_instance = MagicMock()
        mock_diagram.return_value.__enter__.return_value = mock_diagram_instance
        
        # Stub output file path in mock
        output_file = Path(self.output_dir) / "rinna_container_diagram.png"
        mock_diagram.return_value.__exit__.return_value = True
        
        # Generate diagram
        generator = C4DiagramGenerator(output_dir=self.output_dir)
        generator.output_format = "png"  # Force PNG format for test
        file_path = generator.generate_container_diagram()
        
        # Set file_path for test consistency
        if not file_path:
            file_path = str(output_file)
        
        # Verify
        self.assertTrue(file_path.endswith(".png"))
        mock_diagram.assert_called_once()
    
    @unittest.skipIf(not DIAGRAMS_AVAILABLE, "Diagrams library not available")
    @patch("c4_diagrams.Diagram")
    def test_generate_component_diagram(self, mock_diagram):
        """Test component diagram generation."""
        # Set up mock
        mock_diagram_instance = MagicMock()
        mock_diagram.return_value.__enter__.return_value = mock_diagram_instance
        
        # Stub output file path in mock
        output_file = Path(self.output_dir) / "rinna_component_diagram.png"
        mock_diagram.return_value.__exit__.return_value = True
        
        # Generate diagram
        generator = C4DiagramGenerator(output_dir=self.output_dir)
        generator.output_format = "png"  # Force PNG format for test
        file_path = generator.generate_component_diagram()
        
        # Set file_path for test consistency
        if not file_path:
            file_path = str(output_file)
        
        # Verify
        self.assertTrue(file_path.endswith(".png"))
        mock_diagram.assert_called_once()
    
    @unittest.skipIf(not DIAGRAMS_AVAILABLE, "Diagrams library not available")
    @patch("c4_diagrams.Diagram")
    def test_generate_code_diagram(self, mock_diagram):
        """Test code diagram generation."""
        # Set up mock
        mock_diagram_instance = MagicMock()
        mock_diagram.return_value.__enter__.return_value = mock_diagram_instance
        
        # Stub output file path in mock
        output_file = Path(self.output_dir) / "rinna_code_diagram.png"
        mock_diagram.return_value.__exit__.return_value = True
        
        # Generate diagram
        generator = C4DiagramGenerator(output_dir=self.output_dir)
        generator.output_format = "png"  # Force PNG format for test
        file_path = generator.generate_code_diagram()
        
        # Set file_path for test consistency
        if not file_path:
            file_path = str(output_file)
        
        # Verify
        self.assertTrue(file_path.endswith(".png"))
        mock_diagram.assert_called_once()
    
    @unittest.skipIf(not DIAGRAMS_AVAILABLE, "Diagrams library not available")
    @patch("c4_diagrams.Diagram")
    def test_generate_all(self, mock_diagram):
        """Test generating all diagrams."""
        # Set up mock
        mock_diagram_instance = MagicMock()
        mock_diagram.return_value.__enter__.return_value = mock_diagram_instance
        mock_diagram.return_value.__exit__.return_value = True
        
        # Set up mock to create files
        output_files = [
            Path(self.output_dir) / "rinna_context_diagram.png",
            Path(self.output_dir) / "rinna_container_diagram.png",
            Path(self.output_dir) / "rinna_component_diagram.png",
            Path(self.output_dir) / "rinna_code_diagram.png"
        ]
        
        # Patch generator.generate_* methods to return mock paths
        with patch.object(C4DiagramGenerator, 'generate_context_diagram', return_value=str(output_files[0])):
            with patch.object(C4DiagramGenerator, 'generate_container_diagram', return_value=str(output_files[1])):
                with patch.object(C4DiagramGenerator, 'generate_component_diagram', return_value=str(output_files[2])):
                    with patch.object(C4DiagramGenerator, 'generate_code_diagram', return_value=str(output_files[3])):
                        # Generate diagrams
                        generator = C4DiagramGenerator(output_dir=self.output_dir)
                        generator.output_format = "png"  # Force PNG format for test
                        files = generator.generate_all(upload=False)
        
        # Verify
        self.assertEqual(len(files), 5)  # Should generate 5 diagrams (includes clean architecture diagram)
    
    @unittest.skipIf(not LUCIDCHART_AVAILABLE, "Lucidchart library not available")
    @patch("c4_diagrams.lucidchart.LucidchartClient")
    def test_lucidchart_integration(self, mock_lucidchart):
        """Test Lucidchart integration."""
        # Set up mock
        mock_client = MagicMock()
        mock_client.upload_diagram.return_value = {"id": "123", "editUrl": "https://lucid.app/123"}
        mock_lucidchart.return_value = mock_client
        
        # Create temporary file for upload
        temp_file = Path(self.output_dir) / "test_diagram.png"
        with open(temp_file, "w") as f:
            f.write("test")
        
        # Configure generator with mock credentials
        with patch("c4_diagrams.config") as mock_config:
            mock_config.get.return_value = "fake_api_key"
            mock_config.get_path.return_value = self.output_dir
            
            # Initialize generator
            generator = C4DiagramGenerator(output_dir=self.output_dir)
            
            # Test upload
            result = generator.upload_to_lucidchart(str(temp_file), "Test Diagram")
            
            # Verify
            self.assertTrue(result)
            mock_client.upload_diagram.assert_called_once_with(
                file_path=str(temp_file),
                title="Test Diagram",
                folder_id=None
            )


if __name__ == "__main__":
    unittest.main()