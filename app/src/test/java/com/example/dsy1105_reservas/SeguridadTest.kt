package com.example.dsy1105_reservas

import com.example.dsy1105_reservas.data.Seguridad
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Base64

@DisplayName("Pruebas de Criptografía y Hash - Seguridad")
class SeguridadTest {

    @Test
    @DisplayName("nuevaSal genera cadenas aleatorias de 16 bytes en Base64")
    fun nuevaSalGeneraCadenasAleatoriasDe16Bytes() {
        val sal1 = Seguridad.nuevaSal()
        val sal2 = Seguridad.nuevaSal()

        assertNotNull(sal1)
        assertFalse(sal1.isBlank())
        assertNotEquals(sal1, sal2, "Dos sales consecutivas deben ser aleatorias y diferentes")

        val bytes = Base64.getDecoder().decode(sal1)
        assertEquals(16, bytes.size, "La sal debe tener exactamente 16 bytes")
    }

    @Test
    @DisplayName("hash PBKDF2 es determinista con la misma clave y sal")
    fun hashEsDeterministaConMismaClaveYSal() {
        val clave = "ColegioProtectora2026!"
        val sal = Seguridad.nuevaSal()

        val hash1 = Seguridad.hash(clave, sal)
        val hash2 = Seguridad.hash(clave, sal)

        assertEquals(hash1, hash2, "Misma clave y sal deben generar exactamente el mismo hash")
    }

    @Test
    @DisplayName("hash cambia drásticamente si cambia la sal o la clave")
    fun hashCambiaSiCambiaSalOClave() {
        val clave = "MiPassword123"
        val sal1 = Seguridad.nuevaSal()
        val sal2 = Seguridad.nuevaSal()

        val hashConSal1 = Seguridad.hash(clave, sal1)
        val hashConSal2 = Seguridad.hash(clave, sal2)
        assertNotEquals(hashConSal1, hashConSal2, "Distintas sales deben generar distintos hashes")

        val hashClaveDistinta = Seguridad.hash("MiPassword124", sal1)
        assertNotEquals(hashConSal1, hashClaveDistinta, "Distintas claves deben generar distintos hashes")
    }

    @Test
    @DisplayName("verificar responde true solo ante credencial correcta")
    fun verificarRespondeTrueSoloAnteCredencialCorrecta() {
        val clave = "AdminColegio@2026"
        val sal = Seguridad.nuevaSal()
        val hash = Seguridad.hash(clave, sal)

        assertTrue(Seguridad.verificar(clave, sal, hash))
        assertFalse(Seguridad.verificar("AdminColegio@2025", sal, hash))
        assertFalse(Seguridad.verificar("admincolegio@2026", sal, hash))

        val otraSal = Seguridad.nuevaSal()
        assertFalse(Seguridad.verificar(clave, otraSal, hash))
    }
}
