---
title: "BQ-SEC-016: HTTP scheme is registered"
description: "Every http security scheme names an authentication scheme from the IANA registry, such as bearer or basic."
slug: /rules/BQ-SEC-016
rule_id: BQ-SEC-016
severity: MINOR
category: SECURITY
reads: OPENAPI
---

**Checks:** Every `http` security scheme names an HTTP authentication scheme from the [IANA registry](https://www.iana.org/assignments/http-authschemes/http-authschemes.xhtml), ignoring case: `Basic`, `Bearer`, `Concealed`, `Digest`, `DPoP`, `GNAP`, `HOBA`, `Mutual`, `Negotiate`, `OAuth`, `PrivateToken`, `SCRAM-SHA-1`, `SCRAM-SHA-256`, `vapid` and the four `iso-kam3-*` schemes, as listed on 2026-11-21.

## Why it matters when you scale

Clients and Swagger UI only know how to send registered schemes. The usual mistake is `scheme: JWT`: JWT is a token format, sent with the `Bearer` scheme, so clients send nothing they can use, and "Try it out" fails.

## Example

Fails:

```yaml
components:
  securitySchemes:
    jwt: {type: http, scheme: JWT}
```

Passes:

```yaml
components:
  securitySchemes:
    jwt: {type: http, scheme: bearer, bearerFormat: JWT}
```

## How to fix it in Spring

Name the scheme `bearer`, and put the token format in `bearerFormat`:

```java
@SecurityScheme(name = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
```

## How to suppress it

This rule reports security schemes, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt one, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-016]
```

## Based on

- OpenAPI Specification 3.1.1, [Security Scheme Object](https://spec.openapis.org/oas/v3.1.1.html#security-scheme-object): "The values used SHOULD be registered in the IANA Authentication Scheme registry."
- IANA, [HTTP Authentication Scheme Registry](https://www.iana.org/assignments/http-authschemes/http-authschemes.xhtml).

The wording and the implementation of this rule are original.
