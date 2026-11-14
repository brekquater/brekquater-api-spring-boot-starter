# BQ-VUL-001: No Critical known vulnerability in dependencies

| | |
|---|---|
| **Severity** | BLOCKER |
| **Category** | SECURITY |
| **Checks** | The dependency vulnerability report lists no advisory in the **Critical** band (CVSS base score 9.0 to 10.0). |
| **Reads** | An OSV-Scanner or OWASP Dependency-Check JSON report from your build. See [reports](../reports.md). |

## Why it matters when you scale

Most of the code an API runs is in its dependencies. A Critical vulnerability is usually exploitable over the network without credentials or user interaction. It is the kind of issue that is attacked within days of being published, so it blocks: the security rating drops to E until it is fixed.

The starter never looks vulnerabilities up itself: it reads the report your build already wrote, so nothing about your dependencies leaves your machine. When no report is found, this rule is skipped and the response says how to produce one.

## Example

Fails, in OSV-Scanner JSON:

```json
{"results": [{"packages": [{
  "package": {"name": "com.example:parser", "version": "1.0.0", "purl": "pkg:maven/com.example/parser@1.0.0"},
  "groups": [{"ids": ["GHSA-aaaa-bbbb-cccc"], "aliases": ["CVE-2099-0001"], "max_severity": "9.8"}]
}]}]}
```

Passes: a report with no advisory in the Critical band, including an empty one.

The finding points at the dependency's Package URL, names the advisory (the CVE ID when there is one) and its score, and gives the first fixed version when the scanner knows it.

## How to fix it in Spring

Upgrade the dependency to the fixed version. For a dependency that Spring Boot manages, override its version property, for example:

```xml
<properties>
  <snakeyaml.version>x.y.z</snakeyaml.version>
</properties>
```

For any other transitive dependency, pin it in `<dependencyManagement>`. Then run the scanner again, so the report is current.

## How to suppress it

Accept a specific advisory in the scanner itself, so the decision is recorded next to the evidence: an `osv-scanner.toml` `[[IgnoredVulns]]` entry with a reason, or a Dependency-Check suppression file. Suppressed advisories are not read.

To turn the rule off for the whole application:

```yaml
brekquater:
  api:
    rules:
      disabled: [BQ-VUL-001]
```

## Based on

- FIRST [CVSS v4.0 specification](https://www.first.org/cvss/v4-0/specification-document), qualitative severity rating scale (the same bands as CVSS v3.1).
- [OWASP Top 10 A06:2021, Vulnerable and Outdated Components](https://owasp.org/Top10/A06_2021-Vulnerable_and_Outdated_Components/), and [OWASP API Security Top 10 2023](https://owasp.org/API-Security/editions/2023/en/0x11-t10/), API8 Security Misconfiguration (cited, not copied).

The wording and the implementation of this rule are original.
