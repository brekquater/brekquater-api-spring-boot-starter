---
title: "BQ-EVO-009: Sunset did not move earlier"
description: "An x-sunset date does not move earlier than the last release announced."
slug: /rules/BQ-EVO-009
rule_id: BQ-EVO-009
severity: MINOR
category: DESIGN
reads: BASELINE
---

**Checks:** Compares the current document with the baseline, the OpenAPI document of your last release (see [reports](../reports.md#breaking-changes-the-baseline-document)). Operations are matched by method and path, with path variable names ignored. No operation's `x-sunset` date is earlier than the date the baseline gave it. Dates and RFC 3339 date-times are compared by day; a value that is not a date is left to [BQ-DOC-013](BQ-DOC-013.md). The rule is skipped when there is no baseline document.

## Why it matters when you scale

Clients plan their migration around the announced date. Moving it earlier takes away time they were promised.

## Example

Fails, when the baseline has:

```yaml
get: {deprecated: true, x-sunset: '2027-06-30'}
```

and the current document has:

```yaml
get: {deprecated: true, x-sunset: '2027-03-31'}
```

## How to fix it in Spring

Keep the date the last release announced, or move it later:

```java
@Operation(deprecated = true, extensions = @Extension(properties =
        @ExtensionProperty(name = "x-sunset", value = "2027-06-30")))
```

## How to suppress it

Findings about the baseline cannot be suppressed per operation. Refresh the baseline once a change is released on purpose, or turn the rule off.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-EVO-009]
```

## Based on

- [oasdiff](https://github.com/oasdiff/oasdiff) (Apache-2.0) check `api-sunset-date-changed-too-small`.
- RFC 8594, [The Sunset HTTP Header Field](https://www.rfc-editor.org/rfc/rfc8594).

The wording and the implementation of this rule are original.
