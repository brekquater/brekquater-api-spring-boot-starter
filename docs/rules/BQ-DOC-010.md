---
title: "BQ-DOC-010: Examples match their schema"
description: "Examples of parameters and JSON bodies are values their schema accepts."
slug: /rules/BQ-DOC-010
rule_id: BQ-DOC-010
severity: MINOR
category: DOCUMENTATION
reads: OPENAPI
---

**Checks:** Every `example` and `examples.*.value` of a parameter, and of a JSON request or response body, is valid against its schema. A parameter example written as text, such as `'20'` for an integer, is accepted, and a list written in a parameter's own style, such as `a,b`, is skipped. Bodies that are not JSON are skipped. A required `readOnly` or `writeOnly` property may be left out. `format` is not enforced.

## Why it matters when you scale

Swagger UI pre-fills requests with these examples. A wrong one makes "Try it out" fail for every new developer, and teaches client authors a shape the API does not return.

## Example

Fails:

```yaml
parameters:
  - {name: page, in: query, schema: {type: integer}, example: first}
```

Passes:

```yaml
parameters:
  - {name: page, in: query, schema: {type: integer}, example: 1}
```

## How to fix it in Spring

Correct `@Parameter(example = ...)` or `@ExampleObject(value = ...)` so it matches what the API accepts or returns:

```java
@Parameter(example = "1") @RequestParam Integer page
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DOC-010"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers an example written inline in the operation; one in a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-010]
```

## Based on

- Spectral rule `oas3-valid-media-example`, [Spectral OpenAPI rules](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules).
- OpenAPI Specification 3.1.1, [Media Type Object](https://spec.openapis.org/oas/v3.1.1.html#media-type-object): examples SHOULD match the schema.

The wording and the implementation of this rule are original.
