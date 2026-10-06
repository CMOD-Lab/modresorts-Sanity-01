# ModResorts – Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Analysis](#project-analysis)
4. [Local Development with Docker Compose](#local-development-with-docker-compose)
5. [Build and Push Docker Image](#build-and-push-docker-image)
6. [AWS EKS Deployment](#aws-eks-deployment)
7. [Kubernetes Manifest Reference](#kubernetes-manifest-reference)
8. [Configuration Management](#configuration-management)
9. [Scaling and Management](#scaling-and-management)
10. [Troubleshooting](#troubleshooting)
11. [Security Considerations](#security-considerations)

---

## Overview

**ModResorts** is a Java EE web application (WAR) that provides resort booking, weather information, and availability checking. It is packaged as a WAR file and deployed on Apache Tomcat 9 inside a Docker container.

| Property | Value |
|---|---|
| Build Tool | Maven 3.8.x |
| Java Version | 8 |
| Package Type | WAR |
| Application Port | 8080 |
| Health Endpoint | `/health` |
| Base Image (runtime) | `openjdk:8-jdk` |
| Target Platform | AWS EKS |

---

## Prerequisites

### Local Development
- Docker Desktop 24+ (or Docker Engine 24+)
- Docker Compose v2+
- Java 8 JDK (for local builds outside Docker)
- Maven 3.8+ (for local builds outside Docker)

### AWS EKS Deployment
- AWS CLI v2 configured with appropriate IAM permissions
- `kubectl` v1.28+
- `eksctl` (optional, for cluster creation)
- An existing EKS cluster with the **AWS Load Balancer Controller** installed
- Amazon ECR repository (auto-created by `build-push.sh`)
- Amazon ElastiCache (Redis) endpoint for session/state storage

### Required IAM Permissions
```
ecr:GetAuthorizationToken
ecr:BatchCheckLayerAvailability
ecr:GetDownloadUrlForLayer
ecr:BatchGetImage
ecr:PutImage
ecr:InitiateLayerUpload
ecr:UploadLayerPart
ecr:CompleteLayerUpload
ecr:CreateRepository
ecr:DescribeRepositories
eks:DescribeCluster
eks:ListClusters
```

---

## Project Analysis

### Technology Stack
- **Framework**: Java EE (javax.servlet) + Spring WebMVC 5.3.x
- **Build**: Maven (single-module, WAR packaging)
- **Dependencies**: Gson, Log4j2, Commons Collections, Jackson, Jedis (Redis client)
- **External Services**:
  - **Redis / Amazon ElastiCache** – shared state storage (customer information cache)
  - **Weather Microservice** – external HTTP service for weather data

### Environment Variables
| Variable | Default | Description |
|---|---|---|
| `REDIS_HOST` | `localhost` | ElastiCache primary endpoint |
| `REDIS_PORT` | `6379` | ElastiCache port |
| `WEATHER_SERVICE_HOST` | `weather-service` | Weather microservice hostname |
| `WEATHER_SERVICE_PORT` | `8080` | Weather microservice port |
| `WEATHER_SERVICE_BASE_PATH` | `/api/weather` | Weather API base path |
| `WEATHER_API_KEY` | _(empty)_ | External weather provider API key |

---

## Local Development with Docker Compose

### 1. Build and Start the Application

```bash
# From the project root
docker compose up --build
```

The application will be available at: **http://localhost:8080**

### 2. Override Environment Variables

Create a `.env` file in the project root:

```env
REDIS_HOST=my-elasticache.abc123.ng.0001.use1.cache.amazonaws.com
REDIS_PORT=6379
WEATHER_SERVICE_HOST=weather-service
WEATHER_SERVICE_PORT=8080
WEATHER_SERVICE_BASE_PATH=/api/weather
WEATHER_API_KEY=your-api-key-here
```

Then run:
```bash
docker compose --env-file .env up --build
```

### 3. Stop the Application

```bash
docker compose down
```

---

## Build and Push Docker Image

### Linux / macOS

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (default: `latest`)
2. Select a registry (AWS ECR or Docker Hub)
3. Provide registry credentials

### Windows

```cmd
scripts\build-push.bat
```

### Manual Build

```bash
# Build the image
docker build -t modresorts:latest .

# Tag for ECR
docker tag modresorts:latest <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/modresorts:latest

# Push to ECR
aws ecr get-login-password --region <REGION> | \
  docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com
docker push <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/modresorts:latest
```

---

## AWS EKS Deployment

### Step 1: Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### Step 2: Install AWS Load Balancer Controller (if not already installed)

```bash
# Add the EKS chart repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install the controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=<YOUR_CLUSTER_NAME> \
  --set serviceAccountName=aws-load-balancer-controller
```

### Step 3: Run the Deployment Script

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will prompt for:
- AWS Region
- EKS Cluster Name
- Full Docker image URI
- Application environment variable values

### Step 4: Verify Deployment

```bash
# Check pods
kubectl get pods -n modresorts

# Check service
kubectl get svc -n modresorts

# Check ingress (ALB)
kubectl get ingress -n modresorts

# View pod logs
kubectl logs -l app=modresorts -n modresorts --tail=100
```

### Step 5: Access the Application

Once the ALB is provisioned (may take 2–5 minutes):

```bash
kubectl get ingress modresorts-ingress -n modresorts \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

Open the returned hostname in your browser.

---

## Kubernetes Manifest Reference

### namespace.yaml
Creates the `modresorts` namespace to isolate all application resources.

### deployment.yaml
- **Replicas**: 2 (for high availability)
- **Image**: Placeholder `{{IMAGE_URI}}` replaced at deploy time
- **Resources**: requests: 250m CPU / 512Mi RAM; limits: 500m CPU / 1Gi RAM
- **Liveness Probe**: `GET /health` on port 8080, initial delay 60s
- **Readiness Probe**: `GET /health` on port 8080, initial delay 30s
- **Graceful Shutdown**: `terminationGracePeriodSeconds: 30`

### service.yaml
- **Type**: ClusterIP (internal only; traffic routed via Ingress)
- **Port**: 80 → 8080

### ingress.yaml
- **Class**: AWS ALB (via `kubernetes.io/ingress.class: alb`)
- **Scheme**: internet-facing
- **Health Check Path**: `/health`
- **Host**: `modresorts.example.com` (update to your actual domain)

---

## Configuration Management

### Updating Environment Variables

Edit `kubernetes/deployment.yaml` and update the `env` section, then re-apply:

```bash
kubectl apply -f kubernetes/deployment.yaml
kubectl rollout restart deployment/modresorts -n modresorts
```

### Using Kubernetes Secrets for Sensitive Values

```bash
# Create a secret for the Weather API key
kubectl create secret generic modresorts-secrets \
  --from-literal=WEATHER_API_KEY=your-api-key \
  -n modresorts
```

Then reference it in `deployment.yaml`:
```yaml
- name: WEATHER_API_KEY
  valueFrom:
    secretKeyRef:
      name: modresorts-secrets
      key: WEATHER_API_KEY
```

---

## Scaling and Management

### Manual Scaling

```bash
kubectl scale deployment modresorts --replicas=4 -n modresorts
```

### Horizontal Pod Autoscaler (HPA)

```bash
kubectl autoscale deployment modresorts \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n modresorts
```

### Rolling Update

```bash
# Update the image
kubectl set image deployment/modresorts \
  modresorts=<NEW_IMAGE_URI> \
  -n modresorts

# Monitor rollout
kubectl rollout status deployment/modresorts -n modresorts
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/modresorts -n modresorts

# Rollback to a specific revision
kubectl rollout history deployment/modresorts -n modresorts
kubectl rollout undo deployment/modresorts --to-revision=2 -n modresorts
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Describe the pod for events
kubectl describe pod -l app=modresorts -n modresorts

# Check logs
kubectl logs -l app=modresorts -n modresorts --previous
```

**Common causes:**
- Image pull error → verify ECR permissions and image URI
- OOMKilled → increase memory limits in `deployment.yaml`
- CrashLoopBackOff → check application logs for startup errors

### Health Check Failing

```bash
# Test health endpoint from within the cluster
kubectl run test-pod --image=busybox --rm -it --restart=Never -- \
  wget -qO- http://modresorts-service.modresorts.svc.cluster.local/health
```

The endpoint should return: `{"status":"UP"}`

### Redis Connection Issues

```bash
# Verify REDIS_HOST and REDIS_PORT environment variables
kubectl exec -it <POD_NAME> -n modresorts -- env | grep REDIS

# Test Redis connectivity from pod
kubectl exec -it <POD_NAME> -n modresorts -- \
  sh -c 'echo PING | nc $REDIS_HOST $REDIS_PORT'
```

### Ingress / ALB Not Provisioned

```bash
# Check AWS Load Balancer Controller logs
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller

# Verify ingress annotations
kubectl describe ingress modresorts-ingress -n modresorts
```

### JVM Memory Issues

If pods are being OOMKilled, adjust JVM settings in `deployment.yaml`:

```yaml
- name: JAVA_OPTS
  value: "-Xms128m -Xmx384m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

And increase memory limits:
```yaml
resources:
  limits:
    memory: "1.5Gi"
```

---

## Security Considerations

1. **Non-root container**: The Dockerfile creates and uses a non-root `appuser` account.
2. **Secrets management**: Store sensitive values (API keys, passwords) in Kubernetes Secrets or AWS Secrets Manager, not in plain environment variables.
3. **Network policies**: Consider adding Kubernetes NetworkPolicies to restrict pod-to-pod communication.
4. **Image scanning**: Enable ECR image scanning to detect vulnerabilities in the base image.
5. **RBAC**: Apply least-privilege RBAC roles for the service account running the pods.
6. **TLS**: Configure HTTPS on the ALB by adding an ACM certificate ARN annotation:
   ```yaml
   alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:<region>:<account>:certificate/<id>
   alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}]'
   ```
7. **Dependency updates**: Regularly update `pom.xml` dependencies (Log4j, Commons Collections, Jackson) to patch known CVEs.

---

## Java-Specific Notes

- **JVM Container Support**: `-XX:+UseContainerSupport` ensures the JVM respects container memory limits (available in Java 8u191+).
- **MaxRAMPercentage**: Set to 75% to leave headroom for the OS and non-heap memory.
- **Startup time**: Java 8 WAR applications on Tomcat typically take 30–60 seconds to start. The liveness probe has a 60-second initial delay to accommodate this.
- **Graceful shutdown**: Tomcat handles `SIGTERM` gracefully; `terminationGracePeriodSeconds: 30` gives in-flight requests time to complete.
- **Logging**: Application uses `java.util.logging` and Log4j2. Configure log levels via environment variables or a mounted `log4j2.xml` ConfigMap.
