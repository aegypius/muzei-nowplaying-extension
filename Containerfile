# Build and delivery environments for Now Playing. Runs under both podman and
# docker.
#
# Two targets:
#   toolchain  JDK + Android SDK. Runs `just check` and `just build`. Default.
#   serve      static HTTP server exposing dist/ for Obtainium.
#
# There is deliberately no separate test target: tests are JVM-only, but AGP
# resolves compileSdk at configuration time, so running the pure-Kotlin module's
# tests still needs the SDK present. A test stage would be a synonym for
# toolchain. Add one when it has content of its own -- coverage tooling,
# Robolectric jars -- and not before.
#
# Everything is pinned so rebuilding this image next year produces the same
# environment. That is the only sense in which "it builds in the container" means
# anything durable, so resist replacing a version below with a floating tag.

# --- serve -------------------------------------------------------------------
# Obtainium's HTML source parses a directory listing for .apk links, so the
# server must autoindex. Verified: caddy's file-server --browse emits
# href="./nowplaying-<version>.apk" and returns 200 for a bare directory.
# busybox httpd was measured returning 404 for the same request and is not a
# substitute without generating index.html by hand.
#
# Pinned digest corresponds to docker.io/library/caddy:latest at caddy v2.11.4.
FROM docker.io/library/caddy@sha256:98eb57d882ccd5213d1688764db10c1ca2c58a1ca3a6717a3411ad798f7a423a AS serve

# Port 8080 rather than 80 so the process can bind it as an unprivileged user --
# docker runs these images with --user $(id -u), which cannot take a low port.
EXPOSE 8080

# dist/ is bind-mounted read-only at /srv; nothing is baked in, because the
# contents change on every build.
CMD ["caddy", "file-server", "--browse", "--root", "/srv", "--listen", ":8080"]

# --- toolchain ---------------------------------------------------------------
# Last stage, so a bare `podman build` produces the environment that does the
# actual work. Gradle requires a JVM between 17 and 26; 21 is the LTS Android
# tooling is most tested against.
#
# Pinned digest corresponds to docker.io/library/eclipse-temurin:21-jdk.
FROM docker.io/library/eclipse-temurin:21-jdk@sha256:57865c22b954cf920cb05a610af81d577e89783282514ba071e99c7357f6c769 AS toolchain

# Android SDK component versions. Note that API 37 ships as minor-versioned
# platforms -- there is no `platforms;android-37`, only android-37.0 and .1.
ARG CMDLINE_TOOLS_ZIP=commandlinetools-linux-15859902_latest.zip
ARG CMDLINE_TOOLS_SHA1=040d3996a65543d22ec4bf73e4c37aa37a8d4af4
ARG ANDROID_PLATFORM="platforms;android-37.0"
ARG ANDROID_BUILD_TOOLS="build-tools;37.0.0"

ENV ANDROID_HOME=/opt/android-sdk
# HOME must be a real, writable directory for an arbitrary UID: docker runs this
# image with --user $(id -u), which would otherwise leave HOME unset and send
# Gradle's writes to / . GRADLE_USER_HOME is where the justfile mounts a named
# volume so dependencies are not re-downloaded on every run.
ENV HOME=/home/build
ENV GRADLE_USER_HOME=/home/build/.gradle
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

# Cache mount keeps the 180 MB download across image rebuilds, which matters
# while this file is still being iterated on. Cache mounts apply to build steps
# only -- they cannot serve as the runtime Gradle cache.
RUN --mount=type=cache,target=/tmp/dl \
    set -eux; \
    if [ ! -f "/tmp/dl/${CMDLINE_TOOLS_ZIP}" ]; then \
        curl -fsSL -o "/tmp/dl/${CMDLINE_TOOLS_ZIP}" \
            "https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}"; \
    fi; \
    echo "${CMDLINE_TOOLS_SHA1}  /tmp/dl/${CMDLINE_TOOLS_ZIP}" | sha1sum -c -; \
    mkdir -p "${ANDROID_HOME}/cmdline-tools"; \
    unzip -q "/tmp/dl/${CMDLINE_TOOLS_ZIP}" -d "${ANDROID_HOME}/cmdline-tools"; \
    mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"

RUN set -eux; \
    yes | sdkmanager --licenses > /dev/null; \
    sdkmanager --install "platform-tools" "${ANDROID_PLATFORM}" "${ANDROID_BUILD_TOOLS}"

# The image is run under two different UID regimes -- docker with --user $(id -u),
# podman rootless as container-root mapped to the host user -- so nothing may
# depend on a specific UID owning these paths.
RUN set -eux; \
    mkdir -p "${GRADLE_USER_HOME}"; \
    chmod -R a+rX "${ANDROID_HOME}"; \
    chmod -R a+rwX "${HOME}"

# No USER directive on purpose. Under rootless podman, selecting a non-root user
# maps into subuids that cannot write the bind-mounted work tree; the justfile
# passes --user only for docker, where the default would otherwise be root and
# leave root-owned build output on the host.

WORKDIR /workspace
