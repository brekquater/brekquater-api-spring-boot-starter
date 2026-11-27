# Changelog

All notable changes to this project are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- 9 rules, checked against what springdoc generates for real controllers:
  - design: a `200` has a body, otherwise `204` (`BQ-DES-005`); no two paths differ only in variable names (`BQ-DES-006`); `204`, `205` and `304` have no body (`BQ-DES-007`); `PATCH` uses JSON Merge Patch or JSON Patch (`BQ-DES-008`); schema patterns are anchored with `^` and `$` (`BQ-DES-009`);
  - documentation: no `*/*` media types (`BQ-DOC-008`);
  - naming: property names share one case style (`BQ-NAM-005`);
  - security: no OAuth password flow (`BQ-SEC-010`, a blocker, from RFC 9700) and no sensitive Actuator endpoint in the document (`BQ-SEC-011`).
- Rules can read request body media types, every schema with its location, and OAuth flows (`ApiOperation.requestContent()`, `OpenApiContext.schemas()`, `OpenApiContext.oauthFlows()`).
- 10 security rules, checked against what springdoc generates for real `@SecurityScheme` and `@SecurityRequirement` annotations:
  - authentication: HTTP Basic is not the only login (`BQ-SEC-003`); no API key in the query string (`BQ-SEC-004`); no credentials in path or query parameters (`BQ-SEC-005`); no Negotiate or OAuth 1.0 scheme (`BQ-SEC-015`); HTTP schemes are IANA-registered, which catches `scheme: JWT` (`BQ-SEC-016`);
  - OAuth: no implicit flow (`BQ-SEC-012`, from RFC 9700); required scopes are declared (`BQ-SEC-013`); flow URLs use https (`BQ-SEC-014`);
  - transport and data: declared servers use https (`BQ-SEC-006`); no password or secret is returned unless write-only (`BQ-SEC-017`).
- Rules can read security schemes, the scopes of each security requirement, the scopes and URLs of OAuth flows, the `servers` list, and whether a schema refers to another one or is write-only (`OpenApiContext.securitySchemes()`, `servers()`, `ApiSecurityRequirement.scopes()`, `ApiOAuthFlow.scopes()`, `ApiSchemaNode.writeOnly()`).
- 8 schema rules, checked against what springdoc generates for real annotations:
  - design: a format fits its type (`BQ-DES-010`); enum values match the type (`BQ-DES-011`) and are unique (`BQ-DES-012`); a discriminator's property is defined and required (`BQ-DES-013`); every property is typed (`BQ-DES-014`);
  - naming: no two properties of an object differ only in case (`BQ-NAM-006`);
  - documentation: examples and defaults of schemas (`BQ-DOC-009`), and examples of parameters and JSON bodies (`BQ-DOC-010`), are valid against their schema.
