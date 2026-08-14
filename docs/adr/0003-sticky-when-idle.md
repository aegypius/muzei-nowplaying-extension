---
status: accepted
date: 2026-08-14
---

# Keep the last artwork when nothing is playing

## Context and Problem Statement

When music stops, the wallpaper stays on the last album indefinitely. A reader
will reasonably expect it to revert to something neutral, and will be tempted to
add that. This ADR exists to stop them.

## Decision Drivers

* Playback state across arbitrary media apps is not reliably reportable.
* A stale album cover is a better failure than a wrongly-blanked wallpaper.
* Reverting requires deciding how long "stopped" is, which has no correct answer.

## Considered Options

* **Sticky** — the last artwork stays until something else plays.
* **Revert to a default image after a delay.**
* **Clear the artwork** — publish nothing when playback stops.

## Decision Outcome

Chosen: **sticky**.

This is inherited experience, not a fresh judgement. Revert-to-default existed in
the original extension from version 1.0.2 through 2.0 and was removed in 2.1.0,
whose changelog gives the reason: with artwork providers both local and remote,
determining the actual play state is difficult, so the extension now displays the
most recent artwork. Someone shipped the alternative, lived with it, and took it
out.

Sticky also removes a timer, a threshold, an image picker and a settings entry
from a design that otherwise has one preference.

### Consequences

* Good, because there is no idle state to detect, and therefore no way to detect
  it wrongly.
* Good, because the wallpaper is never blank and never needs recovering.
* Bad, because the wallpaper says nothing about whether music is playing — it
  shows what played last, which after a week is simply a wallpaper.
