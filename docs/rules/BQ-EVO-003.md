---
title: "BQ-EVO-003: Request did not become stricter"
description: "The request body and its properties did not become required."
slug: /rules/BQ-EVO-003
rule_id: BQ-EVO-003
severity: MAJOR
category: DESIGN
reads: BASELINE
---

**Checks:** Compares the current document with the baseline, the OpenAPI document of your last release (see [reports](../reports.md#breaking-changes-the-baseline-document)). Operations are matched by method and path; when no path is the same, one that differs only in path variable names is used, and an operation that several could have become is skipped. Each finding names the operation a change affects. The request body did not become required, and no JSON request property became required or was added as required. Shared schemas, nested objects, arrays and `allOf` branches are followed; a change in a shared schema is reported on every operation that uses it, with where the schema is. A required `readOnly` property is not reported, because clients never send it. The rule is skipped when there is no baseline document.

## Why it matters when you scale

Clients built for the last release leave the body or the property out, and their requests are now rejected.

## Example

Fails, when the baseline has:

```yaml
NewOrder:
  type: object
  properties: {item: {type: string}, region: {type: string}}
  required: [item]
```

and the current document has:

```yaml
NewOrder:
  type: object
  properties: {item: {type: string}, region: {type: string}}
  required: [item, region]
```

## How to fix it in Spring

Remove the new `@NotNull`, `@NotBlank` or `@Schema(requiredMode = REQUIRED)` from the field and apply a default on the server, or publish the change as a new major version.

## How to suppress it

Findings about the baseline cannot be suppressed per operation. Refresh the baseline once a change is released on purpose, or turn the rule off.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-EVO-003]
```

## Based on

- [oasdiff](https://github.com/oasdiff/oasdiff) (Apache-2.0) checks `request-body-became-required`, `new-required-request-property` and `request-property-became-required`.

The wording and the implementation of this rule are original.
