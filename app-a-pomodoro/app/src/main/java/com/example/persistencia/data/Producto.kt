package com.example.persistencia.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Copia liviana guardada en Room de lo que devuelve la API.
 * Solo conservamos los campos que la app realmente muestra.
 */
@Entity(tableName = "tabla_productos")
data class Producto(
    @PrimaryKey
    val id: Int,
    val titulo: String,
    val precio: Double,
    val categoria: String,
    val imagenUrl: String,
    val ultimaActualizacion: Long = System.currentTimeMillis()
)

/** Forma exacta en la que responde fakestoreapi.com; se mapea a Producto antes de guardarse. */
data class ProductoRemoto(
    val id: Int,
    val title: String,
    val price: Double,
    val category: String,
    @SerializedName("image") val imagen: String
)

fun ProductoRemoto.aProducto() = Producto(
    id = id,
    titulo = title,
    precio = price,
    categoria = category,
    imagenUrl = imagen
)