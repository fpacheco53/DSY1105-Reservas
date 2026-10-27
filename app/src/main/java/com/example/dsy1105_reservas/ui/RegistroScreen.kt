package com.mpaz.ticket.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mpaz.ticket.TicketViewModel
import com.mpaz.ticket.Validaciones
import com.mpaz.ticket.model.Rol

/** Roles que una persona puede elegir al registrarse. */
private val rolesRegistrables = listOf(Rol.DOCENTE, Rol.ENCARGADO)

@Composable
fun RegistroScreen(vm: TicketViewModel, innerPadding: PaddingValues, onVolver: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    var mostrarClave by rememberSaveable { mutableStateOf(false) }
    var rol by rememberSaveable { mutableStateOf(Rol.DOCENTE) }
    var intento by rememberSaveable { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var errorGeneral by remember { mutableStateOf<String?>(null) }
    val foco = LocalFocusManager.current

    val errorNombre = if (intento) Validaciones.nombre(nombre) else null
    val errorCorreo = if (intento) Validaciones.correo(correo) else null
    val errorClave = if (intento) Validaciones.claveNueva(clave) else null
    val errorConfirmacion = if (intento) Validaciones.confirmacion(clave, confirmacion) else null

    fun enviar() {
        intento = true
        val hayErrores = Validaciones.nombre(nombre) != null ||
            Validaciones.correo(correo) != null ||
            Validaciones.claveNueva(clave) != null ||
            Validaciones.confirmacion(clave, confirmacion) != null
        if (hayErrores) return
        foco.clearFocus()
        cargando = true
        errorGeneral = null
        vm.registrar(nombre, correo, clave, rol) { error ->
            cargando = false
            if (error == null) onVolver() else errorGeneral = error
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Crear cuenta", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("Completa tus datos para usar MPAZ Ticket.", style = MaterialTheme.typography.bodyMedium)

            CampoTexto(valor = nombre, onCambio = { nombre = it }, etiqueta = "Nombre completo", error = errorNombre)
            CampoTexto(
                valor = correo,
                onCambio = { correo = it },
                etiqueta = "Correo",
                error = errorCorreo,
                teclado = KeyboardType.Email
            )
            CampoClave(
                valor = clave,
                onCambio = { clave = it },
                etiqueta = "Contraseña",
                mostrar = mostrarClave,
                onAlternar = { mostrarClave = !mostrarClave },
                error = errorClave,
                ayuda = "Mínimo 8 caracteres, con letras y números."
            )
            CampoClave(
                valor = confirmacion,
                onCambio = { confirmacion = it },
                etiqueta = "Repetir contraseña",
                mostrar = mostrarClave,
                error = errorConfirmacion,
                imeAction = ImeAction.Done,
                onImeAction = ::enviar
            )

            Selector("Rol", rol, rolesRegistrables, { it.etiqueta }, { rol = it }, Modifier.fillMaxWidth())
            Text(
                "Las cuentas de administrador las crea el colegio.",
                style = MaterialTheme.typography.bodySmall
            )

            AnimatedVisibility(visible = errorGeneral != null) {
                Text(errorGeneral.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = ::enviar, enabled = !cargando, modifier = Modifier.fillMaxWidth()) {
                if (cargando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Registrarme")
                }
            }
            TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
                Text("Ya tengo cuenta")
            }
        }
    }
}
