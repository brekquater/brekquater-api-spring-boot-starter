# Configuration reference

Every property lives under `brekquater.api`. Your IDE autocompletes them from the metadata in the jar.

A value the starter cannot use stops the application at startup. The startup report names the property, the value, and how to fix it.

## Properties

| Property | Default | What it does | Security impact |
|---|---|---|---|
| `brekquater.api.enabled` | `true` | Whether the starter is active at all. `false` registers nothing. | None. |
| `brekquater.api.panel.mode` | *unset* | Who may see the assessment: `disabled`, `authenticated` or `public`. When unset, it is `public` while the `dev` or `local` profile is active, and `disabled` otherwise. | **High.** The assessment lists every operation of your API and its weaknesses. Never set `public` on an environment that untrusted people can reach. See [Exposure](#exposure). |
| `brekquater.api.panel.base-path` | `/brekquater/api` | Path under which the assessment is served. The JSON endpoint is `{base-path}/assessment`. | Low. Changing the path is not a protection; use the mode for that. |
| `brekquater.api.rules.disabled` | *empty* | Comma-separated list of rule IDs that do not run, for example `BQ-DES-001,BQ-NAM-003`. An unknown ID stops startup. | Disabling a security rule (`BQ-SEC-*`) hides security findings. |
| `brekquater.api.rules.severity-overrides.<RULE-ID>` | *none* | Severity to use instead of a rule's own: `BLOCKER`, `MAJOR`, `MINOR` or `INFO`. Only security rules can be raised to `BLOCKER`. | Lowering a security rule's severity lowers its weight in the score and the security rating. |
| `brekquater.api.score.weights.blocker` | `10` | Weight of BLOCKER rules in the score. | None. |
| `brekquater.api.score.weights.major` | `7.5` | Weight of MAJOR rules. | None. |
| `brekquater.api.score.weights.minor` | `5` | Weight of MINOR rules. | None. |
| `brekquater.api.score.weights.info` | `0` | Weight of INFO rules. `0` keeps them out of the score. | None. |
| `brekquater.api.score.grades.a` | `90` | Lowest score for grade A. | None. |
| `brekquater.api.score.grades.b` | `75` | Lowest score for grade B. | None. |
| `brekquater.api.score.grades.c` | `60` | Lowest score for grade C. | None. |
| `brekquater.api.score.grades.d` | `40` | Lowest score for grade D. Anything lower is F. | None. |

Weights must be 0 or more. Grade minimums must be between 0 and 100, with `a >= b >= c >= d`.

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

- **Inside Swagger UI**, as a collapsed "API maturity" bar between the API title and the operations. Click it to see category scores and the findings, grouped by severity. It follows Swagger UI's light and dark switch.
- **On a page of its own** at `{base-path}`, by default `/brekquater/api`. This page works without Swagger UI, so it is also the fallback if the panel cannot be added to Swagger UI.

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
