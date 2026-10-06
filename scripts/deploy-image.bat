@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: deploy-image.bat – Deploy ModResorts to AWS EKS (Windows)
:: =============================================================================

set "APP_NAME=modresorts"
set "NAMESPACE=modresorts"

echo ============================================
echo   ModResorts - AWS EKS Deployment
echo ============================================
echo.

:: ── AWS / EKS configuration ──────────────────────────────────────────────────
set /p "AWS_REGION=Enter AWS Region [us-east-1]: "
if "!AWS_REGION!"=="" set "AWS_REGION=us-east-1"

set /p "CLUSTER_NAME=Enter EKS Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS Cluster Name is required.
    exit /b 1
)

:: ── Docker image URI ─────────────────────────────────────────────────────────
set /p "IMAGE_URI=Enter full Docker image URI: "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

:: ── Application environment variables ────────────────────────────────────────
echo.
echo --- Application Environment Variables ---
echo (Press Enter to keep default value)

set /p "REDIS_HOST_VAL=Enter REDIS_HOST (ElastiCache endpoint) [localhost]: "
if "!REDIS_HOST_VAL!"=="" set "REDIS_HOST_VAL=localhost"

set /p "REDIS_PORT_VAL=Enter REDIS_PORT [6379]: "
if "!REDIS_PORT_VAL!"=="" set "REDIS_PORT_VAL=6379"

set /p "WEATHER_SERVICE_HOST_VAL=Enter WEATHER_SERVICE_HOST [weather-service]: "
if "!WEATHER_SERVICE_HOST_VAL!"=="" set "WEATHER_SERVICE_HOST_VAL=weather-service"

set /p "WEATHER_SERVICE_PORT_VAL=Enter WEATHER_SERVICE_PORT [8080]: "
if "!WEATHER_SERVICE_PORT_VAL!"=="" set "WEATHER_SERVICE_PORT_VAL=8080"

set /p "WEATHER_SERVICE_BASE_PATH_VAL=Enter WEATHER_SERVICE_BASE_PATH [/api/weather]: "
if "!WEATHER_SERVICE_BASE_PATH_VAL!"=="" set "WEATHER_SERVICE_BASE_PATH_VAL=/api/weather"

set /p "WEATHER_API_KEY_VAL=Enter WEATHER_API_KEY (or press Enter to skip): "

:: ── Configure kubectl ─────────────────────────────────────────────────────────
echo.
echo Configuring kubectl for EKS cluster: !CLUSTER_NAME! ...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to cluster.
    exit /b 1
)

:: ── Copy manifests to temp directory ─────────────────────────────────────────
echo.
echo Updating Kubernetes manifests...
set "TMP_DIR=%TEMP%\modresorts-deploy-%RANDOM%"
mkdir "!TMP_DIR!"
copy "kubernetes\*.yaml" "!TMP_DIR!\" >nul

:: Replace placeholders using PowerShell
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '\{\{IMAGE_URI\}\}', '!IMAGE_URI!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '\{\{REDIS_HOST\}\}', '!REDIS_HOST_VAL!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '\{\{REDIS_PORT\}\}', '!REDIS_PORT_VAL!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '\{\{WEATHER_SERVICE_HOST\}\}', '!WEATHER_SERVICE_HOST_VAL!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '\{\{WEATHER_SERVICE_PORT\}\}', '!WEATHER_SERVICE_PORT_VAL!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '\{\{WEATHER_SERVICE_BASE_PATH\}\}', '!WEATHER_SERVICE_BASE_PATH_VAL!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '\{\{WEATHER_API_KEY\}\}', '!WEATHER_API_KEY_VAL!' | Set-Content '!TMP_DIR!\deployment.yaml'"

:: ── Apply manifests ───────────────────────────────────────────────────────────
echo.
echo Applying Kubernetes manifests...

echo   [1/4] Applying namespace...
kubectl apply -f "!TMP_DIR!\namespace.yaml"
if !ERRORLEVEL! neq 0 (echo ERROR: Failed to apply namespace. & exit /b 1)

echo   [2/4] Applying deployment...
kubectl apply -f "!TMP_DIR!\deployment.yaml"
if !ERRORLEVEL! neq 0 (echo ERROR: Failed to apply deployment. & exit /b 1)

echo   [3/4] Applying service...
kubectl apply -f "!TMP_DIR!\service.yaml"
if !ERRORLEVEL! neq 0 (echo ERROR: Failed to apply service. & exit /b 1)

echo   [4/4] Applying ingress...
kubectl apply -f "!TMP_DIR!\ingress.yaml"
if !ERRORLEVEL! neq 0 (echo ERROR: Failed to apply ingress. & exit /b 1)

:: ── Wait for rollout ──────────────────────────────────────────────────────────
echo.
echo Waiting for deployment rollout...
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed. Initiating rollback...
    kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
    exit /b 1
)

:: ── Verify resources ──────────────────────────────────────────────────────────
echo.
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n !NAMESPACE!

echo.
echo ============================================
echo   DEPLOYMENT COMPLETE
echo   Namespace : !NAMESPACE!
echo   Image     : !IMAGE_URI!
echo ============================================
echo.
echo Rollback command (if needed):
echo   kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!

:: Cleanup
rmdir /s /q "!TMP_DIR!" 2>nul

endlocal
exit /b 0
