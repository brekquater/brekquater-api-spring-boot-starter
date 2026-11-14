# Sample build reports

The demo's `bad` profile reads the reports in `bad/`, and the demo's tests read both folders, so the assessment of build evidence can be shown and tested without running a scanner.

They are **made up**. The packages under `com.example.legacy` do not exist, and the advisory IDs (`CVE-2099-…`, `GHSA-demo-…`) are not real advisories. The files follow the formats that JaCoCo 0.8.16, Surefire 3.5 and OSV-Scanner v2.7.1 write.

The `good` profile reads the demo's own reports from `target/` instead. To produce them, run `./mvnw verify` in the repository root; see [docs/reports.md](../../docs/reports.md) for vulnerability reports.
