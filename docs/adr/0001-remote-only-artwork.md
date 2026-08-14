---
status: accepted
date: 2026-08-14
---

# Fetch artwork remotely by album key, ignoring the artwork in the metadata

## Context and Problem Statement

A media session usually carries an album cover of its own, in
`METADATA_KEY_ALBUM_ART` or `METADATA_KEY_ART_URI` — the exact image the
notification shows. Ignoring it and asking a remote service for a picture we were
already handed looks wrong on first reading. This ADR records why.

## Decision Drivers

* Muzei runs in a separate process and cannot read our files without help.
* The muzei-api already implements fetching and caching for a remote URI, so
  pointing at one costs no networking code.
* The original extension shipped a local-artwork path and then deleted it.

## Considered Options

* **Remote lookup by album key** — publish an HTTPS URL and let the API fetch it.
* **Session art first, remote fallback** — cache the session bitmap and serve it.
* **Local library lookup** — resolve the track in MediaStore and read its cover.

## Decision Outcome

Chosen: **remote lookup by album key**. The artwork URL is built from album artist
and album and published as the artwork's persistent URI, and the API's default
`openFile()` implementation does the rest: fetching, caching and retrying. This
project writes no HTTP client, opens no files and requests no storage permission.

Worth being precise about where that happens, because it is natural to assume
Muzei does the downloading. It does not. Muzei calls `openFile` on *this* app's
ContentProvider, so the HTTP request runs in this process and the bytes are cached
in this app's data directory. Hence the `INTERNET` permission in the manifest —
which is also why the original extension declared it.

The local library lookup was version 2.0's design. It matched the track in
MediaStore, read `ALBUM_ART`, fell back to a regex folder search, and wrapped the
result in a third-party stream provider so Muzei could read it — costing
`WRITE_EXTERNAL_STORAGE` and an onboarding screen to request it. Version 2.1.0
removed the whole path because Google closed on-disk artwork discovery. That
route is gone and is not worth reopening.

Session art is a different door and is still open: nothing stops us caching the
bitmap and overriding `openFile`, and the modern API makes that far cheaper than
the stream-provider dance ever was. It was rejected anyway, because the endpoint
already answers on album artist and album, and adding a second artwork origin
means two failure paths, a cache to manage, and per-track image I/O — for an
image the remote service usually has.

### Consequences

* Good, because the app has no networking dependency at all — the previous
  version dropped Retrofit and OkHttp for the same reason.
* Good, because there is one artwork origin, so there is one failure path.
* Bad, because artwork depends on a third-party service staying up, and on the
  album existing in its catalogue.
* Bad, because music not in any catalogue — bootlegs, home recordings — will
  never show its own cover even when the session is holding it.
