#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGETS_FILE="${DEPLOY_TARGETS_FILE:-$SCRIPT_DIR/deploy-targets.json}"

if [[ ! -f "$TARGETS_FILE" ]]; then
  echo "❌ Deploy targets file not found: $TARGETS_FILE"
  exit 1
fi

IMAGE_TAG="pitampoudel/komposeauth:main"

echo "📦 Pulling image: $IMAGE_TAG"
docker pull "$IMAGE_TAG"

IMAGE_DIGEST=$(docker inspect --format='{{index .RepoDigests 0}}' "$IMAGE_TAG")
echo "✅ Using image digest: $IMAGE_DIGEST"

ENTRY_COUNT=$(jq 'length' "$TARGETS_FILE")

for i in $(seq 0 $((ENTRY_COUNT - 1))); do
  SERVICE=$(jq -r ".[$i].service" "$TARGETS_FILE")
  PROJECT=$(jq -r ".[$i].project" "$TARGETS_FILE")
  REGION=$(jq -r ".[$i].region"  "$TARGETS_FILE")

  [[ "$SERVICE" == "null" || "$PROJECT" == "null" || "$REGION" == "null" ]] && {
    echo "❌ Entry $i is missing required fields (service, project, region)"
    exit 1
  }

  echo "🚀 Deploying $SERVICE to $PROJECT ($REGION)"
  gcloud run deploy "$SERVICE" \
    --project "$PROJECT" \
    --region "$REGION" \
    --image "$IMAGE_DIGEST"
done
