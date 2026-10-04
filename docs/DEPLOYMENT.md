# ModResorts - Deployment Guide

## Overview

This guide covers building, containerizing, and deploying the **ModResorts** Java EE 7 web application to **AWS EKS (Elastic Kubernetes Service)**.

- **Application**: ModResorts v2.0.0
- **Technology**: Java 8, Java EE 7 (Servlet/JSP), Maven WAR packaging
- **Runtime**: Apache Tomcat 9 on `eclipse-temurin:8-jdk-alpine`
- **Target Platform**: AWS EKS (Kubernetes)
- **Context Root**: `/resorts`
- **Application Port**: `9080`
- **Health Endpoint**: `GET /resorts/health`

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Local Development with Docker Compose](#local-development-with-docker-compose)
4. [Build and Push Docker Image](#build-and-push-docker-image)
5. [AWS EKS Prerequisites](#aws-eks-prerequisites)
6. [EKS Cluster Setup](#eks-cluster-setup)
7. [Kubernetes Deployment](#kubernetes-deployment)
8. [Configuration Management](#configuration-management)
9. [Scaling and Management](#scaling-and-management)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)
12. [Java-Specific Notes](#java-specific-notes)

---

## Prerequisites

### Local Development
- **Docker** 20.10+ and **Docker Compose** v2+
- **Java 8 JDK** (for local builds)
- **Apache Maven** 3.9+

### AWS EKS Deployment
- **AWS CLI** v2 (`aws --version`)
- **kubectl** 1.27+ (`kubectl version --client`)
- **eksctl** 0.160+ (optional, for cluster creation)
- **AWS IAM permissions**:
  - `eks:DescribeCluster`, `eks:UpdateKubeconfig`
  - `ecr:GetAuthorizationToken`, `ecr:CreateRepository`, `ecr:BatchCheckLayerAvailability`, `ecr:PutImage`
  - `elasticloadbalancing:*` (for ALB Ingress Controller)

---

## Project Structure

```
modresortssanity01/
├── Dockerfile                    # Multi-stage build (Maven builder + Tomcat runtime)
├── .dockerignore                 # Excludes build artifacts and wrapper scripts
├── docker-compose.yml            # Local development compose file
├── pom.xml                       # Maven build descriptor (WAR packaging)
├── src/
│   └── main/
│       ├── java/com/acme/modres/ # Java source files
│       └── resources/            # Application resources (ops.json, reservations.json)
├── WebContent/                   # Web assets (HTML, JSP, CSS, JS, WEB-INF)
├── kubernetes/
│   ├── namespace.yaml            # Kubernetes namespace
│   ├── deployment.yaml           # Deployment with 2 replicas
│   ├── service.yaml              # ClusterIP service
│   └── ingress.yaml              # AWS ALB Ingress
├── scripts/
│   ├── build-push.sh             # Linux/macOS build & push script
│   ├── build-push.bat            # Windows build & push script
│   ├── deploy-image.sh           # Linux/macOS EKS deploy script
│   └── deploy-image.bat          # Windows EKS deploy script
└── docs/
    └── DEPLOYMENT.md             # This file
```

---

## Local Development with Docker Compose

### 1. Configure Environment Variables

Create a `.env` file in the project root (never commit this file):

```bash
# Weather Underground API
WEATHER_API_KEY=your_weather_api_key_here
WEATHER_SERVICE_BASE_URL=http://api.wunderground.com/api/

# Redis / ElastiCache (use localhost for local dev)
REDIS_HOST=localhost
REDIS_PORT=6379

# Naming service
NAMING_SERVICE_HOST=naming-service
NAMING_SERVICE_PORT=8080
```

### 2. Build and Start the Application

```bash
# Build and start the application container
docker-compose up --build

# Run in background
docker-compose up --build -d

# View logs
docker-compose logs -f modresorts
```

### 3. Access the Application

| Endpoint | URL |
|----------|-----|
| Application Home | http://localhost:9080/resorts/ |
| Health Check | http://localhost:9080/resorts/health |
| Weather API | http://localhost:9080/resorts/weather?selectedCity=Paris |
| Availability | http://localhost:9080/resorts/availability |

### 4. Stop the Application

```bash
docker-compose down
```

---

## Build and Push Docker Image

### Linux / macOS

```bash
# Make the script executable (first time only)
chmod +x scripts/build-push.sh

# Run from repository root
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (default: `latest`)
2. Select registry type: **AWS ECR** or **Docker Hub**
3. Provide registry credentials

### Windows

```cmd
scripts\build-push.bat
```

### Manual Docker Build

```bash
# Build the image
docker build -f Dockerfile -t modresorts:latest .

# Tag for ECR
docker tag modresorts:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest

# Push to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest
```

---

## AWS EKS Prerequisites

### 1. Install Required Tools

```bash
# AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install

# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# eksctl (optional)
curl --silent --location "https://github.com/eksctl-io/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```

### 2. Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### 3. Install AWS Load Balancer Controller

The ingress manifest uses the AWS ALB Ingress Controller. Install it on your EKS cluster:

```bash
# Add the EKS chart repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install the controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=<YOUR_CLUSTER_NAME> \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

---

## EKS Cluster Setup

### Create a New EKS Cluster (if needed)

```bash
eksctl create cluster \
  --name modresorts-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed
```

### Configure kubectl

```bash
aws eks update-kubeconfig --region us-east-1 --name modresorts-cluster
kubectl cluster-info
```

---

## Kubernetes Deployment

### Automated Deployment (Recommended)

#### Linux / macOS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows

```cmd
scripts\deploy-image.bat
```

The script will prompt for:
- AWS Region and EKS Cluster Name
- Full Docker image URI (e.g., `123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest`)
- Optional environment variable values (WEATHER_API_KEY, REDIS_HOST, etc.)

### Manual Deployment

```bash
# 1. Update the image URI in deployment.yaml
sed -i 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest|g' kubernetes/deployment.yaml

# 2. Update environment variable placeholders
sed -i 's|{{WEATHER_API_KEY}}|your-api-key|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_HOST}}|your-elasticache-endpoint|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_PORT}}|6379|g' kubernetes/deployment.yaml
sed -i 's|{{NAMING_SERVICE_HOST}}|naming-service|g' kubernetes/deployment.yaml
sed -i 's|{{NAMING_SERVICE_PORT}}|8080|g' kubernetes/deployment.yaml
sed -i 's|{{WEATHER_SERVICE_BASE_URL}}|http://api.wunderground.com/api/|g' kubernetes/deployment.yaml

# 3. Apply manifests in order
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# 4. Wait for rollout
kubectl rollout status deployment/modresorts -n modresorts

# 5. Verify
kubectl get pods,svc,ingress -n modresorts
```

### Kubernetes Manifest Descriptions

| File | Description |
|------|-------------|
| `namespace.yaml` | Creates the `modresorts` namespace |
| `deployment.yaml` | Deploys 2 replicas with liveness/readiness probes on `/resorts/health` |
| `service.yaml` | ClusterIP service exposing port 80 → 9080 |
| `ingress.yaml` | AWS ALB Ingress with health check on `/resorts/health` |

---

## Configuration Management

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `WEATHER_API_KEY` | Weather Underground API key | *(empty)* |
| `WEATHER_SERVICE_BASE_URL` | Weather API base URL | `http://api.wunderground.com/api/` |
| `REDIS_HOST` | Amazon ElastiCache primary endpoint | `localhost` |
| `REDIS_PORT` | ElastiCache port | `6379` |
| `NAMING_SERVICE_HOST` | Naming/discovery service hostname | `naming-service` |
| `NAMING_SERVICE_PORT` | Naming/discovery service port | `8080` |
| `SERVER_DISPLAY_NAME` | Server display name | `modresorts` |
| `SERVER_FULL_NAME` | Server full name | `modresorts-server` |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m ...` |

### Using Kubernetes Secrets for Sensitive Values

```bash
# Create a secret for the Weather API key
kubectl create secret generic modresorts-secrets \
  --from-literal=WEATHER_API_KEY=your-api-key \
  -n modresorts

# Reference in deployment.yaml (add under env:)
# - name: WEATHER_API_KEY
#   valueFrom:
#     secretKeyRef:
#       name: modresorts-secrets
#       key: WEATHER_API_KEY
```

### Using Kubernetes ConfigMap for Non-Sensitive Config

```bash
kubectl create configmap modresorts-config \
  --from-literal=WEATHER_SERVICE_BASE_URL=http://api.wunderground.com/api/ \
  --from-literal=REDIS_HOST=your-elasticache-endpoint \
  --from-literal=REDIS_PORT=6379 \
  -n modresorts
```

---

## Scaling and Management

### Horizontal Scaling

```bash
# Scale to 4 replicas
kubectl scale deployment modresorts --replicas=4 -n modresorts

# Auto-scaling (HPA)
kubectl autoscale deployment modresorts \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n modresorts
```

### Rolling Updates

```bash
# Update the image
kubectl set image deployment/modresorts \
  modresorts=123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:v2.1.0 \
  -n modresorts

# Monitor rollout
kubectl rollout status deployment/modresorts -n modresorts
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/modresorts -n modresorts

# Rollback to specific revision
kubectl rollout history deployment/modresorts -n modresorts
kubectl rollout undo deployment/modresorts --to-revision=2 -n modresorts
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n modresorts

# Describe pod for events
kubectl describe pod <pod-name> -n modresorts

# View pod logs
kubectl logs <pod-name> -n modresorts
kubectl logs <pod-name> -n modresorts --previous  # crashed pod logs
```

### Health Check Failures

```bash
# Test health endpoint directly
kubectl exec -it <pod-name> -n modresorts -- \
  wget -qO- http://localhost:9080/resorts/health

# Check liveness/readiness probe status
kubectl describe pod <pod-name> -n modresorts | grep -A 10 "Liveness\|Readiness"
```

### Ingress / ALB Issues

```bash
# Check ingress status
kubectl describe ingress modresorts-ingress -n modresorts

# Check ALB controller logs
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller

# Verify ALB target group health
aws elbv2 describe-target-health --target-group-arn <arn>
```

### Redis Connection Issues

```bash
# Verify Redis environment variables in pod
kubectl exec -it <pod-name> -n modresorts -- env | grep REDIS

# Test Redis connectivity from pod
kubectl exec -it <pod-name> -n modresorts -- \
  sh -c "nc -zv $REDIS_HOST $REDIS_PORT"
```

### Image Pull Errors

```bash
# Check if ECR credentials are configured
kubectl describe pod <pod-name> -n modresorts | grep "Failed to pull"

# Ensure the node IAM role has ECR read permissions:
# AmazonEC2ContainerRegistryReadOnly policy
```

### JVM Memory Issues

```bash
# Check container memory usage
kubectl top pod -n modresorts

# Increase memory limits in deployment.yaml if OOMKilled:
# resources:
#   limits:
#     memory: "2Gi"
```

---

## Security Considerations

1. **Non-root container**: The application runs as `appuser` (non-root) inside the container.
2. **Secrets management**: Store `WEATHER_API_KEY` and other sensitive values in Kubernetes Secrets, not ConfigMaps.
3. **Network policies**: Consider adding Kubernetes NetworkPolicy to restrict pod-to-pod communication.
4. **Image scanning**: Enable ECR image scanning to detect vulnerabilities in the container image.
5. **RBAC**: Apply least-privilege RBAC policies for the service account used by the deployment.
6. **TLS**: Configure HTTPS on the ALB Ingress using ACM certificates:
   ```yaml
   annotations:
     alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:123456789012:certificate/xxx
     alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
   ```

---

## Java-Specific Notes

### JVM Container Awareness

The Dockerfile sets the following JVM flags for proper container behavior:
- `-XX:+UseContainerSupport`: Enables JVM to respect container CPU/memory limits (Java 8u191+)
- `-XX:MaxRAMPercentage=75.0`: Limits heap to 75% of container memory
- `-Xmx512m -Xms256m`: Explicit heap bounds as a safety net

### Tomcat Configuration

- The WAR is deployed to Tomcat 9 under the `/resorts` context path (matching the original `ibm-web-ext.xml` context root)
- Tomcat listens on port `9080` (configured via `sed` in the Dockerfile)
- The WAR file is placed in `${CATALINA_HOME}/webapps/resorts.war`

### Java EE 7 Compatibility

This application uses Java EE 7 APIs (`javax.*` namespace). Tomcat 9 provides the Servlet 4.0 / JSP 2.3 APIs. Full Java EE features (EJB, CDI, JPA) require a full Java EE container such as Open Liberty or WildFly if needed.

### Redis / ElastiCache

The `ModResortsCustomerInformation` class uses Jedis to connect to Amazon ElastiCache. Configure the `REDIS_HOST` environment variable with your ElastiCache primary endpoint:

```bash
# Example ElastiCache endpoint
REDIS_HOST=modresorts-cache.abc123.ng.0001.use1.cache.amazonaws.com
REDIS_PORT=6379
```

### Weather API

Set `WEATHER_API_KEY` to your Weather Underground API key. If not set, the application falls back to bundled static weather data files in `WebContent/data/`.
