package com.mpaz.ticket.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mpaz.ticket.TicketViewModel
import com.mpaz.ticket.model.EstadoSolicitud
import com.mpaz.ticket.model.SolicitudEspacio

/** Vista del administrador. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AprobacionesScreen(vm: TicketViewModel, innerPadding: PaddingValues) {
    var error by remember { mutableStateOf<String?>(null) }
    val solicitudes by vm.solicitudes.collectAsState()
    val pendientes = solicitudes.filter { it.estado == EstadoSolicitud.PENDIENTE }
    val resueltas = solicitudes.filter { it.estado != EstadoSolicitud.PENDIENTE }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        item { Text("Pendientes (${pendientes.size})", style = MaterialTheme.typography.titleMedium) }
        if (pendientes.isEmpty()) item { Text("No hay solicitudes pendientes.") }
        items(pendientes, key = { it.id }) { s ->
            TarjetaSolicitud(s, Modifier.animateItem()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { error = vm.resolverSolicitud(s.id, true) }) { Text("Aprobar") }
                    OutlinedButton(onClick = { error = vm.resolverSolicitud(s.id, false) }) { Text("Rechazar") }
                }
            }
        }

        if (resueltas.isNotEmpty()) {
            item { Text("Resueltas", style = MaterialTheme.typography.titleMedium) }
            items(resueltas, key = { it.id }) { s -> TarjetaSolicitud(s, Modifier.animateItem()) }
        }
    }
}

@Composable
fun TarjetaSolicitud(
    s: SolicitudEspacio,
    modifier: Modifier = Modifier,
    acciones: @Composable (() -> Unit)? = null
) {
    Card(modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(s.espacio.etiqueta, style = MaterialTheme.typography.titleMedium)
            Text("${s.fecha.texto()}  ${s.inicio.texto()} - ${s.fin.texto()}")
            Text("Solicitante: ${s.solicitante} (${s.ciclo.etiqueta})", style = MaterialTheme.typography.bodySmall)
            Text("Motivo: ${s.motivo}")
            Text(
                (if (s.flexible) "Horario flexible" else "Bloque de clases") + " · " + s.estado.etiqueta,
                style = MaterialTheme.typography.labelMedium
            )
            acciones?.invoke()
        }
    }
}
