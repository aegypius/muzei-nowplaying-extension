---
status: accepted
date: 2026-08-14
---

# Identify artwork by album, not by track

## Context and Problem Statement

Muzei identifies each artwork by a token and skips work when the token is
unchanged. The obvious token is the track — it is what changed, and it is what
the user is listening to. Using the album instead discards the track title from
everything Muzei displays, which looks like information thrown away for nothing.

## Decision Drivers

* The artwork URL is keyed on album artist and album, so consecutive tracks from
  one record resolve to a byte-identical image.
* A wallpaper transition every three minutes is noise, not information.
* Muzei renders artwork by blurring and dimming it; repeating that work for the
  same image is pure cost.

## Considered Options

* **Album key** — album artist plus album.
* **Track key** — artist, album and title, as version 2.1.0 does.
* **Album key for identity, artist for the lookup** — stable token, per-track URL.

## Decision Outcome

Chosen: **album key**. A full record plays as one stable wallpaper, and the
caption names the album and its artist.

The original extension used a track key through three rewrites, so this is a
deliberate divergence rather than an oversight. Its consequence is accepted: the
caption cannot name the song, because Muzei has no reason to update an artwork
whose token has not changed. Naming the album is therefore the only coherent
caption, not a compromise.

Album artist is preferred to artist, falling back to artist when absent. Keying on
artist would make a compilation churn on every track, which is exactly the
behaviour this decision exists to prevent.

### Consequences

* Good, because the wallpaper changes when the record changes, which is the rate
  a person actually perceives as meaningful.
* Good, because identical images are never refetched or re-rendered.
* Bad, because Muzei's artwork info can never name the playing song.
* Bad, because compilations are keyed on their album artist, often "Various
  Artists", which is more likely to miss in a music artwork catalogue. The artist
  fallback then applies and the wallpaper simply holds.
