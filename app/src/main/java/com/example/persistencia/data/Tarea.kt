package com.example.persistencia.data

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
    val tiempoAcumuladoSegundos: Int = 0 // Nuevo campo para las métricas
)