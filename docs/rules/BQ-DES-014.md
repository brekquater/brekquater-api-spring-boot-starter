---
title: "BQ-DES-014: Property is typed"
description: "Every property says what it holds."
slug: /rules/BQ-DES-014
rule_id: BQ-DES-014
severity: MINOR
category: DESIGN
reads: OPENAPI
---

**Checks:** Every property has a `type`, a `$ref`, an `enum`, a `const`, `properties`, `items`, `additionalProperties` or a composition. A map with free values, such as `Map<String, Object>`, passes.

## Why it matters when you scale

An untyped property accepts anything, so clients cannot generate a field for it and the API cannot change it safely: nobody knows which shapes callers already depend on.

## Example

Fails:

```yaml
payload: {}
```

Passes:

```yaml
payload: {type: object, additionalProperties: true}
```

## How to fix it in Spring

Replace `Object` with a specific type. If the value really is free-form, say so with a `Map<String, Object>` or `@Schema(type = "object")`:

```java
record Event(Map<String, Object> payload) {}
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-014"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers a schema written inline in the operation; one in a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-014]
```

## Based on

- Azure OpenAPI validator rule `PropertyType`, [azure-openapi-validator](https://github.com/Azure/azure-openapi-validator) (MIT).

The wording and the implementation of this rule are original.
