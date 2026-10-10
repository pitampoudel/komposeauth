#!/usr/bin/env bash
# Creates or updates the komposeauth deployment on AWS (stack.yaml): ECS Fargate behind a load
# balancer, running the Docker Hub image. Needs the aws CLI signed in to the target account.
# Safe to re-run; re-running with a new IMAGE is how a new release is rolled out.
set -euo pipefail

STACK=${STACK:-komposeauth}
REGION=${REGION:-ap-south-1}
: "${VPC_ID:?the VPC to run in}"
: "${SUBNET_IDS:?two or more public subnets, comma-separated}"
: "${CERTIFICATE_ARN:?an ACM certificate for the server domain, in $REGION}"
: "${APP_SECRET_ARN:?the Secrets Manager secret with MONGODB_URI and BASE64_ENCRYPTION_KEY}"
: "${ISSUER:?the public address clients validate, e.g. https://auth.example.com}"
IMAGE=${IMAGE:-docker.io/pitampoudel/komposeauth:latest}
FILES_BUCKET=${FILES_BUCKET:-}
ALERT_EMAIL=${ALERT_EMAIL:-}

cd "$(dirname "$0")"

aws cloudformation deploy --region "$REGION" --stack-name "$STACK" \
  --template-file stack.yaml \
  --capabilities CAPABILITY_IAM \
  --no-fail-on-empty-changeset \
  --parameter-overrides \
    ServiceName="$STACK" \
    VpcId="$VPC_ID" \
    SubnetIds="$SUBNET_IDS" \
    CertificateArn="$CERTIFICATE_ARN" \
    AppSecretArn="$APP_SECRET_ARN" \
    Issuer="$ISSUER" \
    ImageUri="$IMAGE" \
    FilesBucketName="$FILES_BUCKET" \
    AlertEmail="$ALERT_EMAIL"

aws cloudformation describe-stacks --region "$REGION" --stack-name "$STACK" \
  --query 'Stacks[0].Outputs' --output table
