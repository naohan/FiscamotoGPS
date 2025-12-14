# Script para generar APK de FiscamotoGPS
# Ejecutar desde PowerShell en la carpeta del proyecto

Write-Host "🚀 Generando APK de FiscamotoGPS..." -ForegroundColor Green

# Verificar si estamos en la carpeta correcta
if (-not (Test-Path "app\build.gradle.kts")) {
    Write-Host "❌ Error: No se encuentra app\build.gradle.kts" -ForegroundColor Red
    Write-Host "   Asegúrate de estar en la carpeta FiscamotoGPS" -ForegroundColor Yellow
    exit 1
}

# Intentar usar gradlew.bat si existe
if (Test-Path "gradlew.bat") {
    Write-Host "📦 Usando Gradle Wrapper..." -ForegroundColor Cyan
    & cmd /c "gradlew.bat assembleDebug"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ APK generado exitosamente!" -ForegroundColor Green
        Write-Host "📍 Ubicación: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
    } else {
        Write-Host "❌ Error al generar APK" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "⚠️  No se encontró gradlew.bat" -ForegroundColor Yellow
    Write-Host "" 
    Write-Host "💡 Usa Android Studio para generar el APK:" -ForegroundColor Cyan
    Write-Host "   1. Abre Android Studio" -ForegroundColor White
    Write-Host "   2. Build → Build Bundle(s) / APK(s) → Build APK(s)" -ForegroundColor White
    Write-Host "   3. El APK estará en: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White
    exit 1
}

