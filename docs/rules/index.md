# Rule catalogue

Every rule checks one thing and has a page that explains why it matters, shows a failing and a passing example, and says how to fix or suppress it. Rule IDs never change meaning: a rule that has to behave differently gets a new ID.

**Severity** sets a rule's weight in the score: BLOCKER 10, MAJOR 7.5, MINOR 5, INFO 0 (listed, not scored). Only security rules can be BLOCKER, and any blocker finding caps the grade at F. Change weights and severities in [configuration](../configuration.md).

**Reads** says what a rule needs. Rules that read a build report are skipped, never failed, when the report is missing; see [build reports](../reports.md).

## Design

| Rule | Checks | Severity | Reads |
|---|---|---|---|
| [BQ-API-001](BQ-API-001.md) | Operation has an operationId | MINOR | OpenAPI document |
| [BQ-API-002](BQ-API-002.md) | operationId has no generated suffix | INFO | OpenAPI document |
| [BQ-API-003](BQ-API-003.md) | Response content has a schema | MAJOR | OpenAPI document |
| [BQ-API-004](BQ-API-004.md) | operationId is unique | MAJOR | OpenAPI document |
| [BQ-API-005](BQ-API-005.md) | Response schema is not a free-form object | MINOR | OpenAPI document |
| [BQ-DES-001](BQ-DES-001.md) | Collection GET is paginated | MINOR | OpenAPI document |
| [BQ-DES-002](BQ-DES-002.md) | GET has no request body | MAJOR | OpenAPI document |

## Documentation

| Rule | Checks | Severity | Reads |
|---|---|---|---|
| [BQ-DOC-001](BQ-DOC-001.md) | Operation has a summary or description | MINOR | OpenAPI document |
| [BQ-DOC-002](BQ-DOC-002.md) | API declares a contact | MINOR | OpenAPI document |
| [BQ-DOC-003](BQ-DOC-003.md) | API has a description | MINOR | OpenAPI document |
| [BQ-DOC-004](BQ-DOC-004.md) | API title and version are not placeholders | MINOR | OpenAPI document |

## Security

| Rule | Checks | Severity | Reads |
|---|---|---|---|
| [BQ-SEC-001](BQ-SEC-001.md) | Operation declares security | BLOCKER | OpenAPI document |
| [BQ-SEC-002](BQ-SEC-002.md) | Security requirement names a defined scheme | MAJOR | OpenAPI document |
| [BQ-VUL-001](BQ-VUL-001.md) | No Critical known vulnerability in dependencies | BLOCKER | Vulnerability report |
| [BQ-VUL-002](BQ-VUL-002.md) | No High known vulnerability in dependencies | MAJOR | Vulnerability report |
| [BQ-VUL-003](BQ-VUL-003.md) | No Medium known vulnerability in dependencies | MINOR | Vulnerability report |
| [BQ-VUL-004](BQ-VUL-004.md) | No Low known vulnerability in dependencies | INFO | Vulnerability report |

## Errors

| Rule | Checks | Severity | Reads |
|---|---|---|---|
| [BQ-ERR-001](BQ-ERR-001.md) | Operation documents an error response | MAJOR | OpenAPI document |
| [BQ-ERR-002](BQ-ERR-002.md) | Error responses use problem details | MINOR | OpenAPI document |

## Naming

| Rule | Checks | Severity | Reads |
|---|---|---|---|
| [BQ-NAM-001](BQ-NAM-001.md) | Path segments are kebab-case | MINOR | OpenAPI document |
| [BQ-NAM-002](BQ-NAM-002.md) | Path has no trailing slash | MINOR | OpenAPI document |
| [BQ-NAM-003](BQ-NAM-003.md) | Path has no CRUD verb | MINOR | OpenAPI document |

## Testing

| Rule | Checks | Severity | Reads |
|---|---|---|---|
| [BQ-TST-001](BQ-TST-001.md) | Line coverage reaches the target | MAJOR | Coverage report |
| [BQ-TST-002](BQ-TST-002.md) | Branch coverage reaches the target | MINOR | Coverage report |
| [BQ-TST-003](BQ-TST-003.md) | No test fails | MAJOR | Test reports |

## Not assessed yet

`CODE_QUALITY` and `ARCHITECTURE` are reported as "not assessed" in every assessment. They have no rules yet, and the score never counts them.
