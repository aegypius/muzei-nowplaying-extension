---
status: accepted
date: 2026-08-16
---

# Build in the toolchain image this repository publishes, check beside it

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

Chosen: **publish the toolchain to GHCR and run the CI jobs in it**, referenced by
digest. A workflow builds `Containerfile`'s `toolchain` target when that file
changes, pushes it, and commits the resulting digest to `.github/toolchain-image`.
Jobs then run in that exact image.

Rebuilding the image per run was rejected as slow: it is 1.57 GB and downloads the
Android SDK before the first test runs. The runner's own SDK was rejected because
it makes "the toolchain is pinned" false precisely where it matters.

The digest is committed rather than printed for a human to copy. An image published
but not pinned is an image nothing uses, and the difference is invisible from the
outside; a commit changing that line is the record of which toolchain built what.
This is also why a mutable `:latest` tag is not consumed, though one is pushed so
the package page shows something readable — a moving tag means the toolchain can
change between two runs of the same commit, with nothing in the history saying so.

### Amended during implementation: the checks run beside the image, not in it

The decision above was "run the jobs in it", and the tests do. The checks do not,
and that is a departure from what was decided, accepted on review on 2026-08-16.

It was found by measurement rather than chosen up front: the published image has no
`python3` and no `git`, which the TOML, XML and codeEpoch checks need, and neither
`lefthook`, `hadolint` nor `cog` is installed there. Putting the checks in the image
therefore meant adding five tools to an image every build pulls, for tools no build
uses. The developer machine runs those checks from the host as git hooks, so the
image was never their home either.

What the original decision was protecting is kept: the check *definitions* are not
duplicated, because CI runs `lefthook run pre-commit --all-files` against the same
`lefthook.toml` the hooks use. What is given up is version parity by construction —
the tool versions are pinned in the workflow, by hand, to the ones installed
locally.

## Consequences

* Good, because a test failing in CI fails the same way locally, in the same image.
* Good, because a check added to `lefthook.toml` is enforced in CI with no second
  edit, and cannot drift out of two files.
* Good, because nothing here needs a secret of this project's own. The only token
  is the automatic `GITHUB_TOKEN`, which a private repository's package cannot be
  pulled without.
* Bad, because the toolchain workflow pushes to `main`. It is the narrowest write
  the scheme needs, and it runs only when the `Containerfile` changes, but it is a
  workflow with `contents: write` and that is worth knowing.
* Bad, because the check tool versions are pinned in a second place. `lefthook.toml`
  defines *what* runs; the workflow decides *which build* of each tool runs, and
  upgrading locally without upgrading there is a way for the two to disagree.
* Bad, because one rule now has two callers. `code-epoch-not-raised` cannot fail in
  CI, because it compares the staged `version.properties` against `HEAD`, and a
  checkout makes those the same blob. CI therefore compares two commits instead. The
  rule itself is in `tools/check-code-epoch.sh` and both callers use it, so only the
  pair of objects differs — but a reader must know that two callers exist.
* Bad, because the digest commit does not itself get verified. A push made with the
  automatic `GITHUB_TOKEN` does not trigger workflows, by design, so the commit
  pinning a new toolchain is first exercised by whatever is pushed after it.
* Bad, because the test job runs a pull request's own Gradle scripts inside the
  container, with a token that can read packages. The repository is private, so that
  is limited to people who could push anyway, but making it public would change the
  calculation.
* Bad, because the test job checks out through the REST API rather than cloning:
  the image has no `git`. The build does not need `.git` — the version comes from
  `version.properties` — but anything later that does would have to add `git` to
  the image or move.

## Confirmed

Both workflows have run. The image workflow published the toolchain and committed
its own digest, unattended. A push to `main` then ran green: the checks in 8
seconds, the tests in 2m37s inside that digest.

Red was confirmed deliberately rather than assumed, on a throwaway pull request
carrying two planted defects — trailing whitespace and a failing test. Both jobs
failed, each for its own reason, which is what distinguishes a check from a
decoration. The pull request was closed unmerged and the branch deleted.

The `codeEpoch` rule was proved the same way, on a second throwaway pull request
that raised the value and was committed with `--no-verify`. CI refused it, naming
the old and new values. The build refused it as well, independently: the epoch was
in the future, so the computed `versionCode` would have been negative. Two gates
caught one mistake, which is the right number for a mistake that is only
recoverable by uninstalling the app.

See [ADR-0007](./0007-per-runtime-container-flags.md) for why the image is built
the way it is. Signing and publishing an installable build is a separate workflow,
not yet written, and is the one that will hold a secret.
