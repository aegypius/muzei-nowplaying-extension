---
status: accepted
date: 2026-08-14
---

# Pass --user to docker and never to podman

## Context and Problem Statement

The justfile passes `--user` to one container runtime and deliberately withholds
it from the other. That reads as an inconsistency somebody forgot to clean up, and
the obvious "simplification" — using the same flags for both — breaks one of them.
This records the measurements so nobody has to rediscover them.

The build runs in a container against a bind-mounted work tree. Gradle writes
`build/` and `.gradle/` into that tree, so whichever UID the container process
runs as ends up owning files on the host.

## Decision Drivers

* Build output must be owned by the invoking user, not by root.
* Both podman and docker are installed and either should work.
* Rootless podman and rootful docker map container UIDs to host UIDs in opposite
  ways.

## Considered Options

* **Per-runtime flags** — `--user` for docker, nothing for podman.
* **podman only**, using `--userns=keep-id`.
* **Never bind-mount anything writable** — copy sources in, copy artifacts out.
* **Run as root everywhere, then chown** the output.

## Decision Outcome

Chosen: **per-runtime flags**. Measured on this machine, host UID:GID 1000:1000,
writing a file into a bind mount:

| runtime | flags                  | resulting owner    |
| ------- | ---------------------- | ------------------ |
| docker  | *(none)*               | `0:0` — root-owned |
| docker  | `--user 1000:1000`     | `1000:1000` ✓      |
| podman  | *(none)*               | `1000:1000` ✓      |
| podman  | `--user 1000:1000`     | **permission denied** |

Rootless podman already maps container-root to the invoking user, so `--user`
maps *into* subuids that cannot write the mount. There is therefore no single
invocation that satisfies both, and the flag is not a preference: it is required
for one runtime and forbidden for the other.

The condition tests `runtime == "podman"` rather than `runtime == "docker"`, so an
unrecognised value — `nerdctl`, `podman-remote`, an absolute path like
`/usr/bin/docker` — receives the flag. A needless `--user` surfaces immediately as
a permission error; a missing one silently leaves root-owned files behind, which
is the worse failure.

podman-only was rejected because it forecloses docker for no gain. Copying sources
in and artifacts out is uniform and needs no flags at all, but Gradle writes
`build/` inside the source tree, so the tree could not be mounted writable —
costing incremental builds and the whole dev loop. Running as root then chowning
needs elevated privileges under docker, mid-loop.

## Consequences

* Good, because either runtime produces output you own.
* Good, because an unknown runtime fails loudly rather than silently.
* Bad, because "runs unchanged under either" is now "runs under either, with
  runtime-specific flags" — a conditional that must be understood before editing.
* The `serve` recipe passes no `--user` at all under any runtime: `/srv` is mounted
  read-only, nothing is written to the host, and the caddy stage is a different
  base image where the flag's behaviour is unmeasured.
* The image must not rely on a `USER` directive, since the running UID differs
  between runtimes. `HOME` is world-writable in the image for the same reason.
