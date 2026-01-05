package com.dila.dama.plugin.domain.model;

import java.util.Objects;

public final class TransformedComponents {

    private final String canonCode;
    private final String volume;
    private final String work;
    private final String page;
    private final String column;
    private final String line;

    public TransformedComponents(String canonCode, String volume, String work, String page, String column, String line) {
        this.canonCode = requireCanonCode(canonCode);
        this.volume = requireTrimmedNonEmpty(volume, "volume");
        this.work = trimToNull(work);
        this.page = trimToNull(page);
        this.column = trimToNull(column);
        this.line = trimToNull(line);
    }

    private static String requireCanonCode(String value) {
        String trimmed = requireTrimmedNonEmpty(value, "canonCode").toUpperCase();
        if (trimmed.length() < 1 || trimmed.length() > 2) {
            throw new IllegalArgumentException("Invalid canonCode: " + value);
        }
        return trimmed;
    }

    private static String requireTrimmedNonEmpty(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getCanonCode() {
        return canonCode;
    }

    public String getVolume() {
        return volume;
    }

    public String getWork() {
        return work;
    }

    public String getPage() {
        return page;
    }

    public String getColumn() {
        return column;
    }

    public String getLine() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransformedComponents)) return false;
        TransformedComponents that = (TransformedComponents) o;
        return Objects.equals(canonCode, that.canonCode)
            && Objects.equals(volume, that.volume)
            && Objects.equals(work, that.work)
            && Objects.equals(page, that.page)
            && Objects.equals(column, that.column)
            && Objects.equals(line, that.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonCode, volume, work, page, column, line);
    }
}

