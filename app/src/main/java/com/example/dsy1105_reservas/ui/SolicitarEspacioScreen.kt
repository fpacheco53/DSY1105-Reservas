package com.mpaz.ticket.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mpaz.ticket.TicketViewModel
import com.mpaz.ticket.Validaciones
import com.mpaz.ticket.model.Bloque
import com.mpaz.ticket.model.Ciclo
import com.mpaz.ticket.model.Espacio
import com.mpaz.ticket.model.HorarioEscolar
import java.time.LocalDate
import java.time.LocalTime

/**
 * Formulario de solicitud de espacios.
 * - En horario de clases: bloque fijo de 45 minutos.
 * - Fuera de clases: horario flexible.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SolicitarEspacioScreen(vm: TicketViewModel, innerPadding: PaddingValues) {
    val fechas = remember { HorarioEscolar.proximosDiasHabiles(LocalDate.now(), 10) }

    var espacio by remember { mutableStateOf(Espacio.CANCHA) }
    var ciclo by remember { mutableStateOf(Ciclo.PRIMERO) }
    var fecha by remember { mutableStateOf(fechas.first()) }
    var flexible by remember { mutableStateOf(false) }
    var bloque by remember { mutableStateOf<Bloque?>(null) }
    var inicioLibre by remember { mutableStateOf<LocalTime?>(null) }
    var duracion by remember { mutableStateOf(30) }
    var solicitante by remember { mutableStateOf(vm.usuarioActual?.nombre.orEmpty()) }
    var motivo by remember { mutableStateOf("") }
    var intento by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf(false) }

    val errorSolicitante = if (intento) Validaciones.obligatorio(solicitante, "El solicitante") else null
    val errorMotivo = if (intento) Validaciones.obligatorio(motivo, "El motivo") else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Solicitud de espacio", style = MaterialTheme.typography.titleLarge)

        CampoTexto(solicitante, { solicitante = it }, "Solicitante", error = errorSolicitante)
        Selector("Espacio", espacio, Espacio.entries, { it.etiqueta }, { espacio = it }, Modifier.fillMaxWidth())
        Selector("Ciclo", ciclo, Ciclo.entries, { it.etiqueta }, { ciclo = it }, Modifier.fillMaxWidth())
        Selector("Fecha", fecha, fechas, { it.texto() }, {
            fecha = it
            bloque = null
            inicioLibre = null
        }, Modifier.fillMaxWidth())

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = !flexible,
                onClick = { flexible = false },
                label = { Text("En clases (45 min)") }
            )
            FilterChip(
                selected = flexible,
                onClick = { flexible = true },
                label = { Text("Fuera de clases (flexible)") }
            )
        }

        if (!flexible) {
            val bloques = HorarioEscolar.bloques(fecha.dayOfWeek)
            Selector(
                "Bloque", bloque, bloques,
                { "${it.numero}: ${it.inicio.texto()} - ${it.fin.texto()}" },
                { bloque = it },
                Modifier.fillMaxWidth()
            )
        } else {
            Selector(
                "Hora de inicio", inicioLibre, HorarioEscolar.iniciosFlexibles(fecha.dayOfWeek),
                { it.texto() }, { inicioLibre = it }, Modifier.fillMaxWidth()
            )
            Selector(
                "Duración", duracion, listOf(15, 30, 45, 60, 90),
                { "$it min" }, { duracion = it }, Modifier.fillMaxWidth()
            )
        }

        CampoTexto(motivo, { motivo = it }, "Motivo", error = errorMotivo, singleLine = false)

        AnimatedVisibility(visible = mensaje != null) {
            Text(
                mensaje.orEmpty(),
                color = if (exito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                intento = true
                val inicio = if (flexible) inicioLibre else bloque?.inicio
                val fin = if (flexible) inicioLibre?.plusMinutes(duracion.toLong()) else bloque?.fin
                val error = vm.enviarSolicitud(espacio, solicitante, ciclo, fecha, inicio, fin, motivo, flexible)
                exito = error == null
                mensaje = error ?: "Solicitud enviada. Queda pendiente de aprobación."
                if (exito) {
                    motivo = ""
                    bloque = null
                    inicioLibre = null
                    intento = false
                }
            }
        ) { Text("Enviar solicitud") }
    }
}
