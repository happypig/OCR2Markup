package com.dila.dama.plugin.domain.service;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SecretRedactorTest {

    private final SecretRedactor redactor = new SecretRedactor();

    @Test
    public void redactsBearerTokensAndApiKeys() {
        String redacted = redactor.redact("Authorization: Bearer sk-secret apiKey=sk-secret");

        assertThat(redacted).doesNotContain("sk-secret");
        assertThat(redacted).contains("Bearer ****");
        assertThat(redacted).contains("apiKey=****");
    }

    @Test
    public void redactsEmbeddedUrlCredentials() {
        String redacted = redactor.redact("https://user:pass@example.com/v1/chat/completions");

        assertThat(redacted).doesNotContain("user:pass");
        assertThat(redacted).contains("https://example.com");
    }

    @Test
    public void fingerprintsSecretsWithoutRevealingFullValue() {
        assertThat(redactor.fingerprint("sk-abcdefghijklmnopqrstuvwxyz")).startsWith("****");
        assertThat(redactor.fingerprint("sk-abcdefghijklmnopqrstuvwxyz")).endsWith("wxyz");
    }
}
