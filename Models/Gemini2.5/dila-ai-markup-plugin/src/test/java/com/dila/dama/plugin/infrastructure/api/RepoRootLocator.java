package com.dila.dama.plugin.infrastructure.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Locates the repository root from inside the Maven module, so tests can read artifacts that live
 * above it — chiefly the vendored contract at {@code specs/001-ref-to-link-action/contracts/}.
 *
 * <p>The module sits three levels below the repository root
 * ({@code Models/Gemini2.5/dila-ai-markup-plugin}), and the working directory differs between
 * {@code mvn test} run from the module, {@code mvn test} run from the root, and an IDE launch.
 * Hardcoding {@code ../../../} passes under one and breaks under the others (research D4).
 *
 * <p>This class never returns a fallback. A guard that silently passes because it could not find
 * the file it was supposed to check is worse than no guard, so a failed walk throws and names
 * every directory it tried.
 */
final class RepoRootLocator {

    private RepoRootLocator() {
    }

    /** Marker that identifies the repository root. */
    private static final String MARKER = "specs";

    /**
     * @return the repository root directory
     * @throws IllegalStateException if no ancestor of {@code user.dir} contains {@code specs/}
     */
    static File repoRoot() {
        List<String> tried = new ArrayList<>();
        File current = new File(System.getProperty("user.dir")).getAbsoluteFile();

        while (current != null) {
            File marker = new File(current, MARKER);
            if (marker.isDirectory()) {
                return current;
            }
            tried.add(current.getAbsolutePath());
            current = current.getParentFile();
        }

        throw new IllegalStateException(
            "Could not locate the repository root: no ancestor of user.dir contains a '"
                + MARKER + "' directory. Tried: " + tried);
    }

    /**
     * @param relativePath repository-root-relative path, using forward slashes
     * @return the resolved file
     * @throws IllegalStateException if the repository root cannot be found, or the file is absent
     */
    static File repoFile(String relativePath) {
        File root = repoRoot();
        File resolved = new File(root, relativePath.replace('/', File.separatorChar));
        if (!resolved.isFile()) {
            throw new IllegalStateException(
                "Expected file not found: " + resolved.getAbsolutePath()
                    + " (repository root resolved to " + root.getAbsolutePath() + ")");
        }
        return resolved;
    }
}
