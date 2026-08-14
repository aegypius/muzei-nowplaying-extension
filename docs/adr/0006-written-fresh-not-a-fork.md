---
status: accepted
date: 2026-08-14
---

# Written fresh under Apache 2.0; not a fork of the Muzei Music Extension

## Context and Problem Statement

Every ADR here cites MuzeiMusicExtension, and much of the design came from its
field experience. A reader could reasonably conclude this is a fork carrying that
project's copyright and obligations. It is not, and the distinction constrains how
the code is written.

## Decision Drivers

* The design conclusions are worth inheriting; the code is six years old and
  targets a toolchain that no longer builds.
* MuzeiMusicExtension has no LICENSE file and never has — its Apache 2.0 grant
  exists only as README prose, a thinner paper trail than usual.
* The dependency, `muzei-api`, is Apache 2.0, so nothing upstream constrains the
  choice.

## Decision Outcome

Chosen: **write everything fresh, license under Apache 2.0**.

No code, resource or XML file is copied from MuzeiMusicExtension. Design
conclusions are not copyrightable, so nothing is owed — and this project takes
plenty of them, including the notification-listener setup flow's shape, the sticky
idle behaviour and the remote-only artwork model, each recorded in its own ADR
with its origin cited.

Apache 2.0 was chosen because both muzei and the original extension use it and
`muzei-api:3.4.2` declares it, so the whole stack reads consistently and anyone
reusing a piece has no compatibility question to answer. The copyright holder is
Nicolas "aegypius" LAURENT — the form ties the copyright line to the `aegypius`
git identity commits are authored under, without publishing one identity in place
of the other.

Credit to Tim Malseed and MuzeiMusicExtension goes in the README as the origin of
the design, though none is legally required.

## Consequences

* Good, because the license choice is unconstrained and stays that way.
* Good, because there is no third-party copyright line to maintain in any file.
* Bad, because solved problems get solved again — the grant-check flow in
  particular is worth studying closely before rewriting.
* The constraint is live, not historical: copying a file later, even
  `preferences.xml` or the animator XMLs, would create attribution obligations
  this decision assumes away. Read those files, do not paste them.
* There is no NOTICE file, because nothing is carried forward that would populate
  one. Per-file license headers are also skipped; the root LICENSE and README
  carry the grant.
