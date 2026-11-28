---
title: "BQ-SEC-014: OAuth URLs use https"
description: "Every OAuth flow's authorization, token and refresh URL uses https, except on this machine."
slug: /rules/BQ-SEC-014
rule_id: BQ-SEC-014
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** Every `authorizationUrl`, `tokenUrl` and `refreshUrl` of an OAuth flow uses `https`. Hosts on this machine (`localhost`, `127.0.0.1`, `[::1]`, `*.localhost`) are exempt, for local identity providers such as a development Keycloak.

## Why it matters when you scale

These requests carry authorization codes, client secrets, refresh tokens and access tokens. Over plain `http` anyone on the network path can read them and log in as the user or the client. OAuth 2.0 requires TLS for them.

## Example

Fails:

```yaml
tokenUrl: 'http://sso.example.com/token'
```

Passes:

```yaml
tokenUrl: 'https://sso.example.com/token'
```

## How to fix it in Spring

Point the flow at the identity provider's https endpoints:

```java
@OAuthFlow(authorizationUrl = "https://sso.example.com/authorize", tokenUrl = "https://sso.example.com/token")
```

## How to suppress it

This rule reports security schemes, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt one, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-014]
```

## Based on

- OpenAPI Specification 3.1.1, [OAuth Flow Object](https://spec.openapis.org/oas/v3.1.1.html#oauth-flow-object): "The OAuth2 standard requires the use of TLS."

The wording and the implementation of this rule are original.
