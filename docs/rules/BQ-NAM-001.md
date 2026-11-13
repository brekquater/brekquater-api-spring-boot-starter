# BQ-NAM-001: Path segments are kebab-case

| | |
|---|---|
| **Severity** | MINOR |
| **Category** | NAMING |
| **Checks** | Every fixed segment of every path is lowercase letters and digits, with single hyphens between words. Segments that contain a path parameter, such as `{orderId}`, are not checked. |

## Why it matters when you scale

Mixed naming styles make an API look like several APIs, and clients have to remember which style each path uses. Paths are also case-sensitive, so `/orderItems` and `/orderitems` are different resources to a server.

## Example

Fails:

```yaml
paths:
  /orderItems/{id}: {}
```

Passes:

```yaml
paths:
  /order-items/{id}: {}
```

## How to fix it in Spring

Rename the mapping:

```java
@RequestMapping("/order-items")
```

Renaming a path that clients already use is a breaking change. Add the new path, keep the old one, and mark it `@Deprecated` until clients have moved.

## How to suppress it

This rule reports paths, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt a path, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-NAM-001]
```

## Based on

- Zalando RESTful API Guidelines, rule [129](https://opensource.zalando.com/restful-api-guidelines/#129) (CC-BY 4.0).

The wording and the implementation of this rule are original.
