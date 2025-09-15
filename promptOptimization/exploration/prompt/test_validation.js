// Simple validation test for your JSON files
const fs = require('fs');

function validateCompletion(completion) {
    if (!completion.tags || !completion.values) {
        return { valid: false, error: "Missing tags or values" };
    }
    
    if (completion.tags.length !== completion.values.length) {
        return { 
            valid: false, 
            error: `Length mismatch: ${completion.tags.length} tags vs ${completion.values.length} values` 
        };
    }
    
    return { valid: true };
}

function validateFile(filename) {
    try {
        const data = JSON.parse(fs.readFileSync(filename, 'utf8'));
        console.log(`\nValidating: ${filename}`);
        console.log('='.repeat(50));
        
        if (Array.isArray(data)) {
            data.forEach(item => {
                if (item.examples) {
                    item.examples.forEach((example, i) => {
                        const result = validateCompletion(example.completion);
                        const status = result.valid ? '✅' : '❌';
                        const reconstructed = example.completion.values.join('');
                        const matches = reconstructed === example.prompt ? '✅' : '❌';
                        
                        console.log(`${status} Example ${i+1}: "${example.prompt}"`);
                        console.log(`   Tags: ${example.completion.tags.length}, Values: ${example.completion.values.length}`);
                        console.log(`   Reconstruction ${matches}: "${reconstructed}"`);
                        if (!result.valid) console.log(`   Error: ${result.error}`);
                        console.log('');
                    });
                }
            });
        }
    } catch (error) {
        console.log(`❌ Error reading ${filename}: ${error.message}`);
    }
}

// Test your files
validateFile('ref_segment_V1.json');
validateFile('ref_segment_optimized_small_llm.json');
