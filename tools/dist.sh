#!/bin/sh
# Copies the release APK into dist/ under its versioned name, then prunes.
#
# Runs inside the build container, because the version is read back out of the
# built APK with aapt2 rather than recomputed. It cannot be recomputed: versionCode
# is elapsed seconds resolved once per Gradle invocation, so a second reading would
# produce a different number and name the APK something it is not. See
# docs/adr/0005-elapsed-seconds-version-code.md.
#
# Usage: dist.sh <how-many-builds-to-keep>
#
# POSIX sh has no pipefail, so no step here relies on a pipeline's exit status:
# a failing `ls` or `sort` followed by a successful `head` would report success and
# silently prune nothing. Globs and command substitution with explicit checks are
# used instead. This is the same trap the Containerfile's SHELL -o pipefail exists
# to close.
set -eu

keep="${1:?usage: dist.sh <keep>}"

apk="app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$apk" ]; then
    echo "no release APK at $apk" >&2
    exit 1
fi

# aapt2 lives in build-tools, which is not on PATH. Iterating the glob rather than
# `ls | head -1` detects absence properly. It ends on the lexicographically last
# match, which is the only one the image installs -- the Containerfile pins exactly
# one build-tools version.
aapt2=""
for candidate in "$ANDROID_HOME"/build-tools/*/aapt2; do
    [ -x "$candidate" ] && aapt2="$candidate"
done
if [ -z "$aapt2" ]; then
    echo "no aapt2 under $ANDROID_HOME/build-tools" >&2
    exit 1
fi

# Checked separately from the parse, so a broken APK is not reported as an
# unreadable version.
if ! badging="$("$aapt2" dump badging "$apk")"; then
    echo "aapt2 could not read $apk" >&2
    exit 1
fi
version="$(printf '%s\n' "$badging" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1)"
if [ -z "$version" ]; then
    echo "no versionName in $apk" >&2
    exit 1
fi

newest="nowplaying-$version.apk"
mkdir -p dist
cp "$apk" "dist/$newest"
echo "dist/$newest"

# Ordered exactly as Obtainium orders links: a natural sort of the filename, newest
# last. That is what makes "oldest" here mean "the ones Obtainium will never offer".
# Ordering by the trailing versionCode instead would be chronologically truthful but
# can delete the build Obtainium would serve: given nowplaying-0.2.0-100.apk and
# nowplaying-0.1.0-200.apk it offers 0.2.0-100, which is the lower code.
#
# sort -V is the closest available approximation of Obtainium's compareAlphaNumeric.
#
# Every nowplaying-*.apk is a candidate; nothing else in dist/ is touched.
# The build just written is excluded from the candidates outright, rather than
# skipped while deleting: it must always survive, and excluding it up front means
# exactly `keep` files remain instead of keep+1 whenever it happens to sort low.
names=""
for file in dist/nowplaying-*.apk; do
    [ -f "$file" ] || continue
    name="${file#dist/}"
    [ "$name" = "$newest" ] && continue
    names="$names$name
"
done

# awk rather than `head -n -N`, which is a GNU extension whose absence would
# degrade to pruning nothing at all, silently.
# keep - 1, because the build just written accounts for one of the survivors.
stale_names="$(printf '%s' "$names" | sort -V |
    awk -v keep="$keep" '{name[NR] = $0} END {for (i = 1; i <= NR - keep + 1; i++) print name[i]}')"

printf '%s\n' "$stale_names" | while read -r stale; do
    [ -n "$stale" ] || continue
    rm -f "dist/$stale"
    echo "pruned $stale"
done
