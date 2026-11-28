---
title: "BQ-SEC-003: HTTP Basic is not the only login"
description: "No operation can be called with HTTP Basic alone. Basic offered next to a token scheme is not reported."
slug: /rules/BQ-SEC-003
rule_id: BQ-SEC-003
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** No operation can be called with HTTP Basic alone, whether it declares its own `security` or inherits the document's. An operation that offers Basic next to another scheme, such as a bearer token, is not reported, because callers can choose the stronger one.

## Why it matters when you scale

HTTP Basic sends the user's password itself, encoded but not encrypted, with every request. A password cannot be limited to some operations, cannot expire on its own, and cannot require a second factor. Every client, log and proxy that sees the header sees the password.

## Example

Fails:

```yaml
paths:
  /accounts:
    get:
      security: [{basicAuth: []}]
components:
  securitySchemes:
    basicAuth: {type: http, scheme: basic}
```

Passes:

```yaml
paths:
  /accounts:
    get:
      security: [{bearer: []}]
components:
  securitySchemes:
    bearer: {type: http, scheme: bearer, bearerFormat: JWT}
```

## How to fix it in Spring

Accept tokens instead, for example with Spring Security's OAuth 2.0 resource server (`spring-boot-starter-oauth2-resource-server`), and document the scheme:

```java
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
```

If some clients still need Basic during a migration, offer both: `security: [{basicAuth: []}, {bearer: []}]` passes.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-SEC-003"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-003]
```

## Based on

- Spectral OWASP ruleset, `owasp:api2:2023-no-http-basic` ([ruleset](https://github.com/stoplightio/spectral-owasp-ruleset), MIT).
- OWASP API Security Top 10 2023, [API2: Broken Authentication](https://owasp.org/API-Security/editions/2023/en/0xa2-broken-authentication/) (cited and linked only).

The wording and the implementation of this rule are original.
