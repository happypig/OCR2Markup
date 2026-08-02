package com.dila.dama.plugin.infrastructure.api;

import org.json.JSONObject;

/**
 * The CBRD Parse request body: the selected text and a language indicator, nothing else
 * (FR-006).
 *
 * {@code contracts/openapi.yaml} declares {@code ParseRequest} with
 * {@code additionalProperties: false}, so no system prompt, model name, or platform field may
 * be added. The transformation instruction lives on the DILA server with the pretrained model
 * (research.md R11).
 */
public final class CbrdParseRequest {

    private final String text;
    private final String lang;

    public CbrdParseRequest(String text, String lang) {
        this.text = text == null ? "" : text;
        this.lang = lang == null ? "" : lang;
    }

    public String getText() {
        return text;
    }

    public String getLang() {
        return lang;
    }

    public String toJson() {
        JSONObject body = new JSONObject();
        body.put("text", text);
        body.put("lang", lang);
        return body.toString();
    }

    @Override
    public String toString() {
        // Length only: the selection itself is the editor's document content.
        return "CbrdParseRequest{textLength=" + text.length() + ", lang='" + lang + "'}";
    }
}
