# BQ-TST-003: No test fails

| | |
|---|---|
| **Severity** | MAJOR |
| **Category** | TESTING |
| **Checks** | No test case in the Surefire or Failsafe reports failed or ended with an error. Each test that ran is a target; skipped tests are not. |
| **Reads** | `TEST-*.xml` files in `target/surefire-reports` and `target/failsafe-reports`. See [reports](../reports.md). |

## Why it matters when you scale

A failing test is a known defect, or a test nobody trusts any more. Either way, the suite stops protecting the API, and the next real regression hides among the failures everyone has learned to ignore.

A test that failed and then passed when Surefire reran it (`flakyFailure`) counts as passed, as Surefire itself counts it.

## Example

Fails:

```xml
<testsuite name="com.example.OrdersControllerTest">
  <testcase name="returnsNotFoundForAnUnknownOrder" classname="com.example.OrdersControllerTest">
    <failure message="Status expected:&lt;404&gt; but was:&lt;500&gt;"/>
  </testcase>
</testsuite>
```

Finding at `com.example.OrdersControllerTest#returnsNotFoundForAnUnknownOrder`.

Passes: the same report without the `failure` element.

## How to fix it in Spring

Fix the test or the code it tests, then build again so the report is current. A test that is not ready belongs in `@Disabled("reason")`, which the report counts as skipped.

## How to suppress it

These findings come from a build report, not from the OpenAPI document, so `x-brekquater-ignore` does not apply. Turn the rule off for the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-TST-003]
```

## Based on

- The [Surefire XML report format](https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd), which Surefire and Failsafe share.

The wording and the implementation of this rule are original.
