package com.example.dsy1105_reservas.ui

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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.dsy1105_reservas.TicketViewModel
import com.example.dsy1105_reservas.Validaciones
import com.example.dsy1105_reservas.model.Prioridad

/** Levantamiento de requerimientos ordenados por prioridad. */
@Composable
fun RequerimientosScreen(vm: TicketViewModel, innerPadding: PaddingValues) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    val requerimientos by vm.requerimientos.collectAsState()
    val lista = requerimientos.sortedWith(compareBy({ it.completado }, { it.prioridad.peso }, { it.id }))

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
            if (lista.isEmpty()) {
                item { Text("Aún no hay requerimientos levantados. Usa el botón + para agregar uno.") }
            }
            items(lista, key = { it.id }) { req ->
                Card(Modifier.fillMaxWidth().animateItem().animateContentSize()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = req.completado, onCheckedChange = { vm.alternarRequerimiento(req.id) })
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                req.titulo,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (req.completado) TextDecoration.LineThrough else null
                            )
                            if (req.descripcion.isNotBlank()) Text(req.descripcion)
                            Text("Encargado: ${req.encargado}", style = MaterialTheme.typography.bodySmall)
                        }
                        ChipPrioridad(req.prioridad)
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
            Icon(Icons.Filled.Add, contentDescription = "Agregar requerimiento")
        }
    }

    if (mostrarFormulario) {
        FormularioRequerimiento(vm, onCerrar = { mostrarFormulario = false })
    }
}

@Composable
private fun FormularioRequerimiento(vm: TicketViewModel, onCerrar: () -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var encargado by remember { mutableStateOf("") }
    var prioridad by remember { mutableStateOf(Prioridad.MEDIA) }
    var intento by remember { mutableStateOf(false) }

    val errorTitulo = if (intento) Validaciones.obligatorio(titulo, "El título") else null
    val errorEncargado = if (intento) Validaciones.obligatorio(encargado, "El encargado") else null

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nuevo requerimiento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CampoTexto(titulo, { titulo = it }, "Título", error = errorTitulo)
                CampoTexto(descripcion, { descripcion = it }, "Descripción", singleLine = false)
                CampoTexto(
                    encargado, { encargado = it }, "Encargado",
                    error = errorEncargado,
                    ayuda = "Todo requerimiento debe tener un encargado."
                )
                Selector("Prioridad", prioridad, Prioridad.entries, { it.etiqueta }, { prioridad = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intento = true
                if (Validaciones.obligatorio(titulo, "El título") != null ||
                    Validaciones.obligatorio(encargado, "El encargado") != null
                ) return@TextButton
                vm.agregarRequerimiento(titulo, descripcion, prioridad, encargado)
                onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
