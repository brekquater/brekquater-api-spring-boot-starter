# BQ-SEC-001: Operation declares security

| | |
|---|---|
| **Severity** | BLOCKER |
| **Category** | SECURITY |
| **Checks** | Every operation has effective security, or is marked public on purpose with `x-brekquater-public: true`. An operation's own `security` replaces the document-wide `security`. A list that is empty, or that contains an empty entry (`{}`, meaning anonymous access is allowed), counts as no security. |

## Why it matters when you scale

An operation that declares no security is either public on purpose or an accident, and the document cannot tell which. Broken authentication is one of the most common API security failures. This rule makes every open endpoint a visible decision.

## Example

Fails:

```yaml
paths:
  /orders/{id}:
    get:
      responses: {'200': {description: OK}}
```

Passes:

```yaml
security: [{bearer: []}]
paths:
  /orders/{id}:
    get:
      responses: {'200': {description: OK}}
  /health:
    get:
      x-brekquater-public: true
      responses: {'200': {description: OK}}
components:
  securitySchemes:
    bearer: {type: http, scheme: bearer}
```

## How to fix it in Spring

Document the security your application already enforces. For the whole API:

```java
@Bean
OpenAPI ordersApi() {
    return new OpenAPI()
            .components(new Components().addSecuritySchemes("bearer",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
            .addSecurityItem(new SecurityRequirement().addList("bearer"));
}
```

For one operation, use `@SecurityRequirement(name = "bearer")`. For an endpoint that is public on purpose:

```java
@Operation(extensions = @Extension(properties =
        @ExtensionProperty(name = "x-brekquater-public", value = "true", parseValue = true)))
```

This rule checks the document, not your `SecurityFilterChain`. It cannot tell whether the security you document is really enforced.

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-SEC-001"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation).

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-001]
```

## Based on

- OWASP API Security Top 10 2023, [API2: Broken Authentication](https://owasp.org/API-Security/editions/2023/en/0xa2-broken-authentication/) (cited, not copied).

The wording and the implementation of this rule are original.
