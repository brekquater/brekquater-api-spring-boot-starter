---
title: "BQ-SEC-010: OAuth has no password flow"
description: "No OAuth 2.0 security scheme offers the resource owner password flow, which RFC 9700 says must not be used."
slug: /rules/BQ-SEC-010
rule_id: BQ-SEC-010
severity: BLOCKER
category: SECURITY
reads: OPENAPI
---

**Checks:** No OAuth 2.0 security scheme offers the resource owner password flow (`flows.password`).

## Why it matters when you scale

In the password flow the client application collects the user's password and sends it to the authorization server. Every client then handles passwords, multi-factor authentication cannot work, and a leaked client leaks credentials. The OAuth 2.0 Security Best Current Practice, RFC 9700, says this flow must not be used.

## Example

Fails:

```yaml
components:
  securitySchemes:
    login:
      type: oauth2
      flows:
        password: {tokenUrl: 'https://auth.example.com/token', scopes: {}}
```

Passes:

```yaml
components:
  securitySchemes:
    login:
      type: oauth2
      flows:
        authorizationCode:
          authorizationUrl: 'https://auth.example.com/authorize'
          tokenUrl: 'https://auth.example.com/token'
          scopes: {}
```

## How to fix it in Spring

Offer the authorization code flow (with PKCE) for users, or client credentials for services:

```java
@SecurityScheme(name = "login", type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(authorizationCode = @OAuthFlow(
                authorizationUrl = "https://auth.example.com/authorize",
                tokenUrl = "https://auth.example.com/token")))
```

The authorization server must support the flow too; Spring Authorization Server does not offer the password grant at all.

## How to suppress it

This rule reports security schemes, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt one, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-010]
```

## Based on

- [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700#section-2.4), OAuth 2.0 Security Best Current Practice, section 2.4: the resource owner password credentials grant "MUST NOT be used".

The wording and the implementation of this rule are original.
