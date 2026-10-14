#!/usr/bin/env bash
# Tests the server, builds its image with Buildpacks and pushes it to Docker Hub. The one way the image
# is published: the Publish Docker Image workflow runs this on a push to main and on a release, and
# publishing from a laptop is this same command, so a tag means the same build wherever it came from.
#
# The tags are read off git, the same here as in CI:
#   HEAD is a release tag vX.Y.Z   X.Y.Z and X.Y (a pre-release such as vX.Y.Z-rc.1 gets only its own)
#   on main                        latest and main
#   on any other branch            the branch's name
# The first of them is also the version the jar is built with, as GITHUB_REF_NAME makes it in CI.
#
# Needs JDK 25, Docker, and `docker login` to the registry first; CI signs in with its own action.
# IMAGE overrides the repository, docker.io/pitampoudel/komposeauth.
set -euo pipefail

IMAGE=${IMAGE:-docker.io/pitampoudel/komposeauth}

cd "$(dirname "$0")/.."

# The tags name a commit, so the tree has to be exactly that commit. The settings make WSL's git read
# a Windows checkout the way Windows git does (CRLF endings, every file +x).
if ! git -c core.autocrlf=true -c core.safecrlf=false -c core.filemode=false diff --quiet HEAD; then
  echo "Uncommitted changes. Commit or stash them first." >&2
  exit 1
fi

TAGS=()
RELEASE=$(git describe --tags --exact-match 2>/dev/null || true)
if [[ $RELEASE =~ ^v?([0-9]+)\.([0-9]+)\.([0-9]+)(-.+)?$ ]]; then
  TAGS+=("${RELEASE#v}")
  if [ -z "${BASH_REMATCH[4]}" ]; then TAGS+=("${BASH_REMATCH[1]}.${BASH_REMATCH[2]}"); fi
fi
BRANCH=$(git symbolic-ref --short -q HEAD || true)
if [ "$BRANCH" = main ]; then
  TAGS+=(main latest)
elif [ -n "$BRANCH" ]; then
  TAGS+=("$(printf '%s' "$BRANCH" | tr -c 'A-Za-z0-9_.-' '-')")
fi
if [ ${#TAGS[@]} -eq 0 ]; then
  echo "Nothing to tag: check out main, a branch, or a release tag." >&2
  exit 1
fi
echo "Publishing $(git log -1 --format='%h %s') as ${TAGS[*]/#/$IMAGE:}."

./gradlew :server:check :server:bootBuildImage --imageName="$IMAGE:${TAGS[0]}" \
  -Pversion="${TAGS[0]}" --parallel --build-cache --no-daemon

for tag in "${TAGS[@]}"; do
  if [ "$tag" != "${TAGS[0]}" ]; then docker tag "$IMAGE:${TAGS[0]}" "$IMAGE:$tag"; fi
  docker push "$IMAGE:$tag"
done
