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
   Optional: `FILES_BUCKET` to give the tasks access to an S3 bucket through their role (then list
   it on the admin console's Storage page with no access key, so the server signs in as the task
   role), and `ALERT_EMAIL` for the unreachable, 5xx and memory alarms. `REGION` defaults to
   `ap-south-1` and `STACK` to `komposeauth`.
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
- **Files** are kept wherever the Storage page says (`/admin/storage`, see "Where files are kept" in
  the main README), and the database brings that list with it. To bring files kept on GCS over to S3:
  1. Add the S3 bucket on the Storage page with no access key and test it.
  2. Edit the GCS store and give it a service account key that can list, read and delete in its
     bucket, since the tasks hold no Google identity of their own. Test it.
  3. **Move files** from the GCS store to the S3 one. New files go to S3 at once; then every file is
     copied under the same name, GCS addresses start opening the copies in S3, and every saved
     address is changed to the S3 one. Keep the page open until it is done.
  4. Once the move says nothing points at the GCS store, remove its entry, then delete the bucket
     whenever you are sure.

  The same steps move files between any two stores, in either direction.
- **Profile photos and organization logos** are loaded by browsers straight from the address on
  the record, so the S3 bucket has to let anyone read those two prefixes, as the GCS bucket did.
  KYC documents stay private: this server serves them. A bucket policy is refused while Block
  Public Access covers policies, so lift that part of it first:
  ```bash
  aws s3api put-public-access-block --bucket <bucket> --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=false,RestrictPublicBuckets=false
  aws s3api put-bucket-policy --bucket <bucket> --policy '{"Version": "2012-10-17",
    "Statement": [{"Effect": "Allow", "Principal": "*", "Action": "s3:GetObject",
    "Resource": ["arn:aws:s3:::<bucket>/users/*", "arn:aws:s3:::<bucket>/organization_logos/*"]}]}'
  ```
- **The database** moves as it is: signing keys, sessions and the admin config are all in it. Keep
  `BASE64_ENCRYPTION_KEY` with it, since the config and the KYC documents are encrypted under that
  key.
- **Scale:** at least one task always runs, so there are no cold starts and no CPU throttling between
  requests.
