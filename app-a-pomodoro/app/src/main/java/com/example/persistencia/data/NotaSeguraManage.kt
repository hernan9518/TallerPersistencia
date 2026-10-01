package com.example.persistencia.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.security.MessageDigest
import java.util.UUID

data class NotaConfidencial(
    val id: String,
    val titulo: String,
    val contenido: String
)

/**
 * Bóveda de notas confidenciales protegida por una contraseña maestra.
 * Cada nota se guarda cifrada (EncryptedSharedPreferences). Además se mantiene
 * una copia sin cifrar en almacenamiento externo, solo para comparar qué tan
 * legible queda una frente a la otra (Punto 4, ítems 1 a 3).
 */
class NotaSeguraManager(private val context: Context) {

    companion object {
        private const val PREFS_CIFRADAS = "notas_seguras_prefs"
        private const val CLAVE_PASSWORD_HASH = "password_hash"
        private const val CLAVE_NOTAS_LISTA = "notas_lista"
        private const val NOMBRE_ARCHIVO_EXTERNO = "notas_confidenciales_sin_cifrar.txt"
    }

    private val gson = Gson()

    // Una sola instancia reutilizada: crear una nueva en cada llamada puede
    // devolver lecturas desfasadas justo después de escribir.
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

    // ---------------- Contraseña maestra ----------------

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

    // ---------------- Bóveda de notas (cifrada) ----------------

    fun listarNotas(): List<NotaConfidencial> {
        val json = prefsCifradas().getString(CLAVE_NOTAS_LISTA, null) ?: return emptyList()
        val tipo = object : TypeToken<List<NotaConfidencial>>() {}.type
        return gson.fromJson(json, tipo) ?: emptyList()
    }

    fun obtenerNota(id: String): NotaConfidencial? = listarNotas().find { it.id == id }

    fun cantidadNotas(): Int = listarNotas().size

    /** Crea una nota nueva si [id] es null, o actualiza la existente si ya existe. */
    fun guardarNota(id: String?, titulo: String, contenido: String) {
        val notas = listarNotas().toMutableList()
        if (id == null) {
            notas.add(NotaConfidencial(UUID.randomUUID().toString(), titulo, contenido))
        } else {
            val indice = notas.indexOfFirst { it.id == id }
            if (indice >= 0) {
                notas[indice] = notas[indice].copy(titulo = titulo, contenido = contenido)
            } else {
                notas.add(NotaConfidencial(id, titulo, contenido))
            }
        }
        guardarListaCompleta(notas)
    }

    fun eliminarNota(id: String) {
        guardarListaCompleta(listarNotas().filterNot { it.id == id })
    }

    private fun guardarListaCompleta(notas: List<NotaConfidencial>) {
        prefsCifradas().edit().putString(CLAVE_NOTAS_LISTA, gson.toJson(notas)).apply()
        guardarCopiaSinCifrar(notas) // mantiene la copia externa sincronizada, para la comparación
    }

    // ---------------- Copia sin cifrar (externa, solo para comparar) ----------------

    private fun archivoExterno(): File? {
        val carpeta = context.getExternalFilesDir(null) ?: return null
        return File(carpeta, NOMBRE_ARCHIVO_EXTERNO)
    }

    private fun guardarCopiaSinCifrar(notas: List<NotaConfidencial>) {
        val texto = notas.joinToString("\n\n") { "Título: ${it.titulo}\nContenido: ${it.contenido}" }
        archivoExterno()?.writeText(texto)
    }

    fun leerCopiaSinCifrar(): String {
        val archivo = archivoExterno() ?: return "(sin almacenamiento externo disponible)"
        return if (archivo.exists()) archivo.readText() else "(aún no se ha guardado ninguna nota)"
    }

    /** Ruta real del archivo externo, útil para mostrarla en la demo/README. */
    fun rutaArchivoExterno(): String = archivoExterno()?.absolutePath ?: "(no disponible)"
}