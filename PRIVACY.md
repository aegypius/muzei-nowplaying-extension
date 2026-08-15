# Privacy

Now Playing is a personal application. There is no account, no analytics, no crash
reporting and no server operated by its author.

## What leaves your device

One thing: for each album you listen to, the **album artist and album name** are
sent as a URL to `artwork.shuttlemusicplayer.app`, a third-party artwork service,
in order to retrieve a cover image.

**This app makes that request itself**, which is why it holds the `INTERNET`
permission. Muzei asks Now Playing for the image rather than fetching it, so the
connection to the artwork service is opened by this app, from your device. The
album artist and album name reach a third party, along with whatever your network
connection reveals about you, such as your IP address. That service's handling of
those requests is outside this project's control.

The lookup happens once per album, not once per track.

One exception, and it is not music you played: before anything has ever played, the
wallpaper shows an album from a short list shipped inside the app, looked up the same
way. So the first lookup can name an album you have never listened to. Once you play
something, that never happens again.

## What does not leave your device

- Track titles. Only album artist and album are used in the lookup.
- The list of apps you play music from. It is kept on the device, as described
  below, and is never sent anywhere.
- Album art already present on your device — none is read, uploaded or scanned.
- Anything at all while music is idle, with one exception: the sample album
  described above is looked up when Muzei asks for artwork and nothing has played.

## What the app can see but does not use

Now Playing requires notification-listener access, because that is the only route
Android offers to media sessions. The permission is deliberately broad, and the
system's grant screen says so: it warns that the app can read notifications, reply
to messages and change settings. That wording is fixed by Android and generic to the
permission — it is not derived from what this app does.

What this app does with it:

- It declines every notification type in its manifest
  (`disabled_filter_types`), so it is never delivered a notification at all.
- It does not implement the callbacks that would receive them, and contains no
  call that could reply to a message, dismiss a notification, or change any
  notification or Do Not Disturb setting. There is no such code path to invoke.
- It reads media session metadata — title, artist, album artist, album — and
  nothing else. The title is used for nothing today and is not transmitted.

## What other apps this one can see

Android hides installed apps from each other by default, and this app asks for one
narrow exception: it can see apps that declare themselves as media apps, in the two
ways Android provides for that. It uses this to show an app's name and icon in the
settings list instead of a bare package name.

It deliberately does not ask for the permission that would reveal every installed
app. Media apps that declare neither of those two things stay invisible to it and
are listed by their package name.

## What is stored on the device

The most recent album artist and album, so the wallpaper can be restored after a
restart, along with the album shown before it and which app put the current one up.
That second album is what returns when you block an app, and the app's name is what
tells blocking which wallpaper to undo.

Which apps have had media open, so they can be offered in the settings list, with
each one's name and icon copied at the moment it is first seen — an app you later
uninstall is still recognisable in the list. Clearing the list from settings removes
all of that, apart from the apps you have blocked, which keep their name and icon so
they can still be recognised and unblocked.

Which apps you have blocked, which is kept when you clear the list — otherwise
clearing would quietly unblock them — and is removed only by unblocking them or by
clearing this app's data.

Downloaded cover images are also cached, in this app's own private storage rather
than Muzei's, because this app is what downloads them. Clearing this app's data
clears that cache. The unmetered-only setting is stored too. Nothing else is
persisted, and nothing is stored outside the app's private directory.

## Metered connections

There is a setting to publish only on an unmetered connection. It is off by default.

When on, it stops artwork being looked up for music you play on a metered connection —
the wallpaper simply keeps the last cover instead.

It does not stop everything. When Muzei asks for artwork itself, which happens when
the provider is selected and after a restart, the album last shown is published
regardless of the connection, so that the wallpaper is not left blank with no
explanation. That is usually already cached here, but on a first run it is a sample
album that has never been fetched, so one lookup can happen over metered data.
