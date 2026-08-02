package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import org.json.JSONObject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T007b — the request body is exactly {"text","lang"} (FR-006).
 *
 * contracts/openapi.yaml declares ParseRequest with additionalProperties: false, so a
 * client-supplied system prompt or model name cannot be sent. The transformation instruction
 * now lives on the DILA server (research.md R11).
 */
public class CbrdParseRequestBodyTest {

    @Test
    public void serialisesExactlyTextAndLang() {
        JSONObject body = new JSONObject(new CbrdParseRequest("(T 1442)，大正23", "zh").toJson());

        assertThat(body.keySet()).containsExactlyInAnyOrder("text", "lang");
        assertThat(body.getString("text")).isEqualTo("(T 1442)，大正23");
        assertThat(body.getString("lang")).isEqualTo("zh");
    }

    @Test
    public void carriesNoSystemPromptModelOrPlatformField() {
        JSONObject body = new JSONObject(new CbrdParseRequest("text", "jp").toJson());

        assertThat(body.has("system")).isFalse();
        assertThat(body.has("systemPrompt")).isFalse();
        assertThat(body.has("messages")).isFalse();
        assertThat(body.has("model")).isFalse();
        assertThat(body.has("max_tokens")).isFalse();
        assertThat(body.has("platform")).isFalse();
        assertThat(body.has("temperature")).isFalse();
    }

    @Test
    public void bodySentOnTheWireMatchesTheSerialisedRequest() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, "<ref/>");

        new CbrdParseApiClient(factory).execute(
            new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, "token"),
            new CbrdParseRequest("大正23", "jp")
        );

        JSONObject sent = new JSONObject(factory.getCapturedRequestBody());
        assertThat(sent.keySet()).containsExactlyInAnyOrder("text", "lang");
        assertThat(sent.getString("text")).isEqualTo("大正23");
        assertThat(sent.getString("lang")).isEqualTo("jp");
    }

    @Test
    public void nonAsciiTextSurvivesUtf8Encoding() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, "<ref/>");
        String text = "（大正藏）第二十三冊，頁八六九中";

        new CbrdParseApiClient(factory).execute(
            new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, "token"),
            new CbrdParseRequest(text, "zh")
        );

        assertThat(new JSONObject(factory.getCapturedRequestBody()).getString("text")).isEqualTo(text);
    }

    @Test
    public void exposesTextAndLangForTraceReporting() {
        CbrdParseRequest request = new CbrdParseRequest("  spaced  ", "zh");

        assertThat(request.getText()).isEqualTo("  spaced  ");
        assertThat(request.getLang()).isEqualTo("zh");
    }
}
