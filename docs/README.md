# PixDate

PixDate es un aplicación que permite gestionar las fotos de una forma dinámica mediante un calendario siguiendo 
un orden temporal, acompañada de múltiples funcionalidades como  gestión y clasificación o incluso, descripciones automatizadas.

## Estructura
- `app/`: código fuente de la aplicación
- `docs/`: documentación del proyecto

## Funcionalidades principales
- Visualización de fotos mediante calendario:
  La aplicación permitirá visualizar las fotografías organizadas dentro de un calendario interactivo. 
  En cada día se mostrará las imágenes asociadas a esa fecha, facilitando la navegación entre recuerdos de forma más visual e intuitiva.

- Generación automática de descripciones: 
  La aplicación mandará peticiones a modelos de inteligencia artificial disponibles en plataformas como Hugging Face para 
  generar automáticamente descripciones de las imágenes. Estas descripciones podrán reflejar el contexto de la fotografía o 
  el tipo de momento capturado.

- Gestión y organización de imágenes: 
  Mediante los mismos modelos, se generan categorías automáticas que permite que los usuarios puedan organizar sus 
  fotografías mediante en carpetas sin tanta intervención manual que ralentiza las clasificación.

- Compartir recuerdos a otras plataformas: 
  La aplicación permitirá compartir fácilmente fotografías o colecciones de un día específico en otras plataformas o 
  redes sociales, con las mismas descripciones dadas del modelo, evitando que el usuario tenga que pensar en poner un ç
  texto adecuado a la hora de compartir.

## Tecnologías utilizadas
- Entorno de desarrollo: Android Studio
- Lenguajes de programación: Kotlin
- Base de datos: (almacenamiento local procesando los metadatos y adjuntandole las descripciones)  Room
- Conexión con api externas: Retrofit
- Visualización y gestión de imágenes: MediaStore y Coil
- API de procesamiento: Api de hugging face  
- Control de versiones: Git y GitHub



