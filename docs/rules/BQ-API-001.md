# BQ-API-001: Operation has an operationId

| | |
|---|---|
| **Severity** | MINOR |
| **Category** | DESIGN |
| **Checks** | Every operation has a non-empty `operationId`. |

## Why it matters when you scale

Client generators turn the `operationId` into a method name. Without one they invent a name from the path and the method, and that name changes when the path changes. Every client built from the document then breaks on a change that should have been invisible.

Documentation tools also use the `operationId` for deep links to an operation.

## Example

Fails:

```yaml
paths:
  /orders:
    get:
      summary: List orders
      responses:
        '200': {description: OK}
```

Passes:

```yaml
paths:
  /orders:
    get:
      operationId: listOrders
      summary: List orders
      responses:
        '200': {description: OK}
```

## How to fix it in Spring

springdoc sets the `operationId` to the controller method's name, so this rule rarely fails for a generated document. It fails when a customizer removes the value, or when the document is written by hand.

To choose the name yourself:

```java
@Operation(operationId = "listOrders")
@GetMapping("/orders")
List<Order> list() { ... }
```

This rule does not check the style of the name. springdoc generates camelCase, which is the common convention for client libraries.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-API-001"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-API-001]
```

## Based on

- Spectral rule [`operation-operationId`](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules#operation-operationid).

The wording and the implementation of this rule are original.
