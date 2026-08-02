package com.dila.dama.plugin.infrastructure.i18n;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TranslationBundleCompletenessTest {

    @Test
    public void verifiesAllTranslationKeysHaveAllSupportedLanguages() throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new File("src/main/resources/i18n/translation.xml"));

        Set<String> languages = new HashSet<String>();
        NodeList languageNodes = document.getElementsByTagName("language");
        for (int i = 0; i < languageNodes.getLength(); i++) {
            languages.add(((Element) languageNodes.item(i)).getAttribute("lang"));
        }

        NodeList keyNodes = document.getElementsByTagName("key");
        for (int i = 0; i < keyNodes.getLength(); i++) {
            Element key = (Element) keyNodes.item(i);
            Set<String> keyLanguages = new HashSet<String>();
            NodeList values = key.getElementsByTagName("val");
            for (int j = 0; j < values.getLength(); j++) {
                keyLanguages.add(((Element) values.item(j)).getAttribute("lang"));
            }
            assertThat(keyLanguages)
                .as("Translation key %s should contain all supported languages", key.getAttribute("value"))
                .containsExactlyInAnyOrderElementsOf(languages);
        }
    }

    /**
     * T052 — every key this feature introduces ships in all three languages (NFR-001, FR-018,
     * US4 scenario 10). Listed explicitly so a missing translation fails the build rather than
     * falling back to the raw key at runtime.
     */
    @Test
    public void cbrdParseKeysShipInEverySupportedLanguage() throws Exception {
        String[] requiredKeys = {
            // Preference labels
            "cbrd.parse.api.url.label",
            "cbrd.parse.token.label",
            "cbrd.parse.timeout.ms.label",
            // The nine service-enumerated causes
            "ai.markup.error.text_is_required",
            "ai.markup.error.text_is_too_long",
            "ai.markup.error.unsupported_language",
            "ai.markup.error.unauthorized",
            "ai.markup.error.parse_api_not_configured",
            "ai.markup.error.openai_credentials_unavailable",
            "ai.markup.error.openai_rate_limited",
            "ai.markup.error.openai_unavailable",
            "ai.markup.error.invalid_model_output",
            // Generic fallback, connectivity, and the two local guards
            "ai.markup.error.unexpected",
            "ai.markup.error.connectivity",
            "ai.markup.error.token_not_configured",
            "ai.markup.error.endpoint_url_invalid"
        };

        Document document = parseBundle();
        Set<String> languages = languagesIn(document);
        NodeList keyNodes = document.getElementsByTagName("key");

        for (String requiredKey : requiredKeys) {
            Element key = null;
            for (int i = 0; i < keyNodes.getLength(); i++) {
                if (requiredKey.equals(((Element) keyNodes.item(i)).getAttribute("value"))) {
                    key = (Element) keyNodes.item(i);
                    break;
                }
            }
            assertThat(key).as("Translation key %s must exist", requiredKey).isNotNull();

            Set<String> keyLanguages = new HashSet<String>();
            NodeList values = key.getElementsByTagName("val");
            for (int j = 0; j < values.getLength(); j++) {
                Element value = (Element) values.item(j);
                keyLanguages.add(value.getAttribute("lang"));
                assertThat(value.getTextContent().trim())
                    .as("Translation key %s must not be empty in %s", requiredKey, value.getAttribute("lang"))
                    .isNotEmpty();
            }
            assertThat(keyLanguages)
                .as("Translation key %s must ship in every supported language", requiredKey)
                .containsExactlyInAnyOrderElementsOf(languages);
        }
    }

    /**
     * The OpenAI-era keys retired with the client-side call path must be gone, so nothing can
     * quietly keep depending on them (FR-004).
     */
    @Test
    public void retiredOpenAiKeysAreGone() throws Exception {
        String[] retiredKeys = {
            "system.prompt.ai.markup", "error.no.APIKey", "error.no.parse.model", "http.error", "llm.error"
        };

        NodeList keyNodes = parseBundle().getElementsByTagName("key");
        Set<String> present = new HashSet<String>();
        for (int i = 0; i < keyNodes.getLength(); i++) {
            present.add(((Element) keyNodes.item(i)).getAttribute("value"));
        }

        for (String retired : retiredKeys) {
            assertThat(present).as("Retired key %s should no longer ship", retired).doesNotContain(retired);
        }
    }

    private Document parseBundle() throws Exception {
        return DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new File("src/main/resources/i18n/translation.xml"));
    }

    private Set<String> languagesIn(Document document) {
        Set<String> languages = new HashSet<String>();
        NodeList languageNodes = document.getElementsByTagName("language");
        for (int i = 0; i < languageNodes.getLength(); i++) {
            languages.add(((Element) languageNodes.item(i)).getAttribute("lang"));
        }
        return languages;
    }
}
