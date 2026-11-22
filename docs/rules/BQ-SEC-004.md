---
title: "BQ-SEC-004: API key is not in the query string"
description: "No apiKey security scheme sends the key in the query string."
slug: /rules/BQ-SEC-004
rule_id: BQ-SEC-004
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** No `apiKey` security scheme sends the key in the query string (`in: query`).

## Why it matters when you scale

A URL is written to server and proxy access logs, kept in browser history and sent on in the `Referer` header. A key in the query string ends up in all of those places, readable by anyone with access to them.

## Example

Fails:

```yaml
components:
  securitySchemes:
    key: {type: apiKey, in: query, name: api_key}
```

Passes:

```yaml
components:
  securitySchemes:
    key: {type: apiKey, in: header, name: X-API-Key}
```

## How to fix it in Spring

Read the key from a header, and document it there:

```java
@SecurityScheme(name = "key", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-API-Key")
```

## How to suppress it

This rule reports security schemes, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt one, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-004]
```

## Based on

- Spectral OWASP ruleset, `owasp:api2:2023-no-api-keys-in-url` ([ruleset](https://github.com/stoplightio/spectral-owasp-ruleset), MIT).
- [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700#section-4.3.2), section 4.3.2: credentials in URLs end up in browser history.
- OWASP API Security Top 10 2023, [API2: Broken Authentication](https://owasp.org/API-Security/editions/2023/en/0xa2-broken-authentication/) (cited and linked only).

The wording and the implementation of this rule are original.
