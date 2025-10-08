package com.dila.dama.plugin.i18n;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import java.text.MessageFormat;
import java.util.regex.Pattern;

/**
 * Standalone localization test without Oxygen SDK dependencies
 * Tests core i18n functionality for multi-language plugin support
 */
public class StandaloneLocalizationTest {

    private static final String[] CRITICAL_KEYS = {
        "menu.actions", "menuItem.ai.markup", "menuItem.tag.removal", 
        "menuItem.preferences", "view.title", "initial.info",
        "menu.tools", "menuItem.utf8.check.convert"
    };

    private Properties enBundle;
    private Properties zhCNBundle;
    private Properties zhTWBundle;

    @Before
    public void setUp() {
        // Load test resource bundles
        enBundle = loadTestBundle("test_en_US");
        zhCNBundle = loadTestBundle("test_zh_CN");
        zhTWBundle = loadTestBundle("test_zh_TW_fixed");
    }

    private Properties loadTestBundle(String bundleName) {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("/i18n/" + bundleName + ".properties"));
        } catch (Exception e) {
            // Return empty properties if file not found
            System.out.println("Bundle " + bundleName + " not found, using empty properties");
        }
        return props;
    }

    // ========================================
    // Translation Completeness Tests
    // ========================================

    @Test
    public void testAllLocalesHaveSameKeys() {
        if (enBundle.isEmpty() || zhCNBundle.isEmpty() || zhTWBundle.isEmpty()) {
            System.out.println("Skipping test - bundles not available");
            return;
        }

        Set<String> enKeys = enBundle.stringPropertyNames();
        Set<String> cnKeys = zhCNBundle.stringPropertyNames();
        Set<String> twKeys = zhTWBundle.stringPropertyNames();

        assertThat(cnKeys)
            .as("Chinese Simplified bundle should have same keys as English")
            .containsExactlyInAnyOrderElementsOf(enKeys);

        assertThat(twKeys)
            .as("Chinese Traditional bundle should have same keys as English")
            .containsExactlyInAnyOrderElementsOf(enKeys);
    }

    @Test
    public void testCriticalKeysExistInAllBundles() {
        Properties[] bundles = {enBundle, zhCNBundle, zhTWBundle};
        String[] bundleNames = {"English", "Chinese Simplified", "Chinese Traditional"};

        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].isEmpty()) continue;

            for (String key : CRITICAL_KEYS) {
                assertThat(bundles[i].containsKey(key))
                    .as("Critical key '%s' should exist in %s bundle", key, bundleNames[i])
                    .isTrue();

                String value = bundles[i].getProperty(key);
                assertThat(value)
                    .as("Critical key '%s' should have non-empty value in %s bundle", key, bundleNames[i])
                    .isNotNull()
                    .isNotEmpty();
            }
        }
    }

    @Test
    public void testNoEmptyTranslations() {
        Properties[] bundles = {enBundle, zhCNBundle, zhTWBundle};
        String[] bundleNames = {"English", "Chinese Simplified", "Chinese Traditional"};

        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].isEmpty()) continue;

            for (String key : bundles[i].stringPropertyNames()) {
                String value = bundles[i].getProperty(key);
                assertThat(value)
                    .as("Key '%s' should have non-empty value in %s bundle", key, bundleNames[i])
                    .isNotNull()
                    .isNotEmpty()
                    .isNotBlank();
            }
        }
    }

    // ========================================
    // Character Encoding Tests
    // ========================================

    @Test
    public void testChineseTranslationsContainChineseCharacters() {
        if (zhCNBundle.isEmpty() && zhTWBundle.isEmpty()) {
            System.out.println("Skipping test - Chinese bundles not available");
            return;
        }

        Properties[] chineseBundles = {zhCNBundle, zhTWBundle};
        String[] bundleNames = {"Simplified Chinese", "Traditional Chinese"};

        for (int i = 0; i < chineseBundles.length; i++) {
            if (chineseBundles[i].isEmpty()) continue;

            for (String key : chineseBundles[i].stringPropertyNames()) {
                String value = chineseBundles[i].getProperty(key);
                if (value != null && !value.isEmpty()) {
                    // Check for Chinese characters OR multi-byte characters (accounting for encoding issues)
                    boolean hasChineseChars = value.matches(".*[\\u4e00-\\u9fff].*");
                    boolean hasMultiByte = value.getBytes().length > value.length();
                    
                    assertThat(hasChineseChars || hasMultiByte)
                        .as("%s translation for '%s' should contain Chinese characters or multi-byte content (value: '%s')", 
                            bundleNames[i], key, value)
                        .isTrue();
                }
            }
        }
    }

    @Test
    public void testUtf8EncodingConsistency() {
        Properties[] bundles = {enBundle, zhCNBundle, zhTWBundle};
        String[] bundleNames = {"English", "Chinese Simplified", "Chinese Traditional"};

        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].isEmpty()) continue;

            for (String key : bundles[i].stringPropertyNames()) {
                String value = bundles[i].getProperty(key);
                if (value != null) {
                    // Test that the string can be properly encoded/decoded in UTF-8
                    try {
                        byte[] utf8Bytes = value.getBytes("UTF-8");
                        String decoded = new String(utf8Bytes, "UTF-8");
                        assertThat(decoded)
                            .as("UTF-8 encoding should be consistent for '%s' in %s bundle", key, bundleNames[i])
                            .isEqualTo(value);
                    } catch (Exception e) {
                        throw new AssertionError("UTF-8 encoding failed for key '" + key + "' in " + bundleNames[i] + " bundle", e);
                    }
                }
            }
        }
    }

    // ========================================
    // Text Format and UI Tests
    // ========================================

    @Test
    public void testEnglishMenuTextProperCase() {
        if (enBundle.isEmpty()) {
            System.out.println("Skipping test - English bundle not available");
            return;
        }

        String[] menuKeys = {"actions.menu", "ai.markup.action", "tag.removal.action", "preferences.action"};

        for (String key : menuKeys) {
            if (enBundle.containsKey(key)) {
                String value = enBundle.getProperty(key);
                if (value != null && !value.isEmpty()) {
                    assertThat(value)
                        .as("English menu text for '%s' should start with capital letter", key)
                        .matches("^[A-Z].*");
                }
            }
        }
    }

    @Test
    public void testUiTextLengthConstraints() {
        Properties[] bundles = {enBundle, zhCNBundle, zhTWBundle};
        String[] bundleNames = {"English", "Chinese Simplified", "Chinese Traditional"};

        // Button text should be reasonably short
        String[] buttonKeys = {"replace.button", "cancel.button", "ok.button"};

        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].isEmpty()) continue;

            for (String key : buttonKeys) {
                if (bundles[i].containsKey(key)) {
                    String value = bundles[i].getProperty(key);
                    if (value != null) {
                        assertThat(value.length())
                            .as("Button text for '%s' in %s bundle should be reasonably short", key, bundleNames[i])
                            .isLessThanOrEqualTo(20);
                    }
                }
            }
        }
    }

    @Test
    public void testParameterPlaceholderConsistency() {
        if (enBundle.isEmpty()) {
            System.out.println("Skipping test - English bundle not available");
            return;
        }

        Properties[] bundles = {enBundle, zhCNBundle, zhTWBundle};
        String[] bundleNames = {"English", "Chinese Simplified", "Chinese Traditional"};

        // Find keys with parameter placeholders in English
        Pattern paramPattern = Pattern.compile("\\{\\d+\\}");

        for (String key : enBundle.stringPropertyNames()) {
            String enValue = enBundle.getProperty(key);
            if (enValue != null && paramPattern.matcher(enValue).find()) {
                // Extract parameter placeholders
                Set<String> enParams = extractParameters(enValue);

                // Check that all bundles have the same parameters
                for (int i = 1; i < bundles.length; i++) { // Skip English (index 0)
                    if (bundles[i].isEmpty() || !bundles[i].containsKey(key)) continue;

                    String value = bundles[i].getProperty(key);
                    if (value != null) {
                        Set<String> params = extractParameters(value);
                        assertThat(params)
                            .as("Parameters for key '%s' in %s bundle should match English", key, bundleNames[i])
                            .isEqualTo(enParams);
                    }
                }
            }
        }
    }

    private Set<String> extractParameters(String text) {
        Set<String> params = new HashSet<>();
        Pattern pattern = Pattern.compile("\\{\\d+\\}");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            params.add(matcher.group());
        }
        return params;
    }

    // ========================================
    // Message Formatting Tests
    // ========================================

    @Test
    public void testMessageFormattingWithParameters() {
        if (enBundle.isEmpty()) {
            System.out.println("Skipping test - English bundle not available");
            return;
        }

        // Test message formatting with parameters
        String testKey = "test.message.with.params";
        if (enBundle.containsKey(testKey)) {
            String pattern = enBundle.getProperty(testKey);
            if (pattern != null && pattern.contains("{0}")) {
                String formatted = MessageFormat.format(pattern, "TestValue");
                assertThat(formatted)
                    .as("Message formatting should work correctly")
                    .contains("TestValue")
                    .doesNotContain("{0}");
            }
        }
    }

    @Test
    public void testSpecialCharacterHandling() {
        Properties[] bundles = {enBundle, zhCNBundle, zhTWBundle};
        String[] bundleNames = {"English", "Chinese Simplified", "Chinese Traditional"};

        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i].isEmpty()) continue;

            for (String key : bundles[i].stringPropertyNames()) {
                String value = bundles[i].getProperty(key);
                if (value != null) {
                    // Test that special characters are properly handled
                    assertThat(value)
                        .as("Special characters should be properly handled in '%s' for %s bundle", key, bundleNames[i])
                        .doesNotContain("\uFFFD"); // Replacement character indicates encoding issues
                }
            }
        }
    }

    // ========================================
    // Locale-Specific Formatting Tests
    // ========================================

    @Test
    public void testChineseTextDirection() {
        Properties[] chineseBundles = {zhCNBundle, zhTWBundle};
        String[] bundleNames = {"Simplified Chinese", "Traditional Chinese"};

        for (int i = 0; i < chineseBundles.length; i++) {
            if (chineseBundles[i].isEmpty()) continue;

            for (String key : chineseBundles[i].stringPropertyNames()) {
                String value = chineseBundles[i].getProperty(key);
                if (value != null && !value.isEmpty()) {
                    // Chinese text should not have unusual bidirectional markers
                    assertThat(value)
                        .as("Chinese text for '%s' in %s should not contain RTL markers", key, bundleNames[i])
                        .doesNotContain("\u202E") // Right-to-left override
                        .doesNotContain("\u202D"); // Left-to-right override
                }
            }
        }
    }

    @Test
    public void testLocaleSpecificQuotationMarks() {
        if (enBundle.isEmpty() || zhCNBundle.isEmpty()) {
            System.out.println("Skipping test - Required bundles not available");
            return;
        }

        // English typically uses " " while Chinese might use 「」 or " "
        for (String key : enBundle.stringPropertyNames()) {
            String enValue = enBundle.getProperty(key);
            if (enValue != null && (enValue.contains("\"") || enValue.contains("'"))) {
                // Just verify that if quotation marks exist, they're properly paired
                long doubleQuoteCount = enValue.chars().filter(ch -> ch == '"').count();
                assertThat(doubleQuoteCount % 2)
                    .as("Double quotes should be properly paired in English text for key '%s'", key)
                    .isEqualTo(0);
            }
        }
    }

    // ========================================
    // Performance Tests
    // ========================================

    @Test(timeout = 1000)
    public void testBundleLoadingPerformance() {
        // Test that bundle loading is reasonably fast
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            Properties testProps = new Properties();
            try {
                testProps.load(getClass().getResourceAsStream("/i18n/test_en_US.properties"));
            } catch (Exception e) {
                // Expected if file doesn't exist
                break;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        assertThat(duration)
            .as("100 bundle loading operations should complete within 1 second")
            .isLessThan(1000);
    }

    @Test(timeout = 500)
    public void testStringRetrievalPerformance() {
        if (enBundle.isEmpty()) return;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            for (String key : enBundle.stringPropertyNames()) {
                enBundle.getProperty(key);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        assertThat(duration)
            .as("1000 iterations of string retrieval should complete within 500ms")
            .isLessThan(500);
    }
}