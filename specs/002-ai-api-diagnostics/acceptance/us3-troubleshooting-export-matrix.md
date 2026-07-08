# US3 Troubleshooting Export Matrix

| Scenario | Expected Record State | Expected Export State |
|----------|-----------------------|-----------------------|
| Failed request captured | Request metadata and sanitized error body preserved | Export file remains redacted |
| Manual support export | Local user chooses export destination | JSON package contains support-triage fields |
| Secret-bearing payload | API keys/tokens removed or masked in record | Export contains no raw credentials |
