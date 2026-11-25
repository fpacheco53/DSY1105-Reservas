package com.example.dsy1105_reservas

import com.example.dsy1105_reservas.model.HorarioEscolar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Pruebas de reglas de calendario y bloques - HorarioEscolar")
class HorarioEscolarTest {

    @Nested
    @DisplayName("Días hábiles y hora de cierre")
    inner class DiasHabilesYCierre {

        @ParameterizedTest(name = "El día {0} debe ser hábil")
        @EnumSource(
            value = DayOfWeek::class,
            names = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
        )
        @DisplayName("Días de semana son hábiles")
        fun diasDeSemanaSonHabiles(dia: DayOfWeek) {
            val fecha = LocalDate.of(2026, 11, 23).with(dia)
            assertTrue(HorarioEscolar.esDiaHabil(fecha))
            assertNotNull(HorarioEscolar.cierre(dia))
        }

        @ParameterizedTest(name = "El día {0} debe ser inhábil")
        @EnumSource(
            value = DayOfWeek::class,
            names = ["SATURDAY", "SUNDAY"]
        )
        @DisplayName("Fines de semana son inhábiles")
        fun finesDeSemanaSonInhabiles(dia: DayOfWeek) {
            val fecha = LocalDate.of(2026, 11, 23).with(dia)
            assertFalse(HorarioEscolar.esDiaHabil(fecha))
            assertNull(HorarioEscolar.cierre(dia))
        }

        @Test
        @DisplayName("Cierre de lunes a jueves es a las 17:15")
        fun cierreLunesAJuevesEs1715() {
            assertEquals(LocalTime.of(17, 15), HorarioEscolar.cierre(DayOfWeek.MONDAY))
            assertEquals(LocalTime.of(17, 15), HorarioEscolar.cierre(DayOfWeek.THURSDAY))
        }

        @Test
        @DisplayName("Cierre de viernes es a las 14:00")
        fun cierreViernesEs1400() {
            assertEquals(LocalTime.of(14, 0), HorarioEscolar.cierre(DayOfWeek.FRIDAY))
        }
    }

    @Nested
    @DisplayName("Generación de bloques escolares")
    inner class GeneracionBloques {

        @Test
        @DisplayName("Bloques de lunes a jueves duran 45 minutos y omiten almuerzo")
        fun bloquesLunesAJuevesDuran45MinutosYOmitenAlmuerzo() {
            val bloques = HorarioEscolar.bloques(DayOfWeek.MONDAY)
            assertFalse(bloques.isEmpty())

            for (b in bloques) {
                val duracion = Duration.between(b.inicio, b.fin).toMinutes()
                assertEquals(HorarioEscolar.MINUTOS_BLOQUE, duracion, "Cada bloque debe durar 45 min")

                val invadeAlmuerzo = b.inicio.isBefore(HorarioEscolar.ALMUERZO_FIN) &&
                        b.fin.isAfter(HorarioEscolar.ALMUERZO_INICIO)
                assertFalse(invadeAlmuerzo, "Ningún bloque debe invadir el almuerzo (${b.inicio} - ${b.fin})")
            }

            val bloqueDespuesAlmuerzo = bloques.firstOrNull { it.inicio == HorarioEscolar.ALMUERZO_FIN }
            assertNotNull(bloqueDespuesAlmuerzo, "Debe haber un bloque que inicie exactamente a las 14:00")
        }

        @Test
        @DisplayName("Bloques de viernes no deben exceder las 14:00")
        fun bloquesViernesNoExceden1400() {
            val bloques = HorarioEscolar.bloques(DayOfWeek.FRIDAY)
            assertFalse(bloques.isEmpty())
            val ultimoBloque = bloques.last()
            assertTrue(!ultimoBloque.fin.isAfter(LocalTime.of(14, 0)))
        }

        @Test
        @DisplayName("Bloques en fin de semana retorna lista vacía")
        fun bloquesFinDeSemanaRetornaListaVacia() {
            assertTrue(HorarioEscolar.bloques(DayOfWeek.SATURDAY).isEmpty())
            assertTrue(HorarioEscolar.bloques(DayOfWeek.SUNDAY).isEmpty())
        }
    }

    @Nested
    @DisplayName("Validación dentroDeHorario")
    inner class ValidacionDentroDeHorario {

        @Test
        @DisplayName("Rango normal dentro de jornada valida true")
        fun rangoNormalDentroDeJornadaValidaTrue() {
            assertTrue(HorarioEscolar.dentroDeHorario(DayOfWeek.MONDAY, LocalTime.of(8, 15), LocalTime.of(9, 0)))
            assertTrue(HorarioEscolar.dentroDeHorario(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0)))
        }

        @Test
        @DisplayName("Rango antes del inicio escolar valida false")
        fun rangoAntesDeInicioEscolarValidaFalse() {
            assertFalse(HorarioEscolar.dentroDeHorario(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(8, 45)))
        }

        @Test
        @DisplayName("Rango después del cierre escolar valida false")
        fun rangoDespuesDeCierreEscolarValidaFalse() {
            assertFalse(HorarioEscolar.dentroDeHorario(DayOfWeek.THURSDAY, LocalTime.of(17, 0), LocalTime.of(17, 30)))
            assertFalse(HorarioEscolar.dentroDeHorario(DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(14, 45)))
        }

        @Test
        @DisplayName("Rango que invade el horario de almuerzo valida false")
        fun rangoQueInvadeAlmuerzoValidaFalse() {
            assertFalse(HorarioEscolar.dentroDeHorario(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(13, 45)))
            assertFalse(HorarioEscolar.dentroDeHorario(DayOfWeek.MONDAY, LocalTime.of(13, 30), LocalTime.of(14, 15)))
            assertFalse(HorarioEscolar.dentroDeHorario(DayOfWeek.MONDAY, LocalTime.of(13, 15), LocalTime.of(14, 0)))
        }

        @Test
        @DisplayName("Rango en fin de semana valida false")
        fun rangoFinDeSemanaValidaFalse() {
            assertFalse(HorarioEscolar.dentroDeHorario(DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)))
        }
    }

    @Nested
    @DisplayName("Proyección de días hábiles futuros")
    inner class ProximosDiasHabilesTest {

        @Test
        @DisplayName("Salta fines de semana al proyectar días hábiles")
        fun saltaFinesDeSemanaAlProyectarDiasHabiles() {
            val viernes = LocalDate.of(2026, 11, 27)
            val dias = HorarioEscolar.proximosDiasHabiles(viernes, 3)

            assertEquals(3, dias.size)
            assertEquals(DayOfWeek.FRIDAY, dias[0].dayOfWeek)
            assertEquals(DayOfWeek.MONDAY, dias[1].dayOfWeek)
            assertEquals(DayOfWeek.TUESDAY, dias[2].dayOfWeek)

            assertTrue(dias.none { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY })
        }
    }

    @Nested
    @DisplayName("Inicios flexibles")
    inner class IniciosFlexiblesTest {

        @Test
        @DisplayName("Intervalos de 15 min no contienen horario de almuerzo")
        fun intervalosDe15MinNoContienenAlmuerzo() {
            val inicios = HorarioEscolar.iniciosFlexibles(DayOfWeek.MONDAY)
            assertFalse(inicios.isEmpty())

            assertFalse(inicios.contains(LocalTime.of(13, 15)))
            assertFalse(inicios.contains(LocalTime.of(13, 30)))
            assertFalse(inicios.contains(LocalTime.of(13, 45)))
            assertTrue(inicios.contains(LocalTime.of(13, 0)))
            assertTrue(inicios.contains(LocalTime.of(14, 0)))
        }
    }
}
