package com.dila.dama.plugin.domain.service;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T006 — document-root language resolution (FR-007, US3).
 *
 * The resolver reads xml:lang from the document root element ONLY. Nested-ancestor divergence
 * is covered by T040.
 */
public class DocumentLanguageResolverTest {

    private final DocumentLanguageResolver resolver = new DocumentLanguageResolver();

    @Test
    public void bareChineseTagResolvesToChinese() {
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"zh\"><text/></TEI>")).isEqualTo("zh");
    }

    @Test
    public void bareJapaneseTagResolvesToJapanese() {
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"jp\"><text/></TEI>")).isEqualTo("jp");
    }

    @Test
    public void regionalChineseSubtagResolvesToChinese() {
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"zh-Hant\"><text/></TEI>")).isEqualTo("zh");
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"zh-Hans-CN\"><text/></TEI>")).isEqualTo("zh");
    }

    @Test
    public void regionalJapaneseSubtagResolvesToJapanese() {
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"ja-JP\"><text/></TEI>")).isEqualTo("jp");
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"ja\"><text/></TEI>")).isEqualTo("jp");
    }

    @Test
    public void resolutionIsCaseInsensitiveAndTrims() {
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"  ZH-HANT \"><text/></TEI>")).isEqualTo("zh");
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"JA-jp\"><text/></TEI>")).isEqualTo("jp");
    }

    @Test
    public void missingLanguageAttributeDefaultsToChinese() {
        assertThat(resolver.resolveFromXml("<TEI><text/></TEI>")).isEqualTo("zh");
    }

    @Test
    public void emptyLanguageAttributeDefaultsToChinese() {
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"\"><text/></TEI>")).isEqualTo("zh");
    }

    @Test
    public void unsupportedLanguageDefaultsToChineseRatherThanFailing() {
        // Policy: still send the request with the default indicator (spec Event Storming policy).
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"en-US\"><text/></TEI>")).isEqualTo("zh");
        assertThat(resolver.resolveFromXml("<TEI xml:lang=\"ko\"><text/></TEI>")).isEqualTo("zh");
    }

    @Test
    public void unparseableDocumentDefaultsToChinese() {
        assertThat(resolver.resolveFromXml("<TEI xml:lang=")).isEqualTo("zh");
        assertThat(resolver.resolveFromXml("not xml at all")).isEqualTo("zh");
    }

    @Test
    public void missingDocumentDefaultsToChinese() {
        assertThat(resolver.resolveFromXml(null)).isEqualTo("zh");
        assertThat(resolver.resolveFromXml("")).isEqualTo("zh");
    }

    // T040 - nested-ancestor divergence: the document root wins (US3 scenario 4).

    @Test
    public void nestedElementLanguageIsIgnoredInFavourOfTheDocumentRoot() {
        String xml = "<TEI xml:lang=\"zh\"><text><div xml:lang=\"ja\"><p>選取內容</p></div></text></TEI>";

        assertThat(resolver.resolveFromXml(xml)).isEqualTo("zh");
    }

    @Test
    public void nestedJapaneseRootWinsOverNestedChinese() {
        String xml = "<TEI xml:lang=\"ja-JP\"><text><div xml:lang=\"zh-Hant\"><p>選取內容</p></div></text></TEI>";

        assertThat(resolver.resolveFromXml(xml)).isEqualTo("jp");
    }

    @Test
    public void aNestedLanguageDoesNotRescueAnAbsentRootLanguage() {
        // Standard XML inheritance would pick up the nested ja; document-root-only must not.
        String xml = "<TEI><text><div xml:lang=\"ja\"><p>選取內容</p></div></text></TEI>";

        assertThat(resolver.resolveFromXml(xml)).isEqualTo("zh");
    }

    @Test
    public void deeplyNestedLanguagesAreAllIgnored() {
        String xml = "<TEI xml:lang=\"zh\"><a xml:lang=\"ja\"><b xml:lang=\"ja\"><c xml:lang=\"ja\"/></b></a></TEI>";

        assertThat(resolver.resolveFromXml(xml)).isEqualTo("zh");
    }

    @Test
    public void onlyTheTwoServiceSupportedIndicatorsAreEverReturned() {
        String[] inputs = {
            "<TEI xml:lang=\"zh\"/>", "<TEI xml:lang=\"ja\"/>", "<TEI xml:lang=\"en\"/>",
            "<TEI/>", "garbage"
        };
        for (String input : inputs) {
            assertThat(resolver.resolveFromXml(input)).isIn("zh", "jp");
        }
    }
}
