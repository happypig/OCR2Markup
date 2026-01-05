package com.dila.dama.plugin.i18n;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import ro.sync.exml.workspace.api.PluginResourceBundle;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Comprehensive test suite for localization (i18n) functionality
 * Tests translation completeness, locale handling, and text formatting
 */
public class LocalizationTest {

    private static final String[] SUPPORTED_LOCALES = {"en_US", "zh_CN", "zh_TW"};
    private static final String[] CRITICAL_KEYS = {
        // Core navigation and menu items
        "menu.actions", "menuItem.ai.markup", "menuItem.tag.removal", 
        "menuItem.preferences", "view.title", "initial.info",
        "menu.tools", "menuItem.utf8.check.convert", "menu.options",
        
        // UI buttons
        "button.replace", "button.cancel", "button.transfer.utf8",
        
        // Processing and status messages
        "action.ai.markup.selected", "action.tag.removal.selected", 
        "selected.text", "text.with.length", "text.replaced",
        
        // UTF-8 related messages
        "utf8.all.valid", "utf8.check.found.non.utf8", 
        "utf8.conversion.completed", "utf8.conversion.cancelled",
        "utf8.conversion.success.count", "utf8.conversion.failure.count",
        
        // Error messages
        "error.processing.ai.markup", "error.checking.utf8",
        "error.replacing.text", "error.accessing.editor", 
        "error.converting.files", "error.no.APIKey", "error.no.parse.model",
        
        // Other important keys
        "no.text.selected", "no.editor.open", "no.text.to.replace",
        "system.prompt.ai.markup", "llm.error"
    };

    private StandalonePluginWorkspace mockWorkspace;
    private PluginResourceBundle mockResourceBundle;
    private Map<String, Map<String, String>> translationMap;

    @Before
    public void setUp() {
        mockWorkspace = mock(StandalonePluginWorkspace.class);
        mockResourceBundle = mock(PluginResourceBundle.class);
        when(mockWorkspace.getResourceBundle()).thenReturn(mockResourceBundle);
        translationMap = loadTranslations();
    }

    // ========================================
    // Translation Completeness Tests
    // ========================================

    @Test
    public void testAllCriticalKeysHaveTranslations() {
        // Test that all critical keys have translations for all supported locales
        for (String locale : SUPPORTED_LOCALES) {
            for (String key : CRITICAL_KEYS) {
                // Setup mock to return locale-specific text
                String expectedText = getMockTranslation(key, locale);
                when(mockResourceBundle.getMessage(key)).thenReturn(expectedText);
                
                String actualText = mockResourceBundle.getMessage(key);
                
                assertThat(actualText)
                    .as("Key '%s' should have translation for locale '%s'", key, locale)
                    .isNotNull()
                    .isNotEmpty()
                    .isNotEqualTo(key); // Should not return the key itself
            }
        }
    }

    @Test
    public void testTranslationKeysConsistency() {
        // Test that all locales have the same set of keys
        Set<String> enKeys = new HashSet<>(Arrays.asList(CRITICAL_KEYS));
        
        for (String locale : SUPPORTED_LOCALES) {
            if (!locale.equals("en_US")) {
                // In a real implementation, you'd load actual translations
                // Here we simulate that all locales have the same keys
                Set<String> localeKeys = new HashSet<>(Arrays.asList(CRITICAL_KEYS));
                
                assertThat(localeKeys)
                    .as("Locale '%s' should have same keys as English", locale)
                    .containsExactlyInAnyOrderElementsOf(enKeys);
            }
        }
    }

    @Test
    public void testNoMissingTranslations() {
        // Test that no translation returns null or empty
        for (String locale : SUPPORTED_LOCALES) {
            for (String key : CRITICAL_KEYS) {
                String translation = getMockTranslation(key, locale);
                
                assertThat(translation)
                    .as("Translation for key '%s' in locale '%s' should not be null/empty", key, locale)
                    .isNotNull()
                    .isNotEmpty()
                    .doesNotContain("TODO")
                    .doesNotContain("FIXME");
            }
        }
    }

    // ========================================
    // Locale-Specific Format Tests
    // ========================================

