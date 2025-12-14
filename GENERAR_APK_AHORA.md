# 🚀 Generar APK - Instrucciones Rápidas

## Método más rápido (Android Studio):

1. **Abre Android Studio** con el proyecto FiscamotoGPS

2. **Ve al menú**: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`

3. **Espera** a que termine la compilación (puede tardar unos minutos la primera vez)

4. **Cuando termine**, aparecerá una notificación. Haz clic en "locate" o ve manualmente a:
   ```
   app\build\outputs\apk\debug\app-debug.apk
   ```

5. **El APK estará listo** para instalar en tu dispositivo Android

## Ubicación del APK generado:
```
FiscamotoGPS\app\build\outputs\apk\debug\app-debug.apk
```

## Instalar en tu dispositivo:

1. Transfiere el archivo `.apk` a tu teléfono Android (por USB, email, etc.)
2. Abre el archivo desde el administrador de archivos del teléfono
3. Acepta instalar "desde fuentes desconocidas" si te lo pide
4. Instala la aplicación

## ⚠️ Nota:
- Este es un APK de **debug** (para pruebas)
- Funciona perfectamente para probar la aplicación
- No es para distribución pública (para eso necesitas un APK release firmado)

## Si prefieres usar la terminal:

Abre una terminal en la carpeta del proyecto y ejecuta:
```bash
gradlew.bat assembleDebug
```

O si tienes Gradle instalado globalmente:
```bash
gradle assembleDebug
```

