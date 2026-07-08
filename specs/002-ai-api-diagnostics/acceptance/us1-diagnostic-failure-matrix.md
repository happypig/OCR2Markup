# US1 Diagnostic Failure Matrix

| Scenario | Input Condition | Expected Category | Expected Guidance |
|----------|-----------------|------------------|------------------|
| Missing API key | Empty credential | `CREDENTIALS` | User is told to verify API key configuration |
| Invalid model | Unknown model name | `MODEL_ACCESS` | User is told to verify model access |
| Malformed payload | Unsupported request field/shape | `MALFORMED_REQUEST` | User is told request format is incompatible |
| Endpoint mismatch | Wrong chat completions path | `ENDPOINT_COMPATIBILITY` | User is told to verify endpoint/path compatibility |
| Proxy interference | Request blocked before service | `CONNECTIVITY_OR_PROXY` | User is told to check proxy/network routing |
