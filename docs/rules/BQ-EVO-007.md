---
title: "BQ-EVO-007: Type did not change"
description: "No parameter, body or property changed to an incompatible type."
slug: /rules/BQ-EVO-007
rule_id: BQ-EVO-007
severity: MAJOR
category: DESIGN
reads: BASELINE
---

**Checks:** Compares the current document with the baseline, the OpenAPI document of your last release (see [reports](../reports.md#breaking-changes-the-baseline-document)). Operations are matched by method and path, with path variable names ignored. No parameter, JSON body or property has a type that shares nothing with its type in the baseline. `integer` and `number` may replace each other, adding `null` is not a change, and a schema without a declared type is not compared. The rule is skipped when there is no baseline document.

## Why it matters when you scale

A string that becomes an integer, or an object that becomes an array, fails to parse in every client that was generated or written for the old type.

## Example

Fails, when the baseline has:

```yaml
tag: {type: string}
```

and the current document has:

```yaml
tag: {type: array, items: {type: string}}
```

## How to fix it in Spring

Keep the old field or parameter with its old type, and add a new one with the new type. Remove the old one in the next major version.

## How to suppress it

Findings about the baseline cannot be suppressed per operation. Refresh the baseline once a change is released on purpose, or turn the rule off.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-EVO-007]
```

## Based on

- [oasdiff](https://github.com/oasdiff/oasdiff) (Apache-2.0) checks `request-parameter-type-changed`, `request-property-type-changed` and `response-property-type-changed`.

The wording and the implementation of this rule are original.
