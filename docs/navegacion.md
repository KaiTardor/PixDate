# Estructura de Navegación - PixDate

Este documento detalla la arquitectura de navegación de PixDate, especificando el comportamiento de cada pantalla y el flujo de usuario.

## Información General de Navegación

- **Pantalla Inicial**: La aplicación comienza en la **Galería Principal** (`GALLERY`).
- **Flujo Principal**: El recorrido típico del usuario es: 
    1. Hacer una foto usando la cámara.
    2. Visualización y confirmación de la imagen tomada.
    3. Confirmación para mandarla a procesar.
    4. Organización temática en la sección de Carpetas.

## Detalle de Pantallas

### 1. Galería Principal
- **Representación**: Pantalla central que organiza todas las fotografías cronológicamente.
- **Información**: Calendario interactivo con indicadores de fotos, lista de imágenes con miniaturas y estado de procesamiento.
- **Acciones**: Cambiar entre vista de lista y calendario, filtrar fotos (Todas/Procesadas/Sin procesar), seleccionar una foto.
- **Identificador**: `BottomSection.GALLERY`
- **Argumentos**: Ninguno.
- **Relaciones**: 
    - **Desde**: Prácticamente desde todas las pantallas.
    - **Hacia**: Detalle de Foto, Lista de Carpetas, Apartado legal, Cámara.

### 2. Detalle de Foto
- **Representación**: Vista individual y expandida de una fotografía para su gestión detallada correspondiente.
- **Información**: Imagen a pantalla completa, descripción (IA o manual), etiquetas, categoría y metadatos técnicos.
- **Acciones**: Analizar con IA, editar información, compartir imagen, copiar descripción al portapapeles.
- **Identificador**: `selectedPhoto != null`
- **Argumentos**: 
    - `photo`: `PhotoEntity` (Obligatorio). Identificador único de la fotografía.
- **Relaciones**: 
    - **Desde**: Galería Principal, Detalle de Carpeta.
    - **Hacia**: Galería Principal, Detalle de Carpeta.

### 3. Lista de Carpetas
- **Representación**: Colección de álbumes o carpetas personalizadas creadas por el usuario o de forma automatica.
- **Información**: Grid de carpetas con nombre, imagen de portada (última foto añadida) y cantidad de elementos.
- **Acciones**: Crear nueva carpeta (vía diálogo), seleccionar una carpeta para ver contenido.
- **Identificador**: `BottomSection.FOLDERS`
- **Argumentos**: Ninguno.
- **Relaciones**: 
    - **Desde**: Practicamente desde todas las pantallas. 
    - **Hacia**: Detalle de Carpeta, Galería Principal, Cámara.

### 4. Detalle de Carpeta
- **Representación**: Visualización de las fotografías contenidas dentro de una carpeta específica.
- **Información**: Nombre de la carpeta, grid de miniaturas de las fotos asociadas.
- **Acciones**: Abrir detalle de una foto, renombrar carpeta, eliminar carpeta.
- **Identificador**: `selectedFolder != null`
- **Argumentos**: 
    - `folder`: `FolderEntity` (Obligatorio). Identificador único de la carpeta.
    - `photos`: `List<PhotoEntity>` (Obligatorio). Lista de fotografías asociadas a la carpeta.
- **Relaciones**: 
    - **Desde**: Lista de Carpetas, Detalle de Foto.
    - **Hacia**: Lista de Carpetas, Detalle de Foto.

### 5. Cámara
- **Representación**: Interfaz para capturar nuevas fotografías directamente desde la aplicación. En la práctica, delega en la cámara nativa del sistema.
- **Información**: Vista previa de la cámara (gestionada por el sistema) o un indicador de "Abriendo cámara...".
- **Acciones**: Tomar foto, aceptar foto, cancelar captura.
- **Identificador**: `BottomSection.CAMERA`
- **Argumentos**: Ninguno.
- **Relaciones**: 
    - **Desde**: Galería Principal, Lista de Carpetas.
    - **Hacia**: Galería Principal.

### 6. Pantalla Legal
- **Representación**: Información sobre términos de uso, privacidad y créditos del proyecto.
- **Información**: Texto legal y descriptivo.
- **Acciones**: Lectura y cierre de la pantalla.
- **Identificador**: `BottomSection.LEGAL`
- **Argumentos**: Ninguno.
- **Relaciones**: 
    - **Desde**: Galería Principal.
    - **Hacia**: Galería Principal (Cerrar).


## Diagrama de Navegación

![Diagrama de navegación](/imgs/pixdate_nav.drawio.png)



## Casos de Uso del Flujo

1. **Captura y Análisis Inteligente**: Cámara -> Galería -> Detalle de Foto -> Procesamiento IA -> Galería.
2. **Organización en Álbumes**: Carpetas -> Diálogo Creación -> Detalle de Carpeta -> Galería -> Detalle de Foto.
3. **Compartir Recuerdo**: Galería -> Detalle de Foto -> Compartir / Postear.
