# 📱 Guía para Exportar APK de FiscamotoGPS

## Opción 1: Desde Android Studio (Más Fácil) ✅

### Pasos:
1. **Abre Android Studio**
2. **Ve al menú**: `Build` → `Generate Signed Bundle / APK...`
3. **Selecciona**: `APK` (o `Android App Bundle` si lo necesitas)
4. **Crear Keystore (si no tienes uno)**:
   - Click en "Create new..."
   - Completa los campos:
     - Key store path: `fiscamoto-release-key.jks`
     - Password: (guarda esta contraseña bien)
     - Key alias: `fiscamoto-key`
     - Key password: (puede ser la misma)
     - Validity: 25 años
     - Nombre, Organización, etc.
5. **Selecciona el keystore creado**
6. **Build type**: Selecciona `release`
7. **Click "Finish"**
8. **Ubicación del APK**: 
   ```
   FiscamotoGPS/app/release/app-release.apk
   ```

## Opción 2: Desde Terminal/Línea de Comandos 🔧

### Para generar APK de Debug (sin firmar):
```bash
cd FiscamotoGPS
./gradlew assembleDebug
```
APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

### Para generar APK de Release (requiere keystore):
```bash
cd FiscamotoGPS
./gradlew assembleRelease
```
APK estará en: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Opción 3: Build Simple sin Firmar (Para Pruebas)

Si solo quieres probar rápidamente sin firmar:

1. En Android Studio: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. Espera a que termine la compilación
3. Click en el enlace "locate" que aparece al finalizar
4. O ve a: `app/build/outputs/apk/debug/app-debug.apk`

⚠️ **Nota**: Los APKs sin firmar no se pueden instalar en dispositivos directamente desde el archivo. Para pruebas, mejor usa `Build APK(s)` que genera un APK debug que sí se puede instalar.

## 📦 Instalar el APK en tu dispositivo

1. Transfiere el archivo `.apk` a tu dispositivo Android
2. Abre el archivo desde el administrador de archivos
3. Acepta instalar "desde fuentes desconocidas" si te lo pide
4. Instala la aplicación

## 🔐 Notas Importantes:

- **Guarda bien** tu archivo `.jks` y contraseñas - los necesitarás para futuras actualizaciones
- Para distribuir en Google Play Store, necesitas un APK o AAB firmado
- El APK debug es solo para pruebas, no para distribución pública




