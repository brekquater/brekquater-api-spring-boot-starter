---
title: "BQ-EVO-002: Parameter did not become required"
description: "No parameter became required, and no required parameter was added."
slug: /rules/BQ-EVO-002
rule_id: BQ-EVO-002
severity: MAJOR
category: DESIGN
reads: BASELINE
---

**Checks:** Compares the current document with the baseline, the OpenAPI document of your last release (see [reports](../reports.md#breaking-changes-the-baseline-document)). Operations are matched by method and path; when no path is the same, one that differs only in path variable names is used, and an operation that several could have become is skipped. Each finding names the operation a change affects. No query, header or cookie parameter that was optional or absent in the baseline is required now. Header names are compared ignoring case. Path parameters are part of the path and are not checked. The rule is skipped when there is no baseline document.

## Why it matters when you scale

Clients that worked with the last release do not send the parameter, so every one of their calls now fails with a 400.

## Example

Fails, when the baseline has:

```yaml
parameters: [{name: size, in: query}]
```

and the current document has:

```yaml
parameters: [{name: size, in: query, required: true}]
```

## How to fix it in Spring

Keep it optional and choose a default on the server:

```java
@GetMapping("/orders")
List<Order> list(@RequestParam(required = false, defaultValue = "20") int size) { ... }
```

If the parameter really must be sent, publish the change as a new major version.

## How to suppress it

Findings about the baseline cannot be suppressed per operation. Refresh the baseline once a change is released on purpose, or turn the rule off.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-EVO-002]
```

## Based on

- [oasdiff](https://github.com/oasdiff/oasdiff) (Apache-2.0) checks `request-parameter-became-required` and `new-required-request-parameter`.
- Zimmermann et al., *Patterns for API Design*, Addison-Wesley, 2022, pattern Semantic Versioning.

The wording and the implementation of this rule are original.
