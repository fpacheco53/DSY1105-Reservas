package com.example.dsy1105_reservas

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dsy1105_reservas.data.AlmacenArchivos
import com.example.dsy1105_reservas.data.MpazDatabase
import com.example.dsy1105_reservas.data.Seguridad
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
import kotlinx.coroutines.flow.Flow
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

    private val dao = MpazDatabase.obtener(application).dao()
    private val prefs = application.getSharedPreferences("mpaz_sesion", Context.MODE_PRIVATE)

    // ---- Sesión ----

    val sesion = MutableStateFlow<Sesion>(Sesion.Cargando)
    val avisoLogin = MutableStateFlow<String?>(null)

    val usuarioActual: Usuario?
        get() = (sesion.value as? Sesion.Activa)?.usuario

    // ---- Datos persistentes ----

    private fun <T> Flow<T>.comoEstado(inicial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, inicial)

    val eventos = dao.eventos().comoEstado(emptyList())
    val requerimientos = dao.requerimientos().comoEstado(emptyList())
    val solicitudes = dao.solicitudes().comoEstado(emptyList())

    /** Admin ve todas las fotos, otros roles solo propias */
    val archivos = combine(dao.archivos(), sesion) { lista, sesion ->
        val usuario = (sesion as? Sesion.Activa)?.usuario
        when {
            usuario == null -> emptyList()
            usuario.rol == Rol.ADMINISTRADOR -> lista
            else -> lista.filter { it.usuarioId == usuario.id }
        }
    }.comoEstado(emptyList())

    init {
        viewModelScope.launch {
            if (dao.contarUsuarios() == 0) crearUsuario("Administrador", ADMIN_CORREO, ADMIN_CLAVE, Rol.ADMINISTRADOR)
            val id = prefs.getLong(CLAVE_USUARIO, -1L)
            val usuario = if (id >= 0) dao.usuarioPorId(id) else null
            sesion.value = if (usuario != null) Sesion.Activa(usuario) else Sesion.SinSesion
        }
    }

    // ---- Autenticación ----

    /** Hash de la contraseña */
    private suspend fun crearUsuario(nombre: String, correo: String, clave: String, rol: Rol) {
        val sal = Seguridad.nuevaSal()
        val hash = withContext(Dispatchers.Default) { Seguridad.hash(clave, sal) }
        dao.insertarUsuario(Usuario(nombre = nombre.trim(), correo = correo, rol = rol, sal = sal, hashClave = hash))
    }

    /** Mensaje de error o null si la cuenta ya se creo */
    fun registrar(nombre: String, correo: String, clave: String, rol: Rol, onResultado: (String?) -> Unit) {
        viewModelScope.launch {
            val correoNormalizado = correo.trim().lowercase()
            when {
                rol == Rol.ADMINISTRADOR -> onResultado("Las cuentas de administrador las crea el colegio.")
                dao.usuarioPorCorreo(correoNormalizado) != null -> onResultado("Ya existe una cuenta con ese correo.")
                else -> {
                    crearUsuario(nombre, correoNormalizado, clave, rol)
                    avisoLogin.value = "Cuenta creada. Inicia sesión con tus credenciales."
                    onResultado(null)
                }
            }
        }
    }

    /** Mensaje de error o null si la sesion ya se inicio */
    fun iniciarSesion(correo: String, clave: String, onResultado: (String?) -> Unit) {
        viewModelScope.launch {
            val usuario = dao.usuarioPorCorreo(correo.trim().lowercase())
            val valido = usuario != null &&
                withContext(Dispatchers.Default) { Seguridad.verificar(clave, usuario.sal, usuario.hashClave) }
            if (!valido || usuario == null) {
                onResultado("Correo o contraseña incorrectos.")
                return@launch
            }
            prefs.edit().putLong(CLAVE_USUARIO, usuario.id).apply()
            avisoLogin.value = null
            sesion.value = Sesion.Activa(usuario)
            onResultado(null)
        }
    }

    fun cerrarSesion() {
        prefs.edit().remove(CLAVE_USUARIO).apply()
        sesion.value = Sesion.SinSesion
    }

    // ---- Eventos y requerimientos ----

    fun registrarEvento(titulo: String, descripcion: String, fecha: LocalDate, prioridad: Prioridad, responsable: String) {
        viewModelScope.launch {
            dao.insertarEvento(Evento(titulo = titulo.trim(), descripcion = descripcion.trim(), fecha = fecha, prioridad = prioridad, responsable = responsable.trim()))
        }
    }

    fun agregarRequerimiento(titulo: String, descripcion: String, prioridad: Prioridad, encargado: String) {
        viewModelScope.launch {
            dao.insertarRequerimiento(Requerimiento(titulo = titulo.trim(), descripcion = descripcion.trim(), prioridad = prioridad, encargado = encargado.trim()))
        }
    }

    fun alternarRequerimiento(id: Long) {
        val actual = requerimientos.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { dao.actualizarRequerimiento(actual.copy(completado = !actual.completado)) }
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
            dao.insertarSolicitud(
                SolicitudEspacio(
                    espacio = espacio,
                    solicitante = solicitante.trim(),
                    ciclo = ciclo,
                    fecha = fecha,
                    inicio = inicio,
                    fin = fin,
                    motivo = motivo.trim(),
                    flexible = flexible
                )
            )
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
            dao.actualizarSolicitud(
                solicitud.copy(estado = if (aprobar) EstadoSolicitud.APROBADA else EstadoSolicitud.RECHAZADA)
            )
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

    /** Copia la foto a almacenamiento interno y registra datos */
    fun cargarFoto(uri: Uri, onResultado: (String?) -> Unit) {
        val usuario = usuarioActual ?: return onResultado("Debes iniciar sesión para subir fotos.")
        viewModelScope.launch {
            val error = try {
                dao.insertarArchivo(withContext(Dispatchers.IO) { copiarFoto(uri, usuario) })
                null
            } catch (e: FotoInvalida) {
                e.message
            }
            onResultado(error)
        }
    }

    private fun copiarFoto(uri: Uri, usuario: Usuario): Archivo {
        val app = getApplication<Application>()
        val resolver = app.contentResolver

        // Validacion de foto
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

    /** Elimina el registro y el archivo fisico. Solo admin. */
    fun eliminarArchivo(archivo: Archivo) {
        val usuario = usuarioActual ?: return
        if (usuario.rol != Rol.ADMINISTRADOR && archivo.usuarioId != usuario.id) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { AlmacenArchivos.archivo(getApplication<Application>(), archivo).delete() }
            dao.eliminarArchivo(archivo)
        }
    }

    companion object {
        private const val CLAVE_USUARIO = "usuario_id"
        const val ADMIN_CORREO = "admin@mpaz.cl"
        const val ADMIN_CLAVE = "Admin1234"
    }
}
