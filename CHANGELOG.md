# Changelog

All notable changes to this project are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- The panel inside Swagger UI is redesigned: a score card with the grade in words, coloured category scores, findings in one list (most severe first) with filters for severity and category and a search box, coverage bars, and a **Refresh** button.
- The page at `/brekquater/api` is now a dashboard: operations listed by tag with their finding counts, a view per operation, and a theme switch (System, Light, Dark).

### Added

- `{base-path}/openapi` serves the assessed OpenAPI document to the dashboard, under the same mode and access check as the assessment.

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
