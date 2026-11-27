---
title: "BQ-EVO-001: Removed operation was deprecated"
description: "An operation of the last release is only removed after it was marked deprecated."
slug: /rules/BQ-EVO-001
rule_id: BQ-EVO-001
severity: MAJOR
category: DESIGN
reads: BASELINE
---

**Checks:** Compares the current document with the baseline, the OpenAPI document of your last release (see [reports](../reports.md#breaking-changes-the-baseline-document)). Operations are matched by method and path; when no path is the same, one that differs only in path variable names is used, and an operation that several could have become is skipped. Each finding names the operation a change affects. Every operation of the baseline still exists, unless the baseline already marked it `deprecated: true`. A renamed path variable or operationId is not a removal. The rule is skipped when there is no baseline document.

## Why it matters when you scale

Clients that still call a removed operation get a 404 with no warning. Deprecating first, for at least one release, gives them the signal and the time to move.

## Example

Fails, when the baseline has:

```yaml
/orders/{id}:
  delete: {responses: {'204': {description: No Content}}}
```

and the current document has:

```yaml
/orders/{id}:
  get: {responses: {'200': {description: OK}}}
```

## How to fix it in Spring

Restore the mapping and deprecate it for at least one release:

```java
@Deprecated
@DeleteMapping("/orders/{id}")
void delete(@PathVariable String id) { ... }
```

Remove it in a later release, once the baseline shows it deprecated.

## How to suppress it

Findings about the baseline cannot be suppressed per operation. Refresh the baseline once a change is released on purpose, or turn the rule off.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-EVO-001]
```

## Based on

- [oasdiff](https://github.com/oasdiff/oasdiff) (Apache-2.0) check `api-removed-without-deprecation`.
- Zalando RESTful API Guidelines, rule [187](https://opensource.zalando.com/restful-api-guidelines/#187) (CC-BY 4.0): deprecate before removing.

The wording and the implementation of this rule are original.
