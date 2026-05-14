package com.example.pixdate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.pixdate.notifications.NotificationHelper
import com.example.pixdate.ui.PixDateApp

class MainActivity : ComponentActivity() {
    private var photoIdToOpen by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            com.example.pixdate.ui.theme.PixDateTheme {
                PixDateApp(
                    initialPhotoId = photoIdToOpen,
                    onPhotoOpened = { photoIdToOpen = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val id = intent.getLongExtra(NotificationHelper.EXTRA_PHOTO_ID, -1L)
        if (id != -1L) {
            photoIdToOpen = id
        }
    }
}