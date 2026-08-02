package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.service.DocumentLanguageResolver;
import org.json.JSONObject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T044 — the resolved language reaches the wire (US3 scenarios 1-3).
 */
public class CbrdParseApiClientLanguageTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";

    private final DocumentLanguageResolver resolver = new DocumentLanguageResolver();

    @Test
    public void chineseDocumentSendsLangZh() {
        assertThat(langSentFor("<TEI xml:lang=\"zh\"><text/></TEI>")).isEqualTo("zh");
    }

    @Test
    public void japaneseDocumentSendsLangJp() {
        assertThat(langSentFor("<TEI xml:lang=\"jp\"><text/></TEI>")).isEqualTo("jp");
        assertThat(langSentFor("<TEI xml:lang=\"ja-JP\"><text/></TEI>")).isEqualTo("jp");
    }

    @Test
    public void documentWithoutLanguageSendsTheChineseDefault() {
        assertThat(langSentFor("<TEI><text/></TEI>")).isEqualTo("zh");
    }

    @Test
    public void regionalChineseSubtagStillSendsZh() {
        assertThat(langSentFor("<TEI xml:lang=\"zh-Hant\"><text/></TEI>")).isEqualTo("zh");
    }

    @Test
    public void theLanguageFieldIsAlwaysOneTheServiceAccepts() {
        String[] documents = {
            "<TEI xml:lang=\"zh\"/>", "<TEI xml:lang=\"ja\"/>", "<TEI xml:lang=\"en\"/>", "<TEI/>"
        };
        for (String document : documents) {
            assertThat(langSentFor(document)).isIn("zh", "jp");
        }
    }

    private String langSentFor(String documentXml) {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, "<ref/>");
        String lang = resolver.resolveFromXml(documentXml);

        new CbrdParseApiClient(factory).execute(
            new CbrdParseConfiguration(ENDPOINT, 30000, "token"),
            new CbrdParseRequest("(T 1442)", lang)
        );

        return new JSONObject(factory.getCapturedRequestBody()).getString("lang");
    }
}