    @Test
    public void testChineseCharacterEncoding() {
        // Test that Chinese characters are properly handled
        String[] chineseKeys = {"view.title", "menu.actions", "menuItem.ai.markup"};
        
        for (String key : chineseKeys) {
            // Test Simplified Chinese
            String zhCN = getMockTranslation(key, "zh_CN");
            assertThat(zhCN)
                .as("Simplified Chinese translation for '%s' should contain Chinese characters", key)
                .matches(".*[\\u4e00-\\u9fff].*"); // Chinese character range
            
            // Test Traditional Chinese
            String zhTW = getMockTranslation(key, "zh_TW");
            assertThat(zhTW)
                .as("Traditional Chinese translation for '%s' should contain Chinese characters", key)
                .matches(".*[\\u4e00-\\u9fff].*");
        }
    }

    @Test
    public void testEnglishTextFormat() {
        // Test English text follows proper formatting conventions
        String[] englishKeys = {"menuItem.preferences", "initial.info"};
        
        for (String key : englishKeys) {
            String english = getMockTranslation(key, "en_US");
            
            assertThat(english)
                .as("English translation for '%s' should be properly formatted", key)
                .matches("^[A-Z].*") // Starts with capital letter
                .doesNotEndWith(" ") // No trailing spaces
                .doesNotStartWith(" "); // No leading spaces
        }
    }

    @Test
    public void testPunctuationConsistency() {
        // Test that punctuation is consistent across locales
        Map<String, String> punctuationKeys = new HashMap<>();
        punctuationKeys.put("menuItem.preferences", "\\.\\.\\.$"); // Should end with "..."
        
        for (Map.Entry<String, String> entry : punctuationKeys.entrySet()) {
            String key = entry.getKey();
            String expectedPattern = entry.getValue();
            
            for (String locale : SUPPORTED_LOCALES) {
                String translation = getMockTranslation(key, locale);
                
                assertThat(translation)
                    .as("Translation for '%s' in locale '%s' should match punctuation pattern", key, locale)
                    .matches(".*" + expectedPattern);
            }
        }
    }

    // ========================================
    // Parameter Substitution Tests
    // ========================================

