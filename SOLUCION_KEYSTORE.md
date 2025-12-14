# 🔐 Solución: Error de Keystore

## ❌ Tu Error:
```
Keystore file 'C:\Users\Tecsup\.gradle\daemon\8.7\fiscamoto-release-key.jks' not found
```

## ✅ Soluciones (elige una):

### **Opción 1: Generar APK Debug (Más Rápido - Para Pruebas)** ⚡

Esta opción NO requiere keystore y es perfecta para probar en tu dispositivo:

1. En Android Studio: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. Espera a que compile
3. Click en "locate" cuando termine
4. El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`
5. **Puedes instalar este APK en tu dispositivo directamente**

✅ **Ventajas**: Rápido, sin configuración, funciona perfecto para pruebas
❌ **Desventajas**: No es para distribuir en Play Store

---

### **Opción 2: Crear Keystore y Generar APK Firmado** 🔐

Si necesitas un APK firmado (para Play Store o distribución):

#### Paso 1: Crear el Keystore

1. En Android Studio: `Build` → `Generate Signed Bundle / APK...`
2. Selecciona `APK`
3. Click en **"Create new..."** (botón para crear nuevo keystore)
4. Completa el formulario:
   - **Key store path**: Navega a tu carpeta del proyecto `FiscamotoGPS` y guarda como `fiscamoto-release-key.jks`
   - **Password**: Crea una contraseña (GUÁRDALA BIEN - la necesitarás siempre)
   - **Key alias**: `fiscamoto-key`
   - **Key password**: Puede ser la misma que Password
   - **Validity**: 25 años (máximo recomendado)
   - **Certificate**: Llena tu información (Nombre, Organización, etc.)
5. Click "OK"

#### Paso 2: Usar el Keystore

1. Selecciona el keystore que acabas de crear
2. Ingresa las contraseñas
3. Build variant: `release`
4. Click "Finish"

#### Paso 3: Descomentar la configuración en build.gradle.kts

Después de crear el keystore, actualiza `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("fiscamoto-release-key.jks") // Ruta relativa al proyecto
        storePassword = "tu_contraseña"
        keyAlias = "fiscamoto-key"
        keyPassword = "tu_contraseña"
    }
}

buildTypes {
    release {
        isMinifyEnabled = false
        signingConfig = signingConfigs.getByName("release")
        // ...
    }
}
```

⚠️ **ADVERTENCIA**: Si guardas las contraseñas en el código, NUNCA lo subas a Git público.

**Mejor opción - usar variables de entorno o archivo local:**

1. Crea `keystore.properties` en la raíz del proyecto (NO lo subas a Git):
```properties
storePassword=tu_contraseña
keyPassword=tu_contraseña
keyAlias=fiscamoto-key
storeFile=fiscamoto-release-key.jks
```

2. En `build.gradle.kts`, antes de `android {`:
```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
```

3. Luego en `signingConfigs`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
    }
}
```

---

### **Opción 3: Generar APK Release sin Firma (Temporal)**

Ya modifiqué `build.gradle.kts` para que puedas generar un APK release sin firma:

```bash
cd FiscamotoGPS
./gradlew assembleRelease
```

Esto generará: `app/build/outputs/apk/release/app-release-unsigned.apk`

⚠️ **Nota**: Este APK NO se puede instalar directamente en dispositivos, pero puedes firmarlo después.

---

## 📝 Resumen:

- **Para pruebas rápidas**: Usa Opción 1 (APK Debug) ✅ RECOMENDADO
- **Para distribución**: Usa Opción 2 (Crear Keystore) 
- **Temporal**: Usa Opción 3 (Release sin firma)

## 🚨 Importante:

**GUARDA BIEN tu keystore y contraseñas**. Si los pierdes:
- ❌ No podrás actualizar tu app en Play Store
- ❌ Tendrás que crear una nueva app con diferente package name
- ❌ Los usuarios tendrán que desinstalar y reinstalar

Guarda una copia del `.jks` en un lugar seguro (drive, respaldo, etc.)




