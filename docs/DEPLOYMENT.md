# ModResorts - Deployment Guide

## Overview

This guide covers building, containerizing, and deploying the **ModResorts** Java EE web application to **AWS EKS (Elastic Kubernetes Service)**.

- **Application**: ModResorts v2.0.0
- **Technology**: Java 8, Java EE 7 (Servlet 3.1), Apache Tomcat 9
- **Build Tool**: Maven 3.x
- **Package Type**: WAR
- **Container Port**: 9080
- **Health Endpoint**: `GET /health`
- **Context Root**: `/` (deployed as ROOT.war in Tomcat)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Local Development with Docker Compose](#local-development-with-docker-compose)
4. [Build and Push Docker Image](#build-and-push-docker-image)
5. [AWS EKS Prerequisites](#aws-eks-prerequisites)
6. [EKS Cluster Setup](#eks-cluster-setup)
7. [Kubernetes Deployment](#kubernetes-deployment)
8. [Environment Variables Reference](#environment-variables-reference)
9. [Scaling and Management](#scaling-and-management)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)

---

## Prerequisites

### Local Development
- Docker Desktop 24.x or later
- Docker Compose v2.x or later
- Java 8 JDK (for local builds)
- Maven 3.8.x or later

### AWS EKS Deployment
- AWS CLI v2 (`aws --version`)
- `kubectl` v1.28+ (`kubectl version --client`)
- `eksctl` v0.160+ (optional, for cluster creation)
- AWS IAM permissions:
  - `eks:DescribeCluster`, `eks:UpdateKubeconfig`
  - `ecr:GetAuthorizationToken`, `ecr:CreateRepository`, `ecr:BatchCheckLayerAvailability`, `ecr:PutImage`
  - `ec2:DescribeSubnets`, `ec2:DescribeSecurityGroups` (for ALB Ingress)

---

## Project Structure

```
modresortssanity01/
├── Dockerfile                    # Multi-stage Docker build
├── docker-compose.yml            # Local development compose file
├── .dockerignore                 # Docker build exclusions
├── pom.xml                       # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/acme/modres/ # Java source files
│       └── resources/            # Application resources (ops.json, reservations.json)
├── WebContent/                   # Web assets (HTML, JSP, CSS, JS, WEB-INF)
├── kubernetes/
│   ├── namespace.yaml            # Kubernetes namespace
│   ├── deployment.yaml           # Kubernetes deployment
│   ├── service.yaml              # Kubernetes ClusterIP service
│   └── ingress.yaml              # AWS ALB ingress
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

Create a `.env` file in the project root:

```bash
# Weather Underground API key (optional - uses cached data if not set)
WEATHER_API_KEY=your_wunderground_api_key

# Redis / Amazon ElastiCache (for customer information state)
REDIS_HOST=localhost
REDIS_PORT=6379

# Customer info microservice URL (optional)
CUSTOMER_INFO_SERVICE_URL=http://customer-info-service/api/customers

# Service discovery endpoint (optional)
SERVICE_DISCOVERY_URL=http://service-discovery/api/lookup
```

### 2. Start the Application

```bash
# Build and start
docker-compose up --build

# Start in background
docker-compose up -d --build

# View logs
docker-compose logs -f modresorts

# Stop
docker-compose down
```

### 3. Verify the Application

```bash
# Health check
curl http://localhost:9080/health

# Application home page
open http://localhost:9080/

# Weather endpoint
curl "http://localhost:9080/resorts/weather?selectedCity=Paris"

# Availability endpoint
curl "http://localhost:9080/resorts/availability?date=12/25/2024"
```

---

## Build and Push Docker Image

### Linux / macOS

```bash
# Make script executable
chmod +x scripts/build-push.sh

# Run from repository root
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (default: `latest`)
2. Select registry type (AWS ECR or Docker Hub)
3. Provide registry credentials

### Windows

```cmd
scripts\build-push.bat
```

### Manual Build (Advanced)

```bash
# Build image
docker build -f Dockerfile -t modresorts:latest .

# Tag for ECR
docker tag modresorts:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Push
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest
```

---

## AWS EKS Prerequisites

### 1. Install AWS CLI

```bash
# macOS
brew install awscli

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install

# Configure
aws configure
```

### 2. Install kubectl

```bash
# macOS
brew install kubectl

# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl && sudo mv kubectl /usr/local/bin/
```

### 3. Install AWS Load Balancer Controller

The ingress manifest uses the AWS Load Balancer Controller. Install it on your EKS cluster:

```bash
# Add Helm repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install controller (replace with your cluster details)
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=<your-cluster-name> \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

---

## EKS Cluster Setup

### Create EKS Cluster (if needed)

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

```bash
# Linux/macOS
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh

# Windows
scripts\deploy-image.bat
```

The script will prompt for:
- AWS Region
- EKS Cluster Name
- Docker image URI (from build-push step)
- Application environment variables (WEATHER_API_KEY, REDIS_HOST, etc.)

### Manual Deployment

```bash
# 1. Apply namespace
kubectl apply -f kubernetes/namespace.yaml

# 2. Update image URI in deployment.yaml
sed -i 's|{{IMAGE_URI}}|123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest|g' kubernetes/deployment.yaml

# 3. Update environment variable placeholders
sed -i 's|{{WEATHER_API_KEY}}|your_api_key|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_HOST}}|your-elasticache-endpoint.cache.amazonaws.com|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_PORT}}|6379|g' kubernetes/deployment.yaml
sed -i 's|{{CUSTOMER_INFO_SERVICE_URL}}|http://customer-info-service.modresorts.svc.cluster.local/api/customers|g' kubernetes/deployment.yaml
sed -i 's|{{SERVICE_DISCOVERY_URL}}|http://service-discovery.modresorts.svc.cluster.local/api/lookup|g' kubernetes/deployment.yaml

# 4. Apply manifests
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# 5. Wait for rollout
kubectl rollout status deployment/modresorts -n modresorts

# 6. Verify
kubectl get pods,svc,ingress -n modresorts
```

### Verify Deployment

```bash
# Check pod status
kubectl get pods -n modresorts

# Check pod logs
kubectl logs -l app=modresorts -n modresorts --tail=100

# Describe deployment
kubectl describe deployment modresorts -n modresorts

# Get ingress URL
kubectl get ingress modresorts-ingress -n modresorts
```

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|---|---|---|---|
| `WEATHER_API_KEY` | No | (empty) | Weather Underground API key. If not set, cached weather data is used. |
| `REDIS_HOST` | Yes (for state) | `localhost` | Amazon ElastiCache Redis primary endpoint |
| `REDIS_PORT` | No | `6379` | Redis port |
| `CUSTOMER_INFO_SERVICE_URL` | No | (empty) | Customer info microservice URL (Kubernetes DNS) |
| `SERVICE_DISCOVERY_URL` | No | (empty) | REST-based service discovery endpoint |
| `SERVER_DISPLAY_NAME` | No | `modresorts` | Server display name (replaces WebSphere ServerName) |
| `SERVER_FULL_NAME` | No | `modresorts` | Server full name |
| `JAVA_OPTS` | No | `-Xmx512m -Xms256m` | JVM options |
| `TZ` | No | `UTC` | Timezone |

### Using Kubernetes Secrets for Sensitive Values

For production, store sensitive values in Kubernetes Secrets:

```bash
# Create secret for API keys
kubectl create secret generic modresorts-secrets \
  --from-literal=WEATHER_API_KEY=your_api_key \
  -n modresorts

# Reference in deployment.yaml
# env:
#   - name: WEATHER_API_KEY
#     valueFrom:
#       secretKeyRef:
#         name: modresorts-secrets
#         key: WEATHER_API_KEY
```

---

## Scaling and Management

### Horizontal Scaling

```bash
# Scale to 3 replicas
kubectl scale deployment modresorts --replicas=3 -n modresorts

# Auto-scaling (HPA)
kubectl autoscale deployment modresorts \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n modresorts
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/modresorts \
  modresorts=123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:v2.1.0 \
  -n modresorts

# Monitor rollout
kubectl rollout status deployment/modresorts -n modresorts

# Rollback if needed
kubectl rollout undo deployment/modresorts -n modresorts

# View rollout history
kubectl rollout history deployment/modresorts -n modresorts
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod events
kubectl describe pod -l app=modresorts -n modresorts

# Check logs
kubectl logs -l app=modresorts -n modresorts --previous

# Check resource constraints
kubectl top pods -n modresorts
```

### Health Check Failures

The application exposes `GET /health` which returns:
```json
{"status":"UP","application":"modresorts"}
```

```bash
# Test health endpoint from within cluster
kubectl exec -it $(kubectl get pod -l app=modresorts -n modresorts -o jsonpath='{.items[0].metadata.name}') \
  -n modresorts -- wget -qO- http://localhost:9080/health
```

### Ingress / ALB Issues

```bash
# Check ingress status
kubectl describe ingress modresorts-ingress -n modresorts

# Check ALB controller logs
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller

# Verify ALB controller is installed
kubectl get deployment -n kube-system aws-load-balancer-controller
```

### Redis Connection Issues

```bash
# Verify REDIS_HOST is set correctly
kubectl exec -it $(kubectl get pod -l app=modresorts -n modresorts -o jsonpath='{.items[0].metadata.name}') \
  -n modresorts -- env | grep REDIS

# Check ElastiCache security group allows traffic from EKS node security group
```

### Image Pull Errors

```bash
# Check ECR permissions
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Verify image exists
aws ecr describe-images --repository-name modresorts --region us-east-1

# Check EKS node IAM role has ECR pull permissions (AmazonEC2ContainerRegistryReadOnly)
```

---

## Security Considerations

1. **Non-root container**: The application runs as the `modresorts` user (non-root) inside the container.
2. **Secrets management**: Use Kubernetes Secrets or AWS Secrets Manager for sensitive values (API keys, Redis passwords).
3. **Network policies**: Consider adding Kubernetes NetworkPolicy to restrict pod-to-pod communication.
4. **Image scanning**: Enable ECR image scanning to detect vulnerabilities.
5. **RBAC**: Apply least-privilege IAM roles to EKS node groups.
6. **TLS**: Configure HTTPS on the ALB ingress using AWS Certificate Manager (ACM):
   ```yaml
   annotations:
     alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}]'
     alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-east-1:123456789012:certificate/xxx
   ```
7. **Security groups**: Restrict ElastiCache security group to only allow traffic from EKS node security group.

---

## Java-Specific Notes

- **JVM Container Support**: The Dockerfile sets `-XX:+UseContainerSupport` and `-XX:MaxRAMPercentage=75.0` to ensure the JVM respects container memory limits.
- **Tomcat 9**: Used as the servlet container (Java EE 7 / Servlet 3.1 compatible). The WAR is deployed as `ROOT.war` so the context root is `/`.
- **Startup Time**: Java applications may take 30-60 seconds to start. The `initialDelaySeconds: 60` on the liveness probe accounts for this.
- **Graceful Shutdown**: `terminationGracePeriodSeconds: 30` allows in-flight requests to complete before pod termination.
- **Logging**: Application logs are written to `/opt/tomcat/logs`. Use `kubectl logs` to access them in Kubernetes.
