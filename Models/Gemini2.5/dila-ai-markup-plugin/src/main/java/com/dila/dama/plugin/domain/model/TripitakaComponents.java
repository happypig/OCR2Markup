package com.dila.dama.plugin.domain.model;

import java.util.Objects;

public final class TripitakaComponents {

    private final String canon;
    private final String volume;
    private final String work;
    private final String page;
    private final String column;
    private final String line;

    public TripitakaComponents(String canon, String volume, String work, String page, String column, String line) {
        this.canon = requireTrimmedNonEmpty(canon, "canon");
        this.volume = requireTrimmedNonEmpty(volume, "volume");
        this.page = requireTrimmedNonEmpty(page, "page");
        this.work = trimToNull(work);
        this.column = trimToNull(column);
        this.line = trimToNull(line);
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

    public String getCanon() {
        return canon;
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

    public boolean hasWork() {
        return work != null && !work.isEmpty();
    }

    public boolean hasColumn() {
        return column != null && !column.isEmpty();
    }

    public boolean hasLine() {
        return line != null && !line.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TripitakaComponents)) return false;
        TripitakaComponents that = (TripitakaComponents) o;
        return Objects.equals(canon, that.canon)
            && Objects.equals(volume, that.volume)
            && Objects.equals(work, that.work)
            && Objects.equals(page, that.page)
            && Objects.equals(column, that.column)
            && Objects.equals(line, that.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canon, volume, work, page, column, line);
    }

    @Override
    public String toString() {
        return "TripitakaComponents{" +
            "canon='" + canon + '\'' +
            ", volume='" + volume + '\'' +
            ", work='" + work + '\'' +
            ", page='" + page + '\'' +
            ", column='" + column + '\'' +
            ", line='" + line + '\'' +
            '}';
    }
}

