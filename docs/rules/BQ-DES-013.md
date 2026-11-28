---
title: "BQ-DES-013: Discriminator property is required"
description: "The property a discriminator names is defined and required."
slug: /rules/BQ-DES-013
rule_id: BQ-DES-013
severity: MINOR
category: DESIGN
reads: OPENAPI
---

**Checks:** The property named by `discriminator.propertyName` is defined, and required in the schema itself, in an `allOf` branch, or in every `anyOf` or `oneOf` branch. Schemas that refer outside the document are not judged.

## Why it matters when you scale

A discriminator tells a client which variant it received. If the property can be missing, or no schema defines it, the client cannot choose a type and deserialization fails on real data.

## Example

Fails:

```yaml
Shape:
  type: object
  discriminator: {propertyName: kind}
```

Passes:

```yaml
Shape:
  type: object
  properties: {kind: {type: string}}
  required: [kind]
  discriminator: {propertyName: kind}
```

## How to fix it in Spring

Let Jackson and springdoc declare it together:

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(@JsonSubTypes.Type(value = Circle.class, name = "circle"))
interface Shape {}
```

If you write `@Schema(discriminatorProperty = ...)` by hand, make sure the class has that field.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-013"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers a schema written inline in the operation; one in a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-013]
```

## Based on

- OpenAPI Specification 3.1.1, [Discriminator Object](https://spec.openapis.org/oas/v3.1.1.html#discriminator-object).
- IBM OpenAPI validator rule `ibm-discriminator-property`, [ibm-cloud rules](https://github.com/IBM/openapi-validator/blob/main/docs/ibm-cloud-rules.md) (Apache-2.0).

The wording and the implementation of this rule are original.