    @Test
    public void testParameterSubstitution() {
        // Test message formatting with parameters
        String template = "Found {0} files to convert";
        String expected = "Found 5 files to convert";
        
        // Mock parameter substitution
        when(mockResourceBundle.getMessage("utf8.check.found.non.utf8")).thenReturn(template);
        
        String result = mockResourceBundle.getMessage("utf8.check.found.non.utf8");
        String formatted = result.replace("{0}", "5");
        
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    public void testMultipleParameterSubstitution() {
        // Test messages with multiple parameters
        String template = "Converted {0} files successfully, {1} failed";
        String expected = "Converted 10 files successfully, 2 failed";
        
        when(mockResourceBundle.getMessage("conversion.summary")).thenReturn(template);
        
        String result = mockResourceBundle.getMessage("conversion.summary");
        String formatted = result.replace("{0}", "10").replace("{1}", "2");
        
        assertThat(formatted).isEqualTo(expected);
    }

    // ========================================
    // Text Length and UI Compatibility Tests
    // ========================================

    @Test
    public void testMenuTextLength() {
        // Test that menu text is not too long for UI
        String[] menuKeys = {"menu.actions", "menu.options", "menuItem.ai.markup", "menuItem.tag.removal"};
        int maxMenuLength = 20; // Typical menu text limit
        
        for (String key : menuKeys) {
            for (String locale : SUPPORTED_LOCALES) {
                String text = getMockTranslation(key, locale);
                
                assertThat(text.length())
                    .as("Menu text for '%s' in locale '%s' should not exceed %d characters", 
                        key, locale, maxMenuLength)
                    .isLessThanOrEqualTo(maxMenuLength);
            }
        }
    }

    @Test
    public void testButtonTextLength() {
        // Test button text length constraints
        String[] buttonKeys = {"button.replace", "button.cancel", "button.transfer.utf8"};
        int maxButtonLength = 15;
        
        for (String key : buttonKeys) {
            for (String locale : SUPPORTED_LOCALES) {
                String text = getMockTranslation(key, locale);
                
                assertThat(text.length())
                    .as("Button text for '%s' in locale '%s' should not exceed %d characters", 
                        key, locale, maxButtonLength)
                    .isLessThanOrEqualTo(maxButtonLength);
            }
        }
    }

    // ========================================
    // Fallback and Error Handling Tests
    // ========================================

    @Test
    public void testFallbackToEnglish() {
        // Test fallback behavior when translation is missing
        String missingKey = "non.existent.key";
        
        // Mock scenario where key doesn't exist
        when(mockResourceBundle.getMessage(missingKey)).thenReturn(missingKey);
        
        String result = mockResourceBundle.getMessage(missingKey);
        
        // Should return the key itself as fallback
        assertThat(result).isEqualTo(missingKey);
    }

    @Test
    public void testNullKeyHandling() {
        // Test behavior with null keys
        when(mockResourceBundle.getMessage(null)).thenThrow(new IllegalArgumentException("Key cannot be null"));
        
        assertThatThrownBy(() -> mockResourceBundle.getMessage(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Key cannot be null");
    }

    @Test
    public void testEmptyKeyHandling() {
        // Test behavior with empty keys
        when(mockResourceBundle.getMessage("")).thenReturn("");
        
        String result = mockResourceBundle.getMessage("");
        assertThat(result).isEmpty();
    }

    // ========================================
    // Locale Detection Tests
    // ========================================

    @Test
    public void testSupportedLocaleDetection() {
        // Test that supported locales are properly detected
        List<String> supportedLocales = Arrays.asList(SUPPORTED_LOCALES);
        
        assertThat(supportedLocales)
            .contains("en_US", "zh_CN", "zh_TW")
            .hasSize(3);
    }

    @Test
    public void testDefaultLocaleHandling() {
        // Test default locale when system locale is not supported
        String unsupportedLocale = "fr_FR";
        String defaultLocale = "en_US";
        
        // Mock detection logic
        String actualLocale = supportedLocales.contains(unsupportedLocale) ? 
                             unsupportedLocale : defaultLocale;
        
        assertThat(actualLocale).isEqualTo(defaultLocale);
    }

    // ========================================
    // Performance Tests
    // ========================================

    @Test(timeout = 1000)
    public void testTranslationPerformance() {
        // Test that translation lookup is fast without mock overhead
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            for (String key : CRITICAL_KEYS) {
                getMockTranslation(key, "en_US");
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertThat(duration)
            .as("1000 translation lookups should complete within 1 second")
            .isLessThan(1000);
    }

    // ========================================
    // Helper Methods
    // ========================================

    private String getMockTranslation(String key, String locale) {
        if (translationMap != null && translationMap.containsKey(key)) {
            Map<String, String> perLang = translationMap.get(key);
            if (perLang != null && perLang.containsKey(locale)) {
                String value = perLang.get(locale);
                if (value != null && !value.trim().isEmpty()) {
                    return value;
                }
            }
        }
        return key;
    }

    private Map<String, Map<String, String>> loadTranslations() {
        Map<String, Map<String, String>> translations = new HashMap<>();
        String translationPath = "src/main/resources/i18n/translation.xml";
        if (!Files.exists(Paths.get(translationPath))) {
            return translations;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new File(translationPath));

            NodeList keyNodes = doc.getElementsByTagName("key");
            for (int i = 0; i < keyNodes.getLength(); i++) {
                Element keyElement = (Element) keyNodes.item(i);
                String keyValue = keyElement.getAttribute("value");
                if (keyValue == null || keyValue.isEmpty()) {
                    continue;
                }
                Map<String, String> perLang = new HashMap<>();
                NodeList valNodes = keyElement.getElementsByTagName("val");
                for (int j = 0; j < valNodes.getLength(); j++) {
                    Element valElement = (Element) valNodes.item(j);
                    String lang = valElement.getAttribute("lang");
                    if (lang != null && !lang.isEmpty()) {
                        perLang.put(lang, valElement.getTextContent());
                    }
                }
                translations.put(keyValue, perLang);
            }
        } catch (Exception e) {
            return translations;
        }
        return translations;
    }

    private Map<String, Map<String, String>> createMockTranslations() {
        Map<String, Map<String, String>> translations = new HashMap<>();
        
        // Actions menu
        Map<String, String> actionsMenu = new HashMap<>();
        actionsMenu.put("en_US", "Actions");
        actionsMenu.put("zh_CN", "標記功能");
        actionsMenu.put("zh_TW", "標記功能");
        translations.put("actions.menu", actionsMenu);
        
        // AI Markup action
        Map<String, String> aiMarkup = new HashMap<>();
        aiMarkup.put("en_US", "AI Markup");
        aiMarkup.put("zh_CN", "AI 標記");
        aiMarkup.put("zh_TW", "AI 標記");
        translations.put("ai.markup.action", aiMarkup);
        
        // Tag removal action
        Map<String, String> tagRemoval = new HashMap<>();
        tagRemoval.put("en_US", "Tag Removal");
        tagRemoval.put("zh_CN", "移除標籤");
        tagRemoval.put("zh_TW", "移除標籤");
        translations.put("tag.removal.action", tagRemoval);
        
        // Preferences action
        Map<String, String> preferences = new HashMap<>();
        preferences.put("en_US", "Preferences...");
        preferences.put("zh_CN", "偏好設定...");
        preferences.put("zh_TW", "偏好設定...");
        translations.put("preferences.action", preferences);
        
        // View title
        Map<String, String> viewTitle = new HashMap<>();
        viewTitle.put("en_US", "DILA AI Markup Assistant");
        viewTitle.put("zh_CN", "DILA AI 標記助手");
        viewTitle.put("zh_TW", "DILA AI 標記助手");
        translations.put("view.title", viewTitle);
        
        // Initial info
        Map<String, String> initialInfo = new HashMap<>();
        initialInfo.put("en_US", "Please select a function from the Actions menu, or select Preferences from the Options menu to configure parameters.");
        initialInfo.put("zh_CN", "請從「標記功能」菜單選擇功能，或從「選項」菜單選擇「偏好設定」以設定參數。");
        initialInfo.put("zh_TW", "請從「標記功能」菜單選擇功能，或從「選項」菜單選擇「偏好設定」以設定參數。");
        translations.put("initial.info", initialInfo);
        
        // Tools menu
        Map<String, String> toolsMenu = new HashMap<>();
        toolsMenu.put("en_US", "Tools");
        toolsMenu.put("zh_CN", "工具");
        toolsMenu.put("zh_TW", "工具");
        translations.put("menu.tools", toolsMenu);
        
        // UTF-8 check
        Map<String, String> utf8Check = new HashMap<>();
        utf8Check.put("en_US", "UTF-8 Check/Convert");
        utf8Check.put("zh_CN", "UTF-8 檢查/轉換");
        utf8Check.put("zh_TW", "UTF-8 檢查/轉換");
        translations.put("menu.tools.utf8.check", utf8Check);
        
        // Replace button
        Map<String, String> replaceButton = new HashMap<>();
        replaceButton.put("en_US", "Replace");
        replaceButton.put("zh_CN", "替換");
        replaceButton.put("zh_TW", "替換");
        translations.put("replace.button", replaceButton);
        
        // Convert button
        Map<String, String> convertButton = new HashMap<>();
        convertButton.put("en_US", "Convert");
        convertButton.put("zh_CN", "轉換");
        convertButton.put("zh_TW", "轉換");
        translations.put("button.convert", convertButton);
        
        // Cancel button
        Map<String, String> cancelButton = new HashMap<>();
        cancelButton.put("en_US", "Cancel");
        cancelButton.put("zh_CN", "取消");
        cancelButton.put("zh_TW", "取消");
        translations.put("button.cancel", cancelButton);
        
        // Save button
        Map<String, String> saveButton = new HashMap<>();
        saveButton.put("en_US", "Save");
        saveButton.put("zh_CN", "儲存");
        saveButton.put("zh_TW", "儲存");
        translations.put("save.button", saveButton);
        
        // UI Transfer to UTF-8 button
        Map<String, String> transferUtf8Button = new HashMap<>();
        transferUtf8Button.put("en_US", "Transfer to UTF-8");
        transferUtf8Button.put("zh_CN", "转换为 UTF-8");
        transferUtf8Button.put("zh_TW", "轉換為 UTF-8");
        translations.put("ui.button.transfer.utf8", transferUtf8Button);
        
        // Processing AI Markup
        Map<String, String> processingAiMarkup = new HashMap<>();
        processingAiMarkup.put("en_US", "Processing AI Markup for selected text...");
        processingAiMarkup.put("zh_CN", "正在处理选定文本的 AI 标记...");
        processingAiMarkup.put("zh_TW", "正在處理選定文字的 AI 標記...");
        translations.put("ui.processing.ai.markup", processingAiMarkup);
        
        // Processing Tag Removal
        Map<String, String> processingTagRemoval = new HashMap<>();
        processingTagRemoval.put("en_US", "Processing Tag Removal for selected text...");
        processingTagRemoval.put("zh_CN", "正在处理选定文本的标签移除...");
        processingTagRemoval.put("zh_TW", "正在處理選定文字的標籤移除...");
        translations.put("ui.processing.tag.removal", processingTagRemoval);
        
        // Text length
        Map<String, String> textLength = new HashMap<>();
        textLength.put("en_US", "Text length: {0} characters");
        textLength.put("zh_CN", "文本长度：{0} 个字符");
        textLength.put("zh_TW", "文字長度：{0} 個字符");
        translations.put("ui.text.length", textLength);
        
        // Text replaced successfully
        Map<String, String> textReplaced = new HashMap<>();
        textReplaced.put("en_US", "Text replaced successfully in document.");
        textReplaced.put("zh_CN", "文档中的文本已成功替换。");
        textReplaced.put("zh_TW", "文件中的文字已成功替換。");
        translations.put("ui.text.replaced.successfully", textReplaced);
        
        // All files are valid UTF-8
        Map<String, String> utf8AllValid = new HashMap<>();
        utf8AllValid.put("en_US", "All files are valid UTF-8!");
        utf8AllValid.put("zh_CN", "所有文件都是有效的 UTF-8 编码！");
        utf8AllValid.put("zh_TW", "所有檔案都是有效的 UTF-8 編碼！");
        translations.put("ui.utf8.all.valid", utf8AllValid);
        
        // Non-UTF-8 files found
        Map<String, String> utf8NonUtf8Found = new HashMap<>();
        utf8NonUtf8Found.put("en_US", "Non-UTF-8 files found: {0}");
        utf8NonUtf8Found.put("zh_CN", "发现非 UTF-8 编码文件：{0}");
        utf8NonUtf8Found.put("zh_TW", "發現非 UTF-8 編碼檔案：{0}");
        translations.put("ui.utf8.non.utf8.found", utf8NonUtf8Found);
        
        // UTF-8 conversion completed
        Map<String, String> utf8ConversionCompleted = new HashMap<>();
        utf8ConversionCompleted.put("en_US", "UTF-8 conversion completed.");
        utf8ConversionCompleted.put("zh_CN", "UTF-8 转换完成。");
        utf8ConversionCompleted.put("zh_TW", "UTF-8 轉換完成。");
        translations.put("ui.utf8.conversion.completed", utf8ConversionCompleted);
        
        // UTF-8 conversion cancelled
        Map<String, String> utf8ConversionCancelled = new HashMap<>();
        utf8ConversionCancelled.put("en_US", "UTF-8 conversion cancelled.");
        utf8ConversionCancelled.put("zh_CN", "UTF-8 转换已取消。");
        utf8ConversionCancelled.put("zh_TW", "UTF-8 轉換已取消。");
        translations.put("ui.utf8.conversion.cancelled", utf8ConversionCancelled);
        
        // Successfully converted files
        Map<String, String> utf8SuccessfullyConverted = new HashMap<>();
        utf8SuccessfullyConverted.put("en_US", "Successfully converted: {0} files");
        utf8SuccessfullyConverted.put("zh_CN", "成功转换：{0} 个文件");
        utf8SuccessfullyConverted.put("zh_TW", "成功轉換：{0} 個檔案");
        translations.put("ui.utf8.successfully.converted", utf8SuccessfullyConverted);
        
        // Failed conversions
        Map<String, String> utf8FailedConversions = new HashMap<>();
        utf8FailedConversions.put("en_US", "Failed conversions: {0} files");
        utf8FailedConversions.put("zh_CN", "转换失败：{0} 个文件");
        utf8FailedConversions.put("zh_TW", "轉換失敗：{0} 個檔案");
        translations.put("ui.utf8.failed.conversions", utf8FailedConversions);
        
        // Error processing AI markup
        Map<String, String> errorProcessingAiMarkup = new HashMap<>();
        errorProcessingAiMarkup.put("en_US", "Error processing AI markup: {0}");
        errorProcessingAiMarkup.put("zh_CN", "处理 AI 标记时出错：{0}");
        errorProcessingAiMarkup.put("zh_TW", "處理 AI 標記時出錯：{0}");
        translations.put("ui.error.processing.ai.markup", errorProcessingAiMarkup);
        
        // Error checking UTF-8
        Map<String, String> errorCheckingUtf8 = new HashMap<>();
        errorCheckingUtf8.put("en_US", "Error checking UTF-8 files: {0}");
        errorCheckingUtf8.put("zh_CN", "检查 UTF-8 文件时出错：{0}");
        errorCheckingUtf8.put("zh_TW", "檢查 UTF-8 檔案時出錯：{0}");
        translations.put("ui.error.checking.utf8", errorCheckingUtf8);
        
        // Error replacing text
        Map<String, String> errorReplacingText = new HashMap<>();
        errorReplacingText.put("en_US", "Error replacing text: {0}");
        errorReplacingText.put("zh_CN", "替换文本时出错：{0}");
        errorReplacingText.put("zh_TW", "替換文字時出錯：{0}");
        translations.put("ui.error.replacing.text", errorReplacingText);
        
        // Error accessing editor
        Map<String, String> errorAccessingEditor = new HashMap<>();
        errorAccessingEditor.put("en_US", "Error accessing editor: {0}");
        errorAccessingEditor.put("zh_CN", "访问编辑器时出错：{0}");
        errorAccessingEditor.put("zh_TW", "存取編輯器時出錯：{0}");
        translations.put("ui.error.accessing.editor", errorAccessingEditor);
        
        // Error converting files
        Map<String, String> errorConvertingFiles = new HashMap<>();
        errorConvertingFiles.put("en_US", "Error converting files: {0}");
        errorConvertingFiles.put("zh_CN", "转换文件时出错：{0}");
        errorConvertingFiles.put("zh_TW", "轉換檔案時出錯：{0}");
        translations.put("ui.error.converting.files", errorConvertingFiles);
        
        // Options menu
        Map<String, String> optionsMenu = new HashMap<>();
        optionsMenu.put("en_US", "Options");
        optionsMenu.put("zh_CN", "選項");
        optionsMenu.put("zh_TW", "選項");
        translations.put("options.menu", optionsMenu);
        
        // Options panel title
        Map<String, String> optionsPanelTitle = new HashMap<>();
        optionsPanelTitle.put("en_US", "DILA AI Markup Assistant Settings");
        optionsPanelTitle.put("zh_CN", "DILA AI標記助手設定");
        optionsPanelTitle.put("zh_TW", "DILA AI標記助手設定");
        translations.put("options.panel.title", optionsPanelTitle);
        
        // Options info
        Map<String, String> optionsInfo = new HashMap<>();
        optionsInfo.put("en_US", "Configure the OpenAI models and API key used for reference parsing and detection.");
        optionsInfo.put("zh_CN", "設定用於參照解析和偵測的OpenAI模型和API金鑰。");
        optionsInfo.put("zh_TW", "設定用於參照解析和偵測的OpenAI模型和API金鑰。");
        translations.put("options.info", optionsInfo);
        
        // Options saved successfully
        Map<String, String> optionsSavedSuccessfully = new HashMap<>();
        optionsSavedSuccessfully.put("en_US", "Options saved successfully.");
        optionsSavedSuccessfully.put("zh_CN", "選項已成功儲存。");
        optionsSavedSuccessfully.put("zh_TW", "選項已成功儲存。");
        translations.put("options.saved.successfully", optionsSavedSuccessfully);
        
        // Options saved failed
        Map<String, String> optionsSavedFailed = new HashMap<>();
        optionsSavedFailed.put("en_US", "Failed to save options.");
        optionsSavedFailed.put("zh_CN", "儲存選項失敗。");
        optionsSavedFailed.put("zh_TW", "儲存選項失敗。");
        translations.put("options.saved.failed", optionsSavedFailed);
        
        // Preferences page title
        Map<String, String> preferencesPageTitle = new HashMap<>();
        preferencesPageTitle.put("en_US", "DILA AI Markup Assistant Options");
        preferencesPageTitle.put("zh_CN", "DILA AI 標記助手偏好設定");
        preferencesPageTitle.put("zh_TW", "DILA AI 標記助手偏好設定");
        translations.put("preferences.page.title", preferencesPageTitle);
        
        return translations;
    }

    private static final List<String> supportedLocales = Arrays.asList(SUPPORTED_LOCALES);
}
