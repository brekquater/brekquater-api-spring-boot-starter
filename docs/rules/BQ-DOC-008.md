---
title: "BQ-DOC-008: Media types are specific"
description: "Every request and response body names the media type it is sent as, not a wildcard such as */*."
slug: /rules/BQ-DOC-008
rule_id: BQ-DOC-008
severity: MINOR
category: DOCUMENTATION
reads: OPENAPI
---

**Checks:** Every request and response body names the media type it is sent as, not a wildcard such as `*/*` or `application/*`.

## Why it matters when you scale

A wildcard tells clients nothing about how to read the body, and code generators fall back to untyped strings or bytes. springdoc documents every response as `*/*` unless it is told which type the API sends, so most Spring APIs fail this rule everywhere until one property is set.

## Example

Fails:

```yaml
'200':
  content:
    '*/*': {schema: {$ref: '#/components/schemas/Order'}}
```

Passes:

```yaml
'200':
  content:
    application/json: {schema: {$ref: '#/components/schemas/Order'}}
```

## How to fix it in Spring

Tell springdoc which type your API sends:

```yaml
springdoc:
  default-produces-media-type: application/json
```

Or declare it on the mapping, which also makes Spring refuse requests that ask for another type:

```java
@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DOC-008"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-008]
```

## Based on

- IBM OpenAPI validator rule `ibm-content-type-is-specific`, [ibm-cloud rules](https://github.com/IBM/openapi-validator/blob/main/docs/ibm-cloud-rules.md) (Apache-2.0).
- [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110#section-8.3), section 8.3 (Content-Type).
- Kotstein and Bogner (2021), *Which RESTful API Design Rules Are Important and How Do They Improve Software Quality? A Delphi Study with Industry Experts*, [arXiv 2108.00033](https://arxiv.org/abs/2108.00033): the experts rated "Content-Type must be used" as highly important.

The wording and the implementation of this rule are original.
