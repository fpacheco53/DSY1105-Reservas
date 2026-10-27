package com.example.dsy1105_reservas

import android.util.Patterns

/** Reglas de validación de formularios. */
object Validaciones {

    fun obligatorio(valor: String, campo: String): String? =
        if (valor.isBlank()) "$campo es obligatorio." else null

    fun nombre(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu nombre."
        valor.trim().length < 3 -> "El nombre debe tener al menos 3 caracteres."
        else -> null
    }

    fun correo(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu correo."
        !Patterns.EMAIL_ADDRESS.matcher(valor.trim()).matches() -> "El correo no tiene un formato válido."
        else -> null
    }

    /** Iniciar sesión solo exige que el campo no esté vacío. */
    fun claveLogin(valor: String): String? =
        if (valor.isEmpty()) "Ingresa tu contraseña." else null

    /** Crear una cuenta */
    fun claveNueva(valor: String): String? = when {
        valor.isEmpty() -> "Ingresa una contraseña."
        valor.length < 8 -> "Debe tener al menos 8 caracteres."
        valor.none { it.isLetter() } || valor.none { it.isDigit() } -> "Debe incluir letras y números."
        else -> null
    }

    fun confirmacion(clave: String, confirmacion: String): String? = when {
        confirmacion.isEmpty() -> "Repite la contraseña."
        clave != confirmacion -> "Las contraseñas no coinciden."
        else -> null
    }
}
