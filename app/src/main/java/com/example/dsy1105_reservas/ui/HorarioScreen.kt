package com.example.dsy1105_reservas.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.mpaz.ticket.model.Bloque
import com.mpaz.ticket.model.Ciclo
import com.mpaz.ticket.model.EstadoSolicitud
import com.mpaz.ticket.model.HorarioEscolar
import com.mpaz.ticket.model.SolicitudEspacio
import java.time.DayOfWeek
import java.time.LocalDate

private val diasEscolares = listOf(
    DayOfWeek.MONDAY to "Lun",
    DayOfWeek.TUESDAY to "Mar",
    DayOfWeek.WEDNESDAY to "Mié",
    DayOfWeek.THURSDAY to "Jue",
    DayOfWeek.FRIDAY to "Vie"
)

/** Minimapa de horarios. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HorarioScreen(vm: TicketViewModel, innerPadding: PaddingValues) {
    var dia by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var ciclo by remember { mutableStateOf(Ciclo.PRIMERO) }
    val solicitudes by vm.solicitudes.collectAsState()

    // Próxima fecha que cae en el día seleccionado (hoy incluido).
    val fecha = remember(dia) {
        var f = LocalDate.now()
        while (f.dayOfWeek != dia) f = f.plusDays(1)
        f
    }
    val bloques = HorarioEscolar.bloques(dia)
    val ultimoBloqueAntesDelAlmuerzo = bloques.lastOrNull { !it.fin.isAfter(HorarioEscolar.ALMUERZO_INICIO) }
    val aprobadas = solicitudes.filter {
        it.estado == EstadoSolicitud.APROBADA && it.fecha == fecha && it.ciclo == ciclo
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                diasEscolares.forEach { (d, etiqueta) ->
                    FilterChip(selected = dia == d, onClick = { dia = d }, label = { Text(etiqueta) })
                }
            }
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Ciclo.entries.forEach { c ->
                    FilterChip(selected = ciclo == c, onClick = { ciclo = c }, label = { Text(c.etiqueta) })
                }
            }
        }
        item {
            Text(
                "${fecha.texto()} · Jornada ${HorarioEscolar.INICIO.texto()} a ${HorarioEscolar.cierre(dia)?.texto()}",
                style = MaterialTheme.typography.labelLarge
            )
        }

        items(bloques, key = { "${dia.value}-${it.numero}" }) { b ->
            val ocupados = aprobadas.filter { it.inicio.isBefore(b.fin) && b.inicio.isBefore(it.fin) }
            TarjetaBloque(b, ocupados)
            if (b.numero == ultimoBloqueAntesDelAlmuerzo?.numero) {
                Text(
                    "Almuerzo ${HorarioEscolar.ALMUERZO_INICIO.texto()} - ${HorarioEscolar.ALMUERZO_FIN.texto()}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaBloque(bloque: Bloque, ocupados: List<SolicitudEspacio>) {
    val colorBase = if (ocupados.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.primaryContainer
    val color by animateColorAsState(colorBase, label = "colorBloque")

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Bloque ${bloque.numero}: ${bloque.inicio.texto()} - ${bloque.fin.texto()}",
                style = MaterialTheme.typography.titleSmall
            )
            if (ocupados.isEmpty()) {
                Text("Sin espacios reservados", style = MaterialTheme.typography.bodySmall)
            } else {
                ocupados.forEach {
                    Text("${it.espacio.etiqueta} · ${it.solicitante}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
