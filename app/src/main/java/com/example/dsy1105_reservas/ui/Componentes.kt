package com.example.dsy1105_reservas.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import com.mpaz.ticket.model.Prioridad
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val localeChile: Locale = Locale.forLanguageTag("es-CL")
private val formatoFecha = DateTimeFormatter.ofPattern("EEE dd/MM", localeChile)
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

fun LocalDate.texto(): String = format(formatoFecha).replaceFirstChar { it.uppercase() }
fun LocalTime.texto(): String = format(formatoHora)

fun Long.tamanoLegible(): String = when {
    this < 1024 -> "$this B"
    this < 1024 * 1024 -> "${this / 1024} KB"
    else -> "%.1f MB".format(localeChile, this / (1024.0 * 1024.0))
}

@Composable
fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.ALTA -> MaterialTheme.colorScheme.error
    Prioridad.MEDIA -> MaterialTheme.colorScheme.tertiary
    Prioridad.BAJA -> MaterialTheme.colorScheme.secondary
}

@Composable
fun ChipPrioridad(prioridad: Prioridad) {
    AssistChip(
        onClick = {},
        label = { Text(prioridad.etiqueta, color = colorPrioridad(prioridad)) }
    )
}

/**
 * Campo de texto con validación.
 */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    singleLine: Boolean = true,
    transformacion: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    val textoApoyo = error ?: ayuda
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        singleLine = singleLine,
        supportingText = if (textoApoyo != null) {
            { Text(textoApoyo) }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
        visualTransformation = transformacion,
        trailingIcon = trailing,
        modifier = modifier
    )
}

/** Campo de contraseña. */
@Composable
fun CampoClave(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    mostrar: Boolean,
    onAlternar: (() -> Unit)? = null,
    error: String? = null,
    ayuda: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    CampoTexto(
        valor = valor,
        onCambio = onCambio,
        etiqueta = etiqueta,
        error = error,
        ayuda = ayuda,
        teclado = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        transformacion = if (mostrar) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = if (onAlternar != null) {
            {
                IconButton(onClick = onAlternar) {
                    Icon(
                        imageVector = if (mostrar) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (mostrar) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            }
        } else null
    )
}

/** Selector desplegable genérico. */
@Composable
fun <T> Selector(
    etiqueta: String,
    valor: T?,
    opciones: List<T>,
    texto: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (valor != null) "$etiqueta: ${texto(valor)}" else etiqueta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(texto(opcion)) },
                    onClick = {
                        onSeleccion(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}
