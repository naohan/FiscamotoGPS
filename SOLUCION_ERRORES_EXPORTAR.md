# 🔧 Solución de Errores al Exportar APK

## Errores Comunes y Soluciones

### ❌ Error 1: "Keystore file not found" o "Keystore was tampered with"

**Solución:**
1. Si es la primera vez, crea un nuevo keystore desde Android Studio
2. Si ya tienes uno, verifica la ruta y contraseña
3. Si olvidaste la contraseña, tendrás que crear un nuevo keystore (pero no podrás actualizar la app existente)

### ❌ Error 2: "Duplicate class found" o "Conflict with dependency"

**Solución:**
```kotlin
// En app/build.gradle.kts, agrega esto en android { packaging {
packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "/META-INF/DEPENDENCIES"
        excludes += "/META-INF/LICENSE"
        excludes += "/META-INF/LICENSE.txt"
        excludes += "/META-INF/license.txt"
        excludes += "/META-INF/NOTICE"
        excludes += "/META-INF/NOTICE.txt"
        excludes += "/META-INF/notice.txt"
    }
}
```

### ❌ Error 3: "OutOfMemoryError" o "Java heap space"

**Solución:**
1. Abre `gradle.properties`
2. Aumenta la memoria:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```
3. En Android Studio: File → Settings → Build → Gradle → Gradle VM options: `-Xmx4096m`

### ❌ Error 4: "Execution failed for task ':app:mergeReleaseResources'"

**Solución:**
1. Limpia el proyecto: `Build` → `Clean Project`
2. Reconstruye: `Build` → `Rebuild Project`
3. Intenta exportar de nuevo

### ❌ Error 5: "SDK location not found"

**Solución:**
1. Verifica que `local.properties` existe (está en `.gitignore`, no se sube a Git)
2. Debe contener:
```
sdk.dir=C:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
```

### ❌ Error 6: "Failed to read key" (al usar keystore existente)

**Solución:**
- Verifica que la contraseña del keystore sea correcta
- Verifica que el alias del key sea correcto
- Si usas un keystore de otro proyecto, asegúrate de usar el mismo alias

### ❌ Error 7: Error de compilación de Kotlin

**Solución:**
1. Sincroniza Gradle: `File` → `Sync Project with Gradle Files`
2. Verifica que todas las dependencias estén descargadas
3. Revisa los errores en el panel "Build"

### ❌ Error 8: "V1/V2 Signature" warnings

**Solución:**
Si solo ves warnings (no errores), puedes ignorarlos. Si quieres eliminarlos, en `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            // tu configuración
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## 📋 Pasos de Diagnóstico

1. **Limpia el proyecto:**
   - `Build` → `Clean Project`
   - `Build` → `Rebuild Project`

2. **Sincroniza Gradle:**
   - `File` → `Sync Project with Gradle Files`

3. **Verifica que compile:**
   - Intenta ejecutar la app primero (`Run`)
   - Si funciona, entonces el problema es solo de exportación

4. **Revisa el log de errores:**
   - Ve a `View` → `Tool Windows` → `Build`
   - Lee el error completo (no solo el resumen)

## 🚀 Método Alternativo: Generar APK Debug

Si el APK release falla, genera uno debug:

```bash
cd FiscamotoGPS
./gradlew assembleDebug
```

Esto genera: `app/build/outputs/apk/debug/app-debug.apk`

**Nota:** El APK debug se puede instalar y probar, pero no es para distribución.

## 📝 Comparte el Error

Si ninguno de estos soluciona tu problema, comparte:
1. El mensaje de error completo
2. Qué método usaste (Generate Signed Bundle/APK o Build APK)
3. Si es la primera vez que exportas o ya habías exportado antes




