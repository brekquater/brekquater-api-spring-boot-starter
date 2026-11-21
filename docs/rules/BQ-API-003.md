---
title: "BQ-API-003: Response content has a schema"
description: "Every media type of every documented response declares a schema."
slug: /rules/BQ-API-003
rule_id: BQ-API-003
severity: MAJOR
category: DESIGN
reads: OPENAPI
---

**Checks:** Every media type of every documented response declares a `schema`.

## Why it matters when you scale

A response body without a schema is a promise with no terms. Clients cannot generate types for it, and you cannot tell later whether a change to the body breaks them, because nothing ever said what the body was.

## Example

Fails:

```yaml
responses:
  '200':
    description: OK
    content:
      application/json: {}
```

Passes:

```yaml
responses:
  '200':
    description: OK
    content:
      application/json:
        schema: {$ref: '#/components/schemas/Order'}
```

## How to fix it in Spring

springdoc writes the schema from the controller method's return type. Return a concrete type rather than `ResponseEntity<?>` or `void` with a hand-written body. When the type cannot be inferred, describe it:

```java
@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Order.class)))
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-API-003"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-API-003]
```

## Based on

- Stoplight Spectral and Zalando RESTful API Guidelines conventions for documented response bodies.

The wording and the implementation of this rule are original.
