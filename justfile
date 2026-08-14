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

# The release keystore lives outside the repository, so a stray `git add -f`
# cannot reach it. It is bind-mounted read-only at a fixed path in the container,
# and that path is passed in as an environment variable so the justfile is the
# only place that decides it. See docs/adr/0004-obtainium-distribution.md.
keystore := env(
    'NOWPLAYING_KEYSTORE',
    home_directory() / ".config/nowplaying/release.jks",
)
keystore_in_container := "/keystore.jks"

# NOWPLAYING_KEYSTORE_PASSWORD and _ALIAS are forwarded by name rather than by
# value, so a password taken from a password manager never appears in the
# container's command line where `ps` would show it to anyone on the machine.

# How many builds dist/ keeps. More than one so there is a rollback target.
dist_keep := "5"
keystore_mount := if path_exists(keystore) == "true" {
    "-v " + keystore + ":" + keystore_in_container + ":ro" +
    " -e NOWPLAYING_KEYSTORE=" + keystore_in_container +
    " -e NOWPLAYING_KEYSTORE_PASSWORD -e NOWPLAYING_KEYSTORE_ALIAS"
} else {
    ""
}

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
_run mounts +args:
    {{runtime}} run --rm {{user_flag}} {{mounts}} \
        -v {{justfile_directory()}}:/workspace \
        -v {{gradle_volume}}:{{gradle_home}} \
        -w /workspace \
        {{toolchain_image}} {{args}}

# Produces no APK, but Gradle does write build/ and .gradle/ into the work tree;
# both are gitignored. Gradle reports UP-TO-DATE and runs nothing when no source
# changed, so a green run does not always mean tests executed.

# A filtered run is scoped to :domain because --tests fails a module that has no
# matching tests, and :app has none at all yet. An unfiltered run must therefore
# not pass --tests at all, which is why the whole tail is conditional.
#
# quote() is required: a method pattern contains spaces, which the shell would
# otherwise split into separate arguments.

# Run the tests, optionally filtered, e.g. just test '*AlbumKeyTest*'
test pattern="": (_run "" "./gradlew" "--console=plain" \
    (if pattern == "" { "test" } else { ":domain:test --tests " + quote(pattern) }))

# Release-signed, so it needs the key: see the Signing section of CONTRIBUTING.md.
#
# Two steps, in order: assemble, then name and prune. The naming lives in
# tools/dist.sh because it has to run inside the container, where aapt2 is, and
# because the version is read back out of the APK rather than recomputed.

# Assemble a release APK into dist/.
build: (_run keystore_mount "./gradlew" "--console=plain" "assembleRelease") \
    (_run "" "sh" "tools/dist.sh" dist_keep)

# No --user here: /srv is mounted read-only and nothing is written to the host,
# so the ownership problem in ADR-0007 does not arise. The caddy stage is also a
# different base image, where that flag's behaviour is unmeasured.

# Serve dist/ over HTTP for Obtainium. Ctrl-C to stop.
serve port="8080":
    @mkdir -p dist
    {{runtime}} run --rm -p {{port}}:8080 \
        -v {{justfile_directory()}}/dist:/srv:ro \
        {{serve_image}}

# You type the password into keytool's own prompt, so it never reaches a command
# line, this file, or a shell history. Back the result up somewhere other than this
# machine: losing it means every install has to start over.
#
# Refuses to overwrite an existing key, because that is unrecoverable.

# Create the release keystore.
keystore:
    #!/usr/bin/env sh
    set -eu
    if [ -e "{{keystore}}" ]; then
        echo "refusing to overwrite the existing key at {{keystore}}"
        exit 1
    fi
    mkdir -p "{{parent_directory(keystore)}}"
    {{runtime}} run --rm -it {{user_flag}} \
        -v "{{parent_directory(keystore)}}":/out \
        {{toolchain_image}} \
        keytool -genkeypair -v \
            -keystore "/out/{{file_name(keystore)}}" \
            -alias nowplaying -keyalg RSA -keysize 4096 -validity 10000
    chmod 600 "{{keystore}}"
    echo "created {{keystore}} (mode 600)"
    echo "now set alias and storePass in keystore.properties"

# The order is load-bearing, not stylistic. cog bump writes the new semantic
# version into version.properties, so anything built before it carries the old
# version while the tag carries the new one. Hence: test, then tag, then build
# from the bumped file. `&&` post-dependencies give exactly that sequence, and a
# failed bump skips the build rather than shipping a mislabelled artifact.

# Cut a release: test, bump the version and changelog, then build.
release: test && build
    cog bump --auto
