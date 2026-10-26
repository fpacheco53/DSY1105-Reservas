package com.mpaz.ticket.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class Bloque(val numero: Int, val inicio: LocalTime, val fin: LocalTime)

/**
 * Horarios:
 * - Lunes a jueves 8:15 a 17:15, viernes 8:15 a 14:00.
 * - Almuerzo 13:15 a 14:00.
 * - Bloques fijos de 45 minutos.
 */
object HorarioEscolar {
    val INICIO: LocalTime = LocalTime.of(8, 15)
    val ALMUERZO_INICIO: LocalTime = LocalTime.of(13, 15)
    val ALMUERZO_FIN: LocalTime = LocalTime.of(14, 0)
    const val MINUTOS_BLOQUE = 45L

    fun esDiaHabil(fecha: LocalDate): Boolean = cierre(fecha.dayOfWeek) != null

    fun cierre(dia: DayOfWeek): LocalTime? = when (dia) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY ->
            LocalTime.of(17, 15)
        DayOfWeek.FRIDAY -> LocalTime.of(14, 0)
        else -> null
    }

    /** Bloques de 45 minutos, el almuerzo se omite. */
    fun bloques(dia: DayOfWeek): List<Bloque> {
        val cierre = cierre(dia) ?: return emptyList()
        val resultado = mutableListOf<Bloque>()
        var inicio = INICIO
        while (!inicio.plusMinutes(MINUTOS_BLOQUE).isAfter(cierre)) {
            val fin = inicio.plusMinutes(MINUTOS_BLOQUE)
            if (inicio.isBefore(ALMUERZO_FIN) && fin.isAfter(ALMUERZO_INICIO)) {
                inicio = ALMUERZO_FIN
                continue
            }
            resultado.add(Bloque(resultado.size + 1, inicio, fin))
            inicio = fin
        }
        return resultado
    }

    /** Validacion rango de horario. */
    fun dentroDeHorario(dia: DayOfWeek, inicio: LocalTime, fin: LocalTime): Boolean {
        val cierre = cierre(dia) ?: return false
        if (inicio.isBefore(INICIO) || fin.isAfter(cierre)) return false
        return !(inicio.isBefore(ALMUERZO_FIN) && fin.isAfter(ALMUERZO_INICIO))
    }

    fun proximosDiasHabiles(desde: LocalDate, cantidad: Int): List<LocalDate> {
        val dias = mutableListOf<LocalDate>()
        var actual = desde
        while (dias.size < cantidad) {
            if (esDiaHabil(actual)) dias.add(actual)
            actual = actual.plusDays(1)
        }
        return dias
    }

    /** Solicitudes flexibles. */
    fun iniciosFlexibles(dia: DayOfWeek): List<LocalTime> {
        val cierre = cierre(dia) ?: return emptyList()
        val lista = mutableListOf<LocalTime>()
        var t = INICIO
        while (t.isBefore(cierre)) {
            val enAlmuerzo = !t.isBefore(ALMUERZO_INICIO) && t.isBefore(ALMUERZO_FIN)
            if (!enAlmuerzo) lista.add(t)
            t = t.plusMinutes(15)
        }
        return lista
    }
}
