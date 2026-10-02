@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: build-push.bat – Build and push the modresorts Docker image (Windows)
:: Supports: AWS ECR and Docker Hub
:: Usage   : scripts\build-push.bat   (run from repository root)
:: =============================================================================

set "PROJECT_NAME=modresorts"
set "IMAGE_NAME=modresorts"

echo ============================================
echo   ModResorts - Docker Build ^& Push
echo ============================================
echo.

:: ── Registry selection ────────────────────────────────────────────────────────
echo Select target registry:
echo   1) AWS ECR
echo   2) Docker Hub
set /p "REGISTRY_CHOICE=Enter choice [1 or 2]: "

:: ── Image tag ─────────────────────────────────────────────────────────────────
set /p "RAW_TAG=Enter image tag (press Enter for 'latest'): "
if "!RAW_TAG!"=="" set "RAW_TAG=latest"
set "IMAGE_TAG=!RAW_TAG!"

:: ── Registry-specific setup ───────────────────────────────────────────────────
if "!REGISTRY_CHOICE!"=="1" goto ecr_setup
if "!REGISTRY_CHOICE!"=="2" goto dockerhub_setup
echo ERROR: Invalid choice '!REGISTRY_CHOICE!'. Please enter 1 or 2.
exit /b 1

:ecr_setup
set /p "AWS_REGION=Enter AWS region (e.g. us-east-1): "
set /p "AWS_ACCOUNT_ID=Enter AWS account ID: "
set /p "ECR_REPO_INPUT=Enter ECR repository name [!IMAGE_NAME!]: "
if "!ECR_REPO_INPUT!"=="" (
    set "ECR_REPO=!IMAGE_NAME!"
) else (
    set "ECR_REPO=!ECR_REPO_INPUT!"
)

set "REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com"
set "FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!"

echo.
echo Authenticating with AWS ECR...
aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
if !ERRORLEVEL! neq 0 (
    echo ERROR: ECR login failed.
    exit /b 1
)

echo Ensuring ECR repository '!ECR_REPO!' exists...
aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Creating ECR repository...
    aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECR repository.
        exit /b 1
    )
)
goto build_image

:dockerhub_setup
set /p "DOCKER_USERNAME=Enter Docker Hub username: "
set /p "DOCKER_PASSWORD=Enter Docker Hub password/token: "
set /p "REPO_INPUT=Enter Docker Hub repository name [!DOCKER_USERNAME!/!IMAGE_NAME!]: "
if "!REPO_INPUT!"=="" (
    set "REPO=!DOCKER_USERNAME!/!IMAGE_NAME!"
) else (
    set "REPO=!REPO_INPUT!"
)

set "FULL_IMAGE_NAME=!REPO!:!IMAGE_TAG!"

echo.
echo Authenticating with Docker Hub...
echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker Hub login failed.
    exit /b 1
)
goto build_image

:build_image
echo.
echo Building Docker image: !FULL_IMAGE_NAME!
docker build -f Dockerfile -t "!FULL_IMAGE_NAME!" .
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed.
    exit /b 1
)

echo.
echo Tagging image as !IMAGE_NAME!:!IMAGE_TAG! (local alias)...
docker tag "!FULL_IMAGE_NAME!" "!IMAGE_NAME!:!IMAGE_TAG!"

echo.
echo Pushing image: !FULL_IMAGE_NAME!
docker push "!FULL_IMAGE_NAME!"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed.
    exit /b 1
)

echo.
echo ============================================
echo   Build ^& Push Complete!
echo   Image : !FULL_IMAGE_NAME!
echo ============================================

endlocal
