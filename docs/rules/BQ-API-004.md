---
title: "BQ-API-004: operationId is unique"
description: "No two operations in the document share an operationId. Every operation that shares one is reported."
slug: /rules/BQ-API-004
rule_id: BQ-API-004
severity: MAJOR
category: DESIGN
reads: OPENAPI
---

**Checks:** No two operations in the document share an `operationId`. Every operation that shares one is reported.

## Why it matters when you scale

Client generators turn each `operationId` into a method name. Two operations with the same ID produce clashing methods, so the generator fails or silently drops one of them.

## Example

Fails:

```yaml
paths:
  /orders:
    get: {operationId: list, responses: {'200': {description: OK}}}
  /customers:
    get: {operationId: list, responses: {'200': {description: OK}}}
```

Passes:

```yaml
paths:
  /orders:
    get: {operationId: listOrders, responses: {'200': {description: OK}}}
  /customers:
    get: {operationId: listCustomers, responses: {'200': {description: OK}}}
```

## How to fix it in Spring

In a springdoc application this rule does not fire: springdoc 3.x registers its own `OperationIdCustomizer`, which runs after every other customizer and renames duplicates to `name_1`, `name_2` and so on. The duplicate then shows up as [BQ-API-002](BQ-API-002.md) instead. This rule matters when that customizer is replaced, and for documents that springdoc did not generate.

Give each operation its own ID:

```java
@Operation(operationId = "listCustomers")
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-API-004"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-API-004]
```

## Based on

- Spectral rule [`operation-operationId-unique`](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules#operation-operationid-unique).

The wording and the implementation of this rule are original.
