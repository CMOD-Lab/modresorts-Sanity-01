@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: deploy-image.bat - Deploy ModResorts to AWS EKS (Windows)
:: Usage: scripts\deploy-image.bat
:: Run from the repository root directory
:: =============================================================================

echo ==============================================
echo   ModResorts - AWS EKS Deployment Script
echo ==============================================
echo.

:: -----------------------------------------------------------------------
:: Collect deployment parameters
:: -----------------------------------------------------------------------
set /p AWS_REGION="Enter AWS Region (e.g. us-east-1): "
if "!AWS_REGION!"=="" (
    echo ERROR: AWS Region is required.
    exit /b 1
)

set /p CLUSTER_NAME="Enter EKS Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS Cluster Name is required.
    exit /b 1
)

set /p IMAGE_URI="Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

echo.
echo --- Optional: Application Environment Variables ---
echo Press Enter to skip any variable.
echo.

set /p WEATHER_API_KEY_VAL="Enter WEATHER_API_KEY (or press Enter to skip): "
set /p WEATHER_SERVICE_BASE_URL_VAL="Enter WEATHER_SERVICE_BASE_URL (or press Enter to skip): "
set /p REDIS_HOST_VAL="Enter REDIS_HOST (or press Enter to skip): "
set /p REDIS_PORT_VAL="Enter REDIS_PORT (or press Enter to skip): "
set /p NAMING_SERVICE_HOST_VAL="Enter NAMING_SERVICE_HOST (or press Enter to skip): "
set /p NAMING_SERVICE_PORT_VAL="Enter NAMING_SERVICE_PORT (or press Enter to skip): "

:: Apply defaults
if "!WEATHER_SERVICE_BASE_URL_VAL!"=="" set WEATHER_SERVICE_BASE_URL_VAL=http://api.wunderground.com/api/
if "!REDIS_HOST_VAL!"==""               set REDIS_HOST_VAL=localhost
if "!REDIS_PORT_VAL!"==""               set REDIS_PORT_VAL=6379
if "!NAMING_SERVICE_HOST_VAL!"==""      set NAMING_SERVICE_HOST_VAL=naming-service
if "!NAMING_SERVICE_PORT_VAL!"==""      set NAMING_SERVICE_PORT_VAL=8080

:: -----------------------------------------------------------------------
:: Update Kubernetes manifests
:: -----------------------------------------------------------------------
echo.
echo Updating Kubernetes manifests...

copy kubernetes\deployment.yaml kubernetes\deployment.yaml.bak >nul

powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{WEATHER_API_KEY}}', '!WEATHER_API_KEY_VAL!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{WEATHER_SERVICE_BASE_URL}}', '!WEATHER_SERVICE_BASE_URL_VAL!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{REDIS_HOST}}', '!REDIS_HOST_VAL!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{REDIS_PORT}}', '!REDIS_PORT_VAL!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{NAMING_SERVICE_HOST}}', '!NAMING_SERVICE_HOST_VAL!' | Set-Content kubernetes\deployment.yaml"
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{NAMING_SERVICE_PORT}}', '!NAMING_SERVICE_PORT_VAL!' | Set-Content kubernetes\deployment.yaml"

echo Manifests updated.

:: -----------------------------------------------------------------------
:: Configure kubectl for EKS
:: -----------------------------------------------------------------------
echo.
echo Configuring kubectl for EKS cluster '!CLUSTER_NAME!' in region '!AWS_REGION!'...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl for EKS.
    copy kubernetes\deployment.yaml.bak kubernetes\deployment.yaml >nul
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to EKS cluster.
    copy kubernetes\deployment.yaml.bak kubernetes\deployment.yaml >nul
    exit /b 1
)

:: -----------------------------------------------------------------------
:: Apply Kubernetes manifests
:: -----------------------------------------------------------------------
echo.
echo Applying Kubernetes manifests...

echo   [1/4] Applying namespace...
kubectl apply -f kubernetes\namespace.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply namespace.
    copy kubernetes\deployment.yaml.bak kubernetes\deployment.yaml >nul
    exit /b 1
)

echo   [2/4] Applying deployment...
kubectl apply -f kubernetes\deployment.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply deployment.
    copy kubernetes\deployment.yaml.bak kubernetes\deployment.yaml >nul
    exit /b 1
)

echo   [3/4] Applying service...
kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply service.
    copy kubernetes\deployment.yaml.bak kubernetes\deployment.yaml >nul
    exit /b 1
)

echo   [4/4] Applying ingress...
kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply ingress.
    copy kubernetes\deployment.yaml.bak kubernetes\deployment.yaml >nul
    exit /b 1
)

:: -----------------------------------------------------------------------
:: Wait for rollout
:: -----------------------------------------------------------------------
echo.
echo Waiting for deployment rollout to complete...
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed. Rolling back...
    kubectl rollout undo deployment/modresorts -n modresorts
    copy kubernetes\deployment.yaml.bak kubernetes\deployment.yaml >nul
    exit /b 1
)

:: -----------------------------------------------------------------------
:: Verify resources
:: -----------------------------------------------------------------------
echo.
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n modresorts

echo.
echo ==============================================
echo   Deployment Complete!
echo   Image:   !IMAGE_URI!
echo   Cluster: !CLUSTER_NAME! (!AWS_REGION!)
echo   Check ingress: kubectl get ingress -n modresorts
echo ==============================================

:: Restore original manifest template
copy kubernetes\deployment.yaml.bak kubernetes\deployment.yaml >nul
del kubernetes\deployment.yaml.bak >nul 2>&1

endlocal
