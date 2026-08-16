#!/bin/sh
# Refuses a raise of codeEpoch in version.properties.
#
# Raising codeEpoch lowers every subsequent versionCode. Android then refuses the
# install as a downgrade, and you can recover only if you remove the app. See
# docs/adr/0005-elapsed-seconds-version-code.md.
#
# The rule is here, and not in the caller, because two callers apply it to
# different pairs of objects:
#
#   pre-commit hook   HEAD:version.properties   :version.properties
#                     (the last commit)         (what you are about to commit)
#
#   CI                <base>:version.properties <head>:version.properties
#                     (the commit before)       (the commit pushed)
#
# The hook compares the index, which is the only thing that exists before a
# commit. CI has no index that differs from HEAD, so it compares two commits
# instead. Same rule, different pair.
#
# Usage: check-code-epoch.sh <was-object> <now-object>
set -eu

was_object=$1
now_object=$2

# An object that does not exist gives an empty value. The comparison below then
# does nothing, which is correct: there is nothing to compare.
epoch() {
    git show "$1" 2>/dev/null |
        sed -n 's/^codeEpoch[[:space:]]*=[[:space:]]*//p' |
        head -1
}

was=$(epoch "$was_object")
now=$(epoch "$now_object")

if [ -z "$was" ] || [ -z "$now" ]; then
    exit 0
fi

# Only a raise is refused. ADR-0005 says that a lower value is harmless. The
# comparison is numeric, so a change to the format of the line is not a failure.
if [ "$now" -gt "$was" ] 2>/dev/null; then
    echo "codeEpoch was raised, from $was to $now."
    echo "That lowers every future versionCode; Android then refuses the install."
    echo "See docs/adr/0005-elapsed-seconds-version-code.md."
    exit 1
fi