- A new dependency, the [networknt JSON Schema validator](https://github.com/networknt/json-schema-validator) 3.0.9 (Apache-2.0), validates those examples. It brings `com.ethlo.time:itu` and SLF4J; its YAML reader is excluded. It never fetches a `$ref`.
- 9 breaking-change rules (`BQ-EVO-001` to `BQ-EVO-009`), which compare the OpenAPI document with the one of your last release, committed as `openapi-baseline.json` (JSON or YAML; set another path with `brekquater.api.reports.baseline`):
  - no operation removed without being deprecated first (`BQ-EVO-001`); no parameter (`BQ-EVO-002`), request body or request property (`BQ-EVO-003`) became required; no required response property removed (`BQ-EVO-004`); no request enum value removed (`BQ-EVO-005`); no success status removed (`BQ-EVO-006`); no incompatible type change (`BQ-EVO-007`);
  - a breaking change raises the major version (`BQ-EVO-008`); an `x-sunset` does not move earlier (`BQ-EVO-009`).
- A new input, `BASELINE`, in the assessment's `inputs` list, and the panel lists the baseline's age with the other reports.
- Rules can read the changes since the baseline (`AssessmentContext.changes()`, `ApiChanges`, `ApiChange`).
- 7 versioning rules, checked against what springdoc 3 generates for Spring's own API versioning and for hand-written version prefixes:
  - `info.version` is `MAJOR.MINOR.PATCH` or a date (`BQ-DOC-011`);
  - the API uses one versioning mechanism, a path segment, a header or a query parameter (`BQ-DES-015`); path versions name the major version only (`BQ-NAM-007`) and sit at the same position (`BQ-DES-016`); when several majors coexist, the older ones are deprecated (`BQ-DES-017`, INFO);
  - a deprecated operation says what replaces it (`BQ-DOC-012`), and an `x-sunset` is a date (`BQ-DOC-013`).
- Rules can read whether an operation is deprecated and its `x-sunset` (`ApiOperation.deprecated()`, `ApiOperation.sunset()`).
- Rules can read what each schema accepts and its already validated examples (`ApiSchemaNode.shape()`, `OpenApiContext.examples()`).
- `{base-path}/openapi` serves the assessed OpenAPI document to the dashboard, under the same mode and access check as the assessment.

### Changed

- The panel inside Swagger UI is now a column on the right of the operations on screens at least 1200 pixels wide; narrower screens keep the collapsible bar under the title. It is also redesigned: a score card with the grade in words, coloured category scores, findings in one list (most severe first) with filters for severity and category and a search box, coverage bars, and a **Refresh** button.
- The panel and the dashboard use the Brekquater brand: ivory and charcoal surfaces, gold accents, section labels in spaced capitals, and the Brekquater mark. Severity and score colours are unchanged.
- The page at `/brekquater/api` is now a dashboard: operations listed by tag with their finding counts, a view per operation, and a theme switch (System, Light, Dark).

## [0.1.0] - 2026-11-21

The first release. Everything below is new.

### Added

**In your application**

- One dependency, `brekquater-api-spring-boot-starter`, for Spring Boot 4.1 with Spring MVC and springdoc-openapi 3.2, on Java 17, 21 and 25.
- An API maturity panel inside Swagger UI: the score, a grade, a security rating, a score per category, and the findings grouped by severity, each with its location, a Spring-specific fix and a link to its rule page. Light and dark themes.
- The same panel as a page of its own at `/brekquater/api`, and the assessment as versioned JSON (`schemaVersion` 1) at `/brekquater/api/assessment`.
- Scoring as a weighted pass rate in the style of the OpenSSF Scorecard, with categories that could not be assessed left out instead of counted as perfect. Any blocker finding caps the grade at F.
- Strict mode (`brekquater.api.strict=true`): blocker findings stop startup before the web server accepts connections, so a `@SpringBootTest` fails in CI. Off by default.

**Rules**

- 25 rules, each with a documentation page: design (`BQ-API-001` to `005`, `BQ-DES-001`, `002`), documentation (`BQ-DOC-001` to `004`), security (`BQ-SEC-001`, `002`), errors (`BQ-ERR-001`, `002`), naming (`BQ-NAM-001` to `003`), testing (`BQ-TST-001` to `003`) and known vulnerabilities (`BQ-VUL-001` to `004`). See `docs/rules/index.md`.
- Opt-outs per operation with `x-brekquater-ignore`, and per application by rule ID.
- Your own rules: declare a `Rule` bean and it runs next to the built-in ones.

**Build evidence**

- Reads the JaCoCo, Surefire, Failsafe, OSV-Scanner and OWASP Dependency-Check reports your build already writes, from Maven's default locations or configured paths. It never calls a vulnerability database itself. See `docs/reports.md`.

**Safety**

- The panel and endpoints exist only in the `dev` and `local` profiles unless `brekquater.api.panel.mode` says otherwise. `authenticated` mode checks Spring Security's context without registering or changing a filter chain.
- A startup warning when Swagger UI or a public assessment is reachable outside those profiles.
- No outbound network calls, no telemetry, no stack traces or secrets in responses. Report files are named by file name only.

**Configuration**

- `brekquater.api.*` properties with IDE metadata and validation: an invalid value stops startup with a report that names the property and the fix. See `docs/configuration.md`.

**Project**

- A demo application whose `good` profile passes every rule and whose `bad` profile breaks every rule a springdoc application can break, with made-up sample reports.
- Documentation site, CI on Java 17, 21 and 25, a DCO check, OpenSSF Scorecard, Dependabot, and a release workflow that signs and uploads to Maven Central only on an approved, tagged run.

### Security

- Built against Jackson 3.3.0 and 2.23.0, which include the fix for CVE-2026-110879 (CVSS 9.8, fixed in 3.1.8 and 2.21.8). Spring Boot 4.1.2 still manages 3.1.7 and 2.21.7, and an application's own Spring Boot version decides which Jackson it runs. Until a Spring Boot release manages the fixed versions, set them in your build, for example with the properties `jackson-bom.version` and `jackson-2-bom.version` when you use `spring-boot-starter-parent`.

[Unreleased]: https://github.com/brekquater/brekquater-api-spring-boot-starter/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/brekquater/brekquater-api-spring-boot-starter/releases/tag/v0.1.0
