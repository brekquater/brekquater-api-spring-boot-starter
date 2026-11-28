---
title: "BQ-SEC-005: No credentials in the URL"
description: "No path or query parameter is named like a password, secret, token or API key."
slug: /rules/BQ-SEC-005
rule_id: BQ-SEC-005
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** No path or query parameter is named like a credential: `password`, `passwd`, `pwd`, `secret`, `client_secret`, `token`, `access_token`, `refresh_token`, `id_token` or `api_key`. Names are compared exactly after removing case, `_` and `-`, so `API-Key` matches, but `pageToken`, a pagination cursor, does not.

## Why it matters when you scale

Anything in the URL is logged by servers and proxies and kept in browser history. The OAuth 2.0 Security Best Current Practice says access tokens must not be passed in a query parameter.

## Example

Fails:

```yaml
/accounts:
  get:
    parameters:
      - {name: access_token, in: query}
```

Passes:

```yaml
/accounts:
  get:
    security: [{bearer: []}]
```

## How to fix it in Spring

Send tokens in the `Authorization` header, and let Spring Security read them; send other secrets in the request body:

```java
@PostMapping("/password-resets")
void reset(@RequestBody PasswordReset reset) { ... }
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-SEC-005"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-005]
```

## Based on

- [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700#section-4.3.2), section 4.3.2: clients "MUST NOT pass access tokens in a URI query parameter".
- Spectral OWASP ruleset, `owasp:api2:2023-no-credentials-in-url` ([ruleset](https://github.com/stoplightio/spectral-owasp-ruleset), MIT).

The wording and the implementation of this rule are original.
