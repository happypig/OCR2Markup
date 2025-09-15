.\.venv\Scripts\python.exe validate_completion.py ref_segment_V1.json 2>&1#!/usr/bin/env python3
"""
Simple CLI tool to validate Buddhist citation completions
Usage: python validate_completion.py <json_file>
"""

import sys
import json
import argparse
from pathlib import Path

def validate_single_completion(completion_data):
    """Validate a single completion entry"""
    if 'completion' not in completion_data:
        return False, "No 'completion' field found"
    
    completion = completion_data['completion']
    
    if 'tags' not in completion or 'values' not in completion:
        return False, "Missing 'tags' or 'values' in completion"
    
    tags = completion['tags']
    values = completion['values']
    
    if len(tags) != len(values):
        return False, f"Length mismatch: {len(tags)} tags vs {len(values)} values"
    
    # Check if values reconstruct the prompt
    if 'prompt' in completion_data:
        reconstructed = ''.join(values)
        original = completion_data['prompt']
        if reconstructed != original:
            return False, f"Reconstruction mismatch: '{reconstructed}' != '{original}'"
    
    return True, "Valid"

def validate_file(file_path):
    """Validate a JSON file containing completion examples"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        if isinstance(data, list):
            # Handle array of completion objects
            for item in data:
                if 'examples' in item:
                    examples = item['examples']
                    for i, example in enumerate(examples):
                        is_valid, error = validate_single_completion(example)
                        status = "✅" if is_valid else "❌"
                        print(f"{status} Example {i+1}: {example.get('prompt', 'No prompt')} - {error}")
        else:
            # Handle single completion object
            is_valid, error = validate_single_completion(data)
            status = "✅" if is_valid else "❌"
            print(f"{status} {error}")
            
    except FileNotFoundError:
        print(f"❌ File not found: {file_path}")
    except json.JSONDecodeError as e:
        print(f"❌ JSON parsing error: {e}")
    except Exception as e:
        print(f"❌ Error: {e}")

def main():
    parser = argparse.ArgumentParser(description='Validate Buddhist citation completions')
    parser.add_argument('file', help='JSON file to validate')
    parser.add_argument('--verbose', '-v', action='store_true', help='Verbose output')
    
    args = parser.parse_args()
    
    file_path = Path(args.file)
    if not file_path.exists():
        print(f"❌ File does not exist: {file_path}")
        sys.exit(1)
    
    print(f"Validating: {file_path}")
    print("-" * 50)
    
    validate_file(file_path)

if __name__ == "__main__":
    main()
