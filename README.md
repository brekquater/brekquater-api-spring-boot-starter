# Brekquater API Spring Boot Starter

[![CI](https://github.com/brekquater/brekquater-api-spring-boot-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/brekquater/brekquater-api-spring-boot-starter/actions/workflows/ci.yml)
[![Licence](https://img.shields.io/badge/licence-Apache--2.0-blue.svg)](LICENSE)

Add one dependency, run your app, open Swagger UI, and see what can be improved in your API design, plus a maturity score.

It warns; it does not block. (Unless you ask it to: see [strict mode](#strict-mode).)

![The Brekquater dashboard: operations with their finding counts, the findings with filters, and a score card with category scores and build evidence](docs/assets/panel-light.png#gh-light-mode-only)
![The Brekquater dashboard: operations with their finding counts, the findings with filters, and a score card with category scores and build evidence](docs/assets/panel-dark.png#gh-dark-mode-only)

<!-- A short GIF of the Swagger UI flow (open the panel, expand a finding, follow the fix) will replace this screenshot. -->

## Status

**Early release.** `0.1.0` is on Maven Central. Until `1.0`, the configuration properties and the JSON schema can still change between minor versions; [CHANGELOG.md](CHANGELOG.md) lists every change.

## Quick start

You need a Spring Boot 4.1 application that uses Spring MVC, and a JDK 17 or later.

1. Add the starter to your application. It brings springdoc-openapi and Swagger UI with it if you do not have them yet:

   ```xml
   <dependency>
     <groupId>com.brekquater</groupId>
     <artifactId>brekquater-api-spring-boot-starter</artifactId>
     <version>0.1.0</version>
   </dependency>
   ```

2. Run your application with the `dev` profile:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. Open `http://localhost:8080/swagger-ui.html` and click **API maturity** under the title.

No application of your own at hand? The repository's demo breaks every rule on purpose. Clone the repository, then from its root:

```bash
git clone https://github.com/brekquater/brekquater-api-spring-boot-starter.git
cd brekquater-api-spring-boot-starter
./mvnw -pl brekquater-api-demo spring-boot:run -Dspring-boot.run.profiles=dev,bad
```

## What you get

- **A panel inside Swagger UI** with the score, a grade, a security rating, a score per category, and the findings grouped by severity. Each finding says where the problem is, how to fix it in Spring, and links to its rule page.
- **The same panel on a page of its own** at `/brekquater/api`, and the assessment as JSON at `/brekquater/api/assessment`.
- **25 rules** across design, documentation, security, errors, naming and testing. See the [rule catalogue](https://starter.brekquater.com/api/rules/).
- **Build evidence.** If your build writes JaCoCo, Surefire or Failsafe reports, or an OSV-Scanner or OWASP Dependency-Check report, the starter reads them and checks coverage, failing tests and known vulnerabilities too. It reads files only; it never calls a vulnerability database. See [build reports](docs/reports.md).

The score is a weighted pass rate from 0 to 100, in the style of the OpenSSF Scorecard. Categories with nothing to assess are shown as "not assessed" and left out of the score, rather than counted as perfect. Any blocker finding caps the grade at F.

## Safe by default

- The panel and the endpoints exist only while the `dev` or `local` profile is active. Elsewhere nothing is registered, unless you set `brekquater.api.panel.mode`.
- `authenticated` mode shows the assessment to signed-in users only. The starter checks this itself and never registers or changes a `SecurityFilterChain`.
- No outbound network calls, no telemetry, and no stack traces or secrets in any response.
- The assessment runs on the first request and is cached until the document or a build report changes, not on every page load.

See the [configuration reference](docs/configuration.md) for every property.

## Strict mode

To fail fast in CI, turn on strict mode:

```yaml
brekquater:
  api:
    strict: true
```

The application then refuses to start while any blocker finding exists, such as an operation without security or a dependency with a Critical vulnerability. The check runs before the web server accepts connections, so a `@SpringBootTest` fails the build, and the startup report lists each blocker and how to fix it. It is off by default.

## How this differs from other tools

Good OpenAPI linters already exist, and this starter does not replace them:

- **[Spectral](https://github.com/stoplightio/spectral)** is the de facto OpenAPI linter and ruleset format. It runs as a separate tool, typically in CI.
- **[vacuum](https://github.com/daveshanley/vacuum)** is a fast, Spectral-compatible linter with a 0 to 100 score and an HTML report. It is a separate binary.
- **[Zally](https://github.com/zalando/zally)** checks APIs against Zalando's guidelines and runs as its own server.
- **[Rate My OpenAPI](https://ratemyopenapi.com/)** scores documentation, SDK generation, security and completeness; its CLI needs a Zuplo API key.
- **[OpenAPI Style Validator](https://github.com/OpenAPITools/openapi-style-validator)** is a Java library with a Maven plugin.

The difference is where it lives: inside your Spring Boot application and its Swagger UI, with no setup beyond one dependency. Its findings are Spring-aware: it knows what springdoc generates, and its fixes are annotations and code, not YAML edits. Teams that already run Spectral in CI can keep it, and use this starter for feedback while they code.

## Compatibility

| Brekquater API starter | Spring Boot | springdoc-openapi | Java |
|---|---|---|---|
| 0.1.0 | 4.1.x | 3.2.x | 17, 21, 25 |

Spring MVC only. WebFlux and Spring Boot 3.x are not supported.

## Modules

| Module | Purpose |
|---|---|
| `brekquater-api-spring-boot-starter` | The one dependency you add. It contains no code. |
| `brekquater-api-spring-boot` | Auto-configuration, endpoint, panel and report reading. |
| `brekquater-api-rules` | Rules, rule engine and scoring. Pure Java, no Spring. |
| `brekquater-api-demo` | Sample application and integration test. Not published. |

## Contributing

Contributions are welcome, especially well-tested rules and false-positive fixes. See [CONTRIBUTING.md](CONTRIBUTING.md); commits need a DCO sign-off. Report security issues privately, as described in [SECURITY.md](SECURITY.md).

## Licence and trademark

The code is licensed under the [Apache License 2.0](LICENSE). The Brekquater name and logo are trademarks of Brekquater and are not covered by that licence.
