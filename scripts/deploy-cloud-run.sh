#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGETS_FILE="${DEPLOY_TARGETS_FILE:-$SCRIPT_DIR/deploy-targets.json}"

if [[ ! -f "$TARGETS_FILE" ]]; then
  echo "❌ Deploy targets file not found: $TARGETS_FILE"
  exit 1
fi

IMAGE_TAG="pitampoudel/komposeauth:main"

docker pull "$IMAGE_TAG"

IMAGE_DIGEST=$(docker inspect --format='{{index .RepoDigests 0}}' "$IMAGE_TAG")

ENTRY_COUNT=$(jq 'length' "$TARGETS_FILE")

for i in $(seq 0 $((ENTRY_COUNT - 1))); do
  SERVICE=$(jq -r ".[$i].service" "$TARGETS_FILE")
  PROJECT=$(jq -r ".[$i].project" "$TARGETS_FILE")
  REGION=$(jq -r ".[$i].region"  "$TARGETS_FILE")

  # How many proxies of ours a request passes through before reaching the container. Defaults to 1
  # because every Cloud Run service is behind Google's front end -- there is no zero-hop deployment
  # to reach from here. Set 2 on a target fronted by an external Application Load Balancer, which
  # appends the client address and then its own forwarding rule.
  #
  # The image itself defaults to 0, trusting no forwarded header, which is right for a container
  # exposed directly but would make every caller here share one abuse budget. Stating it per target
  # keeps that fact in the repo rather than in console state nobody can see.
  TRUSTED_PROXY_COUNT=$(jq -r ".[$i].trustedProxyCount // 1" "$TARGETS_FILE")

  [[ "$SERVICE" == "null" || "$PROJECT" == "null" || "$REGION" == "null" ]] && {
    exit 1
  }

  echo "🚀 Deploying $SERVICE to $PROJECT ($REGION), trusting $TRUSTED_PROXY_COUNT proxy hop(s)"
  # --update-env-vars merges into what the service already has. --set-env-vars would replace the
  # lot, taking MONGODB_URI and BASE64_ENCRYPTION_KEY with it and leaving the service unable to boot.
  gcloud run deploy "$SERVICE" \
    --project "$PROJECT" \
    --region "$REGION" \
    --image "$IMAGE_DIGEST" \
    --update-env-vars "TRUSTED_PROXY_COUNT=$TRUSTED_PROXY_COUNT"
done
