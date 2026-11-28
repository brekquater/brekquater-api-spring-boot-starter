---
title: "BQ-SEC-017: Secrets are not returned"
description: "No property named like a password or secret can be read in a response unless it is writeOnly."
slug: /rules/BQ-SEC-017
rule_id: BQ-SEC-017
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** No property named `password`, `passwd`, `pwd`, `secret` or `client_secret` (ignoring case, `_` and `-`) can be read in a response: it must be `writeOnly`. Schemas written inline in a response are checked, and so is every named schema a response refers to, directly or through other schemas. Tokens are not checked, because login and token endpoints return them on purpose.

## Why it matters when you scale

A password or client secret that the API sends back can be read by anyone who can call the endpoint, and ends up in client logs and caches. It usually happens when one class is used both to create an account and to return it.

This rule decides by name, and name-based rules are usually INFO. It is MAJOR because a false positive is rare and takes one annotation to fix, while a returned password is a breach.

## Example

Fails:

```yaml
Account:
  type: object
  properties:
    login: {type: string}
    password: {type: string}
```

Passes:

```yaml
Account:
  type: object
  properties:
    login: {type: string}
    password: {type: string, writeOnly: true}
```

## How to fix it in Spring

Mark the field write-only for Jackson. It is then read from requests, left out of responses, and documented as `writeOnly`:

```java
record Account(String login, @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {}
```

`@Schema(accessMode = Schema.AccessMode.WRITE_ONLY)` alone is not enough: it changes the documentation, but Jackson still sends the field. Better still, use separate classes for what a client sends and what the API returns.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-SEC-017"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers properties written inline in a response; a property of a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-017]
```

## Based on

- OWASP API Security Top 10 2023, [API3: Broken Object Property Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa3-broken-object-property-level-authorization/) (cited and linked only).
- JSON Schema Validation 2020-12, [`writeOnly`](https://json-schema.org/draft/2020-12/json-schema-validation#section-9.4): the value "is never present when the instance is retrieved", but can be sent to create or update it.

The wording and the implementation of this rule are original.
