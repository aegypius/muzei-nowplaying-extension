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
