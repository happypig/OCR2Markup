package com.dila.dama.plugin.domain.model;

public class InvalidReferenceException extends Exception {

    private final String messageKey;
    private final Object[] params;

    public InvalidReferenceException(String messageKey, Object... params) {
        super(messageKey);
        this.messageKey = messageKey;
        this.params = params != null ? params.clone() : new Object[0];
    }

    public InvalidReferenceException(String messageKey, Throwable cause, Object... params) {
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

