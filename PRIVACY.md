# Privacy

Now Playing is a personal application. There is no account, no analytics, no crash
reporting and no server operated by its author.

## What leaves your device

One thing: for each album you listen to, the **album artist and album name** are
sent as a URL to `artwork.shuttlemusicplayer.app`, a third-party artwork service,
in order to retrieve a cover image.

That request is made by Muzei rather than by this app — Now Playing hands Muzei a
URL and Muzei fetches it. Either way the album artist and album name reach a third
party, along with whatever your network connection reveals about you, such as your
IP address. That service's handling of those requests is outside this project's
control.

The lookup happens once per album, not once per track.

## What does not leave your device

- Track titles. Only album artist and album are used in the lookup.
- The list of apps you play music from.
- Album art already present on your device — none is read, uploaded or scanned.
- Anything at all when the app is idle.

## What the app can see but does not use

Now Playing requires notification-listener access, which is a broad permission:
Android grants it visibility of every notification, not only media ones. This app
reads media session metadata — title, artist, album artist, album — and ignores
everything else. The title is used for nothing today and is not transmitted.

The most recent album artist and album are stored on the device so the wallpaper
can be restored after a restart. Nothing else is persisted.

## Metered connections

There is a setting to publish only on an unmetered connection. It gates whether
artwork is published at all, so no artwork lookup occurs while it applies.
