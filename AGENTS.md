# AGENTS.md

Workspace for **Now Playing** — a Muzei art provider that mirrors the album you
are listening to onto the wallpaper. Successor to the Muzei Music Extension.
Two upstream clones sit alongside it as read-only reference.

Read [`CONTEXT.md`](./CONTEXT.md) for the domain language, and
[`docs/adr/`](./docs/adr/) before changing artwork lookup, artwork identity, idle
behaviour, how the app reaches the phone, how versions are numbered, or where the
code came from — each is a decision with a rejected alternative that looks more
obvious than what was chosen.

Two things to know before you touch `version.properties`. Its semantic version
line is **generated** — cocogitto writes it during `cog bump` from Conventional
Commit types — so changing it by hand is editing build output. Its `codeEpoch`
constant is hand-maintained and must never be raised: `versionCode` is computed as
`now - codeEpoch` in seconds, Android refuses any build whose `versionCode` is
lower than the installed one, and raising the epoch lowers every future code and
breaks installs silently. Lowering it is harmless. See
[ADR-0005](./docs/adr/0005-elapsed-seconds-version-code.md).

## Layout

| Path                    | What it is                                                       |
| ----------------------- | ---------------------------------------------------------------- |
| _(root)_                | Now Playing. Its own git repo, no remote.                         |
| `muzei/`                | Clone of github.com/muzei/muzei, clean at `main`. Reference.      |
| `MuzeiMusicExtension/`  | Clone of github.com/timusus/MuzeiMusicExtension, clean at `master`. Reference. |

Root `.gitignore` excludes both clones, so they never enter a root commit. Treat
them as reference: read them, learn from them, and leave both working trees clean.
New code belongs at the root.

**Read, do not paste.** Nothing is copied out of either clone — not code, not
resources, not XML. This project is Apache 2.0 in its own right and owes no
attribution precisely because it carries no one else's code; pasting a file would
change that. Study the original, then write it fresh. See
[ADR-0006](./docs/adr/0006-written-fresh-not-a-fork.md).

`MuzeiMusicExtension` carries a decade of field experience against real music
apps, and its README changelog explains why features were removed as often as why
they were added. Before diverging from it, find out what it does today and what it
used to do — `git log -S` in that clone answers both. Its removals are evidence.
Diverge where there is a reason, and record the reason as an ADR.

## Builds do not run in this environment

There is no Android SDK — `ANDROID_HOME` is unset and neither clone has a
`local.properties`. No Gradle wrapper distribution is cached either, so
`./gradlew` first downloads a full distribution and then fails on the missing
SDK. Verify changes by reading source.

The installed JDK is 26. `MuzeiMusicExtension` is pinned to Gradle 5.6.2 with
AGP 3.5.1 and resolves through `jcenter`, so its build is dead on arrival on this
machine even once an SDK exists. `muzei` is on Gradle 9.6.1 with a version
catalog.

## Where to look things up

- **Art provider API** — `muzei/muzei-api/src/main/java/com/google/android/apps/muzei/api/provider/`:
  `MuzeiArtProvider`, `ProviderContract`, `ProviderClient`, `Artwork`.
- **Worked example** — `muzei/example-unsplash/`, a provider plus a WorkManager
  fetch loop.
- **Minimal real extension** — `MuzeiMusicExtension/extension/`, six Kotlin files.
- **Versions upstream uses** — `muzei/gradle/libs.versions.toml`.

The local API source is 3.4.2 (`muzei/version.properties`);
`MuzeiMusicExtension` compiles against the published `muzei-api:3.1.0`. Where the
two disagree, the local source is newer.

## How an extension attaches to Muzei

An art provider is a `ContentProvider` that Muzei discovers through the manifest —
there is no registration call in code. The wiring, all of it required:

```xml
<provider
    android:name=".YourArtProvider"
    android:authorities="your.package.name"
    android:exported="true"
    android:permission="com.google.android.apps.muzei.api.ACCESS_PROVIDER">
    <intent-filter>
        <action android:name="com.google.android.apps.muzei.api.MuzeiArtProvider" />
    </intent-filter>
    <meta-data android:name="setupActivity" android:value="...SetupActivity" />
    <meta-data android:name="settingsActivity" android:value="...SettingsActivity" />
</provider>
```

Publishing runs the other way, from any component:

```kotlin
ProviderContract.getProviderClient(context, authority)
    .setArtwork(Artwork.Builder().token(...).title(...).byline(...).persistentUri(...).build())
```

`persistentUri` may be remote, and the API's default `openFile()` fetches and
caches it, so an extension needs no storage permission and no HTTP client. Note
where that runs: Muzei calls `openFile` on *your* provider, so the request is made
by your process and the bytes land in your app's data directory — which is why the
`INTERNET` permission is required. See
[ADR-0001](./docs/adr/0001-remote-only-artwork.md). Muzei calls `onLoadRequested(initial)` when it
wants more artwork; that is the only callback a provider must implement.

Both halves in context: `MuzeiMusicExtension/extension/src/main/AndroidManifest.xml`
and `.../MusicExtensionApplication.kt`.

## What the music extension actually does

A `NotificationListenerService` watches the active `MediaController` for metadata
and playback-state changes, builds a `Track` from title/artist/album, and
publishes an `Artwork` whose `persistentUri` points at
`artwork.shuttlemusicplayer.app`. It keeps the last track in `SharedPreferences`
so `onLoadRequested` can republish after a restart. No MediaStore access, no
local artwork scanning — Google's restrictions removed both, and the README's 2.1.0
changelog entry explains why.
