---
title: "BQ-DOC-002: API declares a contact"
description: "info.contact gives at least one non-blank name, url or email."
slug: /rules/BQ-DOC-002
rule_id: BQ-DOC-002
severity: MINOR
category: DOCUMENTATION
reads: OPENAPI
---

**Checks:** `info.contact` gives at least one non-blank `name`, `url` or `email`.

## Why it matters when you scale

When a client team finds a bug or needs a change, the document is often the only thing they have. Without a contact they open tickets in the wrong place, or build workarounds instead of asking.

## Example

Fails:

```yaml
info:
  title: Orders API
  version: '1.4'
```

Passes:

```yaml
info:
  title: Orders API
  version: '1.4'
  contact: {name: Orders team, email: orders@example.com}
```

## How to fix it in Spring

Declare an `OpenAPI` bean:

```java
@Bean
OpenAPI ordersApi() {
    return new OpenAPI().info(new Info()
            .title("Orders API")
            .version("1.4")
            .contact(new Contact().name("Orders team").email("orders@example.com")));
}
```

## How to suppress it

This rule checks the document as a whole, so it can only be switched off for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-002]
```

## Based on

- Spectral rule [`info-contact`](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules#info-contact).
- Zalando RESTful API Guidelines, rule [218](https://opensource.zalando.com/restful-api-guidelines/#218) (CC-BY 4.0).

The wording and the implementation of this rule are original.
