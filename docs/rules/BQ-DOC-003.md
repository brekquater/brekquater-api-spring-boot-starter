# BQ-DOC-003: API has a description

| | |
|---|---|
| **Severity** | MINOR |
| **Category** | DOCUMENTATION |
| **Checks** | `info.description` is present and not blank. |

## Why it matters when you scale

The title says what the API is called; the description says what it is for and what it is not for. Without it, new client teams pick the wrong API or misuse the right one.

## Example

Fails:

```yaml
info:
  title: Orders API
  version: '1.4'
```

Passes:

```yaml
info:
  title: Orders API
  version: '1.4'
  description: Places and tracks customer orders. Payments live in the Payments API.
```

## How to fix it in Spring

Set the description on the `OpenAPI` bean:

```java
new Info().title("Orders API").version("1.4")
        .description("Places and tracks customer orders.")
```

## How to suppress it

This rule checks the document as a whole, so it can only be switched off for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-DOC-003]
```

## Based on

- Spectral rule [`info-description`](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules#info-description).
- Zalando RESTful API Guidelines, rule [218](https://opensource.zalando.com/restful-api-guidelines/#218) (CC-BY 4.0).

The wording and the implementation of this rule are original.
