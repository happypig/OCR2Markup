package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.service.SecretRedactor;
import org.json.JSONObject;

public final class OpenAiErrorResponse {

    private final String message;
    private final String type;
    private final String code;
    private final String param;
    private final String sanitizedBody;

    public OpenAiErrorResponse(String message, String type, String code, String param, String sanitizedBody) {
        this.message = message;
        this.type = type;
        this.code = code;
        this.param = param;
        this.sanitizedBody = sanitizedBody;
    }

    public static OpenAiErrorResponse parse(String body, SecretRedactor redactor) {
        String sanitized = redactor.redact(body);
        if (sanitized == null || sanitized.trim().isEmpty()) {
            return new OpenAiErrorResponse("", "", "", "", "");
        }
        try {
            JSONObject object = new JSONObject(sanitized);
            JSONObject errorObject = object.optJSONObject("error");
            if (errorObject != null) {
                return new OpenAiErrorResponse(
                    errorObject.optString("message", ""),
                    errorObject.optString("type", ""),
                    errorObject.optString("code", ""),
                    errorObject.optString("param", ""),
                    sanitized
                );
            }
            return new OpenAiErrorResponse(
                object.optString("message", sanitized),
                object.optString("type", ""),
                object.optString("code", ""),
                object.optString("param", ""),
                sanitized
            );
        } catch (Exception e) {
            return new OpenAiErrorResponse(sanitized, "", "", "", sanitized);
        }
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getParam() {
        return param;
    }

    public String getSanitizedBody() {
        return sanitizedBody;
    }

    public String asSearchableText() {
        return (message + " " + type + " " + code + " " + param + " " + sanitizedBody).toLowerCase();
    }
}
