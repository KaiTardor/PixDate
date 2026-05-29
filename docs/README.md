# PixDate

PixDate es una aplicación Android que permite gestionar las fotos del dispositivo de forma dinámica mediante un calendario cronológico, acompañada de funcionalidades de inteligencia artificial para análisis automático, clasificación y compartición de recuerdos.

## Estructura del proyecto

```
PixDate/
├── app/                        # Código fuente de la aplicación
│   └── src/main/java/.../
│       ├── data/
│       │   ├── local/          # Room DB: entidades, DAOs, base de datos
│       │   ├── remote/         # Servicio de IA (Google Gemini)
│       │   └── repository/     # Repositorios de datos
│       ├── ui/
│       │   ├── screens/
│       │   │   ├── gallery/    # Galería principal + ViewModel
│       │   │   ├── detail/     # Detalle de foto
│       │   │   ├── folders/    # Carpetas y detalle de carpeta
│       │   │   └── legal/      # Pantalla de información legal
│       │   └── theme/          # Tema visual de la app
│       ├── notifications/      # Notificaciones locales de análisis IA
│       └── MainActivity.kt
└── docs/                       # Documentación del proyecto
```

## Funcionalidades principales

- **Visualización de fotos mediante calendario:**
  La aplicación organiza las fotografías del dispositivo dentro de un calendario interactivo. Cada día muestra las imágenes asociadas a esa fecha, con indicadores visuales y navegación intuitiva entre recuerdos.

- **Análisis automático con IA (Google Gemini):**
  Las fotos se envían a la API de Google Gemini (`gemini-2.5-flash`) para generar automáticamente una descripción en inglés, una categoría principal y hasta 5 etiquetas descriptivas. El análisis se ejecuta en segundo plano y notifica al usuario al finalizar.

- **Gestión y organización en carpetas:**
  Las carpetas se crean automáticamente a partir de las categorías asignadas por la IA (p. ej. `TRAVEL`, `ANIMALS`, `NIGHT`), permitiendo organizar el contenido sin intervención manual. El usuario también puede crear, renombrar y eliminar carpetas.

- **Cámara integrada:**
  La aplicación permite capturar nuevas fotografías directamente desde la galería, delegando en la cámara nativa del sistema y registrando la nueva foto en la base de datos local.

- **Compartir recuerdos:**
  Las fotos y su descripción generada por IA se pueden compartir fácilmente con otras aplicaciones o redes sociales, evitando que el usuario tenga que escribir un texto manualmente.

- **Notificaciones locales:**
  Cuando el análisis de una foto finaliza (con éxito o con error) mientras el usuario está fuera de la app, se muestra una notificación del sistema que abre directamente el detalle de esa foto.

## Tecnologías utilizadas

| Categoría | Tecnología |
|---|---|
| Entorno de desarrollo | Android Studio |
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Base de datos local | Room (con KSP) |
| Visualización de imágenes | MediaStore + Coil |
| API de inteligencia artificial | Google Gemini API (`gemini-2.5-flash`) |
| Comunicación HTTP | OkHttp 4 + Gson |
| Notificaciones | NotificationCompat (AndroidX) |
| Control de versiones | Git + GitHub |

## Configuración de la API Key

El token de Gemini se gestiona de forma segura a través de `local.properties` y nunca se sube al repositorio. Para configurarlo:

1. Obtén tu API Key en [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Añade la siguiente línea a tu archivo `local.properties` (en la raíz del proyecto):
   ```
   GEMINI_API_KEY=tu_clave_aqui
   ```
3. La clave se inyecta en tiempo de compilación como `BuildConfig.GEMINI_API_KEY`.

## Requisitos

- Android mínimo: API 24 (Android 7.0)
- Android objetivo: API 34 (Android 14)
- Compilado con SDK 35

## Documentación adicional

- [`docs/estructura_datos.md`](estructura_datos.md) — Esquema de la base de datos local (tablas, campos y relaciones).
- [`docs/navegacion.md`](navegacion.md) — Arquitectura de navegación y flujos de usuario entre pantallas.
