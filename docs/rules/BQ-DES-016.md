---
title: "BQ-DES-016: Path versions share one position"
description: "Every version path segment sits at the same position."
slug: /rules/BQ-DES-016
rule_id: BQ-DES-016
severity: MINOR
category: DESIGN
reads: OPENAPI
---

**Checks:** Every path version segment sits at the same segment index, counting from 0 after the leading slash. The most common index is taken as the API's own, and paths with the version elsewhere are reported. A version segment is a literal segment such as `v2`, `v1beta` or `2`; springdoc writes Spring's path-segment versioning that way. A template such as `{version}` is not read.

## Why it matters when you scale

Spring's path-segment versioning reads the version from one fixed index, so an API with `/v1/orders` and `/api/v2/customers` cannot move to it without breaking URLs. Clients also build every URL from one base path.

## Example

Fails:

```yaml
/api/v1/orders: {}
/v2/invoices: {}
```

Passes:

```yaml
/api/v1/orders: {}
/api/v2/invoices: {}
```

## How to fix it in Spring

Give every controller the same prefix, and let Spring read the version from it:

```java
@RestController
@RequestMapping("/api/{version}/invoices")
class InvoicesController {
    @GetMapping(version = "2")
    List<Invoice> list() { ... }
}
```

```yaml
spring:
  mvc:
    apiversion:
      use:
        path-segment: 1
```

## How to suppress it

This rule judges a path, not one operation, so it can only be turned off for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-016]
```

## Based on

- Spring Framework reference, [API version configuration](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/api-version.html): the path segment resolver reads one index.
- Google API Improvement Proposal [AIP-185](https://google.aip.dev/185) (CC-BY 4.0).

The wording and the implementation of this rule are original.
