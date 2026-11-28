---
title: "BQ-EVO-005: Request value was not removed"
description: "Every value a client could send in the last release is still accepted."
slug: /rules/BQ-EVO-005
rule_id: BQ-EVO-005
severity: MAJOR
category: DESIGN
reads: BASELINE
---

**Checks:** Compares the current document with the baseline, the OpenAPI document of your last release (see [reports](../reports.md#breaking-changes-the-baseline-document)). Operations are matched by method and path; when no path is the same, one that differs only in path variable names is used, and an operation that several could have become is skipped. Each finding names the operation a change affects. No value of a request parameter's or JSON request property's `enum` in the baseline is missing from the same enum now. Dropping the enum altogether accepts more, so it is not reported. The rule is skipped when there is no baseline document.

## Why it matters when you scale

A value taken out of an enum turns requests that worked into errors, often for the clients that adopted the value first.

## Example

Fails, when the baseline has:

```yaml
parameters: [{name: format, in: query, schema: {type: string, enum: [PDF, CSV, XLS]}}]
```

and the current document has:

```yaml
parameters: [{name: format, in: query, schema: {type: string, enum: [PDF, CSV]}}]
```

## How to fix it in Spring

Keep the Java enum constant, marked `@Deprecated`, and map it to its replacement on the server until the next major version.

## How to suppress it

Findings about the baseline cannot be suppressed per operation. Refresh the baseline once a change is released on purpose, or turn the rule off.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-EVO-005]
```

## Based on

- [oasdiff](https://github.com/oasdiff/oasdiff) (Apache-2.0) checks `request-parameter-enum-value-removed` and `request-property-enum-value-removed`.

The wording and the implementation of this rule are original.
