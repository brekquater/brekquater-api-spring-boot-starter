---
title: "BQ-NAM-005: Property names share one case style"
description: "Property names follow one case style: whichever of camelCase, snake_case, kebab-case or PascalCase most of the API's names already use."
slug: /rules/BQ-NAM-005
rule_id: BQ-NAM-005
severity: MINOR
category: NAMING
reads: OPENAPI
---

**Checks:** Property names follow one case style: whichever of camelCase, snake_case, kebab-case or PascalCase most of the API's property names already use. When two styles are equally common, camelCase wins, because it is Jackson's default. Names of one lowercase word, such as `id`, fit every style and are not counted. A name that mixes styles, such as `customer_Name`, is always reported.

## Why it matters when you scale

A client that reads `customerName` will guess `orderId`, not `order_id`. Guidelines disagree on which style to use (Zalando asks for snake_case, Microsoft and Google for camelCase), but all of them ask for one style across the API. In Spring a stray `@JsonProperty("order_id")` or a class from another library is usually the cause.

## Example

Fails:

```yaml
LegacyError:
  properties:
    errorMessage: {type: string}
    error_code: {type: string}
    requestId: {type: string}
```

Passes:

```yaml
LegacyError:
  properties:
    errorMessage: {type: string}
    errorCode: {type: string}
    requestId: {type: string}
```

## How to fix it in Spring

Rename the property so it matches the rest of the API:

```java
record LegacyError(String errorMessage, String errorCode, String requestId) {}
```

If the whole class must use another style, for example because it mirrors an external format, set it once with `@JsonNaming` rather than on each field.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-NAM-005"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). This covers properties written inline in the operation; a property of a named schema under `components` can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-NAM-005]
```

## Based on

- Zalando RESTful API Guidelines, rule [118](https://opensource.zalando.com/restful-api-guidelines/#118) (CC-BY 4.0), and the [Microsoft Azure REST API Guidelines](https://github.com/microsoft/api-guidelines/blob/vNext/azure/Guidelines.md) (CC-BY 4.0), which choose different styles.
- IBM OpenAPI validator rule `ibm-property-casing-convention`, [ibm-cloud rules](https://github.com/IBM/openapi-validator/blob/main/docs/ibm-cloud-rules.md) (Apache-2.0).

The wording and the implementation of this rule are original.
