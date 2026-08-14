# Now Playing -- task runner.
# This file is the source of truth for commands; the docs point here rather
# than restating recipes.
#
# NOTE ON COMMENTS: `just --list` takes the single comment line immediately above
# a recipe as its description. Longer explanations go above a blank line, or they
# end up as the listing text.

# Container runtime. Both podman and docker work; override with
# CONTAINER_RUNTIME=docker or `just runtime=docker <recipe>`.
runtime := env('CONTAINER_RUNTIME', 'podman')

toolchain_image := "nowplaying-build:local"
serve_image := "nowplaying-serve:local"

# Named volume for GRADLE_USER_HOME, so dependencies survive between runs and
# nothing lands on the host filesystem. Must match the Containerfile.
gradle_volume := "nowplaying-gradle"
gradle_home := "/home/build/.gradle"

# Required for docker, forbidden for rootless podman. See ADR-0007 for the
# measurements and why no single invocation covers both.
#
# Gated on "not podman" rather than "is docker" deliberately: an unrecognised
# runtime -- nerdctl, podman-remote, an absolute path like /usr/bin/docker --
# then fails on the safe side. A needless --user is a permission error you see
# immediately; a missing one silently leaves root-owned files on the host.
user_flag := if runtime == "podman" { "" } else { "--user " + `id -u` + ":" + `id -g` }

# List available recipes.
default:
    @just --list

# Nothing rebuilds automatically and no recipe builds on demand, so run this
# after changing the Containerfile.

# Build both container images.
image:
    {{runtime}} build -f Containerfile --target toolchain -t {{toolchain_image}} .
    {{runtime}} build -f Containerfile --target serve -t {{serve_image}} .

# Requires `just image` first.
#
# Invoked as a dependency rather than by re-entering just, so that a `runtime=`
# override on the command line reaches it. Recursion would carry only the
# environment variable, silently ignoring the override.

# Run a command inside the toolchain container.
[private]
_run +args:
    {{runtime}} run --rm {{user_flag}} \
        -v {{justfile_directory()}}:/workspace \
        -v {{gradle_volume}}:{{gradle_home}} \
        -w /workspace \
        {{toolchain_image}} {{args}}

# Produces no APK, but Gradle does write build/ and .gradle/ into the work tree;
# both are gitignored.

# Run the tests.
check: (_run "./gradlew" "--console=plain" "test")

# Neither the signing config nor the versioned copy into dist/ is wired up yet,
# so this cannot produce something installable.

# Assemble a release APK.
build: (_run "./gradlew" "--console=plain" "assembleRelease")

# No --user here: /srv is mounted read-only and nothing is written to the host,
# so the ownership problem in ADR-0007 does not arise. The caddy stage is also a
# different base image, where that flag's behaviour is unmeasured.

# Serve dist/ over HTTP for Obtainium. Ctrl-C to stop.
serve port="8080":
    @mkdir -p dist
    {{runtime}} run --rm -p {{port}}:8080 \
        -v {{justfile_directory()}}/dist:/srv:ro \
        {{serve_image}}

# The order is load-bearing, not stylistic. cog bump writes the new semantic
# version into version.properties, so anything built before it carries the old
# version while the tag carries the new one. Hence: verify, then tag, then build
# from the bumped file. `&&` post-dependencies give exactly that sequence, and a
# failed bump skips the build rather than shipping a mislabelled artifact.

# Cut a release: check, bump the version and changelog, then build.
release: check && build
    cog bump --auto
