package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.preferences.DAMAOptionPagePluginExtension;
import org.junit.Before;
import org.junit.Test;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T023a — the guards that run before any request is sent (FR-010, FR-019, FR-021).
 *
 * Each cause must name itself: "no token configured", "token rejected", "endpoint URL broken",
 * and "selection unusable" send the editor to four different places.
 */
public class AIMarkupConfigGuardTest {

    private static final String VALID_SELECTION = "(T 1442)，大正23";

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void fullyConfiguredRequestPassesPreflight() {
        extension.setOptionStorageForTests(storage("https://cbss.dila.edu.tw/cbrd/parse", "token", "30000"));

        assertThat(extension.aiMarkupPreflightGuidance(VALID_SELECTION)).isNull();
    }

    @Test
    public void missingTokenIsRefusedAndNamesTheTokenPreference() {
        extension.setOptionStorageForTests(storage("https://cbss.dila.edu.tw/cbrd/parse", "", "30000"));

        assertThat(extension.aiMarkupPreflightGuidance(VALID_SELECTION))
            .isEqualTo("ai.markup.error.token_not_configured");
    }

    @Test
    public void malformedEndpointUrlIsRefusedAndNamesTheEndpointPreference() {
        extension.setOptionStorageForTests(storage("not-a-url", "token", "30000"));

        assertThat(extension.aiMarkupPreflightGuidance(VALID_SELECTION))
            .isEqualTo("ai.markup.error.endpoint_url_invalid");
    }

    @Test
    public void endpointAndTokenProblemsProduceDifferentGuidance() {
        extension.setOptionStorageForTests(storage("not-a-url", "token", "30000"));
        String endpointGuidance = extension.aiMarkupPreflightGuidance(VALID_SELECTION);

        extension.setOptionStorageForTests(storage("https://cbss.dila.edu.tw/cbrd/parse", "", "30000"));
        String tokenGuidance = extension.aiMarkupPreflightGuidance(VALID_SELECTION);

        assertThat(endpointGuidance).isNotEqualTo(tokenGuidance);
    }

    @Test
    public void emptySelectionIsRefusedWithTheInputMissingGuidance() {
        extension.setOptionStorageForTests(storage("https://cbss.dila.edu.tw/cbrd/parse", "token", "30000"));

        assertThat(extension.aiMarkupPreflightGuidance("")).isEqualTo("ai.markup.error.text_is_required");
        assertThat(extension.aiMarkupPreflightGuidance("   ")).isEqualTo("ai.markup.error.text_is_required");
    }

    @Test
    public void tooLongSelectionIsRefusedWithTheTooLongGuidance() {
        extension.setOptionStorageForTests(storage("https://cbss.dila.edu.tw/cbrd/parse", "token", "30000"));
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i <= 4000; i++) {
            tooLong.append('好');
        }

        assertThat(extension.aiMarkupPreflightGuidance(tooLong.toString()))
            .isEqualTo("ai.markup.error.text_is_too_long");
    }

    @Test
    public void selectionAtTheServiceLimitIsAccepted() {
        extension.setOptionStorageForTests(storage("https://cbss.dila.edu.tw/cbrd/parse", "token", "30000"));
        StringBuilder atLimit = new StringBuilder();
        for (int i = 0; i < 4000; i++) {
            atLimit.append('好');
        }

        assertThat(extension.aiMarkupPreflightGuidance(atLimit.toString())).isNull();
    }

    @Test
    public void everyGuardMessageIsDistinct() {
        extension.setOptionStorageForTests(storage("not-a-url", "", "30000"));
        String endpoint = extension.aiMarkupPreflightGuidance("");

        extension.setOptionStorageForTests(storage("https://cbss.dila.edu.tw/cbrd/parse", "", "30000"));
        String token = extension.aiMarkupPreflightGuidance("");

        extension.setOptionStorageForTests(storage("https://cbss.dila.edu.tw/cbrd/parse", "token", "30000"));
        String empty = extension.aiMarkupPreflightGuidance("");
        StringBuilder tooLongText = new StringBuilder();
        for (int i = 0; i <= 4000; i++) {
            tooLongText.append('好');
        }
        String tooLong = extension.aiMarkupPreflightGuidance(tooLongText.toString());

        assertThat(java.util.Arrays.asList(endpoint, token, empty, tooLong)).doesNotHaveDuplicates();
    }

    private static WSOptionsStorage storage(String endpointUrl, String token, String timeoutMs) {
        WSOptionsStorage storage = mock(WSOptionsStorage.class);
        when(storage.getOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_API_URL.equals(key)) {
                return endpointUrl;
            }
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TIMEOUT_MS.equals(key)) {
                return timeoutMs;
            }
            return invocation.getArgument(1);
        });
        when(storage.getSecretOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN.equals(key)
                ? token
                : invocation.getArgument(1);
        });
        return storage;
    }
}
