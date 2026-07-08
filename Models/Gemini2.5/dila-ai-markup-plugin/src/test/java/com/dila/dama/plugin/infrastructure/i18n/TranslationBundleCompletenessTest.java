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
}
