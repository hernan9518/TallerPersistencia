package com.example.persistencia.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_tareas")
data class Tarea(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titulo: String,
    val descripcion: String,
    val estadoCompletado: Boolean = false,
    val fechaCreacion: String,
    val tiempoAcumuladoSegundos: Int = 0,

    // --- Offline-First (Punto 2, ítem 5) ---
    // true = el cambio se hizo localmente y aún no se ha "enviado" al servidor
    @ColumnInfo(defaultValue = "1")
    val pendienteSincronizacion: Boolean = true,
    // Borrado lógico: la fila se conserva hasta que la sincronización lo confirme
    @ColumnInfo(defaultValue = "0")
    val eliminada: Boolean = false
)