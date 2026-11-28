---
title: "BQ-DES-011: Enum values match the type"
description: "Every enum value has the type the schema declares."
slug: /rules/BQ-DES-011
rule_id: BQ-DES-011
severity: MINOR
category: DESIGN
reads: OPENAPI
---

**Checks:** Every `enum` value is of the schema's `type`. A whole number counts as a `number`, and `null` is ignored.

## Why it matters when you scale

A client cannot send the string `"1"` where the schema asks for an integer, so the value can never be used. Generated clients either fail to compile or reject every request.

## Example

Fails:

```yaml
size: {type: integer, enum: ['1', '2']}
```

Passes:

```yaml
size: {type: integer, enum: [1, 2]}
```

## How to fix it in Spring

`@Schema(allowableValues = ...)` always writes strings, so do not use it on a number field. Declare a Java enum, or describe the range with Bean Validation:

```java
record Product(@Min(1) @Max(2) int size) {}
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-011"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers a schema written inline in the operation; one in a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-011]
```

## Based on

- Spectral rule `typed-enum`, [Spectral OpenAPI rules](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules).
- JSON Schema Validation, [`enum`](https://json-schema.org/draft/2020-12/json-schema-validation#section-6.1.2).

The wording and the implementation of this rule are original.
