#!/bin/bash
# =============================================================================
# deploy-image.sh - Deploy ModResorts to AWS EKS
# Usage: ./scripts/deploy-image.sh
# Run from the repository root directory
# =============================================================================
set -e
set -o pipefail

echo "=============================================="
echo "  ModResorts - AWS EKS Deployment Script"
echo "=============================================="
echo ""

# -----------------------------------------------------------------------
# Collect deployment parameters
# -----------------------------------------------------------------------
read -rp "Enter AWS Region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS Region is required." >&2
  exit 1
fi

read -rp "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS Cluster Name is required." >&2
  exit 1
fi

read -rp "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required." >&2
  exit 1
fi

echo ""
echo "--- Optional: Application Environment Variables ---"
echo "Press Enter to skip any variable (placeholder will remain in manifest)."
echo ""

read -rp "Enter WEATHER_API_KEY (or press Enter to skip): " WEATHER_API_KEY_VAL
read -rp "Enter WEATHER_SERVICE_BASE_URL (or press Enter to skip, default: http://api.wunderground.com/api/): " WEATHER_SERVICE_BASE_URL_VAL
read -rp "Enter REDIS_HOST (or press Enter to skip, default: localhost): " REDIS_HOST_VAL
read -rp "Enter REDIS_PORT (or press Enter to skip, default: 6379): " REDIS_PORT_VAL
read -rp "Enter NAMING_SERVICE_HOST (or press Enter to skip, default: naming-service): " NAMING_SERVICE_HOST_VAL
read -rp "Enter NAMING_SERVICE_PORT (or press Enter to skip, default: 8080): " NAMING_SERVICE_PORT_VAL

# Apply defaults for optional variables
[ -z "$WEATHER_SERVICE_BASE_URL_VAL" ] && WEATHER_SERVICE_BASE_URL_VAL="http://api.wunderground.com/api/"
[ -z "$REDIS_HOST_VAL" ]               && REDIS_HOST_VAL="localhost"
[ -z "$REDIS_PORT_VAL" ]               && REDIS_PORT_VAL="6379"
[ -z "$NAMING_SERVICE_HOST_VAL" ]      && NAMING_SERVICE_HOST_VAL="naming-service"
[ -z "$NAMING_SERVICE_PORT_VAL" ]      && NAMING_SERVICE_PORT_VAL="8080"

# -----------------------------------------------------------------------
# Update Kubernetes manifests with collected values
# -----------------------------------------------------------------------
echo ""
echo "Updating Kubernetes manifests..."

# Work on copies to avoid modifying originals permanently
cp kubernetes/deployment.yaml kubernetes/deployment.yaml.bak

sed -i 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g'                                   kubernetes/deployment.yaml
sed -i 's|{{WEATHER_API_KEY}}|'"$WEATHER_API_KEY_VAL"'|g'                   kubernetes/deployment.yaml
sed -i 's|{{WEATHER_SERVICE_BASE_URL}}|'"$WEATHER_SERVICE_BASE_URL_VAL"'|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_HOST}}|'"$REDIS_HOST_VAL"'|g'                             kubernetes/deployment.yaml
sed -i 's|{{REDIS_PORT}}|'"$REDIS_PORT_VAL"'|g'                             kubernetes/deployment.yaml
sed -i 's|{{NAMING_SERVICE_HOST}}|'"$NAMING_SERVICE_HOST_VAL"'|g'           kubernetes/deployment.yaml
sed -i 's|{{NAMING_SERVICE_PORT}}|'"$NAMING_SERVICE_PORT_VAL"'|g'           kubernetes/deployment.yaml

echo "Manifests updated."

# -----------------------------------------------------------------------
# Configure kubectl for EKS
# -----------------------------------------------------------------------
echo ""
echo "Configuring kubectl for EKS cluster '${CLUSTER_NAME}' in region '${AWS_REGION}'..."
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to EKS cluster." >&2; exit 1; }

# -----------------------------------------------------------------------
# Apply Kubernetes manifests
# -----------------------------------------------------------------------
echo ""
echo "Applying Kubernetes manifests..."

echo "  [1/4] Applying namespace..."
kubectl apply -f kubernetes/namespace.yaml

echo "  [2/4] Applying deployment..."
kubectl apply -f kubernetes/deployment.yaml

echo "  [3/4] Applying service..."
kubectl apply -f kubernetes/service.yaml

echo "  [4/4] Applying ingress..."
kubectl apply -f kubernetes/ingress.yaml

# -----------------------------------------------------------------------
# Wait for rollout
# -----------------------------------------------------------------------
echo ""
echo "Waiting for deployment rollout to complete..."
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if [ $? -ne 0 ]; then
  echo "ERROR: Deployment rollout failed. Rolling back..." >&2
  kubectl rollout undo deployment/modresorts -n modresorts
  # Restore original manifest
  mv kubernetes/deployment.yaml.bak kubernetes/deployment.yaml
  exit 1
fi

# -----------------------------------------------------------------------
# Verify resources
# -----------------------------------------------------------------------
echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n modresorts

# -----------------------------------------------------------------------
# Display application URL
# -----------------------------------------------------------------------
echo ""
echo "Retrieving application URL..."
INGRESS_HOST=$(kubectl get ingress modresorts-ingress -n modresorts \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")

echo ""
echo "=============================================="
echo "  Deployment Complete!"
echo "  Image:   ${IMAGE_URI}"
echo "  Cluster: ${CLUSTER_NAME} (${AWS_REGION})"
if [ "$INGRESS_HOST" != "pending" ] && [ -n "$INGRESS_HOST" ]; then
  echo "  App URL: http://${INGRESS_HOST}/resorts/"
else
  echo "  App URL: (Ingress hostname pending - check 'kubectl get ingress -n modresorts')"
fi
echo "=============================================="

# Restore original manifest template
mv kubernetes/deployment.yaml.bak kubernetes/deployment.yaml
