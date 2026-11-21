---
title: "BQ-SEC-011: No sensitive Actuator endpoint"
description: "The document lists no Spring Boot Actuator endpoint other than health and info."
slug: /rules/BQ-SEC-011
rule_id: BQ-SEC-011
severity: MAJOR
category: SECURITY
reads: OPENAPI
---

**Checks:** The document lists no Spring Boot Actuator endpoint other than `health` and `info`: no path with an `actuator` segment followed by another endpoint, such as `/actuator/env` or `/actuator/heapdump`. A custom `management.endpoints.web.base-path` without an `actuator` segment is not detected.

## Why it matters when you scale

Endpoints such as `env`, `configprops`, `heapdump`, `threaddump`, `loggers` and `shutdown` reveal configuration and memory, or change the running application. Spring Boot exposes only `health` over HTTP by default, and springdoc leaves Actuator out of the document unless `springdoc.show-actuator=true`. When one appears in the API document, it is reachable and advertised to every reader.

## Example

Fails:

```yaml
paths:
  /actuator/env:
    get: ...
```

Passes:

```yaml
paths:
  /actuator/health:
    get: ...
```

## How to fix it in Spring

Expose only what you need, and keep Actuator out of the API document:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
springdoc:
  show-actuator: false
```

If operators need more endpoints, serve Actuator on its own port with `management.server.port` and protect it with Spring Security.

## How to suppress it

This rule reports paths, not operations, and `x-brekquater-ignore` only covers findings inside an operation. To exempt one, disable the rule for the whole application.

For the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-SEC-011]
```

## Based on

- Spring Boot reference, [Actuator endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html): endpoints may contain sensitive information, and only `health` is exposed over HTTP by default.
- springdoc-openapi [properties](https://springdoc.org/properties.html): `springdoc.show-actuator` is `false` by default.
- OWASP API Security Top 10 2023, [API8: Security Misconfiguration](https://owasp.org/API-Security/editions/2023/en/0xa8-security-misconfiguration/) (cited and linked only).

The wording and the implementation of this rule are original.
