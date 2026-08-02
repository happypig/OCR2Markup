package com.dila.dama.plugin.application.command;

import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import com.dila.dama.plugin.domain.service.DiagnosticClassifier;
import com.dila.dama.plugin.domain.service.RequestValidationService;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import com.dila.dama.plugin.infrastructure.api.CbrdParseApiClient;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
import com.dila.dama.plugin.infrastructure.api.CbrdParseResponse;
import com.dila.dama.plugin.infrastructure.api.RequestTraceSnapshot;
import com.dila.dama.plugin.infrastructure.logging.SanitizedDiagnosticLogger;

public class RunAiMarkupDiagnosticsCommand {

    public static final class Result {
        private final boolean success;
        private final String markupResult;
        private final String summaryMessageKey;
        private final DiagnosticFailureCategory failureCategory;
        private final SanitizedTroubleshootingRecord troubleshootingRecord;
        private final AiMarkupDiagnosticSession session;

        private Result(
            boolean success,
            String markupResult,
            String summaryMessageKey,
            DiagnosticFailureCategory failureCategory,
            SanitizedTroubleshootingRecord troubleshootingRecord,
            AiMarkupDiagnosticSession session
        ) {
            this.success = success;
            this.markupResult = markupResult;
            this.summaryMessageKey = summaryMessageKey;
            this.failureCategory = failureCategory;
            this.troubleshootingRecord = troubleshootingRecord;
            this.session = session;
        }

        public static Result success(String markupResult, AiMarkupDiagnosticSession session) {
            return new Result(true, markupResult, null, null, null, session);
        }

        public static Result failure(String summaryMessageKey, DiagnosticFailureCategory failureCategory, SanitizedTroubleshootingRecord record, AiMarkupDiagnosticSession session) {
            return new Result(false, "", summaryMessageKey, failureCategory, record, session);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMarkupResult() {
            return markupResult;
        }

        public String getSummaryMessageKey() {
            return summaryMessageKey;
        }

        public DiagnosticFailureCategory getFailureCategory() {
            return failureCategory;
        }

        public SanitizedTroubleshootingRecord getTroubleshootingRecord() {
            return troubleshootingRecord;
        }

        public AiMarkupDiagnosticSession getSession() {
            return session;
        }
    }

    private final RequestValidationService validationService;
    private final DiagnosticClassifier classifier;
    private final CbrdParseApiClient parseClient;
    private final SecretRedactor redactor;
    private final SanitizedDiagnosticLogger logger;

    public RunAiMarkupDiagnosticsCommand() {
        this(new RequestValidationService(), new DiagnosticClassifier(), new CbrdParseApiClient(),
            new SecretRedactor(), new SanitizedDiagnosticLogger());
    }

    public RunAiMarkupDiagnosticsCommand(
        RequestValidationService validationService,
        DiagnosticClassifier classifier,
        CbrdParseApiClient parseClient,
        SecretRedactor redactor,
        SanitizedDiagnosticLogger logger
    ) {
        this.validationService = validationService;
        this.classifier = classifier;
        this.parseClient = parseClient;
        this.redactor = redactor;
        this.logger = logger;
    }

    /**
     * Runs one AI Markup transformation through the DILA CBRD Parse endpoint (FR-001, FR-006).
     *
     * Takes no system prompt: the transformation instruction lives on the DILA server with the
     * pretrained model, and the request contract forbids extra fields (research.md R11).
     */
    public Result execute(CbrdParseRequest request, CbrdParseConfiguration configuration, String platform) {
        String selectedText = request == null ? "" : request.getText();
        AiMarkupDiagnosticSession session = new AiMarkupDiagnosticSession(selectedText.length(), configuration);
        session.startOperation();
        try {
            session.validatingConfiguration();
            RequestValidationService.ValidationResult validationResult =
                validationService.validate(configuration, selectedText);
            if (!validationResult.isValid()) {
                // Caught before anything is sent: missing token, broken endpoint, unusable selection.
                DiagnosticClassifier.Classification classification = classifier.classifyValidationFailure(validationResult);
                SanitizedTroubleshootingRecord record = createRecord(
                    session,
                    platform,
                    null,
                    validationResult.getDetail(),
                    classification.getCategory(),
                    classification.getGuidanceMessageKey(),
                    null,
                    null
                );
                session.classifiedFailure(classification.getCategory(), classification.getGuidanceMessageKey(), record);
                session.exportReady();
                logger.logFailure(record);
                return Result.failure(classification.getGuidanceMessageKey(), classification.getCategory(), record, session);
            }

            session.buildingRequest();
            session.callingEndpoint();
            CbrdParseResponse response = parseClient.execute(configuration, request);
            if (response.isSuccess()) {
                session.completedSuccess();
                // The service returns the complete <ref> element - do not wrap it again.
                return Result.success(response.getMarkupXml(), session);
            }

            session.parsingResponse();
            DiagnosticClassifier.Classification classification = classifier.classifyParseError(response.getError());
            SanitizedTroubleshootingRecord record = createRecord(
                session,
                platform,
                response.getHttpStatus(),
                response.getErrorBody(),
                classification.getCategory(),
                classification.getGuidanceMessageKey(),
                response.getTrace(),
                sanitizeTransportError(response.getException())
            );
            session.classifiedFailure(classification.getCategory(), classification.getGuidanceMessageKey(), record);
            session.exportReady();
            logger.logFailure(record);
            return Result.failure(classification.getGuidanceMessageKey(), classification.getCategory(), record, session);
        } finally {
            session.finishOperation();
        }
    }

    private SanitizedTroubleshootingRecord createRecord(
        AiMarkupDiagnosticSession session,
        String platform,
        Integer httpStatus,
        String serviceErrorBody,
        DiagnosticFailureCategory category,
        String guidanceKey,
        RequestTraceSnapshot traceSnapshot,
        String transportError
    ) {
        String requestId = traceSnapshot == null ? session.getSessionId() : traceSnapshot.getRequestId();
        String endpointSummary = traceSnapshot == null ? session.getEndpointSummary() : traceSnapshot.getEndpointSummary();
        String requestSnapshot = traceSnapshot == null
            ? "validation=" + redactor.redact(session.getEndpointSummary())
            : redactor.redact(traceSnapshot.getRequestMetadataSummary());
        return new SanitizedTroubleshootingRecord(
            requestId,
            platform,
            endpointSummary,
            requestSnapshot,
            httpStatus,
            redactor.redact(serviceErrorBody),
            category,
            guidanceKey,
            System.currentTimeMillis(),
            true,
            transportError
        );
    }

    /**
     * Renders a transport exception as a compact, redacted {@code Class: message} chain so the
     * diagnostics export (FR-022) names the real cause — e.g. {@code SocketTimeoutException:
     * connect timed out} — instead of an opaque {@code CONNECTIVITY_OR_PROXY} category. Returns
     * null when the failure carried no exception (a known HTTP status, for instance).
     */
    private String sanitizeTransportError(Exception exception) {
        if (exception == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        Throwable current = exception;
        while (current != null) {
            if (builder.length() > 0) {
                builder.append(" -> ");
            }
            builder.append(current.getClass().getSimpleName());
            String message = current.getMessage();
            if (message != null) {
                builder.append(": ").append(redactor.redact(message.trim()));
            }
            current = current.getCause();
        }
        String rendered = builder.toString();
        return rendered.length() > 1000 ? rendered.substring(0, 1000) : rendered;
    }
}
