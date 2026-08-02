package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.preferences.DAMAOptionPagePluginExtension;
import org.junit.Before;
import org.junit.Test;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T035 — obsolete OpenAI values are ignored after upgrade, never migrated into the new fields
 * (FR-004, FR-005, US2 scenarios 2-3).
 */
public class UpgradeNoMigrationTest {

    private DAMAWorkspaceAccessPluginExtension extension;
    private final List<String> keysRead = new ArrayList<>();

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
        keysRead.clear();
    }

    @Test
    public void configurationIsBuiltEntirelyFromCbrdParseKeys() {
        extension.setOptionStorageForTests(storageWithLegacyValues());

        CbrdParseConfiguration configuration = extension.buildAiMarkupConfiguration();

        assertThat(configuration.getEndpointUrl()).isEqualTo("https://cbss.dila.edu.tw/cbrd/parse");
        assertThat(configuration.getSharedToken()).isEqualTo("shared-token-9876");
        assertThat(configuration.getTimeoutMs()).isEqualTo(30000);
    }

    @Test
    public void noObsoleteOpenAiKeyIsEverRead() {
        extension.setOptionStorageForTests(recordingStorage());

        extension.buildAiMarkupConfiguration();

        assertThat(keysRead).doesNotContain(
            DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_KEY,
            DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_BASE_URL,
            DAMAOptionPagePluginExtension.KEY_DILA_DAMA_CHAT_COMPLETIONS_PATH,
            DAMAOptionPagePluginExtension.KEY_DILA_DAMA_FT_PARSE_MODEL,
            DAMAOptionPagePluginExtension.KEY_DILA_DAMA_FT_DETECT_MODEL,
            DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_TIMEOUT_MS
        );
    }

    @Test
    public void onlyTheThreeCbrdParseKeysAreRead() {
        extension.setOptionStorageForTests(recordingStorage());

        extension.buildAiMarkupConfiguration();

        assertThat(keysRead).containsExactlyInAnyOrder(
            DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_API_URL,
            DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN,
            DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TIMEOUT_MS
        );
    }

    @Test
    public void anOldApiKeyIsNeverAdoptedAsTheParseToken() {
        WSOptionsStorage storage = mock(WSOptionsStorage.class);
        when(storage.getOption(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        when(storage.getSecretOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            // Only the legacy key has a value; the new one was never entered.
            return DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_KEY.equals(key)
                ? "sk-legacy-key"
                : invocation.getArgument(1);
        });
        extension.setOptionStorageForTests(storage);

        CbrdParseConfiguration configuration = extension.buildAiMarkupConfiguration();

        assertThat(configuration.hasSharedToken()).isFalse();
        assertThat(configuration.getSharedToken()).isNotEqualTo("sk-legacy-key");
    }

    @Test
    public void anOldBaseUrlIsNeverAdoptedAsTheParseEndpoint() {
        WSOptionsStorage storage = mock(WSOptionsStorage.class);
        when(storage.getOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_BASE_URL.equals(key)
                ? "https://api.openai.com"
                : invocation.getArgument(1);
        });
        when(storage.getSecretOption(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        extension.setOptionStorageForTests(storage);

        assertThat(extension.buildAiMarkupConfiguration().getEndpointUrl())
            .isEqualTo("https://cbss.dila.edu.tw/cbrd/parse");
    }

    private WSOptionsStorage recordingStorage() {
        WSOptionsStorage storage = mock(WSOptionsStorage.class);
        when(storage.getOption(anyString(), anyString())).thenAnswer(invocation -> {
            keysRead.add(invocation.getArgument(0));
            return invocation.getArgument(1);
        });
        when(storage.getSecretOption(anyString(), anyString())).thenAnswer(invocation -> {
            keysRead.add(invocation.getArgument(0));
            return invocation.getArgument(1);
        });
        return storage;
    }

    private WSOptionsStorage storageWithLegacyValues() {
        WSOptionsStorage storage = mock(WSOptionsStorage.class);
        when(storage.getOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_API_URL.equals(key)) {
                return "https://cbss.dila.edu.tw/cbrd/parse";
            }
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TIMEOUT_MS.equals(key)) {
                return "30000";
            }
            if (DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_BASE_URL.equals(key)) {
                return "https://api.openai.com";
            }
            return invocation.getArgument(1);
        });
        when(storage.getSecretOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN.equals(key)) {
                return "shared-token-9876";
            }
            if (DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_KEY.equals(key)) {
                return "sk-legacy-key";
            }
            return invocation.getArgument(1);
        });
        return storage;
    }
}
