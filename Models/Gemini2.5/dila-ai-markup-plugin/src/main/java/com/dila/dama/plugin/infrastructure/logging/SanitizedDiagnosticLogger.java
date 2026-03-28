package com.dila.dama.plugin.infrastructure.logging;

import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import com.dila.dama.plugin.util.PluginLogger;

public class SanitizedDiagnosticLogger {

    public void logFailure(SanitizedTroubleshootingRecord record) {
        if (record == null) {
            return;
        }
        PluginLogger.error("[AiMarkupDiagnostics] requestId=" + record.getRequestId()
            + ", category=" + record.getFailureCategory()
            + ", status=" + record.getHttpStatus()
            + ", endpoint=" + record.getEndpointSummary()
            + ", snapshot=" + record.getRequestSnapshot()
            + ", serviceError=" + record.getServiceErrorBody());
    }
}
