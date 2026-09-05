package com.dany.macroautomator

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Captura cualquier cierre inesperado de la app y guarda el error
 * completo en DOS lugares distintos, por si uno de los dos falla
 * silenciosamente en este dispositivo:
 *  1) La carpeta pública de Descargas (vía MediaStore).
 *  2) La carpeta propia de la app (no requiere ningún permiso, casi
 *     nunca falla) en Android/data/com.dany.macroautomator/files/
 */
class MacroApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val text = sw.toString()

            // Intento 1: carpeta pública de Descargas.
            try {
                val resolver = applicationContext.contentResolver
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "macro_automator_crash.txt")
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out -> out.write(text.toByteArray()) }
                    }
                }
            } catch (t: Throwable) {
                // Seguimos con el segundo intento aunque este falle.
            }

            // Intento 2: carpeta propia de la app (no pide permiso).
            try {
                val dir = applicationContext.getExternalFilesDir(null)
                if (dir != null) {
                    File(dir, "macro_automator_crash.txt").writeText(text)
                }
            } catch (t: Throwable) {
                // Si ambos fallan, seguimos con el cierre normal de todas formas.
            }

            defaultHandler?.uncaughtException(thread, throwable)
                ?: Process.killProcess(Process.myPid())
        }
    }
}
