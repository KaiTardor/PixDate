package com.example.pixdate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Esquinas rectas para la estética pixelada
val PixelShapes = Shapes(
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPastel,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryPastel,
    onSecondary = OnSecondaryDark,
    background = BackgroundWarm,
    onBackground = OnBackgroundDark,
    surface = SurfaceWarm,
    onSurface = OnSurfaceDark
)

// Usamos la misma paleta cálida y clara como estandar, pero si
// tuviéramos un modo oscuro auténtico, lo definiríamos aquí.
// Por homogeneidad de diseño, forzaremos estos colores de momento.
private val DarkColorScheme = LightColorScheme

@Composable
fun PixDateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Hemos desactivado los colores dinámicos (Android 12+) para forzar 
    // siempre nuestra paleta naranja pixelada especial.
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PixelShapes,
        content = content
    )
}