package com.dila.dama.plugin.infrastructure.release;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReleaseNotesResourceLoader {

    private static final String RESOURCE_PATH = "release-notes.xhtml";

    public String load() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(RESOURCE_PATH);
        if (stream == null) {
            throw new IOException("Missing release notes resource: " + RESOURCE_PATH);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        String content = new String(output.toByteArray(), "UTF-8").trim();
        if (content.isEmpty()) {
            throw new IOException("Release notes resource is empty");
        }
        return content;
    }
}
