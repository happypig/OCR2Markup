package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.TransformationException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NumeralConverterTest {

    private final NumeralConverter converter = new NumeralConverter();

    @Test
    public void toArabicDigits_returnsArabicUnchanged() throws Exception {
        assertThat(converter.toArabicDigits("25")).isEqualTo("25");
        assertThat(converter.toArabicDigits("1.16")).isEqualTo("1.16");
        assertThat(converter.toArabicDigits("01")).isEqualTo("01");
    }

    @Test
    public void toArabicDigits_convertsChineseDigitSequence() throws Exception {
        assertThat(converter.toArabicDigits("二四九")).isEqualTo("249");
        assertThat(converter.toArabicDigits("九一七")).isEqualTo("917");
        assertThat(converter.toArabicDigits("一.一六")).isEqualTo("1.16");
        assertThat(converter.toArabicDigits("〇一")).isEqualTo("01");
    }

    @Test
    public void toArabicDigits_supportsKnownLegacyEncodingsFromDocs() throws Exception {
        assertThat(converter.toArabicDigits("„,?")).isEqualTo("1");
        assertThat(converter.toArabicDigits("„§O†>>")).isEqualTo("24");
        assertThat(converter.toArabicDigits("„,?aŸ¯„,?†.-")).isEqualTo("1.16");
    }

    @Test
    public void toArabicDigits_rejectsUnknownFormat() {
        assertThatThrownBy(() -> converter.toArabicDigits("十"))
            .isInstanceOf(TransformationException.class);
    }
}

