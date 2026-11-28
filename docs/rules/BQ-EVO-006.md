---
title: "BQ-EVO-006: Success status was not removed"
description: "Every success status code of the last release is still documented."
slug: /rules/BQ-EVO-006
rule_id: BQ-EVO-006
severity: MAJOR
category: DESIGN
reads: BASELINE
---

**Checks:** Compares the current document with the baseline, the OpenAPI document of your last release (see [reports](../reports.md#breaking-changes-the-baseline-document)). Operations are matched by method and path; when no path is the same, one that differs only in path variable names is used, and an operation that several could have become is skipped. Each finding names the operation a change affects. Every 2xx status code of each baseline operation is still documented, either itself or through `2XX`. The rule is skipped when there is no baseline document.

## Why it matters when you scale

Clients branch on the status code. If a `201 Created` becomes a `200 OK`, code that checks for 201 takes its error path on every successful call.

## Example

Fails, when the baseline has:

```yaml
post: {responses: {'201': {description: Created}}}
```

and the current document has:

```yaml
post: {responses: {'200': {description: OK}}}
```

## How to fix it in Spring

Return the status the last release documented, for example with `@ResponseStatus(HttpStatus.CREATED)` or `ResponseEntity.created(location)`. If both must exist while clients move, document both with two `@ApiResponse` annotations.

## How to suppress it

Findings about the baseline cannot be suppressed per operation. Refresh the baseline once a change is released on purpose, or turn the rule off.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-EVO-006]
```

## Based on

- [oasdiff](https://github.com/oasdiff/oasdiff) (Apache-2.0) check `response-success-status-removed`.

The wording and the implementation of this rule are original.
