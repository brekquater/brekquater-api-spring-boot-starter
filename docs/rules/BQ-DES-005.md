---
title: "BQ-DES-005: 200 response has a body"
description: "A 200 response documents a body. A success with nothing to send is 204 No Content. HEAD and OPTIONS operations are not checked."
slug: /rules/BQ-DES-005
rule_id: BQ-DES-005
severity: MINOR
category: DESIGN
reads: OPENAPI
---

**Checks:** A `200` response documents a body. A success with nothing to send is `204 No Content`. `HEAD` and `OPTIONS` operations are not checked, because they answer with headers.

## Why it matters when you scale

A client that reads `200` looks for a body, and a code generator gives the method a return type. `204` tells both that there is nothing to read. springdoc documents every `void` controller method without `@ResponseStatus` as an empty `200`, so this is easy to get wrong without noticing.

## Example

Fails:

```yaml
/orders/{id}/archive:
  post:
    responses:
      '200': {description: OK}
```

Passes:

```yaml
/orders/{id}/archive:
  post:
    responses:
      '204': {description: No Content}
```

## How to fix it in Spring

Annotate the method with the status it really answers:

```java
@PostMapping("/orders/{id}/archive")
@ResponseStatus(HttpStatus.NO_CONTENT)
void archive(@PathVariable String id) { ... }
```

springdoc reads the status from `@ResponseStatus`, not from the value a method returns, so `return ResponseEntity.noContent().build();` alone is still documented as `200`.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-005"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-005]
```

## Based on

- [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110#section-15.3.1), sections 15.3.1 (200 OK) and 15.3.5 (204 No Content).
- Zalando RESTful API Guidelines, rule [150](https://opensource.zalando.com/restful-api-guidelines/#150) (CC-BY 4.0).
- IBM OpenAPI validator, [ibm-cloud rules](https://github.com/IBM/openapi-validator/blob/main/docs/ibm-cloud-rules.md) (Apache-2.0).
- Kotstein and Bogner (2021), *Which RESTful API Design Rules Are Important and How Do They Improve Software Quality? A Delphi Study with Industry Experts*, [arXiv 2108.00033](https://arxiv.org/abs/2108.00033): the experts rated "204 when the body is intentionally empty" as highly important.

The wording and the implementation of this rule are original.
