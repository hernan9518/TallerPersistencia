package com.example.persistencia.data

import retrofit2.http.GET
import retrofit2.http.Query

interface TareaSugeridaApiService {
    @GET("todos")
    suspend fun obtenerTareas(@Query("_limit") limite: Int = 20): List<TareaSugeridaRemota>
}