# UTF-8 Validation Test Files

This directory contains test files for validating the UTF-8 Check/Convert Tool functionality.

## Test File Types

### UTF-8 Valid Files
- `utf8-valid.xml` - Pure UTF-8 XML file
- `utf8-valid.txt` - UTF-8 text file

### Non-UTF-8 Files (for testing detection)
- `utf16-le.txt` - UTF-16 Little Endian with BOM
- `utf16-be.txt` - UTF-16 Big Endian with BOM  
- `windows1252.txt` - Windows-1252 encoded file
- `iso-8859-1.txt` - ISO-8859-1 encoded file

## Expected Behavior

The enhanced Java UTF-8 validation service should:

1. **Accurately detect** UTF-16 files (with and without BOM)
2. **Identify** Windows-1252 and ISO-8859-1 encoded files
3. **Auto-detect** source encodings during conversion
4. **Create backups** before conversion (.utf8backup extension)
5. **Provide detailed results** with success/failure statistics

## Testing Instructions

1. Load the plugin in Oxygen XML Author
2. Open the DILA AI Markup Assistant view
3. Select Tools > UTF-8 Check/Convert
4. Choose this test directory
5. Verify detection accuracy and conversion results