#!/usr/bin/env python3
"""
I18n Key Distribution Analyzer
Compiles and analyzes i18n key usage across JavaScript and Java source files.
Generates a comprehensive CSV report showing key distribution and missing keys.
"""

import os
import re
import csv
import xml.etree.ElementTree as ET
from pathlib import Path
from collections import defaultdict, OrderedDict

class I18nKeyAnalyzer:
    def __init__(self):
        self.base_path = Path(r"C:\Project\OCR2Markup")
        self.js_files = {
            'bk': self.base_path / "Models/Gemini2.5/jsCopy/dila-ai-markup-bk.js",
            'enhanced': self.base_path / "Models/Gemini2.5/jsCopy/dila-ai-markup-enhanced.js",
            'legacy': self.base_path / "Models/Gemini2.5/jsCopy/dila-ai-markup-legacy.js"
        }
        self.java_files = [
            self.base_path / "Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/workspace/DAMAWorkspaceAccessPluginExtension.java",
            self.base_path / "Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/preferences/DAMAOptionPagePluginExtension.java",
            self.base_path / "Models/Gemini2.5/dila-ai-markup-plugin/src/main/java/com/dila/dama/plugin/utf8/UTF8ValidationService.java"
        ]
        self.translation_file = self.base_path / "Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/i18n/translation.xml"
        
        # Pattern to match i18n key usage
        self.js_i18n_pattern = re.compile(r'i18nFn\s*\(\s*["\']([^"\']+)["\']')
        self.java_i18n_pattern = re.compile(r'i18n\s*\(\s*["\']([^"\']+)["\']')
        self.java_getmessage_pattern = re.compile(r'getMessage\s*\(\s*["\']([^"\']+)["\']')
        
        # Storage for extracted data
        self.translation_keys = set()
        self.js_keys = {'bk': set(), 'enhanced': set(), 'legacy': set()}
        self.java_keys = set()
        self.missing_keys = {'bk': set(), 'enhanced': set(), 'legacy': set(), 'java': set()}

    def extract_translation_keys(self):
        """Extract all keys from translation.xml"""
        print(f"📖 Reading translation keys from: {self.translation_file}")
        
        if not self.translation_file.exists():
            print(f"❌ Translation file not found: {self.translation_file}")
            return
        
        try:
            tree = ET.parse(self.translation_file)
            root = tree.getroot()
            
            for key_elem in root.findall('.//key[@value]'):
                key_value = key_elem.get('value')
                if key_value:
                    self.translation_keys.add(key_value)
            
            print(f"✅ Found {len(self.translation_keys)} translation keys")
            
        except ET.ParseError as e:
            print(f"❌ Error parsing translation.xml: {e}")
        except Exception as e:
            print(f"❌ Unexpected error reading translation.xml: {e}")

    def extract_js_keys(self, file_type, file_path):
        """Extract i18n keys from JavaScript file"""
        print(f"📄 Analyzing JS file ({file_type}): {file_path.name}")
        
        if not file_path.exists():
            print(f"❌ JavaScript file not found: {file_path}")
            return
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Find all i18nFn() calls
            matches = self.js_i18n_pattern.findall(content)
            keys = set(matches)
            
            self.js_keys[file_type] = keys
            print(f"✅ Found {len(keys)} i18n keys in {file_type}.js")
            
            # Identify missing keys (not in translation.xml)
            missing = keys - self.translation_keys
            if missing:
                self.missing_keys[file_type] = missing
                print(f"⚠️  Found {len(missing)} missing keys in {file_type}.js: {sorted(missing)}")
                
        except Exception as e:
            print(f"❌ Error reading {file_type}.js: {e}")

    def extract_java_keys(self):
        """Extract i18n keys from all Java files"""
        print("☕ Analyzing Java files...")
        all_java_keys = set()
        
        for java_file in self.java_files:
            if not java_file.exists():
                print(f"❌ Java file not found: {java_file}")
                continue
            
            print(f"📄 Processing: {java_file.name}")
            
            try:
                with open(java_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Find i18n() calls
                i18n_matches = self.java_i18n_pattern.findall(content)
                
                # Find getMessage() calls
                getmessage_matches = self.java_getmessage_pattern.findall(content)
                
                file_keys = set(i18n_matches + getmessage_matches)
                all_java_keys.update(file_keys)
                
                print(f"✅ Found {len(file_keys)} i18n keys in {java_file.name}")
                
            except Exception as e:
                print(f"❌ Error reading {java_file.name}: {e}")
        
        self.java_keys = all_java_keys
        print(f"✅ Total Java keys: {len(all_java_keys)}")
        
        # Identify missing keys (not in translation.xml)
        missing = all_java_keys - self.translation_keys
        if missing:
            self.missing_keys['java'] = missing
            print(f"⚠️  Found {len(missing)} missing keys in Java files: {sorted(missing)}")

    def generate_csv_report(self, output_file):
        """Generate comprehensive CSV report"""
        print(f"📊 Generating CSV report: {output_file}")
        
        # Collect all unique keys (translation + missing)
        all_keys = set(self.translation_keys)
        for category_keys in self.missing_keys.values():
            all_keys.update(category_keys)
        
        # Sort keys for consistent output
        sorted_keys = sorted(all_keys)
        
        # Prepare CSV data
        csv_data = []
        
        # Header
        header = ['Translation Key', 'dila-ai-markup-bk.js', 'dila-ai-markup-enhanced.js', 'dila-ai-markup-legacy.js', 'Java Files']
        csv_data.append(header)
        
        # Process each key
        for key in sorted_keys:
            row = [key]
            
            # Check usage in each JS file
            for js_type in ['bk', 'enhanced', 'legacy']:
                if key in self.js_keys[js_type]:
                    row.append('✓')
                else:
                    row.append('')
            
            # Check usage in Java files
            if key in self.java_keys:
                row.append('✓')
            else:
                row.append('')
            
            csv_data.append(row)
        
        # Add missing keys section
        if any(self.missing_keys.values()):
            # Add separator row
            csv_data.append(['--- MISSING KEYS (Not in translation.xml) ---', '', '', '', ''])
            
            # Add missing keys with suffix indicators
            all_missing = set()
            for category_keys in self.missing_keys.values():
                all_missing.update(category_keys)
            
            for missing_key in sorted(all_missing):
                suffixes = []
                if missing_key in self.missing_keys['bk']:
                    suffixes.append('bk')
                if missing_key in self.missing_keys['enhanced']:
                    suffixes.append('enhanced')
                if missing_key in self.missing_keys['legacy']:
                    suffixes.append('legacy')
                if missing_key in self.missing_keys['java']:
                    suffixes.append('java')
                
                row = [f"{missing_key}-{'+'.join(suffixes)}"]
                
                # Mark usage columns
                for js_type in ['bk', 'enhanced', 'legacy']:
                    if missing_key in self.missing_keys[js_type]:
                        row.append('❌')
                    else:
                        row.append('')
                
                # Java column
                if missing_key in self.missing_keys['java']:
                    row.append('❌')
                else:
                    row.append('')
                
                csv_data.append(row)
        
        # Write CSV file
        try:
            with open(output_file, 'w', newline='', encoding='utf-8') as csvfile:
                writer = csv.writer(csvfile)
                writer.writerows(csv_data)
            
            print(f"✅ CSV report generated successfully: {output_file}")
            print(f"📈 Report includes:")
            print(f"   • {len(self.translation_keys)} translation keys")
            print(f"   • {len(self.js_keys['bk'])} keys from bk.js")
            print(f"   • {len(self.js_keys['enhanced'])} keys from enhanced.js")
            print(f"   • {len(self.js_keys['legacy'])} keys from legacy.js")
            print(f"   • {len(self.java_keys)} keys from Java files")
            
            total_missing = sum(len(keys) for keys in self.missing_keys.values())
            if total_missing > 0:
                print(f"   • {total_missing} missing keys requiring attention")
            
        except Exception as e:
            print(f"❌ Error writing CSV file: {e}")

    def print_summary(self):
        """Print analysis summary"""
        print("\n" + "="*60)
        print("📋 I18N KEY ANALYSIS SUMMARY")
        print("="*60)
        
        print(f"\n📖 Translation Keys (translation.xml): {len(self.translation_keys)}")
        
        print(f"\n📄 JavaScript Files:")
        for js_type in ['bk', 'enhanced', 'legacy']:
            keys_count = len(self.js_keys[js_type])
            missing_count = len(self.missing_keys[js_type])
            print(f"   • {js_type:8}: {keys_count:3} keys ({missing_count} missing)")
        
        print(f"\n☕ Java Files: {len(self.java_keys)} keys ({len(self.missing_keys['java'])} missing)")
        
        if any(self.missing_keys.values()):
            print(f"\n⚠️  MISSING KEYS (require translation.xml entries):")
            for category, missing in self.missing_keys.items():
                if missing:
                    print(f"   • {category:8}: {sorted(missing)}")

    def run_analysis(self):
        """Run complete i18n key analysis"""
        print("🚀 Starting I18n Key Distribution Analysis")
        print("="*60)
        
        # Extract all keys
        self.extract_translation_keys()
        
        # Process JavaScript files
        for js_type, file_path in self.js_files.items():
            self.extract_js_keys(js_type, file_path)
        
        # Process Java files
        self.extract_java_keys()
        
        # Generate report
        output_file = self.base_path / "scripts/i18n_distribution_py_report.csv"
        self.generate_csv_report(output_file)
        
        # Print summary
        self.print_summary()
        
        print(f"\n✅ Analysis complete! Report saved to: {output_file}")
        return output_file

def main():
    """Main entry point"""
    analyzer = I18nKeyAnalyzer()
    analyzer.run_analysis()

if __name__ == "__main__":
    main()