# BQ-ERR-002: Error responses use problem details

| | |
|---|---|
| **Severity** | MINOR |
| **Category** | ERRORS |
| **Checks** | Every documented error response (`4xx`, `5xx`, `4XX`, `5XX` or `default`) that has a body offers `application/problem+json` (or `application/problem+xml`). Error responses without a body are not checked. |

## Why it matters when you scale

If every endpoint invents its own error shape, every client needs custom parsing for every endpoint. RFC 9457 gives one standard shape, and Spring supports it out of the box with `ProblemDetail`.

## Example

Fails:

```yaml
responses:
  '404':
    description: Not Found
    content:
      '*/*': {schema: {$ref: '#/components/schemas/ErrorBody'}}
```

Passes:

```yaml
responses:
  '404':
    description: Not Found
    content:
      application/problem+json: {schema: {$ref: '#/components/schemas/ProblemDetail'}}
```

## How to fix it in Spring

Return `ProblemDetail` from your exception handlers, and document the media type:

```java
@ExceptionHandler(OrderNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
@ApiResponse(responseCode = "404", description = "Order not found",
        content = @Content(mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
ProblemDetail orderNotFound(OrderNotFoundException exception) { ... }
```

Setting `spring.mvc.problemdetails.enabled=true` makes Spring's own errors use the same format.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-ERR-002"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-ERR-002]
```

## Based on

- [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457), Problem Details for HTTP APIs.
- Zalando RESTful API Guidelines, rule [176](https://opensource.zalando.com/restful-api-guidelines/#176) (CC-BY 4.0).

The wording and the implementation of this rule are original.
