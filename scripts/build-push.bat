@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: build-push.bat - Build and push ModResorts Docker image (Windows)
:: Usage: scripts\build-push.bat
:: Run from the repository root directory
:: =============================================================================

set PROJECT_NAME=modresorts
set DOCKERFILE_PATH=Dockerfile

echo ==============================================
echo   ModResorts - Docker Build ^& Push Script
echo ==============================================
echo.

:: Sanitize image name using PowerShell
for /f "delims=" %%i in ('powershell -Command "\"modresorts\" -replace '[^a-z0-9]','-' -replace '^-+','' -replace '-+$',''"') do set IMAGE_NAME=%%i

:: Prompt for image tag
set /p IMAGE_TAG_INPUT="Enter image tag [latest]: "
if "!IMAGE_TAG_INPUT!"=="" set IMAGE_TAG_INPUT=latest
for /f "delims=" %%i in ('powershell -Command "\"!IMAGE_TAG_INPUT!\" -replace '[^a-z0-9._-]','-' -replace '^-+','' -replace '-+$','' -replace '  +',' '"') do set IMAGE_TAG=%%i
if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest

echo.
echo Select container registry:
echo   1. AWS ECR (Elastic Container Registry)
echo   2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice [1 or 2]: "

echo.

:: -----------------------------------------------------------------------
:: AWS ECR
:: -----------------------------------------------------------------------
if "!REGISTRY_CHOICE!"=="1" (
    set /p AWS_REGION="Enter AWS Region (e.g. us-east-1): "
    set /p AWS_ACCOUNT_ID="Enter AWS Account ID (12-digit): "

    if "!AWS_REGION!"=="" (
        echo ERROR: AWS Region is required.
        exit /b 1
    )
    if "!AWS_ACCOUNT_ID!"=="" (
        echo ERROR: AWS Account ID is required.
        exit /b 1
    )

    set ECR_REPO=!IMAGE_NAME!
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!

    echo Authenticating with AWS ECR...
    aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: ECR login failed.
        exit /b 1
    )

    echo Checking ECR repository !ECR_REPO!...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Creating ECR repository !ECR_REPO!...
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
        if !ERRORLEVEL! neq 0 (
            echo ERROR: Failed to create ECR repository.
            exit /b 1
        )
    )

    goto BUILD_IMAGE
)

:: -----------------------------------------------------------------------
:: Docker Hub
:: -----------------------------------------------------------------------
if "!REGISTRY_CHOICE!"=="2" (
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub password/token: "

    if "!DOCKER_USERNAME!"=="" (
        echo ERROR: Docker Hub username is required.
        exit /b 1
    )
    if "!DOCKER_PASSWORD!"=="" (
        echo ERROR: Docker Hub password is required.
        exit /b 1
    )

    set REGISTRY_URL=docker.io
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!IMAGE_NAME!:!IMAGE_TAG!

    echo Authenticating with Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Docker Hub login failed.
        exit /b 1
    )

    goto BUILD_IMAGE
)

echo ERROR: Invalid choice. Please enter 1 or 2.
exit /b 1

:: -----------------------------------------------------------------------
:: Build Docker image
:: -----------------------------------------------------------------------
:BUILD_IMAGE
echo.
echo Building Docker image: !FULL_IMAGE_NAME!
echo Build context: . (repository root)
echo Dockerfile: %DOCKERFILE_PATH%
echo.

docker build -f %DOCKERFILE_PATH% -t !FULL_IMAGE_NAME! .
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed.
    exit /b 1
)

echo.
echo Docker image built successfully: !FULL_IMAGE_NAME!

:: -----------------------------------------------------------------------
:: Push Docker image
:: -----------------------------------------------------------------------
echo.
echo Pushing image to registry...
docker push !FULL_IMAGE_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed.
    exit /b 1
)

echo.
echo ==============================================
echo   Image pushed successfully!
echo   Image: !FULL_IMAGE_NAME!
echo ==============================================
echo.
echo Use this image URI in your Kubernetes deployment:
echo   !FULL_IMAGE_NAME!

endlocal
