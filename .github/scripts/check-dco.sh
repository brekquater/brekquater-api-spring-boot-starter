#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
# Copyright Brekquater
#
# Fails when a commit in BASE..HEAD has no "Signed-off-by:" line (Developer Certificate of Origin).
# Usage: check-dco.sh <base-sha> <head-sha>
set -eu

base="$1"
head="$2"

unsigned=$(git log --no-merges --invert-grep --grep='^Signed-off-by: .\+ <.\+>$' --format='%h %s' "${base}..${head}")

if [ -n "$unsigned" ]; then
  echo "These commits are not signed off. Amend them with: git commit --amend --signoff"
  echo "$unsigned"
  exit 1
fi

echo "Every commit is signed off."
