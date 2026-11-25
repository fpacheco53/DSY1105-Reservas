package com.example.dsy1105_reservas

/** Reglas de validación de formularios. */
object Validaciones {

    private val REGEX_CORREO = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")

    fun obligatorio(valor: String, campo: String): String? =
        if (valor.isBlank()) "$campo es obligatorio." else null

    fun nombre(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu nombre."
        valor.trim().length < 3 -> "El nombre debe tener al menos 3 caracteres."
        else -> null
    }

    fun correo(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu correo."
        !REGEX_CORREO.matches(valor.trim()) -> "El correo no tiene un formato válido."
        else -> null
    }

    /** Iniciar sesión */
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
