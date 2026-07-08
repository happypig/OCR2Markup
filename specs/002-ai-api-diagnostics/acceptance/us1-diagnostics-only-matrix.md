# US1 Diagnostics-Only Matrix

Confirms that running AI Markup diagnostics never mutates stored plugin settings, credentials, the selected model, the endpoint selection, or the outgoing request shape. Diagnostics only read configuration and report classification results.

| Scenario | Trigger | Storage Interaction Observed | Expected Outcome |
|----------|---------|-------------------------------|-------------------|
| Successful AI Markup run | Diagnostics command returns `Result.success` | `WSOptionsStorage.getOption` / `getSecretOption` read only | No `setOption`, `setSecretOption`, `setStringArrayOption`, or `setPersistentObjectOption` call occurs |
| Credential failure | Diagnostics command returns `Result.failure` with `CREDENTIALS` | Configuration read once to build the request | Stored API key, endpoint, and model values are unchanged after the run |
| Endpoint/model failure | Diagnostics command returns `Result.failure` with `ENDPOINT_COMPATIBILITY` or `MODEL_ACCESS` | Configuration read once to build the request | Stored endpoint base URL, chat completions path, and model identifier are unchanged after the run |
| Repeated runs | Diagnostics triggered twice in a row (success then failure, or vice versa) | Configuration re-read each run | Request shape (`MarkupServiceConfiguration`) passed to the diagnostics command matches the currently stored settings on every run, with no residual mutation between runs |

**Verification approach**: inject a mocked `WSOptionsStorage` and a mocked `RunAiMarkupDiagnosticsCommand` into `DAMAWorkspaceAccessPluginExtension`, trigger AI Markup diagnostics, and assert zero interactions with any settings-mutating method on the mock while confirming the configuration passed to the diagnostics command matches the mocked read-only values.
