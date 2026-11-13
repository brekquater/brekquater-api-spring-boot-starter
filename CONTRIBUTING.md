# Contributing

Thank you for helping. This project is small on purpose, and contributions that keep it small are the most welcome: a well-tested rule, a false-positive fix, or clearer docs.

By taking part you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Build

You need a JDK (17, 21 or 25). Maven comes from the wrapper.

```bash
./mvnw verify
```

That command is the main check. It must pass before you open a pull request.

To run the demo application:

```bash
./mvnw install -DskipTests
```

```bash
./mvnw -pl brekquater-api-demo spring-boot:run
```

Then open `http://localhost:8080/swagger-ui.html`, and `http://localhost:8080/brekquater/api/assessment` for the assessment. The demo starts with the `dev` and `good` profiles. To see an API that breaks the rules:

```bash
./mvnw -pl brekquater-api-demo spring-boot:run -Dspring-boot.run.profiles=dev,bad
```

## Sign your commits (DCO)

This project uses the [Developer Certificate of Origin](https://developercertificate.org/) instead of a contributor licence agreement. Add a sign-off line to every commit:

```bash
git commit -s -m "Add rule for missing pagination"
```

CI rejects pull requests that contain commits without a `Signed-off-by:` line.

## Code rules

- Small functions (about 5 to 15 lines, at most 3 parameters) and small classes. Names that explain intent; no `Util`, `Helper` or `Manager` classes.
- Immutable data. No `null` in public APIs.
- Every source file starts with the SPDX header. A test checks this:

  ```java
  // SPDX-License-Identifier: Apache-2.0
  // Copyright Brekquater
  ```

- Comments explain why, never what. No commented-out code.
- Everything not meant for users is package-private.
- `brekquater-api-rules` never depends on Spring, and rules read the document only through `OpenApiContext`. Architecture tests enforce both.
- Tests describe behaviour in their names and use AssertJ.
- Ask in an issue before you change a public contract: a configuration property, the `Rule` interface, or the JSON schema.

## Add a rule in 5 steps

1. **Open a rule request** issue, so the check and its public source can be agreed first. A rule checks exactly one thing.
2. **Write the tests first**: a passing, a failing and an edge-case OpenAPI fixture, as inline YAML text blocks.
3. **Add one rule class** in `com.brekquater.api.rules.catalogue`, and add its name to `META-INF/services/com.brekquater.api.rules.Rule`. A rule that checks each operation on its own extends `OperationRule` and only implements `check`; see `OperationHasSummaryOrDescription`. A rule with other targets (responses, paths, security entries) extends `OpenApiRule<T>`; see `ResponseContentHasSchema`.
4. **Add the doc page** `docs/rules/<RULE-ID>.md`: why it matters, bad and good YAML, the Spring fix, how to suppress it, and a "Based on" link.
5. **Run `./mvnw verify`**. The build fails if the rule is not registered, has no doc page, or touches the OpenAPI library directly instead of going through `OpenApiContext`.

Rule wording must be original. Cite the source of the idea; never copy text from another ruleset or guideline.

## Reporting security issues

Do not open a public issue. See [SECURITY.md](SECURITY.md).
