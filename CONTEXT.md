# Now Playing

A Muzei art provider that mirrors the album you are currently listening to onto
the home screen wallpaper. Successor to the Muzei Music Extension, inheriting its
design conclusions rather than its code.

## Language

**Now Playing**:
The album currently driving the wallpaper. There is exactly one at any moment.
_Avoid_: current track, current song — the unit is the album, not the song.

**Provider**:
The component Muzei discovers and pulls artwork from. Muzei called these
"sources" before version 3.0; that word now means something else.
_Avoid_: source, extension, plugin.

**Winning session**:
The media session that last began playing, and the only one permitted to change
the wallpaper. Several sessions may be alive at once — a paused player, a
podcast, a video — and exactly one of them wins. A session is identified by its
own handle, never by the app it belongs to: one app can own two at once.
_Avoid_: active session, current session — several sessions are active at once,
which is the whole reason the term exists.

**Track**:
One song's metadata as a media session reports it: title, artist, album artist,
album. The raw input, before it is reduced to an album key.

**Album key**:
Album artist plus album. Two tracks sharing an album key are the same Now Playing
and produce no wallpaper change. Album artist is used rather than artist so a
compilation stays one album. The album may be absent — an artist alone is enough
to look artwork up — so an album key without one is the artist
fallback's lookup rather than a different kind of thing.
_Avoid_: token for the concept — that is the name of the field the key is written
into. The word is correct when referring to that serialised value, which is also
what gets persisted.

**Sample album**:
One of a short list shipped inside the app, shown before anything has ever played.
Looked up exactly like a real one, so the first run proves the whole path works.
_Avoid_: placeholder, default artwork — it is a real album, looked up for real, and
the second term names something ADR-0003 rejected.

**Publish**:
To hand a Now Playing to Muzei, replacing whatever it held. It does not mean the
wallpaper has changed, only that Muzei has been told. Immediate, but not
unconditional: the gate can refuse it.
_Avoid_: push, sync, update.

**Gate**:
The check made at the moment of publishing, which can refuse it. Currently one
question — whether the connection is one the user is willing to spend. It applies to
music you play, not to Muzei asking for artwork itself.
_Avoid_: filter, throttle — it neither transforms nor delays, it declines.

**Miss**:
The artwork service having nothing for an album key. A miss is ordinary and
expected — podcasts, video, adverts and obscure records all miss.

**Artist fallback**:
The retry after a miss, asking for the album artist alone. Yields an artist
image instead of a cover, and is the last attempt made.

**Idle**:
No session is playing. The wallpaper keeps the last Now Playing indefinitely;
idle is not a state the wallpaper reflects.
_Avoid_: stopped, paused — neither is reliably distinguishable from the other.
