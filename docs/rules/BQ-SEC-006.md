---
title: "BQ-SEC-006: Servers use https"
description: "Every declared server that is not on this machine is reached over https. The server springdoc builds from the request is not checked."
slug: /rules/BQ-SEC-006
rule_id: BQ-SEC-006
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** Every server in the document's `servers` list that is not on this machine (`localhost`, `127.0.0.1`, `[::1]`, `*.localhost`) uses `https`. Relative URLs and URLs with variables are not checked. The entry springdoc adds itself, described as `Generated server url`, is skipped: it records how you fetched the document, not where the API is served.

## Why it matters when you scale

Over plain `http`, tokens, passwords and data can be read and changed by anyone on the network path. A documented `http` server also sends Swagger UI's "Try it out" and generated clients there.

## Example

Fails:

```yaml
servers:
  - {url: 'http://api.example.com', description: Production}
```

Passes:

```yaml
servers:
  - {url: 'https://api.example.com', description: Production}
```

## How to fix it in Spring

Declare the https address in your `OpenAPI` bean or with `@Server`:

```java
@Bean
OpenAPI ordersApi() {
    return new OpenAPI().addServersItem(new Server().url("https://api.example.com").description("Production"));
}
```

## How to suppress it

This rule reports servers, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt one, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-006]
```

## Based on

- Spectral OWASP ruleset, `owasp:api8:2023-no-server-http` ([ruleset](https://github.com/stoplightio/spectral-owasp-ruleset), MIT).
- OWASP API Security Top 10 2023, [API8: Security Misconfiguration](https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/) (cited and linked only).

The wording and the implementation of this rule are original.
