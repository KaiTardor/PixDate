package com.example.pixdate.ui.screens.legal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LegalScreen(innerPadding: PaddingValues, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        // Barra superior con botón X
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "INFO & LEGAL",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "POLÍTICA DE PRIVACIDAD",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

        Spacer(modifier = Modifier.height(16.dp))

        LegalSection(
            title = "1. RECOPILACIÓN DE DATOS",
            body = "PixDate accede a las imágenes almacenadas en tu dispositivo con el único " +
                    "propósito de organizarlas y clasificarlas mediante análisis local. " +
                    "No se transmite ninguna imagen ni dato personal a servicios externos ajenos que no se hayan comentado. "
        )

        LegalSection(
            title = "2. PERMISOS",
            body = "La aplicación solicita los siguientes permisos para funcionar correctamente:\n" +
                    "• Cámara: para hacer fotos e incluir nuevas imagenes a la app.\n" +
                    "• Notificación: para notificar del estado de las fotos.\n" +
                    "• Almacenamiento: para leer las imágenes existentes de la galería.\n\n" +
                    "Estos permisos son esenciales para el correcto funcionamiento de la app."
        )

        LegalSection(
            title = "3. ALMACENAMIENTO LOCAL",
            body = "PixDate utiliza una base de datos local (Room) para almacenar metadatos " +
                    "de las fotos (nombre, fecha, categoría, etiquetas, etc). Las imágenes originales " +
                    "permanecen en su ubicación original del sistema de archivos y no son " +
                    "duplicadas ni modificadas de ninguna forma."
        )

        LegalSection(
            title = "4. PROCESAMIENTO CON IA",
            body = "El análisis de imágenes se realiza mediante modelos de inteligencia artificial " +
                    "que se ejecutan mediante una API externa. Por lo que ten precaución sobre las " +
                    "imágenes a compartir. No nos hacemos responsables de los usos que le dé el usuario" +
                    "Los resultados del análisis (descripción, " +
                    "categoría, etiquetas, etc) se almacenan exclusivamente en la base de datos local."
        )

        LegalSection(
            title = "5. COMPARTICIÓN DE DATOS",
            body = "PixDate NO comparte, vende ni transfiere datos personales a terceros bajo " +
                    "ninguna circunstancia. La aplicación funciona completamente offline una " +
                    "vez instalada."
        )

        LegalSection(
            title = "6. DERECHOS DEL USUARIO",
            body = "Puedes en cualquier momento:\n" +
                    "• Eliminar todos los datos generados por la app borrando su caché.\n" +
                    "• Revocar los permisos concedidos desde Ajustes > Aplicaciones.\n" +
                    "• Desinstalar la aplicación, lo que eliminará toda la base de datos local."
        )

        LegalSection(
            title = "7. TÉRMINOS DE USO",
            body = "Al utilizar PixDate, aceptas que:\n" +
                    "• La aplicación se proporciona \"tal cual\" sin garantías de ningún tipo.\n" +
                    "• El desarrollador no se responsabiliza de la pérdida de datos.\n" +
                    "• El uso de la cámara es responsabilidad exclusiva del usuario.\n" +
                    "• La clasificación automática es orientativa y puede contener errores."
        )

        LegalSection(
            title = "8. CONTACTO",
            body = "Para consultas sobre privacidad o términos de uso, puedes contactar " +
                    " con el equipo legal de PixDate @Adriu o al correo adri@notfake.com" +
                    "Se responderá con la mayor brevedad posible."
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Última actualización: Abril 2026",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LegalSection(title: String, body: String) {
    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground,
        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
    )
}
