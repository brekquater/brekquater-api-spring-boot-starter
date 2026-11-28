---
description: Feed JaCoCo, Surefire, Failsafe, OSV-Scanner or OWASP Dependency-Check reports, and the OpenAPI document of your last release, to the starter to turn on its testing, vulnerability and breaking-change rules.
---

# Build reports

Besides the OpenAPI document, the starter reads three kinds of report that your build can write, and the OpenAPI document of your last release. Each one turns on more rules:

| Report | Rules | Category |
|---|---|---|
| JaCoCo XML coverage | [BQ-TST-001](rules/BQ-TST-001.md) line coverage, [BQ-TST-002](rules/BQ-TST-002.md) branch coverage | TESTING |
| Surefire or Failsafe XML test results | [BQ-TST-003](rules/BQ-TST-003.md) no failing tests | TESTING |
| OSV-Scanner or OWASP Dependency-Check JSON | [BQ-VUL-001](rules/BQ-VUL-001.md) to [BQ-VUL-004](rules/BQ-VUL-004.md), one per CVSS band | SECURITY |
| OpenAPI document of the last release | [BQ-EVO-001](rules/BQ-EVO-001.md) to [BQ-EVO-009](rules/BQ-EVO-009.md), breaking changes since that release | DESIGN |

No report is required. When one is missing, its rules are skipped, the category says so, and the assessment's `inputs` list tells you what to run.

## Your data stays on your machine

The starter only reads files. It never calls a vulnerability database or any other service: the scanner your build runs does that, at build time. In the assessment and in its log, a report is named by its file name only, never by its directory.

## Where the starter looks

Paths are relative to the working directory, which is the project directory when you run `./mvnw spring-boot:run` or start the application from an IDE.

| Report | Default location | Property |
|---|---|---|
| Coverage | `target/site/jacoco/jacoco.xml` | `brekquater.api.reports.coverage` |
| Test results | every `TEST-*.xml` in `target/surefire-reports` and `target/failsafe-reports` | `brekquater.api.reports.test-results` |
| Vulnerabilities | `target/osv-scanner.json`, then `target/dependency-check-report.json` | `brekquater.api.reports.vulnerabilities` |
| Baseline document | `openapi-baseline.json` | `brekquater.api.reports.baseline` |

The starter notices when a report is written again and reassesses on the next request. A report larger than 50 MB, or one that is malformed, is marked `UNREADABLE`, and the log says why.

Reports describe the build they came from. The panel shows each report's age; rebuild after you change code, so the evidence matches what is running.

## Coverage: JaCoCo

Add the JaCoCo plugin, then run `./mvnw verify`:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.16</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

The project totals of the report are checked against `brekquater.api.thresholds.line-coverage` (80 by default) and `brekquater.api.thresholds.branch-coverage` (70 by default).

## Test results: Surefire and Failsafe

Nothing to add: Surefire writes `target/surefire-reports` on every `./mvnw test`, and Failsafe writes `target/failsafe-reports` when you use it.

## Vulnerabilities: OSV-Scanner (recommended)

[OSV-Scanner](https://google.github.io/osv-scanner/) needs no API key and reads `pom.xml` directly. With version 2, from the project directory:

```bash
osv-scanner scan -r . --format json --output-file target/osv-scanner.json
```

OSV-Scanner exits with a non-zero code when it finds vulnerabilities; the report is written either way. The starter reads the highest CVSS score of each group of related advisories. An advisory with no score is placed by its GitHub severity, or counted as Medium when it has none.

OSV-Scanner matches advisories by package. A newly published CVE that no advisory database has tied to a Maven package yet, and that names only a source repository, is not reported, even though other scanners may already flag it. Dependency-Check matches by CPE instead, so running both narrows the gap.

To accept an advisory you have reviewed, add it to `osv-scanner.toml` with a reason. Ignored advisories are not in the report, so the starter does not see them.

## Vulnerabilities: OWASP Dependency-Check

[Dependency-Check](https://dependency-check.github.io/DependencyCheck/) runs inside Maven. Ask it for a JSON report:

```xml
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>13.0.0</version>
  <configuration>
    <formats>
      <format>HTML</format>
      <format>JSON</format>
    </formats>
  </configuration>
</plugin>
```

```bash
./mvnw dependency-check:check
```

The first run downloads the NVD data, which is slow without an NVD API key; see the plugin's documentation for `nvdApiKey`. The starter uses the newest CVSS version in the report (v4, then v3, then v2). Vulnerabilities you suppress in Dependency-Check's suppression file are not read.

## Breaking changes: the baseline document

The BQ-EVO rules compare the current OpenAPI document with the one of your last release, which you commit with your code. It is a file in your repository, not something the starter stores or fetches: the starter only reads it.

At each release, save the document the release serves and commit it:

```bash
curl http://localhost:8080/v3/api-docs -o openapi-baseline.json
```

The file can be JSON or YAML, in OpenAPI 3.0 or 3.1, and must be saved as UTF-8. Windows PowerShell's `Out-File` writes UTF-16, so use `curl.exe -o`; a file that cannot be read is marked `UNREADABLE`, and the log says why. Between releases it stays the same, so every change you make is compared with what your clients actually use, and code review shows the API's diff when someone refreshes it.

What is compared:

- Operations are matched by method and path. When no path is the same, one that differs only in path variable names is used, so renaming `{id}` to `{orderId}`, or changing an operationId, is not a change. An operation that several current ones could have become is skipped rather than guessed.
- Parameters, request bodies and the JSON bodies of success responses are compared, following shared schemas, nested objects, arrays and `allOf`. A change in a shared schema is reported on every operation that uses it, and the message says where the schema is. A `*/*` body, which springdoc writes by default, is compared as JSON.
- The server address springdoc generates for the running application is ignored.

When a change is intended, release it, and refresh the baseline in the same release.

## What the assessment says about reports

Each report appears in the `inputs` list of the JSON:

```json
{"type": "COVERAGE", "status": "READ", "file": "jacoco.xml", "modifiedAt": "2026-11-13T09:58:00Z"}
{"type": "VULNERABILITIES", "status": "MISSING", "hint": "Run OSV-Scanner or OWASP Dependency-Check with JSON output. See https://starter.brekquater.com/api/reports"}
```

`status` is `READ`, `MISSING` or `UNREADABLE`. When coverage or test results were read, the `TESTING` category also has `metrics`: `lineCoverage`, `branchCoverage`, `testsPassed`, `testsFailed` and `testsSkipped`.
