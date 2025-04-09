import json
import os
import subprocess
import unittest
from pathlib import Path


class TestCliIntegration(unittest.TestCase):
    """Integration tests for Python-CLI interactions."""

    def setUp(self):
        """Set up the test environment."""
        # Determine paths
        self.project_root = Path(__file__).parents[3]
        self.cli_path = self.project_root / "bin" / "rin"
        
        # Skip tests if CLI not available
        if not self.cli_path.exists():
            self.skipTest(f"CLI executable not found at {self.cli_path}")
        
        # Create a temporary work item for testing
        self.work_item_id = "WI-999"  # We'll use a fixed ID for testing
        
    def test_cli_version_command(self):
        """Test that the CLI version command works correctly."""
        # Run the version command
        try:
            result = subprocess.run(
                [str(self.cli_path), "version", "current"],
                capture_output=True,
                text=True,
                check=True,
            )
            
            # Verify output
            self.assertIn("version", result.stdout.lower())
            self.assertEqual(0, result.returncode)
        except subprocess.CalledProcessError as e:
            self.fail(f"CLI version command failed with: {e}")
    
    def test_cli_json_output(self):
        """Test that the CLI can output JSON that Python can parse."""
        try:
            # Run a command that outputs JSON
            result = subprocess.run(
                [str(self.cli_path), "view", self.work_item_id, "--format=json"],
                capture_output=True,
                text=True,
            )
            
            # If the work item doesn't exist, this test won't be relevant
            if "not found" in result.stderr:
                self.skipTest(f"Work item {self.work_item_id} not found - skipping test")
                
            # Try to parse the JSON output
            try:
                data = json.loads(result.stdout)
                
                # Verify the parsed data
                self.assertIsInstance(data, dict)
                self.assertEqual(data.get("id"), self.work_item_id)
            except json.JSONDecodeError:
                self.fail("CLI output was not valid JSON")
                
        except subprocess.CalledProcessError as e:
            self.fail(f"CLI command failed with: {e}")
    
    def test_cli_environment_integration(self):
        """Test that the CLI properly integrates with environment variables."""
        # Set a test environment variable
        test_env = os.environ.copy()
        test_env["RINNA_OUTPUT_FORMAT"] = "json"
        
        try:
            # Run command with the environment variable
            result = subprocess.run(
                [str(self.cli_path), "view", self.work_item_id],  # No format arg
                capture_output=True,
                text=True,
                env=test_env,
            )
            
            # Skip if work item not found
            if "not found" in result.stderr:
                self.skipTest(f"Work item {self.work_item_id} not found - skipping test")
            
            # Verify the output was in JSON format (due to env var)
            self.assertTrue(
                result.stdout.strip().startswith("{"),
                "Output should be JSON due to environment variable",
            )
        except subprocess.CalledProcessError as e:
            self.fail(f"CLI command failed with: {e}")
    
    def test_cli_python_interprocess_communication(self):
        """Test Python's ability to communicate with the CLI."""
        # Create a subprocess to use for IPC testing
        process = subprocess.Popen(
            [str(self.cli_path), "view", self.work_item_id, "--format=json"],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        
        # Get output from the process
        stdout, stderr = process.communicate()
        
        # Check if work item exists
        if "not found" in stderr:
            self.skipTest(f"Work item {self.work_item_id} not found - skipping test")
        
        # Check process exit code
        self.assertEqual(0, process.returncode, f"Process failed with: {stderr}")
        
        # Verify output
        try:
            data = json.loads(stdout)
            self.assertIsInstance(data, dict)
        except json.JSONDecodeError:
            self.fail("CLI output was not valid JSON")


if __name__ == "__main__":
    unittest.main()