---
status: accepted
date: 2026-08-14
---

# Distribute through Obtainium over a local HTTP server

## Context and Problem Statement

The app is for one person on one phone. It still needs to get onto that phone
repeatedly, and several otherwise-odd constraints in the build fall out of how.
No store listing, no adb, and a signing key that must never change.

## Decision Drivers

* The build runs in a container, which has no USB access to the phone.
* Obtainium installs an update in place only when the signature is unchanged.
* Every build should be installable, without a promotion step to remember.

## Considered Options

* **Obtainium against an HTML directory index**, served on demand.
* **adb install** — host or container, over USB or TCP.
* **Play Store** — a real listing.
* **Third-party F-Droid repo** — a generated, signed index.

## Decision Outcome

Chosen: **Obtainium against an HTML directory index**. Every build is
release-signed and written to a served directory as
`nowplaying-<versionName>.apk`; a `just` recipe serves that directory over HTTP
while an update is wanted. Obtainium's HTML source extracts the version from the
filename with a regex. Nothing runs between updates.

adb was rejected because reaching the phone from a container means USB
passthrough with matching udev rules and usually a privileged flag, or wireless
debugging that needs re-pairing. The Play Store was rejected as disproportionate:
store metadata, a privacy policy and target-SDK deadlines for an audience of one.
An F-Droid repo gives exact update detection and a real manifest, but needs
`fdroidserver` in the build image and a separately signed index — more machinery
than a filename convention earns here.

Anything placed in the served directory is release-signed. AGP always provides a
`debug` build type and it cannot be removed, so debug builds do exist and are
useful for one-off manual installs — but they are never served, because their
signature is not stable: the debug keystore is regenerated per container build,
and Obtainium updates in place only while the signature is unchanged.

The release keystore is therefore required to produce anything installable
through this route, though not to run tests.

## Consequences

* Good, because the container stays a pure build environment with no device
  access.
* Good, because nothing runs between releases.
* Bad, because the app must be signed with a dedicated release keystore, kept
  outside the repository and backed up separately — losing it means uninstalling
  and reinstalling by hand.
* Bad, because the served directory accumulates an APK per build. The build prunes
  to the most recent few, which bounds disk while leaving a rollback target or two.
  Pruning uses the same ordering Obtainium uses to pick the latest, so it can never
  delete the build the phone would be offered.

## Confirmed

Install and in-place update both verified on hardware. An update exercises the
whole scheme at once: it would have failed had the signature not been stable, had
versionCode not increased, had the filename not carried the version where the
regex reads it, or had link sorting not picked the newest build.

## Notes on the mechanism

These were established by reading Obtainium's source, and they contradict what is
widely assumed about the setup:

* Obtainium's HTML source **never reads versionCode**. It scrapes a version string
  from the APK link via `versionExtractionRegEx` (`lib/app_sources/html.dart`),
  falling back to pseudo-versioning — ETag, link hash, or a partial-download hash
  of the APK bytes — when no regex is set.
* Android rejects only a *decrease* in versionCode. An unchanged versionCode
  installs fine as a reinstall.
* Which APK is "latest" comes from sorting the page's links with
  `compareAlphaNumeric` and taking the last. The sort is natural, so `1.10.0`
  correctly outranks `1.9.0`.
* That re-sorting is load-bearing, not a convenience. The server's own listing
  order is *lexicographic* — caddy's autoindex was measured emitting `0.1.0`,
  `0.10.0`, `0.2.0` in that order, whose last entry is the wrong build. Obtainium
  reorders the links before choosing, which is what makes the scheme work, so its
  link sorting must stay enabled in the app's source configuration.

* The **HTML source cannot be selected manually** — it is greyed out in the list.
  It is hostless and does not set `neverAutoSelect` (`source_provider.dart`), so it
  is the catch-all Obtainium falls back to once every host-matching source has
  refused the URL. Paste the URL and let it resolve.
* **The URL must contain a dot.** `preStandardizeUrl` throws `UnsupportedURLError`
  unless the URL contains a `.` that is not its last character, or is a bracketed
  IPv6 literal. So `http://localhost:8080/` and a bare hostname are rejected before
  any source is consulted; a dotted IP works. The same function silently prepends
  `https://` when no scheme is given, which will never reach a plain-HTTP server.
* Cleartext HTTP is fine: Obtainium's manifest sets `usesCleartextTraffic="true"`.

The accepted risk is in that last sorting point: a build whose semver sorts lower than an
existing one — a hotfix on an older line — would leave the phone offered the older
build, silently and with no error. Versions here only ever go forward, so the
situation does not arise.

See [ADR-0005](./0005-elapsed-seconds-version-code.md) for where the version
numbers come from.
