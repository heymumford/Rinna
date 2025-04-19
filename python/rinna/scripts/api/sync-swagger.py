#\!/usr/bin/env python3
"""
Synchronize Swagger YAML and JSON files.

This script converts between Swagger YAML and JSON formats to ensure 
documentation consistency across different representations.
"""

import argparse
import json
import os
import sys, os
import yaml


def convert_yaml_to_json(yaml_file, json_file):
    """
    Convert YAML file to JSON.
    
    Args:
        yaml_file: Path to input YAML file
        json_file: Path to output JSON file
    """
    print(f"Converting {yaml_file} to {json_file}")
    try:
        with open(yaml_file, 'r', encoding='utf-8') as f:
            yaml_content = yaml.safe_load(f)
        
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(yaml_content, f, indent=2)
        
        print(f"Successfully converted {yaml_file} to {json_file}")
    except yaml.YAMLError as e:
        print(f"YAML parsing error: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


def convert_json_to_yaml(json_file, yaml_file):
    """
    Convert JSON file to YAML.
    
    Args:
        json_file: Path to input JSON file
        yaml_file: Path to output YAML file
    """
    print(f"Converting {json_file} to {yaml_file}")
    try:
        with open(json_file, 'r', encoding='utf-8') as f:
            json_content = json.load(f)
        
        with open(yaml_file, 'w', encoding='utf-8') as f:
            yaml.dump(json_content, f, sort_keys=False, default_flow_style=False)
        
        print(f"Successfully converted {json_file} to {yaml_file}")
    except json.JSONDecodeError as e:
        print(f"JSON parsing error: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


def validate_swagger_yaml(yaml_file):
    """
    Validate Swagger YAML file.
    
    Args:
        yaml_file: Path to YAML file to validate
        
    Returns:
        bool: True if valid, False otherwise
    """
    print(f"Validating {yaml_file}")
    try:
        with open(yaml_file, 'r', encoding='utf-8') as f:
            yaml_content = yaml.safe_load(f)
        
        # Basic validation - ensure required OpenAPI fields exist
        required_fields = ['swagger', 'info', 'paths']
        for field in required_fields:
            if field not in yaml_content:
                print(f"Error: Missing required field '{field}' in Swagger YAML")
                return False
                
        print(f"YAML validation passed for {yaml_file}")
        return True
    except yaml.YAMLError as e:
        print(f"YAML validation error: {e}")
        return False
    except Exception as e:
        print(f"Error during validation: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(description='Swagger YAML/JSON synchronization tool')
    parser.add_argument('--yaml', help='Path to the Swagger YAML file', default='/home/emumford/NativeLinuxProjects/Rinna/api/swagger.yaml')
    parser.add_argument('--json', help='Path to the Swagger JSON file', default='/home/emumford/NativeLinuxProjects/Rinna/api/docs/swagger.json')
    parser.add_argument('--direction', choices=['yaml-to-json', 'json-to-yaml', 'both'], 
                        default='yaml-to-json', help='Conversion direction')
    parser.add_argument('--validate', action='store_true', help='Validate YAML without conversion')
    
    args = parser.parse_args()
    
    if args.validate:
        if validate_swagger_yaml(args.yaml):
            print("Validation successful")
            sys.exit(0)
        else:
            print("Validation failed")
            sys.exit(1)
    
    if args.direction in ['yaml-to-json', 'both']:
        if validate_swagger_yaml(args.yaml):
            convert_yaml_to_json(args.yaml, args.json)
        else:
            print("YAML validation failed - not converting to JSON")
            sys.exit(1)
    
    if args.direction in ['json-to-yaml', 'both']:
        try:
            convert_json_to_yaml(args.json, args.yaml)
            # Re-validate after JSON to YAML conversion
            validate_swagger_yaml(args.yaml)
        except Exception as e:
            print(f"Error converting JSON to YAML: {e}")
            sys.exit(1)


if __name__ == "__main__":
    main()
