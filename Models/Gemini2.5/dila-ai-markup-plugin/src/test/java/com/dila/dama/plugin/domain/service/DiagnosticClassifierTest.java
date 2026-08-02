package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.ParseError;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Retargeted to the CBRD Parse path (004-cbrd-parse-endpoint). The OpenAI HTTP-shape heuristics
 * are gone: the DILA service enumerates its own causes, so classification is a lookup rather
 * than message sniffing.
 */
public class DiagnosticClassifierTest {

    private final DiagnosticClassifier classifier = new DiagnosticClassifier();

    @Test
    public void classifiesRejectedTokenAsCredentials() {
        DiagnosticClassifier.Classification classification = classifier.classifyParseError(ParseError.UNAUTHORIZED);

        assertThat(classification.getCategory()).isEqualTo(DiagnosticFailureCategory.CREDENTIALS);
        assertThat(classification.getGuidanceMessageKey()).isEqualTo("ai.markup.error.unauthorized");
    }

    @Test
    public void classifiesRateLimitingAsCapacity() {
        assertThat(classifier.classifyParseError(ParseError.OPENAI_RATE_LIMITED).getCategory())
            .isEqualTo(DiagnosticFailureCategory.RATE_LIMIT_OR_CAPACITY);
    }

    @Test
    public void classifiesUpstreamOutageAsAServiceFailureNotACredentialProblem() {
        assertThat(classifier.classifyParseError(ParseError.OPENAI_UNAVAILABLE).getCategory())
            .isEqualTo(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE);
        assertThat(classifier.classifyParseError(ParseError.OPENAI_CREDENTIALS_UNAVAILABLE).getCategory())
            .isEqualTo(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE);
    }

    @Test
    public void classifiesInputProblemsAsMalformedRequests() {
        assertThat(classifier.classifyParseError(ParseError.TEXT_IS_TOO_LONG).getCategory())
            .isEqualTo(DiagnosticFailureCategory.MALFORMED_REQUEST);
        assertThat(classifier.classifyParseError(ParseError.UNSUPPORTED_LANGUAGE).getCategory())
            .isEqualTo(DiagnosticFailureCategory.MALFORMED_REQUEST);
    }

    @Test
    public void parseErrorClassificationCarriesFullConfidence() {
        assertThat(classifier.classifyParseError(ParseError.INVALID_MODEL_OUTPUT).getConfidence()).isEqualTo(100);
    }

    @Test
    public void everyParseErrorIsClassifiable() {
        for (ParseError error : ParseError.values()) {
            DiagnosticClassifier.Classification classification = classifier.classifyParseError(error);

            assertThat(classification.getCategory()).isNotNull();
            assertThat(classification.getGuidanceMessageKey()).isNotEmpty();
        }
    }

    @Test
    public void transportFailuresAreStillClassifiedAsConnectivity() {
        assertThat(classifier.classifyException(new SocketTimeoutException("timeout"), "generic").getCategory())
            .isEqualTo(DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY);
        assertThat(classifier.classifyException(new IOException("refused"), "generic").getCategory())
            .isEqualTo(DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY);
    }

    @Test
    public void platformSuffixingStillAppliesToTheLegacyDiagnosticKeys() {
        assertThat(classifier.classifyException(new IOException("refused"), "windows").getGuidanceMessageKey())
            .endsWith(".windows");
    }
}
