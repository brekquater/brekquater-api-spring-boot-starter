---
title: "BQ-DES-010: Format fits the type"
description: "A well-known format, such as date-time or int32, sits on a type it can describe."
slug: /rules/BQ-DES-010
rule_id: BQ-DES-010
severity: MAJOR
category: DESIGN
reads: OPENAPI
---

**Checks:** A well-known `format` matches the schema's `type`. Text formats (`date`, `date-time`, `time`, `duration`, `uuid`, `email`, `uri`, `hostname`, `ipv4`, `ipv6`, `byte`, `binary`, `password`) need `string`. `int32` and `int64` need `integer`, and `float` and `double` need `number`; both also accept `string`, which some APIs use for large numbers. Unknown and custom formats, and schemas without a type, are ignored.

## Why it matters when you scale

A format that contradicts its type has no correct reading. One code generator creates an `Integer` and another a date, so two clients of the same API disagree about what the field holds.

## Example

Fails:

```yaml
createdAt: {type: integer, format: date-time}
```

Passes:

```yaml
createdAt: {type: string, format: date-time}
```

## How to fix it in Spring

Declare the field with the Java type that means what you want, and springdoc writes both correctly:

```java
record Order(OffsetDateTime createdAt) {}
```

If an annotation overrides the type, correct `@Schema(type = ..., format = ...)`.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-010"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers a schema written inline in the operation; one in a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-010]
```

## Based on

- OpenAPI Specification 3.1.1, [data type formats](https://spec.openapis.org/oas/v3.1.1.html#data-type-format).
- IBM OpenAPI validator rule `ibm-schema-type-format`, [ibm-cloud rules](https://github.com/IBM/openapi-validator/blob/main/docs/ibm-cloud-rules.md) (Apache-2.0).

The wording and the implementation of this rule are original.
