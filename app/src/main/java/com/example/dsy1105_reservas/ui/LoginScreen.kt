package com.example.dsy1105_reservas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dsy1105_reservas.TicketViewModel
import com.example.dsy1105_reservas.Validaciones

/** Vista de inicio de sesión. */
@Composable
fun LoginScreen(vm: TicketViewModel, innerPadding: PaddingValues, onIrARegistro: () -> Unit) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var mostrarClave by rememberSaveable { mutableStateOf(false) }
    var intento by rememberSaveable { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var errorGeneral by remember { mutableStateOf<String?>(null) }
    val aviso by vm.avisoLogin.collectAsState()
    val foco = LocalFocusManager.current

    val errorCorreo = if (intento) Validaciones.correo(correo) else null
    val errorClave = if (intento) Validaciones.claveLogin(clave) else null

    fun enviar() {
        intento = true
        if (Validaciones.correo(correo) != null || Validaciones.claveLogin(clave) != null) return
        foco.clearFocus()
        cargando = true
        errorGeneral = null
        vm.iniciarSesion(correo, clave) { error ->
            cargando = false
            errorGeneral = error
        }
    }

    val entrada = remember { MutableTransitionState(false).apply { targetState = true } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(innerPadding)
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visibleState = entrada,
                enter = fadeIn(tween(600)) + scaleIn(initialScale = 0.9f, animationSpec = tween(600))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "MPAZ Ticket",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Eventos, requerimientos y espacios del colegio",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge)

            AnimatedVisibility(visible = aviso != null) {
                Text(aviso.orEmpty(), color = MaterialTheme.colorScheme.primary)
            }

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
                imeAction = ImeAction.Done,
                onImeAction = ::enviar
            )

            AnimatedVisibility(visible = errorGeneral != null) {
                Text(errorGeneral.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = ::enviar, enabled = !cargando, modifier = Modifier.fillMaxWidth()) {
                if (cargando) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Ingresar")
                }
            }
            TextButton(onClick = onIrARegistro, modifier = Modifier.fillMaxWidth()) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }
    }
}
