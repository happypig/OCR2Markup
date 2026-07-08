package com.dila.dama.plugin.infrastructure.export;

import com.dila.dama.plugin.domain.model.ExportedDiagnosticPackage;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class DiagnosticExportWriter {

    public void write(ExportedDiagnosticPackage diagnosticPackage, File outputFile) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", diagnosticPackage.getSchemaVersion());
        root.put("generatedAt", diagnosticPackage.getGeneratedAt());
        root.put("sessionId", diagnosticPackage.getSessionId());
        root.put("exportReason", diagnosticPackage.getExportReason());
        root.put("record", serializeRecord(diagnosticPackage.getRecord()));

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")) {
            writer.write(root.toString());
        }
    }

    private JSONObject serializeRecord(SanitizedTroubleshootingRecord record) {
        JSONObject json = new JSONObject();
        json.put("requestId", record.getRequestId());
        json.put("platform", record.getPlatform());
        json.put("endpointSummary", record.getEndpointSummary());
        json.put("requestSnapshot", record.getRequestSnapshot());
        json.put("httpStatus", record.getHttpStatus());
        json.put("serviceErrorBody", record.getServiceErrorBody());
        json.put("failureCategory", record.getFailureCategory() == null ? JSONObject.NULL : record.getFailureCategory().name());
        json.put("guidanceMessageKey", record.getGuidanceMessageKey());
        json.put("capturedAtEpochMs", record.getCapturedAtEpochMs());
        json.put("redactionApplied", record.isRedactionApplied());
        return json;
    }
}
