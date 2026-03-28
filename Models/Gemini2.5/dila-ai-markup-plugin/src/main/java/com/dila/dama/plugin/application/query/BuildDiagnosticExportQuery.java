package com.dila.dama.plugin.application.query;

import com.dila.dama.plugin.domain.model.ExportedDiagnosticPackage;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class BuildDiagnosticExportQuery {

    public ExportedDiagnosticPackage build(String sessionId, SanitizedTroubleshootingRecord record, String exportReason) {
        return new ExportedDiagnosticPackage("1.0.0", isoNow(), sessionId, record, exportReason == null ? "" : exportReason.trim());
    }

    private String isoNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}
