# BQ-TST-001: Line coverage reaches the target

| | |
|---|---|
| **Severity** | MAJOR |
| **Category** | TESTING |
| **Checks** | The share of lines that tests run, from the project totals of a JaCoCo XML report, is at least the target: 80% by default. |
| **Reads** | `jacoco.xml` from your build. See [reports](../reports.md). |

## Why it matters when you scale

Code no test runs is code whose behaviour nobody notices changing. As more clients depend on an API, a silent change in an untested path costs more to find and to fix. Coverage does not prove tests are good, but low coverage reliably shows where nothing checks the code at all.

The project is one target, as in a quality gate: the rule passes or fails as a whole, and raises one finding, not one per class. The finding names the three classes with the most missed lines, so you know where to start.

## Example

Fails with the default target (73 of 100 lines covered):

```xml
<report name="orders">
  <counter type="LINE" missed="27" covered="73"/>
</report>
```

Finding: `Line coverage is 73%, below the target of 80%. Most missed lines: com.example.OrderService (15), ...`

Passes: `<counter type="LINE" missed="20" covered="80"/>`. The comparison is exact, so 79.99% fails even though it would round to 80%.

## How to fix it in Spring

Add tests for the code no test runs yet, starting with the named classes: `@WebMvcTest` slices for controllers, plain unit tests for services. Make sure the build writes the XML report:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <executions>
    <execution><goals><goal>prepare-agent</goal></goals></execution>
    <execution><id>report</id><goals><goal>report</goal></goals></execution>
  </executions>
</plugin>
```

To use another target:

```yaml
brekquater:
  api:
    thresholds:
      line-coverage: 70
```

## How to suppress it

These findings come from a build report, not from the OpenAPI document, so `x-brekquater-ignore` does not apply. Change the target (see above), or turn the rule off for the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-TST-001]
```

## Based on

- The Sonar way quality gate, which requires 80% coverage on new code ([SonarQube metrics and quality gates](https://docs.sonarsource.com/sonarqube-server/latest/user-guide/code-metrics/metrics-definition/)).
- Google Testing Blog, [Code Coverage Best Practices](https://testing.googleblog.com/2020/08/code-coverage-best-practices.html) (2020), which calls 60% acceptable, 75% commendable and 90% exemplary.

The wording and the implementation of this rule are original.
