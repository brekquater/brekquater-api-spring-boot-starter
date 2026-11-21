---
title: "BQ-DES-008: PATCH uses a patch media type"
description: "A PATCH request body is sent as application/merge-patch+json or application/json-patch+json, which say how missing and null fields are applied."
slug: /rules/BQ-DES-008
rule_id: BQ-DES-008
severity: MINOR
category: DESIGN
reads: OPENAPI
---

**Checks:** A `PATCH` request body is sent as `application/merge-patch+json` (JSON Merge Patch) or `application/json-patch+json` (JSON Patch).

## Why it matters when you scale

A partial update must say what a missing field and a `null` field mean: keep the old value, or clear it? Plain `application/json` does not say, so every endpoint answers differently. JSON Merge Patch (RFC 7396) defines it: a missing field is kept and `null` removes the value. JSON Patch (RFC 6902) sends an explicit list of operations instead.

## Example

Fails:

```yaml
/orders/{id}:
  patch:
    requestBody:
      content:
        application/json: {schema: {$ref: '#/components/schemas/Order'}}
```

Passes:

```yaml
/orders/{id}:
  patch:
    requestBody:
      content:
        application/merge-patch+json: {schema: {$ref: '#/components/schemas/OrderChange'}}
```

## How to fix it in Spring

Declare the media type on the mapping. Spring's JSON converter reads `application/merge-patch+json` into the same types as `application/json`:

```java
@PatchMapping(path = "/{id}", consumes = "application/merge-patch+json")
Order change(@PathVariable String id, @RequestBody OrderChange change) { ... }
```

Give `OrderChange` only the fields a client may change, and treat a field that was not sent as "keep the current value". A request sent as plain `application/json` is then refused with `415 Unsupported Media Type`, so tell your clients before you switch.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-008"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-008]
```

## Based on

- [RFC 5789](https://www.rfc-editor.org/rfc/rfc5789) (PATCH), [RFC 7396](https://www.rfc-editor.org/rfc/rfc7396) (JSON Merge Patch) and [RFC 6902](https://www.rfc-editor.org/rfc/rfc6902) (JSON Patch).
- Zalando RESTful API Guidelines, rule [148](https://opensource.zalando.com/restful-api-guidelines/#148) (CC-BY 4.0).

The wording and the implementation of this rule are original.
