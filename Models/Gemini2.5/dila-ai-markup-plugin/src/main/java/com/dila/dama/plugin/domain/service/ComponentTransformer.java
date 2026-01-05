package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.TransformationException;
import com.dila.dama.plugin.domain.model.TransformedComponents;
import com.dila.dama.plugin.domain.model.TripitakaComponents;

import java.util.HashMap;
import java.util.Map;

public class ComponentTransformer {

    private static final Map<String, String> CANON_MAP = new HashMap<>();

    static {
        // From quickstart.md canon mapping examples
        CANON_MAP.put("†‘-œŠ\"æ", "T");
        CANON_MAP.put("‡sŠ\"æ", "X");
    }

    private final NumeralConverter numeralConverter;

    public ComponentTransformer(NumeralConverter numeralConverter) {
        this.numeralConverter = numeralConverter;
    }

    public TransformedComponents transform(TripitakaComponents components) throws TransformationException {
        if (components == null) {
            throw new TransformationException("error.invalid.numerals", "null");
        }

        String canonCode = normalizeCanon(components.getCanon());
        String volume = numeralConverter.toArabicDigits(components.getVolume());
        String page = numeralConverter.toArabicDigits(components.getPage());

        String work = components.getWork() != null ? numeralConverter.toArabicDigits(components.getWork()) : null;
        String line = components.getLine() != null ? numeralConverter.toArabicDigits(components.getLine()) : null;
        String column = components.getColumn() != null ? normalizeColumn(components.getColumn()) : null;

        return new TransformedComponents(canonCode, volume, work, page, column, line);
    }

    private static String normalizeCanon(String canon) throws TransformationException {
        if (canon == null || canon.trim().isEmpty()) {
            throw new TransformationException("error.missing.canon");
        }

        String trimmed = canon.trim();
        String upper = trimmed.toUpperCase();
        if ("T".equals(upper) || "X".equals(upper)) {
            return upper;
        }

        String mapped = CANON_MAP.get(trimmed);
        if (mapped != null) {
            return mapped;
        }

        throw new TransformationException("error.unknown.canon", canon);
    }

    private static String normalizeColumn(String column) throws TransformationException {
        String trimmed = column.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String lower = trimmed.toLowerCase();
        if ("a".equals(lower) || "b".equals(lower) || "c".equals(lower)) {
            return lower;
        }

        if ("„,S".equals(trimmed) || "†úÝ„,S".equals(trimmed)) {
            return "a";
        }
        if ("„,-".equals(trimmed)) {
            return "b";
        }
        if ("„,<".equals(trimmed)) {
            return "c";
        }

        throw new TransformationException("error.unknown.column", column);
    }
}

