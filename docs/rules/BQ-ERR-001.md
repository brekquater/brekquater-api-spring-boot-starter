---
title: "BQ-ERR-001: Operation documents an error response"
description: "Every operation documents at least one error response: a 4xx or 5xx status code, a 4XX or 5XX range, or default."
slug: /rules/BQ-ERR-001
rule_id: BQ-ERR-001
severity: MAJOR
category: ERRORS
reads: OPENAPI
---

**Checks:** Every operation documents at least one error response: a `4xx` or `5xx` status code, a `4XX` or `5XX` range, or `default`.

## Why it matters when you scale

Every operation can fail. If the document shows only the success response, client developers learn the error format by trial and error, and each client handles failures differently. Changing the error format later then breaks clients in ways you cannot see from the document.

Documenting one error response per operation is the smallest step that makes failure part of the contract.

## Example

Fails:

```yaml
paths:
  /orders/{id}:
    get:
      responses:
        '200': {description: OK}
```

Passes:

```yaml
paths:
  /orders/{id}:
    get:
      responses:
        '200': {description: OK}
        '404': {description: Not Found}
```

## How to fix it in Spring

The fix that covers the whole application is an exception handler. springdoc reads `@ControllerAdvice` classes and adds the status of each handler to every operation:

```java
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail orderNotFound(OrderNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }
}
```

The handler needs `@ResponseStatus`; springdoc cannot see a status that is only set inside the method body.

To document an error on one operation only:

```java
@ApiResponse(responseCode = "404", description = "Order not found")
@GetMapping("/orders/{id}")
Order findOrder(@PathVariable String id) { ... }
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-ERR-001"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-ERR-001]
```

## Based on

- Zalando RESTful API Guidelines, rule 151, ["MUST specify success and error responses"](https://opensource.zalando.com/restful-api-guidelines/#151) (CC-BY 4.0).
- [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457), Problem Details for HTTP APIs.

This is an original rule. The wording and the implementation are our own.
