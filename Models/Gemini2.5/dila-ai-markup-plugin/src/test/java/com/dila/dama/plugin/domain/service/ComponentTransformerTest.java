package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.TransformationException;
import com.dila.dama.plugin.domain.model.TransformedComponents;
import com.dila.dama.plugin.domain.model.TripitakaComponents;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ComponentTransformerTest {

    private final ComponentTransformer transformer = new ComponentTransformer(new NumeralConverter());

    @Test
    public void transform_mapsCanonAndNumeralsAndColumn() throws Exception {
        TripitakaComponents components = new TripitakaComponents("‡sŠ\"æ", "一.一六", "268", "二四九", "†úÝ„,S", "一");

        TransformedComponents transformed = transformer.transform(components);

        assertThat(transformed.getCanonCode()).isEqualTo("X");
        assertThat(transformed.getVolume()).isEqualTo("1.16");
        assertThat(transformed.getWork()).isEqualTo("268");
        assertThat(transformed.getPage()).isEqualTo("249");
        assertThat(transformed.getColumn()).isEqualTo("a");
        assertThat(transformed.getLine()).isEqualTo("1");
    }

    @Test
    public void transform_unknownCanon_throws() {
        TripitakaComponents components = new TripitakaComponents("UNKNOWN", "25", null, "917", null, null);
        assertThatThrownBy(() -> transformer.transform(components))
            .isInstanceOf(TransformationException.class);
    }
}

