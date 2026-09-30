package com.example.persistencia.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Compara dos formas de almacenar la misma nota sensible:
 * - Cifrada, en almacenamiento interno privado (EncryptedSharedPreferences).
 * - Sin cifrar, en almacenamiento externo (simulado con getExternalFilesDir).
 */
class NotaSeguraManager(private val context: Context) {

    companion object {
        private const val PREFS_CIFRADAS = "notas_seguras_prefs"
        private const val CLAVE_NOTA = "nota_confidencial"
        private const val NOMBRE_ARCHIVO_EXTERNO = "nota_confidencial_sin_cifrar.txt"
    }

    // ---------------- Copia cifrada (interna) ----------------

    private fun prefsCifradas() = EncryptedSharedPreferences.create(
        context,
        PREFS_CIFRADAS,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun guardarNotaCifrada(texto: String) {
        prefsCifradas().edit().putString(CLAVE_NOTA, texto).apply()
    }

    fun leerNotaCifrada(): String {
        return prefsCifradas().getString(CLAVE_NOTA, "") ?: ""
    }

    // ---------------- Copia sin cifrar (externa) ----------------

    private fun archivoExterno(): File? {
        val carpeta = context.getExternalFilesDir(null) ?: return null
        return File(carpeta, NOMBRE_ARCHIVO_EXTERNO)
    }

    fun guardarCopiaSinCifrar(texto: String) {
        archivoExterno()?.writeText(texto)
    }

    fun leerCopiaSinCifrar(): String {
        val archivo = archivoExterno() ?: return "(sin almacenamiento externo disponible)"
        return if (archivo.exists()) archivo.readText() else "(aún no se ha guardado)"
    }

    /** Ruta real del archivo externo, útil para mostrarla en la demo/README. */
    fun rutaArchivoExterno(): String = archivoExterno()?.absolutePath ?: "(no disponible)"
}