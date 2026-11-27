---
title: "BQ-NAM-006: Property names differ beyond case"
description: "No two properties of one object differ only in case."
slug: /rules/BQ-NAM-006
rule_id: BQ-NAM-006
severity: MAJOR
category: NAMING
reads: OPENAPI
---

**Checks:** No two properties of the same object are equal when case is ignored, such as `url` and `URL`. The same name in different objects is fine.

## Why it matters when you scale

Many languages, databases and generated clients ignore or normalise case. The two properties then collide: a client fails to compile, or silently keeps only one of the two values.

## Example

Fails:

```yaml
Link:
  type: object
  properties:
    url: {type: string}
    URL: {type: string}
```

Passes:

```yaml
Link:
  type: object
  properties:
    url: {type: string}
    callbackUrl: {type: string}
```

## How to fix it in Spring

Rename one of the fields, or its `@JsonProperty`. A common cause is a field plus a getter that Jackson reads as a second property with different capitals.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-NAM-006"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers a schema written inline in the operation; one in a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-NAM-006]
```

## Based on

- IBM OpenAPI validator rule `ibm-avoid-property-name-collision`, [ibm-cloud rules](https://github.com/IBM/openapi-validator/blob/main/docs/ibm-cloud-rules.md) (Apache-2.0).

The wording and the implementation of this rule are original.
