# Running komposeauth on AWS

The same Docker Hub image the Cloud Run recipe in the main README runs, on ECS Fargate behind an
Application Load Balancer, from one CloudFormation stack (`stack.yaml`) applied by `setup.sh`.
Sessions, authorizations, signing keys and the admin config all live in MongoDB, so a server on AWS
pointed at the same database is the same server: tokens it issued on Cloud Run stay valid.

## Steps

1. **Secret:** one Secrets Manager secret holding the two values the server reads from its
   environment.
   ```bash
   aws secretsmanager create-secret --region ap-south-1 --name komposeauth/app --secret-string "$(jq -n \
     --arg m "$MONGODB_URI" --arg k "$BASE64_ENCRYPTION_KEY" '{MONGODB_URI:$m, BASE64_ENCRYPTION_KEY:$k}')"
   ```
2. **Certificate:** request an ACM certificate for the server's domain in the same region and
   validate it by DNS.
3. **Stack:**
   ```bash
   VPC_ID=vpc-... SUBNET_IDS=subnet-a,subnet-b CERTIFICATE_ARN=arn:aws:acm:... \
   APP_SECRET_ARN=arn:aws:secretsmanager:... ISSUER=https://auth.example.com \
   IMAGE=docker.io/pitampoudel/komposeauth:<release> bash setup.sh
   ```
   Optional: `FILES_BUCKET` to give the tasks access to an S3 bucket through their role (then leave
   `s3AccessKeyId` and `s3SecretAccessKey` blank in the admin config), and `ALERT_EMAIL` for the
   unreachable, 5xx and memory alarms. `REGION` defaults to `ap-south-1` and `STACK` to `komposeauth`.
4. **MongoDB:** allow the tasks in your database's network access. They get public addresses that
   change, so either allow `0.0.0.0/0` or put them behind a NAT gateway with a fixed address.
5. **Switch:** point the domain at the stack's `LoadBalancerDnsName` output. Keep the issuer exactly
   as it was, since every client validates it.

A new release is `IMAGE=...:<release> bash setup.sh` with the same values; the circuit breaker rolls
back a release that doesn't become healthy.

## Things that carry over and things that don't

- **Client addresses:** the load balancer appends the caller's address as the last
  `X-Forwarded-For` entry, as Cloud Run does, so `trustedProxyCount` stays `1`. Check it with
  `/admin/client-ip` after the switch.
- **Files already on GCS** are copied to S3 under the same names, because the tasks hold no Google
  credential and could neither read a KYC document nor delete a photo still there. Set `storageProvider`
  to `s3` first (and restart the old deployment, so nothing still writes to GCS), then copy from a
  machine signed in to both clouds; it sends only what S3 lacks, deletes nothing, and is safe to
  run again:
  ```bash
  gcloud storage rsync gs://<gcpBucketName> s3://<s3BucketName> --recursive
  ```
  Then set `gcsFilesReadFrom` to `s3` in the admin config. An address recorded under GCS is then
  read and deleted in S3 and nothing in the database is rewritten. Keep the GCS bucket while
  profile photos are still shown from their old addresses, since a browser loads those straight
  from GCS.
- **Scale:** at least one task always runs, so there are no cold starts and no CPU throttling between
  requests.
