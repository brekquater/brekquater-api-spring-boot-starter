---
title: "BQ-DES-006: Path templates are distinct"
description: "No two paths differ only in the names of their path variables, such as /orders/{id} and /orders/{orderId}."
slug: /rules/BQ-DES-006
rule_id: BQ-DES-006
severity: MAJOR
category: DESIGN
reads: OPENAPI
---

**Checks:** No two paths differ only in the names of their path variables, such as `/orders/{id}` and `/orders/{orderId}`. Both paths are reported.

## Why it matters when you scale

The two paths match exactly the same requests, so they are one resource documented twice. The OpenAPI specification says such paths must not exist, and code generators and gateways handle them unpredictably. In Spring it happens when two controller methods name the same path variable differently.

## Example

Fails:

```yaml
paths:
  /orders/{id}:
    get: ...
  /orders/{orderId}:
    put: ...
```

Passes:

```yaml
paths:
  /orders/{id}:
    get: ...
    put: ...
```

## How to fix it in Spring

Use one name for the variable at each position in every mapping:

```java
@GetMapping("/orders/{id}")
Order find(@PathVariable String id) { ... }

@PutMapping("/orders/{id}")
Order replace(@PathVariable("id") String orderId, @RequestBody Order order) { ... }
```

## How to suppress it

This rule reports paths, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt one, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-006]
```

## Based on

- OpenAPI Specification 3.0.3, [Paths Object](https://spec.openapis.org/oas/v3.0.3#paths-object): "Templated paths with the same hierarchy but different templated names MUST NOT exist as they are identical."
- Redocly rule [`no-identical-paths`](https://redocly.com/docs/cli/rules/oas/no-identical-paths) (MIT).

The wording and the implementation of this rule are original.
