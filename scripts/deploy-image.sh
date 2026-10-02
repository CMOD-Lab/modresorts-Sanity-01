#!/bin/bash
# =============================================================================
# deploy-image.sh – Deploy modresorts to AWS EKS
# Prerequisites: aws-cli, kubectl
# Usage        : bash scripts/deploy-image.sh   (run from repository root)
# =============================================================================
set -e
set -o pipefail

echo "============================================"
echo "  ModResorts – Deploy to AWS EKS"
echo "============================================"
echo ""

# ── Collect deployment parameters ────────────────────────────────────────────
read -rp "Enter AWS region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS region is required."
  exit 1
fi

read -rp "Enter EKS cluster name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS cluster name is required."
  exit 1
fi

read -rp "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required."
  exit 1
fi

echo ""
echo "── Application environment variables ──────────────────────────────────"
echo "  (Press Enter to keep the placeholder value for later manual update)"
echo ""

read -rp "Enter REDIS_HOST (ElastiCache endpoint) [localhost]: " REDIS_HOST_VAL
REDIS_HOST_VAL="${REDIS_HOST_VAL:-localhost}"

read -rp "Enter REDIS_PORT [6379]: " REDIS_PORT_VAL
REDIS_PORT_VAL="${REDIS_PORT_VAL:-6379}"

read -rp "Enter WEATHER_API_KEY (or press Enter to skip): " WEATHER_API_KEY_VAL

read -rp "Enter SERVICE_DISCOVERY_URL [http://service-registry:8080]: " SERVICE_DISCOVERY_URL_VAL
SERVICE_DISCOVERY_URL_VAL="${SERVICE_DISCOVERY_URL_VAL:-http://service-registry:8080}"

read -rp "Enter CUSTOMER_SERVICE_URL [http://customer-service:8080]: " CUSTOMER_SERVICE_URL_VAL
CUSTOMER_SERVICE_URL_VAL="${CUSTOMER_SERVICE_URL_VAL:-http://customer-service:8080}"

# ── Configure kubectl ─────────────────────────────────────────────────────────
echo ""
echo "Configuring kubectl for EKS cluster '${CLUSTER_NAME}' in '${AWS_REGION}'..."
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to EKS cluster."; exit 1; }

# ── Substitute placeholders in manifests ─────────────────────────────────────
echo ""
echo "Updating Kubernetes manifests with deployment values..."

# Work on copies to avoid modifying tracked files
cp kubernetes/deployment.yaml /tmp/modresorts-deployment.yaml
cp kubernetes/service.yaml    /tmp/modresorts-service.yaml
cp kubernetes/ingress.yaml    /tmp/modresorts-ingress.yaml
cp kubernetes/namespace.yaml  /tmp/modresorts-namespace.yaml

sed -i 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g'                           /tmp/modresorts-deployment.yaml
sed -i 's|{{REDIS_HOST}}|'"$REDIS_HOST_VAL"'|g'                     /tmp/modresorts-deployment.yaml
sed -i 's|{{REDIS_PORT}}|'"$REDIS_PORT_VAL"'|g'                     /tmp/modresorts-deployment.yaml
sed -i 's|{{WEATHER_API_KEY}}|'"$WEATHER_API_KEY_VAL"'|g'           /tmp/modresorts-deployment.yaml
sed -i 's|{{SERVICE_DISCOVERY_URL}}|'"$SERVICE_DISCOVERY_URL_VAL"'|g' /tmp/modresorts-deployment.yaml
sed -i 's|{{CUSTOMER_SERVICE_URL}}|'"$CUSTOMER_SERVICE_URL_VAL"'|g' /tmp/modresorts-deployment.yaml

# ── Apply manifests ───────────────────────────────────────────────────────────
echo ""
echo "Applying Kubernetes manifests..."

echo "  [1/4] Applying namespace..."
kubectl apply -f /tmp/modresorts-namespace.yaml

echo "  [2/4] Applying deployment..."
kubectl apply -f /tmp/modresorts-deployment.yaml

echo "  [3/4] Applying service..."
kubectl apply -f /tmp/modresorts-service.yaml

echo "  [4/4] Applying ingress..."
kubectl apply -f /tmp/modresorts-ingress.yaml

# ── Wait for rollout ──────────────────────────────────────────────────────────
echo ""
echo "Waiting for deployment rollout to complete..."
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s || {
  echo ""
  echo "ERROR: Deployment rollout timed out or failed."
  echo "To rollback, run: kubectl rollout undo deployment/modresorts -n modresorts"
  exit 1
}

# ── Verify resources ──────────────────────────────────────────────────────────
echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n modresorts

# ── Display access URL ────────────────────────────────────────────────────────
echo ""
echo "Fetching application ingress URL..."
INGRESS_HOST=$(kubectl get ingress modresorts-ingress -n modresorts \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")

echo ""
echo "============================================"
echo "  Deployment Complete!"
echo "  Image   : ${IMAGE_URI}"
echo "  Cluster : ${CLUSTER_NAME} (${AWS_REGION})"
if [ "$INGRESS_HOST" != "pending" ] && [ -n "$INGRESS_HOST" ]; then
  echo "  App URL : http://${INGRESS_HOST}/resorts/"
else
  echo "  App URL : (ALB provisioning – check 'kubectl get ingress -n modresorts')"
fi
echo "============================================"
echo ""
echo "Rollback command (if needed):"
echo "  kubectl rollout undo deployment/modresorts -n modresorts"
