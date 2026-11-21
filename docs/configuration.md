# Configuration reference

Every property lives under `brekquater.api`. Your IDE autocompletes them from the metadata in the jar.

A value the starter cannot use stops the application at startup. The startup report names the property, the value, and how to fix it.

## Properties

| Property | Default | What it does | Security impact |
|---|---|---|---|
| `brekquater.api.enabled` | `true` | Whether the starter is active at all. `false` registers nothing. | None. |
| `brekquater.api.panel.mode` | *unset* | Who may see the assessment: `disabled`, `authenticated` or `public`. When unset, it is `public` while the `dev` or `local` profile is active, and `disabled` otherwise. | **High.** The assessment lists every operation of your API and its weaknesses. Never set `public` on an environment that untrusted people can reach. See [Exposure](#exposure). |
| `brekquater.api.panel.base-path` | `/brekquater/api` | Path under which the assessment is served. The JSON endpoint is `{base-path}/assessment`; the dashboard page also reads the assessed document from `{base-path}/openapi`. | Low. Changing the path is not a protection; use the mode for that. |
| `brekquater.api.rules.disabled` | *empty* | Comma-separated list of rule IDs that do not run, for example `BQ-DES-001,BQ-NAM-003`. An unknown ID stops startup. | Disabling a security rule (`BQ-SEC-*`) hides security findings. |
| `brekquater.api.rules.severity-overrides.<RULE-ID>` | *none* | Severity to use instead of a rule's own: `BLOCKER`, `MAJOR`, `MINOR` or `INFO`. Only security rules can be raised to `BLOCKER`. | Lowering a security rule's severity lowers its weight in the score and the security rating. |
| `brekquater.api.score.weights.blocker` | `10` | Weight of BLOCKER rules in the score. | None. |
| `brekquater.api.score.weights.major` | `7.5` | Weight of MAJOR rules. | None. |
| `brekquater.api.score.weights.minor` | `5` | Weight of MINOR rules. | None. |
| `brekquater.api.score.weights.info` | `0` | Weight of INFO rules. `0` keeps them out of the score. | None. |
| `brekquater.api.score.grades.a` | `90` | Lowest score for grade A. | None. |
| `brekquater.api.score.grades.b` | `75` | Lowest score for grade B. | None. |
| `brekquater.api.score.grades.c` | `60` | Lowest score for grade C. | None. |
| `brekquater.api.score.grades.d` | `40` | Lowest score for grade D. Anything lower is F. Any blocker finding also makes the grade F; see below. | None. |
| `brekquater.api.reports.coverage` | `target/site/jacoco/jacoco.xml` | JaCoCo XML report. | Low. Only the file name and its age appear in the assessment, never the directory. |
| `brekquater.api.reports.test-results` | `target/surefire-reports`, `target/failsafe-reports` | Surefire or Failsafe reports: files, or directories whose `TEST-*.xml` files are all read. | Low. Failed tests are shown by class and method name. |
| `brekquater.api.reports.vulnerabilities` | `target/osv-scanner.json`, then `target/dependency-check-report.json` | OSV-Scanner or OWASP Dependency-Check JSON report. The scanner is recognised from the content. | **Medium.** The assessment then lists the vulnerable dependencies of your application. |
| `brekquater.api.thresholds.line-coverage` | `80` | Line coverage, in whole percent, that `BQ-TST-001` expects. | None. |
| `brekquater.api.thresholds.branch-coverage` | `70` | Branch coverage, in whole percent, that `BQ-TST-002` expects. | None. |
| `brekquater.api.strict` | `false` | Whether blocker findings stop the application at startup. See [Strict mode](#strict-mode). | None. It makes the starter stricter, never more open. |

Weights must be 0 or more. Grade minimums must be between 0 and 100, with `a >= b >= c >= d`. Coverage thresholds must be between 0 and 100.

Report paths are relative to the working directory unless they are absolute. A report that is not found is never an error: the rules that need it are skipped, and the assessment says how to produce it. See [reports](reports.md).

While any blocker finding remains, such as an operation without security or a Critical known vulnerability, the grade is F whatever the score. The score itself still counts everything else, so progress stays visible; the panel shows "capped by blockers" next to the grade. Suppress or fix the blocker to lift the cap.

The default weights are the OpenSSF Scorecard risk weights for Critical, High and Medium checks. They are a convention, not a measurement; change them if your team values things differently.

## Example

```yaml
brekquater:
  api:
    panel:
      mode: authenticated
    rules:
      disabled: [BQ-DES-001]
      severity-overrides:
        BQ-DOC-002: MAJOR
```

## Strict mode

Strict mode turns blocker findings into a failed startup. It is meant for CI, where a `@SpringBootTest` then fails the build:

```yaml
brekquater:
  api:
    strict: true
```

- It runs once, after every bean is ready and before the web server accepts connections, so a failing application never serves a request.
- Blockers are the findings of severity BLOCKER: operations without security (`BQ-SEC-001`) and dependencies with a Critical known vulnerability (`BQ-VUL-001`), plus any security rule you raise to BLOCKER.
- The startup report lists up to ten blockers with their location, and how to fix or suppress them.
- It does not depend on the panel mode or the profile: it also runs where the panel is disabled.
- Without it, nothing is assessed at startup; the assessment runs on the first request.

The document is read with a stand-in request for `http://localhost{context-path}`, because no real request exists yet. Only the `servers` entry of the document depends on the request, and no rule reads it.

## Exposure

The assessment shows the shape of your whole API, including the operations that have no security declared. Treat it like Swagger UI itself.

| Mode | Behaviour |
|---|---|
| `disabled` | No handler is registered. The paths return 404. |
| `public` | Anyone who can reach the application can read the assessment. |
| `authenticated` | Only callers that Spring Security has authenticated can read it; anonymous callers get 401. |

About `authenticated`:

- It needs Spring Security on the classpath. Without it, startup fails and says so.
- The starter checks the caller inside its own handler. It never registers or changes a `SecurityFilterChain`.
- Your own security configuration must let authenticated users reach the path. For example:

  ```java
  http.authorizeHttpRequests(requests -> requests
          .requestMatchers("/brekquater/api/**").authenticated()
          // ... the rest of your rules
  );
  ```

## The panel

When the assessment is served, it also appears in two places:

- **Inside Swagger UI**, as a column on the right of the operations when the window is at least 1200 pixels wide, and as a collapsed "API maturity" bar between the API title and the operations on narrower screens. It shows the score card, category scores, the findings (most severe first, with filters for severity and category and a search box) and the build evidence. It follows Swagger UI's light and dark switch.
- **On a dashboard page** at `{base-path}`, by default `/brekquater/api`. It lists your operations by tag with the number of findings on each; pick one to see only its findings. It has its own theme switch (System, Light, Dark), remembered in the browser. This page works without Swagger UI, so it is also the fallback if the panel cannot be added to Swagger UI. It reads the assessed OpenAPI document from `{base-path}/openapi`, which follows the same mode as the assessment.

**Refresh** fetches the assessment again. It is re-run only when the OpenAPI document or a build report has changed; otherwise the cached result comes back.

The panel is added through springdoc's `SwaggerIndexTransformer`. If your application defines its own bean of that type, the starter leaves it alone; the page of its own still works.

If your application uses Spring's path-segment API versioning (`spring.mvc.apiversion.use.path-segment`), the starter exempts its base path from version resolution, the same way springdoc exempts its own paths. Your own paths keep their versioning.

Outside the `dev` and `local` profiles the starter logs a warning at startup if Swagger UI is enabled or the assessment is public.

## Suppressing a finding on one operation

Add the `x-brekquater-ignore` extension to the operation, with the IDs of the rules to ignore:

```java
@Operation(extensions = @Extension(properties =
        @ExtensionProperty(name = "x-brekquater-ignore", value = "[\"BQ-DOC-001\"]", parseValue = true)))
```

`parseValue = true` matters: without it the value is a string, not a list, and nothing is ignored.

A suppressed finding counts as passing, so suppressing never lowers the score. Suppression only covers findings inside the operation. Findings about a path (`BQ-NAM-*`), the document's `info` (`BQ-DOC-002` to `BQ-DOC-004`) or the document-wide `security` list can only be switched off with `brekquater.api.rules.disabled`.

To mark an operation public on purpose, so that `BQ-SEC-001` accepts it:

```java
@Operation(extensions = @Extension(properties =
        @ExtensionProperty(name = "x-brekquater-public", value = "true", parseValue = true)))
```

## Replacing parts of the starter

Every bean backs off when your application defines a bean of the same type:

| Bean type | Replace it to |
|---|---|
| `OpenApiDocumentSource` | Supply the OpenAPI document another way. |
| `AssessmentService` | Change how documents are assessed or cached. |
| `AccessGuard` | Decide who may see the assessment with your own logic. |
| `RuleRegistry` | Choose the rules yourself. |
| `ScoreCalculator` | Score differently. |
| `ReportWriter` | Write a different output format. |

To add a rule of your own, declare a bean that implements `com.brekquater.api.rules.Rule`. It runs next to the built-in rules.
