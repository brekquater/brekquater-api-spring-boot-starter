# BQ-SEC-002: Security requirement names a defined scheme

| | |
|---|---|
| **Severity** | MAJOR |
| **Category** | SECURITY |
| **Checks** | Every entry of every `security` list, at document or operation level, names only schemes that are defined under `components.securitySchemes`. |

## Why it matters when you scale

A requirement that names an undefined scheme tells clients that authentication is needed but not how. Generated clients cannot add the credentials, and the mistake hides until the first real call fails.

## Example

Fails:

```yaml
security: [{bearer: []}]
components:
  securitySchemes:
    jwt: {type: http, scheme: bearer}
```

Passes:

```yaml
security: [{bearer: []}]
components:
  securitySchemes:
    bearer: {type: http, scheme: bearer}
```

## How to fix it in Spring

Use the same name in the requirement and the scheme:

```java
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer")
@SecurityRequirement(name = "bearer")
```

## How to suppress it

For one operation, add `x-brekquater-ignore` with the value `["BQ-SEC-002"]` to the operation (see [BQ-DOC-001](BQ-DOC-001.md#how-to-suppress-it) for the annotation). A finding on the document-wide `security` list can only be suppressed by disabling the rule.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-002]
```

## Based on

- Spectral rule [`oas3-operation-security-defined`](https://docs.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules#oas3-operation-security-defined).

The wording and the implementation of this rule are original.
