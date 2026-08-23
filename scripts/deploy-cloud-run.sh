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

  # Scale to zero by default: this service is idle most of the day and a warm instance is billed for
  # every minute of that. Everything below exists to make the resulting cold start cheap enough that
  # keeping one alive is not the only way to get an acceptable first response. Set minInstances to 1
  # on a target where even a tuned cold start is too slow to sit in front of a user.
  MIN_INSTANCES=$(jq -r ".[$i].minInstances // 0" "$TARGETS_FILE")
  MAX_INSTANCES=$(jq -r ".[$i].maxInstances // 10" "$TARGETS_FILE")

  # Requests one instance handles at once. Tomcat is sized to match (TOMCAT_MAX_THREADS in
  # application.yml); leaving this at Cloud Run's default of 80 while the server has 40 workers only
  # moves the queue inside the container, where the platform cannot see it and will not scale out.
  CONCURRENCY=$(jq -r ".[$i].concurrency // 40" "$TARGETS_FILE")

  # A JVM on less than 512Mi spends its life in GC, and Cloud Run ties available CPU to the memory
  # tier at the smallest sizes. One vCPU is enough to serve; it is the cold start that wants more,
  # and --cpu-boost covers that without paying for it for the life of the instance.
  CPU=$(jq -r ".[$i].cpu // 1" "$TARGETS_FILE")
  MEMORY=$(jq -r '.['"$i"'].memory // "1Gi"' "$TARGETS_FILE")

  # The image is built by Paketo buildpacks, whose memory calculator divides the container's memory
  # into heap and everything else, and sizes "everything else" partly from an assumed 250 threads at
  # a megabyte of stack each. This server runs nowhere near that -- 40 Tomcat workers plus the Mongo
  # driver's own and the JVM's -- so the assumption quietly withholds well over a hundred megabytes
  # from the heap of a 1Gi instance. Stated with headroom rather than trimmed to the count, because
  # being wrong downwards is an OOM kill and being wrong upwards is only a smaller heap.
  JVM_THREAD_COUNT=$(jq -r ".[$i].jvmThreadCount // 100" "$TARGETS_FILE")

  [[ "$SERVICE" == "null" || "$PROJECT" == "null" || "$REGION" == "null" ]] && {
    exit 1
  }

  echo "🚀 Deploying $SERVICE to $PROJECT ($REGION), trusting $TRUSTED_PROXY_COUNT proxy hop(s)"
  # --update-env-vars merges into what the service already has. --set-env-vars would replace the
  # lot, taking MONGODB_URI and BASE64_ENCRYPTION_KEY with it and leaving the service unable to boot.
  #
  # The flags after it, by contrast, are restated on every deploy on purpose. They are what makes
  # scaling to zero tolerable, and leaving them to whatever the console happens to hold gives a
  # service that is fast on one project and slow on the next, with nothing in the repo to explain it.
  #
  # --cpu-boost is the largest single item: Cloud Run grants extra CPU until the container reports
  # ready, which is exactly the window a JVM spends classloading and refreshing a Spring context.
  #
  # The startup probe replaces the default, which is a TCP check against the port. Tomcat binds the
  # port during context refresh -- before the migration runner and the app-config warm-up have run --
  # so a TCP check goes green while the instance still cannot answer, and the first real request of
  # every cold start then queues behind the rest of startup. /actuator/health/readiness reports
  # ready on ApplicationReadyEvent instead, which is after both. failureThreshold x periodSeconds is
  # the startup budget -- 90s here, generous because overrunning it kills the container.
  gcloud run deploy "$SERVICE" \
    --project "$PROJECT" \
    --region "$REGION" \
    --image "$IMAGE_DIGEST" \
    --update-env-vars "TRUSTED_PROXY_COUNT=$TRUSTED_PROXY_COUNT,BPL_JVM_THREAD_COUNT=$JVM_THREAD_COUNT" \
    --min-instances "$MIN_INSTANCES" \
    --max-instances "$MAX_INSTANCES" \
    --concurrency "$CONCURRENCY" \
    --cpu "$CPU" \
    --memory "$MEMORY" \
    --cpu-boost \
    --startup-probe "httpGet.path=/actuator/health/readiness,httpGet.port=8080,initialDelaySeconds=4,periodSeconds=2,timeoutSeconds=2,failureThreshold=45"
done
