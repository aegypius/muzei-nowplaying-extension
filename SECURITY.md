# Security

Now Playing is a personal Android application maintained by one person in their own
time. There is no server, no user accounts and no data held anywhere but on the
device it is installed on. Expect no response time commitment.

## Reporting

Open an issue, or contact the author at `git@aegypius.com`. If the finding is
sensitive, say so and keep the details out of the initial message.

## What is actually at risk

**The signing key.** Every build is signed with a dedicated release keystore, and
Obtainium installs updates in place only while the signature is unchanged. Anyone
holding that key can produce a build the phone will accept as an update. It lives
outside this repository, `keystore.properties` is gitignored, and neither is ever
committed.

**The build server.** Releases are served over plain HTTP on a local network. An
attacker on that network could serve a different APK — though Android still
enforces the signature check, so an unsigned or differently-signed replacement
cannot install over the real app. Serve only on a network you trust, and only
while you are actually installing.

**Notification-listener access.** The app holds a permission that grants
visibility of every notification on the device. It reads media session metadata
and nothing else, but the permission itself is broad, and a compromise of the app
is a compromise of that visibility.

## What is not in scope

- The third-party artwork service. It is not operated by this project; see
  [PRIVACY.md](./PRIVACY.md) for what reaches it.
- Muzei itself, and the Android platform.
- The two upstream repositories checked out beside this one for reference. Report
  issues in those to their own maintainers.
