package com.example.dsy1105_reservas.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dsy1105_reservas.TicketViewModel
import com.example.dsy1105_reservas.data.AlmacenArchivos
import com.example.dsy1105_reservas.model.Archivo
import com.example.dsy1105_reservas.model.Rol
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/**
 * Gestión de archivos. Solo se suben fotos del teléfono. Se pueden ver y eliminar.
 */
@Composable
fun ArchivosScreen(vm: TicketViewModel, innerPadding: PaddingValues, onVer: (Long) -> Unit) {
    val archivos by vm.archivos.collectAsState()
    val esAdministrador = vm.usuarioActual?.rol == Rol.ADMINISTRADOR
    var mensaje by remember { mutableStateOf<String?>(null) }
    var mensajeEsError by remember { mutableStateOf(false) }
    var porEliminar by remember { mutableStateOf<Archivo?>(null) }

    val selectorDeFotos = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            vm.cargarFoto(uri) { error ->
                mensajeEsError = error != null
                mensaje = error ?: "Foto subida correctamente."
            }
        }
    }

    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            delay(4000)
            mensaje = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Sube fotos de tu teléfono (JPG, PNG, WebP o HEIC). Máximo 15 MB por foto.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    AnimatedVisibility(visible = mensaje != null) {
                        Text(
                            mensaje.orEmpty(),
                            color = if (mensajeEsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (archivos.isEmpty()) {
                item { Text("Aún no hay fotos subidas.") }
            }
            items(archivos, key = { it.id }) { archivo ->
                TarjetaArchivo(
                    archivo = archivo,
                    mostrarPropietario = esAdministrador,
                    onVer = { onVer(archivo.id) },
                    onEliminar = { porEliminar = archivo },
                    modifier = Modifier.animateItem()
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                selectorDeFotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            icon = { Icon(Icons.Filled.UploadFile, contentDescription = null) },
            text = { Text("Subir foto") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = innerPadding.calculateBottomPadding())
        )
    }

    porEliminar?.let { archivo ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar foto") },
            text = { Text("¿Eliminar \"${archivo.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.eliminarArchivo(archivo)
                    porEliminar = null
                    mensajeEsError = false
                    mensaje = "Foto eliminada."
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaArchivo(
    archivo: Archivo,
    mostrarPropietario: Boolean,
    onVer: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fecha = Instant.ofEpochMilli(archivo.subidoEn).atZone(ZoneId.systemDefault()).toLocalDate()

    Card(modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Coil decodifica la foto reducida y respeta su orientación (EXIF).
                AsyncImage(
                    model = AlmacenArchivos.archivo(context, archivo),
                    contentDescription = "Miniatura de ${archivo.nombre}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        archivo.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Foto · ${archivo.tamanoBytes.tamanoLegible()} · ${fecha.texto()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (mostrarPropietario) {
                        Text("Subida por ${archivo.propietario}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onVer) { Text("Ver") }
                OutlinedButton(onClick = onEliminar) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}
