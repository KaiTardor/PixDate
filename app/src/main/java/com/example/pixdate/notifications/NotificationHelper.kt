package com.example.pixdate.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pixdate.MainActivity
import com.example.pixdate.R

/**
 * Helper para crear y lanzar notificaciones locales cuando la IA termina de procesar
 * una foto mientras el usuario está fuera de la app.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "pixdate_ai_analysis"
    private const val CHANNEL_NAME = "Análisis de IA"
    private const val CHANNEL_DESCRIPTION = "Notificaciones cuando la IA termina de analizar una foto"

    /**
     * Crea el canal de notificaciones (necesario en Android 8+).
     * Es seguro llamar varias veces: si el canal ya existe, no hace nada.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Lanza una notificación del sistema indicando que el análisis de una foto ha terminado o ha fallado.
     *
     * Al pulsar la notificación, se abrirá la app directamente en la pantalla de detalle
     * de la foto indicada por [photoId].
     */
    fun showAnalysisCompleteNotification(
        context: Context,
        photoId: Long,
        photoName: String,
        isSuccess: Boolean = true,
        errorMessage: String? = null
    ) {
        // Intent que abre MainActivity con el ID de la foto pendiente de revisión
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PHOTO_ID, photoId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            photoId.toInt(), // requestCode único por foto
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSuccess) "¡Análisis Completado!" else "Hubo un problema"
        val text = if (isSuccess) {
            "PixDate ha extraído los mejores tags para tu foto. Toca para ver el resultado."
        } else {
            "No se pudo procesar tu foto. Inténtalo de nuevo más tarde."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                photoId.toInt(),
                notification
            )
        } catch (e: SecurityException) {
            // El usuario no ha concedido el permiso de notificaciones. No hacemos nada.
        }
    }

    const val EXTRA_PHOTO_ID = "extra_photo_id"
}
