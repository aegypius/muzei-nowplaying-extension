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
there: `just check` owns those, where taking a minute is acceptable.

This is not decoration. `cog` derives the next semantic version from commit types
and generates `CHANGELOG.md` from them, so a lazily-typed message is a lost
changelog entry.

Write the *why* in the body, especially when removing something. The single most
useful artefact in this project's predecessor was a changelog that explained why
features were deleted; that is where several of the ADRs here came from.

## Releasing

`cog bump` — with `--auto`, or `--patch` / `--minor` / `--major` — updates
`CHANGELOG.md`, writes the new version into `version.properties` through a
pre-bump hook, commits both and creates a tag.

`version.properties` is generated output for the version name. Do not edit that
line by hand; edit commit messages instead. The `codeEpoch` constant in the same
file *is* hand-maintained, and must never be raised — see
[ADR-0005](./docs/adr/0005-elapsed-seconds-version-code.md).

Bumping does not build anything. Every build is release-signed and installable
already, so a bump only records that the semantic version moved. Building and
serving are separate recipes.

To get a build onto a phone: build, then serve the output directory, then let
Obtainium pull from it. The served directory is pruned to the last few builds so
there is always a rollback target.

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
