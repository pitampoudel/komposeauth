#!/bin/bash
# Exit immediately if a command exits with a non-zero status
set -e

# Check if all required arguments are provided
if [ "$#" -ne 3 ]; then
  echo "Usage: $0 <GCP_PROJECT_ID> <SERVICE_NAME> <REGION>"
  exit 1
fi

# Assign arguments to variables
GCP_PROJECT_ID="$1"
SERVICE_NAME="$2"
REGION="$3"

gcloud auth configure-docker "$REGION"-docker.pkg.dev --quiet

# Build Docker image
IMAGE_TAG="$REGION-docker.pkg.dev/$GCP_PROJECT_ID/docker-images/$SERVICE_NAME"

docker build --no-cache -t "$IMAGE_TAG" .

# Push Docker image to Artifact Registry
docker push "$IMAGE_TAG"

# Deploy to Cloud Run
gcloud run services update "$SERVICE_NAME" --platform=managed  --image="$IMAGE_TAG" --region="$REGION" --quiet

# Send notification
curl -d "Workflow completed: $SERVICE_NAME" https://ntfy.sh/github-actions