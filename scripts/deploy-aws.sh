#!/usr/bin/env bash
# Creates or updates komposeauth on AWS (aws-stack.yaml): ECS Fargate behind a load balancer, and an
# S3 bucket for its files. Re-running it deploys the image pitampoudel/komposeauth:main points at now,
# the same image deploy-cloud-run.sh deploys. Needs docker and the aws CLI signed in to the account.
set -euo pipefail

STACK=${STACK:-komposeauth}
REGION=${REGION:-ap-south-1}
: "${VPC_ID:?the VPC to run in}"
: "${SUBNET_IDS:?two or more public subnets, comma-separated}"
: "${CERTIFICATE_ARN:?an ACM certificate for the server domain, in $REGION}"
: "${APP_SECRET_ARN:?the Secrets Manager secret with MONGODB_URI and BASE64_ENCRYPTION_KEY}"
ALERT_EMAIL=${ALERT_EMAIL:-}
IMAGE_TAG=${IMAGE_TAG:-pitampoudel/komposeauth:main}

cd "$(dirname "$0")"

# Pinned by digest, so a task started later by autoscaling runs the same build as the others.
docker pull "$IMAGE_TAG"
IMAGE_DIGEST=$(docker inspect --format='{{index .RepoDigests 0}}' "$IMAGE_TAG")
echo "Image: $IMAGE_DIGEST"

aws cloudformation deploy --region "$REGION" --stack-name "$STACK" \
  --template-file aws-stack.yaml \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides \
    ServiceName="$STACK" \
    VpcId="$VPC_ID" \
    SubnetIds="$SUBNET_IDS" \
    CertificateArn="$CERTIFICATE_ARN" \
    AppSecretArn="$APP_SECRET_ARN" \
    AlertEmail="$ALERT_EMAIL" \
    ImageUri="docker.io/$IMAGE_DIGEST"

aws cloudformation describe-stacks --region "$REGION" --stack-name "$STACK" \
  --query 'Stacks[0].Outputs' --output table
