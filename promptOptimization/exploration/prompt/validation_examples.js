// JavaScript validation examples for Buddhist citation segmentation

/**
 * Basic validation function
 */
function validateCompletion(completion) {
    if (!completion || !completion.tags || !completion.values) {
        return { valid: false, error: "Missing tags or values array" };
    }
    
    if (!Array.isArray(completion.tags) || !Array.isArray(completion.values)) {
        return { valid: false, error: "Tags and values must be arrays" };
    }
    
    if (completion.tags.length !== completion.values.length) {
        return { 
            valid: false, 
            error: `Length mismatch: ${completion.tags.length} tags vs ${completion.values.length} values` 
        };
    }
    
    return { valid: true };
}

/**
 * Enhanced validation with content checks
 */
function validateCompletionEnhanced(completion, originalText) {
    // Basic structure validation
    const basicValidation = validateCompletion(completion);
    if (!basicValidation.valid) {
        return basicValidation;
    }
    
    // Check if reconstructed text matches original
    const reconstructed = completion.values.join('');
    if (reconstructed !== originalText) {
        return {
            valid: false,
            error: `Reconstructed text "${reconstructed}" doesn't match original "${originalText}"`
        };
    }
    
    // Check for valid tags
    const validTags = ['canon', 'v', 'w', 'p', 'c', 'l', 'str'];
    const invalidTags = completion.tags.filter(tag => !validTags.includes(tag));
    if (invalidTags.length > 0) {
        return {
            valid: false,
            error: `Invalid tags found: ${invalidTags.join(', ')}`
        };
    }
    
    return { valid: true };
}

/**
 * Batch validation for multiple examples
 */
function validateBatch(examples) {
    const results = [];
    
    examples.forEach((example, index) => {
        const validation = validateCompletionEnhanced(example.completion, example.prompt);
        results.push({
            index,
            prompt: example.prompt,
            valid: validation.valid,
            error: validation.error || null
        });
    });
    
    return results;
}

/**
 * Real-time validation during LLM output generation
 */
function validatePartialCompletion(partialCompletion) {
    try {
        const parsed = JSON.parse(partialCompletion);
        
        // Check if we have both arrays
        if (parsed.tags && parsed.values) {
            if (parsed.tags.length > parsed.values.length) {
                return { status: "incomplete", message: "More tags than values" };
            } else if (parsed.tags.length < parsed.values.length) {
                return { status: "error", message: "More values than tags" };
            } else {
                return { status: "valid", message: "Arrays are balanced" };
            }
        }
        
        return { status: "incomplete", message: "Waiting for complete structure" };
    } catch (e) {
        return { status: "parsing", message: "Invalid JSON format" };
    }
}

// Example usage:
const testExample = {
    prompt: "t.22,761a21-29",
    completion: {
        tags: ["canon", "str", "v", "str", "p", "c", "l", "str", "l"],
        values: ["T", ".", "22", ",", "761", "a", "21", "-", "29"]
    }
};

console.log("Basic validation:", validateCompletion(testExample.completion));
console.log("Enhanced validation:", validateCompletionEnhanced(testExample.completion, testExample.prompt));

// Export for use in other modules
module.exports = {
    validateCompletion,
    validateCompletionEnhanced,
    validateBatch,
    validatePartialCompletion
};
