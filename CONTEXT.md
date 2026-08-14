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
podcast, a video — and exactly one of them wins.
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
_Avoid_: token — that is the name of the field the key is written into, not the
concept.

**Publish**:
To hand a Now Playing to Muzei, replacing whatever it held. Publishing is
unconditional and immediate; it does not mean the wallpaper has changed, only
that Muzei has been told.
_Avoid_: push, sync, update.

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
