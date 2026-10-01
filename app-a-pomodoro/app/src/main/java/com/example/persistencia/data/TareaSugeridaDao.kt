package com.example.persistencia.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TareaSugeridaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarTodas(sugeridas: List<TareaSugerida>)

    @Query("SELECT * FROM tabla_tareas_sugeridas ORDER BY id ASC LIMIT 20")
    suspend fun obtenerTodas(): List<TareaSugerida>

    @Query("UPDATE tabla_tareas_sugeridas SET importada = 1 WHERE id = :id")
    suspend fun marcarImportada(id: Int)
}