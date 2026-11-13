# Security policy

## Reporting a vulnerability

Please report vulnerabilities privately. Do not open a public issue.

- Preferred: use GitHub's [private vulnerability reporting](https://github.com/brekquater/brekquater-api-spring-boot-starter/security/advisories/new) for this repository.
- Alternative: email `security@brekquater.com`.

Include the affected version, what an attacker could do, and steps to reproduce. Please do not include real secrets or production data.

We aim to acknowledge a report within 5 working days. We will keep you informed while we work on a fix, and we will credit you in the release notes unless you ask us not to.

## Supported versions

The project has not made its first release yet. After `0.1.0`, security fixes go into the latest `0.x` release.

## What this library does to stay safe

- The panel and the assessment endpoint are enabled only in the `dev` and `local` profiles unless you enable them explicitly.
- The library makes no outbound network calls and collects no telemetry.
- The library never registers or changes a Spring Security filter chain.
- Responses never contain stack traces, environment values or secrets.

A deviation from any of these is a vulnerability. Please report it.
