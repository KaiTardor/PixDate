# Estructura de datos de PixDate

## Fuente de los datos de prueba

Para las pruebas iniciales del proyecto se utilizará un subconjunto de imágenes elegidas de forma aleatoria 
junto con sus captions de referencia, que se almacenarán de forma local dentro del proyecto en la carpeta `assets/`. 
Estas imágenes se emplearán únicamente como datos de prueba para validar la estructura de datos, la lectura de información 
y el posterior almacenamiento en la base de datos local.

Del conjunto original solo se conservarán los campos necesarios para la aplicación, como el nombre del archivo y 
una descripción textual de referencia. A partir de estos datos se añadirá información propia de la app que se 
considere necesaria para realizar la práctica, como el estado de procesamiento, las etiquetas, la categoría principal o 
la carpeta asociada.

## Tabla 1: photos

### Qué representa

Esta tabla representa cada fotografía detectada por la aplicación. No almacena la imagen en sí, sino su referencia dentro 
del dispositivo del usuario o del conjunto de prueba, junto con metadatos básicos necesarios para la organización en calendario.

### Campos

* `photoId`: Long. Obligatorio. Identificador único de la fotografía.
* `contentUri`: String. Obligatorio. Ruta o URI que referencia la imagen.
* `dateTaken`: Long. Obligatorio. Fecha asociada a la imagen.
* `displayName`: String. Obligatorio. Nombre del archivo de imagen.
* `mimeType`: String. Opcional. Tipo MIME de la imagen.
* `isProcessed`: Boolean. Obligatorio. Indica si la imagen ya ha sido procesada.
* `folderId`: Long. Opcional. Identificador de la carpeta principal asociada.
* `createdAt`: Long. Obligatorio. Fecha de creación del registro en la base de datos.
* `updatedAt`: Long. Obligatorio. Fecha de última actualización del registro.

### Ejemplo de registro

```json
{
  "photoId": 1,
  "contentUri": "file:///android_asset/sample_images/3354474353_daf9e168cf.jpg",
  "dateTaken": 1712165400000,
  "displayName": "3354474353_daf9e168cf.jpg",
  "mimeType": "image/jpeg",
  "isProcessed": true,
  "folderId": 1,
  "createdAt": 1712165500000,
  "updatedAt": 1712165600000
}
```

### Relaciones

* Relación 1:1 opcional con `photo_analysis`, mediante `photoId`.
* Relación N:M con `tags`, implementada mediante la tabla intermedia `photo_tag_cross_ref`.
* Relación N:1 con `folders`, mediante `folderId`.

## Tabla 2: photo_analysis

### Qué representa

Esta tabla almacena la información generada tras procesar una fotografía, como la descripción automática, 
la categoría principal o el modelo utilizado.

### Campos

* `analysisId`: Long. Obligatorio. Identificador único del análisis.
* `photoId`: Long. Obligatorio. Identificador de la fotografía analizada.
* `description`: String. Opcional. Descripción generada o asociada a la imagen.
* `mainCategory`: String. Opcional. Categoría principal asignada a la imagen.
* `modelUsed`: String. Opcional. Modelo utilizado para generar la descripción.
* `processedAt`: Long. Opcional. Fecha del procesamiento.
* `confidence`: Float. Opcional. Valor de confianza del resultado.
* `errorMessage`: String. Opcional. Mensaje de error si el procesamiento falla.

### Ejemplo de registro

```json
{
  "analysisId": 1,
  "photoId": 1,
  "description": "Two dogs walk in the snow, the larger dog has a fish in his mouth.",
  "mainCategory": "ANIMALS",
  "modelUsed": "Salesforce/blip-image-captioning-base",
  "processedAt": 1712165600000,
  "confidence": 0.91,
  "errorMessage": null
}
```

### Relaciones

* Relación 1:1 con `photos`, implementada mediante la clave foránea `photoId`.

## Tabla 3: tags

### Qué representa

Esta tabla almacena etiquetas reutilizables que permiten clasificar las imágenes según su contenido o contexto.

### Campos

* `tagId`: Long. Obligatorio. Identificador único de la etiqueta.
* `name`: String. Obligatorio. Nombre de la etiqueta.
* `source`: String. Obligatorio. Indica si la etiqueta es automática o manual.

### Ejemplo de registro

```json
{
  "tagId": 1,
  "name": "dog",
  "source": "AUTO"
}
```

### Relaciones

* Relación N:M con `photos`, implementada mediante `photo_tag_cross_ref`.

## Tabla 4: photo_tag_cross_ref

### Qué representa

Esta tabla intermedia implementa la relación muchos a muchos entre fotografías y etiquetas.

### Campos

* `photoId`: Long. Obligatorio. Identificador de la fotografía.
* `tagId`: Long. Obligatorio. Identificador de la etiqueta.

### Ejemplo de registro

```json
{
  "photoId": 1,
  "tagId": 1
}
```

### Relaciones

* Clave foránea hacia `photos(photoId)`.
* Clave foránea hacia `tags(tagId)`.

## Tabla 5: folders

### Qué representa

Esta tabla almacena las carpetas o agrupaciones principales de la aplicación. 
Permite organizar las imágenes según categorías generales o agrupaciones definidas automáticamente a partir del 
contenido procesado.

### Campos

* `folderId`: Long. Obligatorio. Identificador único de la carpeta.
* `name`: String. Obligatorio. Nombre de la carpeta.
* `description`: String. Opcional. Breve descripción de la carpeta.
* `isAutoGenerated`: Boolean. Obligatorio. Indica si la carpeta ha sido creada automáticamente por la app.
* `createdAt`: Long. Obligatorio. Fecha de creación de la carpeta.

### Ejemplo de registro

```json
{
  "folderId": 1,
  "name": "ANIMALS",
  "description": "Carpeta generada automáticamente para imágenes con animales.",
  "isAutoGenerated": true,
  "createdAt": 1712165500000
}
```

### Relaciones

* Relación 1:N con `photos`. Una carpeta puede contener varias fotografías, pero cada fotografía solo tendrá una carpeta principal asociada en esta primera versión.

## Relaciones y cardinalidades

* `photos` → `photo_analysis`: relación 1:1 opcional. Una fotografía puede no haber sido procesada todavía, pero si lo ha sido tendrá un único análisis principal asociado.
* `photos` ↔ `tags`: relación N:M. Una fotografía puede tener varias etiquetas y una etiqueta puede estar asociada a varias fotografías.
* `photos` → `folders`: relación N:1. Varias fotografías pueden pertenecer a una misma carpeta principal.

