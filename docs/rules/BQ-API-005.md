# BQ-API-005: Response schema is not a free-form object

| | |
|---|---|
| **Severity** | MINOR |
| **Category** | DESIGN |
| **Checks** | A response schema describes its shape. It fails when the schema is an `object`, or has no type at all, and names no properties, no `$ref`, no `allOf`/`anyOf`/`oneOf`/`not`, and no typed `additionalProperties`. Arrays and scalar types pass. |

## Why it matters when you scale

A schema that allows any object documents nothing. springdoc writes one whenever a controller returns `Object`, `Map<String, Object>` or `ResponseEntity<?>`. Clients then have to read the source code, or guess, to find out which fields exist.

## Example

Fails:

```yaml
content:
  application/json:
    schema: {type: object}
```

Passes:

```yaml
content:
  application/json:
    schema: {$ref: '#/components/schemas/Order'}
```

## How to fix it in Spring

Return a record or class with named fields:

```java
record OrderSummary(String id, String status) {}

@GetMapping("/orders/{id}/summary")
OrderSummary summary(@PathVariable String id) { ... }
```

A map with a typed value, such as `Map<String, Integer>`, also passes.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-API-005"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-API-005]
```

## Based on

- Zalando RESTful API Guidelines, rule [151](https://opensource.zalando.com/restful-api-guidelines/#151) (CC-BY 4.0).

This is an original rule. The wording and the implementation are our own.
