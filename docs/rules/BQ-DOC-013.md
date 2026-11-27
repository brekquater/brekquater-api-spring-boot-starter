---
title: "BQ-DOC-013: Sunset is a date"
description: "An x-sunset extension holds a date or an RFC 3339 date-time."
slug: /rules/BQ-DOC-013
rule_id: BQ-DOC-013
severity: MINOR
category: DOCUMENTATION
reads: OPENAPI
---

**Checks:** Every `x-sunset` extension on an operation is a date such as `2027-06-30` or an RFC 3339 date-time such as `2027-06-30T00:00:00Z`. Operations without one are not checked.

## Why it matters when you scale

`x-sunset` tells clients and tools when an operation may be removed, and tools such as oasdiff read it to warn about removals. A value like `soon` or `Q3` is skipped by those tools, so the warning never reaches anyone.

## Example

Fails:

```yaml
get: {deprecated: true, x-sunset: soon}
```

Passes:

```yaml
get: {deprecated: true, x-sunset: '2027-06-30'}
```

## How to fix it in Spring

```java
@Operation(deprecated = true, extensions = @Extension(properties =
        @ExtensionProperty(name = "x-sunset", value = "2027-06-30")))
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DOC-013"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-013]
```

## Based on

- [oasdiff deprecation](https://github.com/oasdiff/oasdiff/blob/main/docs/DEPRECATION.md) (Apache-2.0): `x-sunset` holds the date after which a deprecated operation may be removed.
- RFC 8594, [The Sunset HTTP Header Field](https://www.rfc-editor.org/rfc/rfc8594).

The wording and the implementation of this rule are original.
