# BQ-DES-002: GET has no request body

| | |
|---|---|
| **Severity** | MAJOR |
| **Category** | DESIGN |
| **Checks** | No `GET` operation declares a `requestBody`. |

## Why it matters when you scale

HTTP gives a body on `GET` no defined meaning. Proxies, caches, API gateways and some client libraries drop it, so the request that worked in development fails behind the production gateway. A controlled experiment with 105 developers also found that APIs which use GET only to retrieve are significantly easier to understand.

## Example

Fails:

```yaml
paths:
  /orders/search:
    get:
      requestBody:
        content: {application/json: {schema: {$ref: '#/components/schemas/OrderQuery'}}}
```

Passes:

```yaml
paths:
  /orders:
    get:
      parameters:
        - {name: status, in: query, schema: {type: string}}
```

## How to fix it in Spring

Bind the input from query parameters instead of `@RequestBody`:

```java
@GetMapping("/orders")
List<Order> search(@ParameterObject OrderQuery query) { ... }
```

If the input is too large or too sensitive for a URL, use `POST` on a search resource.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-002"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-002]
```

## Based on

- [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110#section-9.3.1), section 9.3.1 (GET).
- Bogner, Kotstein and Pfaff, ["Do RESTful API Design Rules Have an Impact on the Understandability of Web APIs?"](https://arxiv.org/abs/2305.07346), Empirical Software Engineering 28, 2023 (rule GETRetrieve).

The wording and the implementation of this rule are original.
