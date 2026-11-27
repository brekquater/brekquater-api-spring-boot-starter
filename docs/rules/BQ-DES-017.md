---
title: "BQ-DES-017: Older versions are deprecated"
description: "When paths carry several major versions, the older versions' operations are deprecated."
slug: /rules/BQ-DES-017
rule_id: BQ-DES-017
severity: INFO
category: DESIGN
reads: OPENAPI
---

**Checks:** When path version segments name more than one major version, every operation under a major older than the newest is `deprecated: true`. An API with a single version left is not checked. A version segment is a literal segment such as `v2`, `v1beta` or `2`; springdoc writes Spring's path-segment versioning that way. A template such as `{version}` is not read. INFO findings are shown but never counted in the score.

## Why it matters when you scale

A deprecation flag is how clients and their tools learn that a version will go away. Without it, the newest version exists only as an option, and the old one has no announced end. This is INFO because running two versions side by side, with neither deprecated, is a legitimate choice for some APIs.

## Example

Fails:

```yaml
/v1/orders:
  get: {}
/v2/orders:
  get: {}
```

Passes:

```yaml
/v1/orders:
  get: {deprecated: true}
/v2/orders:
  get: {}
```

## How to fix it in Spring

Mark the old handlers. springdoc reads `@Deprecated`:

```java
@Deprecated
@GetMapping("/v1/orders")
List<OrderV1> listV1() { ... }
```

Spring can also send `Deprecation` and `Sunset` response headers for a whole version, with `ApiVersionConfigurer.setDeprecationHandler(new StandardApiVersionDeprecationHandler())`.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-017"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-017]
```

## Based on

- Google API Improvement Proposal [AIP-185](https://google.aip.dev/185) (CC-BY 4.0): an old version is deprecated before it is removed.
- Zimmermann et al., *Patterns for API Design*, Addison-Wesley, 2022, patterns Aggressive Obsolescence and Two in Production.
- RFC 9745, [The Deprecation HTTP Response Header Field](https://www.rfc-editor.org/rfc/rfc9745).

The wording and the implementation of this rule are original.
