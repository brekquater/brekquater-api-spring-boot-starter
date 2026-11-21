---
title: "BQ-SEC-015: No Negotiate or OAuth 1.0 scheme"
description: "No http security scheme uses Negotiate or OAuth 1.0."
slug: /rules/BQ-SEC-015
rule_id: BQ-SEC-015
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** No `http` security scheme uses `negotiate` (Kerberos or NTLM through SPNEGO) or `oauth` (OAuth 1.0).

## Why it matters when you scale

Negotiate is built for Windows domains and intranets: it needs the caller to share the server's domain, and NTLM, its fallback, is weak. OAuth 1.0 is superseded by OAuth 2.0 and few clients still support it. Neither suits callers of a public API.

## Example

Fails:

```yaml
components:
  securitySchemes:
    kerberos: {type: http, scheme: negotiate}
```

Passes:

```yaml
components:
  securitySchemes:
    bearer: {type: http, scheme: bearer, bearerFormat: JWT}
```

## How to fix it in Spring

Accept OAuth 2.0 bearer tokens with Spring Security's resource server, and document them:

```java
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
```

## How to suppress it

This rule reports security schemes, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt one, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-015]
```

## Based on

- Spectral OWASP ruleset, `owasp:api2:2023-auth-insecure-schemes` ([ruleset](https://github.com/stoplightio/spectral-owasp-ruleset), MIT).
- OWASP API Security Top 10 2023, [API2: Broken Authentication](https://owasp.org/API-Security/editions/2023/en/0xa2-broken-authentication/) (cited and linked only).

The wording and the implementation of this rule are original.
