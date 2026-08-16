# Working on Now Playing

This is a one-person project with no remote. What follows is the runbook — how to
build, test, version and ship it — written mostly for whoever opens this repository
after six months away.

If that is about to stop being true and someone else is reading this: patches are
welcome, the conventions below are not negotiable because tooling depends on them,
and everything else is open to argument.

## Environment

The build runs in a container. On the host you need only `docker` or `podman`,
plus `just`. There is no Android SDK on the host and none is wanted — see
[AGENTS.md](./AGENTS.md) for why an agent should not try to run Gradle directly.

`just --list` is the authoritative list of recipes. This document explains the
workflow; it does not restate the commands, so it cannot drift out of date.

## The loop

Tests are plain JVM tests over the `:domain` module — JUnit 5, `kotlin.test`, and
`kotlinx-coroutines-test` for the publish path. They run in under a second, which
is what makes test-first viable here. `:domain` is a pure Kotlin library and
cannot depend on Android; if a test needs an Android type, the boundary is in the
wrong place.

Dependencies are passed through constructors and wired by hand in a container
object built by the application. There is no DI framework, so tests construct what
they need directly and pass fakes.

## Commits

Run `lefthook install` once per clone. It installs both hooks, and a malformed
commit message is then rejected as you write it — the only moment fixing one is
free. After the fact it means rewriting history.

Commits follow [Conventional Commits](https://www.conventionalcommits.org/). The
`commit-msg` hook runs `cog verify`; do not install cocogitto's own hook, because
`lefthook install` would rename it away. `lefthook.toml` is the single place hooks
are declared.

The `pre-commit` hook only runs checks that finish in well under a second —
conflict markers, trailing whitespace, final newlines, TOML and XML parsing, and
a refusal to commit signing material. Compilation and tests are deliberately not
there: `just test` owns those, where taking a minute is acceptable.

This is not decoration. `cog` derives the next semantic version from commit types
and generates `CHANGELOG.md` from them, so a lazily-typed message is a lost
changelog entry.

Write the *why* in the body, especially when removing something. The single most
useful artefact in this project's predecessor was a changelog that explained why
features were deleted; that is where several of the ADRs here came from.

## Continuous integration

Every push to `main` and every pull request runs the tests and the same checks the
hooks run. CI does not maintain its own list: it runs `lefthook run pre-commit
--all-files` against this repository's `lefthook.toml`, so a check added here is
enforced there without a second edit. It also runs `cog check` over the whole
history, which catches a message committed with the hook skipped.

The tests run inside the pinned toolchain image, published to the registry by a
separate workflow and referenced by digest in `.github/toolchain-image`, so CI
compiles with the image this repository builds rather than whatever a runner
happens to have. Change the `Containerfile` and that workflow republishes the image
and commits the new digest.

The checks run on the runner rather than in that image, because they need `python3`
and `git`, which a build toolchain has no other reason to carry. See
[ADR-0011](./docs/adr/0011-ci-on-github-actions.md).

## Signing

Every release build is signed, and there is no unsigned fallback: an unsigned APK
cannot be installed, so producing one would only waste a build. `just build` fails
at packaging with a message naming what is missing.

`just keystore` creates the key at `~/.config/nowplaying/release.jks`, prompting
for the password so it is never written down by anything but you. It refuses to
overwrite an existing key.

The password reaches the build one of two ways, and the build prefers the first:

- **`NOWPLAYING_KEYSTORE_PASSWORD` in the environment.** Nothing is written to disk.
  This is the path CI uses, injecting the secret from wherever it keeps them; how
  you populate it locally is your business and no concern of this repository.
- **`keystore.properties`**, gitignored and refused by a pre-commit hook. Simpler,
  at the cost of a plaintext password in the work tree.

`NOWPLAYING_KEYSTORE_ALIAS` overrides the alias, which otherwise defaults to
`nowplaying` — what `just keystore` creates. The justfile forwards both variables
into the container by name rather than by value, so a password never appears in a
command line where `ps` would show it.

A caveat if Gradle's configuration cache is ever enabled: the resolved password
would be written into the cache entry on disk, putting the plaintext back somewhere
less obvious than a properties file.

The key lives outside the work tree so no `git add` can reach it, and the justfile
bind-mounts it read-only into the build container. Override the location with
`NOWPLAYING_KEYSTORE` if you keep it elsewhere.

Back it up somewhere other than this machine. Obtainium updates in place only
while the signature is unchanged, so losing the key means uninstalling and
reinstalling by hand on every device.

## Releasing

`cog bump` — with `--auto`, or `--patch` / `--minor` / `--major` — updates
`CHANGELOG.md`, writes the new version into `version.properties` through a
pre-bump hook, commits both and creates a tag.

`version.properties` is generated output for the version name. Do not edit that
line by hand; edit commit messages instead. The `codeEpoch` constant in the same
file *is* hand-maintained, and must never be raised — see
[ADR-0005](./docs/adr/0005-elapsed-seconds-version-code.md).

`just release` runs the tests, bumps, and pushes the commit and the tag. CI then
signs the tag and publishes a GitHub Release, with the APK attached and the
changelog for that version as the body.

`cog bump` on its own stays local. It does not push, and there is no cog hook that
does, so running it by hand never reaches the remote. `just release` is the command
that means "publish this".

There are two ways to get a build onto a phone, and they coexist:

- **A release.** Obtainium tracks the repository and installs the Release asset.
- **An untagged build.** `just build`, then serve the output directory, then let
  Obtainium pull from it. The server publishes on all interfaces, but a host
  firewall can still block it — if the phone cannot reach the port, check that
  before suspecting Obtainium. The served directory is pruned to the last few
  builds so there is always a rollback target.

Both routes are safe to mix, because `versionCode` is elapsed seconds: a build made
later always outranks one made earlier, whichever machine made it.

## Decisions

When a choice is hard to reverse, surprising to a reader, and had a real
alternative, write an ADR in `docs/adr/` using [MADR](https://adr.github.io/madr/)
and record the rejected option and why. Several existing ADRs exist purely to stop
a future reader "fixing" something deliberate.

New domain vocabulary goes in [CONTEXT.md](./CONTEXT.md) as it is settled, not
afterwards. It is a glossary — no implementation details.

## The one rule that is not style

Nothing is copied out of `muzei/` or `MuzeiMusicExtension/` — not code, not
resources, not XML. Read them, then write it fresh.
[ADR-0006](./docs/adr/0006-written-fresh-not-a-fork.md) explains what pasting
would cost.
