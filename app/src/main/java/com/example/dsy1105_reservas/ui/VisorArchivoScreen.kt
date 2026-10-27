package com.example.dsy1105_reservas.ui

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dsy1105_reservas.TicketViewModel
import com.example.dsy1105_reservas.data.AlmacenArchivos

/** Visor de fotos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisorArchivoScreen(vm: TicketViewModel, archivoId: Long, onVolver: () -> Unit) {
    val context = LocalContext.current
    val archivos by vm.archivos.collectAsState()
    val archivo = archivos.firstOrNull { it.id == archivoId }
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(archivo?.nombre ?: "Archivo", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(if (archivo == null) 24.dp else 0.dp)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        escala = (escala * zoom).coerceIn(1f, 5f)
                        desplazamiento = if (escala == 1f) Offset.Zero else desplazamiento + pan
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (archivo == null) {
                Text("El archivo no está disponible.", textAlign = TextAlign.Center)
            } else {
                AsyncImage(
                    model = AlmacenArchivos.archivo(context, archivo),
                    contentDescription = archivo.nombre,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                            translationX = desplazamiento.x
                            translationY = desplazamiento.y
                        }
                )
            }
        }
    }
}
