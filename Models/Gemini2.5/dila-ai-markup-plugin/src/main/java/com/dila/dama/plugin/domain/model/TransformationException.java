package com.dila.dama.plugin.domain.model;

public class TransformationException extends Exception {

    private final String messageKey;
    private final Object[] params;

    public TransformationException(String messageKey, Object... params) {
        super(messageKey);
        this.messageKey = messageKey;
        this.params = params != null ? params.clone() : new Object[0];
    }

    public TransformationException(String messageKey, Throwable cause, Object... params) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.params = params != null ? params.clone() : new Object[0];
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getParams() {
        return params.clone();
    }
}

