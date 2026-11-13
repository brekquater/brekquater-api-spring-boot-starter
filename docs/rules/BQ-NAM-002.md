# BQ-NAM-002: Path has no trailing slash

| | |
|---|---|
| **Severity** | MINOR |
| **Category** | NAMING |
| **Checks** | No path ends with `/`, except the root path `/` itself. |

## Why it matters when you scale

`/orders` and `/orders/` look like one resource, but servers, caches and clients often treat them as two. Spring Framework 6 and later no longer match a trailing slash by default, so a client that adds one gets a 404.

## Example

Fails:

```yaml
paths:
  /orders/: {}
```

Passes:

```yaml
paths:
  /orders: {}
```

## How to fix it in Spring

Remove the slash from the mapping:

```java
@GetMapping("/orders")
```

## How to suppress it

This rule reports paths, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt a path, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-NAM-002]
```

## Based on

- Spectral rule [`path-keys-no-trailing-slash`](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules#path-keys-no-trailing-slash).
- Zalando RESTful API Guidelines, rule [136](https://opensource.zalando.com/restful-api-guidelines/#136) (CC-BY 4.0).

The wording and the implementation of this rule are original.
