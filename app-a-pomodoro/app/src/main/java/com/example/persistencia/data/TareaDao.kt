package com.example.persistencia.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TareaDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTarea(tarea: Tarea)

    // READ (no muestra las tareas con borrado lógico)
    @Query("SELECT * FROM tabla_tareas WHERE eliminada = 0 ORDER BY id DESC")
    suspend fun obtenerTodasLasTareas(): List<Tarea>

    // UPDATE
    @Update
    suspend fun actualizarTarea(tarea: Tarea)

    // DELETE (físico)
    @Delete
    suspend fun eliminarTarea(tarea: Tarea)

    // --- Sincronización Offline-First ---
    @Query("SELECT * FROM tabla_tareas WHERE pendienteSincronizacion = 1")
    suspend fun obtenerPendientes(): List<Tarea>

    @Query("UPDATE tabla_tareas SET pendienteSincronizacion = 0 WHERE id = :id")
    suspend fun marcarSincronizada(id: Int)

    @Query("DELETE FROM tabla_tareas WHERE id = :id")
    suspend fun eliminarPorId(id: Int)
}
