package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.InvalidReferenceException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ReferenceValidationTest {

    private final ReferenceParser parser = new ReferenceParser();

    @Test
    public void invalidXml_reportsInvalidXmlKey() {
        assertInvalid("<ref><canon>T</canon>", "error.invalid.xml");
    }

    @Test
    public void nonRefElement_reportsNotRefKey() {
        assertInvalid("<div><canon>T</canon></div>", "error.not.ref.element");
    }

    @Test
    public void missingCanon_reportsMissingCanonKey() {
        assertInvalid("<ref><v>25</v><p>917</p></ref>", "error.missing.canon");
    }

    @Test
    public void missingVolume_reportsMissingVolumeKey() {
        assertInvalid("<ref><canon>T</canon><p>917</p></ref>", "error.missing.volume");
    }

    @Test
    public void missingPage_reportsMissingPageKey() {
        assertInvalid("<ref><canon>T</canon><v>25</v></ref>", "error.missing.page");
    }

    private void assertInvalid(String xml, String expectedKey) {
        try {
            parser.parseReference(xml);
            fail("Expected InvalidReferenceException");
        } catch (InvalidReferenceException ex) {
            assertThat(ex.getMessageKey()).isEqualTo(expectedKey);
        }
    }
}
