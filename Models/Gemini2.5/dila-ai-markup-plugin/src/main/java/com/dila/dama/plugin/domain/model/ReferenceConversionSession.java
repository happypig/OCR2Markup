package com.dila.dama.plugin.domain.model;

import java.util.Objects;

public final class ReferenceConversionSession {

    public enum Status {
        INITIALIZED,
        PARSING,
        TRANSFORMING,
        CALLING_API,
        COMPLETED,
        FAILED,
        REPLACED
    }

    private final String selectedRefXml;
    private final TripitakaComponents tripitakaComponents;
    private final TransformedComponents transformedComponents;
    private final String generatedUrl;
    private final Status status;
    private final String messageKey;
    private final Object[] messageParams;

    private ReferenceConversionSession(
        String selectedRefXml,
        TripitakaComponents tripitakaComponents,
        TransformedComponents transformedComponents,
        String generatedUrl,
        Status status,
        String messageKey,
        Object[] messageParams
    ) {
        this.selectedRefXml = selectedRefXml;
        this.tripitakaComponents = tripitakaComponents;
        this.transformedComponents = transformedComponents;
        this.generatedUrl = generatedUrl;
        this.status = status;
        this.messageKey = messageKey;
        this.messageParams = messageParams != null ? messageParams.clone() : new Object[0];
    }

    public static ReferenceConversionSession initialize(String selectedRefXml) {
        return new ReferenceConversionSession(selectedRefXml, null, null, null, Status.INITIALIZED, null, null);
    }

    public ReferenceConversionSession parsing() {
        return new ReferenceConversionSession(selectedRefXml, tripitakaComponents, transformedComponents, generatedUrl, Status.PARSING, null, null);
    }

    public ReferenceConversionSession withParsed(TripitakaComponents parsed) {
        return new ReferenceConversionSession(selectedRefXml, parsed, transformedComponents, generatedUrl, Status.TRANSFORMING, null, null);
    }

    public ReferenceConversionSession withTransformed(TransformedComponents transformed) {
        return new ReferenceConversionSession(selectedRefXml, tripitakaComponents, transformed, generatedUrl, Status.CALLING_API, null, null);
    }

    public ReferenceConversionSession completed(String url) {
        return new ReferenceConversionSession(selectedRefXml, tripitakaComponents, transformedComponents, url, Status.COMPLETED, "success.link.generated", null);
    }

    public ReferenceConversionSession replaced() {
        return new ReferenceConversionSession(selectedRefXml, tripitakaComponents, transformedComponents, generatedUrl, Status.REPLACED, "success.replacement.complete", null);
    }

    public ReferenceConversionSession failed(String messageKey, Object... params) {
        return new ReferenceConversionSession(selectedRefXml, tripitakaComponents, transformedComponents, generatedUrl, Status.FAILED, messageKey, params);
    }

    public String getSelectedRefXml() {
        return selectedRefXml;
    }

    public TripitakaComponents getTripitakaComponents() {
        return tripitakaComponents;
    }

    public TransformedComponents getTransformedComponents() {
        return transformedComponents;
    }

    public String getGeneratedUrl() {
        return generatedUrl;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageParams() {
        return messageParams.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReferenceConversionSession)) return false;
        ReferenceConversionSession that = (ReferenceConversionSession) o;
        return Objects.equals(selectedRefXml, that.selectedRefXml)
            && Objects.equals(tripitakaComponents, that.tripitakaComponents)
            && Objects.equals(transformedComponents, that.transformedComponents)
            && Objects.equals(generatedUrl, that.generatedUrl)
            && status == that.status
            && Objects.equals(messageKey, that.messageKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectedRefXml, tripitakaComponents, transformedComponents, generatedUrl, status, messageKey);
    }
}

