# Now Playing -- task runner.
# This file is the source of truth for commands; the docs point here rather
# than restating recipes.

# List available recipes.
default:
    @just --list

# Run the tests. Produces no artifact.
check:
    @echo "check: not implemented yet"

# Build a signed APK into dist/.
build:
    @echo "build: not implemented yet"

# The order is load-bearing, not stylistic. cog bump writes the new semantic
# version into version.properties, so anything built before it carries the old
# version while the tag carries the new one. Hence: verify, then tag, then build
# from the bumped file. `&&` post-dependencies give exactly that sequence, and a
# failed bump skips the build rather than shipping a mislabelled artifact.

# Cut a release: check, bump the version and changelog, then build.
release: check && build
    cog bump --auto
