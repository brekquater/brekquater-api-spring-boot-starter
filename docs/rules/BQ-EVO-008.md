---
title: "BQ-EVO-008: Major version marks a breaking change"
description: "When the document breaks clients of the last release, the major version went up."
slug: /rules/BQ-EVO-008
rule_id: BQ-EVO-008
severity: MINOR
category: DESIGN
reads: BASELINE
---

**Checks:** Compares the current document with the baseline, the OpenAPI document of your last release (see [reports](../reports.md#breaking-changes-the-baseline-document)). Operations are matched by method and path, with path variable names ignored. When any of BQ-EVO-001 to BQ-EVO-007 finds a breaking change, the major number of `info.version` is higher than in the baseline. Versions are read as [Semantic Versioning 2.0](https://semver.org/spec/v2.0.0.html), an optional `v` included; the rule is skipped when either version has no major number. MINOR, because the breaking changes themselves are already reported. The rule is skipped when there is no baseline document.

## Why it matters when you scale

The version is how clients and their dependency tools decide whether an upgrade is safe. A breaking change under the same major version reaches them as if it were a fix.

## Example

Fails, when the baseline has:

```yaml
info: {version: 1.4.0}
```

and the current document has:

```yaml
info: {version: 1.5.0}   # and an operation was removed
```

## How to fix it in Spring

Raise the major version when you release the change:

```java
new Info().title("Orders API").version("2.0.0")
```

Or undo the breaking changes the other BQ-EVO rules report.

## How to suppress it

Findings about the baseline cannot be suppressed per operation. Refresh the baseline once a change is released on purpose, or turn the rule off.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-EVO-008]
```

## Based on

- Zalando RESTful API Guidelines, rule [116](https://opensource.zalando.com/restful-api-guidelines/#116) (CC-BY 4.0): increment the major version for incompatible changes.
- [Semantic Versioning 2.0](https://semver.org/spec/v2.0.0.html), rule 8.

The wording and the implementation of this rule are original.
