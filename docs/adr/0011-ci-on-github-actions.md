---
status: accepted
date: 2026-08-16
---

# Run CI in the toolchain image this repository publishes

## Context and Problem Statement

Everything so far was verified on one machine, by hand. Putting the repository on
GitHub raises a question the local setup never had to answer: what does CI build
with? The whole point of containerising the build was that the toolchain is pinned
and reproducible, and a CI that ignores that image gives up the property in the one
place it is hardest to debug.

## Decision Drivers

* A CI failure that cannot be reproduced locally is the expensive kind.
* The checks already exist, in `lefthook.toml`. A second copy would drift.
* The repository is private, so Actions minutes are a real budget.

## Considered Options

* **Publish the toolchain to GHCR and run jobs in it**, referenced by digest.
* **Build the Containerfile in every run.**
* **`ubuntu-latest` with the runner's preinstalled JDK and Android SDK.**

## Decision Outcome

Chosen: **publish the toolchain to GHCR and pin it by digest**. A workflow builds
`Containerfile`'s `toolchain` target when that file changes, pushes it, and commits
the resulting digest to `.github/toolchain-image`. The test job runs in that exact
image.

Rebuilding the image per run was rejected as slow: it is 1.57 GB and downloads the
Android SDK before the first test runs. The runner's own SDK was rejected because
it makes "the toolchain is pinned" false precisely where it matters.

The digest is committed rather than printed for a human to copy. An image published
but not pinned is an image nothing uses, and the difference is invisible from the
outside; a commit changing that line is the record of which toolchain built what.
This is also why a mutable `:latest` tag is not consumed, though one is pushed so
the package page shows something readable — a moving tag means the toolchain can
change between two runs of the same commit, with nothing in the history saying so.

**The checks do not run in that image.** They run on the runner. This contradicts
the obvious symmetry and is deliberate: the image has no `python3` and no `git`,
which the TOML, XML and codeEpoch checks need, and neither does `lefthook` or
`hadolint` live there. Adding four tools to an image every build pulls, for tools
no build uses, costs more than it buys — the developer machine runs those checks
from the host too, as git hooks, so the image was never their home. What matters is
that the *definitions* are not duplicated: CI runs `lefthook run pre-commit
--all-files` against the same `lefthook.toml` the hooks use, and the tool versions
are pinned in the workflow to the ones installed locally.

## Consequences

* Good, because a test failing in CI fails the same way locally, in the same image.
* Good, because a check added to `lefthook.toml` is enforced in CI with no second
  edit, and cannot drift out of two files.
* Good, because no job in this workflow uses a configured secret. The only token is
  the automatic `GITHUB_TOKEN`, needed to pull a package from a private repository.
* Bad, because the toolchain workflow pushes to `main`. It is the narrowest write
  the scheme needs, and it runs only when the `Containerfile` changes, but it is a
  workflow with `contents: write` and that is worth knowing.
* Bad, because the check tool versions are pinned in a second place. `lefthook.toml`
  defines *what* runs; the workflow decides *which build* of each tool runs, and
  upgrading locally without upgrading there is a way for the two to disagree.
* Bad, because the test job checks out through the REST API rather than cloning:
  the image has no `git`. The build does not need `.git` — the version comes from
  `version.properties` — but anything later that does would have to add `git` to
  the image or move.

See [ADR-0007](./0007-per-runtime-container-flags.md) for why the image is built
the way it is. Signing and publishing an installable build is a separate workflow,
not yet written, and is the one that will hold a secret.
