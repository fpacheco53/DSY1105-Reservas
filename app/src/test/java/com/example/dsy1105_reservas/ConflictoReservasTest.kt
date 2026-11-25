package com.example.dsy1105_reservas

import com.example.dsy1105_reservas.model.Ciclo
import com.example.dsy1105_reservas.model.Espacio
import com.example.dsy1105_reservas.model.EstadoSolicitud
import com.example.dsy1105_reservas.model.SolicitudEspacio
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Pruebas de Detección de Conflicto de Horarios y Solicitudes")
class ConflictoReservasTest {

    private fun hayConflicto(
        solicitudes: List<SolicitudEspacio>,
        espacio: Espacio,
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        excluirId: Long = -1
    ): Boolean = solicitudes.any {
        it.id != excluirId &&
                it.estado == EstadoSolicitud.APROBADA &&
                it.espacio == espacio &&
                it.fecha == fecha &&
                it.inicio.isBefore(fin) && inicio.isBefore(it.fin)
    }

    private val fecha = LocalDate.of(2026, 11, 25)

    private val solicitudExistente = SolicitudEspacio(
        id = 1L,
        espacio = Espacio.CANCHA,
        solicitante = "Prof. Juan",
        ciclo = Ciclo.PRIMERO,
        fecha = fecha,
        inicio = LocalTime.of(10, 0),
        fin = LocalTime.of(11, 0),
        motivo = "Educación Física",
        flexible = false,
        estado = EstadoSolicitud.APROBADA
    )

    @Test
    @DisplayName("Detecta colisión cuando el horario es exactamente el mismo")
    fun detectaColisionHorarioExactamenteIgual() {
        val conflicto = hayConflicto(
            listOf(solicitudExistente),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(10, 0),
            fin = LocalTime.of(11, 0)
        )
        assertTrue(conflicto)
    }

    @Test
    @DisplayName("Detecta colisión cuando la nueva solicitud queda contenida dentro de la existente")
    fun detectaColisionNuevaSolicitudContenidaDentro() {
        val conflicto = hayConflicto(
            listOf(solicitudExistente),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(10, 15),
            fin = LocalTime.of(10, 45)
        )
        assertTrue(conflicto)
    }

    @Test
    @DisplayName("Detecta colisión cuando la nueva solicitud sobrepasa a la existente")
    fun detectaColisionNuevaSolicitudSobrepasaExistente() {
        val conflicto = hayConflicto(
            listOf(solicitudExistente),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(9, 30),
            fin = LocalTime.of(11, 30)
        )
        assertTrue(conflicto)
    }

    @Test
    @DisplayName("Detecta colisión con cruce parcial por el inicio")
    fun detectaColisionCruceParcialPorElInicio() {
        val conflicto = hayConflicto(
            listOf(solicitudExistente),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(9, 30),
            fin = LocalTime.of(10, 30)
        )
        assertTrue(conflicto)
    }

    @Test
    @DisplayName("Detecta colisión con cruce parcial por el final")
    fun detectaColisionCruceParcialPorElFinal() {
        val conflicto = hayConflicto(
            listOf(solicitudExistente),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(10, 30),
            fin = LocalTime.of(11, 30)
        )
        assertTrue(conflicto)
    }

    @Test
    @DisplayName("No hay conflicto si son bloques adyacentes anterior o posterior")
    fun noHayConflictoBloquesAdyacentes() {
        val anterior = hayConflicto(
            listOf(solicitudExistente),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(9, 0),
            fin = LocalTime.of(10, 0)
        )
        assertFalse(anterior, "Terminar cuando otra empieza no es conflicto")

        val posterior = hayConflicto(
            listOf(solicitudExistente),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(11, 0),
            fin = LocalTime.of(12, 0)
        )
        assertFalse(posterior, "Empezar cuando otra termina no es conflicto")
    }

    @Test
    @DisplayName("No hay conflicto si el espacio o la fecha son distintos")
    fun noHayConflictoDistintoEspacioOFecha() {
        val otroEspacio = hayConflicto(
            listOf(solicitudExistente),
            Espacio.BIBLIOTECA,
            fecha,
            inicio = LocalTime.of(10, 0),
            fin = LocalTime.of(11, 0)
        )
        assertFalse(otroEspacio, "Mismo horario pero distinto espacio no colisiona")

        val otraFecha = hayConflicto(
            listOf(solicitudExistente),
            Espacio.CANCHA,
            fecha.plusDays(1),
            inicio = LocalTime.of(10, 0),
            fin = LocalTime.of(11, 0)
        )
        assertFalse(otraFecha, "Mismo horario pero distinta fecha no colisiona")
    }

    @Test
    @DisplayName("No colisiona con solicitudes pendientes o rechazadas")
    fun noColisionaSolicitudesPendientesORechazadas() {
        val solicitudRechazada = solicitudExistente.copy(estado = EstadoSolicitud.RECHAZADA)
        val rechazadaConflicto = hayConflicto(
            listOf(solicitudRechazada),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(10, 0),
            fin = LocalTime.of(11, 0)
        )
        assertFalse(rechazadaConflicto)

        val solicitudPendiente = solicitudExistente.copy(estado = EstadoSolicitud.PENDIENTE)
        val pendienteConflicto = hayConflicto(
            listOf(solicitudPendiente),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(10, 0),
            fin = LocalTime.of(11, 0)
        )
        assertFalse(pendienteConflicto)
    }

    @Test
    @DisplayName("excluirId permite aprobar o actualizar la misma solicitud sin autoconflicto")
    fun excluirIdPermiteAprobarMismaSolicitudSinAutoconflicto() {
        val conflictoPropio = hayConflicto(
            listOf(solicitudExistente),
            Espacio.CANCHA,
            fecha,
            inicio = LocalTime.of(10, 0),
            fin = LocalTime.of(11, 0),
            excluirId = 1L
        )
        assertFalse(conflictoPropio, "Debe excluirse a sí misma al revalidarse")
    }
}
