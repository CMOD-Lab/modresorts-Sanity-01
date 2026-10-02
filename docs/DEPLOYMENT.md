# ModResorts – Deployment Guide (AWS EKS)

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Local Development with Docker Compose](#local-development-with-docker-compose)
5. [Build and Push Docker Image](#build-and-push-docker-image)
6. [AWS EKS Deployment](#aws-eks-deployment)
7. [Kubernetes Manifest Reference](#kubernetes-manifest-reference)
8. [Environment Variables](#environment-variables)
9. [Health Checks](#health-checks)
10. [Scaling and Management](#scaling-and-management)
11. [Troubleshooting](#troubleshooting)
12. [Security Considerations](#security-considerations)
13. [Java / Open Liberty Notes](#java--open-liberty-notes)

---

## Overview

**ModResorts** is a Java EE 7 web application packaged as a WAR and deployed on **Open Liberty**. It exposes a resort booking and weather information portal under the `/resorts` context root.

| Property | Value |
|---|---|
| Build tool | Maven 3.x |
| Java version | 8 (eclipse-temurin) |
| Packaging | WAR |
| Application server | Open Liberty 23.x |
| Application port | 9080 |
| Context root | `/resorts` |
| Health endpoint | `GET /resorts/health` |
| Target platform | AWS EKS |

---

## Prerequisites

### Local Development
| Tool | Minimum Version |
|---|---|
| Docker Desktop | 24.x |
| Docker Compose | v2.x |
| Java JDK | 8+ |
| Maven | 3.8+ |

### AWS EKS Deployment
| Tool | Notes |
|---|---|
| AWS CLI | v2 – configured with IAM credentials |
| kubectl | Matching your EKS cluster version |
| eksctl | Optional – for cluster creation |
| AWS Load Balancer Controller | Installed on the EKS cluster |

### IAM Permissions Required
- `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:PutImage`
- `eks:DescribeCluster`, `eks:UpdateKubeconfig`
- `elasticloadbalancing:*` (for ALB Ingress)

---

## Project Structure

```
modresorts-MContMono/
├── Dockerfile                    # Multi-stage build (Maven builder + Liberty runtime)
├── docker-compose.yml            # Local development (application only)
├── .dockerignore                 # Excludes wrapper files, target/, .git/, etc.
├── docker/
│   └── server.xml                # Open Liberty server configuration
├── kubernetes/
│   ├── namespace.yaml            # Kubernetes namespace: modresorts
│   ├── deployment.yaml           # Deployment (2 replicas, health probes)
│   ├── service.yaml              # ClusterIP Service (port 80 → 9080)
│   └── ingress.yaml              # AWS ALB Ingress
├── scripts/
│   ├── build-push.sh             # Linux/macOS: build & push to ECR or Docker Hub
│   ├── build-push.bat            # Windows: build & push to ECR or Docker Hub
│   ├── deploy-image.sh           # Linux/macOS: deploy to EKS
│   └── deploy-image.bat          # Windows: deploy to EKS
├── src/                          # Java source code
├── WebContent/                   # Static web resources, JSPs, WEB-INF
└── pom.xml                       # Maven build descriptor
```

---

## Local Development with Docker Compose

### 1. Configure environment variables

Create a `.env` file in the project root (never commit this file):

```bash
# .env
REDIS_HOST=your-elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379
WEATHER_API_KEY=your_weather_api_key_here
SERVICE_DISCOVERY_URL=http://service-registry:8080
CUSTOMER_SERVICE_URL=http://customer-service:8080
```

### 2. Build and start the application

```bash
# Build the image and start the container
docker compose up --build

# Run in background
docker compose up --build -d
```

### 3. Access the application

| URL | Description |
|---|---|
| `http://localhost:9080/resorts/` | Application home |
| `http://localhost:9080/resorts/health` | Health check endpoint |
| `http://localhost:9080/resorts/weather?selectedCity=Paris` | Weather API |

### 4. Stop the application

```bash
docker compose down
```

---

## Build and Push Docker Image

### Linux / macOS

```bash
# Make the script executable (first time only)
chmod +x scripts/build-push.sh

# Run from the repository root
bash scripts/build-push.sh
```

The script will prompt you to:
1. Select registry type (AWS ECR or Docker Hub)
2. Enter registry credentials and details
3. Enter an image tag (defaults to `latest`)

### Windows

```cmd
scripts\build-push.bat
```

### Manual Docker Build

```bash
# Build
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

### Step 1 – Create or connect to an EKS cluster

```bash
# Create a new cluster (optional)
eksctl create cluster \
  --name modresorts-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4

# Configure kubectl for an existing cluster
aws eks update-kubeconfig --region us-east-1 --name modresorts-cluster
```

### Step 2 – Install AWS Load Balancer Controller

The Ingress manifest uses the AWS ALB Ingress Controller. Install it if not already present:

```bash
# Add the EKS chart repository
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install the controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=modresorts-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Step 3 – Deploy the application

#### Linux / macOS

```bash
chmod +x scripts/deploy-image.sh
bash scripts/deploy-image.sh
```

#### Windows

```cmd
scripts\deploy-image.bat
```

The script will prompt for:
- AWS region and EKS cluster name
- Full Docker image URI (e.g. `123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest`)
- Application environment variables (REDIS_HOST, REDIS_PORT, WEATHER_API_KEY, etc.)

### Step 4 – Verify the deployment

```bash
# Check pod status
kubectl get pods -n modresorts

# Check service
kubectl get svc -n modresorts

# Check ingress (ALB hostname appears after ~2 minutes)
kubectl get ingress -n modresorts

# View pod logs
kubectl logs -l app=modresorts -n modresorts --tail=100
```

### Step 5 – Access the application

Once the ALB is provisioned, retrieve the hostname:

```bash
kubectl get ingress modresorts-ingress -n modresorts \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

Access the application at: `http://<ALB_HOSTNAME>/resorts/`

---

## Kubernetes Manifest Reference

### namespace.yaml
Creates the `modresorts` namespace to isolate all application resources.

### deployment.yaml
- **Replicas**: 2 (for high availability)
- **Image**: Placeholder `{{IMAGE_URI}}` replaced by `deploy-image.sh`
- **Port**: 9080 (Open Liberty HTTP)
- **Liveness probe**: `GET /resorts/health` – restarts unresponsive pods
- **Readiness probe**: `GET /resorts/health` – gates traffic until ready
- **Resources**: requests `250m CPU / 512Mi RAM`, limits `500m CPU / 1Gi RAM`
- **Topology spread**: Pods distributed across availability zones

### service.yaml
- **Type**: ClusterIP (internal cluster access)
- **Port mapping**: 80 → 9080

### ingress.yaml
- **Controller**: AWS Load Balancer Controller (ALB)
- **Scheme**: internet-facing
- **Target type**: IP (direct pod routing)
- **Health check path**: `/resorts/health`

---

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `REDIS_HOST` | Yes | `localhost` | Amazon ElastiCache primary endpoint |
| `REDIS_PORT` | No | `6379` | ElastiCache port |
| `WEATHER_API_KEY` | No | _(empty)_ | Weather Underground API key; falls back to static data if absent |
| `SERVICE_DISCOVERY_URL` | No | `http://service-registry:8080` | Kubernetes DNS URL for service discovery |
| `CUSTOMER_SERVICE_URL` | No | `http://customer-service:8080` | URL of the independent Customer microservice |
| `JAVA_OPTS` | No | See Dockerfile | JVM tuning flags |
| `TZ` | No | `UTC` | Container timezone |

### Using Kubernetes Secrets for sensitive values

```bash
# Create a secret for the weather API key
kubectl create secret generic modresorts-secrets \
  --from-literal=WEATHER_API_KEY=your_api_key_here \
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

## Health Checks

The application exposes a dedicated health endpoint implemented in `HealthCheckServlet`:

```
GET /resorts/health
```

**Response (HTTP 200)**:
```json
{"status":"UP","application":"modresorts"}
```

Both the Kubernetes liveness and readiness probes use this endpoint.

| Probe | Path | Initial Delay | Period | Failure Threshold |
|---|---|---|---|---|
| Liveness | `/resorts/health` | 60s | 20s | 3 |
| Readiness | `/resorts/health` | 45s | 10s | 3 |

> **Note**: The initial delay is set to 60s to allow Open Liberty and the JVM to fully start before probes begin.

---

## Scaling and Management

### Manual scaling

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

### Rolling update

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
kubectl rollout undo deployment/modresorts -n modresorts

# Rollback to a specific revision
kubectl rollout undo deployment/modresorts --to-revision=2 -n modresorts
```

---

## Troubleshooting

### Pods not starting

```bash
# Describe the pod for events
kubectl describe pod -l app=modresorts -n modresorts

# Check container logs
kubectl logs -l app=modresorts -n modresorts --previous
```

**Common causes**:
- `ImagePullBackOff` – ECR credentials not configured or image URI incorrect
- `CrashLoopBackOff` – Application startup failure; check logs for Java exceptions
- `OOMKilled` – Increase memory limits in `deployment.yaml`

### Health probe failures

```bash
# Exec into a running pod to test the health endpoint
kubectl exec -it <POD_NAME> -n modresorts -- \
  sh -c 'wget -qO- http://localhost:9080/resorts/health'
```

### Redis connection issues

Verify the `REDIS_HOST` environment variable points to the correct ElastiCache endpoint:

```bash
kubectl exec -it <POD_NAME> -n modresorts -- \
  sh -c 'echo $REDIS_HOST'
```

### Ingress / ALB not provisioning

```bash
# Check AWS Load Balancer Controller logs
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller

# Verify ingress annotations
kubectl describe ingress modresorts-ingress -n modresorts
```

### Service not reachable

```bash
# Verify endpoints are populated
kubectl get endpoints modresorts-service -n modresorts

# Port-forward for local testing
kubectl port-forward svc/modresorts-service 9080:80 -n modresorts
# Then access: http://localhost:9080/resorts/
```

---

## Security Considerations

1. **Non-root container**: The Dockerfile creates a dedicated `appuser` account; the application never runs as root.
2. **Secrets management**: Store `WEATHER_API_KEY` and database credentials in Kubernetes Secrets, not ConfigMaps.
3. **Network policies**: Consider adding Kubernetes NetworkPolicy resources to restrict pod-to-pod traffic.
4. **Image scanning**: Enable ECR image scanning on push to detect vulnerabilities.
5. **RBAC**: Apply least-privilege RBAC roles for the service account used by the deployment.
6. **TLS**: Configure HTTPS on the ALB using an ACM certificate:
   ```yaml
   alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:<REGION>:<ACCOUNT>:certificate/<ID>
   alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}]'
   ```

---

## Java / Open Liberty Notes

### JVM Memory Tuning

The Dockerfile sets container-aware JVM flags:

```
-Xmx512m -Xms256m
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
```

Adjust `JAVA_OPTS` in `deployment.yaml` if you change the memory limits.

### Open Liberty Features

The `docker/server.xml` enables:
- `servlet-3.1` – Java Servlet 3.1 support
- `jsp-2.3` – JavaServer Pages
- `jndi-1.0` – JNDI lookups
- `localConnector-1.0` – JMX local connector

### Logging

Open Liberty writes logs to stdout/stderr by default, which Kubernetes captures automatically. View logs with:

```bash
kubectl logs -l app=modresorts -n modresorts -f
```

### Graceful Shutdown

Open Liberty handles `SIGTERM` gracefully, completing in-flight requests before shutting down. The default quiesce timeout is 30 seconds.
