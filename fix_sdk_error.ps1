# Script to fix Android SDK "Not in GZIP format" error
# This script clears corrupted cache files and retries the installation

$sdkPath = "C:\Users\Panha Ra\AppData\Local\Android\Sdk"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Android SDK Installation Error Fixer" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

if (Test-Path $sdkPath) {
    Write-Host "SDK Path found: $sdkPath" -ForegroundColor Green
    Write-Host ""
    
    # Method 1: Clear .temp directory
    $tempPath = Join-Path $sdkPath ".temp"
    if (Test-Path $tempPath) {
        Write-Host "Clearing .temp directory..." -ForegroundColor Yellow
        Remove-Item -Path "$tempPath\*" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "✓ Cleared .temp directory" -ForegroundColor Green
    }
    
    # Method 2: Clear .cache directory
    $cachePath = Join-Path $sdkPath ".cache"
    if (Test-Path $cachePath) {
        Write-Host "Clearing .cache directory..." -ForegroundColor Yellow
        Remove-Item -Path "$cachePath\*" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "✓ Cleared .cache directory" -ForegroundColor Green
    }
    
    # Method 3: Clear downloadIntermediates
    $downloadPath = Join-Path $sdkPath "downloadIntermediates"
    if (Test-Path $downloadPath) {
        Write-Host "Clearing downloadIntermediates..." -ForegroundColor Yellow
        Remove-Item -Path "$downloadPath\*" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "✓ Cleared downloadIntermediates" -ForegroundColor Green
    }
    
    # Method 4: Clear specific system image cache
    $sysImgPath = Join-Path $sdkPath "system-images\android-36\google_apis_playstore\x86_64"
    if (Test-Path $sysImgPath) {
        Write-Host "Clearing corrupted system image files..." -ForegroundColor Yellow
        Get-ChildItem -Path $sysImgPath -Filter "*.zip" -ErrorAction SilentlyContinue | Remove-Item -Force
        Write-Host "✓ Cleared corrupted system image files" -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host "Cache cleared successfully!" -ForegroundColor Green
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Open Android Studio" -ForegroundColor White
    Write-Host "2. Go to Tools > SDK Manager" -ForegroundColor White
    Write-Host "3. Try installing the system image again" -ForegroundColor White
    Write-Host ""
    Write-Host "If the error persists, try:" -ForegroundColor Yellow
    Write-Host "- Check your internet connection" -ForegroundColor White
    Write-Host "- Disable VPN/proxy if using one" -ForegroundColor White
    Write-Host "- Try installing from SDK Manager > SDK Tools tab" -ForegroundColor White
    Write-Host "- Use command line: sdkmanager 'system-images;android-36;google_apis_playstore;x86_64'" -ForegroundColor White
    
} else {
    Write-Host "ERROR: SDK path not found at: $sdkPath" -ForegroundColor Red
    Write-Host "Please verify your SDK installation path." -ForegroundColor Yellow
}

Write-Host ""
Read-Host "Press Enter to exit"

