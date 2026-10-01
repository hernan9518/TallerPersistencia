package com.example.persistencia.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Copia liviana (caché) de lo que devuelve JSONPlaceholder /todos.
 * Vive en una tabla separada de Tarea: mientras no se "importe", no es
 * una tarea real de la app, solo una sugerencia descargada de la nube.
 */
@Entity(tableName = "tabla_tareas_sugeridas")
data class TareaSugerida(
    @PrimaryKey
    val id: Int,
    val titulo: String,
    val completadaEnOrigen: Boolean,
    val importada: Boolean = false
)

/** Forma exacta en la que responde https://jsonplaceholder.typicode.com/todos */
data class TareaSugeridaRemota(
    val id: Int,
    @SerializedName("title") val titulo: String,
    @SerializedName("completed") val completada: Boolean
)

fun TareaSugeridaRemota.aTareaSugerida() = TareaSugerida(
    id = id,
    titulo = titulo,
    completadaEnOrigen = completada
)