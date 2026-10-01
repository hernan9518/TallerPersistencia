package com.example.persistencia.data

import retrofit2.http.GET

interface ProductoApiService {
    @GET("products")
    suspend fun obtenerProductos(): List<ProductoRemoto>
}