---
status: accepted
date: 2026-08-14
---

# Show a real album from a shipped list before anything has played

## Context and Problem Statement

Muzei asks the provider for artwork as soon as it is selected, which is before any
music has played and therefore before there is anything truthful to show. Leaving it
empty makes a freshly selected provider look broken with no explanation.

Whatever is shown then is not what the user is listening to, and ADR-0002 fixes the
caption as album plus artist — so it is indistinguishable from a real Now Playing.
That is the uncomfortable part of this decision and the reason it is written down.

## Decision Drivers

* A wallpaper that is blank with no explanation reads as a bug.
* ADR-0003 rejected reverting to a default image, on the grounds that the wallpaper
  should not try to represent an absence.
* On first run nothing has yet proved that the lookup, fetch and render path works.

## Considered Options

* **A random album from a shipped list**, looked up the ordinary way.
* **A bundled placeholder image**, published from an `android.resource://` URI.
* **Nothing**, leaving the provider empty until something plays.
* **A text-only artwork** explaining the state.

## Decision Outcome

Chosen: **a random album from a shipped list**, published through the same lookup as
any real album.

It ships no image, and it exercises the entire path on first run — album key, URL,
fetch, render — so a broken pipeline announces itself immediately instead of looking
identical to "no music detected". That diagnostic value is the deciding factor: there
is no adb in this project and no on-device log, so a first run that proves the
pipeline is worth a great deal.

The sample is remembered like any other album, so it does not reshuffle. Muzei calls
`onLoadRequested` whenever it wants more artwork rather than only the first time, and
an unremembered sample would pick a different album on every call — turning the
wallpaper into a random slideshow, which is precisely what ADR-0003 forbids.

A bundled image was rejected only because it proves nothing about the pipeline; it is
otherwise the more honest option, and remains the fallback if the sample proves
confusing in practice.

## Consequences

* Good, because a fresh install shows something immediately and proves the machinery.
* Good, because no image asset ships.
* Bad, because the caption names an album the user never played and cannot be
  distinguished from a real one. ADR-0002 fixes the caption format, so there is no
  room to mark it as a sample without breaking that rule.
* Bad, because the first run needs network, where a bundled image would not.
* Bad, because it sends one artwork lookup for an album the user never played, which
  PRIVACY.md now has to state explicitly.
* Once anything real has played, the sample can never reappear: the remembered album
  is overwritten and never reverts.
