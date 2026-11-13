# BQ-DES-001: Collection GET is paginated

| | |
|---|---|
| **Severity** | MINOR |
| **Category** | DESIGN |
| **Checks** | A `GET` that returns a list documents at least one paging query parameter: `page`, `size`, `limit`, `offset`, `cursor`, `after`, `before`, `pageable`, `page-token` or a variant such as `page_size` or `pageSize`. |

## Why it matters when you scale

A list that is fine with fifty rows becomes a slow, memory-hungry response with fifty thousand. Adding paging later changes the contract for every client. Unbounded lists are also a resource-consumption risk.

## Example

Fails:

```yaml
paths:
  /orders:
    get:
      responses:
        '200':
          description: OK
          content: {application/json: {schema: {type: array, items: {$ref: '#/components/schemas/Order'}}}}
```

Passes:

```yaml
paths:
  /orders:
    get:
      parameters:
        - {name: page, in: query, schema: {type: integer}}
        - {name: size, in: query, schema: {type: integer}}
      responses:
        '200':
          description: OK
          content: {application/json: {schema: {type: array, items: {$ref: '#/components/schemas/Order'}}}}
```

## How to fix it in Spring

Accept a Spring Data `Pageable`. Annotate it with `@ParameterObject` so springdoc documents `page`, `size` and `sort` as query parameters:

```java
@GetMapping("/orders")
List<Order> list(@ParameterObject Pageable pageable) { ... }
```

**Heuristic.** An operation is a "collection GET" when it is a `GET`, its path does not end in a path parameter, and a success response is an array. An endpoint that returns a page object, such as Spring Data's `Page`, is not an array and is not checked. False positives are possible for small, fixed lists; suppress those.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-DES-001"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DES-001]
```

## Based on

- Zalando RESTful API Guidelines, rule [159](https://opensource.zalando.com/restful-api-guidelines/#159) (CC-BY 4.0).
- OWASP API Security Top 10 2023, [API4: Unrestricted Resource Consumption](https://owasp.org/API-Security/editions/2023/en/0xa4-unrestricted-resource-consumption/) (cited, not copied).

The wording and the implementation of this rule are original.
