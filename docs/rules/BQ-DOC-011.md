---
title: "BQ-DOC-011: API version is semantic"
description: "info.version is a Semantic Versioning version such as 1.4.0, or a release date."
slug: /rules/BQ-DOC-011
rule_id: BQ-DOC-011
severity: MINOR
category: DOCUMENTATION
reads: OPENAPI
---

**Checks:** `info.version` is a [Semantic Versioning 2.0](https://semver.org/spec/v2.0.0.html) version, `MAJOR.MINOR.PATCH` with an optional pre-release or build part (`1.4.0`, `0.2.0-SNAPSHOT`), or a date `YYYY-MM-DD` with an optional `-preview`. A missing version and springdoc's `v0` placeholder are reported by [BQ-DOC-004](BQ-DOC-004.md) instead.

## Why it matters when you scale

A version tells clients whether an update can break them. `1.0`, `v2` or `latest` cannot be ordered or compared reliably, so neither people nor tools comparing two releases of the document can tell a fix from a breaking change. Serbout and Pautasso found SemVer in about 9 of 10 versioned API descriptions.

## Example

Fails:

```yaml
info: {title: Orders API, version: '1.0'}
```

Passes:

```yaml
info: {title: Orders API, version: 1.0.0}
```

## How to fix it in Spring

Write all three numbers, or take the version from the build so it never drifts:

```java
@Bean
OpenAPI api(BuildProperties build) {
    return new OpenAPI().info(new Info().title("Orders API").version(build.getVersion()));
}
```

`BuildProperties` needs the `build-info` goal of `spring-boot-maven-plugin`.

## How to suppress it

This rule judges the document's version, not one operation, so it can only be turned off for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-011]
```

## Based on

- Zalando RESTful API Guidelines, rule [116](https://opensource.zalando.com/restful-api-guidelines/#116) (CC-BY 4.0): use semantic versioning.
- Serbout and Pautasso, "How Are Web APIs Versioned in Practice? A Large-Scale Empirical Study", Journal of Web Engineering 23(4), 2024, [doi:10.13052/jwe1540-9589.2341](https://doi.org/10.13052/jwe1540-9589.2341).

The wording and the implementation of this rule are original.
