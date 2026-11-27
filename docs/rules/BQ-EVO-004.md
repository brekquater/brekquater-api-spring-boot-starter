---
title: "BQ-EVO-004: Response property was not removed"
description: "A property that every response had in the last release is still returned."
slug: /rules/BQ-EVO-004
rule_id: BQ-EVO-004
severity: MAJOR
category: DESIGN
reads: BASELINE
---

**Checks:** Compares the current document with the baseline, the OpenAPI document of your last release (see [reports](../reports.md#breaking-changes-the-baseline-document)). Operations are matched by method and path, with path variable names ignored. Every property the baseline lists as `required` in a success response is still a property of that response. Shared schemas, nested objects, arrays and `allOf` branches are followed. A `writeOnly` property is not reported, because it was never returned. The rule is skipped when there is no baseline document.

## Why it matters when you scale

Required means clients can read the property without checking for it. Taking it away turns that code into a null pointer or a crash in every client.

## Example

Fails, when the baseline has:

```yaml
Order:
  type: object
  properties: {id: {type: string}, total: {type: number}}
  required: [id, total]
```

and the current document has:

```yaml
Order:
  type: object
  properties: {id: {type: string}}
  required: [id]
```

## How to fix it in Spring

Keep the field until the next major version, and mark it deprecated so clients stop using it:

```java
record Order(String id, @Deprecated @Schema(deprecated = true) BigDecimal total) {}
```

## How to suppress it

Findings about the baseline cannot be suppressed per operation. Refresh the baseline once a change is released on purpose, or turn the rule off.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-EVO-004]
```

## Based on

- [oasdiff](https://github.com/oasdiff/oasdiff) (Apache-2.0) check `response-required-property-removed`.

The wording and the implementation of this rule are original.
