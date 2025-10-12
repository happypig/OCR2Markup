package com.dila.dama.plugin.i18n;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test to validate the actual translation.xml file structure and content
 * Ensures all translations are complete and properly formatted
 */
public class TranslationXmlValidatorTest {

    private Document translationDoc;
    private Set<String> supportedLanguages;
    private Set<String> allKeys;

    @Before
    public void setUp() throws Exception {
        // Load the actual translation.xml file
        String translationPath = "src/main/resources/i18n/translation.xml";
        
        if (Files.exists(Paths.get(translationPath))) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            translationDoc = builder.parse(new File(translationPath));
            
            // Extract supported languages
            supportedLanguages = extractSupportedLanguages();
            
            // Extract all translation keys
            allKeys = extractAllKeys();
        }
    }

    // ========================================
    // XML Structure Validation Tests
    // ========================================

    @Test
    public void testTranslationXmlExists() {
        String translationPath = "src/main/resources/i18n/translation.xml";
        assertThat(Files.exists(Paths.get(translationPath)))
            .as("translation.xml should exist")
            .isTrue();
    }

    @Test
    public void testXmlStructureIsValid() throws Exception {
        assertThat(translationDoc).isNotNull();
        
        // Check root element
        Element root = translationDoc.getDocumentElement();
        assertThat(root.getNodeName()).isEqualTo("translation");
        
        // Check languageList exists
        NodeList languageList = root.getElementsByTagName("languageList");
        assertThat(languageList.getLength()).isEqualTo(1);
        
        // Check that we have key elements
        NodeList keys = root.getElementsByTagName("key");
        assertThat(keys.getLength()).isGreaterThan(0);
    }

    @Test
    public void testAllSupportedLanguagesAreDeclared() throws Exception {
        assertThat(supportedLanguages)
            .as("Should have all expected languages")
            .contains("en_US", "zh_CN", "zh_TW")
            .hasSize(3);
    }

    // ========================================
    // Translation Completeness Tests
    // ========================================

    @Test
    public void testAllKeysHaveAllLanguageTranslations() throws Exception {
        if (translationDoc == null) return; // Skip if file doesn't exist
        
        for (String key : allKeys) {
            Element keyElement = findKeyElement(key);
            assertThat(keyElement).as("Key element for '%s' should exist", key).isNotNull();
            
            Set<String> keyLanguages = extractLanguagesForKey(keyElement);
            
            assertThat(keyLanguages)
                .as("Key '%s' should have translations for all supported languages", key)
                .containsExactlyInAnyOrderElementsOf(supportedLanguages);
        }
    }

    @Test
    public void testNoEmptyTranslations() throws Exception {
        if (translationDoc == null) return;
        
        for (String key : allKeys) {
            Element keyElement = findKeyElement(key);
            NodeList valElements = keyElement.getElementsByTagName("val");
            
            for (int i = 0; i < valElements.getLength(); i++) {
                Element valElement = (Element) valElements.item(i);
                String lang = valElement.getAttribute("lang");
                String text = valElement.getTextContent();
                
                assertThat(text)
                    .as("Translation for key '%s' in language '%s' should not be empty", key, lang)
                    .isNotNull()
                    .isNotEmpty()
                    .isNotBlank();
            }
        }
    }

    @Test
    public void testCriticalKeysExist() throws Exception {
        if (translationDoc == null) return;
        
        String[] criticalKeys = {
            "menu.actions", "menuItem.ai.markup", "menuItem.tag.removal",
            "menuItem.preferences", "view.title", "initial.info",
            "button.replace", "menu.tools", "menuItem.utf8.check.convert"
        };
        
        for (String criticalKey : criticalKeys) {
            assertThat(allKeys)
                .as("Critical key '%s' should exist in translations", criticalKey)
                .contains(criticalKey);
        }
    }

    // ========================================
    // Text Quality Tests
    // ========================================

    @Test
    public void testChineseTranslationsContainChineseCharacters() throws Exception {
        if (translationDoc == null) return;
        
        for (String key : allKeys) {
            Element keyElement = findKeyElement(key);
            
            // Check Chinese translations contain Chinese characters
            String zhCN = getTranslationText(keyElement, "zh_CN");
            String zhTW = getTranslationText(keyElement, "zh_TW");
            
            if (zhCN != null && !zhCN.isEmpty()) {
                assertThat(zhCN)
                    .as("Simplified Chinese translation for '%s' should contain Chinese characters", key)
                    .matches(".*[\\u4e00-\\u9fff].*");
            }
            
            if (zhTW != null && !zhTW.isEmpty()) {
                assertThat(zhTW)
                    .as("Traditional Chinese translation for '%s' should contain Chinese characters", key)
                    .matches(".*[\\u4e00-\\u9fff].*");
            }
        }
    }

    @Test
    public void testEnglishTranslationsProperCase() throws Exception {
        if (translationDoc == null) return;
        
        String[] menuKeys = {"menu.actions", "menu.tools", "menuItem.ai.markup", "menuItem.tag.removal"};
        
        for (String key : menuKeys) {
            if (allKeys.contains(key)) {
                Element keyElement = findKeyElement(key);
                String english = getTranslationText(keyElement, "en_US");
                
                if (english != null && !english.isEmpty()) {
                    assertThat(english)
                        .as("English menu text for '%s' should start with capital letter", key)
                        .matches("^[A-Z].*");
                }
            }
        }
    }

    @Test
    public void testParameterPlaceholdersConsistency() throws Exception {
        if (translationDoc == null) return;
        
        // Find keys that might have parameters
        for (String key : allKeys) {
            Element keyElement = findKeyElement(key);
            Map<String, String> translations = getAllTranslationsForKey(keyElement);
            
            // Check if any translation has parameters
            boolean hasParameters = false;
            Set<String> parameterPatterns = new HashSet<>();
            
            for (String translation : translations.values()) {
                if (translation.contains("{") && translation.contains("}")) {
                    hasParameters = true;
                    // Extract parameter patterns like {0}, {1}, etc.
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\d+\\}");
                    java.util.regex.Matcher matcher = pattern.matcher(translation);
                    while (matcher.find()) {
                        parameterPatterns.add(matcher.group());
                    }
                }
            }
            
            // If one translation has parameters, all should have the same parameters
            if (hasParameters) {
                for (Map.Entry<String, String> entry : translations.entrySet()) {
                    String lang = entry.getKey();
                    String translation = entry.getValue();
                    
                    for (String param : parameterPatterns) {
                        assertThat(translation)
                            .as("Translation for key '%s' in language '%s' should contain parameter '%s'", 
                                key, lang, param)
                            .contains(param);
                    }
                }
            }
        }
    }

    // ========================================
    // Performance Tests
    // ========================================

    @Test(timeout = 5000)
    public void testTranslationXmlParsingPerformance() throws Exception {
        // Test that parsing is reasonably fast
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            String translationPath = "src/main/resources/i18n/translation.xml";
            if (Files.exists(Paths.get(translationPath))) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.parse(new File(translationPath));
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertThat(duration)
            .as("100 XML parsing operations should complete within 5 seconds")
            .isLessThan(5000);
    }

    // ========================================
    // Helper Methods
    // ========================================

    private Set<String> extractSupportedLanguages() throws Exception {
        Set<String> languages = new HashSet<>();
        
        NodeList languageList = translationDoc.getElementsByTagName("language");
        for (int i = 0; i < languageList.getLength(); i++) {
            Element langElement = (Element) languageList.item(i);
            String lang = langElement.getAttribute("lang");
            if (lang != null && !lang.isEmpty()) {
                languages.add(lang);
            }
        }
        
        return languages;
    }

    private Set<String> extractAllKeys() throws Exception {
        Set<String> keys = new HashSet<>();
        
        NodeList keyList = translationDoc.getElementsByTagName("key");
        for (int i = 0; i < keyList.getLength(); i++) {
            Element keyElement = (Element) keyList.item(i);
            String keyValue = keyElement.getAttribute("value");
            if (keyValue != null && !keyValue.isEmpty()) {
                keys.add(keyValue);
            }
        }
        
        return keys;
    }

    private Element findKeyElement(String keyValue) throws Exception {
        NodeList keyList = translationDoc.getElementsByTagName("key");
        for (int i = 0; i < keyList.getLength(); i++) {
            Element keyElement = (Element) keyList.item(i);
            if (keyValue.equals(keyElement.getAttribute("value"))) {
                return keyElement;
            }
        }
        return null;
    }

    private Set<String> extractLanguagesForKey(Element keyElement) {
        Set<String> languages = new HashSet<>();
        
        NodeList valElements = keyElement.getElementsByTagName("val");
        for (int i = 0; i < valElements.getLength(); i++) {
            Element valElement = (Element) valElements.item(i);
            String lang = valElement.getAttribute("lang");
            if (lang != null && !lang.isEmpty()) {
                languages.add(lang);
            }
        }
        
        return languages;
    }

    private String getTranslationText(Element keyElement, String language) {
        NodeList valElements = keyElement.getElementsByTagName("val");
        for (int i = 0; i < valElements.getLength(); i++) {
            Element valElement = (Element) valElements.item(i);
            if (language.equals(valElement.getAttribute("lang"))) {
                return valElement.getTextContent();
            }
        }
        return null;
    }

    private Map<String, String> getAllTranslationsForKey(Element keyElement) {
        Map<String, String> translations = new HashMap<>();
        
        NodeList valElements = keyElement.getElementsByTagName("val");
        for (int i = 0; i < valElements.getLength(); i++) {
            Element valElement = (Element) valElements.item(i);
            String lang = valElement.getAttribute("lang");
            String text = valElement.getTextContent();
            translations.put(lang, text);
        }
        
        return translations;
    }
}