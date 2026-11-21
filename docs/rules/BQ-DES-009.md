---
title: "BQ-DES-009: Pattern is anchored"
description: "Every schema pattern starts with ^ and ends with $, so clients and the server accept the same values."
slug: /rules/BQ-DES-009
rule_id: BQ-DES-009
severity: MINOR
category: DESIGN
reads: OPENAPI
---

**Checks:** Every schema `pattern` starts with `^` and ends with an unescaped `$`. Named schemas, nested properties, array items and the schemas of parameters and bodies are all checked.

## Why it matters when you scale

Bean Validation's `@Pattern` must match the whole value, so on the server `[A-Z]+` accepts only capital letters. JSON Schema validators, which clients and gateways use, only search for a match, so to them `[A-Z]+` accepts `order A1`. The same annotation means two different rules on the two sides of the API. Anchoring the expression makes both sides agree.

## Example

Fails:

```yaml
status: {type: string, pattern: '[A-Z]+'}
```

Passes:

```yaml
status: {type: string, pattern: '^[A-Z]+$'}
```

## How to fix it in Spring

Add the anchors to the regular expression. The server behaves exactly as before, because Bean Validation already matches the whole value:

```java
record OrderQuery(@Pattern(regexp = "^[A-Z]+$") String status) {}
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-009"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers patterns written inline in the operation; a pattern in a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-009]
```

## Based on

- IBM OpenAPI validator rule `ibm-anchored-patterns`, [ibm-cloud rules](https://github.com/IBM/openapi-validator/blob/main/docs/ibm-cloud-rules.md) (Apache-2.0).
- JSON Schema Validation, [`pattern`](https://json-schema.org/draft/2020-12/json-schema-validation#section-6.3.3): regular expressions are not implicitly anchored.
- Jakarta Bean Validation, [`@Pattern`](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/Pattern.html).

The wording and the implementation of this rule are original.
