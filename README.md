# Now Playing

A [Muzei](https://muzei.co/) art provider that puts the cover of whatever you are
listening to on your home screen wallpaper.

> **Status:** working. Verified on a clean install: enabling the extension asks for
> notification access, a sample album appears before anything has played, and playing
> a song replaces it with that album's cover. Delivered to the phone with Obtainium,
> including in-place updates.
>
> Settings, reachable from Muzei's provider list, offer an unmetered-only switch and
> a blocklist of apps that may not change the wallpaper.

## How it works

A notification listener watches the media session that last started playing and
reads its metadata. The album artist and album become a lookup against a remote
artwork service, and the resulting image URL is handed to Muzei, which downloads,
caches and renders it.

The unit is the album, not the song — a full record plays as one stable wallpaper
rather than changing every three minutes. When nothing is playing, the last cover
stays. Podcasts, video and adverts usually miss in a music artwork catalogue and
change nothing — but a video whose title happens to name a real record does not
miss, which is why any app can be blocked from settings.

## Building and installing

Everything runs in a container; the only host requirements are `docker` or
`podman`, and `just`. Run `just --list` for the current recipes — the justfile is
the source of truth, not this file. CI runs the same tests in the same image; see
[CONTRIBUTING.md](./CONTRIBUTING.md).

There is no Play Store listing and no `adb`. Builds are release-signed and served
over HTTP on your local network, and [Obtainium](https://obtainium.imranr.dev/)
installs them from there.

Setup you have to do once: run `just keystore` to create the release key, then
copy its alias and passwords into `keystore.properties` from the example. The key
itself lives outside the repository, at `~/.config/nowplaying/release.jks`. Back it
up somewhere else — lose it and updates stop installing over the existing app.

## Privacy

Every track you play sends its album artist and album name to a third-party
artwork service. Nothing else leaves the device. See [PRIVACY.md](./PRIVACY.md).

## Documentation

- [CONTRIBUTING.md](./CONTRIBUTING.md) — the working runbook
- [CONTEXT.md](./CONTEXT.md) — the domain language this project uses
- [docs/adr/](./docs/adr/) — why things are the way they are, including several
  that look wrong until you read the reasoning
- [AGENTS.md](./AGENTS.md) — rules for coding agents working in this repository

## Credit

The design comes from [MuzeiMusicExtension](https://github.com/timusus/MuzeiMusicExtension)
by Tim Malseed, which solved these problems first and whose changelog explains
why several features were removed. This is not a fork and contains none of its
code — the debt is to the thinking, and it is documented throughout the ADRs.

Muzei itself is by Roman Nurik and Ian Lake.

## License

Apache 2.0 — see [LICENSE](./LICENSE).
