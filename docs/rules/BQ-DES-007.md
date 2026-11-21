---
title: "BQ-DES-007: No-content responses have no body"
description: "A 204, 205 or 304 response documents no body, because HTTP sends none with these status codes."
slug: /rules/BQ-DES-007
rule_id: BQ-DES-007
severity: MAJOR
category: DESIGN
reads: OPENAPI
---

**Checks:** A `204`, `205` or `304` response documents no body. HTTP sends no body with these status codes.

## Why it matters when you scale

A documented body on these codes never arrives: clients and proxies stop reading after the headers. Either the status code is wrong and callers miss the data, or the documentation is wrong and generated clients expect data that is not there. In Spring it happens when `@ResponseStatus(HttpStatus.NO_CONTENT)` sits on a method that returns an object.

## Example

Fails:

```yaml
/orders/{id}:
  delete:
    responses:
      '204':
        description: No Content
        content:
          '*/*': {schema: {$ref: '#/components/schemas/Order'}}
```

Passes:

```yaml
/orders/{id}:
  delete:
    responses:
      '204': {description: No Content}
```

## How to fix it in Spring

Return nothing from a method that answers `204`:

```java
@DeleteMapping("/orders/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
void cancel(@PathVariable String id) { ... }
```

If the caller needs the object, keep the return value and remove `@ResponseStatus(HttpStatus.NO_CONTENT)`, so the method answers `200`.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-007"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-007]
```

## Based on

- [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110#section-15.3.5), sections 15.3.5 (204), 15.3.6 (205) and 15.4.5 (304).
- IBM OpenAPI validator, [ibm-cloud rules](https://github.com/IBM/openapi-validator/blob/main/docs/ibm-cloud-rules.md) (Apache-2.0).

The wording and the implementation of this rule are original.
