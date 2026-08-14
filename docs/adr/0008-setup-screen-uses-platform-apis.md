---
status: accepted
date: 2026-08-14
---

# Ask for notification access with platform APIs and no support library

## Context and Problem Statement

The setup screen extends `android.app.Activity`, styles itself with
`@android:style/Theme.DeviceDefault.DayNight` and checks access with
`NotificationManager.isNotificationListenerAccessGranted`. Every one of those is
the older-looking choice next to `AppCompatActivity`, Material and
`NotificationManagerCompat`, and the predecessor this project inherits its design
from used the latter. This records why the divergence is deliberate.

## Decision Drivers

* minSdk is 34, so APIs the predecessor could not reach are available
  unconditionally.
* The app has no UI beyond two small screens; nothing else needs a widget library.
* Notification-listener access cannot be requested in-app at all — there is no
  runtime dialog for it, only a system settings page.

## Considered Options

* **Platform Activity, platform theme, platform grant check.**
* **AppCompat and Material**, as the predecessor used.
* **A dialog-themed activity** rather than a full screen.

## Decision Outcome

Chosen: **platform APIs throughout**.

`isNotificationListenerAccessGranted` (API 27) replaces
`NotificationManagerCompat.getEnabledListenerPackages`, so androidx-core is not a
dependency. `Theme.DeviceDefault.DayNight` (API 29) gives dark mode and correctly
styled dialogs without Material. Two screens do not justify a widget library in an
app whose only other component is a service.

More importantly, `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` (API 30) deep-links
to *this app's own toggle*, where the predecessor could only open the full list of
every app with a notification listener and leave the user to find it. That is a
better flow than the one being inherited, and it was unavailable at minSdk 21.

The generic list remains as a fallback, because the detail screen is absent on some
ROMs — Muzei's own code guards every comparable call the same way.

## Consequences

* Good, because the APK carries no support library for two screens.
* Good, because the user lands on their own toggle rather than a list to search.
* Bad, because edge-to-edge, insets and scrolling are handled by hand in the layout
  rather than by Material defaults; targetSdk 35+ enforces edge-to-edge
  unconditionally, so the layout must opt into insets itself.
* Bad, because anything more elaborate than a button and a paragraph will make this
  choice worth revisiting rather than extending.
