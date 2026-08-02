package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.util.XmlDomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Locale;

/**
 * Resolves the language indicator sent to the DILA CBRD Parse service (FR-007, US3).
 *
 * The value is read from {@code xml:lang} on the DOCUMENT ROOT ELEMENT only — never from the
 * selected element or any nested ancestor. Standard XML language inheritance would pick the
 * nearest ancestor; the team chose document-root-only for predictability (research.md R4).
 */
public class DocumentLanguageResolver {

    public static final String LANG_CHINESE = "zh";
    public static final String LANG_JAPANESE = "jp";

    private static final String XML_NAMESPACE_URI = "http://www.w3.org/XML/1998/namespace";

    /**
     * Resolves from a document's serialized XML. Anything unusable — absent attribute,
     * unparseable document, or a language the service does not accept — yields the Chinese
     * default rather than failing, so the request still goes out (spec Event Storming policy).
     */
    public String resolveFromXml(String documentXml) {
        if (documentXml == null || documentXml.trim().isEmpty()) {
            return LANG_CHINESE;
        }
        try {
            return resolve(XmlDomUtils.parseXml(documentXml));
        } catch (Exception e) {
            return LANG_CHINESE;
        }
    }

    public String resolve(Document document) {
        if (document == null) {
            return LANG_CHINESE;
        }
        Element root = document.getDocumentElement();
        if (root == null) {
            return LANG_CHINESE;
        }
        return resolveFromLanguageTag(root.getAttributeNS(XML_NAMESPACE_URI, "lang"));
    }

    /**
     * Maps a BCP-47-style tag to a service indicator: anything starting {@code zh} is Chinese,
     * anything starting {@code ja} or {@code jp} is Japanese, everything else defaults to
     * Chinese. Regional subtags such as {@code zh-Hant} and {@code ja-JP} are handled here.
     */
    public String resolveFromLanguageTag(String languageTag) {
        if (languageTag == null) {
            return LANG_CHINESE;
        }
        String normalized = languageTag.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return LANG_CHINESE;
        }
        if (normalized.startsWith("ja") || normalized.startsWith("jp")) {
            return LANG_JAPANESE;
        }
        return LANG_CHINESE;
    }
}
