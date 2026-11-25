package com.example.dsy1105_reservas.data

import com.example.dsy1105_reservas.model.Archivo
import com.example.dsy1105_reservas.model.Ciclo
import com.example.dsy1105_reservas.model.Espacio
import com.example.dsy1105_reservas.model.EstadoSolicitud
import com.example.dsy1105_reservas.model.Evento
import com.example.dsy1105_reservas.model.Prioridad
import com.example.dsy1105_reservas.model.Requerimiento
import com.example.dsy1105_reservas.model.Rol
import com.example.dsy1105_reservas.model.SolicitudEspacio
import com.example.dsy1105_reservas.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime

object ApiClient {

    private const val BASE_URL = "https://mpaz-backend.onrender.com/api"
    private const val TIMEOUT_MS = 45_000

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                    outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (code in 200..299) {
                Result.success(text)
            } else {
                val msg = when (code) {
                    401 -> "Correo o contraseña incorrectos."
                    403 -> "No tienes permisos para realizar esta acción."
                    404 -> "Recurso no encontrado."
                    409 -> "Conflicto con los datos existentes."
                    else -> text.ifBlank { "Error del servidor ($code)." }
                }
                Result.failure(IOException(msg))
            }
        } catch (e: Exception) {
            Result.failure(IOException("No se pudo conectar con el servidor.", e))
        } finally {
            conn?.disconnect()
        }
    }

    // ---- Usuarios ----

    suspend fun registrar(nombre: String, correo: String, clave: String, rol: Rol): Result<Usuario> {
        val body = JSONObject()
            .put("nombre", nombre)
            .put("correo", correo)
            .put("rol", rol.name)
            .put("clave", clave)
        return request("POST", "/usuarios/registro", body).map { parseUsuario(JSONObject(it)) }
    }

    suspend fun login(correo: String, clave: String): Result<Usuario> {
        val body = JSONObject()
            .put("correo", correo)
            .put("clave", clave)
        return request("POST", "/usuarios/login", body).map { parseUsuario(JSONObject(it)) }
    }

    // ---- Eventos ----

    suspend fun listarEventos(): Result<List<Evento>> =
        request("GET", "/eventos").map { text ->
            val arr = JSONArray(text)
            List(arr.length()) { i -> parseEvento(arr.getJSONObject(i)) }
        }

    suspend fun crearEvento(
        titulo: String,
        descripcion: String,
        fecha: LocalDate,
        prioridad: Prioridad,
        responsableId: Long
    ): Result<Evento> {
        val body = JSONObject()
            .put("titulo", titulo)
            .put("descripcion", descripcion)
            .put("fecha", fecha.toString())
            .put("prioridad", prioridad.name)
            .put("responsableId", responsableId)
        return request("POST", "/eventos", body).map { parseEvento(JSONObject(it)) }
    }

    // ---- Requerimientos ----

    suspend fun listarRequerimientosPendientes(): Result<List<Requerimiento>> =
        request("GET", "/requerimientos/pendientes").map { text ->
            val arr = JSONArray(text)
            List(arr.length()) { i -> parseRequerimiento(arr.getJSONObject(i)) }
        }

    suspend fun crearRequerimiento(
        titulo: String,
        descripcion: String,
        prioridad: Prioridad,
        encargadoId: Long
    ): Result<Requerimiento> {
        val body = JSONObject()
            .put("titulo", titulo)
            .put("descripcion", descripcion)
            .put("prioridad", prioridad.name)
            .put("encargadoId", encargadoId)
        return request("POST", "/requerimientos", body).map { parseRequerimiento(JSONObject(it)) }
    }

    suspend fun marcarRequerimiento(id: Long, completado: Boolean): Result<Requerimiento> =
        request("PATCH", "/requerimientos/$id/completado?completado=$completado")
            .map { parseRequerimiento(JSONObject(it)) }

    // ---- Solicitudes ----

    suspend fun listarSolicitudes(): Result<List<SolicitudEspacio>> =
        request("GET", "/solicitudes").map { text ->
            val arr = JSONArray(text)
            List(arr.length()) { i -> parseSolicitud(arr.getJSONObject(i)) }
        }

    suspend fun crearSolicitud(
        espacio: Espacio,
        solicitanteId: Long,
        ciclo: Ciclo,
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        motivo: String,
        flexible: Boolean
    ): Result<SolicitudEspacio> {
        val body = JSONObject()
            .put("espacio", espacio.name)
            .put("solicitanteId", solicitanteId)
            .put("ciclo", ciclo.name)
            .put("fecha", fecha.toString())
            .put("inicio", inicio.toString())
            .put("fin", fin.toString())
            .put("motivo", motivo)
            .put("flexible", flexible)
        return request("POST", "/solicitudes", body).map { parseSolicitud(JSONObject(it)) }
    }

    suspend fun aprobarSolicitud(id: Long): Result<SolicitudEspacio> =
        request("PATCH", "/solicitudes/$id/aprobar").map { parseSolicitud(JSONObject(it)) }

    suspend fun rechazarSolicitud(id: Long): Result<SolicitudEspacio> =
        request("PATCH", "/solicitudes/$id/rechazar").map { parseSolicitud(JSONObject(it)) }

    // ---- Archivos ----

    suspend fun listarArchivos(): Result<List<Archivo>> =
        request("GET", "/archivos").map { text ->
            val arr = JSONArray(text)
            List(arr.length()) { i -> parseArchivo(arr.getJSONObject(i)) }
        }

    suspend fun registrarArchivo(
        usuarioId: Long,
        nombre: String,
        nombreInterno: String,
        tamanoBytes: Long
    ): Result<Archivo> {
        val body = JSONObject()
            .put("usuarioId", usuarioId)
            .put("nombre", nombre)
            .put("nombreInterno", nombreInterno)
            .put("tamanoBytes", tamanoBytes)
        return request("POST", "/archivos", body).map { parseArchivo(JSONObject(it)) }
    }

    suspend fun eliminarArchivo(id: Long): Result<Unit> =
        request("DELETE", "/archivos/$id").map { }

    // ---- Parsers ----

    private fun parseUsuario(o: JSONObject) = Usuario(
        id = o.getLong("id"),
        nombre = o.getString("nombre"),
        correo = o.getString("correo"),
        rol = Rol.valueOf(o.getString("rol")),
        sal = "",
        hashClave = ""
    )

    private fun parseEvento(o: JSONObject) = Evento(
        id = o.getLong("id"),
        titulo = o.getString("titulo"),
        descripcion = o.getString("descripcion"),
        fecha = LocalDate.parse(o.getString("fecha")),
        prioridad = Prioridad.valueOf(o.getString("prioridad")),
        responsable = o.getString("responsable")
    )

    private fun parseRequerimiento(o: JSONObject) = Requerimiento(
        id = o.getLong("id"),
        titulo = o.getString("titulo"),
        descripcion = o.getString("descripcion"),
        prioridad = Prioridad.valueOf(o.getString("prioridad")),
        encargado = o.getString("encargado"),
        completado = o.getBoolean("completado")
    )

    private fun parseSolicitud(o: JSONObject) = SolicitudEspacio(
        id = o.getLong("id"),
        espacio = Espacio.valueOf(o.getString("espacio")),
        solicitante = o.getString("solicitante"),
        ciclo = Ciclo.valueOf(o.getString("ciclo")),
        fecha = LocalDate.parse(o.getString("fecha")),
        inicio = LocalTime.parse(o.getString("inicio")),
        fin = LocalTime.parse(o.getString("fin")),
        motivo = o.getString("motivo"),
        flexible = o.getBoolean("flexible"),
        estado = EstadoSolicitud.valueOf(o.getString("estado"))
    )

    private fun parseArchivo(o: JSONObject) = Archivo(
        id = o.getLong("id"),
        usuarioId = o.getLong("usuarioId"),
        propietario = o.getString("propietario"),
        nombre = o.getString("nombre"),
        nombreInterno = o.getString("nombreInterno"),
        tamanoBytes = o.getLong("tamanoBytes"),
        subidoEn = o.getLong("subidoEn")
    )
}
