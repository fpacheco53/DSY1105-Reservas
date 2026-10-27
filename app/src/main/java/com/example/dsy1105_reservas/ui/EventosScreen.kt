package com.mpaz.ticket.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mpaz.ticket.TicketViewModel
import com.mpaz.ticket.Validaciones
import com.mpaz.ticket.model.Prioridad
import java.time.LocalDate

/** Pantalla principal. */
@Composable
fun EventosScreen(vm: TicketViewModel, innerPadding: PaddingValues) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    val eventos by vm.eventos.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (eventos.isEmpty()) {
                item { Text("Aún no hay eventos registrados. Usa el botón + para agregar el primero.") }
            }
            items(eventos, key = { it.id }) { evento ->
                Card(Modifier.fillMaxWidth().animateItem().animateContentSize()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(evento.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            ChipPrioridad(evento.prioridad)
                        }
                        Text(evento.fecha.texto(), style = MaterialTheme.typography.labelLarge)
                        if (evento.descripcion.isNotBlank()) Text(evento.descripcion)
                        Text("Responsable: ${evento.responsable}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { mostrarFormulario = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Registrar evento")
        }
    }

    if (mostrarFormulario) {
        FormularioEvento(vm, onCerrar = { mostrarFormulario = false })
    }
}

@Composable
private fun FormularioEvento(vm: TicketViewModel, onCerrar: () -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var responsable by remember { mutableStateOf(vm.usuarioActual?.nombre.orEmpty()) }
    var prioridad by remember { mutableStateOf(Prioridad.MEDIA) }
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var intento by remember { mutableStateOf(false) }
    val fechas = remember { List(30) { LocalDate.now().plusDays(it.toLong()) } }

    val errorTitulo = if (intento) Validaciones.obligatorio(titulo, "El título") else null
    val errorResponsable = if (intento) Validaciones.obligatorio(responsable, "El responsable") else null

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Registrar evento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CampoTexto(titulo, { titulo = it }, "Título", error = errorTitulo)
                CampoTexto(descripcion, { descripcion = it }, "Descripción", singleLine = false)
                CampoTexto(responsable, { responsable = it }, "Responsable", error = errorResponsable)
                Selector("Fecha", fecha, fechas, { it.texto() }, { fecha = it }, Modifier.fillMaxWidth())
                Selector("Prioridad", prioridad, Prioridad.entries, { it.etiqueta }, { prioridad = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intento = true
                if (Validaciones.obligatorio(titulo, "El título") != null ||
                    Validaciones.obligatorio(responsable, "El responsable") != null
                ) return@TextButton
                vm.registrarEvento(titulo, descripcion, fecha, prioridad, responsable)
                onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
