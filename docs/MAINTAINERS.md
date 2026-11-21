# Maintainers' guide

This page lists what only a maintainer can do: steps that need accounts, keys, DNS or repository settings. The automation in this repository never does them. Each step says where it is used, so nothing is set up without a reason.

## Before the first release

### 1. Repository settings

On the `brekquater/brekquater-api-spring-boot-starter` repository:

- Protect `master`: require pull requests, require the `CI` checks to pass, and require review from code owners for changes under `.github/`.
- Enable secret scanning, push protection, Dependabot alerts and private vulnerability reporting (`SECURITY.md` points reporters to it).
- Require the DCO sign-off. The `dco` job in `ci.yml` already checks pull requests; make it a required check.
- Finish `.github/CODEOWNERS`: replace the placeholder owner on the `*` line with a real user or team.

### 2. The Maven Central namespace

1. Sign in to the [Central Publisher Portal](https://central.sonatype.com/) and register the namespace `com.brekquater`.
2. Verify it with the DNS TXT record the portal shows, on `brekquater.com`.
3. Generate a portal user token. It is a username and password pair for publishing; it is not your login.

### 3. The signing key

1. Create a GPG key for releases, for example with `gpg --full-generate-key` (RSA 4096 or Ed25519), using a release address such as `releases@brekquater.com`.
2. Publish the public key to `keys.openpgp.org`, so Central and users can check signatures.
3. Keep the private key and its passphrase out of every file in this repository.

### 4. The release environment

Create a GitHub environment named `release` with required reviewers, and add these secrets to it only (never as repository secrets):

| Secret | Value |
|---|---|
| `CENTRAL_USERNAME` | The portal token's username |
| `CENTRAL_PASSWORD` | The portal token's password |
| `GPG_PRIVATE_KEY` | The ASCII-armoured secret key (`gpg --armor --export-secret-keys KEY_ID`). The workflow signs with Maven's Bouncy Castle signer, so no `gpg` program is needed on the runner. |
| `GPG_PASSPHRASE` | Its passphrase |

### 5. The documentation site

1. Point `starter.brekquater.com` at GitHub Pages with a DNS `CNAME` record to `brekquater.github.io`.
2. In the repository's Pages settings, choose **GitHub Actions** as the source and set the custom domain to `starter.brekquater.com`, with HTTPS enforced.
3. Set the repository variable `DOCS_DEPLOY` to `true`. The `Docs` workflow then publishes on every push to `master`, and when you run it by hand on `master`. Until then it only builds the site, so broken links still fail pull requests.

The site is published under `/api/`, because every finding links to `https://starter.brekquater.com/api/rules/<RULE-ID>`. Those addresses must keep working for every released version. If another Brekquater starter later shares the domain, move the root redirect (`docs/root-index.html`) to a repository of its own.

### 6. The security mailbox

Create `security@brekquater.com`, which `SECURITY.md` names as the fallback contact.

## Releasing

The `Release` workflow has two modes, both started by hand from the Actions tab:

- **Dry run** (the default): builds jars, sources, Javadoc and CycloneDX SBOMs, signs and uploads nothing, and keeps the result as a workflow artifact. Run it on `master` before every release and look inside the artifact.
- **Publish** (tick *publish*): only on a `v*` tag whose version matches the POM. It runs in the `release` environment, so a reviewer approves it first. It signs, uploads to the Central Publisher Portal and stops at **validated**; nothing is public until you press **Publish** on the portal.

Before the first release, do steps 1 to 6 above. Then:

### Checklist for 0.1.0

1. Run the dry run on `master` and check the artifact: every published module (`brekquater-api-parent`, `-rules`, `-spring-boot`, `-spring-boot-starter`) has its jar or POM, `-sources.jar`, `-javadoc.jar` and SBOM; the demo is absent.
2. On a branch, set the release version and finish the changelog:

   ```bash
   ./mvnw versions:set -DnewVersion=0.1.0 -DgenerateBackupPoms=false
   ```

   - In `CHANGELOG.md`, rename `## [Unreleased]` to `## [0.1.0] - YYYY-MM-DD` and add an empty `## [Unreleased]` above it.
   - In `README.md` and `docs/index.md`, replace the build-from-source steps with the Maven Central dependency, version `0.1.0`, and change the compatibility table's first column to `0.1.0`.
3. Merge it with a signed-off commit, after CI passes.
4. Tag that commit and push the tag. Use the full commit ID of the merge, so the tag cannot land on another commit:

   ```bash
   git tag -a v0.1.0 <full commit ID> -m "Release 0.1.0"
   ```

   ```bash
   git push origin v0.1.0
   ```

   The tag is annotated but not GPG-signed (`-s`): `git tag -s` looks for a personal key for your Git email, and the release key is a different key. The workflow only checks the tag's name against the POM version.

5. Run the `Release` workflow on the tag with *publish* ticked, and approve the `release` environment.
6. On the [Central Publisher Portal](https://central.sonatype.com/publishing/deployments), open the validated deployment and check its *Component Coordinates* table: four artifact IDs and no demo. (The component summary lists five lines, because the parent's POM and its SBOM files appear twice.) Press **Publish** on the deployment's card, not **Publish Component** at the top of the page, which uploads a new bundle. It can take up to about 30 minutes to appear on Maven Central.
7. Verify it from an outside project: a fresh Spring Boot 4.1 application with only `com.brekquater:brekquater-api-spring-boot-starter:0.1.0` added, run with the `dev` profile, shows the panel in Swagger UI. This is the phase's exit check.

   Maven 3.10 refuses paths that are not on Central's list of top-level paths (`https://repo.maven.apache.org/maven2/.meta/prefixes.txt`), and that list is regenerated some time after a new group first publishes. For `0.1.0` it took about half an hour, and until then builds failed with `NOT allowed from central`. Check with `curl -s https://repo.maven.apache.org/maven2/.meta/prefixes.txt | grep brekquater`, and wait for a line to appear before you test.
8. Create the GitHub release for `v0.1.0`, with the changelog section as its notes.
9. On `master`, start the next version:

   ```bash
   ./mvnw versions:set -DnewVersion=0.2.0-SNAPSHOT -DgenerateBackupPoms=false
   ```

If the deployment fails validation, drop it on the portal, fix the cause on `master`, delete the tag, and start again from step 2. A version that was published can never be replaced; release the fix as `0.1.1`.

## After the release

- Announce it (blog, r/java, r/SpringBoot) and ask for feedback before building more.
- Record a short GIF of the Swagger UI flow to replace the screenshot in the README.
