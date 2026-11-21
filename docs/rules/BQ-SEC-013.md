---
title: "BQ-SEC-013: OAuth scopes are declared"
description: "Every scope an operation requires of an OAuth 2.0 scheme is declared by one of that scheme's flows."
slug: /rules/BQ-SEC-013
rule_id: BQ-SEC-013
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** Every scope an operation, or the whole document, requires of an OAuth 2.0 scheme is declared by one of that scheme's flows. OpenID Connect schemes are not checked: their scopes are published by the identity provider, and the starter makes no network calls.

## Why it matters when you scale

An undeclared scope is usually a typo, such as `ordres:write`. A token can never carry it, so either every caller is rejected, or the check in the code is not the one the document describes. Swagger UI also cannot ask for it when a user logs in.

## Example

Fails:

```yaml
paths:
  /orders:
    post:
      security: [{login: [ordres:write]}]
components:
  securitySchemes:
    login:
      type: oauth2
      flows:
        clientCredentials:
          tokenUrl: 'https://sso.example.com/token'
          scopes: {orders:write: Change orders}
```

Passes:

```yaml
paths:
  /orders:
    post:
      security: [{login: [orders:write]}]
```

## How to fix it in Spring

Fix the name in the requirement, or declare the scope on the flow:

```java
@SecurityRequirement(name = "login", scopes = "orders:write")

@OAuthFlow(tokenUrl = "https://sso.example.com/token",
        scopes = @OAuthScope(name = "orders:write", description = "Change orders"))
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-SEC-013"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). A finding on the document-wide `security` list can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-013]
```

## Based on

- OpenAPI Specification 3.1.1, [Security Requirement Object](https://spec.openapis.org/oas/v3.1.1.html#security-requirement-object) and [OAuth Flow Object](https://spec.openapis.org/oas/v3.1.1.html#oauth-flow-object).

The wording and the implementation of this rule are original.
