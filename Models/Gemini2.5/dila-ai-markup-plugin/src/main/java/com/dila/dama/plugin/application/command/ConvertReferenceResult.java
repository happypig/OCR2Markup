package com.dila.dama.plugin.application.command;

public final class ConvertReferenceResult {

    private final boolean success;
    private final String url;
    private final String messageKey;
    private final Object[] messageParams;

    private ConvertReferenceResult(boolean success, String url, String messageKey, Object[] messageParams) {
        this.success = success;
        this.url = url;
        this.messageKey = messageKey;
        this.messageParams = messageParams != null ? messageParams.clone() : new Object[0];
    }

    public static ConvertReferenceResult success(String url) {
        return new ConvertReferenceResult(true, url, "success.link.generated", null);
    }

    public static ConvertReferenceResult failure(String messageKey, Object... params) {
        return new ConvertReferenceResult(false, null, messageKey, params);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getUrl() {
        return url;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageParams() {
        return messageParams.clone();
    }
}

