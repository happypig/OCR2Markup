package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.InvalidReferenceException;
import com.dila.dama.plugin.domain.model.TripitakaComponents;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReferenceParserTest {

    private final ReferenceParser parser = new ReferenceParser();

    @Test
    public void parseReference_validRef_extractsComponents() throws Exception {
        String xml = "<ref xml:id=\"ref001\" checked=\"0\">"
            + "<canon>X</canon>"
            + "<v>1.16</v>"
            + "<w>268</w>"
            + "<p>249</p>"
            + "<c>a</c>"
            + "<l>01</l>"
            + "</ref>";

        TripitakaComponents components = parser.parseReference(xml);

        assertThat(components.getCanon()).isEqualTo("X");
        assertThat(components.getVolume()).isEqualTo("1.16");
        assertThat(components.getWork()).isEqualTo("268");
        assertThat(components.getPage()).isEqualTo("249");
        assertThat(components.getColumn()).isEqualTo("a");
        assertThat(components.getLine()).isEqualTo("01");
    }

    @Test
    public void parseReference_invalidXml_throws() {
        withSilencedErr(() -> assertThatThrownBy(() -> parser.parseReference("<ref><canon>X</canon>"))
            .isInstanceOf(InvalidReferenceException.class));
    }

    @Test
    public void parseReference_notRefElement_throws() {
        assertThatThrownBy(() -> parser.parseReference("<div><canon>X</canon></div>"))
            .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    public void parseReference_missingCanon_throws() {
        String xml = "<ref><v>25</v><p>917</p></ref>";
        assertThatThrownBy(() -> parser.parseReference(xml))
            .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    public void parseReference_missingVolume_throws() {
        String xml = "<ref><canon>T</canon><p>917</p></ref>";
        assertThatThrownBy(() -> parser.parseReference(xml))
            .isInstanceOf(InvalidReferenceException.class);
    }

    @Test
    public void parseReference_missingPage_throws() {
        String xml = "<ref><canon>T</canon><v>25</v></ref>";
        assertThatThrownBy(() -> parser.parseReference(xml))
            .isInstanceOf(InvalidReferenceException.class);
    }

    private void withSilencedErr(Runnable action) {
        PrintStream originalErr = System.err;
        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            action.run();
        } finally {
            System.setErr(originalErr);
        }
    }
}
