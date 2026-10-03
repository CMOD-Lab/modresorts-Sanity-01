#!/bin/bash
set -e

# =============================================================================
# build-push.sh - Build and push ModResorts Docker image
# Usage: ./scripts/build-push.sh
# Run from the repository root directory
# =============================================================================

PROJECT_NAME="modresorts"
DOCKERFILE_PATH="Dockerfile"

echo "=============================================="
echo "  ModResorts - Docker Build & Push Script"
echo "=============================================="
echo ""

# Sanitize image name: lowercase, replace non-alphanumeric with hyphens, trim hyphens
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

# Prompt for image tag
read -p "Enter image tag [latest]: " IMAGE_TAG_INPUT
IMAGE_TAG=$(echo "${IMAGE_TAG_INPUT:-latest}" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9._-' '-' | sed 's/^-*//;s/-*$//')
if [ -z "$IMAGE_TAG" ]; then
  IMAGE_TAG="latest"
fi

echo ""
echo "Select container registry:"
echo "  1. AWS ECR (Elastic Container Registry)"
echo "  2. Docker Hub"
read -p "Enter choice [1 or 2]: " REGISTRY_CHOICE

echo ""

# -----------------------------------------------------------------------
# AWS ECR
# -----------------------------------------------------------------------
if [ "$REGISTRY_CHOICE" = "1" ]; then
  read -p "Enter AWS Region (e.g. us-east-1): " AWS_REGION
  read -p "Enter AWS Account ID (12-digit): " AWS_ACCOUNT_ID

  if [ -z "$AWS_REGION" ] || [ -z "$AWS_ACCOUNT_ID" ]; then
    echo "ERROR: AWS Region and Account ID are required."
    exit 1
  fi

  ECR_REPO="${IMAGE_NAME}"
  REGISTRY_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
  FULL_IMAGE_NAME="${REGISTRY_URL}/${ECR_REPO}:${IMAGE_TAG}"

  echo "Authenticating with AWS ECR..."
  aws ecr get-login-password --region "${AWS_REGION}" | \
    docker login --username AWS --password-stdin "${REGISTRY_URL}"
  if [ $? -ne 0 ]; then
    echo "ERROR: ECR login failed."
    exit 1
  fi

  # Auto-create ECR repository if it does not exist
  echo "Checking ECR repository '${ECR_REPO}'..."
  aws ecr describe-repositories --repository-names "${ECR_REPO}" --region "${AWS_REGION}" > /dev/null 2>&1 || \
    aws ecr create-repository --repository-name "${ECR_REPO}" --region "${AWS_REGION}"

# -----------------------------------------------------------------------
# Docker Hub
# -----------------------------------------------------------------------
elif [ "$REGISTRY_CHOICE" = "2" ]; then
  read -p "Enter Docker Hub username: " DOCKER_USERNAME
  read -s -p "Enter Docker Hub password/token: " DOCKER_PASSWORD
  echo ""

  if [ -z "$DOCKER_USERNAME" ] || [ -z "$DOCKER_PASSWORD" ]; then
    echo "ERROR: Docker Hub username and password are required."
    exit 1
  fi

  REGISTRY_URL="docker.io"
  FULL_IMAGE_NAME="${DOCKER_USERNAME}/${IMAGE_NAME}:${IMAGE_TAG}"

  echo "Authenticating with Docker Hub..."
  echo "${DOCKER_PASSWORD}" | docker login --username "${DOCKER_USERNAME}" --password-stdin
  if [ $? -ne 0 ]; then
    echo "ERROR: Docker Hub login failed."
    exit 1
  fi

else
  echo "ERROR: Invalid choice. Please enter 1 or 2."
  exit 1
fi

# -----------------------------------------------------------------------
# Build Docker image
# -----------------------------------------------------------------------
echo ""
echo "Building Docker image: ${FULL_IMAGE_NAME}"
echo "Build context: . (repository root)"
echo "Dockerfile: ${DOCKERFILE_PATH}"
echo ""

docker build -f "${DOCKERFILE_PATH}" -t "${FULL_IMAGE_NAME}" .
if [ $? -ne 0 ]; then
  echo "ERROR: Docker build failed."
  exit 1
fi

echo ""
echo "Docker image built successfully: ${FULL_IMAGE_NAME}"

# -----------------------------------------------------------------------
# Push Docker image
# -----------------------------------------------------------------------
echo ""
echo "Pushing image to registry..."
docker push "${FULL_IMAGE_NAME}"
if [ $? -ne 0 ]; then
  echo "ERROR: Docker push failed."
  exit 1
fi

echo ""
echo "=============================================="
echo "  Image pushed successfully!"
echo "  Image: ${FULL_IMAGE_NAME}"
echo "=============================================="
echo ""
echo "Use this image URI in your Kubernetes deployment:"
echo "  ${FULL_IMAGE_NAME}"
