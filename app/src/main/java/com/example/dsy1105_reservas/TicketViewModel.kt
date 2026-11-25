package com.example.dsy1105_reservas

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dsy1105_reservas.data.AlmacenArchivos
import com.example.dsy1105_reservas.data.ApiClient
import com.example.dsy1105_reservas.model.Archivo
import com.example.dsy1105_reservas.model.Ciclo
import com.example.dsy1105_reservas.model.EstadoSolicitud
import com.example.dsy1105_reservas.model.Espacio
import com.example.dsy1105_reservas.model.Evento
import com.example.dsy1105_reservas.model.HorarioEscolar
import com.example.dsy1105_reservas.model.Prioridad
import com.example.dsy1105_reservas.model.Requerimiento
import com.example.dsy1105_reservas.model.Rol
import com.example.dsy1105_reservas.model.SolicitudEspacio
import com.example.dsy1105_reservas.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** Estado de la sesión */
sealed interface Sesion {
    data object Cargando : Sesion
    data object SinSesion : Sesion
    data class Activa(val usuario: Usuario) : Sesion
}

/** Error de validación al subir una foto */
private class FotoInvalida(mensaje: String) : Exception(mensaje)

/** Estado central de la app */
class TicketViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mpaz_sesion", Context.MODE_PRIVATE)

    // ---- Sesión ----

    val sesion = MutableStateFlow<Sesion>(Sesion.Cargando)
    val avisoLogin = MutableStateFlow<String?>(null)

    val usuarioActual: Usuario?
        get() = (sesion.value as? Sesion.Activa)?.usuario

    // ---- Datos sincronizados con el backend ----

    private val _eventos = MutableStateFlow<List<Evento>>(emptyList())
    val eventos: StateFlow<List<Evento>> = _eventos

    private val _requerimientos = MutableStateFlow<List<Requerimiento>>(emptyList())
    val requerimientos: StateFlow<List<Requerimiento>> = _requerimientos

    private val _solicitudes = MutableStateFlow<List<SolicitudEspacio>>(emptyList())
    val solicitudes: StateFlow<List<SolicitudEspacio>> = _solicitudes

    private val _archivos = MutableStateFlow<List<Archivo>>(emptyList())

    /** Admin ve todas las fotos, otros roles solo propias */
    val archivos: StateFlow<List<Archivo>> = combine(_archivos, sesion) { lista, sesion ->
        val usuario = (sesion as? Sesion.Activa)?.usuario
        when {
            usuario == null -> emptyList()
            usuario.rol == Rol.ADMINISTRADOR -> lista
            else -> lista.filter { it.usuarioId == usuario.id }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val usuario = leerUsuarioGuardado()
            sesion.value = if (usuario != null) Sesion.Activa(usuario) else Sesion.SinSesion
            if (usuario != null) sincronizarTodo()
        }
    }

    private fun guardarUsuario(usuario: Usuario) {
        prefs.edit()
            .putLong(CLAVE_USUARIO, usuario.id)
            .putString(CLAVE_NOMBRE, usuario.nombre)
            .putString(CLAVE_CORREO, usuario.correo)
            .putString(CLAVE_ROL, usuario.rol.name)
            .apply()
    }

    private fun leerUsuarioGuardado(): Usuario? {
        val id = prefs.getLong(CLAVE_USUARIO, -1L)
        if (id < 0) return null
        val nombre = prefs.getString(CLAVE_NOMBRE, null) ?: return null
        val correo = prefs.getString(CLAVE_CORREO, null) ?: return null
        val rolStr = prefs.getString(CLAVE_ROL, null) ?: return null
        val rol = runCatching { Rol.valueOf(rolStr) }.getOrNull() ?: return null
        return Usuario(id = id, nombre = nombre, correo = correo, rol = rol, sal = "", hashClave = "")
    }

    fun sincronizarTodo() {
        viewModelScope.launch {
            ApiClient.listarEventos().onSuccess { _eventos.value = it }
            ApiClient.listarRequerimientosPendientes().onSuccess { _requerimientos.value = it }
            ApiClient.listarSolicitudes().onSuccess { _solicitudes.value = it }
            ApiClient.listarArchivos().onSuccess { _archivos.value = it }
        }
    }

    // ---- Autenticación ----

    /** Mensaje de error o null si la cuenta ya se creo */
    fun registrar(nombre: String, correo: String, clave: String, rol: Rol, onResultado: (String?) -> Unit) {
        if (rol == Rol.ADMINISTRADOR) {
            onResultado("Las cuentas de administrador las crea el colegio.")
            return
        }
        viewModelScope.launch {
            val res = ApiClient.registrar(nombre.trim(), correo.trim().lowercase(), clave, rol)
            res.fold(
                onSuccess = {
                    avisoLogin.value = "Cuenta creada. Inicia sesión con tus credenciales."
                    onResultado(null)
                },
                onFailure = { onResultado(it.message ?: "No se pudo registrar la cuenta.") }
            )
        }
    }

    /** Mensaje de error o null si la sesion ya se inicio */
    fun iniciarSesion(correo: String, clave: String, onResultado: (String?) -> Unit) {
        viewModelScope.launch {
            val res = ApiClient.login(correo.trim().lowercase(), clave)
            res.fold(
                onSuccess = { usuario ->
                    guardarUsuario(usuario)
                    avisoLogin.value = null
                    sesion.value = Sesion.Activa(usuario)
                    sincronizarTodo()
                    onResultado(null)
                },
                onFailure = { onResultado(it.message ?: "Correo o contraseña incorrectos.") }
            )
        }
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
        sesion.value = Sesion.SinSesion
        _eventos.value = emptyList()
        _requerimientos.value = emptyList()
        _solicitudes.value = emptyList()
        _archivos.value = emptyList()
    }

    // ---- Eventos y requerimientos ----

    fun registrarEvento(titulo: String, descripcion: String, fecha: LocalDate, prioridad: Prioridad, responsable: String) {
        val usuario = usuarioActual ?: return
        viewModelScope.launch {
            ApiClient.crearEvento(titulo.trim(), descripcion.trim(), fecha, prioridad, usuario.id)
                .onSuccess { ApiClient.listarEventos().onSuccess { _eventos.value = it } }
        }
    }

    fun agregarRequerimiento(titulo: String, descripcion: String, prioridad: Prioridad, encargado: String) {
        val usuario = usuarioActual ?: return
        viewModelScope.launch {
            ApiClient.crearRequerimiento(titulo.trim(), descripcion.trim(), prioridad, usuario.id)
                .onSuccess { ApiClient.listarRequerimientosPendientes().onSuccess { _requerimientos.value = it } }
        }
    }

    fun alternarRequerimiento(id: Long) {
        val actual = requerimientos.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            ApiClient.marcarRequerimiento(id, !actual.completado)
                .onSuccess { ApiClient.listarRequerimientosPendientes().onSuccess { _requerimientos.value = it } }
        }
    }

    // ---- Solicitud de espacios ----

    /** Mensaje de error o null si la solicitud se registro como pendiente */
    fun enviarSolicitud(
        espacio: Espacio,
        solicitante: String,
        ciclo: Ciclo,
        fecha: LocalDate,
        inicio: LocalTime?,
        fin: LocalTime?,
        motivo: String,
        flexible: Boolean
    ): String? {
        val usuario = usuarioActual ?: return "Debes iniciar sesión para solicitar un espacio."
        if (solicitante.isBlank()) return "Indica quién solicita el espacio."
        if (motivo.isBlank()) return "Indica el motivo de la solicitud."
        if (inicio == null || fin == null) return "Selecciona el horario."
        if (!HorarioEscolar.esDiaHabil(fecha)) return "El colegio no funciona ese día."
        if (!fin.isAfter(inicio)) return "La hora de término debe ser posterior al inicio."
        if (!HorarioEscolar.dentroDeHorario(fecha.dayOfWeek, inicio, fin)) {
            return "El horario queda fuera de la jornada o invade el almuerzo."
        }
        if (!flexible && Duration.between(inicio, fin).toMinutes() != HorarioEscolar.MINUTOS_BLOQUE) {
            return "Las solicitudes en horario de clases deben ser de ${HorarioEscolar.MINUTOS_BLOQUE} minutos."
        }
        if (hayConflicto(espacio, fecha, inicio, fin)) {
            return "El espacio ya tiene una solicitud aprobada que se cruza con ese horario."
        }
        viewModelScope.launch {
            ApiClient.crearSolicitud(espacio, usuario.id, ciclo, fecha, inicio, fin, motivo.trim(), flexible)
                .onSuccess { ApiClient.listarSolicitudes().onSuccess { _solicitudes.value = it } }
        }
        return null
    }

    /** Admin resuelve solicitudes. */
    fun resolverSolicitud(id: Long, aprobar: Boolean): String? {
        if (usuarioActual?.rol != Rol.ADMINISTRADOR) return "Solo el administrador puede resolver solicitudes."
        val solicitud = solicitudes.value.firstOrNull { it.id == id } ?: return null
        if (aprobar && hayConflicto(solicitud.espacio, solicitud.fecha, solicitud.inicio, solicitud.fin, excluirId = id)) {
            return "No se puede aprobar: se cruza con otra solicitud aprobada."
        }
        viewModelScope.launch {
            val res = if (aprobar) ApiClient.aprobarSolicitud(id) else ApiClient.rechazarSolicitud(id)
            res.onSuccess { ApiClient.listarSolicitudes().onSuccess { _solicitudes.value = it } }
        }
        return null
    }

    private fun hayConflicto(espacio: Espacio, fecha: LocalDate, inicio: LocalTime, fin: LocalTime, excluirId: Long = -1): Boolean =
        solicitudes.value.any {
            it.id != excluirId &&
                it.estado == EstadoSolicitud.APROBADA &&
                it.espacio == espacio &&
                it.fecha == fecha &&
                it.inicio.isBefore(fin) && inicio.isBefore(it.fin)
        }

    // ---- Fotos ----

    /** Copia la foto a almacenamiento interno y registra datos en el backend */
    fun cargarFoto(uri: Uri, onResultado: (String?) -> Unit) {
        val usuario = usuarioActual ?: return onResultado("Debes iniciar sesión para subir fotos.")
        viewModelScope.launch {
            val error = try {
                val local = withContext(Dispatchers.IO) { copiarFoto(uri, usuario) }
                val res = ApiClient.registrarArchivo(usuario.id, local.nombre, local.nombreInterno, local.tamanoBytes)
                res.fold(
                    onSuccess = {
                        ApiClient.listarArchivos().onSuccess { _archivos.value = it }
                        null
                    },
                    onFailure = { it.message ?: "No se pudo registrar la foto en el servidor." }
                )
            } catch (e: FotoInvalida) {
                e.message
            }
            onResultado(error)
        }
    }

    private fun copiarFoto(uri: Uri, usuario: Usuario): Archivo {
        val app = getApplication<Application>()
        val resolver = app.contentResolver

        val mime = resolver.getType(uri).orEmpty().lowercase()
        val extension = when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> throw FotoInvalida("Solo se pueden subir fotos (JPG, PNG, WebP o HEIC).")
        }
        val nombre = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: "foto"

        val nombreInterno = "${UUID.randomUUID()}.$extension"
        val destino = File(AlmacenArchivos.carpeta(app), nombreInterno)
        try {
            val entrada = resolver.openInputStream(uri) ?: throw FotoInvalida("No se pudo abrir la foto seleccionada.")
            entrada.use { origen -> destino.outputStream().use { origen.copyTo(it) } }
            if (destino.length() == 0L) throw FotoInvalida("La foto está vacía.")
            if (destino.length() > AlmacenArchivos.MAX_BYTES) throw FotoInvalida("La foto supera el máximo de 15 MB.")
        } catch (e: Exception) {
            destino.delete()
            throw if (e is FotoInvalida) e else FotoInvalida("No se pudo copiar la foto. Intenta nuevamente.")
        }

        return Archivo(
            usuarioId = usuario.id,
            propietario = usuario.nombre,
            nombre = nombre,
            nombreInterno = nombreInterno,
            tamanoBytes = destino.length(),
            subidoEn = System.currentTimeMillis()
        )
    }

    /** Elimina el registro y el archivo fisico. Solo admin o propietario. */
    fun eliminarArchivo(archivo: Archivo) {
        val usuario = usuarioActual ?: return
        if (usuario.rol != Rol.ADMINISTRADOR && archivo.usuarioId != usuario.id) return
        viewModelScope.launch {
            ApiClient.eliminarArchivo(archivo.id).onSuccess {
                withContext(Dispatchers.IO) { AlmacenArchivos.archivo(getApplication<Application>(), archivo).delete() }
                ApiClient.listarArchivos().onSuccess { _archivos.value = it }
            }
        }
    }

    companion object {
        private const val CLAVE_USUARIO = "usuario_id"
        private const val CLAVE_NOMBRE = "usuario_nombre"
        private const val CLAVE_CORREO = "usuario_correo"
        private const val CLAVE_ROL = "usuario_rol"
    }
}
