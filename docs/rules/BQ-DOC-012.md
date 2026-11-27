---
title: "BQ-DOC-012: Deprecated operation is explained"
description: "A deprecated operation has a description that says what replaces it."
slug: /rules/BQ-DOC-012
rule_id: BQ-DOC-012
severity: MINOR
category: DOCUMENTATION
reads: OPENAPI
---

**Checks:** Every operation with `deprecated: true` has a non-blank `description`. A summary alone does not count, because the replacement and end date need more than a title.

## Why it matters when you scale

`deprecated: true` tells a client only that something will change. Without the replacement and a date, every client team has to ask, and most will wait until the operation is gone.

## Example

Fails:

```yaml
get: {deprecated: true, summary: List orders}
```

Passes:

```yaml
get:
  deprecated: true
  summary: List orders
  description: Replaced by GET /v2/orders. Works until 2027-06-30.
```

## How to fix it in Spring

```java
@Operation(deprecated = true, description = "Replaced by GET /v2/orders. Works until 2027-06-30.")
@GetMapping("/v1/orders")
List<OrderV1> listV1() { ... }
```

With Spring's header or query-parameter versioning, springdoc 3 merges all versions of a mapping into one operation, and marks it deprecated when any version is. Describe which version is deprecated.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DOC-012"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-012]
```

## Based on

- Zalando RESTful API Guidelines, rule [187](https://opensource.zalando.com/restful-api-guidelines/#187) (CC-BY 4.0): reflect deprecation in the API specification, with the replacement.
- RFC 9745, [The Deprecation HTTP Response Header Field](https://www.rfc-editor.org/rfc/rfc9745).

The wording and the implementation of this rule are original.
