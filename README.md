# Brekquater API Spring Boot Starter

[![CI](https://github.com/brekquater/brekquater-api-spring-boot-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/brekquater/brekquater-api-spring-boot-starter/actions/workflows/ci.yml)

Add one dependency, run your app, open Swagger UI, and see what can be improved in your API design, plus a maturity score.

It warns; it does not block.

## Status

**Pre-release.** The project is under active development and nothing is published to Maven Central yet. Today the starter assesses your API with 18 rules and shows the result inside Swagger UI, on its own page at `/brekquater/api`, and as JSON at `/brekquater/api/assessment`, in the `dev` and `local` profiles. Coverage and vulnerability reports and strict mode are next; see [CHANGELOG.md](CHANGELOG.md) for what has landed.

## What it will do

- Show API design findings inside your application's own Swagger UI, each with evidence, a fix and a link to a rule page.
- Show a maturity score, and say which quality categories were not assessed.
- Stay safe by default: enabled only in the `dev` and `local` profiles, no outbound network calls, no telemetry.

It does not replace Spectral, vacuum or Zally. Its difference is that it lives inside the Spring Boot app, with no extra setup.

## Compatibility

| Brekquater API starter | Spring Boot | springdoc-openapi | Java |
|---|---|---|---|
| 0.1.0 (in development) | 4.1.x | 3.2.x | 17, 21, 25 |

Spring MVC only. WebFlux, Gradle and Spring Boot 3.x are not supported.

## Build from source

```bash
./mvnw verify
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for how to run the demo and how to contribute, and [docs/configuration.md](docs/configuration.md) for every property.

## Modules

| Module | Purpose |
|---|---|
| `brekquater-api-rules` | Rules, rule engine and scoring. Pure Java, no Spring. |
| `brekquater-api-spring-boot` | Auto-configuration, endpoint and panel. |
| `brekquater-api-spring-boot-starter` | The one dependency you add. It contains no code. |
| `brekquater-api-demo` | Sample application and integration test. Not published. |

## Licence and trademark

The code is licensed under the [Apache License 2.0](LICENSE). The Brekquater name and logo are trademarks of Brekquater and are not covered by that licence.

Security issues: see [SECURITY.md](SECURITY.md).
