package com.dila.dama.plugin.domain.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecretRedactor {

    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9_\\-\\.]+");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(api[_-]?key\\s*[:=]\\s*)([^\\s,\\\"]+)");

    public String redact(String value) {
        if (value == null) {
            return "";
        }
        String redacted = redactBearerTokens(value);
        redacted = redactApiKeyAssignments(redacted);
        redacted = redactUrlCredentials(redacted);
        return redacted;
    }

    public String fingerprint(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            return "";
        }
        String trimmed = secret.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    private String redactBearerTokens(String input) {
        Matcher matcher = BEARER_PATTERN.matcher(input);
        return matcher.replaceAll("Bearer ****");
    }

    private String redactApiKeyAssignments(String input) {
        Matcher matcher = API_KEY_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + "****");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String redactUrlCredentials(String input) {
        try {
            if (!input.contains("://")) {
                return input;
            }
            URI uri = new URI(input);
            if (uri.getUserInfo() == null) {
                return input;
            }
            return new URI(
                uri.getScheme(),
                null,
                uri.getHost(),
                uri.getPort(),
                uri.getPath(),
                uri.getQuery(),
                uri.getFragment()
            ).toString();
        } catch (URISyntaxException e) {
            return input.replaceAll("://[^/@]+@", "://");
        }
    }
}
