---
title: "BQ-DOC-009: Schema examples match their schema"
description: "The examples and defaults of a schema are values the schema accepts."
slug: /rules/BQ-DOC-009
rule_id: BQ-DOC-009
severity: MINOR
category: DOCUMENTATION
reads: OPENAPI
---

**Checks:** Every `example`, `examples` entry and `default` of a schema is valid against that schema, as JSON Schema 2020-12 defines it: type, `enum`, limits, lengths, `pattern`, required properties and so on. `format` is not enforced, because JSON Schema treats it as a note. A `oneOf` is satisfied by at least one branch, and a schema that cannot be checked is not reported.

## Why it matters when you scale

The example is the first thing a client developer copies, and mock servers answer with it. A default outside the allowed values tells callers that leaving a field out sends something the API rejects.

## Example

Fails:

```yaml
stock: {type: integer, example: many}
```

Passes:

```yaml
stock: {type: integer, example: 3}
```

## How to fix it in Spring

Correct the annotation, or the field's type and constraints:

```java
record Product(@Schema(example = "3") int stock) {}
```

The check uses the [networknt JSON Schema validator](https://github.com/networknt/json-schema-validator) (Apache-2.0). It only reads the document in memory and never fetches a `$ref`.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DOC-009"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers a value written inline in the operation; one in a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-009]
```

## Based on

- Spectral rule `oas3-valid-schema-example`, [Spectral OpenAPI rules](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules).
- JSON Schema Validation, [`default` and `examples`](https://json-schema.org/draft/2020-12/json-schema-validation#section-9).

The wording and the implementation of this rule are original.
