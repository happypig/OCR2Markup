package com.dila.dama.plugin.infrastructure.release;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The declared version and the release notes must agree.
 *
 * This exists because the two are edited by hand in different files, at different moments, by
 * whoever is cutting the release - exactly the shape of mistake that produced a shipped plugin
 * whose About dialog and release notes disagreed. Asserting the relationship rather than either
 * value means a version bump keeps this green, while bumping only one of the two turns it red.
 */
public class VersionConsistencyTest {

    @Test
    public void newestReleaseNotesHeadingMatchesTheDeclaredVersion() throws Exception {
        String declared = declaredProjectVersion();
        String newestHeading = newestReleaseNotesHeading();

        assertThat(newestHeading)
            .as("release-notes.xhtml's newest <h4> must match pom.xml's <version>; "
                + "bump both together (pom.xml is the single source of truth)")
            .isEqualTo("v" + declared);
    }

    @Test
    public void declaredVersionLooksLikeASemanticVersion() {
        assertThat(declaredProjectVersion()).matches("\\d+\\.\\d+\\.\\d+(-.+)?");
    }

    @Test
    public void releaseNotesAreOrderedNewestFirst() throws Exception {
        NodeList headings = releaseNotes().getElementsByTagName("h4");
        assertThat(headings.getLength()).isGreaterThan(1);

        Comparable<String> previous = null;
        for (int i = 0; i < headings.getLength(); i++) {
            String version = headings.item(i).getTextContent().trim();
            assertThat(version).startsWith("v");
            if (previous != null) {
                assertThat(compare(previous.toString(), version))
                    .as("release notes must run newest first, but %s precedes %s", previous, version)
                    .isPositive();
            }
            previous = version;
        }
    }

    private String declaredProjectVersion() {
        try {
            // The project's own <version>, not a dependency's: take the direct child of <project>.
            Document pom = parse(new File("pom.xml"));
            NodeList children = pom.getDocumentElement().getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE && "version".equals(child.getNodeName())) {
                    return child.getTextContent().trim();
                }
            }
            throw new IllegalStateException("pom.xml declares no project <version>");
        } catch (Exception e) {
            throw new IllegalStateException("Could not read pom.xml", e);
        }
    }

    private String newestReleaseNotesHeading() throws Exception {
        NodeList headings = releaseNotes().getElementsByTagName("h4");
        assertThat(headings.getLength()).as("release-notes.xhtml must contain at least one <h4>").isPositive();
        return ((Element) headings.item(0)).getTextContent().trim();
    }

    private Document releaseNotes() throws Exception {
        return parse(new File("src/main/resources/release-notes.xhtml"));
    }

    private Document parse(File file) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
    }

    /** Compares "v1.2.3" style headings numerically; positive when {@code a} is newer than {@code b}. */
    private int compare(String a, String b) {
        String[] left = a.substring(1).split("\\.");
        String[] right = b.substring(1).split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int l = i < left.length ? parse(left[i]) : 0;
            int r = i < right.length ? parse(right[i]) : 0;
            if (l != r) {
                return l - r;
            }
        }
        return 0;
    }

    private int parse(String segment) {
        try {
            return Integer.parseInt(segment.replaceAll("\\D.*$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
