from setuptools import setup, find_packages
import os
import sys

# Determine which package to install based on environment
package_name = os.environ.get("SETUP_PACKAGE", "rinna")

# Find all packages
packages = find_packages()

if package_name == "rinna":
    # Main Rinna package
    setup(
        name="rinna",
        version="0.1.0",
        packages=["rinna"] + [f"rinna.{pkg}" for pkg in find_packages(where="rinna")],
        description="Rinna Python package",
        author="Rinna Project",
        author_email="noreply@example.com",
        url="https://github.com/example/rinna",
        classifiers=[
            "Programming Language :: Python :: 3",
            "License :: OSI Approved :: MIT License",
            "Operating System :: OS Independent",
        ],
        python_requires=">=3.8",
    )
elif package_name == "lucidchart-py":
    # Mock Lucidchart API client for diagram integrations
    setup(
        name="lucidchart-py",
        version="0.1.0",
        packages=["lucidchart_py"],
        description="Mock Lucidchart API Python client",
        author="Rinna Project",
        author_email="noreply@example.com",
        url="https://github.com/example/lucidchart-py",
        classifiers=[
            "Programming Language :: Python :: 3",
            "License :: OSI Approved :: MIT License",
            "Operating System :: OS Independent",
        ],
        python_requires=">=3.8",
    )
else:
    print(f"Unknown package: {package_name}")
    print("Set SETUP_PACKAGE environment variable to 'rinna' or 'lucidchart-py'")
    sys.exit(1)