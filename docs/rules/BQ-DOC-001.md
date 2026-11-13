# BQ-DOC-001: Operation has a summary or description

| | |
|---|---|
| **Severity** | MINOR |
| **Category** | DOCUMENTATION |
| **Checks** | Every operation has a non-empty `summary` or `description`. Text that is only whitespace counts as missing. |

## Why it matters when you scale

While your API has one client, you can explain an endpoint in a chat message. Once it has many, the OpenAPI document is the only explanation most developers ever read. An operation with no text forces each of them to guess from the path and the method, and the guesses differ.

Generated client libraries and API portals also show this text as the method documentation.

## Example

Fails:

```yaml
paths:
  /orders/{id}:
    delete:
      operationId: deleteOrder
      responses:
        '204': {description: No Content}
```

Passes:

```yaml
paths:
  /orders/{id}:
    delete:
      operationId: deleteOrder
      summary: Cancel an order
      responses:
        '204': {description: No Content}
```

## How to fix it in Spring

Add `@Operation` to the controller method:

```java
@Operation(summary = "Cancel an order")
@DeleteMapping("/orders/{id}")
void cancelOrder(@PathVariable String id) { ... }
```

A `description` on its own also passes. Use `summary` for a short label and `description` for detail.

## How to suppress it

For one operation, add the extension to the operation:

```java
@Operation(extensions = @Extension(properties =
        @ExtensionProperty(name = "x-brekquater-ignore", value = "[\"BQ-DOC-001\"]", parseValue = true)))
```

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-001]
```

## Based on

- Spectral rule [`operation-description`](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules#operation-description).

The wording and the implementation of this rule are original.
