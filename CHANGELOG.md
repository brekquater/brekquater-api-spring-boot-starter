# Changelog

All notable changes to this project are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Multi-module Maven build with the Maven Wrapper: `brekquater-api-rules`, `brekquater-api-spring-boot`, `brekquater-api-spring-boot-starter` and `brekquater-api-demo`.
- Demo application that serves a small orders API with Swagger UI.
- CI on Java 17, 21 and 25, a DCO sign-off check, OpenSSF Scorecard and Dependabot.
- Licence, notice and community files.
- Rule engine in `brekquater-api-rules`: the `Rule` SPI, deterministic and failure-isolated evaluation, `x-brekquater-ignore` suppression, risk-weighted pass-rate scoring with a security rating, and the JSON assessment report (`schemaVersion` 1).
- First rules, each with a documentation page: `BQ-DOC-001`, `BQ-ERR-001` and `BQ-API-001`.

- All 15 remaining core rules, each with a documentation page: `BQ-API-002` to `BQ-API-005`, `BQ-DES-001`, `BQ-DES-002`, `BQ-DOC-002` to `BQ-DOC-004`, `BQ-ERR-002`, `BQ-SEC-001`, `BQ-SEC-002`, `BQ-NAM-001` to `BQ-NAM-003`.
- Spring Boot auto-configuration: reads springdoc's document in-process, assesses it once per change, and serves the result at `/brekquater/api/assessment`.
- The assessment is served only in the `dev` and `local` profiles unless `brekquater.api.panel.mode` says otherwise; `authenticated` mode checks Spring Security's context without registering a filter chain.
- `brekquater.api.*` properties with IDE metadata, validation, and a startup failure report that names the property and the fix. See `docs/configuration.md`.
- Demo profiles: `good` passes every rule; `bad` breaks every rule a springdoc application can break.
- The assessment inside Swagger UI: a collapsible panel with the score, grade, security rating, category scores and findings grouped by severity, in light and dark themes. The same panel is served as a page of its own at `/brekquater/api`.
- A startup warning when Swagger UI or a public assessment is reachable outside the `dev` and `local` profiles.
- The panel keeps working when the application uses Spring's path-segment API versioning.

### Fixed

- The demo jar is now executable (`java -jar`).
