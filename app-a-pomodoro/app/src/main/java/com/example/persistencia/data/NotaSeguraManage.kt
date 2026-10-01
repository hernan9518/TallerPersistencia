package com.example.persistencia.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.MessageDigest
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

        private const val CLAVE_PASSWORD_HASH = "password_hash"
    }

    // ---------------- Copia cifrada (interna) ----------------

    private val prefsCifradasInstancia by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_CIFRADAS,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun prefsCifradas() = prefsCifradasInstancia

    fun guardarNotaCifrada(texto: String) {
        prefsCifradas().edit().putString(CLAVE_NOTA, texto).apply()
    }

    fun leerNotaCifrada(): String {
        return prefsCifradas().getString(CLAVE_NOTA, "") ?: ""
    }
    // ---------------- Contraseña maestra ----------------
    // Se guarda solo el hash, nunca la contraseña en texto plano.

    fun existePassword(): Boolean = prefsCifradas().contains(CLAVE_PASSWORD_HASH)

    fun establecerPassword(password: String) {
        prefsCifradas().edit().putString(CLAVE_PASSWORD_HASH, hash(password)).apply()
    }

    fun verificarPassword(password: String): Boolean {
        val guardado = prefsCifradas().getString(CLAVE_PASSWORD_HASH, null) ?: return false
        return guardado == hash(password)
    }

    private fun hash(texto: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ---------------- Para el conteo expuesto a App B ----------------

    /** No revela contenido, solo si hay o no una nota guardada. */
    fun hayNotaGuardada(): Boolean = leerNotaCifrada().isNotBlank()

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