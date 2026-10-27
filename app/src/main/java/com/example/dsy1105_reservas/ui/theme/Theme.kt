package com.mpaz.ticket.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Azul80,
    secondary = AzulGris80,
    tertiary = Ambar80
)

private val LightColorScheme = lightColorScheme(
    primary = Azul40,
    secondary = AzulGris40,
    tertiary = Ambar40
)

@Composable
fun MpazTicketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Se desactiva el color dinámico para mantener una identidad visual consistente.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
