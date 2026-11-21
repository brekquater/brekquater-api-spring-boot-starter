---
title: "BQ-TST-002: Branch coverage reaches the target"
description: "The share of branches that tests take, from the project totals of a JaCoCo XML report, is at least the target: 70% by default. A project without branches is not checked."
slug: /rules/BQ-TST-002
rule_id: BQ-TST-002
severity: MINOR
category: TESTING
reads: COVERAGE
---

**Checks:** The share of branches that tests take, from the project totals of a JaCoCo XML report, is at least the target: 70% by default. A project without branches is not checked.

**Reads:** `jacoco.xml` from your build. See [reports](../reports.md).

## Why it matters when you scale

Line coverage counts an `if` as covered when only one of its outcomes was tested. Error handling, validation and authorisation checks often sit in the branch that no test takes, and those are the paths clients hit first when something goes wrong.

The target is lower than for lines because some branches, such as defensive checks, are hard to reach on purpose. 70% is a Brekquater default, not an industry standard; set your own if it does not fit.

## Example

Fails with the default target:

```xml
<report name="orders">
  <counter type="BRANCH" missed="40" covered="60"/>
</report>
```

Passes: `<counter type="BRANCH" missed="30" covered="70"/>`.

## How to fix it in Spring

Add a test for the other outcome of each condition: the invalid request, the missing entity, the denied caller. `@WebMvcTest` with `MockMvc` makes error paths cheap to test. To use another target:

```yaml
brekquater:
  api:
    thresholds:
      branch-coverage: 60
```

## How to suppress it

These findings come from a build report, not from the OpenAPI document, so `x-brekquater-ignore` does not apply. Change the target (see above), or turn the rule off for the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-TST-002]
```

## Based on

- JaCoCo's [`BRANCH` counter](https://www.jacoco.org/jacoco/trunk/doc/counters.html).
- Google Testing Blog, [Code Coverage Best Practices](https://testing.googleblog.com/2020/08/code-coverage-best-practices.html) (2020), on why line coverage alone overstates what is tested.

The wording and the implementation of this rule are original.
