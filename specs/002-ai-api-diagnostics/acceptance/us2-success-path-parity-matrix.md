# US2 Success-Path Parity Matrix

Confirms that a successful AI Markup request produces identical outcomes on Windows and macOS, with no warning or diagnostic wording appearing on the success path.

| Scenario | Windows Expectation | macOS Expectation | Shared Outcome |
|----------|---------------------|--------------------|-----------------|
| Successful completion, identical input | `Result.success` with markup content | `Result.success` with markup content | Identical markup result text; no platform-specific wording introduced |
| Result rendering | Replace button visible, export button hidden | Replace button visible, export button hidden | Same UI button state on both platforms |
| No warning drift | No diagnostic/guidance message shown | No diagnostic/guidance message shown | Result area contains only the markup output, never a guidance key, on either platform |
| Repeated success runs | Second run overwrites first with new success content, no residual failure state | Second run overwrites first with new success content, no residual failure state | Session/troubleshooting state from a prior run never leaks into a subsequent successful run on either platform |

**Verification approach**: drive `RunAiMarkupDiagnosticsCommand.execute(...)` with `platform` set to `"windows"` and `"macos"` against a mocked successful `OpenAiCompatibleChatClient`, feed both `Result` objects through `DAMAWorkspaceAccessPluginExtension#completeAiMarkupOperation`, and assert identical rendered markup text and button visibility with no guidance/warning text present.
