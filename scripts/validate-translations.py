#!/usr/bin/env python3
"""
Translation Key Consistency Validator
Checks consistency between Java code and translation.xml
"""

import re
import xml.etree.ElementTree as ET
import os
import sys
from pathlib import Path

class TranslationValidator:
    def __init__(self, project_root):
        self.project_root = Path(project_root)
        self.java_src = self.project_root / "Models/Gemini2.5/dila-ai-markup-plugin/src/main/java"
        self.translation_file = self.project_root / "Models/Gemini2.5/dila-ai-markup-plugin/src/main/resources/i18n/translation.xml"
        
    def extract_keys_from_xml(self):
        """Extract all translation keys from XML file"""
        try:
            tree = ET.parse(self.translation_file)
            root = tree.getroot()
            keys = set()
            
            for key_elem in root.findall('.//key'):
                key_value = key_elem.get('value')
                if key_value:
                    keys.add(key_value)
                    
            return keys
        except Exception as e:
            print(f"Error reading translation.xml: {e}")
            return set()
    
    def extract_keys_from_java(self):
        """Extract all i18n keys used in Java code"""
        used_keys = set()
        java_files = list(self.java_src.rglob("*.java"))
        
        # Pattern to match i18n("key") or i18n("key", params...)
        pattern = r'i18n\s*\(\s*["\']([^"\']+)["\']'
        
        for java_file in java_files:
            try:
                with open(java_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    matches = re.findall(pattern, content)
                    for match in matches:
                        used_keys.add(match)
                        
            except Exception as e:
                print(f"Error reading {java_file}: {e}")
                
        return used_keys
    
    def validate(self):
        """Main validation method"""
        print("🔍 Translation Key Consistency Validator")
        print("=" * 50)
        
        xml_keys = self.extract_keys_from_xml()
        java_keys = self.extract_keys_from_java()
        
        print(f"📄 Translation XML keys: {len(xml_keys)}")
        print(f"☕ Java code keys: {len(java_keys)}")
        print()
        
        # Find unused keys (in XML but not in Java)
        unused_keys = xml_keys - java_keys
        
        # Find missing keys (in Java but not in XML)
        missing_keys = java_keys - xml_keys
        
        # Results
        if not unused_keys and not missing_keys:
            print("✅ Perfect consistency! All keys match.")
            return True
            
        success = True
        
        if unused_keys:
            print(f"⚠️  UNUSED KEYS ({len(unused_keys)} keys in XML not used in Java):")
            for key in sorted(unused_keys):
                print(f"   - {key}")
            print()
            success = False
            
        if missing_keys:
            print(f"❌ MISSING KEYS ({len(missing_keys)} keys used in Java but not in XML):")
            for key in sorted(missing_keys):
                print(f"   - {key}")
            print()
            success = False
            
        # Summary
        usage_rate = (len(java_keys) / len(xml_keys)) * 100 if xml_keys else 0
        print(f"📊 Usage Rate: {usage_rate:.1f}% ({len(java_keys)}/{len(xml_keys)} keys used)")
        
        return success

if __name__ == "__main__":
    project_root = Path(__file__).parent.parent
    validator = TranslationValidator(project_root)
    
    success = validator.validate()
    sys.exit(0 if success else 1)