package com.example.dsy1105_reservas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.dsy1105_reservas.model.Archivo
import com.example.dsy1105_reservas.model.Evento
import com.example.dsy1105_reservas.model.Requerimiento
import com.example.dsy1105_reservas.model.SolicitudEspacio
import com.example.dsy1105_reservas.model.Usuario
import java.time.LocalDate
import java.time.LocalTime

/** Room guarda fechas y horas como texto ISO-8601. Enums se guardan por nombre. */
class Convertidores {
    @TypeConverter
    fun fechaATexto(fecha: LocalDate?): String? = fecha?.toString()

    @TypeConverter
    fun textoAFecha(texto: String?): LocalDate? = texto?.let { LocalDate.parse(it) }

    @TypeConverter
    fun horaATexto(hora: LocalTime?): String? = hora?.toString()

    @TypeConverter
    fun textoAHora(texto: String?): LocalTime? = texto?.let { LocalTime.parse(it) }
}

@Database(
    entities = [
        Usuario::class,
        Evento::class,
        Requerimiento::class,
        SolicitudEspacio::class,
        Archivo::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Convertidores::class)
abstract class MpazDatabase : RoomDatabase() {

    abstract fun dao(): TicketDao

    companion object {
        @Volatile
        private var instancia: MpazDatabase? = null

        fun obtener(context: Context): MpazDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    MpazDatabase::class.java,
                    "mpaz_ticket.db"
                )
                    // Etapa de desarrollo: si cambia el esquema se recrea la base.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instancia = it }
            }
    }
}
