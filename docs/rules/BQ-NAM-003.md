# BQ-NAM-003: Path has no CRUD verb

| | |
|---|---|
| **Severity** | MINOR |
| **Category** | NAMING |
| **Checks** | No fixed path segment starts with one of these verbs: get, create, update, delete, add, remove, fetch, list, insert, save, retrieve, modify. Words are split at hyphens, underscores, dots and camelCase, so `getOrders` and `delete-order` fail while `addresses` and `listings` pass. |

## Why it matters when you scale

The HTTP method already says whether you read, create, update or delete. A verb in the path repeats it, invites contradictions such as `GET /deleteOrder`, and leads to one path per action instead of one per resource. In a controlled experiment with 105 developers, CRUD names in URIs had the largest negative effect on understanding of all the rules tested.

## Example

Fails:

```yaml
paths:
  /orders/{id}/delete:
    post: {}
```

Passes:

```yaml
paths:
  /orders/{id}:
    delete: {}
```

## How to fix it in Spring

Map the action to the HTTP method:

```java
@DeleteMapping("/orders/{id}")
void cancel(@PathVariable String id) { ... }
```

Actions that are not create, read, update or delete, such as `/orders/{id}/cancellation`, are fine as nouns.

**Heuristic.** Only the first word of each segment is checked, because words such as "update" are often nouns later in a segment (`/software-update`).

## How to suppress it

This rule reports paths, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt a path, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-NAM-003]
```

## Based on

- Bogner, Kotstein and Pfaff, ["Do RESTful API Design Rules Have an Impact on the Understandability of Web APIs?"](https://arxiv.org/abs/2305.07346), Empirical Software Engineering 28, 2023 (rule CRUDNames).
- Zalando RESTful API Guidelines, rule [141](https://opensource.zalando.com/restful-api-guidelines/#141) (CC-BY 4.0).

The wording and the implementation of this rule are original.
