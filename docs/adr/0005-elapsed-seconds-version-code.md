---
status: accepted
date: 2026-08-14
---

# Derive versionCode from seconds elapsed since the project's first commit

## Context and Problem Statement

`versionCode` is a nine-digit number in the 200-millions that nobody chose, and
`version.properties` holds a `codeEpoch` constant that is never read by anything
a person edits. Both look like mistakes. They are load-bearing.

Every build is servable ([ADR-0004](./0004-obtainium-distribution.md)), so every
build must be distinguishable from the one before it, and the phone must be able
to tell which is newer. A number bumped by hand cannot satisfy that — it would
have to be edited on every build, including the ones made while iterating on
uncommitted changes.

## Decision Drivers

* `versionCode` must never decrease, or Android refuses the install as a
  downgrade and the only recovery is uninstalling.
* It must advance between two builds of the *same* commit, since that is what
  iterating looks like.
* It must not depend on state that a fresh clone or a shallow checkout loses.

## Considered Options

* **Elapsed seconds** since a fixed epoch.
* **A build counter** in a gitignored file.
* **Git commit count** — `rev-list --count HEAD`.
* **A ULID**, or another sortable unique identifier.
* **Manual** edits to `version.properties`.

## Decision Outcome

Chosen: **elapsed seconds**. `versionCode = now - codeEpoch`, where `codeEpoch` is
the first commit's timestamp, read once and frozen as a constant in
`version.properties`. `versionName` is the semantic version from the same file
with the code appended: `1.2.3-209584112`. The served APK carries that same
string, so the filename, the manifest and Android's app info all agree.

The two numbers in `version.properties` are maintained in opposite ways, which is
worth stating because the file looks uniform. The semantic version is **generated**:
cocogitto derives it from Conventional Commit types and a pre-bump hook writes it
into the file, which `cog bump` then commits alongside `CHANGELOG.md` and tags.
Editing that line by hand is editing generated output. `codeEpoch` is the opposite
— written once, by hand, and never again.

Deriving `versionName` from the git tag at build time was rejected for the same
reason as computing the epoch live: it puts git, cocogitto and full history inside
the build container and breaks on a shallow clone. Writing the value into a
committed file keeps the build reading one static source.

A ULID was considered and is not representable: `versionCode` is a 32-bit signed
integer and a ULID is 128 bits, with no truncation that preserves ordering. Its
useful property — monotonic and time-encoding, with no shared state — is exactly
what elapsed seconds provides within the field's range.

A build counter was rejected for being machine-local: delete the file or clone
elsewhere and the count restarts, which means going backwards, which is the one
thing that cannot happen. Commit count was rejected because rebuilding a dirty
tree does not advance it, so the phone would see no new version despite new bytes.

The epoch is frozen rather than computed live from git each build. The value is
identical either way, but freezing makes it immune to a rewritten root commit —
which would shift every subsequent code, potentially forward, turning later
installs into downgrades — and removes any need for git or full history inside
the build container.

## Consequences

* Good, because no manual step exists on the build path at all. The build code
  comes from the clock and the semantic version comes from commit messages, so
  neither is something to remember.
* Good, because the version is self-describing: the code is the build time, so two
  builds are ordered by construction.
* Good, because the same string appears in the filename and in the manifest, so
  the phone alone can answer which build is installed.
* Bad, because the numbers are long and unmemorable in every UI that shows them.
* Bad, because `codeEpoch` must never be raised. Lowering it is harmless;
  raising it lowers every future `versionCode` and breaks installs silently until
  the app is uninstalled.
* Bad, because two builds within the same second collide. A Gradle build takes
  longer than that, so this is theoretical.
* The scheme expires around 2088, when elapsed seconds exceed the 32-bit signed
  range.
