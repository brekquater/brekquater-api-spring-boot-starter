---
title: "BQ-DES-012: Enum values are unique"
description: "An enum lists each value once."
slug: /rules/BQ-DES-012
rule_id: BQ-DES-012
severity: MINOR
category: DESIGN
reads: OPENAPI
---

**Checks:** No `enum` lists the same value twice. Values that differ only in case are different.

## Why it matters when you scale

Code generators turn each value into a constant, so a repeat produces two constants with one name. In Spring it usually means two Java constants serialize to the same text, and the server can only ever read one of them back.

## Example

Fails:

```yaml
colour: {type: string, enum: [red, red]}
```

Passes:

```yaml
colour: {type: string, enum: [red, blue]}
```

## How to fix it in Spring

Give each constant its own value. Check `@JsonValue`, `@JsonProperty` and `toString()` on the enum, and `@Schema(allowableValues = ...)` on the field.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-012"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers a schema written inline in the operation; one in a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-012]
```

## Based on

- Spectral rule `duplicated-entry-in-enum`, [Spectral OpenAPI rules](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules).
- JSON Schema Validation, [`enum`](https://json-schema.org/draft/2020-12/json-schema-validation#section-6.1.2): the elements SHOULD be unique.

The wording and the implementation of this rule are original.
