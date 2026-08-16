---
status: accepted
date: 2026-08-16
---

# Sign releases in CI on a tag, and publish them as GitHub Releases

## Context and Problem Statement

Getting a build onto the phone means running `just build` and `just serve` on this
machine, and leaving an HTTP server up while the phone fetches. That works, and
[ADR-0004](./0004-obtainium-distribution.md) explains why. It also means a release
exists only while one laptop is on.

A tag already marks a release: `cog bump` writes the version, the changelog, the
commit and the tag. Nothing acts on that tag. The question is whether CI should,
and what it costs.

## Decision Drivers

* An APK is installable only if it is signed with the key already on the phone.
* That key cannot be replaced. Obtainium installs an update in place only while the
  signature is unchanged.
* A release should not depend on which machine is switched on.

## Considered Options

* **CI signs on a tag**, with the key in GitHub Secrets.
* **CI builds unsigned; sign and publish locally.** The key never leaves here.
* **Keep the local HTTP route only.** No release artifacts at all.

## Decision Outcome

Chosen: **CI signs on a tag**. Pushing a `v*` tag runs the tests, decodes the key,
assembles a release APK, checks its certificate, and publishes a GitHub Release
with the APK attached and `cog changelog --at <tag>` as the body.

This puts a second copy of an unrotatable key on someone else's infrastructure.
That is the whole cost, it was accepted knowingly, and it is worth stating
precisely rather than softening:

* The key cannot be rotated. A compromise is permanent. Recovery means uninstalling
  and reinstalling by hand on every device, and anyone holding the key can sign an
  APK that Android accepts as an update to this app.
* Any code that runs in the signing job can read the key, including the release
  build's own Gradle plugins and their transitive dependencies.

**The rejected alternative was strictly safer**: CI builds unsigned, and you sign
locally and upload with `gh release upload`. One copy of the key, and CI still
proves the release build works. It was rejected for one reason — a release would
then need a manual step, and a release that needs a manual step is a release that
is sometimes not finished. That is a convenience argument beating a security
argument, which is worth admitting rather than dressing up.

The mitigations are therefore not optional, and each is in `release.yml`:

* The secrets live on a GitHub Environment, not on the repository, so a workflow
  running from a branch cannot read them.
* The key exists for one step. A `trap` removes it on the failing path too, so no
  later step — artifact upload, release upload, any third-party action — runs while
  it is on disk.
* It is written to `RUNNER_TEMP`, never the workspace, which `upload-artifact`
  globs and `actions/cache` would persist across runs.
* `umask 077` precedes the write.
* The secrets are on that step's `env`, not the job's, and are read from variables
  rather than interpolated into a command line.
* There is no `pull_request` trigger, and there must never be one.
* `gh release create` is used without `--clobber`, so it refuses a tag that already
  has a release. A second, different APK cannot replace one that is published.
* A final `shred` runs with `if: always()`. On a GitHub-hosted runner this is
  defence in depth rather than erasure — SSD wear levelling, thin provisioning and
  journaling all defeat an overwrite guarantee, and the virtual machine is destroyed
  regardless. It earns its place on a self-hosted runner, and on the paths where the
  step's own trap did not run.

The build also refuses to publish if the signing certificate does not match the one
already installed. A mismatch is not a bad release; it is a release nobody can
upgrade to.

## Delivery

GitHub Releases become a delivery channel. The local HTTP route stays, for builds
that are not tagged — which is how every device confirmation in this project has
been done.

Obtainium can track a private repository, which is what makes this possible. Read
from its source rather than assumed: for APK assets it prefers the API asset `url`
over `browser_download_url` (`lib/app_sources/github.dart`), and its GitHub source
sends `Authorization: Token <pat>` together with `Accept: application/octet-stream`.
That pair is exactly how GitHub serves a private release asset. A fine-grained,
read-only token goes in Obtainium's GitHub source configuration.

Mixing the two routes is safe because `versionCode` is elapsed seconds since a fixed
epoch ([ADR-0005](./0005-elapsed-seconds-version-code.md)): a build made later
always outranks one made earlier, whichever machine made it.

## Consequences

* Good, because a release no longer depends on this machine being switched on, and
  the tag is what starts it.
* Good, because `just release` stops building. CI checks out the tag, so it can only
  build the bumped version, which retires the ordering hazard the old recipe existed
  to work around.
* Bad, because the signing key exists in two places, and one of them is not yours.
* Bad, because the release job holds `contents: write` to create the Release.
* Bad, because `v*` tags are not protected. This was intended, and it is not
  available: a ruleset needs GitHub Pro on a private repository, and the older tag
  protection endpoint has been removed. So a tag can still be deleted or moved. Only
  the repository owner can push, so the risk is a forced push by the person cutting
  the release, and the refusal to clobber an existing release limits what that
  achieves. Making the repository public, or paying for Pro, would allow the rule.
* Bad, because the certificate check hard-codes a fingerprint. Deliberately changing
  the key means editing `release.yml`, which is the point, but a reader may mistake
  it for configuration.

## Amends ADR-0004

[ADR-0004](./0004-obtainium-distribution.md) chose an HTML directory index served
over HTTP, and reads as though that is the only route. It is now one of two.
Everything it says about signature stability, `versionCode` and Obtainium's link
sorting still holds, and still governs the HTTP route.
