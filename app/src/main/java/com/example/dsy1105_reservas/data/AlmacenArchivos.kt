package com.example.dsy1105_reservas.data

import android.content.Context
import com.example.dsy1105_reservas.model.Archivo
import java.io.File

/** Ubicación de los archivos cargados dentro del almacenamiento interno. */
object AlmacenArchivos {
    private const val CARPETA = "archivos"
    const val MAX_BYTES = 15L * 1024 * 1024

    fun carpeta(context: Context): File = File(context.filesDir, CARPETA).apply { mkdirs() }

    fun archivo(context: Context, archivo: Archivo): File = File(carpeta(context), archivo.nombreInterno)
}
