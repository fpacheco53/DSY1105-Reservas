package com.example.dsy1105_reservas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mpaz.ticket.model.Archivo
import com.mpaz.ticket.model.Evento
import com.mpaz.ticket.model.Requerimiento
import com.mpaz.ticket.model.SolicitudEspacio
import com.mpaz.ticket.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    // ---- Usuarios ----
    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contarUsuarios(): Int

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun usuarioPorCorreo(correo: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun usuarioPorId(id: Long): Usuario?

    @Insert
    suspend fun insertarUsuario(usuario: Usuario): Long

    // ---- Eventos ----
    @Query("SELECT * FROM eventos ORDER BY fecha ASC")
    fun eventos(): Flow<List<Evento>>

    @Insert
    suspend fun insertarEvento(evento: Evento): Long

    // ---- Requerimientos ----
    @Query("SELECT * FROM requerimientos")
    fun requerimientos(): Flow<List<Requerimiento>>

    @Insert
    suspend fun insertarRequerimiento(requerimiento: Requerimiento): Long

    @Update
    suspend fun actualizarRequerimiento(requerimiento: Requerimiento)

    // ---- Solicitudes de espacios ----
    @Query("SELECT * FROM solicitudes ORDER BY fecha ASC, inicio ASC")
    fun solicitudes(): Flow<List<SolicitudEspacio>>

    @Insert
    suspend fun insertarSolicitud(solicitud: SolicitudEspacio): Long

    @Update
    suspend fun actualizarSolicitud(solicitud: SolicitudEspacio)

    // ---- Archivos ----
    @Query("SELECT * FROM archivos ORDER BY subidoEn DESC")
    fun archivos(): Flow<List<Archivo>>

    @Insert
    suspend fun insertarArchivo(archivo: Archivo): Long

    @Delete
    suspend fun eliminarArchivo(archivo: Archivo)
}
