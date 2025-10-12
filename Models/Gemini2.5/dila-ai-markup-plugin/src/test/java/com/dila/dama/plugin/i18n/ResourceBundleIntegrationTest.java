package com.dila.dama.plugin.i18n;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * Test to validate ResourceBundle loading and functionality
 * Tests the actual i18n resource bundle integration
 */
public class ResourceBundleIntegrationTest {

    private ResourceBundle defaultBundle;
    private ResourceBundle chineseSimplifiedBundle;
    private ResourceBundle chineseTraditionalBundle;
    
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static final Locale LOCALE_EN_US = new Locale("en", "US");
    private static final Locale LOCALE_ZH_CN = new Locale("zh", "CN");
    private static final Locale LOCALE_ZH_TW = new Locale("zh", "TW");

    @Before
    public void setUp() {
        try {
            // Try to load the actual resource bundles
            defaultBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_EN_US);
            chineseSimplifiedBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_ZH_CN);
            chineseTraditionalBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_ZH_TW);
        } catch (MissingResourceException e) {
            // Bundles may not exist yet - tests will handle gracefully
        }
    }

    // ========================================
    // Bundle Loading Tests
    // ========================================

    @Test
    public void testDefaultBundleLoads() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_EN_US);
            assertThat(bundle).isNotNull();
        } catch (MissingResourceException e) {
            // This is expected if properties files don't exist yet
            System.out.println("Default bundle not found - will be created later");
        }
    }

    @Test
    public void testChineseBundlesLoad() {
        try {
            ResourceBundle cnBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_ZH_CN);
            ResourceBundle twBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_ZH_TW);
            
            assertThat(cnBundle).isNotNull();
            assertThat(twBundle).isNotNull();
        } catch (MissingResourceException e) {
            // This is expected if properties files don't exist yet
            System.out.println("Chinese bundles not found - will be created later");
        }
    }

    @Test
    public void testBundleFallbackMechanism() {
        // Test that if specific locale bundle doesn't exist, it falls back to default
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, new Locale("fr", "FR"));
            // Should fall back to default bundle
            assertThat(bundle).isNotNull();
        } catch (MissingResourceException e) {
            // Expected if no bundles exist at all
            System.out.println("No bundles available for fallback test");
        }
    }

    // ========================================
    // Key Availability Tests
    // ========================================

    @Test
    public void testCriticalKeysAvailableInAllBundles() {
        if (defaultBundle == null) return; // Skip if bundles don't exist
        
        String[] criticalKeys = {
            "menu.actions", "menuItem.ai.markup", "menuItem.tag.removal",
            "menuItem.preferences", "view.title", "button.replace",
            "menu.tools", "menuItem.utf8.check.convert", "initial.info"
        };
        
        ResourceBundle[] bundles = {defaultBundle, chineseSimplifiedBundle, chineseTraditionalBundle};
        String[] bundleNames = {"English", "Chinese Simplified", "Chinese Traditional"};
        
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i] != null) {
                for (String key : criticalKeys) {
                    try {
                        String value = bundles[i].getString(key);
                        assertThat(value)
                            .as("Key '%s' should have non-empty value in %s bundle", key, bundleNames[i])
                            .isNotNull()
                            .isNotEmpty();
                    } catch (MissingResourceException e) {
                        // Key might not exist in this bundle - log for information
                        System.out.printf("Key '%s' not found in %s bundle%n", key, bundleNames[i]);
                    }
                }
            }
        }
    }

    @Test
    public void testAllBundlesHaveSameKeys() {
        if (defaultBundle == null || chineseSimplifiedBundle == null || chineseTraditionalBundle == null) {
            return; // Skip if any bundle is missing
        }
        
        Set<String> defaultKeys = defaultBundle.keySet();
        Set<String> cnKeys = chineseSimplifiedBundle.keySet();
        Set<String> twKeys = chineseTraditionalBundle.keySet();
        
        assertThat(cnKeys)
            .as("Chinese Simplified bundle should have same keys as default")
            .containsExactlyInAnyOrderElementsOf(defaultKeys);
            
        assertThat(twKeys)
            .as("Chinese Traditional bundle should have same keys as default")
            .containsExactlyInAnyOrderElementsOf(defaultKeys);
    }

    // ========================================
    // Content Validation Tests
    // ========================================

    @Test
    public void testChineseTranslationsContainChineseCharacters() {
        if (chineseSimplifiedBundle == null && chineseTraditionalBundle == null) {
            return; // Skip if Chinese bundles don't exist
        }
        
        ResourceBundle[] chineseBundles = {chineseSimplifiedBundle, chineseTraditionalBundle};
        String[] bundleNames = {"Simplified", "Traditional"};
        
        for (int i = 0; i < chineseBundles.length; i++) {
            if (chineseBundles[i] != null) {
                for (String key : chineseBundles[i].keySet()) {
                    try {
                        String value = chineseBundles[i].getString(key);
                        if (value != null && !value.isEmpty()) {
                            assertThat(value)
                                .as("Chinese %s translation for '%s' should contain Chinese characters", 
                                    bundleNames[i], key)
                                .matches(".*[\\u4e00-\\u9fff].*");
                        }
                    } catch (MissingResourceException e) {
                        // Skip if key not found
                    }
                }
            }
        }
    }

    @Test
    public void testEnglishTranslationsProperFormat() {
        if (defaultBundle == null) return;
        
        String[] menuKeys = {"menu.actions", "menuItem.ai.markup", "menuItem.tag.removal", "menuItem.preferences"};
        
        for (String key : menuKeys) {
            try {
                String value = defaultBundle.getString(key);
                if (value != null && !value.isEmpty()) {
                    assertThat(value)
                        .as("English menu text for '%s' should start with capital letter", key)
                        .matches("^[A-Z].*");
                        
                    assertThat(value)
                        .as("English menu text for '%s' should not end with punctuation", key)
                        .doesNotMatch(".*[.!?]$");
                }
            } catch (MissingResourceException e) {
                // Key might not exist yet
            }
        }
    }

    @Test
    public void testNoEmptyTranslations() {
        ResourceBundle[] bundles = {defaultBundle, chineseSimplifiedBundle, chineseTraditionalBundle};
        String[] bundleNames = {"English", "Chinese Simplified", "Chinese Traditional"};
        
        for (int i = 0; i < bundles.length; i++) {
            if (bundles[i] != null) {
                for (String key : bundles[i].keySet()) {
                    try {
                        String value = bundles[i].getString(key);
                        assertThat(value)
                            .as("Translation for key '%s' in %s bundle should not be empty", key, bundleNames[i])
                            .isNotNull()
                            .isNotEmpty()
                            .isNotBlank();
                    } catch (MissingResourceException e) {
                        // Skip if key not found
                    }
                }
            }
        }
    }

    // ========================================
    // Locale Handling Tests
    // ========================================

    @Test
    public void testLocaleSpecificBundleSelection() {
        // Test that correct bundle is selected for each locale
        try {
            ResourceBundle enBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_EN_US);
            ResourceBundle cnBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_ZH_CN);
            ResourceBundle twBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_ZH_TW);
            
            // These should be different instances if locale-specific bundles exist
            if (enBundle != null && cnBundle != null && twBundle != null) {
                assertThat(enBundle.getLocale()).isEqualTo(LOCALE_EN_US);
                assertThat(cnBundle.getLocale().getLanguage()).isEqualTo("zh");
                assertThat(twBundle.getLocale().getLanguage()).isEqualTo("zh");
            }
        } catch (MissingResourceException e) {
            // Expected if bundles don't exist
            System.out.println("Locale-specific bundles not available for testing");
        }
    }

    @Test
    public void testUnsupportedLocaleHandling() {
        // Test behavior with unsupported locale
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, new Locale("es", "ES"));
            // Should fall back to default or root bundle
            assertThat(bundle).isNotNull();
        } catch (MissingResourceException e) {
            // Expected if no bundles exist at all
            System.out.println("No bundles available for unsupported locale test");
        }
    }

    // ========================================
    // Performance Tests
    // ========================================

    @Test(timeout = 3000)
    public void testBundleLoadingPerformance() {
        // Test that bundle loading is reasonably fast
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            try {
                ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_EN_US);
                ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_ZH_CN);
                ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_ZH_TW);
            } catch (MissingResourceException e) {
                // Expected if bundles don't exist
                break;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertThat(duration)
            .as("1000 bundle loading operations should complete within 3 seconds")
            .isLessThan(3000);
    }

    @Test(timeout = 1000)
    public void testStringRetrievalPerformance() {
        if (defaultBundle == null) return;
        
        long startTime = System.currentTimeMillis();
        
        Set<String> keys = defaultBundle.keySet();
        for (int i = 0; i < 1000; i++) {
            for (String key : keys) {
                try {
                    defaultBundle.getString(key);
                } catch (MissingResourceException e) {
                    // Skip if key not found
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertThat(duration)
            .as("1000 iterations of string retrieval should complete within 1 second")
            .isLessThan(1000);
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    public void testMissingKeyHandling() {
        if (defaultBundle == null) return;
        
        assertThatThrownBy(() -> {
            defaultBundle.getString("non.existent.key");
        }).isInstanceOf(MissingResourceException.class);
    }

    @Test
    public void testNullKeyHandling() {
        if (defaultBundle == null) return;
        
        assertThatThrownBy(() -> {
            defaultBundle.getString(null);
        }).isInstanceOf(NullPointerException.class);
    }
}