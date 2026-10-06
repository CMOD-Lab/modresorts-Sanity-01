#!/bin/bash
# =============================================================================
# deploy-image.sh – Deploy ModResorts to AWS EKS
# =============================================================================
set -e
set -o pipefail

APP_NAME="modresorts"
NAMESPACE="modresorts"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "  ModResorts – AWS EKS Deployment"
echo "============================================"
echo ""

# ── AWS / EKS configuration ──────────────────────────────────────────────────
read -rp "Enter AWS Region [us-east-1]: " AWS_REGION
AWS_REGION="${AWS_REGION:-us-east-1}"

read -rp "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS Cluster Name is required." >&2
  exit 1
fi

# ── Docker image URI ─────────────────────────────────────────────────────────
read -rp "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required." >&2
  exit 1
fi

# ── Application environment variables ────────────────────────────────────────
echo ""
echo "--- Application Environment Variables ---"
echo "(Press Enter to keep placeholder value)"

read -rp "Enter REDIS_HOST (ElastiCache endpoint) [localhost]: " REDIS_HOST_VAL
REDIS_HOST_VAL="${REDIS_HOST_VAL:-localhost}"

read -rp "Enter REDIS_PORT [6379]: " REDIS_PORT_VAL
REDIS_PORT_VAL="${REDIS_PORT_VAL:-6379}"

read -rp "Enter WEATHER_SERVICE_HOST [weather-service]: " WEATHER_SERVICE_HOST_VAL
WEATHER_SERVICE_HOST_VAL="${WEATHER_SERVICE_HOST_VAL:-weather-service}"

read -rp "Enter WEATHER_SERVICE_PORT [8080]: " WEATHER_SERVICE_PORT_VAL
WEATHER_SERVICE_PORT_VAL="${WEATHER_SERVICE_PORT_VAL:-8080}"

read -rp "Enter WEATHER_SERVICE_BASE_PATH [/api/weather]: " WEATHER_SERVICE_BASE_PATH_VAL
WEATHER_SERVICE_BASE_PATH_VAL="${WEATHER_SERVICE_BASE_PATH_VAL:-/api/weather}"

read -rp "Enter WEATHER_API_KEY (or press Enter to skip): " WEATHER_API_KEY_VAL

# ── Configure kubectl ─────────────────────────────────────────────────────────
echo ""
echo "Configuring kubectl for EKS cluster: $CLUSTER_NAME ..."
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to configure kubectl." >&2
  exit 1
fi

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to cluster." >&2; exit 1; }

# ── Update manifests with actual values ──────────────────────────────────────
echo ""
echo "Updating Kubernetes manifests..."

# Work on copies to avoid modifying originals
MANIFEST_DIR="${PROJECT_ROOT}/kubernetes"
TMP_DIR=$(mktemp -d)
cp "${MANIFEST_DIR}"/*.yaml "$TMP_DIR/"

# Replace placeholders using pipe delimiter
sed -i "s|{{IMAGE_URI}}|${IMAGE_URI}|g"                                   "${TMP_DIR}/deployment.yaml"
sed -i "s|{{REDIS_HOST}}|${REDIS_HOST_VAL}|g"                             "${TMP_DIR}/deployment.yaml"
sed -i "s|{{REDIS_PORT}}|${REDIS_PORT_VAL}|g"                             "${TMP_DIR}/deployment.yaml"
sed -i "s|{{WEATHER_SERVICE_HOST}}|${WEATHER_SERVICE_HOST_VAL}|g"         "${TMP_DIR}/deployment.yaml"
sed -i "s|{{WEATHER_SERVICE_PORT}}|${WEATHER_SERVICE_PORT_VAL}|g"         "${TMP_DIR}/deployment.yaml"
sed -i "s|{{WEATHER_SERVICE_BASE_PATH}}|${WEATHER_SERVICE_BASE_PATH_VAL}|g" "${TMP_DIR}/deployment.yaml"
sed -i "s|{{WEATHER_API_KEY}}|${WEATHER_API_KEY_VAL}|g"                   "${TMP_DIR}/deployment.yaml"

# ── Apply manifests ───────────────────────────────────────────────────────────
echo ""
echo "Applying Kubernetes manifests..."

echo "  [1/4] Applying namespace..."
kubectl apply -f "${TMP_DIR}/namespace.yaml"

echo "  [2/4] Applying deployment..."
kubectl apply -f "${TMP_DIR}/deployment.yaml"

echo "  [3/4] Applying service..."
kubectl apply -f "${TMP_DIR}/service.yaml"

echo "  [4/4] Applying ingress..."
kubectl apply -f "${TMP_DIR}/ingress.yaml"

# ── Wait for rollout ──────────────────────────────────────────────────────────
echo ""
echo "Waiting for deployment rollout..."
kubectl rollout status deployment/${APP_NAME} -n ${NAMESPACE} --timeout=300s
if [ $? -ne 0 ]; then
  echo "ERROR: Deployment rollout failed. Initiating rollback..." >&2
  kubectl rollout undo deployment/${APP_NAME} -n ${NAMESPACE}
  exit 1
fi

# ── Verify resources ──────────────────────────────────────────────────────────
echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n ${NAMESPACE}

# ── Display access URL ────────────────────────────────────────────────────────
echo ""
INGRESS_HOST=$(kubectl get ingress modresorts-ingress -n ${NAMESPACE} \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
echo "============================================"
echo "  DEPLOYMENT COMPLETE"
echo "  Application URL: http://${INGRESS_HOST}"
echo "  Namespace      : ${NAMESPACE}"
echo "  Image          : ${IMAGE_URI}"
echo "============================================"
echo ""
echo "Rollback command (if needed):"
echo "  kubectl rollout undo deployment/${APP_NAME} -n ${NAMESPACE}"

# Cleanup temp dir
rm -rf "$TMP_DIR"
