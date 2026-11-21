---
title: "BQ-DOC-004: API title and version are not placeholders"
description: "info.title is present, not blank and not \"OpenAPI definition\", and info.version is present, not blank and not \"v0\"."
slug: /rules/BQ-DOC-004
rule_id: BQ-DOC-004
severity: MINOR
category: DOCUMENTATION
reads: OPENAPI
---

**Checks:** `info.title` is present, not blank and not "OpenAPI definition", and `info.version` is present, not blank and not "v0".

## Why it matters when you scale

springdoc fills in "OpenAPI definition" and "v0" when the application declares no `info`. Every service then looks the same in an API portal, and the version never tells clients that something changed.

## Example

Fails:

```yaml
info:
  title: OpenAPI definition
  version: v0
```

Passes:

```yaml
info:
  title: Orders API
  version: '1.4'
```

## How to fix it in Spring

Declare an `OpenAPI` bean, or annotate the application class:

```java
@OpenAPIDefinition(info = @Info(title = "Orders API", version = "1.4"))
```

A common choice is to take the version from the build, for example from `BuildProperties`.

## How to suppress it

This rule checks the document as a whole, so it can only be switched off for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-004]
```

## Based on

- Zalando RESTful API Guidelines, rules [116](https://opensource.zalando.com/restful-api-guidelines/#116) and [218](https://opensource.zalando.com/restful-api-guidelines/#218) (CC-BY 4.0).

This is an original rule. The wording and the implementation are our own.
