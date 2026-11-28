---
title: "BQ-SEC-012: OAuth has no implicit flow"
description: "No OAuth 2.0 security scheme offers the implicit flow, which RFC 9700 says clients should not use."
slug: /rules/BQ-SEC-012
rule_id: BQ-SEC-012
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** No OAuth 2.0 security scheme offers the implicit flow (`flows.implicit`).

## Why it matters when you scale

The implicit flow returns the access token in the redirect URL. There it can leak through browser history and `Referer` headers, or be swapped for an attacker's token, and the client cannot prove it was the one that asked. The OAuth 2.0 Security Best Current Practice says clients should not use it. Swagger UI setups often add it for "Try it out"; the authorization code flow with PKCE does the same job safely.

## Example

Fails:

```yaml
components:
  securitySchemes:
    sso:
      type: oauth2
      flows:
        implicit:
          authorizationUrl: 'https://sso.example.com/authorize'
          scopes: {}
```

Passes:

```yaml
components:
  securitySchemes:
    sso:
      type: oauth2
      flows:
        authorizationCode:
          authorizationUrl: 'https://sso.example.com/authorize'
          tokenUrl: 'https://sso.example.com/token'
          scopes: {}
```

## How to fix it in Spring

Offer the authorization code flow, and turn on PKCE for Swagger UI:

```java
@SecurityScheme(name = "sso", type = SecuritySchemeType.OAUTH2, flows = @OAuthFlows(
        authorizationCode = @OAuthFlow(
                authorizationUrl = "https://sso.example.com/authorize",
                tokenUrl = "https://sso.example.com/token")))
```

```yaml
springdoc:
  swagger-ui:
    oauth:
      use-pkce-with-authorization-code-grant: true
```

## How to suppress it

This rule reports security schemes, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt one, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-012]
```

## Based on

- [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700#section-2.1.2), OAuth 2.0 Security Best Current Practice, section 2.1.2: clients "SHOULD NOT use the implicit grant".

The wording and the implementation of this rule are original.
