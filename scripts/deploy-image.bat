@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: deploy-image.bat – Deploy modresorts to AWS EKS (Windows)
:: Prerequisites: aws-cli, kubectl
:: Usage        : scripts\deploy-image.bat   (run from repository root)
:: =============================================================================

echo ============================================
echo   ModResorts - Deploy to AWS EKS
echo ============================================
echo.

:: ── Collect deployment parameters ────────────────────────────────────────────
set /p "AWS_REGION=Enter AWS region (e.g. us-east-1): "
if "!AWS_REGION!"=="" (
    echo ERROR: AWS region is required.
    exit /b 1
)

set /p "CLUSTER_NAME=Enter EKS cluster name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS cluster name is required.
    exit /b 1
)

set /p "IMAGE_URI=Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

echo.
echo -- Application environment variables --
echo   (Press Enter to keep the placeholder value for later manual update)
echo.

set /p "REDIS_HOST_VAL=Enter REDIS_HOST (ElastiCache endpoint) [localhost]: "
if "!REDIS_HOST_VAL!"=="" set "REDIS_HOST_VAL=localhost"

set /p "REDIS_PORT_VAL=Enter REDIS_PORT [6379]: "
if "!REDIS_PORT_VAL!"=="" set "REDIS_PORT_VAL=6379"

set /p "WEATHER_API_KEY_VAL=Enter WEATHER_API_KEY (or press Enter to skip): "

set /p "SERVICE_DISCOVERY_URL_VAL=Enter SERVICE_DISCOVERY_URL [http://service-registry:8080]: "
if "!SERVICE_DISCOVERY_URL_VAL!"=="" set "SERVICE_DISCOVERY_URL_VAL=http://service-registry:8080"

set /p "CUSTOMER_SERVICE_URL_VAL=Enter CUSTOMER_SERVICE_URL [http://customer-service:8080]: "
if "!CUSTOMER_SERVICE_URL_VAL!"=="" set "CUSTOMER_SERVICE_URL_VAL=http://customer-service:8080"

:: ── Configure kubectl ─────────────────────────────────────────────────────────
echo.
echo Configuring kubectl for EKS cluster '!CLUSTER_NAME!' in '!AWS_REGION!'...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to EKS cluster.
    exit /b 1
)

:: ── Copy manifests to temp and substitute placeholders ────────────────────────
echo.
echo Updating Kubernetes manifests with deployment values...

copy /Y kubernetes\deployment.yaml %TEMP%\modresorts-deployment.yaml >nul
copy /Y kubernetes\service.yaml    %TEMP%\modresorts-service.yaml    >nul
copy /Y kubernetes\ingress.yaml    %TEMP%\modresorts-ingress.yaml    >nul
copy /Y kubernetes\namespace.yaml  %TEMP%\modresorts-namespace.yaml  >nul

:: Use PowerShell for sed-like substitution on Windows
powershell -Command "(Get-Content '%TEMP%\modresorts-deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' -replace '{{REDIS_HOST}}', '!REDIS_HOST_VAL!' -replace '{{REDIS_PORT}}', '!REDIS_PORT_VAL!' -replace '{{WEATHER_API_KEY}}', '!WEATHER_API_KEY_VAL!' -replace '{{SERVICE_DISCOVERY_URL}}', '!SERVICE_DISCOVERY_URL_VAL!' -replace '{{CUSTOMER_SERVICE_URL}}', '!CUSTOMER_SERVICE_URL_VAL!' | Set-Content '%TEMP%\modresorts-deployment.yaml'"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update deployment manifest.
    exit /b 1
)

:: ── Apply manifests ───────────────────────────────────────────────────────────
echo.
echo Applying Kubernetes manifests...

echo   [1/4] Applying namespace...
kubectl apply -f %TEMP%\modresorts-namespace.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply namespace.
    exit /b 1
)

echo   [2/4] Applying deployment...
kubectl apply -f %TEMP%\modresorts-deployment.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply deployment.
    exit /b 1
)

echo   [3/4] Applying service...
kubectl apply -f %TEMP%\modresorts-service.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply service.
    exit /b 1
)

echo   [4/4] Applying ingress...
kubectl apply -f %TEMP%\modresorts-ingress.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply ingress.
    exit /b 1
)

:: ── Wait for rollout ──────────────────────────────────────────────────────────
echo.
echo Waiting for deployment rollout to complete...
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo.
    echo ERROR: Deployment rollout timed out or failed.
    echo To rollback, run: kubectl rollout undo deployment/modresorts -n modresorts
    exit /b 1
)

:: ── Verify resources ──────────────────────────────────────────────────────────
echo.
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n modresorts

echo.
echo ============================================
echo   Deployment Complete!
echo   Image   : !IMAGE_URI!
echo   Cluster : !CLUSTER_NAME! (!AWS_REGION!)
echo   Check ingress URL: kubectl get ingress -n modresorts
echo ============================================
echo.
echo Rollback command (if needed):
echo   kubectl rollout undo deployment/modresorts -n modresorts

endlocal
