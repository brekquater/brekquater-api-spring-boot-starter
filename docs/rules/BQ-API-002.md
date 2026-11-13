# BQ-API-002: operationId has no generated suffix

| | |
|---|---|
| **Severity** | INFO |
| **Category** | DESIGN |
| **Checks** | No `operationId` ends in an underscore followed by digits, such as `findAll_1`. INFO findings are shown but never counted in the score. |

## Why it matters when you scale

springdoc names an operation after its controller method. When two controllers have a method with the same name, springdoc adds `_1`, `_2` and so on to keep the IDs unique. Which method gets which suffix depends on the order springdoc meets them, and that order can change between builds. Client libraries generated from the document then rename their methods without warning.

## Example

Fails:

```yaml
paths:
  /orders:
    get: {operationId: findAll, responses: {'200': {description: OK}}}
  /customers:
    get: {operationId: findAll_1, responses: {'200': {description: OK}}}
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

Give the controller methods distinct names, or set the ID explicitly:

```java
@Operation(operationId = "listCustomers")
@GetMapping("/customers")
List<Customer> findAll() { ... }
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-API-002"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-API-002]
```

## Based on

- springdoc's behaviour of de-duplicating operation IDs.

This is an original rule. The wording and the implementation are our own.
