package com.dila.dama.plugin.application.command;

import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import com.dila.dama.plugin.domain.service.DiagnosticClassifier;
import com.dila.dama.plugin.domain.service.RequestValidationService;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import com.dila.dama.plugin.infrastructure.api.OpenAiCompatibleChatClient;
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
    private final OpenAiCompatibleChatClient client;
    private final SecretRedactor redactor;
    private final SanitizedDiagnosticLogger logger;

    public RunAiMarkupDiagnosticsCommand() {
        this(new RequestValidationService(), new DiagnosticClassifier(), new OpenAiCompatibleChatClient(), new SecretRedactor(), new SanitizedDiagnosticLogger());
    }

    public RunAiMarkupDiagnosticsCommand(
        RequestValidationService validationService,
        DiagnosticClassifier classifier,
        OpenAiCompatibleChatClient client,
        SecretRedactor redactor,
        SanitizedDiagnosticLogger logger
    ) {
        this.validationService = validationService;
        this.classifier = classifier;
        this.client = client;
        this.redactor = redactor;
        this.logger = logger;
    }

    public Result execute(String selectedText, MarkupServiceConfiguration configuration, String systemPrompt, String platform) {
        AiMarkupDiagnosticSession session = new AiMarkupDiagnosticSession(selectedText == null ? 0 : selectedText.length(), configuration);
        session.startOperation();
        try {
            session.validatingConfiguration();
            RequestValidationService.ValidationResult validationResult = validationService.validate(configuration, selectedText);
            if (!validationResult.isValid()) {
                DiagnosticClassifier.Classification classification = classifier.classifyValidationFailure(validationResult);
                SanitizedTroubleshootingRecord record = createRecord(
                    session,
                    platform,
                    null,
                    validationResult.getDetail(),
                    classification.getCategory(),
                    classification.getGuidanceMessageKey(),
                    null
                );
                session.classifiedFailure(classification.getCategory(), classification.getGuidanceMessageKey(), record);
                session.exportReady();
                logger.logFailure(record);
                return Result.failure(classification.getGuidanceMessageKey(), classification.getCategory(), record, session);
            }

            session.buildingRequest();
            session.callingEndpoint();
            OpenAiCompatibleChatClient.Response response = client.execute(configuration, systemPrompt, selectedText);
            if (response.isSuccess()) {
                session.completedSuccess();
                return Result.success("<ref>" + response.getContent() + "</ref>", session);
            }

            session.parsingResponse();
            DiagnosticClassifier.Classification classification;
            if (response.getException() != null) {
                classification = classifier.classifyException(response.getException(), platform);
            } else {
                classification = classifier.classifyHttpFailure(
                    response.getHttpStatus() == null ? 0 : response.getHttpStatus().intValue(),
                    response.getErrorResponse(),
                    configuration,
                    platform
                );
            }
            SanitizedTroubleshootingRecord record = createRecord(
                session,
                platform,
                response.getHttpStatus(),
                response.getErrorResponse() == null ? "" : response.getErrorResponse().getSanitizedBody(),
                classification.getCategory(),
                classification.getGuidanceMessageKey(),
                response.getTraceSnapshot()
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
        RequestTraceSnapshot traceSnapshot
    ) {
        String requestId = traceSnapshot == null ? session.getSessionId() : traceSnapshot.getRequestId();
        String endpointSummary = traceSnapshot == null ? session.getConfiguration().getEndpointSummary() : traceSnapshot.getEndpointSummary();
        String requestSnapshot = traceSnapshot == null
            ? "validation=" + redactor.redact(session.getConfiguration().getEndpointSummary())
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
            true
        );
    }
}
