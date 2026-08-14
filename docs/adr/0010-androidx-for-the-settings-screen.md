---
status: accepted
date: 2026-08-14
---

# Use AndroidX Preference and Material once there is a settings screen

Supersedes the toolkit conclusion of
[ADR-0008](./0008-setup-screen-uses-platform-apis.md). That ADR's other decisions —
the deep link to this app's own notification toggle, and the platform grant check —
still stand.

## Context and Problem Statement

ADR-0008 chose platform widgets on the grounds that two screens do not justify a
widget library. A settings screen changes the arithmetic: a list of switches is
precisely what AndroidX Preference exists for, and the platform screen it replaced
looked visibly dated.

## Decision Drivers

* A settings list built by hand is padding and typography decisions, repeatedly.
* `PreferenceFragmentCompat` requires an AppCompat-derived theme, so adopting
  Preference brings AppCompat whether or not it is wanted.
* The setup screen looked unpolished, which was the observation that prompted this.

## Considered Options

* **AndroidX Preference with a Material 3 theme.**
* **Platform widgets throughout**, as ADR-0008 chose.
* **Material 3 with Compose.**

## Decision Outcome

Chosen: **AndroidX Preference with Material 3**. The settings screen is declared in
`res/xml/preferences.xml` and rendered by `PreferenceFragmentCompat`; the setup screen
uses the same theme, a Material button and a Material dialog, so the two match.

Compose was rejected as disproportionate: it would add the compiler and runtime for
two screens, and there is no first-party Compose preference library, so the switch
rows would be hand-built regardless — the very work this decision avoids.

## Confirmed

Verified on the device: the settings screen opens from Muzei's provider list and the
switch behaves. The reason for this ADR — that the screens should look like one
application — held up in practice.

## The gate's scope

The setting refuses publishing for music you play, and does not refuse Muzei's own
request for artwork. Blocking that too would honour "no publish, no fetch" more
strictly, and was rejected because it leaves the wallpaper blank on a metered
connection with nothing explaining why — which reads as a fault rather than as
frugality.

Being honest about the hole: restoring is usually served from this app's own cache,
but on a first run the album restored is a sample that has never been fetched, so one
lookup can happen over metered data even with the setting on. PRIVACY.md states this.

A publish refused by the gate is also not remembered, since nothing was shown. A
brief loss of connectivity at the start of a track therefore leaves the previous
cover in place for the rest of the album, because nothing else will ask again until
the album changes. That is consistent with ADR-0003 and is not treated as a fault.

## Consequences

* Good, because the settings screen is declarative, and spacing, switch styling, dark
  mode and accessibility come from the library rather than from guesswork.
* Good, because both screens now look like the same application.
* **Bad, and measurably so: the APK grew from 4.1 MB to 11.9 MB.** AppCompat,
  Preference and Material together are most of that, and almost all of it is unused.
  Minification is not enabled on release builds, which is where that would be
  recovered; enabling it is tracked separately because R8 can break resource and
  reflection paths that only fail at runtime.
* Bad, because AppCompat drags in initialisers of its own — emoji2, lifecycle and
  profileinstaller now appear in the merged manifest.
* The theme must stay AppCompat-derived. Reverting to `Theme.DeviceDefault` would
  crash both screens at runtime rather than merely look different.
