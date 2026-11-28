---
title: "BQ-NAM-007: Path version is the major version"
description: "A version in the path names the major version only, as in /v2/orders."
slug: /rules/BQ-NAM-007
rule_id: BQ-NAM-007
severity: MINOR
category: NAMING
reads: OPENAPI
---

**Checks:** A path version segment has no minor or patch number: `v2`, `v2beta` and `2` pass, `v2.1` and `2.0.1` fail. A version segment is a literal segment such as `v2`, `v1beta` or `2`; springdoc writes Spring's path-segment versioning that way. A template such as `{version}` is not read.

## Why it matters when you scale

A minor release is compatible by definition, so clients have no reason to change their URLs for it. Putting it in the path makes every compatible release a breaking change for every stored link, bookmark and client configuration.

## Example

Fails:

```yaml
/v2.1/orders: {}
```

Passes:

```yaml
/v2/orders: {}
```

## How to fix it in Spring

Keep only the major version in the mapping and the full version in `info.version`. With Spring's path-segment versioning, declare the version as `"2"`, or use a baseline such as `"2+"`, rather than `"2.1"`:

```java
@GetMapping(path = "/api/{version}/orders", version = "2")
```

## How to suppress it

This rule judges a path, not one operation, so it can only be turned off for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-NAM-007]
```

## Based on

- Google API Improvement Proposal [AIP-185](https://google.aip.dev/185) (CC-BY 4.0): the version in the path is the major version only.
- Zalando RESTful API Guidelines, rule [116](https://opensource.zalando.com/restful-api-guidelines/#116) (CC-BY 4.0).

The wording and the implementation of this rule are original.
