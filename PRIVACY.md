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

## What is stored on the device

The most recent album artist and album, so the wallpaper can be restored after a
restart.

Downloaded cover images are also cached, in this app's own private storage rather
than Muzei's, because this app is what downloads them. Clearing this app's data
clears that cache. Nothing else is persisted, and nothing is stored outside the
app's private directory.

## Metered connections

There is a setting to publish only on an unmetered connection. It gates whether
artwork is published at all, so no artwork lookup occurs while it applies.
