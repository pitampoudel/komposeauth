#!/usr/bin/env bash
set -euo pipefail

[[ -f .env ]] && source .env

IMAGE_TAG="pitampoudel/komposeauth:main"

: "${KOMPOSEAUTH_DEPLOYS:?KOMPOSEAUTH_DEPLOYS environment variable is required}"

echo "📦 Pulling image: $IMAGE_TAG"
docker pull "$IMAGE_TAG"

IMAGE_DIGEST=$(docker inspect --format='{{index .RepoDigests 0}}' "$IMAGE_TAG")
echo "✅ Using image digest: $IMAGE_DIGEST"

IFS=';' read -ra DEPLOY_ENTRIES <<< "$KOMPOSEAUTH_DEPLOYS"

for ENTRY in "${DEPLOY_ENTRIES[@]}"; do
  IFS=',' read -r SERVICE PROJECT REGION <<< "$ENTRY"

  [[ -z "$SERVICE" || -z "$PROJECT" || -z "$REGION" ]] && {
    echo "❌ Invalid deploy entry: $ENTRY"
    exit 1
  }

  echo "🚀 Deploying $SERVICE to $PROJECT ($REGION)"
  gcloud run deploy "$SERVICE" \
    --project "$PROJECT" \
    --region "$REGION" \
    --image "$IMAGE_DIGEST"
done
