"""
Python validation functions for Buddhist citation segmentation
"""
import json
from typing import Dict, List, Union, Tuple

def validate_completion(completion: Dict) -> Tuple[bool, str]:
    """
    Basic validation for completion structure
    
    Args:
        completion: Dictionary with 'tags' and 'values' keys
        
    Returns:
        Tuple of (is_valid, error_message)
    """
    if not isinstance(completion, dict):
        return False, "Completion must be a dictionary"
    
    if 'tags' not in completion or 'values' not in completion:
        return False, "Missing 'tags' or 'values' key"
    
    tags = completion['tags']
    values = completion['values']
    
    if not isinstance(tags, list) or not isinstance(values, list):
        return False, "Tags and values must be lists"
    
    if len(tags) != len(values):
        return False, f"Length mismatch: {len(tags)} tags vs {len(values)} values"
    
    return True, "Valid"

def validate_completion_enhanced(completion: Dict, original_text: str) -> Tuple[bool, str]:
    """
    Enhanced validation with content verification
    """
    # Basic validation first
    is_valid, error = validate_completion(completion)
    if not is_valid:
        return is_valid, error
    
    # Check reconstruction
    reconstructed = ''.join(completion['values'])
    if reconstructed != original_text:
        return False, f"Reconstructed '{reconstructed}' != original '{original_text}'"
    
    # Validate tag names
    valid_tags = {'canon', 'v', 'w', 'p', 'c', 'l', 'str'}
    invalid_tags = [tag for tag in completion['tags'] if tag not in valid_tags]
    if invalid_tags:
        return False, f"Invalid tags: {invalid_tags}"
    
    return True, "Valid"

def validate_json_response(llm_output: str) -> Tuple[bool, Union[Dict, str]]:
    """
    Validate LLM JSON output and parse if valid
    """
    try:
        parsed = json.loads(llm_output)
        is_valid, error = validate_completion(parsed)
        if is_valid:
            return True, parsed
        else:
            return False, f"Structure error: {error}"
    except json.JSONDecodeError as e:
        return False, f"JSON parsing error: {str(e)}"

def validate_batch(examples: List[Dict]) -> List[Dict]:
    """
    Validate multiple examples
    """
    results = []
    for i, example in enumerate(examples):
        is_valid, error = validate_completion_enhanced(
            example['completion'], 
            example['prompt']
        )
        results.append({
            'index': i,
            'prompt': example['prompt'],
            'valid': is_valid,
            'error': error if not is_valid else None
        })
    return results

class CitationValidator:
    """
    Class-based validator with configuration options
    """
    def __init__(self, valid_tags=None, strict_mode=True):
        self.valid_tags = valid_tags or {'canon', 'v', 'w', 'p', 'c', 'l', 'str'}
        self.strict_mode = strict_mode
        
    def validate(self, completion: Dict, original_text: str = None) -> Dict:
        """
        Comprehensive validation
        """
        result = {
            'valid': True,
            'errors': [],
            'warnings': []
        }
        
        # Structure validation
        if not isinstance(completion, dict):
            result['valid'] = False
            result['errors'].append("Completion must be a dictionary")
            return result
            
        if 'tags' not in completion or 'values' not in completion:
            result['valid'] = False
            result['errors'].append("Missing 'tags' or 'values' key")
            return result
        
        tags = completion['tags']
        values = completion['values']
        
        # Array validation
        if not isinstance(tags, list) or not isinstance(values, list):
            result['valid'] = False
            result['errors'].append("Tags and values must be lists")
            return result
        
        # Length validation
        if len(tags) != len(values):
            result['valid'] = False
            result['errors'].append(f"Length mismatch: {len(tags)} tags vs {len(values)} values")
            return result
        
        # Tag validation
        invalid_tags = [tag for tag in tags if tag not in self.valid_tags]
        if invalid_tags:
            if self.strict_mode:
                result['valid'] = False
                result['errors'].append(f"Invalid tags: {invalid_tags}")
            else:
                result['warnings'].append(f"Unknown tags: {invalid_tags}")
        
        # Text reconstruction validation
        if original_text is not None:
            reconstructed = ''.join(values)
            if reconstructed != original_text:
                result['valid'] = False
                result['errors'].append(f"Text mismatch: '{reconstructed}' != '{original_text}'")
        
        return result

# Example usage
if __name__ == "__main__":
    # Test data
    test_example = {
        "prompt": "t.22,761a21-29",
        "completion": {
            "tags": ["canon", "str", "v", "str", "p", "c", "l", "str", "l"],
            "values": ["T", ".", "22", ",", "761", "a", "21", "-", "29"]
        }
    }
    
    # Basic validation
    is_valid, error = validate_completion(test_example["completion"])
    print(f"Basic validation: {is_valid} - {error}")
    
    # Enhanced validation
    is_valid, error = validate_completion_enhanced(
        test_example["completion"], 
        test_example["prompt"]
    )
    print(f"Enhanced validation: {is_valid} - {error}")
    
    # Class-based validation
    validator = CitationValidator()
    result = validator.validate(test_example["completion"], test_example["prompt"])
    print(f"Class validation: {result}")
