#!/bin/bash
set -e
set -o pipefail

# =============================================================================
# deploy-image.sh - Deploy ModResorts to AWS EKS
# Usage: ./scripts/deploy-image.sh
# Run from the repository root directory
# Prerequisites: aws-cli, kubectl
# =============================================================================

echo "=============================================="
echo "  ModResorts - AWS EKS Deployment Script"
echo "=============================================="
echo ""

# -----------------------------------------------------------------------
# Collect deployment parameters
# -----------------------------------------------------------------------
read -p "Enter AWS Region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS Region is required."
  exit 1
fi

read -p "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS Cluster Name is required."
  exit 1
fi

read -p "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required."
  exit 1
fi

echo ""
echo "--- Application Environment Variables ---"
echo "Press Enter to skip any optional variable."
echo ""

read -p "Enter WEATHER_API_KEY (Weather Underground API key) [optional]: " WEATHER_API_KEY_VAL
read -p "Enter REDIS_HOST (Amazon ElastiCache endpoint) [optional, default: localhost]: " REDIS_HOST_VAL
REDIS_HOST_VAL="${REDIS_HOST_VAL:-localhost}"
read -p "Enter REDIS_PORT (Redis port) [optional, default: 6379]: " REDIS_PORT_VAL
REDIS_PORT_VAL="${REDIS_PORT_VAL:-6379}"
read -p "Enter CUSTOMER_INFO_SERVICE_URL (Customer info microservice URL) [optional]: " CUSTOMER_INFO_SERVICE_URL_VAL
read -p "Enter SERVICE_DISCOVERY_URL (Service discovery endpoint) [optional]: " SERVICE_DISCOVERY_URL_VAL

echo ""

# -----------------------------------------------------------------------
# Configure kubectl for EKS
# -----------------------------------------------------------------------
echo "Configuring kubectl for EKS cluster '${CLUSTER_NAME}' in region '${AWS_REGION}'..."
aws eks update-kubeconfig --region "${AWS_REGION}" --name "${CLUSTER_NAME}"
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to configure kubectl. Check your AWS credentials and cluster name."
  exit 1
fi

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to EKS cluster."; exit 1; }

echo ""

# -----------------------------------------------------------------------
# Update Kubernetes manifests with actual values
# -----------------------------------------------------------------------
echo "Updating Kubernetes manifests with deployment values..."

# Work on copies to avoid modifying originals permanently
cp kubernetes/deployment.yaml /tmp/modresorts-deployment.yaml
cp kubernetes/service.yaml /tmp/modresorts-service.yaml
cp kubernetes/ingress.yaml /tmp/modresorts-ingress.yaml
cp kubernetes/namespace.yaml /tmp/modresorts-namespace.yaml

# Replace image URI placeholder
sed -i 's|{{IMAGE_URI}}|'"${IMAGE_URI}"'|g' /tmp/modresorts-deployment.yaml

# Replace environment variable placeholders
sed -i 's|{{WEATHER_API_KEY}}|'"${WEATHER_API_KEY_VAL}"'|g' /tmp/modresorts-deployment.yaml
sed -i 's|{{REDIS_HOST}}|'"${REDIS_HOST_VAL}"'|g' /tmp/modresorts-deployment.yaml
sed -i 's|{{REDIS_PORT}}|'"${REDIS_PORT_VAL}"'|g' /tmp/modresorts-deployment.yaml
sed -i 's|{{CUSTOMER_INFO_SERVICE_URL}}|'"${CUSTOMER_INFO_SERVICE_URL_VAL}"'|g' /tmp/modresorts-deployment.yaml
sed -i 's|{{SERVICE_DISCOVERY_URL}}|'"${SERVICE_DISCOVERY_URL_VAL}"'|g' /tmp/modresorts-deployment.yaml

echo "Manifests updated successfully."
echo ""

# -----------------------------------------------------------------------
# Apply Kubernetes manifests in order
# -----------------------------------------------------------------------
echo "Applying Kubernetes manifests..."

echo "  [1/4] Applying namespace..."
kubectl apply -f /tmp/modresorts-namespace.yaml

echo "  [2/4] Applying deployment..."
kubectl apply -f /tmp/modresorts-deployment.yaml

echo "  [3/4] Applying service..."
kubectl apply -f /tmp/modresorts-service.yaml

echo "  [4/4] Applying ingress..."
kubectl apply -f /tmp/modresorts-ingress.yaml

echo ""

# -----------------------------------------------------------------------
# Wait for rollout
# -----------------------------------------------------------------------
echo "Waiting for deployment rollout to complete..."
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if [ $? -ne 0 ]; then
  echo ""
  echo "ERROR: Deployment rollout failed or timed out."
  echo "To rollback, run:"
  echo "  kubectl rollout undo deployment/modresorts -n modresorts"
  exit 1
fi

echo ""
echo "Deployment rollout completed successfully!"
echo ""

# -----------------------------------------------------------------------
# Verify resources
# -----------------------------------------------------------------------
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n modresorts

echo ""

# -----------------------------------------------------------------------
# Display application URL
# -----------------------------------------------------------------------
echo "Retrieving application URL from ingress..."
INGRESS_HOST=$(kubectl get ingress modresorts-ingress -n modresorts \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "")

if [ -n "$INGRESS_HOST" ]; then
  echo ""
  echo "=============================================="
  echo "  Deployment Successful!"
  echo "  Application URL: http://${INGRESS_HOST}"
  echo "  Health Check:    http://${INGRESS_HOST}/health"
  echo "=============================================="
else
  echo ""
  echo "=============================================="
  echo "  Deployment Successful!"
  echo "  Ingress host is still provisioning."
  echo "  Run the following to check status:"
  echo "    kubectl get ingress modresorts-ingress -n modresorts"
  echo "=============================================="
fi

echo ""
echo "Rollback command (if needed):"
echo "  kubectl rollout undo deployment/modresorts -n modresorts"
echo ""

# Cleanup temp files
rm -f /tmp/modresorts-deployment.yaml /tmp/modresorts-service.yaml \
      /tmp/modresorts-ingress.yaml /tmp/modresorts-namespace.yaml
