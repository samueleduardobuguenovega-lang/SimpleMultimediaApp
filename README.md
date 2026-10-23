# SimpleMultimediaApp — Actividad 2.4: Elementos multimedia

Aplicación Android en **Java** que integra los cuatro tipos de contenido multimedia vistos en la
unidad, más la gestión de permisos en tiempo de ejecución.

## Qué demuestra

| Requisito de la pauta | Dónde está implementado |
| --- | --- |
| Imagen desde `res/drawable` con `ImageView` | `activity_main.xml` (`@drawable/imagen_demo`) + `imageView` en `MainActivity` |
| Audio con `MediaPlayer` desde `res/raw` | `configurarAudio()` → `audio_demo` en `res/raw/audio_demo.mp3` |
| Video con `VideoView` y URI `android.resource://` | `configurarVideo()` → `res/raw/video_demo.mp4` |
| Animación XML desde `res/anim` | `res/anim/fade_in.xml` + `configurarAnimacion()` |
| Permisos en tiempo de ejecución | `permisosNecesarios()`, `requestPermissions`, `onRequestPermissionsResult` |
| Liberación de recursos | `liberarAudio()` en `onDestroy()` y `stopPlayback()` del `VideoView` |

## Requisitos

- Android Studio (Ladybug o superior) o JDK 17 + Android SDK 34.
- `compileSdk 34`, `minSdk 24`, `targetSdk 34`.

## Cómo compilar

```bash
# Con Android Studio: File > Open > seleccionar esta carpeta.
# Por consola (necesita JDK 17 y ANDROID_HOME apuntando al SDK):
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Estructura

```
app/src/main/
├── AndroidManifest.xml          # permisos INTERNET, READ_EXTERNAL_STORAGE (<33), READ_MEDIA_* (33+)
├── java/com/example/simplemultimediaapp/MainActivity.java
└── res/
    ├── anim/fade_in.xml         # animación de opacidad 0 -> 1 en 1 s
    ├── drawable/imagen_demo.png # imagen mostrada en el ImageView
    ├── layout/activity_main.xml
    ├── raw/audio_demo.mp3       # audio reproducido con MediaPlayer
    ├── raw/video_demo.mp4       # video reproducido con VideoView
    └── values/{strings,colors,themes}.xml
```
