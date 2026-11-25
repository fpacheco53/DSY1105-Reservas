package com.example.dsy1105_reservas

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Pruebas de Reglas de Validación de Formularios - Validaciones")
class ValidacionesTest {

    @Nested
    @DisplayName("Campo obligatorio")
    inner class ObligatorioTest {

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "   ", "\t", "\n"])
        @DisplayName("Valores vacíos o en blanco retornan mensaje de error")
        fun valoresVaciosRetornanError(valor: String) {
            val error = Validaciones.obligatorio(valor, "El campo")
            assertNotNull(error)
            assertEquals("El campo es obligatorio.", error)
        }

        @ParameterizedTest
        @ValueSource(strings = ["a", "Texto", " 123 "])
        @DisplayName("Valores con texto retornan null")
        fun valoresConTextoRetornanNull(valor: String) {
            assertNull(Validaciones.obligatorio(valor, "El campo"))
        }
    }

    @Nested
    @DisplayName("Validación de nombre")
    inner class NombreTest {

        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        @DisplayName("Nombre vacío pide ingresar nombre")
        fun nombreVacioPideIngresarNombre(valor: String) {
            assertEquals("Ingresa tu nombre.", Validaciones.nombre(valor))
        }

        @ParameterizedTest
        @ValueSource(strings = ["a", "ab", "  ab  "])
        @DisplayName("Nombre con menos de 3 caracteres exige longitud mínima")
        fun nombreCortoExigeLongitudMinima(valor: String) {
            assertEquals("El nombre debe tener al menos 3 caracteres.", Validaciones.nombre(valor))
        }

        @ParameterizedTest
        @ValueSource(strings = ["Ana", "Juan", "Pedro Pablo", "  María  "])
        @DisplayName("Nombre válido de 3 o más caracteres retorna null")
        fun nombreValidoRetornaNull(valor: String) {
            assertNull(Validaciones.nombre(valor))
        }
    }

    @Nested
    @DisplayName("Validación de correo electrónico")
    inner class CorreoTest {

        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        @DisplayName("Correo vacío pide ingresar correo")
        fun correoVacioPideIngresarCorreo(valor: String) {
            assertEquals("Ingresa tu correo.", Validaciones.correo(valor))
        }

        @ParameterizedTest
        @ValueSource(strings = ["usuario", "usuario@", "usuario@dominio", "usuario@.com", "@dominio.com"])
        @DisplayName("Correo con formato inválido retorna error")
        fun correoConFormatoInvalidoRetornaError(valor: String) {
            assertEquals("El correo no tiene un formato válido.", Validaciones.correo(valor))
        }

        @ParameterizedTest
        @ValueSource(strings = ["test@colegio.cl", "docente.mpaz@gmail.com", "admin+1@escuela.edu"])
        @DisplayName("Correo con formato correcto retorna null")
        fun correoConFormatoCorrectoRetornaNull(valor: String) {
            assertNull(Validaciones.correo(valor))
        }
    }

    @Nested
    @DisplayName("Validación de contraseña nueva")
    inner class ClaveNuevaTest {

        @Test
        @DisplayName("Clave vacía pide ingresar clave")
        fun claveVaciaPideIngresarClave() {
            assertEquals("Ingresa una contraseña.", Validaciones.claveNueva(""))
        }

        @ParameterizedTest
        @ValueSource(strings = ["1234567", "abcdefg", "Abc123"])
        @DisplayName("Clave con menos de 8 caracteres exige longitud mínima")
        fun claveCortaExigeLongitudMinima(valor: String) {
            assertEquals("Debe tener al menos 8 caracteres.", Validaciones.claveNueva(valor))
        }

        @ParameterizedTest
        @ValueSource(strings = ["abcdefghij", "sololetraslargas"])
        @DisplayName("Clave sin números pide letras y números")
        fun claveSinNumerosPideLetrasYNumeros(valor: String) {
            assertEquals("Debe incluir letras y números.", Validaciones.claveNueva(valor))
        }

        @ParameterizedTest
        @ValueSource(strings = ["1234567890", "987654321"])
        @DisplayName("Clave sin letras pide letras y números")
        fun claveSinLetrasPideLetrasYNumeros(valor: String) {
            assertEquals("Debe incluir letras y números.", Validaciones.claveNueva(valor))
        }

        @ParameterizedTest
        @ValueSource(strings = ["Colegio2026", "pass12345", "1234567a"])
        @DisplayName("Clave de al menos 8 caracteres con letras y números es válida")
        fun claveValidaRetornaNull(valor: String) {
            assertNull(Validaciones.claveNueva(valor))
        }
    }

    @Nested
    @DisplayName("Confirmación de contraseña")
    inner class ConfirmacionTest {

        @Test
        @DisplayName("Confirmación vacía pide repetir la clave")
        fun confirmacionVaciaPideRepetirClave() {
            assertEquals("Repite la contraseña.", Validaciones.confirmacion("Password123", ""))
        }

        @ParameterizedTest
        @CsvSource(
            "Password123, Password124",
            "Clave1, Clave2",
            "MismaClave, mismaclave"
        )
        @DisplayName("Claves no coincidentes retornan error")
        fun clavesNoCoincidentesRetornanError(clave: String, confirmacion: String) {
            assertEquals("Las contraseñas no coinciden.", Validaciones.confirmacion(clave, confirmacion))
        }

        @Test
        @DisplayName("Claves coincidentes retornan null")
        fun clavesCoincidentesRetornanNull() {
            assertNull(Validaciones.confirmacion("Password123", "Password123"))
        }
    }
}
