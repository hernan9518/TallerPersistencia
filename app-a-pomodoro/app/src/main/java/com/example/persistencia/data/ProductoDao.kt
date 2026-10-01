package com.example.persistencia.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(productos: List<Producto>)

    @Query("SELECT * FROM tabla_productos ORDER BY id ASC")
    suspend fun obtenerTodos(): List<Producto>

    @Query("SELECT COUNT(*) FROM tabla_productos")
    suspend fun contar(): Int
}