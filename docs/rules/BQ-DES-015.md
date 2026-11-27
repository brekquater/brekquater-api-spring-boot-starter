---
title: "BQ-DES-015: One versioning mechanism"
description: "The API receives its version one way: a path segment, a header or a query parameter."
slug: /rules/BQ-DES-015
rule_id: BQ-DES-015
severity: MINOR
category: DESIGN
reads: OPENAPI
---

**Checks:** Every versioned operation receives its version the same way: a path version segment, a header, or a query parameter whose name ends in `version` (`X-API-Version`, `Accept-Version`, `api-version`). The most common mechanism is taken as the API's own, and operations that use another, or two at once, are reported. Unversioned operations are not checked. A version segment is a literal segment such as `v2`, `v1beta` or `2`; springdoc writes Spring's path-segment versioning that way. A template such as `{version}` is not read. A version in a media-type parameter does not appear in the document, so it is not seen.

## Why it matters when you scale

Clients configure versioning once, in one place. When one operation wants `/v2` and the next `X-API-Version: 2`, every client needs special cases, and Spring itself resolves the version with one strategy, so the hand-written one is never checked.

## Example

Fails:

```yaml
/v1/orders:
  get: {}
/invoices:
  get:
    parameters: [{name: api-version, in: query}]
```

Passes:

```yaml
/v1/orders:
  get: {}
/v1/invoices:
  get: {}
```

## How to fix it in Spring

Choose one strategy and let Spring apply it to every controller:

```yaml
spring:
  mvc:
    apiversion:
      use:
        header: X-API-Version
```

Then remove hand-written `@RequestHeader("X-API-Version")` parameters and `/v1` prefixes that use another mechanism, and declare versions with `@GetMapping(path = "/orders", version = "2")`.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-015"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-015]
```

## Based on

- Spring Framework reference, [API versioning](https://docs.spring.io/spring-framework/reference/web/webmvc-versioning.html): one `ApiVersionStrategy` resolves the version for every request.
- Zimmermann et al., *Patterns for API Design*, Addison-Wesley, 2022, pattern Version Identifier.
- Serbout and Pautasso, "How Are Web APIs Versioned in Practice? A Large-Scale Empirical Study", Journal of Web Engineering 23(4), 2024, [doi:10.13052/jwe1540-9589.2341](https://doi.org/10.13052/jwe1540-9589.2341).

The wording and the implementation of this rule are original.
