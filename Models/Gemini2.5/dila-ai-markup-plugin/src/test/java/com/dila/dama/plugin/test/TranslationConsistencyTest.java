package com.dila.dama.plugin.test;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * Unit tests to validate translation key consistency
 * between Java code and translation.xml
 */
public class TranslationConsistencyTest {
    
    private static Set<String> xmlKeys;
    private static Set<String> javaKeys;
    private static final String TRANSLATION_FILE = "src/main/resources/i18n/translation.xml";
    private static final String JAVA_SRC_DIR = "src/main/java";
    
    @BeforeClass
    public static void setupTranslationData() throws Exception {
        xmlKeys = extractKeysFromXML();
        javaKeys = extractKeysFromJava();
    }
    
    @Test
    public void testNoMissingTranslationKeys() {
        Set<String> missingKeys = new HashSet<>(javaKeys);
        missingKeys.removeAll(xmlKeys);
        
        if (!missingKeys.isEmpty()) {
            StringBuilder message = new StringBuilder("Missing translation keys:\n");
            missingKeys.forEach(key -> message.append("  - ").append(key).append("\n"));
            fail(message.toString());
        }
    }
    
    @Test
    public void testReportUnusedTranslationKeys() {
        Set<String> unusedKeys = new HashSet<>(xmlKeys);
        unusedKeys.removeAll(javaKeys);
        
        if (!unusedKeys.isEmpty()) {
            System.out.println("Unused translation keys found:");
            unusedKeys.forEach(key -> System.out.println("  - " + key));
            
            // Don't fail the test, just report
            double usageRate = ((double) javaKeys.size() / xmlKeys.size()) * 100;
            System.out.printf("Translation usage rate: %.1f%% (%d/%d keys)%n", 
                            usageRate, javaKeys.size(), xmlKeys.size());
        }
    }
    
    @Test
    public void testTranslationFileExists() {
        assertTrue("Translation file should exist: " + TRANSLATION_FILE, 
                  Files.exists(Paths.get(TRANSLATION_FILE)));
    }
    
    @Test
    public void testMinimumTranslationUsageRate() {
        double usageRate = ((double) javaKeys.size() / xmlKeys.size()) * 100;
        assertTrue(String.format("Translation usage rate too low: %.1f%% (minimum 40%%)", usageRate),
                  usageRate >= 40.0);
    }
    
    private static Set<String> extractKeysFromXML() throws Exception {
        Set<String> keys = new HashSet<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(TRANSLATION_FILE));
        
        NodeList keyNodes = doc.getElementsByTagName("key");
        for (int i = 0; i < keyNodes.getLength(); i++) {
            Element keyElement = (Element) keyNodes.item(i);
            String keyValue = keyElement.getAttribute("value");
            if (keyValue != null && !keyValue.trim().isEmpty()) {
                keys.add(keyValue);
            }
        }
        
        return keys;
    }
    
    private static Set<String> extractKeysFromJava() throws Exception {
        Set<String> keys = new HashSet<>();
        Pattern pattern = Pattern.compile("i18n\\s*\\(\\s*[\"']([^\"']+)[\"']");
        
        Files.walk(Paths.get(JAVA_SRC_DIR))
             .filter(path -> path.toString().endsWith(".java"))
             .forEach(javaFile -> {
                 try {
                     byte[] bytes = Files.readAllBytes(javaFile);
                     String content = new String(bytes, StandardCharsets.UTF_8);
                     Matcher matcher = pattern.matcher(content);
                     while (matcher.find()) {
                         keys.add(matcher.group(1));
                     }
                 } catch (IOException e) {
                     System.err.println("Error reading " + javaFile + ": " + e.getMessage());
                 }
             });
        
        return keys;
    }
}
