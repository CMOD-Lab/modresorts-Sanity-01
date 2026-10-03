@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: deploy-image.bat - Deploy ModResorts to AWS EKS (Windows)
:: Usage: scripts\deploy-image.bat
:: Run from the repository root directory
:: Prerequisites: aws-cli, kubectl
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
echo --- Application Environment Variables ---
echo Press Enter to skip any optional variable.
echo.

set /p WEATHER_API_KEY_VAL="Enter WEATHER_API_KEY (Weather Underground API key) [optional]: "
set /p REDIS_HOST_VAL="Enter REDIS_HOST (Amazon ElastiCache endpoint) [optional, default: localhost]: "
if "!REDIS_HOST_VAL!"=="" set REDIS_HOST_VAL=localhost
set /p REDIS_PORT_VAL="Enter REDIS_PORT (Redis port) [optional, default: 6379]: "
if "!REDIS_PORT_VAL!"=="" set REDIS_PORT_VAL=6379
set /p CUSTOMER_INFO_SERVICE_URL_VAL="Enter CUSTOMER_INFO_SERVICE_URL (Customer info microservice URL) [optional]: "
set /p SERVICE_DISCOVERY_URL_VAL="Enter SERVICE_DISCOVERY_URL (Service discovery endpoint) [optional]: "

echo.

:: -----------------------------------------------------------------------
:: Configure kubectl for EKS
:: -----------------------------------------------------------------------
echo Configuring kubectl for EKS cluster '!CLUSTER_NAME!' in region '!AWS_REGION!'...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl. Check your AWS credentials and cluster name.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to EKS cluster.
    exit /b 1
)

echo.

:: -----------------------------------------------------------------------
:: Update Kubernetes manifests with actual values using PowerShell
:: -----------------------------------------------------------------------
echo Updating Kubernetes manifests with deployment values...

:: Copy manifests to temp directory
copy kubernetes\deployment.yaml %TEMP%\modresorts-deployment.yaml >nul
copy kubernetes\service.yaml %TEMP%\modresorts-service.yaml >nul
copy kubernetes\ingress.yaml %TEMP%\modresorts-ingress.yaml >nul
copy kubernetes\namespace.yaml %TEMP%\modresorts-namespace.yaml >nul

:: Replace placeholders using PowerShell
powershell -Command "(Get-Content '%TEMP%\modresorts-deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '%TEMP%\modresorts-deployment.yaml'"
powershell -Command "(Get-Content '%TEMP%\modresorts-deployment.yaml') -replace '{{WEATHER_API_KEY}}', '!WEATHER_API_KEY_VAL!' | Set-Content '%TEMP%\modresorts-deployment.yaml'"
powershell -Command "(Get-Content '%TEMP%\modresorts-deployment.yaml') -replace '{{REDIS_HOST}}', '!REDIS_HOST_VAL!' | Set-Content '%TEMP%\modresorts-deployment.yaml'"
powershell -Command "(Get-Content '%TEMP%\modresorts-deployment.yaml') -replace '{{REDIS_PORT}}', '!REDIS_PORT_VAL!' | Set-Content '%TEMP%\modresorts-deployment.yaml'"
powershell -Command "(Get-Content '%TEMP%\modresorts-deployment.yaml') -replace '{{CUSTOMER_INFO_SERVICE_URL}}', '!CUSTOMER_INFO_SERVICE_URL_VAL!' | Set-Content '%TEMP%\modresorts-deployment.yaml'"
powershell -Command "(Get-Content '%TEMP%\modresorts-deployment.yaml') -replace '{{SERVICE_DISCOVERY_URL}}', '!SERVICE_DISCOVERY_URL_VAL!' | Set-Content '%TEMP%\modresorts-deployment.yaml'"

echo Manifests updated successfully.
echo.

:: -----------------------------------------------------------------------
:: Apply Kubernetes manifests in order
:: -----------------------------------------------------------------------
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

echo.

:: -----------------------------------------------------------------------
:: Wait for rollout
:: -----------------------------------------------------------------------
echo Waiting for deployment rollout to complete...
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo.
    echo ERROR: Deployment rollout failed or timed out.
    echo To rollback, run:
    echo   kubectl rollout undo deployment/modresorts -n modresorts
    exit /b 1
)

echo.
echo Deployment rollout completed successfully!
echo.

:: -----------------------------------------------------------------------
:: Verify resources
:: -----------------------------------------------------------------------
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n modresorts

echo.

:: -----------------------------------------------------------------------
:: Display application URL
:: -----------------------------------------------------------------------
echo Retrieving application URL from ingress...
for /f "delims=" %%i in ('kubectl get ingress modresorts-ingress -n modresorts -o jsonpath^="{.status.loadBalancer.ingress[0].hostname}" 2^>nul') do set INGRESS_HOST=%%i

if not "!INGRESS_HOST!"=="" (
    echo.
    echo ==============================================
    echo   Deployment Successful!
    echo   Application URL: http://!INGRESS_HOST!
    echo   Health Check:    http://!INGRESS_HOST!/health
    echo ==============================================
) else (
    echo.
    echo ==============================================
    echo   Deployment Successful!
    echo   Ingress host is still provisioning.
    echo   Run the following to check status:
    echo     kubectl get ingress modresorts-ingress -n modresorts
    echo ==============================================
)

echo.
echo Rollback command (if needed):
echo   kubectl rollout undo deployment/modresorts -n modresorts
echo.

:: Cleanup temp files
del /f /q %TEMP%\modresorts-deployment.yaml %TEMP%\modresorts-service.yaml >nul 2>&1
del /f /q %TEMP%\modresorts-ingress.yaml %TEMP%\modresorts-namespace.yaml >nul 2>&1

endlocal
