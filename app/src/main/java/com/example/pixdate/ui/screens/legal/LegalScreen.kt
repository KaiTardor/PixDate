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
            body = "PixDate accede a los archivos multimedia almacenadas en el dispositivo del usuario con el único " +
                    "propósito de organizarlas y clasificarlas. " +
                    "Salvo la tranmisión técnica estrictamente necesaria de la cláusula 4, la aplicación no compila, " +
                    "extrae, almacena ni transmite datos de carácter personal o informacion confidencial a servidores de terceros " +
                    "que no se hayan comentado previamente."
        )

        LegalSection(
            title = "2. CONCESIÓN DE PERMISOS",
            body = "La aplicación solicita los siguientes permisos de forma explicita para funcionar correctamente:\n" +
                    "• Cámara: Para la captura directa e integración de material fotográfico en la plataforma.\n" +
                    "• Notificación: Para la notificación sobre los estados de las imágenes.\n" +
                    "• Almacenamiento:  Para la lectura, indexación y gestión de los archivos.\n\n"
        )

        LegalSection(
            title = "3. ALMACENAMIENTO LOCAL",
            body = "PixDate utiliza una base de datos local (Room) de manera exclusiva en la memoria del dispositivo del usuario con "+
                    "el fin de para almacenar metadatos de las fotos (nombre, fecha, categoría, etiquetas, etc). "+
                    "Los archivos multimedia originales permanecen inalterados en el sistema de archivos del sistema operativo, "+
                    "prohibiéndose expresamente a la aplicación la modificación, supresión o duplicación no autorizada de los mismos."
        )

        LegalSection(
            title = "4. PROCESAMIENTO EXTERNO MEDIANTE IA",
            body = "El análisis de imágenes se realiza mediante modelos de inteligencia artificial " +
                    "que se ejecutan mediante una API (interfaz de programación de aplicaciones) externa. Por lo que ten precaución sobre las " +
                    "imágenes a compartir. Ya que el usuario asume la responsabilidad total e indelegable sobre la legalidad y naturaleza " +
                    "del contenido sometido a dicho análisis. Los datos resultantes de la inferencia se integrarán de forma exclusiva en el almacenamiento " +
                    "local del dispositivo del usuario."
        )

        LegalSection(
            title = "5. CONFIDENCIALIDAD Y CESIÓN A TERCEROS",
            body = "Sin perjuicio de la transmisión técnica estipulada en la Cláusula 4, PixDate se compromete expresamente a no ceder,"+
                    "comercializar, alquilar ni transferir a título gratuito u oneroso la información personal o los archivos del usuario a " +
                    "terceras partes bajo ninguna circunstancia. El software subyacente opera primariamente en un entorno local y cerrado para el usuario."
        )

        LegalSection(
            title = "6. DERECHOS DEL USUARIO",
            body = " El Usuario goza de plena soberanía sobre su información, reservándose en tood momento el derecho a:\n" +
                    "• Eliminar todos los datos generados por la app borrando su caché.\n" +
                    "• Revocar el consentimiento de acceso mediante la deshabilitación de permisos del sistema\n" +
                    "• Proceder a la desinstalación del software, acción que conllevará la destrucción irreversible de la base de datos local ."
        )

        LegalSection(
            title = "7. TÉRMINOS DE USO",
            body = "La utilización de PixDate implica la aceptación expresa e inequívoca de las siguientes condiciones limitativas:\n" +
                    "• El software y sus servicios asociados se licencian bajo la premisa de \"tal cual\" (\"as is\"), declinando cualquier garantía " +
                    "implícita o explícita de idoneidad, continuidad o infalibilidad técnica.\n" +
                    "• El desarrollador se exime de toda responsabilidad civil, penal o administrativa derivada de la pérdida incidental, alteración o corrupción de datos.\n" +
                    "• La generación, selección y subida de contenido es de la exclusiva responsabilidad del Usuario.\n" +
                    "• Los resultados de categorización generados por la IA son de naturaleza estocástica, tienen carácter puramente orientativo " +
                    "y carecen de validez vinculante, pudiendo contener errores u omisiones en el proceso."
        )

        LegalSection(
            title = "8. CONTACTO",
            body = "Para el ejercicio de sus derechos, la presentación de reclamaciones o cualquier consulta referente a las presentes condiciones de uso " +
                    "y política de privacidad, el usuario podrá dirigirse al equipo responsable de PixDate mediante @Adriu o a través de la dirección de" +
                    "correo electrónico adri_notfake@pixdate.com. Toda comunicación será procesada y respondida con la debida diligencia legal."
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
