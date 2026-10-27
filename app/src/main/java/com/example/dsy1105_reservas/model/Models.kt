package com.example.dsy1105_reservas.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

enum class Rol(val etiqueta: String) {
    ADMINISTRADOR("Administrador"),
    DOCENTE("Docente"),
    ENCARGADO("Encargado")
}

enum class Prioridad(val etiqueta: String, val peso: Int) {
    ALTA("Alta", 0),
    MEDIA("Media", 1),
    BAJA("Baja", 2)
}

enum class EstadoSolicitud(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada")
}

enum class Ciclo(val etiqueta: String) {
    PRIMERO("1° a 4°"),
    SEGUNDO("5° a 8°")
}

enum class Espacio(val etiqueta: String) {
    CANCHA("Cancha"),
    BIBLIOTECA("Biblioteca"),
    LABORATORIO("Laboratorio de ciencias"),
    COMPUTACION("Sala de computación"),
    AUDITORIO("Auditorio")
}

/** Cuenta de usuario */
@Entity(tableName = "usuarios", indices = [Index(value = ["correo"], unique = true)])
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val sal: String,
    val hashClave: String
)

/** Registro de eventos del colegio. */
@Entity(tableName = "eventos")
data class Evento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val fecha: LocalDate,
    val prioridad: Prioridad,
    val responsable: String
)

/** Levantamiento de requerimiento. */
@Entity(tableName = "requerimientos")
data class Requerimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val prioridad: Prioridad,
    val encargado: String,
    val completado: Boolean = false
)

@Entity(tableName = "solicitudes")
data class SolicitudEspacio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val espacio: Espacio,
    val solicitante: String,
    val ciclo: Ciclo,
    val fecha: LocalDate,
    val inicio: LocalTime,
    val fin: LocalTime,
    val motivo: String,
    val flexible: Boolean,
    val estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE
)

/**
 * Datos de un archivo subido.
 */
@Entity(tableName = "archivos")
data class Archivo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val propietario: String,
    val nombre: String,
    val nombreInterno: String,
    val tamanoBytes: Long,
    val subidoEn: Long
)
